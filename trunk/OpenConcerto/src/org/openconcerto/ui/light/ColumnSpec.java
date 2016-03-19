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

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;
import org.openconcerto.utils.ui.StringWithId;
import net.minidev.json.JSONObject;

public class ColumnSpec implements Externalizable, Transferable {
    // Must stay immutable

    private String id;
    private Class<?> valueClass;
    private String columnName;
    // Default value (to add a new line)
    private Object defaultValue;
    private int width;
    private int maxWidth;
    private int minWidth;
    private boolean editable;
    private LightUIElement editors;

    public ColumnSpec() {
        // Serialization
    }

    public ColumnSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public ColumnSpec(final String id, final Class<?> valueClass, final String columnName, final Object defaultValue, final int width, final boolean editable, final LightUIElement editors) {
        this.init(id, valueClass, columnName, defaultValue, editable, editors);
        this.width = width;

        final int minWidth = width - 200;
        final int maxWidth = width + 200;

        this.minWidth = (minWidth < 0) ? 0 : minWidth;
        this.maxWidth = maxWidth;
    }

    public ColumnSpec(final String id, final Class<?> valueClass, final String columnName, final Object defaultValue, final boolean editable, final LightUIElement editors) {
        this.init(id, valueClass, columnName, defaultValue, editable, editors);
        this.setDefaultPrefs();
    }

    private void init(final String id, final Class<?> valueClass, final String columnName, final Object defaultValue, final boolean editable, final LightUIElement editors) {
        this.id = id;
        this.valueClass = valueClass;
        this.columnName = columnName;
        this.defaultValue = defaultValue;
        this.editable = editable;
        this.editors = editors;
    }

    public void setPrefs(final int width, final int maxWidth, final int minWidth) {
        this.width = width;
        this.maxWidth = maxWidth;
        this.minWidth = minWidth;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Class<?> getValueClass() {
        return this.valueClass;
    }

    public void setValueClass(Class<?> valueClass) {
        this.valueClass = valueClass;
    }

    public String getColumnName() {
        return this.columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public Object getDefaultValue() {
        return this.defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public int getMaxWidth() {
        return this.maxWidth;
    }
    
    public void setMaxWidth(final int maxWidth) {
        this.maxWidth = maxWidth;
    }

    public int getMinWidth() {
        return this.minWidth;
    }
    
    public void setMinWidth(final int minWidth) {
        this.minWidth = minWidth;
    }

    public int getWidth() {
        return this.width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    public LightUIElement getEditor() {
        return this.editors;
    }

    public void setEditors(LightUIElement editors) {
        this.editors = editors;
    }

    private void setDefaultPrefs() {
        // TODO : Faire varier en fonction du type;
        this.width = 200;
        this.maxWidth = 500;
        this.minWidth = 50;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.id);
        out.writeUTF(this.columnName);
        out.writeInt(this.width);
        out.writeInt(this.maxWidth);
        out.writeInt(this.minWidth);
        out.writeObject(this.defaultValue);
        out.writeBoolean(this.editable);
        out.writeObject(this.editors);
        out.writeObject(this.valueClass);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readUTF();
        this.columnName = in.readUTF();
        this.width = in.readInt();
        this.maxWidth = in.readInt();
        this.minWidth = in.readInt();
        this.defaultValue = in.readObject();
        this.editable = in.readBoolean();
        this.editors = (LightUIElement) in.readObject();
        this.valueClass = (Class<?>) in.readObject();
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "ColumnSpec");
        result.put("id", this.id);
        result.put("column-name", this.columnName);
        result.put("width", this.width);
        result.put("max-width", this.maxWidth);
        result.put("min-width", this.minWidth);
        if(this.defaultValue != null) {
            result.put("default-value", JSONConverter.getJSON(this.defaultValue));
        }
        if (this.editable) {
            result.put("editable", true);
        }
        if(this.editors != null) {
            result.put("editors", JSONConverter.getJSON(this.editors));
        }
        result.put("value-class", JSONConverter.getJSON(this.valueClass));
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = (String) JSONConverter.getParameterFromJSON(json, "id", String.class);
        this.columnName = (String) JSONConverter.getParameterFromJSON(json, "column-name", String.class);
        this.width = (Integer) JSONConverter.getParameterFromJSON(json, "width", Integer.class);
        this.maxWidth = (Integer) JSONConverter.getParameterFromJSON(json, "max-width", Integer.class);
        this.minWidth = (Integer) JSONConverter.getParameterFromJSON(json, "min-width", Integer.class);
        this.editable = (Boolean) JSONConverter.getParameterFromJSON(json, "editable", Boolean.class, Boolean.FALSE);

        final JSONObject jsonDefaultValue = (JSONObject) JSONConverter.getParameterFromJSON(json, "default-value", JSONObject.class);
        if (jsonDefaultValue != null) {
            final String defaultValueClassName = (String) JSONConverter.getParameterFromJSON(jsonDefaultValue, "class", String.class);
            if (defaultValueClassName.equals(StringWithId.class.getSimpleName())) {
                this.defaultValue = new StringWithId(jsonDefaultValue);
            }
        }
        final JSONObject jsonEditors = (JSONObject) JSONConverter.getParameterFromJSON(json, "editors", JSONObject.class);
        if (jsonEditors != null) {
            this.editors = LightUIElement.createUIElementFromJSON(jsonEditors);
        }
        final String sValueClass = (String) JSONConverter.getParameterFromJSON(json, "value-class", String.class);
        if (sValueClass != null) {
            try {
                this.valueClass = Class.forName(sValueClass);
            } catch (Exception ex) {
                throw new IllegalArgumentException("invalid value for 'value-class', " + ex.getMessage());
            }
        }
    }
}
