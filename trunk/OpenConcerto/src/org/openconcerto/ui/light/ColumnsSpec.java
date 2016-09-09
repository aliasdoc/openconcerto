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

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class ColumnsSpec implements Externalizable, Transferable {
    private String id;
    // All the columns that could be displayed
    private List<ColumnSpec> columns = new ArrayList<ColumnSpec>();
    // Ids visible in the table, in the same order of the display
    private List<String> possibleColumnIds = new ArrayList<String>();
    // Ids of the sorted columns
    private List<String> sortedIds = new ArrayList<String>();
    // number of fixed columns, used for ve rtical "split"
    private int fixedColumns;

    private Boolean adaptWidth = false;
    private Boolean allowMove = false;
    private Boolean allowResize = false;

    public ColumnsSpec() {
        // Serialization
    }

    public ColumnsSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public ColumnsSpec(final String id, final List<ColumnSpec> columns, final List<String> possibleColumnIds, final List<String> sortedIds) throws IllegalArgumentException {
        // Id checks
        if (id == null) {
            throw new IllegalArgumentException("null id");
        }
        this.id = id;

        // Columns checks
        if (columns == null) {
            throw new IllegalArgumentException("null columns");
        }
        if (columns.isEmpty()) {
            throw new IllegalArgumentException("empty columns");
        }
        this.columns = columns;

        // Possible checks
        if (possibleColumnIds == null) {
            throw new IllegalArgumentException("null possible column ids");
        }
        if (possibleColumnIds.isEmpty()) {
            throw new IllegalArgumentException("empty possible column ids");
        }
        this.possibleColumnIds = possibleColumnIds;

        // Sort assign
        this.sortedIds = sortedIds;

    }

    public String getId() {
        return this.id;
    }

    public List<String> getPossibleColumnIds() {
        return this.possibleColumnIds;
    }

    public List<String> getSortedIds() {
        return this.sortedIds;
    }

    public int getFixedColumns() {
        return this.fixedColumns;

    }

    public int getColumnCount() {
        return this.columns.size();

    }

    public ColumnSpec getColumn(int i) {
        return this.columns.get(i);
    }

    public ColumnSpec setColumn(int i, final ColumnSpec column) {
        return this.columns.set(i, column);
    }

    public Boolean isAdaptWidth() {
        return this.adaptWidth;
    }

    public void setAdaptWidth(final boolean adaptWidth) {
        this.adaptWidth = adaptWidth;
    }

    public Boolean isAllowMove() {
        return this.allowMove;
    }

    public void setAllowMove(final boolean allowMove) {
        this.allowMove = allowMove;
    }

    public Boolean isAllowResize() {
        return this.allowResize;
    }

    public void setAllowResize(final boolean allowResize) {
        this.allowResize = allowResize;
    }

    public List<String> getColumnsIds() {
        ArrayList<String> result = new ArrayList<String>(this.columns.size());
        for (ColumnSpec c : this.columns) {
            result.add(c.getId());
        }
        return result;
    }

    public void setUserPrefs(final Document columnsPrefs) {

        if (columnsPrefs != null) {
            // user preferences application
            final Element rootElement = columnsPrefs.getRootElement();
            if (!rootElement.getName().equals("list")) {
                throw new IllegalArgumentException("invalid xml, roots node list expected but " + rootElement.getName() + " found");
            }
            final List<Element> xmlColumns = rootElement.getChildren();
            final int columnsCount = this.columns.size();
            if (xmlColumns.size() == columnsCount) {
                for (int i = 0; i < columnsCount; i++) {
                    final ColumnSpec columnSpec = this.columns.get(i);
                    final String columnId = columnSpec.getId();
                    boolean find = false;

                    for (int j = 0; j < columnsCount; j++) {
                        final Element xmlColumn = xmlColumns.get(j);
                        final String xmlColumnId = xmlColumn.getAttribute("id").getValue();

                        if (xmlColumnId.equals(columnId)) {

                            if (!xmlColumn.getName().equals("column")) {
                                throw new IllegalArgumentException("ColumnSpec setPrefs - Invalid xml, element node column expected but " + xmlColumn.getName() + " found");
                            }
                            if (xmlColumn.getAttribute("width") == null || xmlColumn.getAttribute("min-width") == null || xmlColumn.getAttribute("max-width") == null) {
                                throw new IllegalArgumentException("ColumnSpec setPrefs - Invalid column node for " + columnId + ", it must have attribute width, min-width, max-width");
                            }

                            final int width = Integer.parseInt(xmlColumn.getAttribute("width").getValue());
                            final int maxWidth = Integer.parseInt(xmlColumn.getAttribute("max-width").getValue());
                            final int minWidth = Integer.parseInt(xmlColumn.getAttribute("min-width").getValue());

                            columnSpec.setPrefs(width, maxWidth, minWidth);
                            if (i != j) {
                                final ColumnSpec swap = this.columns.get(i);
                                this.columns.set(i, this.columns.get(j));
                                this.columns.set(j, swap);
                            }
                            find = true;
                            break;
                        }
                    }
                    if (!find) {
                        System.out.println("XML columns preferences does'nt contain this column: " + columnId);
                    }
                }
            } else {
                System.out.println("ColumnsSpec.setUserPrefs() - Incorrect columns count in XML for ColumnsSpec: " + this.id);
            }
        }
    }

    public Document createDefaultXmlPref() {
        final Element rootElement = new Element("list");

        for (final ColumnSpec column : this.columns) {
            final Element columnElement = column.createXmlColumnPref();
            rootElement.addContent(columnElement);
        }
        final Document xmlConf = new Document(rootElement);

        return xmlConf;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(this.id);
        out.writeInt(this.fixedColumns);
        out.writeObject(this.columns);
        out.writeObject(this.possibleColumnIds);
        out.writeObject(this.sortedIds);
        out.writeBoolean(this.allowMove);
        out.writeBoolean(this.allowResize);
        out.writeBoolean(this.adaptWidth);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.id = in.readUTF();
        this.fixedColumns = in.readInt();
        this.columns = (List<ColumnSpec>) in.readObject();
        this.possibleColumnIds = (List<String>) in.readObject();
        this.sortedIds = (List<String>) in.readObject();
        this.allowMove = in.readBoolean();
        this.allowResize = in.readBoolean();
        this.adaptWidth = in.readBoolean();
    }

    public List<Object> getDefaultValues() {
        final List<Object> l = new ArrayList<Object>();
        for (ColumnSpec column : this.columns) {
            final Object v = column.getDefaultValue();
            l.add(v);
        }
        return l;
    }

    public ColumnSpec getColumn(String id) {
        for (ColumnSpec c : this.columns) {
            if (c.getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    public ColumnSpec getColumnWithEditor(String id) {
        for (ColumnSpec c : this.columns) {
            LightUIElement editor = c.getEditor();
            if (editor != null && c.getEditor().getId().equals(id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "ColumnsSpec");
        result.put("id", this.id);
        result.put("fixed-columns", this.fixedColumns);
        if (this.sortedIds != null && this.sortedIds.size() > 0) {
            result.put("sorted-ids", this.sortedIds);
        }
        if (this.possibleColumnIds != null && this.possibleColumnIds.size() > 0) {
            result.put("possible-column-ids", this.possibleColumnIds);
        }
        if (this.columns != null && this.columns.size() > 0) {
            result.put("columns", JSONConverter.getJSON(this.columns));
        }
        if (this.adaptWidth) {
            result.put("adapt-width", JSONConverter.getJSON(true));
        }
        if (this.allowMove) {
            result.put("allow-move", JSONConverter.getJSON(true));
        }
        if (this.allowResize) {
            result.put("allow-resize", JSONConverter.getJSON(true));
        }
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = (String) JSONConverter.getParameterFromJSON(json, "id", String.class);
        this.fixedColumns = (Integer) JSONConverter.getParameterFromJSON(json, "fixed-columns", Integer.class);
        this.adaptWidth = (Boolean) JSONConverter.getParameterFromJSON(json, "adapt-width", Boolean.class, false);
        this.allowMove = (Boolean) JSONConverter.getParameterFromJSON(json, "allow-move", Boolean.class, false);
        this.allowResize = (Boolean) JSONConverter.getParameterFromJSON(json, "allow-resize", Boolean.class, false);
        final JSONArray jsonSortedIds = (JSONArray) JSONConverter.getParameterFromJSON(json, "sorted-ids", JSONArray.class, null);
        if (jsonSortedIds != null) {
            this.sortedIds = (List<String>) (List<?>) jsonSortedIds;
        }
        final JSONArray jsonPossibleColumnIds = (JSONArray) JSONConverter.getParameterFromJSON(json, "possible-column-ids", JSONArray.class, null);
        if (jsonPossibleColumnIds != null) {
            this.possibleColumnIds = (List<String>) (List<?>) jsonPossibleColumnIds;
        }

        final JSONArray jsonColumns = (JSONArray) JSONConverter.getParameterFromJSON(json, "columns", JSONArray.class, null);
        if (jsonColumns != null) {
            final int columnsSize = jsonColumns.size();
            for (int i = 0; i < columnsSize; i++) {
                final Object objColumnSpec = jsonColumns.get(i);
                if (!(objColumnSpec instanceof JSONObject)) {
                    throw new IllegalArgumentException("invalid value for 'columns', List<ColumnSpec> expected");
                }
                this.columns.add(new ColumnSpec((JSONObject) objColumnSpec));
            }
        }
    }
}
