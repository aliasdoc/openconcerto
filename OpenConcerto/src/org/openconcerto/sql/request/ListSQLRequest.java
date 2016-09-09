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
 
 package org.openconcerto.sql.request;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class ListSQLRequest extends FilteredFillSQLRequest {

    // les champs à afficher (avant expansion)
    // immutable
    private final List<SQLField> listFields;

    private final FieldExpander showAs;

    public ListSQLRequest(SQLTable table, List fieldss) {
        this(table, fieldss, null);
    }

    // fieldss : list de String (préferré) ou de SQLField
    public ListSQLRequest(SQLTable table, List fieldss, Where where) {
        this(table, fieldss, where, null);
    }

    public ListSQLRequest(SQLTable table, List fieldss, Where where, final FieldExpander showAs) {
        super(table, where);
        if (!this.getPrimaryTable().isOrdered())
            throw new IllegalArgumentException(table + " is not ordered.");

        final List<SQLField> tmpList = new ArrayList<SQLField>();
        for (final Object field : fieldss) {
            final SQLField f;
            if (field instanceof String)
                f = this.getPrimaryTable().getField((String) field);
            else if (field instanceof SQLField) {
                final SQLField fToCheck = (SQLField) field;
                if (fToCheck.getTable().equals(this.getPrimaryTable()))
                    f = fToCheck;
                else
                    throw new IllegalArgumentException("field " + fToCheck + " not part of the primary table : " + this.getPrimaryTable());
            } else
                throw new IllegalArgumentException("must be a fieldname or a SQLField but got : " + field);

            tmpList.add(f);
        }
        this.listFields = Collections.unmodifiableList(tmpList);

        if (showAs != null) {
            this.showAs = showAs;
        } else {
            final Configuration conf = Configuration.getInstance();
            if (conf == null) {
                this.showAs = FieldExpander.getEmpty();
            } else {
                this.showAs = conf.getShowAs();
            }
        }
    }

    protected ListSQLRequest(ListSQLRequest req, final boolean freeze) {
        super(req, freeze);
        this.listFields = req.listFields;
        this.showAs = req.showAs;
    }

    // wasFrozen() : our showAs might change but our fetcher won't, MAYBE remove final modifier and
    // clone showAs

    @Override
    public ListSQLRequest toUnmodifiable() {
        return this.toUnmodifiableP(ListSQLRequest.class);
    }

    @Override
    public ListSQLRequest clone() {
        synchronized (this) {
            return this.clone(false);
        }
    }

    @Override
    protected ListSQLRequest clone(boolean forFreeze) {
        return new ListSQLRequest(this, forFreeze);
    }

    @Override
    protected FieldExpander getShowAs() {
        return this.showAs;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.request.BaseSQLRequest#getFields()
     */
    @Override
    public final List<SQLField> getFields() {
        return this.listFields;
    }

    @Override
    protected void customizeToFetch(SQLRowValues graphToFetch) {
        super.customizeToFetch(graphToFetch);
        addField(graphToFetch, getPrimaryTable().getCreationDateField());
        addField(graphToFetch, getPrimaryTable().getCreationUserField());
        addField(graphToFetch, getPrimaryTable().getModifDateField());
        addField(graphToFetch, getPrimaryTable().getModifUserField());
        addField(graphToFetch, getPrimaryTable().getFieldRaw(SQLComponent.READ_ONLY_FIELD));
        addField(graphToFetch, getPrimaryTable().getFieldRaw(SQLComponent.READ_ONLY_USER_FIELD));
    }

    private void addField(SQLRowValues graphToFetch, final SQLField f) {
        if (f != null && !graphToFetch.getFields().contains(f.getName())) {
            if (f.isKey())
                graphToFetch.putRowValues(f.getName()).putNulls("NOM", "PRENOM");
            else
                graphToFetch.put(f.getName(), null);
        }
    }
}
