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
import org.openconcerto.sql.model.SQLFilter;
import org.openconcerto.sql.model.SQLFilterListener;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.TableRef;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.CompareUtils;
import org.openconcerto.utils.Tuple2;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// une fill request qui utilise le filtre
public abstract class FilteredFillSQLRequest extends BaseFillSQLRequest {

    // make sure that all rows are from the same table
    static protected final Tuple2<SQLTable, Set<Number>> rows2ids(final Collection<SQLRow> rows) {
        final Set<SQLTable> tables = new HashSet<SQLTable>();
        final Set<Number> ids = new HashSet<Number>(rows.size());
        for (final SQLRow r : rows) {
            tables.add(r.getTable());
            ids.add(r.getIDNumber());
        }
        final SQLTable filterTable = CollectionUtils.getSole(tables);
        if (filterTable == null)
            throw new IllegalStateException("not 1 table: " + rows);
        return Tuple2.create(filterTable, ids);
    }

    static protected final SQLFilter getDefaultFilter() {
        final Configuration conf = Configuration.getInstance();
        return conf == null ? null : conf.getFilter();
    }

    // never null (but can be <null, null>)
    private Tuple2<Set<SQLRow>, Path> filterInfo;
    private final SQLFilter filter;
    // initially false since we don't listen to filter before calling setFilterEnabled()
    private boolean filterEnabled = false;
    private final SQLFilterListener filterListener;

    {
        this.filterListener = new SQLFilterListener() {
            public void filterChanged(Collection<SQLTable> tables) {
                if (CollectionUtils.containsAny(getTables(), tables))
                    updateFilterWhere();
            }
        };
    }

    public FilteredFillSQLRequest(final SQLTable primaryTable, Where w) {
        super(primaryTable, w);
        this.filter = getDefaultFilter();
        this.filterInfo = Tuple2.create(null, null);
        this.setFilterEnabled(true);
    }

    public FilteredFillSQLRequest(FilteredFillSQLRequest req) {
        super(req);
        this.filter = req.filter;
        this.filterInfo = req.filterInfo;
        this.setFilterEnabled(req.filterEnabled);
    }

    protected final SQLFilter getFilter() {
        return this.filter;
    }

    public final void setFilterEnabled(boolean b) {
        final SQLFilter filter = this.getFilter();
        b = filter == null ? false : b;
        if (this.filterEnabled != b) {
            // since filter is final and filterEnabled is initially false
            assert filter != null;
            this.filterEnabled = b;
            if (this.filterEnabled)
                filter.addWeakListener(this.filterListener);
            else
                filter.rmListener(this.filterListener);
            updateFilterWhere();
        }
    }

    private void updateFilterWhere() {
        if (this.filterEnabled) {
            this.setFilterWhere(getFilter().getLeaf(), getFilter().getPath(getPrimaryTable()));
        } else
            this.setFilterWhere(null, null);
    }

    private synchronized void setFilterWhere(final Set<SQLRow> w, final Path p) {
        if (!CompareUtils.equals(this.filterInfo.get0(), w) || !CompareUtils.equals(this.filterInfo.get1(), p)) {
            // shall we filter : w==null => no filter selected, p==null => filter doesn't affect us
            if (w == null || p == null) {
                this.filterInfo = Tuple2.create(null, null);
            } else {
                this.filterInfo = Tuple2.create((Set<SQLRow>) new HashSet<SQLRow>(w), p);
            }
            fireWhereChange();
        }
    }

    public final SQLRowValues getValues(int id) {
        final List<SQLRowValues> res = getValues(new Where(this.getPrimaryTable().getKey(), "=", id));
        if (res.size() > 1)
            throw new IllegalStateException("there's more than one line which has ID " + id + " for " + this + " : " + res);
        return CollectionUtils.getFirst(res);
    }

    public final List<SQLRowValues> getValues() {
        return this.getValues(null);
    }

    protected List<SQLRowValues> getValues(final Where w) {
        return getFetcher(w).fetch();
    }

    protected final List<SQLRowValues> fetchValues(final SQLRowValuesListFetcher f, final Where w) {
        return this.setupFetcher(f, w).fetch();
    }

    @Override
    protected SQLSelect transformSelect(SQLSelect sel) {
        final Tuple2<Set<SQLRow>, Path> filterInfo = getFilterInfo();
        // the filter is not empty and it concerns us
        if (filterInfo.get1() != null) {
            final Tuple2<SQLTable, Set<Number>> tableNids = rows2ids(filterInfo.get0());
            final SQLTable filterTable = tableNids.get0();

            final Path path = filterInfo.get1();
            final TableRef lastAlias = sel.assurePath(getPrimaryTable().getName(), path);
            if (filterTable != lastAlias.getTable())
                throw new IllegalStateException("table mismatch: " + filterTable + " is not from " + lastAlias + ": " + lastAlias.getTable());

            sel.andWhere(new Where(lastAlias.getKey(), tableNids.get1()));
        }
        return super.transformSelect(sel);
    }

    /**
     * The filter row acting on this request.
     * 
     * @return the row or <code>null</code> if there's no filter or the filter do not affect this.
     */
    public final Set<SQLRow> getFilterRows() {
        return this.getFilterInfo().get0();
    }

    // when the filter doesn't affect us, do not return the filter row
    private synchronized final Tuple2<Set<SQLRow>, Path> getFilterInfo() {
        return this.filterInfo;
    }

    // the where applied to all request (as opposed to the one passed to getFillRequest())
    public final Where getInstanceWhere() {
        return this.getFetcher(null).getReq().getWhere();
    }
}
