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

import java.util.List;

import org.openconcerto.utils.io.JSONConverter;
import net.minidev.json.JSONObject;

public class LightUITable extends LightUIElement {
    private Boolean verticallyScrollable = false;
    private TableSpec tableSpec = null;

    // Init from json constructor
    public LightUITable(final JSONObject json) {
        this.fromJSON(json);
    }
    // Clone constructor
    public LightUITable(final LightUITable tableElement) {
        super(tableElement);
        this.verticallyScrollable = tableElement.verticallyScrollable;
        this.tableSpec = tableElement.tableSpec;
    }
    
    public LightUITable(final String id) {
        this.setId(id);
        this.setType(LightUIElement.TYPE_TABLE);
        this.setFillWidth(true);
    }

    public TableSpec getTableSpec() {
        return this.tableSpec;
    }

    public void setTableSpec(final TableSpec tableSpec) {
        this.tableSpec = tableSpec;
    }
    
    public Boolean isVerticallyScrollable() {
        return this.verticallyScrollable;
    }
    
    public void setVerticallyScrollable(final Boolean verticallyScrollable) {
        this.verticallyScrollable = verticallyScrollable;
    }
    
    public LightUIElement getElementById(final String elementId) {
        if(this.tableSpec != null) {
            final TableContent content = this.tableSpec.getContent();
            if(content != null) {
                final List<Row> listRows = content.getRows();
                if(listRows != null && listRows.size() > 0) {
                    for(final Row row : listRows) {
                        final List<Object> rowValues = row.getValues();
                        for(final Object value : rowValues) {
                            if(value instanceof LightUIElement) {
                                final LightUIElement element = (LightUIElement) value;
                                if(element.getId().equals(elementId)) {
                                    return element;
                                }
                            }
                        }
                    }
                } else {
                    System.out.println("LightUITable.getElementById() - No rows for table: " + this.getId());
                }
            } else {
                System.out.println("LightUITable.getElementById() - Null TableContent for table: " + this.getId());
            }
        } else {
            System.out.println("LightUITable.getElementById() - Null TableSpec for table: " + this.getId());
        }
        return null;
    }

    @Override 
    public LightUIElement clone() {
        return new LightUITable(this);
    }
    
    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if(this.verticallyScrollable) {
            json.put("vertically-scrollable", true);
        }
        if(this.tableSpec != null) {
            json.put("table-spec", this.tableSpec.toJSON());
        }
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.verticallyScrollable = (Boolean) JSONConverter.getParameterFromJSON(json, "vertically-scrollable", Boolean.class, false);
        
        final JSONObject jsonRawContent = (JSONObject) JSONConverter.getParameterFromJSON(json, "table-spec", JSONObject.class);
        if (jsonRawContent != null) {
            this.tableSpec = new TableSpec(jsonRawContent);
        }
    }
}
