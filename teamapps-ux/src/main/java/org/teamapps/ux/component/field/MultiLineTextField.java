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
package org.teamapps.ux.component.field;

import org.teamapps.dto.UiField;
import org.teamapps.dto.UiMultiLineTextField;

public class MultiLineTextField extends TextField {

	private int minHeight = 100;
	private int maxHeight;

	public MultiLineTextField() {
		super();
	}

	@Override
	public UiField createUiComponent() {
		UiMultiLineTextField uiField = new UiMultiLineTextField(getId());
		mapAbstractFieldAttributesToUiField(uiField);
		uiField.setMaxCharacters(getMaxCharacters());
		uiField.setShowClearButton(isShowClearButton());
		uiField.setEmptyText(getEmptyText());
		uiField.setMinHeight(minHeight);
		uiField.setMaxHeight(maxHeight);
		return uiField;
	}

	public void append(String s, boolean scrollToBottom) {
		MultiWriteLockableValue.Lock lock = setAndLockValue(s);
		if (isRendered()) {
			getSessionContext().queueCommand(new UiMultiLineTextField.AppendCommand(getId(), s, scrollToBottom), aVoid -> lock.release());
		} else {
			lock.release();
		}
	}

	public int getMinHeight() {
		return minHeight;
	}

	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
		queueCommandIfRendered(() -> new UiMultiLineTextField.SetMinHeightCommand(getId(), minHeight));
	}

	public int getMaxHeight() {
		return maxHeight;
	}

	public void setMaxHeight(int maxHeight) {
		this.maxHeight = maxHeight;
		queueCommandIfRendered(() -> new UiMultiLineTextField.SetMaxHeightCommand(getId(), maxHeight));
	}

	public void setFixedHeight(int height) {
		this.minHeight = height;
		this.maxHeight = height;
		queueCommandIfRendered(() -> new UiMultiLineTextField.SetMinHeightCommand(getId(), height));
		queueCommandIfRendered(() -> new UiMultiLineTextField.SetMaxHeightCommand(getId(), height));
	}

}