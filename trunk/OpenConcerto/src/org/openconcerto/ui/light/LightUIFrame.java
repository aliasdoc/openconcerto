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
import net.minidev.json.JSONObject;

public class LightUIFrame extends LightUIElement {
    LightUIPanel mainPanel;
    Boolean active = false;
    String title;

    // Init from json constructor
    public LightUIFrame(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUIFrame(final LightUIFrame frame) {
        super(frame);
        this.mainPanel = frame.mainPanel;
        this.active = frame.active;
        this.title = frame.title;
    }

    public LightUIFrame(final String id) {
        this.setId(id);
        this.setType(TYPE_FRAME);
    }

    public LightUIPanel getMainPanel() {
        return this.mainPanel;
    }
    
    public void setMainPanel(final LightUIPanel mainPanel) {
        this.mainPanel = mainPanel;
    }
    
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(final String title) {
        this.title = title;
    }
    
    public boolean isActive() {
        return this.active;
    }
    
    public void setActive(final boolean active) {
        this.active = active;
    }
    
    public LightUIElement getElementById(final String elementId) {
        return this.mainPanel.getElementById(elementId);
    }

    @Override
    public LightUIElement clone() {
        return new LightUIFrame(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("main-panel", this.mainPanel.toJSON());
        if(this.active) {
            json.put("active", true);
        }
        if(this.title != null) {
            json.put("title", this.title);
        }
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        final JSONObject jsonMainPanel = (JSONObject) JSONConverter.getParameterFromJSON(json, "main-panel", JSONObject.class, null);
        if(jsonMainPanel != null) {
            this.mainPanel = new LightUIPanel(jsonMainPanel);
        }
        this.active = (Boolean)JSONConverter.getParameterFromJSON(json, "active", Boolean.class, false);
        this.title = (String)JSONConverter.getParameterFromJSON(json, "title", String.class);
    }
}
