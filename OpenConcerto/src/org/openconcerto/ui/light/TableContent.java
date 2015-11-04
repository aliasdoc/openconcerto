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

import org.openconcerto.utils.io.JSONconverter;
import org.openconcerto.utils.io.Transferable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TableContent implements Transferable {
    private static final long serialVersionUID = 3648381615123520834L;
    private List<Row> rows;
    private RowSpec spec;

    public TableContent() {
        // Serialization
    }

    public List<Row> getRows() {
        return this.rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public RowSpec getSpec() {
        return this.spec;
    }

    public void setSpec(RowSpec spec) {
        this.spec = spec;
    }

    @Override
    public String toString() {
        return "TableContent of " + this.spec.getTableId() + " columns: " + new ArrayList<String>(Arrays.asList(this.spec.getIds())) + " : " + getRows().size() + " lines";
    }

    @Override
    public String toJSON() {
        final StringBuilder result = new StringBuilder("{");

        result.append("\"rows\":" + JSONconverter.getJSON(this.rows) + ",");
        result.append("\"spec\":" + JSONconverter.getJSON(this.spec));

        result.append("}");
        return result.toString();
    }
}
