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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.openconcerto.utils.io.JSONAble;
import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.ui.StringWithId;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class Row implements Externalizable, JSONAble {

    private long id;
    private List<Object> values;
    
    private Boolean fillWidth = false;
    private Boolean toggleable = false;
    private Boolean visible = true;

    public Row() {
        // Serialization
    }

    public Row(final JSONObject json) {
        this.fromJSON(json);
    }

    public Row(long id, int valueCount) {
        this.id = id;
        if (valueCount > 0)
            this.values = new ArrayList<Object>(valueCount);
    }

    public Row(long id) {
        this.id = id;
    }

    public void setValues(List<Object> values) {
        this.values = values;
    }

    public List<Object> getValues() {
        return this.values;
    }

    public long getId() {
        return this.id;
    }

    public void addValue(Object v) {
        this.values.add(v);
    }

    public void setValue(int index, Object v) {
        this.values.set(index, v);
    }
    
    public Boolean isFillWidth() {
        return this.fillWidth;
    }

    public void setFillWidth(final Boolean fillWidth) {
        this.fillWidth = fillWidth;
    }
    
    public Boolean isToggleable() {
        return this.toggleable;
    }

    public void setToggleable(final Boolean toggleable) {
        this.toggleable = toggleable;
    }
    
    public Boolean isVisible() {
        return this.visible;
    }

    public void setVisible(final Boolean visible) {
        this.visible = visible;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(this.id);
        out.writeObject(this.values);
        out.writeBoolean(this.fillWidth);
        out.writeBoolean(this.toggleable);
        out.writeBoolean(this.visible);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readLong();
        this.values = (List<Object>) in.readObject();
        this.fillWidth = in.readBoolean();
        this.toggleable = in.readBoolean();
        this.visible = in.readBoolean();
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();

        result.put("class", "Row");
        result.put("id", this.id);
        if(!this.values.isEmpty()) {
            result.put("values", JSONConverter.getJSON(this.values));
        }
        if(this.fillWidth) {
            result.put("fill-width", true);
        }
        if(this.toggleable) {
            result.put("toggleable", true);
        }
        if(!this.visible) {
            result.put("visible", false);
        }
        
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = (Long) JSONConverter.getParameterFromJSON(json, "id", Long.class);
        this.fillWidth = (Boolean) JSONConverter.getParameterFromJSON(json, "fill-width", Boolean.class, false);
        this.toggleable = (Boolean) JSONConverter.getParameterFromJSON(json, "toggleable", Boolean.class, false);
        this.visible = (Boolean) JSONConverter.getParameterFromJSON(json, "visible", Boolean.class, true);

        final JSONArray jsonValues = (JSONArray) JSONConverter.getParameterFromJSON(json, "values", JSONArray.class);
        if (jsonValues != null) {
            final int valuesSize = jsonValues.size();
            this.values = new ArrayList<Object>(valuesSize);
            for (int i = 0; i < valuesSize; i++) {
                Object objValue = jsonValues.get(i);
                if (objValue instanceof JSONObject) {
                    final JSONObject jsonValue = (JSONObject) objValue;
                    final String valueClassName = (String) JSONConverter.getParameterFromJSON(jsonValue, "class", String.class);
                    if (valueClassName == null){
                        throw new IllegalArgumentException("null value store in ghost");
                    }
                    if(valueClassName.equals(StringWithId.class.getSimpleName())) {
                        objValue = new StringWithId(jsonValue);
                    } else if (valueClassName.equals(LightUIElement.class.getSimpleName())) {
                        objValue = LightUIElement.createUIElementFromJSON(jsonValue);
                    } else {
                        throw new IllegalArgumentException("invalid value for 'values', StringWithId or LightUIElement expected");
                    }
                } else {
                    if(objValue instanceof String) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, String.class);
                    } else if(objValue instanceof Integer) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, Integer.class);
                    } else if(objValue instanceof Long) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, Long.class);
                    } else if(objValue instanceof Boolean) {
                        objValue = JSONConverter.getObjectFromJSON(objValue, Boolean.class);
                    } else if(objValue != null) {
                        throw new IllegalArgumentException("unknow type: " + objValue.getClass().getName());
                    }
                }
                this.values.add(objValue);
            }
        }
    }
    
    @Override
    public String toString() {
        return "Row id: " + this.id + " values: " + this.values;
    }
}
