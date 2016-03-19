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

import java.util.ArrayList;
import java.util.List;

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.ui.StringWithId;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUICombo extends LightUIElement {
    List<StringWithId> values = new ArrayList<StringWithId>();

    // Init from json constructor
    public LightUICombo(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUICombo(final LightUICombo combo) {
        super(combo);
        this.values = combo.values;
    }

    public LightUICombo(String id) {
        this.setId(id);
        this.setType(TYPE_COMBOBOX);
    }

    public void addValue(final StringWithId values) {
        this.values.add(values);
    }

    public void addValues(final List<StringWithId> values) {
        this.values.addAll(values);
    }

    @Override
    public LightUIElement clone() {
        return new LightUICombo(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        final JSONArray jsonValues = new JSONArray();
        for (final StringWithId value : this.values) {
            jsonValues.add(value.toJSON());
        }
        json.put("values", jsonValues);

        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        
        final JSONArray jsonValues = (JSONArray) JSONConverter.getParameterFromJSON(json, "values", JSONArray.class);
        this.values.clear();
        if (jsonValues != null) {
            for (final Object jsonValue : jsonValues) {
                if (!(jsonValue instanceof JSONObject)) {
                    throw new IllegalArgumentException("value for 'values' is invalid");
                }
                this.values.add(new StringWithId((JSONObject) jsonValue));
            }
        }
    }
}
