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
import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONObject;

public class TableSpec implements Transferable {
    private String id;
    private ColumnsSpec columns;
    private TableContent content;
    private RowSelectionSpec selection;
    private SearchSpec search;

    public TableSpec(final String tableId, final RowSelectionSpec selection, final ColumnsSpec columns) {
        this.id = tableId + ".spec";
        this.selection = selection;
        this.columns = columns;
    }

    public TableSpec(final JSONObject json) {
        this.fromJSON(json);
    }

    public String getId() {
        return this.id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public ColumnsSpec getColumns() {
        return this.columns;
    }

    public void setColumns(final ColumnsSpec columns) {
        this.columns = columns;
    }

    public TableContent getContent() {
        return this.content;
    }

    public void setContent(final TableContent content) {
        this.content = content;
    }

    public RowSelectionSpec getSelection() {
        return this.selection;
    }

    public void setSelection(final RowSelectionSpec selection) {
        this.selection = selection;
    }

    public SearchSpec getSearch() {
        return this.search;
    }

    public void setSearch(final SearchSpec search) {
        this.search = search;
    }

    public void setRowEditorFromColumnSpec() {
        if (this.columns == null) {
            throw new IllegalArgumentException("ColumnsSpec must not be null for run thsi function");
        }

        if (this.content != null) {
            final int columnsSize = this.columns.getColumnCount();
            final List<Row> listRow = this.content.getRows();

            if (listRow != null && listRow.size() > 0) {
                final int listRowSize = listRow.size();
                for (int k = 0; k < columnsSize; k++) {
                    final ColumnSpec columnSpec = this.columns.getColumn(k);
                    final LightUIElement columnEditor = columnSpec.getEditor();

                    if (columnEditor != null) {
                        System.out.println("TableSpec.setRowEditorFromColumnSpec() - Editor found for table: " + this.id + " and column: " + columnSpec.getId());
                        for (int l = 0; l < listRowSize; l++) {
                            final Row row = listRow.get(l);
                            if (row == null) {
                                throw new IllegalArgumentException("Table: " + this.id + " has one or more rows null");
                            }
                            final List<Object> rowValues = row.getValues();
                            if (rowValues == null) {
                                throw new IllegalArgumentException("Table: " + this.id + " has null values for row " + row.getId());
                            }
                            if (rowValues.size() < k) {
                                throw new IllegalArgumentException("Table: " + this.id + " has incorrect values count for row " + row.getId());
                            }
                            final LightUIElement rowEditor = columnEditor.clone();
                            rowEditor.setId(columnEditor.getId() + "." + String.valueOf(row.getId()));

                            final Object value = rowValues.get(k);
                            if (value != null) {
                                rowEditor.setValue(rowValues.get(k).toString());
                            } else {
                                rowEditor.setValue(null);
                            }
                            rowValues.set(k, rowEditor);
                        }
                    }
                }
            } else {
                System.out.println("TableSpec.setRowEditorFromColumnSpec() - TableContent without Row for table: " + this.id);
            }
        } else {
            System.out.println("TableSpec.setRowEditorFromColumnSpec() - TableContent null for table: " + this.id);
        }
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "TableSpec");
        result.put("id", this.id);
        result.put("columns", JSONConverter.getJSON(this.columns));
        result.put("content", JSONConverter.getJSON(this.content));
        result.put("selection", JSONConverter.getJSON(this.selection));
        result.put("search", JSONConverter.getJSON(this.search));
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = (String) JSONConverter.getParameterFromJSON(json, "id", String.class);

        final JSONObject jsonColumns = (JSONObject) JSONConverter.getParameterFromJSON(json, "columns", JSONObject.class);
        if (jsonColumns != null) {
            this.columns = new ColumnsSpec(jsonColumns);
        }

        final JSONObject jsonContent = (JSONObject) JSONConverter.getParameterFromJSON(json, "content", JSONObject.class);
        if (jsonContent != null) {
            this.content = new TableContent(jsonContent);
        }
        final JSONObject jsonSelection = (JSONObject) JSONConverter.getParameterFromJSON(json, "selection", JSONObject.class);
        if (jsonSelection != null) {
            this.selection = new RowSelectionSpec(jsonSelection);
        }

        final JSONObject jsonSearch = (JSONObject) JSONConverter.getParameterFromJSON(json, "search", JSONObject.class);
        if (jsonSearch != null) {
            this.search = new SearchSpec(jsonSearch);
        }
    }
}
