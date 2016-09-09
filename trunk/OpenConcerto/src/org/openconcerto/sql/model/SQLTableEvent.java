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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.model.SQLRowValues.ForeignCopyMode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Define how a table has changed: what row, what action (insert,delete, update), and what columns.
 * 
 * @author Sylvain
 */
public class SQLTableEvent {

    public enum Mode {
        /**
         * A row was inserted in the DB
         */
        ROW_ADDED {
            @Override
            public Mode opposite() {
                return ROW_DELETED;
            }
        },
        /**
         * A row was deleted in the DB
         */
        ROW_DELETED {
            @Override
            public Mode opposite() {
                return ROW_ADDED;
            }
        },
        /**
         * Some columns of a row were updated
         */
        ROW_UPDATED {
            @Override
            public Mode opposite() {
                return this;
            }
        };

        public abstract Mode opposite();
    }

    private final SQLTable table;
    private final SQLRow row;
    private final Mode mode;
    private final List<String> fieldNames;
    private final List<SQLField> fields;
    private final TransactionPoint txPoint;
    private SQLRowValues vals;

    public SQLTableEvent(final SQLTable table, final int id, final Mode mode) {
        this(table, id, mode, null);
    }

    public SQLTableEvent(final SQLTable table, final int id, final Mode mode, final Collection<String> fields) {
        // allow to signal change on a table w/o a primary key
        this(table, id < SQLRow.MIN_VALID_ID ? null : new SQLRow(table, id), mode, fields);
    }

    /**
     * Construct a new event.
     * 
     * @param row the row that has changed.
     * @param mode how <code>row</code> has changed.
     * @param fields what part of <code>row</code> has changed, <code>null</code> meaning all of it.
     */
    public SQLTableEvent(final SQLRow row, final Mode mode, final Collection<String> fields) {
        this(row.getTable(), row, mode, fields);
    }

    /**
     * Construct a new event.
     * 
     * @param table the table that has changed.
     * @param row the row that has changed.
     * @param mode how <code>row</code> has changed.
     * @param fields what part of <code>row</code> has changed, <code>null</code> meaning all of it.
     */
    private SQLTableEvent(final SQLTable table, final SQLRow row, final Mode mode, final Collection<String> fields) {
        this(table, row, mode, fields, table.getDBSystemRoot().getDataSource().getTransactionPoint());
    }

    private SQLTableEvent(final SQLTable table, final SQLRow row, final Mode mode, final Collection<String> fields, final TransactionPoint txPoint) {
        super();
        this.table = table;
        this.row = row;
        this.mode = mode;
        if (fields == null) {
            this.fieldNames = new ArrayList<String>(this.getTable().getFieldsName());
            this.fields = new ArrayList<SQLField>(this.getTable().getFields());
        } else {
            // remove dups
            this.fieldNames = new ArrayList<String>(new LinkedHashSet<String>(fields));
            this.fields = new ArrayList<SQLField>(this.fieldNames.size());
            for (final String fieldName : this.fieldNames) {
                this.fields.add(this.getTable().getField(fieldName));
            }
        }
        this.txPoint = txPoint;
    }

    public final TransactionPoint getTransactionPoint() {
        return this.txPoint;
    }

    final SQLTableEvent opposite() {
        return new SQLTableEvent(this.table, this.row == null ? null : new SQLRow(this.table, this.row.getID()), this.mode.opposite(), this.fieldNames, this.txPoint);
    }

    public final List<SQLField> getFields() {
        return Collections.unmodifiableList(this.fields);
    }

    public final List<String> getFieldNames() {
        return Collections.unmodifiableList(this.fieldNames);
    }

    /**
     * The row that has changed.
     * 
     * @return the row that has changed, or <code>null</code> if all rows have changed.
     */
    public final SQLRow getRow() {
        return this.row;
    }

    /**
     * Return the rowValues that has changed. NOTE: if this event was generated by
     * {@link SQLRowValues} the result will be linked with all rows committed at the same time.
     * 
     * @return the rowValues that has changed.
     */
    public final SQLRowValues getRowValues() {
        if (this.vals == null) {
            this.vals = this.getRow().asRowValues();
        }
        return this.vals;
    }

    final void setRowValues(SQLRowValues vals) {
        if (this.vals != null)
            throw new IllegalStateException("already set to " + this.vals);
        if (this.getId() != vals.getID())
            throw new IllegalArgumentException("incoherent ID: " + this.getId() + " != " + vals.getID());
        // only foreign rows differ (but at least keep the same ID)
        assert new SQLRowValues(vals, ForeignCopyMode.COPY_ID_OR_RM).asRow().getAbsolutelyAll().equals(this.getRow().getAbsolutelyAll());
        this.vals = vals;
    }

    public final int getId() {
        return this.getRow() == null ? SQLRow.NONEXISTANT_ID : this.getRow().getID();
    }

    public final Mode getMode() {
        return this.mode;
    }

    public final SQLTable getTable() {
        return this.table;
    }

    @Override
    public String toString() {
        return "in " + this.getTable() + " " + this.getMode() + " " + this.getId();
    }

}
