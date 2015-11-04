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

import org.openconcerto.utils.io.JSONAble;
import org.openconcerto.utils.io.JSONconverter;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class RowSpec implements Externalizable, JSONAble {
    private String tableId;
    private String[] columnIds;

    public RowSpec() {
        // Serialization
    }

    public RowSpec(String tableId, String[] columnIds) {
        this.tableId = tableId;
        this.columnIds = columnIds;
    }

    public String[] getIds() {
        return this.columnIds;
    }

    public void setIds(String[] columnIds) {
        this.columnIds = columnIds;
    }

    public void setTableId(String tableId) {
        this.tableId = tableId;
    }

    public String getTableId() {
        return this.tableId;
    }

    @Override
    public String toString() {
        String r = "RowSpec:" + this.tableId + " : ";
        for (int i = 0; i < this.columnIds.length; i++) {
            if (i < this.columnIds.length - 1) {
                r += this.columnIds[i] + ", ";
            } else {
                r += this.columnIds[i];
            }
        }
        return r;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        try {
            out.writeUTF(this.tableId);
            out.writeObject(this.columnIds);

        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException(e);
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.tableId = in.readUTF();
        this.columnIds = (String[]) in.readObject();

    }

    @Override
    public String toJSON() {
        final StringBuilder result = new StringBuilder("{");
        
        result.append("\"tableId\":" + JSONconverter.getJSON(this.tableId) + ",");
        result.append("\"columnIds\":" + JSONconverter.getJSON(this.columnIds));

        result.append("}");
        return result.toString();
    }

}
