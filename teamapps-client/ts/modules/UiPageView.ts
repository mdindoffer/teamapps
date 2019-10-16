/*-
 * ========================LICENSE_START=================================
 * TeamApps
 * ---
 * Copyright (C) 2014 - 2019 TeamApps.org
 * ---
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */

import {AbstractUiComponent} from "./AbstractUiComponent";
import {TeamAppsUiContext} from "./TeamAppsUiContext";
import {UiPageViewBlock_Alignment, UiPageViewBlockConfig} from "../generated/UiPageViewBlockConfig";
import {UiMessagePageViewBlockConfig} from "../generated/UiMessagePageViewBlockConfig";
import {insertAfter, insertBefore, parseHtml, removeClassesByFunction, removeDangerousTags} from "./Common";
import {UiComponentConfig} from "../generated/UiComponentConfig";
import {UiCitationPageViewBlockConfig} from "../generated/UiCitationPageViewBlockConfig";
import {UiComponentPageViewBlockConfig} from "../generated/UiComponentPageViewBlockConfig";
import {UiPageViewConfig} from "../generated/UiPageViewConfig";
import {TeamAppsUiComponentRegistry} from "./TeamAppsUiComponentRegistry";
import {UiPageViewBlockCreatorImageAlignment} from "../generated/UiPageViewBlockCreatorImageAlignment";
import {executeWhenFirstDisplayed} from "./util/ExecuteWhenFirstDisplayed";
import {UiComponent} from "./UiComponent";
import {UiHorizontalElementAlignment} from "../generated/UiHorizontalElementAlignment";
// require("bootstrap/js/transition");
// require("bootstrap/js/carousel");


interface Row {
	$row: HTMLElement;
	$headerContainer: HTMLElement;
	$leftColumn: HTMLElement;
	$rightColumn: HTMLElement;
	blocks: Block[];
}

interface Block {
	$blockWrapper: HTMLElement;
	$blockContentContainer: HTMLElement;
	block: BlockComponent<UiPageViewBlockConfig>;
}

export class UiPageView extends AbstractUiComponent<UiPageViewConfig> {

	private $component: HTMLElement;
	private rows: Row[] = [];

	constructor(config: UiPageViewConfig, context: TeamAppsUiContext) {
		super(config, context);
		this.$component = parseHtml(`<div class="UiPageView"></div>`);

		if (config.blocks) {
			for (var i = 0; i < config.blocks.length; i++) {
				this.addBlock(config.blocks[i], false);
			}
		}
	}

	public getMainDomElement(): HTMLElement {
		return this.$component;
	}

	@executeWhenFirstDisplayed()
	public addBlock(blockConfig: UiPageViewBlockConfig, before: boolean, otherBlockId?: string) {
		let row;
		if (this.rows.length == 0) {
			row = this.addRow(false);
		} else if (before && otherBlockId == null) {
			row = this.rows[0];
			if (row.blocks[0].block.getAlignment() == UiPageViewBlock_Alignment.FULL) {
				row = this.addRow(true);
			}
		} else if (!before && otherBlockId == null) {
			if (blockConfig.alignment === UiPageViewBlock_Alignment.FULL) {
				this.addRow(false);
			}
			row = this.rows[this.rows.length - 1];
		} else if (before && otherBlockId != null) {
			// TODO
		}

		let $blockWrapper = parseHtml(`<div class="block-wrapper teamapps-blurredBackgroundImage">
    <div class="background-color-div"></div>
</div>`);
		let $blockContentContainer = $blockWrapper.querySelector<HTMLElement>(':scope .background-color-div');
		let block = new (blockTypes[blockConfig._type as keyof typeof blockTypes] as any)(blockConfig, this._context) as BlockComponent<UiPageViewBlockConfig>;
		$blockContentContainer.appendChild(block.getMainDomElement());
		row.blocks.push({$blockWrapper, $blockContentContainer, block});

		// TODO prepend vs append vs insert...
		if (blockConfig.alignment === UiPageViewBlock_Alignment.FULL) {
			row.$headerContainer.appendChild($blockWrapper);
		} else if (blockConfig.alignment === UiPageViewBlock_Alignment.LEFT) {
			row.$leftColumn.appendChild($blockWrapper);
		} else if (blockConfig.alignment === UiPageViewBlock_Alignment.RIGHT) {
			row.$rightColumn.appendChild($blockWrapper);
		}
	}

	private addRow(before: boolean, otherRowIndex?: number): Row {
		let $row = parseHtml('<div class="block-section row">');
		let $headerContainer = parseHtml('<div class="header-container col-md-12">');
		$row.appendChild($headerContainer);
		let $leftColumn = parseHtml('<div class="left-column col-md-8">');
		$row.appendChild($leftColumn);
		let $rightColumn = parseHtml('<div class="right-column col-md-4">');
		$row.appendChild($rightColumn);
		let row: Row = {
			$row: $row,
			$leftColumn: $leftColumn,
			$rightColumn: $rightColumn,
			$headerContainer: $headerContainer,
			blocks: []
		};

		if (before && otherRowIndex == null) {
			this.rows.unshift(row);
			this.$component.prepend($row);
		} else if (!before && otherRowIndex == null) {
			this.rows.push(row);
			this.$component.appendChild($row);
		} else if (before && otherRowIndex != null) {
			this.rows.splice(otherRowIndex, 0, row);
			insertBefore($row, this.rows[otherRowIndex].$row)
		} else if (!before && otherRowIndex != null) {
			this.rows.splice(otherRowIndex + 1, 0, row);
			insertAfter($row, this.rows[otherRowIndex].$row)
		}

		return row;
	};

	onResize(): void {
		this.rows.forEach(row => {
			row.blocks.forEach(block => block.block.reLayout());
		});
	}

	public destroy(): void {
		this.rows.forEach(row => {
			row.blocks.forEach(block => block.block.destroy());
		});
	}

}

abstract class BlockComponent<C extends UiPageViewBlockConfig> {
	constructor(protected config: C, protected context: TeamAppsUiContext) {
	}

	public getAlignment() {
		return this.config.alignment;
	}

	abstract getMainDomElement(): HTMLElement;

	public reLayout() {
		// default implementation
	}

	public destroy() {
		// default implementation
	}
}

class UiMessagePageViewBlock extends BlockComponent<UiMessagePageViewBlockConfig> {
	private $main: HTMLElement;
	private $topRecord: HTMLElement;
	private $htmlContainer: HTMLElement;
	private $images: HTMLElement;

	constructor(config: UiMessagePageViewBlockConfig, context: TeamAppsUiContext) {
		super(config, context);

		this.$main = parseHtml(`<div class="UiMessagePageViewBlock">
	<div class="top-record"></div>
	<div class="html"></div>
	<div class="images"></div>
</div>`);
		this.$topRecord = this.$main.querySelector(":scope .top-record");
		this.$htmlContainer = this.$main.querySelector(":scope .html");
		this.$images = this.$main.querySelector(":scope .images");

		removeClassesByFunction(this.$topRecord.classList, className => className.startsWith("align-"));
		this.$topRecord.classList.add("align-" + UiHorizontalElementAlignment[config.topRecordAlignment].toLocaleLowerCase());
		let topTemplateRenderer = context.templateRegistry.createTemplateRenderer(config.topTemplate);
		this.$topRecord.innerHTML = config.topRecord != null ? topTemplateRenderer.render(config.topRecord.values) : "";

		this.$htmlContainer.innerHTML = config.html != null ? removeDangerousTags(config.html) : "";

		if (config.imageUrls) {
				$(this.$images).slick({
					dots: true,
					infinite: true,
					speed: 300,
					slidesToShow: 1,
					centerMode: true,
					variableWidth: true,
					draggable: true,
				});

				for (var i = 0; i < this.config.imageUrls.length; i++) {
					const $sliderItem = document.createElement("div");
					$sliderItem.classList.add("slider-item");
					const $image = new Image();
					$sliderItem.appendChild($image);
					$image.classList.add("slider-item-img");
					$image.onload = () => {
						$(this.$images).slick('slickAdd', $sliderItem);
						this.reLayout();
					};
					$image.src = this.config.imageUrls[i];
				}
		}
	}

	reLayout() {
	}

	public getMainDomElement(): HTMLElement {
		return this.$main;
	}

	public destroy(): void {
		// nothing to do
	}
}

class UiCitationPageViewBlock extends BlockComponent<UiCitationPageViewBlockConfig> {

	private $component: HTMLElement;

	constructor(config: UiCitationPageViewBlockConfig, context: TeamAppsUiContext) {
		super(config, context);

		this.$component = parseHtml(`<div class="UiCitationPageViewBlock">
    <div class="creator-image-wrapper align-${UiPageViewBlockCreatorImageAlignment[config.creatorImageAlignment].toLowerCase()}">
		${config.creatorImageUrl ? `<img class="creator-image" src="${config.creatorImageUrl}"></img>` : ''}
    </div>
    <div class="content-wrapper">

    </div>
</div>`);
		let $contentWrapper = this.$component.querySelector<HTMLElement>(':scope .content-wrapper');
		$contentWrapper.appendChild($(`<div class="citation">${removeDangerousTags(config.citation)}</div>`)[0]);
		$contentWrapper.appendChild($(`<div class="author">${removeDangerousTags(config.author)}</div>`)[0]);
	}


	public getMainDomElement(): HTMLElement {
		return this.$component;
	}

	public set attachedToDom(attachedToDom: boolean) {
		// do nothing
	}

	public destroy(): void {
		// nothing to do
	}
}

class UiComponentPageViewBlock extends BlockComponent<UiComponentPageViewBlockConfig> {

	private $div: HTMLElement;
	private component: UiComponent<UiComponentConfig>;
	private $componentWrapper: HTMLElement;

	constructor(config: UiComponentPageViewBlockConfig, context: TeamAppsUiContext) {
		super(config, context);

		this.$div = parseHtml(`<div class="UiComponentPageViewBlock" style="height:${config.height}px">
                <div class="component-wrapper"></div>
            </div>`);
		this.$componentWrapper = this.$div.querySelector<HTMLElement>(':scope .component-wrapper');

		if (config.title) {
			this.$div.prepend($(`<div class="title">${removeDangerousTags(config.title)}</div>`)[0]);
		}

		this.component = config.component as UiComponent;
		this.$componentWrapper.appendChild(this.component.getMainDomElement());
	}

	public getMainDomElement(): HTMLElement {
		return this.$div;
	}

	public destroy(): void {
	}
}

var blockTypes = {
	"UiMessagePageViewBlock": UiMessagePageViewBlock,
	"UiCitationPageViewBlock": UiCitationPageViewBlock,
	"UiComponentPageViewBlock": UiComponentPageViewBlock
};

TeamAppsUiComponentRegistry.registerComponentClass("UiPageView", UiPageView);
