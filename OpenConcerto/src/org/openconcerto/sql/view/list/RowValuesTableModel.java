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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.Constraint;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax.ConstraintType;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.OrderedSet;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

public class RowValuesTableModel extends AbstractTableModel {
    // modification of rowValues MUST be done in AWT EDT
    // methods that perform requests, MUST use the runnableQueue
    // synchronized is used to protect list access
    private List<SQLRowValues> rowValues = new ArrayList<SQLRowValues>();

    private OrderedSet<TableModelListener> tableModelListeners = new OrderedSet<TableModelListener>();

    protected SQLElement element;

    private int nbColumn;

    private List<SQLTableElement> list; // Liste de SQLTableElement
    private Map<String, Integer> mapColumnField = new HashMap<String, Integer>();

    private List<SQLField> requiredFields;

    private SQLField requiredField, validationField;

    private SQLRowValues defautRow;
    private List<SQLRowValues> rowValuesDeleted = new ArrayList<SQLRowValues>();

    private boolean editable = true;

    private Where fillWhere;

    public RowValuesTableModel() {

    }

    public RowValuesTableModel(final SQLElement e, final List<SQLTableElement> list, SQLField validField) {
        this(e, list, validField, true);
    }

    public RowValuesTableModel(final SQLElement e, final List<SQLTableElement> list, SQLField validField, boolean addDefault, SQLRowValues rowParDefaut) {
        this(e, list, validField, addDefault, rowParDefaut, null);

    }

    public RowValuesTableModel(final SQLElement e, final List<SQLTableElement> list, SQLField validField, boolean addDefault, SQLRowValues rowParDefaut, SQLField validationField) {
        init(e, list, validField, addDefault, rowParDefaut, validationField);

    }

    public RowValuesTableModel(final SQLElement e, final List<SQLTableElement> list, SQLField validField, boolean addDefault) {
        this(e, list, validField, addDefault, null);
    }

    protected void init(final SQLElement e, final List<SQLTableElement> list, SQLField validField, boolean addDefault, SQLRowValues rowParDefaut) {
        init(e, list, validField, addDefault, rowParDefaut, null);
    }

    /**
     * @param e
     * @param list
     * @param validField
     * @param addDefault
     * @param rowParDefaut
     */
    protected void init(final SQLElement e, final List<SQLTableElement> list, SQLField validField, boolean addDefault, SQLRowValues rowParDefaut, SQLField validationField) {
        this.element = e;
        this.requiredField = validField;
        this.requiredFields = new ArrayList<SQLField>();

        this.requiredFields.add(validField);
        this.validationField = validationField;
        this.list = list;
        this.nbColumn = list.size();

        if (rowParDefaut != null) {
            this.defautRow = rowParDefaut;
        } else {
            this.defautRow = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(e.getTable()));
        }

        // rowParDefaut
        if (addDefault) {
            final SQLRowValues row = new SQLRowValues(this.defautRow);
            final BigDecimal maxOrder = RowValuesTableModel.this.element.getTable().getMaxOrder();
            row.put(RowValuesTableModel.this.element.getTable().getOrderField().getName(), maxOrder.add(BigDecimal.ONE));
            RowValuesTableModel.this.rowValues.add(row);
        }
    }

    public void setFillWhere(Where fillWhere) {
        this.fillWhere = fillWhere;
    }

    public void addRequiredField(SQLField f) {
        this.requiredFields.add(f);
    }

    public SQLRowValues getDefaultRowValues() {
        return this.defautRow;
    }

    public synchronized void addColumn(SQLTableElement e) {
        this.nbColumn++;
        this.list.add(e);
    }

    public synchronized int getColumnCount() {
        return this.nbColumn;
    }

    public int getRowCount() {
        checkEDT();
        return this.rowValues.size();
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (!this.editable)
            return false;

        SQLTableElement elt = this.list.get(columnIndex);
        boolean validate = false;
        boolean fieldValidate = false;
        if (this.validationField != null) {
            fieldValidate = elt.getField().getName().equalsIgnoreCase(this.validationField.getName());
            validate = this.getRowValuesAt(rowIndex).getBoolean(this.validationField.getName());
        }
        if (validate && fieldValidate) {
            return this.list.get(columnIndex).isCellEditable(this.getRowValuesAt(rowIndex), rowIndex, columnIndex);
        } else {
            return (!validate) && this.list.get(columnIndex).isCellEditable(this.getRowValuesAt(rowIndex), rowIndex, columnIndex);
        }
    }

    public synchronized Class<?> getColumnClass(int columnIndex) {
        return this.list.get(columnIndex).getElementClass();
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        checkEDT();
        final Object result;

        if (rowIndex >= this.rowValues.size()) {
            System.err.println("RowValuesTableModel: get(" + rowIndex + "," + columnIndex + ") rowIndex>" + this.rowValues.size());
            Thread.dumpStack();
            result = new Integer(0);
        } else {
            SQLRowValues val = this.rowValues.get(rowIndex);
            SQLTableElement sqlTableElem = this.list.get(columnIndex);
            Object storedObject = val.getObject(sqlTableElem.getRowField());
            result = sqlTableElem.getValueFrom(val);
            if (sqlTableElem.getElementClass() != null && Number.class.isAssignableFrom(sqlTableElem.getElementClass()) && !CompareUtils.equals(result, storedObject)) {
                fireTableDataChanged();
            }

        }

        return result;
    }

    private void checkEDT() {
        if (!SwingUtilities.isEventDispatchThread())
            Thread.dumpStack();
    }

    public void putValue(Object value, int rowIndex, String fieldName) {
        putValue(value, rowIndex, fieldName, false);
    }

    public void putValue(Object value, int rowIndex, String fieldName, boolean forcedFire) {
        checkEDT();
        final SQLRowValues rowVal = this.rowValues.get(rowIndex);
        Object oldValue = rowVal.getObject(fieldName);
        if (!forcedFire) {
            if (oldValue == value) {
                return;
            }
            if (oldValue != null && oldValue.equals(value)) {
                return;
            }
        }
        rowVal.put(fieldName, value);
        for (SQLTableElement sqlTableElem : this.list) {
            sqlTableElem.fireModification(rowVal);
        }
        fireTableModelModified(rowIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        checkEDT();
        if (!this.editable)
            return;
        if (this.list.size() <= columnIndex) {
            return;
        }

        Object oldValue = getValueAt(rowIndex, columnIndex);
        if (aValue == oldValue) {
            return;
        }
        if (oldValue != null && oldValue.equals(aValue)) {
            return;
        }
        try {
            SQLTableElement sqlTableElem = this.list.get(columnIndex);

            SQLRowValues rowVal = this.rowValues.get(rowIndex);
            Object realVal = sqlTableElem.convertEditorValueToModel(aValue, rowVal);
            if (realVal == null || realVal.getClass() == this.getColumnClass(columnIndex)) {
                sqlTableElem.setValueFrom(rowVal, realVal);
                fireTableChanged(new TableModelEvent(this, rowIndex, rowIndex, columnIndex));
            } else {
                System.err.println("RowValuesTableModel:setValueAt:" + realVal + "(" + realVal.getClass() + ") at (row:" + rowIndex + "/col:" + columnIndex + ") column class : "
                        + this.getColumnClass(columnIndex));
                Thread.dumpStack();
            }
        } catch (Exception e) {
            // can append when stop editing occur while removing a line
            e.printStackTrace();
        }
    }

    /**
     * 
     */
    public void dumpValues() {
        for (int i = 0; i < this.rowValues.size(); i++) {
            SQLRowValues val = this.rowValues.get(i);
            System.out.println("Item" + i + ":" + val);
        }
    }

    public String getColumnName(int columnIndex) {
        SQLTableElement sqlTableElem = this.list.get(columnIndex);
        return sqlTableElem.getColumnName();
    }

    /**
     * Valider les modifications dans la base
     */
    public void commitData() {
        checkEDT();
        final List<SQLRowValues> rowsToCommmit = new ArrayList<SQLRowValues>();
        rowsToCommmit.addAll(this.rowValues);
        try {
            final SQLTable table = getDefaultRowValues().getTable();
            // Remove constraint on ORDRE for private
            final Constraint constraint = table.getConstraint(ConstraintType.UNIQUE, Arrays.asList(table.getOrderField().getName()));
            if (constraint != null) {
                final String req = new AlterTable(table).dropConstraint(constraint.getName()).asString();
                table.getDBSystemRoot().getDataSource().execute(req);
                table.fetchFields();
                table.getSchema().updateVersion();
            }
            BigDecimal o = table.getMaxOrder(Boolean.FALSE);
            final int size = rowsToCommmit.size();
            for (int i = 0; i < size; i++) {
                final SQLRowValues r = rowsToCommmit.get(i);
                r.put(r.getTable().getOrderField().getFieldName(), o.add(new BigDecimal(i + 1)));
                final SQLRow row = r.commit();
                r.setID(row.getIDNumber());
            }
        } catch (SQLException e) {
            ExceptionHandler.handle("Unable to commit rows", e);
        }
    }

    public void addTableModelListener(TableModelListener l) {
        this.tableModelListeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l) {
        this.tableModelListeners.remove(l);
    }

    public void fireTableModelModified(int line) {
        checkEDT();
        this.fireTableRowsUpdated(line, line);

    }

    public int getColumnForField(String to) {
        if (this.mapColumnField.get(to) == null) {

            for (int columnIndex = 0; columnIndex < this.list.size(); columnIndex++) {
                SQLTableElement sqlTableElem = this.list.get(columnIndex);
                if (sqlTableElem.getField() != null) {
                    if (sqlTableElem.getField().getName().equalsIgnoreCase(to)) {
                        this.mapColumnField.put(to, columnIndex);
                        return columnIndex;
                    }
                }
            }
            this.mapColumnField.put(to, -1);
            return -1;
        } else {
            return this.mapColumnField.get(to);
        }
    }

    public void addNewRowAt(final int index) {
        checkEDT();
        if (index > getRowCount()) {
            throw new IllegalArgumentException(index + " > row count: " + getRowCount());
        } else if (index < 0) {
            throw new IllegalArgumentException(index + " <0");
        }

        final SQLRowValues newRowParDefaut = new SQLRowValues(RowValuesTableModel.this.defautRow);
        if (index > 0 && index < getRowCount() && newRowParDefaut.getTable().contains("NIVEAU")) {
            newRowParDefaut.put("NIVEAU", this.rowValues.get(index - 1).getObject("NIVEAU"));
        }
        RowValuesTableModel.this.rowValues.add(index, newRowParDefaut);

        final int size = RowValuesTableModel.this.tableModelListeners.size();
        for (int i = 0; i < size; i++) {
            final TableModelListener l = RowValuesTableModel.this.tableModelListeners.get(i);
            l.tableChanged(new TableModelEvent(RowValuesTableModel.this, index, index, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
        }

    }

    public void addRowAt(final int index, SQLRowValues rowVals) {
        checkEDT();
        if (index > getRowCount()) {
            throw new IllegalArgumentException(index + " > row count: " + getRowCount());
        } else if (index < 0) {
            throw new IllegalArgumentException(index + " <0");
        }

        RowValuesTableModel.this.rowValues.add(index, rowVals);

        final int size = RowValuesTableModel.this.tableModelListeners.size();
        for (int i = 0; i < size; i++) {
            final TableModelListener l = RowValuesTableModel.this.tableModelListeners.get(i);
            l.tableChanged(new TableModelEvent(RowValuesTableModel.this, index, index, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
        }

    }

    /**
     * Suppression d'une ligne de la table
     * 
     * @param index index de la ligne
     */
    public void removeRowAt(final int index) {
        checkEDT();
        if (index < 0) {
            return;
        }
        final SQLRowValues removedLine = this.rowValues.remove(index);
        this.rowValuesDeleted.add(removedLine);
        fireTableRowsDeleted(index, index);
    }

    /**
     * Suppression de plusieurs ligne de la table
     * 
     * @param index tableau des index de ligne à supprimer
     */
    public void removeRowsAt(final int[] index) {
        checkEDT();
        if (index.length <= 0)
            return;

        final List<SQLRowValues> rowVals = new ArrayList<SQLRowValues>(index.length);
        for (int i : index) {
            final SQLRowValues rowValues2 = RowValuesTableModel.this.rowValues.get(i);
            rowVals.add(rowValues2);
            RowValuesTableModel.this.rowValuesDeleted.add(rowValues2);
        }

        // Ne pas utiliser removeAll, supprime toutes les lignes equals
        // RowValuesTableModel.this.rowValues.removeAll(rowVals);
        for (SQLRowValues sqlRowValues2Remove : rowVals) {
            for (int i = 0; i < getRowCount(); i++) {
                SQLRowValues rowValsList = RowValuesTableModel.this.rowValues.get(i);
                if (rowValsList == sqlRowValues2Remove) {
                    RowValuesTableModel.this.rowValues.remove(i);
                    break;
                }
            }
        }
        fireTableDataChanged();

    }

    public void addNewRow() {
        addNewRowAt(getRowCount());
    }

    public boolean isLastRowValid() {
        return isRowValid(this.rowValues.size() - 1);
    }

    public boolean isRowValid(int index) {
        checkEDT();
        if (this.rowValues.size() == 0)
            return true;

        if (index < 0 || index >= this.rowValues.size()) {
            return false;
        }

        SQLRowValues row = this.rowValues.get(index);

        boolean valid = true;
        for (SQLField f : this.requiredFields) {
            if (f.isKey()) {
                valid &= !row.isForeignEmpty(f.getName());
            } else {
                final Object object = row.getObject(f.getName());
                valid &= object != null && object.toString().trim().length() > 0;
            }
            if (!valid) {
                break;
            }
        }
        return valid;
    }

    public boolean isValidated() {
        boolean b = true;
        for (int i = 0; i < getRowCount(); i++) {
            b &= isRowValid(i);
        }
        return b;
    }

    public final List<SQLTableElement> getList() {
        return this.list;
    }

    public void updateField(String field, SQLRowValues rowVals, String fieldCondition) {
        checkEDT();
        if (rowVals != null) {
            int stop = this.rowValues.size();

            // FIXME check à faire sur l'ensemble des rows avec la methode isValid(). Quand
            // RowValuesTable deviendra un RowItemView.
            // if (!isLastRowValid()) {
            // stop--;
            // }
            int id = rowVals.getID();

            for (int i = 0; i < stop; i++) {

                SQLRowValues r = this.rowValues.get(i);

                if (fieldCondition != null) {
                    Object o = r.getObject(fieldCondition);
                    if (o != null && ((Boolean) o)) {
                        if (id != SQLRow.NONEXISTANT_ID) {
                            r.put(field, id);
                        } else {
                            r.put(field, rowVals);
                        }
                    } else {
                        r.put(field, 1);
                    }
                } else {
                    if (id != SQLRow.NONEXISTANT_ID) {
                        r.put(field, id);
                    } else {
                        r.put(field, rowVals);
                    }
                }
            }
            // FIXME : must be done in commit not here
            List<SQLRowValues> l = new ArrayList<SQLRowValues>(this.rowValuesDeleted);
            Set<SQLRowValues> toArchive = new HashSet<SQLRowValues>();
            for (int i = 0; i < l.size(); i++) {
                SQLRowValues rowVals2 = l.get(i);
                int idRow = rowVals2.getID();
                if (idRow != SQLRow.NONEXISTANT_ID) {
                    toArchive.add(rowVals2);
                } else {
                    rowVals2.putEmptyLink(field);
                }
            }
            this.rowValuesDeleted.clear();
            try {
                this.element.archive(toArchive);
            } catch (SQLException e) {
                throw new IllegalStateException("Unable to archive rows : " + toArchive, e);
            }

            if (id != SQLRow.NONEXISTANT_ID) {
                this.commitData();
            }
        }
    }

    public void updateField(String field, SQLRowValues rowVals) {
        updateField(field, rowVals, null);
    }

    public void updateField(String field, int id) {
        updateField(field, id, null);
    }

    public void updateField(String field, int id, String fieldCondition) {
        if (id > 0) {
            updateField(field, this.element.getTable().getForeignTable(field).getRow(id).createUpdateRow(), fieldCondition);
        }
    }

    public void insertFrom(String field, int id) {
        insertFrom(field, id, SQLRow.NONEXISTANT_ID);
    }

    public void insertFrom(final String field, final int id, final int exceptID) {
        checkEDT();
        if (id > 0) {

            SQLSelect sel = new SQLSelect();
            final SQLTable table = RowValuesTableModel.this.element.getTable();
            sel.addSelectStar(table);
            Where w = new Where(table.getField(field), "=", id);
            w = w.and(new Where(table.getKey(), "!=", exceptID));
            if (fillWhere != null) {
                w = w.and(fillWhere);
            }
            sel.setWhere(w);
            sel.addFieldOrder(table.getOrderField());

            final List<SQLRow> listOfRows = SQLRowListRSH.execute(sel);

            RowValuesTableModel.this.rowValues.clear();
            final int size = listOfRows.size();
            for (int i = 0; i < size; i++) {
                SQLRow sqlRow = listOfRows.get(i);
                SQLRowValues row = sqlRow.createUpdateRow();
                RowValuesTableModel.this.rowValues.add(row);
            }
            fireTableModelModified(RowValuesTableModel.this.rowValues.size());

        }

    }

    public void insertFrom(final SQLRowAccessor rowVals) {
        insertFrom(rowVals, null);
    }

    public void insertFrom(final SQLRowAccessor rowVals, final SQLField referentField) {
        insertFrom(rowVals, referentField, null, null);
    }

    // Remplit la table à partir de la SQLRow parente
    public void insertFrom(final SQLRowAccessor rowVals, final SQLField referentField, final SQLField fieldWhere, final Object value) {
        if (!SwingUtilities.isEventDispatchThread()) {
            Thread.dumpStack();
        }
        if (rowVals != null) {

            final List<SQLRowValues> newRows = new ArrayList<SQLRowValues>();

            if (rowVals.getID() > 1) {
                SQLRow row = rowVals.getTable().getRow(rowVals.getID());
                List<SQLRow> rowSet;
                if (referentField == null) {
                    rowSet = row.getReferentRows(RowValuesTableModel.this.element.getTable());
                } else {
                    rowSet = row.getReferentRows(referentField);
                }
                for (SQLRow row2 : rowSet) {
                    if (fieldWhere == null || CompareUtils.equals(row2.getObject(fieldWhere.getName()), value)) {
                        SQLRowValues rowVals2 = new SQLRowValues(RowValuesTableModel.this.element.getTable());
                        rowVals2.loadAbsolutelyAll(row2);
                        newRows.add(rowVals2);
                    }
                }

            } else {
                final Collection<? extends SQLRowAccessor> colRows;
                if (referentField == null) {
                    colRows = rowVals.getReferentRows(RowValuesTableModel.this.element.getTable());
                } else {
                    colRows = rowVals.getReferentRows(referentField);
                }
                for (SQLRowAccessor rowValues : colRows) {
                    if (fieldWhere == null || CompareUtils.equals(rowValues.getObject(fieldWhere.getName()), value)) {
                        newRows.add(rowValues.asRowValues());
                    }
                }
            }

            RowValuesTableModel.this.rowValues.clear();
            RowValuesTableModel.this.rowValues.addAll(newRows);
            fireTableModelModified(RowValuesTableModel.this.rowValues.size());

        }
    }

    public void addRow(SQLRowValues row) {
        checkEDT();
        addRow(row, true);
    }

    public void addRow(final SQLRowValues row, final boolean fireModified) {
        checkEDT();
        final List<SQLRowValues> rows = new ArrayList<SQLRowValues>(1);
        rows.add(row);
        addRows(rows, true);
    }

    public void submit(Runnable r) {
        checkEDT();
        r.run();
    }

    public void addRows(final List<SQLRowValues> rows, final boolean fireModified) {
        checkEDT();
        if (rows.isEmpty()) {
            return;
        }

        addRowsSync(rows, fireModified);

    }

    public void clearRows() {
        checkEDT();
        final int size = RowValuesTableModel.this.rowValues.size();
        if (size > 0) {
            RowValuesTableModel.this.rowValues.clear();
            fireTableRowsDeleted(0, size - 1);
        }

    }

    public synchronized SQLTableElement getSQLTableElementAt(int columnIndex) {
        checkEDT();
        if (columnIndex >= 0 && columnIndex < this.list.size()) {
            return this.list.get(columnIndex);
        } else {
            return null;
        }
    }

    public synchronized int getColumnIndexForElement(SQLTableElement e) {
        checkEDT();
        for (int columnIndex = 0; columnIndex < this.list.size(); columnIndex++) {
            SQLTableElement sqlTableElem = this.list.get(columnIndex);
            if (sqlTableElem.equals(e)) {
                return columnIndex;
            }
        }
        return -1;
    }

    public SQLRowValues getRowValuesAt(int rowIndex) {
        checkEDT();
        return this.rowValues.get(rowIndex);
    }

    public final int id2index(int id) {
        checkEDT();
        for (int i = 0; i < this.getRowCount(); i++) {
            if (this.getRowValuesAt(i).getID() == id)
                return i;
        }
        return -1;
    }

    public final int row2index(SQLRowAccessor row) {
        checkEDT();
        return this.rowValues.indexOf(row);
    }

    public void fireTableChanged(TableModelEvent event) {
        checkEDT();
        for (int i = 0; i < this.tableModelListeners.size(); i++) {
            TableModelListener l = this.tableModelListeners.get(i);
            l.tableChanged(event);
        }
    }

    /**
     * Notifies all listeners that all cell values in the table's rows may have changed. The number
     * of rows may also have changed and the <code>JTable</code> should redraw the table from
     * scratch. The structure of the table (as in the order of the columns) is assumed to be the
     * same.
     * 
     * @see TableModelEvent
     * @see EventListenerList
     * @see javax.swing.JTable#tableChanged(TableModelEvent)
     */
    public void fireTableDataChanged() {
        fireTableChanged(new TableModelEvent(this));
    }

    /**
     * Notifies all listeners that the table's structure has changed. The number of columns in the
     * table, and the names and types of the new columns may be different from the previous state.
     * If the <code>JTable</code> receives this event and its
     * <code>autoCreateColumnsFromModel</code> flag is set it discards any table columns that it had
     * and reallocates default columns in the order they appear in the model. This is the same as
     * calling <code>setModel(TableModel)</code> on the <code>JTable</code>.
     * 
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableStructureChanged() {
        fireTableChanged(new TableModelEvent(this, TableModelEvent.HEADER_ROW));
    }

    /**
     * Notifies all listeners that rows in the range <code>[firstRow, lastRow]</code>, inclusive,
     * have been inserted.
     * 
     * @param firstRow the first row
     * @param lastRow the last row
     * 
     * @see TableModelEvent
     * @see EventListenerList
     * 
     */
    public void fireTableRowsInserted(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
    }

    /**
     * Notifies all listeners that rows in the range <code>[firstRow, lastRow]</code>, inclusive,
     * have been updated.
     * 
     * @param firstRow the first row
     * @param lastRow the last row
     * 
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableRowsUpdated(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE));
    }

    /**
     * Notifies all listeners that rows in the range <code>[firstRow, lastRow]</code>, inclusive,
     * have been deleted.
     * 
     * @param firstRow the first row
     * @param lastRow the last row
     * 
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableRowsDeleted(int firstRow, int lastRow) {
        fireTableChanged(new TableModelEvent(this, firstRow, lastRow, TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE));
    }

    /**
     * Notifies all listeners that the value of the cell at <code>[row, column]</code> has been
     * updated.
     * 
     * @param row row of cell which has been updated
     * @param column column of cell which has been updated
     * @see TableModelEvent
     * @see EventListenerList
     */
    public void fireTableCellUpdated(int row, int column) {
        fireTableChanged(new TableModelEvent(this, row, row, column));
    }

    /**
     * Déplacer une ligne
     * 
     * @param rowIndex ligne à déplacer
     * @param inc incrémentation +1 ou -1
     * @return le nouvel index
     */
    public int moveBy(int rowIndex, int inc) {
        checkEDT();
        int destIndex = rowIndex + inc;

        // On vérifie que l'on reste dans le tableau
        if (rowIndex >= 0 && destIndex >= 0) {
            if (rowIndex < this.rowValues.size() && destIndex < this.rowValues.size()) {
                SQLRowValues rowValues1 = this.rowValues.get(rowIndex);
                SQLRowValues rowValues2 = this.rowValues.get(destIndex);
                this.rowValues.set(rowIndex, rowValues2);
                this.rowValues.set(destIndex, rowValues1);
                this.fireTableRowsUpdated(rowIndex, destIndex);
                this.fireTableDataChanged();
            }
        }
        return destIndex;
    }

    public List<SQLRowValues> getCopyOfValues() {
        checkEDT();
        List<SQLRowValues> vals = new ArrayList<SQLRowValues>(this.rowValues.size());
        for (SQLRowValues sqlRowValues : this.rowValues) {
            vals.add(sqlRowValues.asRowValues());
        }
        return vals;
    }

    /**
     * Rendre une colonne éditable ou nom
     * 
     * @param b
     * @param column
     */
    public void setEditable(boolean b, int column) {
        checkEDT();
        this.list.get(column).setEditable(b);
    }

    public void setEditable(boolean b) {
        this.editable = b;

    }

    public SQLElement getSQLElement() {
        return this.element;
    }

    public SQLField getRequiredField() {
        return this.requiredField;
    }

    public List<SQLField> getRequiredsField() {
        return this.requiredFields;
    }

    public void addRowsSync(final List<SQLRowValues> rows, final boolean fireModified) {
        checkEDT();
        if (rows.isEmpty()) {
            return;
        }
        RowValuesTableModel.this.rowValues.addAll(rows);
        if (fireModified) {
            for (SQLRowValues row : rows) {
                for (SQLTableElement sqlTableElem : RowValuesTableModel.this.list) {
                    sqlTableElem.fireModification(row);
                }
            }
            fireTableRowsInserted(RowValuesTableModel.this.rowValues.size() - rows.size(), RowValuesTableModel.this.rowValues.size() - 1);
        }
    }
}
