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

public class LightUIDropDownButton extends LightUIElement {
    String title = null;
    LinkedHashMap<String, LightUIElement> tableActions = new LinkedHashMap<String, LightUIElement>();

    public LightUIDropDownButton(final String id, final String title) {
        super();
        this.setType(TYPE_DROPDOWN_BUTTON);
        this.setId(id);
        this.title = title;
    }

    // Init from json constructor
    public LightUIDropDownButton(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUIDropDownButton(final LightUIDropDownButton dropDownElement) {
        super(dropDownElement);
        this.title = dropDownElement.title;
        this.tableActions = dropDownElement.tableActions;
    }

    public void addAction(final String id, final LightUIElement button) {
        this.tableActions.put(id, button);
    }

    @Override
    public LightUIElement clone() {
        return new LightUIDropDownButton(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("title", this.title);
        final JSONArray jsonTableActions = new JSONArray();
        for (Entry<String, LightUIElement> action : this.tableActions.entrySet()) {
            final JSONObject jsonAction = new JSONObject();
            jsonAction.put("id", action.getKey());
            jsonAction.put("button", action.getValue().toJSON());
            jsonTableActions.add(jsonAction);
        }
        json.put("table-actions", jsonTableActions);

        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.title = (String) JSONConverter.getParameterFromJSON(json, "title", String.class);
        final JSONArray jsonTabs = (JSONArray) JSONConverter.getParameterFromJSON(json, "table-actions", JSONArray.class);
        this.tableActions.clear();
        if (jsonTabs != null) {
            for (final Object o : jsonTabs) {
                if (!(o instanceof JSONObject)) {
                    throw new IllegalArgumentException("invalid value for 'table-actions', Map<String, LightUIElement> expected");
                }
                final JSONObject jsonTableAction = (JSONObject) o;
                final String id = (String) JSONConverter.getParameterFromJSON(jsonTableAction, "id", String.class);
                final LightUIElement button = new LightUIElement((JSONObject) JSONConverter.getParameterFromJSON(jsonTableAction, "button", JSONObject.class));
                this.tableActions.put(id, button);
            }
        }
    }
}
