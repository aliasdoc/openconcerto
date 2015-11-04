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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLTable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

class SQLElementRowR extends BaseSQLElementRow {

    public SQLElementRowR(SQLRow row) {
        super(row);
    }

    public SQLElementRowR(SQLElement element, SQLRow row) {
        super(element, row);
    }

    @Override
    public boolean equals(Object obj) {
        return this.equals(obj, false);
    }

    public boolean equals(Object obj, final boolean ignoreNotDeepCopied) {
        if (obj instanceof SQLElementRowR) {
            final SQLElementRowR o = (SQLElementRowR) obj;
            // test relations parent-enfant
            final Map<SQLRow, SQLRow> copies = new HashMap<SQLRow, SQLRow>();
            if (!equalsRec(o, copies, ignoreNotDeepCopied))
                return false;

            // test relations normal ff
            for (final SQLRow thisRow : copies.keySet()) {
                final Set<String> ffs = getElement(thisRow).getNormalForeignFields();
                for (final String ff : ffs) {
                    final SQLRow foreignRow = thisRow.getForeignRow(ff);
                    if (copies.containsKey(foreignRow)) {
                        final SQLRow copy = copies.get(thisRow);
                        if (!getElement(foreignRow).equals(copy.getForeignRow(ff), foreignRow, ignoreNotDeepCopied))
                            return false;
                    }
                }
            }

            return true;
        } else
            return false;
    }

    private boolean equalsRec(SQLElementRowR o, Map<SQLRow, SQLRow> copies, final boolean ignoreNotDeepCopied) {
        if (!this.getElem().equals(this.getRow(), o.getRow(), ignoreNotDeepCopied))
            return false;
        final Map<SQLTable, List<SQLRow>> children1 = this.getElem().getChildrenRows(this.getRow());
        final Map<SQLTable, List<SQLRow>> children2 = this.getElem().getChildrenRows(o.getRow());
        if (!children1.keySet().equals(children2.keySet()))
            return false;
        for (final SQLTable childT : children1.keySet()) {
            final List<SQLRow> l1 = children1.get(childT);
            final List<SQLRow> l2 = children2.get(childT);
            if (l1.size() != l2.size())
                return false;

            final Iterator<SQLRow> lIter1 = l1.iterator();
            final Iterator<SQLRow> lIter2 = l2.iterator();
            while (lIter1.hasNext()) {
                final SQLRow r1 = lIter1.next();
                final SQLRow r2 = lIter2.next();
                final SQLElementRowR o1 = new SQLElementRowR(r1);
                final SQLElementRowR o2 = new SQLElementRowR(r2);
                if (!o1.equalsRec(o2, copies, ignoreNotDeepCopied))
                    return false;
            }

        }
        copies.put(this.getRow(), o.getRow());
        return true;
    }

}
