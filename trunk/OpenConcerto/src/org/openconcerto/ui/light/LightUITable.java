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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUITable extends LightUIElement implements IUserControlContainer {
    private Boolean dynamicLoad = false;
    private Boolean verticallyScrollable = false;
    private Boolean allowSelection = false;
    private TableSpec tableSpec = null;
    private String elementCode = null;

    private List<ActionListener> selectionListeners = new ArrayList<ActionListener>();

    // Nombre de ligne à afficher par Row
    private int linePerRow = 1;

    // Init from json constructor
    public LightUITable(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUITable(final LightUITable tableElement) {
        super(tableElement);
        this.verticallyScrollable = tableElement.verticallyScrollable;
        this.tableSpec = tableElement.tableSpec;
        this.elementCode = tableElement.elementCode;
        this.allowSelection = tableElement.allowSelection;
    }

    public LightUITable(final String id) {
        super(id);
        this.setType(LightUIElement.TYPE_TABLE);

        this.setWeightX(1);
        this.setFillWidth(true);
    }

    public int getIndexfromRowID(int rowID) {
        final TableContent content = this.getTableSpec().getContent();

        for (int i = 0; i < content.getRows().size(); i++) {
            Row r = content.getRows().get(i);
            if (r.getId() == rowID) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setId(final String id) {
        super.setId(id);
        this.tableSpec.setId(id);
        this.tableSpec.getSelection().setTableId(id);
    }

    public String getElementCode() {
        return this.elementCode;
    }

    public void setElementCode(final String elementCode) {
        this.elementCode = elementCode;
    }

    public void setLinePerRow(int linePerRow) {
        this.linePerRow = linePerRow;
    }

    public int getLinePerRow() {
        return this.linePerRow;
    }

    public TableSpec getTableSpec() {
        return this.tableSpec;
    }

    public void setTableSpec(final TableSpec tableSpec) {
        this.tableSpec = tableSpec;
    }

    public Boolean isAllowSelection() {
        return this.allowSelection;
    }

    public void setAllowSelection(final boolean allowSelection) {
        this.allowSelection = allowSelection;
    }

    public Boolean isDynamicLoad() {
        return this.dynamicLoad;
    }

    public void setDynamicLoad(final boolean dynamicLoad) {
        this.dynamicLoad = dynamicLoad;
    }

    public Boolean isVerticallyScrollable() {
        return this.verticallyScrollable;
    }

    public void setVerticallyScrollable(final Boolean verticallyScrollable) {
        this.verticallyScrollable = verticallyScrollable;
    }

    public boolean replaceChild(final LightUIElement pChild) {
        final List<Row> tableRows = this.getTableSpec().getContent().getRows();
        final int tableRowsCount = tableRows.size();
        pChild.setReadOnly(this.isReadOnly());

        for (int i = 0; i < tableRowsCount; i++) {
            final Row tableRow = tableRows.get(i);
            final List<Object> tableRowValues = tableRow.getValues();
            final int tableRowValuesCount = tableRowValues.size();

            for (int j = 0; j < tableRowValuesCount; j++) {
                final Object tableRowValue = tableRowValues.get(j);
                if (tableRowValue instanceof LightUIElement) {
                    final LightUIElement child = (LightUIElement) tableRowValue;

                    if (child.getId().equals(pChild.getId())) {
                        tableRowValues.set(i, pChild);
                        child.setParent(this);
                        return true;
                    }
                    if (child instanceof LightUIContainer) {
                        if (((LightUIContainer) child).replaceChild(pChild)) {
                            return true;
                        }
                    }
                    if (child instanceof LightUITable) {
                        if (((LightUITable) child).replaceChild(pChild)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public LightUIElement findElement(final String searchParam, final boolean byUUID) {
        return this.findElement(searchParam, byUUID, LightUIElement.class);
    }

    public <T extends LightUIElement> T findElement(final String searchParam, final boolean byUUID, final Class<T> objectClass) {
        if (this.tableSpec != null) {
            final TableContent content = this.tableSpec.getContent();
            if (content != null) {
                final List<Row> listRows = content.getRows();
                if (listRows != null) {
                    for (final Row row : listRows) {
                        final List<Object> rowValues = row.getValues();
                        for (final Object value : rowValues) {
                            if (value instanceof LightUIContainer) {
                                final LightUIContainer panel = (LightUIContainer) value;
                                final T element = panel.findChild(searchParam, byUUID, objectClass);
                                if (element != null) {
                                    return element;
                                }
                            } else if (value instanceof LightUIElement) {
                                final LightUIElement element = (LightUIElement) value;
                                if (byUUID) {
                                    if (element.getUUID().equals(searchParam)) {
                                        if (objectClass.isAssignableFrom(element.getClass())) {
                                            return (T) element;
                                        } else {
                                            throw new IllegalArgumentException("Element found at is not an instance of " + objectClass.getName() + ", element class: " + element.getClass().getName()
                                                    + " element ID: " + element.getId());
                                        }
                                    }
                                } else {
                                    if (element.getId().equals(searchParam)) {
                                        if (objectClass.isAssignableFrom(element.getClass())) {
                                            return (T) element;
                                        } else {
                                            throw new IllegalArgumentException("Element found at is not an instance of " + objectClass.getName() + ", element class: " + element.getClass().getName()
                                                    + " element ID: " + element.getId());
                                        }
                                    }
                                }

                                if (element instanceof LightUITable) {
                                    final T resultElement = ((LightUITable) element).findElement(searchParam, byUUID, objectClass);
                                    if (resultElement != null) {
                                        return resultElement;
                                    }
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

    public void addSelectionListener(final ActionListener selectionListener) {
        this.selectionListeners.add(selectionListener);
    }

    public void removeSelectionListeners() {
        this.selectionListeners.clear();
    }

    public void fireSelectionChange() {
        for (final ActionListener listener : this.selectionListeners) {
            listener.actionPerformed(new ActionEvent(this, 1, "selection"));
        }
    }

    @Override
    public void setReadOnly(final boolean readOnly) {
        super.setReadOnly(readOnly);
        final List<Row> rows = this.tableSpec.getContent().getRows();
        if (rows != null) {
            final int rowCount = rows.size();
            for (int i = 0; i < rowCount; i++) {
                final Row row = rows.get(i);
                final List<Object> values = row.getValues();
                for (final Object value : values) {
                    if (value != null && value instanceof LightUIElement) {
                        ((LightUIElement) value).setReadOnly(readOnly);
                    }
                }
            }
        }
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUITable(json);
            }
        };
    }

    @Override
    public void setValueFromContext(final Object value) {
        if (value != null) {
            final JSONArray jsonContext = (JSONArray) JSONConverter.getObjectFromJSON(value, JSONArray.class);
            final ColumnsSpec columnsSpec = this.getTableSpec().getColumns();
            final int columnsCount = columnsSpec.getColumnCount();

            final List<Integer> editorsIndex = new ArrayList<Integer>();

            for (int i = 0; i < columnsCount; i++) {
                final ColumnSpec columnSpec = columnsSpec.getColumn(i);
                if (columnSpec.getEditor() != null) {
                    editorsIndex.add(i);
                }
            }

            final TableContent tableContent = this.getTableSpec().getContent();
            if (tableContent != null) {
                final List<Row> rows = tableContent.getRows();
                for (int i = 0; i < rows.size(); i++) {
                    final Row row = rows.get(i);
                    final JSONObject jsonLineContext = (JSONObject) JSONConverter.getObjectFromJSON(jsonContext.get(i), JSONObject.class);
                    final Long rowId = (Long) JSONConverter.getParameterFromJSON(jsonLineContext, "row.id", Long.class);
                    final String rowExtendId = (String) JSONConverter.getParameterFromJSON(jsonLineContext, "row.extend.id", String.class);
                    if (rowId == row.getId() && (row.getExtendId() == null || (row.getExtendId() != null && rowExtendId.equals(row.getExtendId())))) {
                        if (row.isFillWidth()) {
                            if (!row.getValues().isEmpty() && row.getValues().get(0) instanceof IUserControl) {
                                final LightUIElement element = (LightUIElement) row.getValues().get(0);
                                if (element instanceof IUserControl) {
                                    if (jsonLineContext.containsKey(element.getUUID())) {
                                        ((IUserControl) element).setValueFromContext(jsonLineContext.get(element.getUUID()));
                                    } else {
                                        System.out.println("LightUITable.setValueFromContext() - Unable to find element : id - " + element.getId() + " uuid - " + element.getUUID());
                                        System.out.println("LightUITable.setValueFromContext() - In JSON                : " + jsonLineContext.toJSONString());
                                    }
                                }
                            }
                        } else {
                            for (int k = 0; k < editorsIndex.size(); k++) {
                                final Object objEditor = row.getValues().get(editorsIndex.get(k));
                                if (!(objEditor instanceof IUserControl)) {
                                    throw new IllegalArgumentException("Impossible to find editor for row: " + rowId.toString() + " at position: " + String.valueOf(k));
                                }
                                final LightUIElement editor = (LightUIElement) objEditor;

                                if (editor instanceof IUserControl && jsonLineContext.containsKey(editor.getUUID())) {
                                    ((IUserControl) editor).setValueFromContext(jsonLineContext.get(editor.getUUID()));
                                } else {
                                    throw new IllegalArgumentException(
                                            "Impossible to find value for editor: " + editor.getId() + " for row: " + rowId.toString() + " at position: " + String.valueOf(k));
                                }
                            }
                        }
                    } else {
                        throw new IllegalArgumentException("Impossible to find row: " + rowId.toString());
                    }
                }
            }
        }
    }

    @Override
    public LightUIElement clone() {
        return new LightUITable(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (this.allowSelection) {
            json.put("allow-selection", true);
        }
        if (this.dynamicLoad) {
            json.put("dynamic-load", true);
        }
        if (this.verticallyScrollable) {
            json.put("vertically-scrollable", true);
        }
        if (this.tableSpec != null) {
            json.put("table-spec", this.tableSpec.toJSON());
        }
        if (this.elementCode != null) {
            json.put("element-code", this.elementCode);
        }
        json.put("line-per-row", this.linePerRow);
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.allowSelection = JSONConverter.getParameterFromJSON(json, "allow-selection", Boolean.class, false);
        this.dynamicLoad = JSONConverter.getParameterFromJSON(json, "dynamic-load", Boolean.class, false);
        this.verticallyScrollable = JSONConverter.getParameterFromJSON(json, "vertically-scrollable", Boolean.class, false);
        this.elementCode = JSONConverter.getParameterFromJSON(json, "element-code", String.class);
        this.linePerRow = JSONConverter.getParameterFromJSON(json, "line-per-row", Integer.class);

        final JSONObject jsonRawContent = (JSONObject) JSONConverter.getParameterFromJSON(json, "table-spec", JSONObject.class);

        if (jsonRawContent != null) {
            this.tableSpec = new TableSpec(jsonRawContent);
        }
    }
}
