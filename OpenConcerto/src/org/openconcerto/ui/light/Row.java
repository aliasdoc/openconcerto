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
import java.util.ArrayList;
import java.util.List;

public class Row implements Externalizable, JSONAble {

    private long id;
    private List<Object> values;

    public Row() {
        // Serialization
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

    @Override
    public String toString() {
        return "Row id: " + this.id + " values: " + this.values;
    }

    public void addValue(Object v) {
        this.values.add(v);

    }

    public void setValue(int index, Object v) {
        this.values.set(index, v);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(this.id);
        out.writeObject(this.values);

    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readLong();
        this.values = (List<Object>) in.readObject();
    }

    @Override
    public String toJSON() {
        final StringBuilder result = new StringBuilder("{");
        
        result.append("\"id\":" + JSONconverter.getJSON(this.id) + ",");
        result.append("\"values\":" + JSONconverter.getJSON(this.values));
        
        result.append("}");
        return result.toString();
    }

}
