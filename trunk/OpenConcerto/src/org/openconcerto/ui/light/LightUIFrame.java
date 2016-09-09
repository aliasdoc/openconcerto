/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.ui.light;

import org.openconcerto.utils.io.JSONConverter;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUIFrame extends LightUIContainer {

    private Boolean active = false;
    private String title;
    private final LightUIPanel footerPanel = new LightUIPanel(this.getId() + ".footer.panel");

    private List<LightUIFrame> childrenFrame;

    // Init from json constructor
    public LightUIFrame(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUIFrame(final LightUIFrame frame) {
        super(frame);
        this.active = frame.active;
        this.title = frame.title;
        this.footerPanel.copy(frame.footerPanel);
        this.childrenFrame = frame.childrenFrame;
    }

    /**
     * Creation of an instance of a frame, this one is initialized with an empty main panel
     * 
     * @param id Id of the frame
     */
    public LightUIFrame(final String id) {
        super(id);
        this.setType(TYPE_FRAME);

        this.childrenFrame = new ArrayList<LightUIFrame>();
        this.addChild(new LightUIPanel(this.getId() + ".main.panel"));
        this.footerPanel.setParent(this);
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public LightUIPanel getFooterPanel() {
        return this.footerPanel;
    }

    public void updateFooterPanel(final LightUIPanel footerPanel) {
        if (footerPanel != null) {
            this.footerPanel.copy(footerPanel);
        } else {
            this.footerPanel.clear();
        }

    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }

    public String getPanelId() {
        return this.getId() + ".main.panel";
    }

    public void removeChildFrame(final LightUIFrame childFrame) {
        this.childrenFrame.remove(childFrame);
    }

    public void removeChildFrame(final int index) {
        this.childrenFrame.remove(index);
    }

    public void clearChildrenFrame() {
        this.childrenFrame.clear();
    }

    @Override
    /**
     * Only one panel is accepted into a frame. And it's Id is always : frame.getId() +
     * ".main.panel"
     * 
     * @param parent The parent frame of this one.
     * @throws InvalidClassException
     */
    public void setParent(final LightUIElement parent) {
        if (!(parent instanceof LightUIFrame)) {
            throw new InvalidClassException(LightUIFrame.class.getName(), parent.getClassName(), parent.getId());
        }
        super.setParent(parent);

        ((LightUIFrame) parent).childrenFrame.add(this);
    }

    @Override
    public <T extends LightUIElement> T findChild(String searchParam, boolean byUUID, Class<T> childClass) {
        final T result = super.findChild(searchParam, byUUID, childClass);
        if (result != null) {
            return result;
        } else {
            return this.footerPanel.findChild(searchParam, byUUID, childClass);
        }
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        super.setReadOnly(readOnly);
        this.footerPanel.setReadOnly(readOnly);
    }

    @Override
    /**
     * Only one panel is accepted into a frame. And it's Id is always : frame.getId() +
     * ".main.panel"
     * 
     * @param child The panel which will replace the main panel
     * @throws InvalidClassException
     */
    public void addChild(final LightUIElement child) throws InvalidClassException {
        if (!(child instanceof LightUIPanel)) {
            throw new InvalidClassException(LightUIPanel.class.getName(), child.getClassName(), child.getId());
        }
        child.setId(this.getPanelId());
        this.clear();
        super.addChild(child);
    }

    @Override
    /**
     * Only one panel is accepted into a frame. And it's Id is always : frame.getId() +
     * ".main.panel"
     * 
     * @param index No importance
     * @param child The panel which will replace the main panel
     * @throws InvalidClassException
     */
    public void insertChild(int index, LightUIElement child) throws InvalidClassException {
        if (!(child instanceof LightUIPanel)) {
            throw new InvalidClassException(LightUIPanel.class.getName(), child.getClassName(), child.getId());
        }
        child.setId(this.getPanelId());
        this.clear();
        super.insertChild(index, child);
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIFrame(json);
            }
        };
    }

    @Override
    public void dump(final PrintStream out, final int depth) {
        out.println("------------- LightUIFrame -------------");
        super.dump(out, 0);

        out.println("footer-panel: ");
        if (this.footerPanel != null) {
            this.footerPanel.dump(out, 0);
        } else {
            out.println("null");
        }

        out.println("--------------------------");
    }

    @Override
    public LightUIElement clone() {
        final LightUIFrame clone = new LightUIFrame(this);
        clone.getFooterPanel().setParent(clone);
        return clone;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (this.active) {
            json.put("active", true);
        }
        if (this.title != null) {
            json.put("title", this.title);
        }
        json.put("footer-panel", this.footerPanel.toJSON());
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.active = (Boolean) JSONConverter.getParameterFromJSON(json, "active", Boolean.class, false);
        this.title = (String) JSONConverter.getParameterFromJSON(json, "title", String.class);

        final JSONObject jsonFooterPanel = (JSONObject) JSONConverter.getParameterFromJSON(json, "footer-panel", JSONObject.class, null);
        if (jsonFooterPanel != null) {
            this.footerPanel.fromJSON(jsonFooterPanel);
        }

        final JSONArray jsonChildrenFrame = (JSONArray) JSONConverter.getParameterFromJSON(json, "children-frame", JSONArray.class, null);
        this.childrenFrame = new ArrayList<LightUIFrame>();
        if (jsonChildrenFrame != null) {
            for (final Object objJsonFrame : jsonChildrenFrame) {
                final JSONObject jsonFrame = JSONConverter.getObjectFromJSON(objJsonFrame, JSONObject.class);
                final LightUIFrame childFrame = new LightUIFrame(jsonFrame);
                this.childrenFrame.add(childFrame);
            }
        }
    }
}
