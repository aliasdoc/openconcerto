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
import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class TableContent implements Transferable {
    private static final long serialVersionUID = 3648381615123520834L;
    private String tableId;
    private List<Row> rows;

    public TableContent() {
        // Serialization
    }

    public TableContent(final String tableId) {
        this.init(tableId, null);
    }

    public TableContent(final String tableId, final List<Row> rows) {
        this.init(tableId, rows);
    }

    public TableContent(final JSONObject json) {
        this.fromJSON(json);
    }

    private void init(final String tableId, final List<Row> rows) {
        this.tableId = tableId;
        if (rows != null) {
            this.rows = rows;
        } else {
            this.rows = new ArrayList<Row>();
        }
    }

    public String getTableId() {
        return this.tableId;
    }

    public void setTableId(final String tableId) {
        this.tableId = tableId;
    }

    public List<Row> getRows() {
        return this.rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        return "TableContent of " + this.tableId + " lines count : " + getRows().size();
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "TableContent");
        result.put("table-id", this.tableId);
        result.put("rows", JSONConverter.getJSON(this.rows));
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.tableId = (String) JSONConverter.getParameterFromJSON(json, "table-id", String.class);
        final JSONArray jsonRows = (JSONArray) JSONConverter.getParameterFromJSON(json, "rows", JSONArray.class);
        if (jsonRows != null) {
            this.rows = new ArrayList<Row>();
            for (final Object o : jsonRows) {
                this.rows.add(new Row((JSONObject) JSONConverter.getObjectFromJSON(o, JSONObject.class)));
            }
        }
    }
}
