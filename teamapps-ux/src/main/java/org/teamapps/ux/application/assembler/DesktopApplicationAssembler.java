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
package org.teamapps.ux.application.assembler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamapps.ux.application.*;
import org.teamapps.ux.application.perspective.Perspective;
import org.teamapps.ux.application.view.View;
import org.teamapps.ux.application.view.ViewSize;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.workspacelayout.ViewGroupPanelState;
import org.teamapps.ux.component.workspacelayout.WorkSpaceLayout;
import org.teamapps.ux.component.workspacelayout.WorkSpaceLayoutView;
import org.teamapps.ux.component.workspacelayout.WorkSpaceLayoutViewGroup;
import org.teamapps.ux.component.workspacelayout.definition.LayoutItemDefinition;

import java.util.List;

public class DesktopApplicationAssembler implements ApplicationAssembler {

    private static Logger LOGGER = LoggerFactory.getLogger(DesktopApplicationAssembler.class);

    private ResponsiveApplicationToolbar responsiveApplicationToolbar;
    private WorkSpaceLayout workSpaceLayout;
    private LayoutItemDefinition currentLayout;

    public DesktopApplicationAssembler() {
        this.workSpaceLayout = new WorkSpaceLayout();
    }

    public void addView(View view) {
        String layoutPosition = view.getLayoutPosition();
        WorkSpaceLayoutViewGroup viewGroup = workSpaceLayout.getViewGroupById(layoutPosition);
        if (viewGroup == null) {
            LOGGER.error("Cannot find viewGroup for layout position " + layoutPosition);
            return;
        }

        WorkSpaceLayoutView layoutView = new WorkSpaceLayoutView(workSpaceLayout, view.getPanel(), view.getPanel().getTitle(), true, false);
        viewGroup.addView(layoutView);

        if (viewGroup != null && viewGroup.getPanelState() != ViewGroupPanelState.NORMAL) {
            viewGroup.setPanelState(ViewGroupPanelState.NORMAL);
        }
    }

    public boolean isViewAttached(View view) {
        String layoutPosition = view.getLayoutPosition();
        WorkSpaceLayoutViewGroup viewGroup = workSpaceLayout.getViewGroupById(layoutPosition);
        if (viewGroup == null) {
            return false;
        }
        for (WorkSpaceLayoutView layoutView : viewGroup.getViews()) {
            if (view.getPanel().equals(layoutView.getPanel())) {
                return true;
            }
        }
        return false;
    }

    public void removeView(View view) {
        WorkSpaceLayoutView layoutView = workSpaceLayout.getViewByPanel(view.getPanel());
        if (layoutView != null) {
            layoutView.remove();
        }
    }

    @Override
    public void setWorkSpaceToolbar(ResponsiveApplicationToolbar responsiveApplicationToolbar) {
        this.responsiveApplicationToolbar = responsiveApplicationToolbar;
        workSpaceLayout.setToolbar(responsiveApplicationToolbar.getToolbar());
    }

    @Override
    public Component createApplication(ResponsiveApplication application) {
        return workSpaceLayout;
    }

    @Override
    public void handleApplicationViewAdded(ResponsiveApplication application, View view) {
        addView(view);
    }

    @Override
    public void handleApplicationViewRemoved(ResponsiveApplication application, View view) {
        removeView(view);
    }

    @Override
    public void handlePerspectiveChange(ResponsiveApplication application, Perspective perspective, Perspective previousPerspective, List<View> activeViews, List<View> addedViews, List<View> removedViews) {
        if (currentLayout == null || !perspective.getLayout().equals(currentLayout)) {
            currentLayout = perspective.getLayout();
            workSpaceLayout.applyLayoutDefinition(currentLayout);
        }
        removedViews.forEach(view -> removeView(view));
        addedViews.forEach(view -> addView(view));

        application.getApplicationViews().forEach(view -> {
            if (!isViewAttached(view)) {
                addView(view);
            }
        });
    }

    @Override
    public void handleLayoutChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, LayoutItemDefinition layout) {
        if (isActivePerspective) {
            currentLayout = layout;
            workSpaceLayout.applyLayoutDefinition(layout);
        }
    }

    @Override
    public void handleViewAdded(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view) {
        if (isActivePerspective) {
            addView(view);
        }
    }

    @Override
    public void handleViewRemoved(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view) {
        if (isActivePerspective) {
            removeView(view);
        }
    }

    @Override
    public void handleViewVisibilityChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, boolean visible) {
        if (isActivePerspective) {
            if (visible) {
                addView(view);
            } else {
                removeView(view);
            }
        }
    }

    @Override
    public void handleViewFocusRequest(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, boolean ensureVisible) {
        if (isActivePerspective && ensureVisible) {
            //todo find view group and ensure it is visible
        }
    }

    @Override
    public void handleViewSizeChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, ViewSize viewSize) {
        if (isActivePerspective && viewSize != null) {
            WorkSpaceLayoutView layoutView = workSpaceLayout.getViewByPanel(view.getPanel());
            if (layoutView != null) {
                if (viewSize.isWidthAvailable()) {
                    if (viewSize.getRelativeWidth() != null) {
                        layoutView.setRelativeWidth(viewSize.getRelativeWidth());
                    } else {
                        layoutView.setAbsoluteWidth(viewSize.getAbsoluteWidth());
                    }
                }
                if (viewSize.isHeightAvailable()) {
                    if (viewSize.getRelativeHeight() != null) {
                        layoutView.setRelativeHeight(viewSize.getRelativeHeight());
                    } else {
                        layoutView.setAbsoluteHeight(viewSize.getAbsoluteHeight());
                    }
                }
            }
        }
    }

    @Override
    public void handleViewLayoutPositionChange(ResponsiveApplication application, boolean isActivePerspective, Perspective perspective, View view, String position) {
        if (isActivePerspective) {
            WorkSpaceLayoutView layoutView = workSpaceLayout.getViewByPanel(view.getPanel());
            layoutView.remove();
            addView(view);
        }
    }



}