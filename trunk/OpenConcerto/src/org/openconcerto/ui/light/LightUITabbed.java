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

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.openconcerto.utils.io.JSONConverter;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUITabbed extends LightUIElement {
    LinkedHashMap<String, LightUIPanel> tabs = new LinkedHashMap<String, LightUIPanel>();
    String selectedTab;
    
    // Init from json constructor
    public LightUITabbed(final JSONObject json) {
        this.fromJSON(json);
    }
    // Clone constructor
    public LightUITabbed(final LightUITabbed tabbedElement) {
        super(tabbedElement);
        this.tabs = tabbedElement.tabs;
        this.selectedTab = tabbedElement.selectedTab;
    }
    
    public LightUITabbed(final String id) {
        super();
        
        this.setId(id);
        this.setType(TYPE_TABBED_UI);
    }
    
    public LightUIPanel getTab(final String title) {
        return this.tabs.get(title);
    }
    
    public LightUIPanel getTab(final int index) {
        final Object o = this.tabs.values().toArray()[index];
        if(!(o instanceof LightUIPanel)) {
            throw new IllegalArgumentException("the value at " + String.valueOf(index) + " is not valid");
        }
        return (LightUIPanel) o;
    }
    
    public void addTab(final String title, final LightUIPanel panel) {
        this.tabs.put(title, panel);
    }
    
    public void setSelectedTab(final String selectedTab) {
        this.selectedTab = selectedTab;
    }
    
    public int getTabsCount() {
        return this.tabs.size();
    }
    
    public LightUIElement getElementById(final String id) {
        for(final LightUIPanel tab : this.tabs.values()) {
            final LightUIElement result = tab.getElementById(id);
            if(result != null) {
                return result;
            }
        }
        return null;
    }
    
    @Override 
    public LightUIElement clone() {
        return new LightUITabbed(this);
    }
    
    @Override public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("selected-tab", this.selectedTab);
        
        final JSONArray jsonTabs = new JSONArray();
        for(final Entry<String, LightUIPanel> entry : this.tabs.entrySet()) {
            final JSONObject jsonTab = new JSONObject();
            jsonTab.put("title", entry.getKey());
            jsonTab.put("panel", entry.getValue().toJSON());
            jsonTabs.add(jsonTab);
        }
        
        json.put("tabs", jsonTabs);
        
        return json;
    }
    
    @Override public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.selectedTab = (String) JSONConverter.getParameterFromJSON(json, "selected-tab", String.class);
        
        final JSONArray jsonTabs = (JSONArray) JSONConverter.getParameterFromJSON(json, "tabs", JSONArray.class);
        this.tabs.clear();
        if(jsonTabs != null) {
            for(final Object objTab : jsonTabs) {
                if(!(objTab instanceof JSONObject)) {
                    throw new IllegalArgumentException("value for 'selected-tab' is invalid");
                }
                final JSONObject jsonTab = (JSONObject) objTab;
                final String title = (String) JSONConverter.getParameterFromJSON(jsonTab, "title", String.class);
                final LightUIPanel panel = new LightUIPanel((JSONObject) JSONConverter.getParameterFromJSON(jsonTab, "panel", JSONObject.class));
                this.tabs.put(title, panel);
            }
        }
    }
}
