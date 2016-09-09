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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUITable;
import org.openconcerto.ui.light.Row;
import org.openconcerto.ui.light.TableContent;
import org.openconcerto.utils.io.JSONConverter;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightRowValuesTable extends LightUITable {
    List<SQLRowValues> listRowValues = new ArrayList<SQLRowValues>();

    String fieldRefName;
    private SQLRowAccessor refRow;
    private boolean autoCommit = false;
    private List<Integer> deletedIds = new ArrayList<Integer>();

    // Init from json constructor
    public LightRowValuesTable(final JSONObject json) {
        super(json);
        this.init();
    }

    // Clone constructor
    public LightRowValuesTable(final LightRowValuesTable tableElement) {
        super(tableElement);
        this.listRowValues = tableElement.listRowValues;
        this.fieldRefName = tableElement.fieldRefName;
        this.deletedIds = tableElement.deletedIds;
        this.init();
    }

    public LightRowValuesTable(final LightUITable table, final String fieldRefName) {
        super(table);
        this.fieldRefName = fieldRefName;
        this.init();
    }

    public String getFieldRefName() {
        return this.fieldRefName;
    }

    public SQLRowValues getRowValues(final int index) {
        return this.listRowValues.get(index);
    }

    public int getRowValuesCount() {
        return this.listRowValues.size();
    }

    public boolean isAutoCommit() {
        return this.autoCommit;
    }

    public void setAutoCommit(boolean autoCommit) {
        if (autoCommit) {
            if (this.refRow == null) {
                throw new IllegalArgumentException("Set parent row before put this table in auto commit mode");
            }
        }
        this.autoCommit = autoCommit;
    }

    /**
     * Permet de charger les lignes et de lier les nouvelles lignes
     * 
     */
    public void setParentSQLRow(final Configuration configuration, final SQLElement sqlElement, final SQLRowAccessor sqlRow) {

        final TableContent content = new TableContent(getId());
        content.setRows(new ArrayList<Row>());
        getTableSpec().setContent(content);
        this.refRow = sqlRow;
        this.refetchTable(configuration, sqlElement);
    }

    public void refetchTable(final Configuration configuration, final SQLElement sqlElement) {
        this.getTableSpec().getContent().getRows().clear();
        this.listRowValues.clear();
        if (this.refRow != null && !this.refRow.isUndefined()) {
            final SQLTableModelSourceOnline tableSource = sqlElement.getTableSource(true);

            final ListSQLRequest req = tableSource.getReq();
            req.setWhere(new Where(sqlElement.getTable().getField(getFieldRefName()), "=", this.refRow.getID()));
            List<SQLRowValues> listRowValues = req.getValues();

            for (final SQLRowValues rowValues : listRowValues) {
                this.addRowValues(configuration, rowValues);
            }
        }
    }

    public SQLRowAccessor getRefRow() {
        return this.refRow;
    }

    public void clearRowValues(){
        this.getTableSpec().getContent().getRows().clear();
        this.listRowValues.clear();
    }
    
    public void removeRowValuesAt(int index) {
        final TableContent content = this.getTableSpec().getContent();
        content.getRows().remove(index);
        this.listRowValues.remove(index);
    }

    public void addRowValues(final Configuration configuration, final SQLRowValues rowValues) {
        final TableContent content = this.getTableSpec().getContent();
        this.listRowValues.add(rowValues);

        content.getRows().add(this.createRowFromRowValues(configuration, rowValues, this.listRowValues.size() - 1));
    }

    public void setRowValues(final Configuration configuration, final SQLRowValues rowValues, final int index) {
        final TableContent content = this.getTableSpec().getContent();
        this.listRowValues.set(index, rowValues);
        content.getRows().set(index, this.createRowFromRowValues(configuration, rowValues, index));
    }

    public void archiveDeletedRows(final Configuration configuration) {
        final SQLElement sqlElement = configuration.getDirectory().getElementForCode(this.getElementCode());
        for (final Integer deletedId : this.deletedIds) {
            try {
                sqlElement.archive(deletedId);
            } catch (final SQLException ex) {
                throw new IllegalArgumentException(ex.getMessage(), ex);
            }
        }
    }

    public void addDeletedId(final int idToDelete) {
        this.deletedIds.add(idToDelete);
    }

    protected Row createRowFromRowValues(final Configuration configuration, final SQLRowValues sqlRow, final int index) {
        final SQLElement element = configuration.getDirectory().getElementForCode(this.getElementCode());
        if (element == null) {
            throw new IllegalArgumentException("Unable to find element for code: " + this.getElementCode());
        }

        final SQLTableModelSourceOnline tableSource = element.getTableSource(true);
        final List<SQLTableModelColumn> allCols = tableSource.getColumns();

        final Row row = element.createRowFromSQLRow(sqlRow, allCols, this.getTableSpec().getColumns());
        row.setId(index);
        return row;
    }

    private void init() {
        if (this.getTableSpec().getContent() == null) {
            this.getTableSpec().setContent(new TableContent(this.getId()));
        }
    }

    @Override
    public LightUIElement clone() {
        return new LightRowValuesTable(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();

        // TODO: implement row values JSONAble
        if (this.listRowValues != null && !this.listRowValues.isEmpty()) {
            json.put("list-row-values", null);
        }
        if (this.deletedIds != null && !this.deletedIds.isEmpty()) {
            json.put("deleted-ids", JSONConverter.getJSON(this.deletedIds));
        }

        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        final JSONArray jsonListRowValues = JSONConverter.getParameterFromJSON(json, "list-row-values", JSONArray.class);
        this.listRowValues = new ArrayList<SQLRowValues>();
        // TODO: implement row values JSONAble
        if (jsonListRowValues != null) {
        }

        if (this.getTableSpec().getContent() != null) {
            this.getTableSpec().getContent().getRows().clear();
        }

        final JSONArray jsonDeletedIds = JSONConverter.getParameterFromJSON(json, "deleted-ids", JSONArray.class);
        this.deletedIds = new ArrayList<Integer>();
        if (jsonDeletedIds != null) {
            for (final Object jsonDeletedId : jsonDeletedIds) {
                this.deletedIds.add(JSONConverter.getObjectFromJSON(jsonDeletedId, Integer.class));
            }
        }
    }
}
