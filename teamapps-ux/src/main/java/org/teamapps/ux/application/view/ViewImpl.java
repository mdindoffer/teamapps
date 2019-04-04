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
package org.teamapps.ux.application.view;

import org.teamapps.icons.api.Icon;
import org.teamapps.ux.component.Component;
import org.teamapps.ux.component.panel.Panel;
import org.teamapps.ux.component.toolbar.Toolbar;
import org.teamapps.ux.component.toolbar.ToolbarButtonGroup;

import java.util.ArrayList;
import java.util.List;

public class ViewImpl implements View {


    private boolean visible = true;
    private String layoutPosition;
    private ViewSize viewSize;


    private Panel panel = new Panel();
    private Toolbar toolbar;
    private List<ToolbarButtonGroup> workspaceToolbarButtonGroups = new ArrayList<>();
    private List<ViewChangeHandler> changeHandlers = new ArrayList<>();

    public ViewImpl(String layoutPosition) {
        this.layoutPosition = layoutPosition;
    }

    public ViewImpl(String layoutPosition, Icon icon, String title, Component component) {
        this.layoutPosition = layoutPosition;
        panel.setIcon(icon);
        panel.setTitle(title);
        panel.setContent(component);
    }


    @Override
    public void addViewChangeHandler(ViewChangeHandler viewChangeHandler) {
        changeHandlers.add(viewChangeHandler);
    }

    @Override
    public void removeViewChangeHandler(ViewChangeHandler viewChangeHandler) {
        changeHandlers.remove(viewChangeHandler);
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
        changeHandlers.forEach(changeHandler -> changeHandler.handleVisibilityChange(visible));
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void focus() {
        focus(false);
    }

    @Override
    public void focus(boolean ensureVisible) {
        changeHandlers.forEach(changeHandler -> changeHandler.handleViewFocusRequest(ensureVisible));
    }

    @Override
    public ViewSize getCustomViewSize() {
        return viewSize;
    }

    @Override
    public void setSize(ViewSize viewSize) {
        this.viewSize = viewSize;
        changeHandlers.forEach(changeHandler -> changeHandler.handleViewSizeChange(viewSize));
    }

    @Override
    public void addLocalButtonGroup(ToolbarButtonGroup buttonGroup) {
        checkToolbar();
        toolbar.addButtonGroup(buttonGroup);
    }

    @Override
    public void removeLocalButtonGroup(ToolbarButtonGroup buttonGroup) {
        toolbar.removeToolbarButtonGroup(buttonGroup);
    }

    @Override
    public List<ToolbarButtonGroup> getLocalButtonGroups() {
        return toolbar.getToolbarButtonGroups();
    }

    private void checkToolbar() {
        if (toolbar == null) {
            toolbar = new Toolbar();
            panel.setToolbar(toolbar);
        }
    }

    @Override
    public void addWorkspaceButtonGroup(ToolbarButtonGroup buttonGroup) {
        workspaceToolbarButtonGroups.add(buttonGroup);
        changeHandlers.forEach(changeHandler -> changeHandler.handleWorkspaceButtonGroupAdded(buttonGroup));
    }

    @Override
    public void removeWorkspaceButtonGroup(ToolbarButtonGroup buttonGroup) {
        workspaceToolbarButtonGroups.remove(buttonGroup);
        changeHandlers.forEach(changeHandler -> changeHandler.handleWorkspaceButtonGroupRemoved(buttonGroup));
    }

    @Override
    public List<ToolbarButtonGroup> getWorkspaceButtonGroups() {
        return workspaceToolbarButtonGroups;
    }

    @Override
    public void setComponent(Component component) {
        panel.setContent(component);
    }

    @Override
    public Component getComponent() {
        return panel.getContent();
    }

    @Override
    public Panel getPanel() {
        return panel;
    }

    @Override
    public void setLayoutPosition(String position) {
        this.layoutPosition = position;
        changeHandlers.forEach(changeHandler -> changeHandler.handleLayoutPositionChange(position));
    }

    @Override
    public String getLayoutPosition() {
        return layoutPosition;
    }
}