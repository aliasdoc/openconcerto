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

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.ui.StringWithId;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUIList extends LightUIElement {
    private ArrayList<StringWithId> values = new ArrayList<StringWithId>();

    // Init from json constructor
    public LightUIList(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUIList(final LightUIList listElement) {
        super(listElement);
        this.values = listElement.values;
    }

    public LightUIList(String id, final ArrayList<StringWithId> values) {
        this.setId(id);
        this.setType(TYPE_LIST);
        this.values = values;
    }
    
    public ArrayList<StringWithId> getValues(){
        return (ArrayList<StringWithId>) this.values.clone();
    }

    @Override
    public LightUIElement clone() {
        return new LightUIList(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("class", "LightUIList");
        json.put("values", this.values);
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
                    throw new IllegalArgumentException("values of list must be json of StringWithId");
                }
                this.values.add(new StringWithId((JSONObject) jsonValue));
            }
        }
    }
}
