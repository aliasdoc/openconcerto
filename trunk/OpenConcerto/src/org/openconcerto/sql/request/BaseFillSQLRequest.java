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

import org.openconcerto.sql.FieldExpander;
import org.openconcerto.sql.model.FieldRef;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValues.CreateMode;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSearchMode;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public abstract class BaseFillSQLRequest extends BaseSQLRequest implements Cloneable {

    private final static Pattern QUERY_SPLIT_PATTERN = Pattern.compile("\\s+");
    private static boolean DEFAULT_SELECT_LOCK = true;

    /**
     * Whether to use "FOR SHARE" in list requests (preventing roles with just SELECT right from
     * seeing the list).
     * 
     * @return <code>true</code> if select should obtain a lock.
     * @see SQLSelect#setWaitPreviousWriteTX(boolean)
     */
    public static final boolean getDefaultLockSelect() {
        return DEFAULT_SELECT_LOCK;
    }

    public static final void setDefaultLockSelect(final boolean b) {
        DEFAULT_SELECT_LOCK = b;
    }

    static public void setupForeign(final SQLRowValuesListFetcher fetcher) {
        // include rows having NULL (not undefined ID) foreign keys
        fetcher.setFullOnly(false);
        // treat the same way tables with or without undefined ID
        fetcher.setIncludeForeignUndef(false);
        // be predictable
        fetcher.setReferentsOrdered(true, true);
    }

    static public final void addToFetch(final SQLRowValues input, final Path p, final Collection<String> fields) {
        // don't back track : e.g. if path is SITE -> CLIENT <- SITE we want the siblings of SITE,
        // if we want fields of the primary SITE we pass the path SITE
        final SQLRowValues r = p == null ? input : input.followPathToOne(p, CreateMode.CREATE_ONE, false);
        for (final String f : fields) {
            // don't overwrite foreign rows
            if (!r.getFields().contains(f))
                r.put(f, null);
        }
    }

    private final SQLTable primaryTable;
    @GuardedBy("this")
    private Where where;
    @GuardedBy("this")
    private Map<SQLField, SQLSearchMode> searchFields;
    @GuardedBy("this")
    private List<String> searchQuery;
    @GuardedBy("this")
    private ITransformer<SQLSelect, SQLSelect> selTransf;
    @GuardedBy("this")
    private boolean lockSelect;

    @GuardedBy("this")
    private SQLRowValues graph;
    @GuardedBy("this")
    private SQLRowValues graphToFetch;

    @GuardedBy("this")
    private SQLRowValuesListFetcher frozen;

    {
        // a new instance is never frozen
        this.frozen = null;
    }

    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);

    public BaseFillSQLRequest(final SQLTable primaryTable, final Where w) {
        super();
        if (primaryTable == null)
            throw new NullPointerException();
        this.primaryTable = primaryTable;
        this.where = w;
        this.searchFields = Collections.emptyMap();
        this.searchQuery = Collections.emptyList();
        this.selTransf = null;
        this.lockSelect = getDefaultLockSelect();
        this.graph = null;
        this.graphToFetch = null;
    }

    public BaseFillSQLRequest(final BaseFillSQLRequest req) {
        super();
        this.primaryTable = req.getPrimaryTable();
        synchronized (req) {
            this.where = req.where;
            this.searchFields = req.searchFields;
            this.searchQuery = req.searchQuery;
            this.selTransf = req.selTransf;
            this.lockSelect = req.lockSelect;
            // use methods since they're both lazy
            this.graph = req.getGraph();
            this.graphToFetch = req.getGraphToFetch();
        }
    }

    public synchronized final boolean isFrozen() {
        return this.frozen != null;
    }

    public final void freeze() {
        this.freeze(this);
    }

    private final synchronized void freeze(final BaseFillSQLRequest from) {
        if (!this.isFrozen()) {
            // compute the fetcher once and for all
            this.frozen = from.getFetcher();
            assert this.frozen.isFrozen();
            this.wasFrozen();
        }
    }

    protected void wasFrozen() {
    }

    protected final void checkFrozen() {
        if (this.isFrozen())
            throw new IllegalStateException("this has been frozen: " + this);
    }

    // not final so we can narrow down the return type
    public BaseFillSQLRequest toUnmodifiable() {
        return this.toUnmodifiableP(this.getClass());
    }

    // should be passed the class created by cloneForFreeze(), i.e. not this.getClass() or this
    // won't support anonymous classes
    protected final <T extends BaseFillSQLRequest> T toUnmodifiableP(final Class<T> clazz) {
        final Class<? extends BaseFillSQLRequest> thisClass = this.getClass();
        if (clazz != thisClass && !(thisClass.isAnonymousClass() && clazz == thisClass.getSuperclass()))
            throw new IllegalArgumentException("Passed class isn't our class : " + clazz + " != " + thisClass);
        final BaseFillSQLRequest res;
        synchronized (this) {
            if (this.isFrozen()) {
                res = this;
            } else {
                res = this.clone(true);
                if (res.getClass() != clazz)
                    throw new IllegalStateException("Clone class mismatch : " + res.getClass() + " != " + clazz);
                // freeze before releasing lock (even if not recommended, allow to modify the state
                // of getSelectTransf() while holding our lock)
                // pass ourselves so that if we are an anonymous class the fetcher created with our
                // overloaded methods is used
                res.freeze(this);
            }
        }
        assert res.getClass() == clazz || res.getClass().getSuperclass() == clazz;
        @SuppressWarnings("unchecked")
        final T casted = (T) res;
        return casted;
    }

    @Override
    public BaseFillSQLRequest clone() {
        synchronized (this) {
            return this.clone(false);
        }
    }

    // must be called with our lock
    protected abstract BaseFillSQLRequest clone(boolean forFreeze);

    private final SQLRowValues computeGraph() {
        if (this.getFields() == null)
            return null;

        final SQLRowValues vals = new SQLRowValues(this.getPrimaryTable());
        for (final SQLField f : this.getFields()) {
            vals.put(f.getName(), null);
        }

        this.getShowAs().expand(vals);
        return vals.toImmutable();
    }

    /**
     * The graph computed by expanding {@link #getFields()} by {@link #getShowAs()}.
     * 
     * @return the expanded frozen graph.
     */
    public final SQLRowValues getGraph() {
        synchronized (this) {
            if (this.graph == null) {
                assert !this.isFrozen() : "no computation should take place after frozen()";
                this.graph = this.computeGraph();
            }
            return this.graph;
        }
    }

    // should be called if getFields(), getOrder() or getShowAs() change
    protected final void clearGraph() {
        synchronized (this) {
            checkFrozen();
            this.graph = null;
            this.graphToFetch = null;
        }
    }

    /**
     * The graph to fetch, should be a superset of {@link #getGraph()}. To modify it, see
     * {@link #addToGraphToFetch(Path, Set)} and {@link #changeGraphToFetch(IClosure)}.
     * 
     * @return the graph to fetch, frozen.
     */
    public final SQLRowValues getGraphToFetch() {
        synchronized (this) {
            if (this.graphToFetch == null && this.getGraph() != null) {
                assert !this.isFrozen() : "no computation should take place after frozen()";
                final SQLRowValues tmp = this.getGraph().deepCopy();
                this.customizeToFetch(tmp);
                // fetch order fields, so that consumers can order an updated row in an existing
                // list
                for (final Path orderP : this.getOrder()) {
                    final SQLRowValues orderVals = tmp.followPath(orderP);
                    if (orderVals != null && orderVals.getTable().isOrdered()) {
                        orderVals.put(orderVals.getTable().getOrderField().getName(), null);
                    }
                }
                this.graphToFetch = tmp.toImmutable();
            }
            return this.graphToFetch;
        }
    }

    public final void addToGraphToFetch(final String... fields) {
        this.addToGraphToFetch(Arrays.asList(fields));
    }

    public final void addToGraphToFetch(final Collection<String> fields) {
        this.addToGraphToFetch(null, fields);
    }

    public final void addForeignToGraphToFetch(final String foreignField, final Collection<String> fields) {
        this.addToGraphToFetch(new Path(getPrimaryTable()).addForeignField(foreignField), fields);
    }

    /**
     * Make sure that the fields at the end of the path are fetched.
     * 
     * @param p a path.
     * @param fields fields to fetch.
     */
    public final void addToGraphToFetch(final Path p, final Collection<String> fields) {
        this.changeGraphToFetch(new IClosure<SQLRowValues>() {
            @Override
            public void executeChecked(SQLRowValues input) {
                addToFetch(input, p, fields);
            }
        });
    }

    public final void changeGraphToFetch(IClosure<SQLRowValues> cl) {
        synchronized (this) {
            checkFrozen();
            final SQLRowValues tmp = this.getGraphToFetch().deepCopy();
            cl.executeChecked(tmp);
            this.graphToFetch = tmp.toImmutable();
        }
    }

    protected void customizeToFetch(final SQLRowValues graphToFetch) {
    }

    protected synchronized final SQLRowValuesListFetcher getFetcher() {
        if (this.isFrozen())
            return this.frozen;
        // graphToFetch can be modified freely so don't the use the simple constructor
        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(getGraphToFetch(), false);
        return setupFetcher(fetcher);
    }

    // allow to pass fetcher since they are mostly immutable (and for huge graphs they are slow to
    // create)
    protected final SQLRowValuesListFetcher setupFetcher(final SQLRowValuesListFetcher fetcher) {
        final String tableName = getPrimaryTable().getName();
        setupForeign(fetcher);
        synchronized (this) {
            fetcher.setOrder(getOrder());
            fetcher.setReturnedRowsUnmodifiable(true);
            fetcher.appendSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect sel) {
                    sel = transformSelect(sel);
                    if (isLockSelect())
                        sel.addLockedTable(tableName);
                    return sel.andWhere(getWhere());
                }
            });
            // freeze to execute setSelTransf() before leaving the synchronized block
            fetcher.freeze();
        }
        return fetcher;
    }

    protected List<Path> getOrder() {
        return Collections.singletonList(Path.get(getPrimaryTable()));
    }

    public final void setWhere(final Where w) {
        synchronized (this) {
            checkFrozen();
            this.where = w;
        }
        fireWhereChange();
    }

    public synchronized final Where getWhere() {
        return this.where;
    }

    /**
     * Whether this request is searchable.
     * 
     * @param b <code>true</code> if the {@link #getFields() local fields} should be used,
     *        <code>false</code> to not be searchable.
     */
    public final void setSearchable(final boolean b) {
        this.setSearchFields(b ? this.getFields() : Collections.<SQLField> emptyList());
    }

    /**
     * Set the fields used to search.
     * 
     * @param searchFields only rows with these fields containing the terms will match.
     * @see #setSearch(String)
     */
    public final void setSearchFields(final Collection<SQLField> searchFields) {
        this.setSearchFields(CollectionUtils.<SQLField, SQLSearchMode> createMap(searchFields));
    }

    /**
     * Set the fields used to search.
     * 
     * @param searchFields for each field to search, how to match.
     * @see #setSearch(String)
     */
    public final void setSearchFields(Map<SQLField, SQLSearchMode> searchFields) {
        // can be outside the synchronized block, since it can't be reverted
        checkFrozen();
        searchFields = new HashMap<SQLField, SQLSearchMode>(searchFields);
        final Iterator<Entry<SQLField, SQLSearchMode>> iter = searchFields.entrySet().iterator();
        while (iter.hasNext()) {
            final Entry<SQLField, SQLSearchMode> e = iter.next();
            if (!String.class.isAssignableFrom(e.getKey().getType().getJavaType())) {
                iter.remove();
            } else if (e.getValue() == null) {
                e.setValue(SQLSearchMode.CONTAINS);
            }
        }
        searchFields = Collections.unmodifiableMap(searchFields);
        synchronized (this) {
            this.searchFields = searchFields;
        }
        fireWhereChange();
    }

    public Map<SQLField, SQLSearchMode> getSearchFields() {
        synchronized (this) {
            return this.searchFields;
        }
    }

    /**
     * Set the search query. The query will be used to match rows using
     * {@link #setSearchFields(Map)}. I.e. if there's no field set, this method won't have any
     * effect.
     * 
     * @param s the search query.
     * @return <code>true</code> if the request changed.
     */
    public boolean setSearch(String s) {
        // no need to trim() since trailing empty strings are not returned
        final List<String> split = Arrays.asList(QUERY_SPLIT_PATTERN.split(s));
        boolean res = false;
        synchronized (this) {
            checkFrozen();
            if (!split.equals(this.searchQuery)) {
                this.searchQuery = split;
                if (!this.getSearchFields().isEmpty()) {
                    res = true;
                }
            }
        }
        if (res)
            this.fireWhereChange();
        return res;
    }

    public final synchronized void setLockSelect(boolean lockSelect) {
        checkFrozen();
        this.lockSelect = lockSelect;
    }

    public final synchronized boolean isLockSelect() {
        return this.lockSelect;
    }

    @Override
    public final Collection<SQLField> getAllFields() {
        // don't rely on the expansion of our fields, since our fetcher can be arbitrary modified
        // (eg by adding a where on a field of a non-displayed table)
        return this.getFetcher().getReq().getFields();
    }

    protected abstract Collection<SQLField> getFields();

    protected SQLSelect transformSelect(final SQLSelect sel) {
        final Map<SQLField, SQLSearchMode> searchFields;
        final List<String> searchQuery;
        synchronized (this) {
            searchFields = this.getSearchFields();
            searchQuery = this.searchQuery;
        }
        final Where w;
        final Set<String> matchScore = new HashSet<String>();
        if (!searchFields.isEmpty()) {
            Where where = null;
            for (final String searchTerm : searchQuery) {
                Where termWhere = null;
                for (final FieldRef selF : sel.getSelectFields()) {
                    final SQLSearchMode mode = searchFields.get(selF.getField());
                    if (mode != null) {
                        termWhere = Where.createRaw(createWhere(selF, mode, searchTerm)).or(termWhere);
                        if (!mode.equals(SQLSearchMode.EQUALS))
                            matchScore.add("case when " + createWhere(selF, SQLSearchMode.EQUALS, searchTerm) + " then 1 else 0 end");
                    }
                }
                where = Where.and(termWhere, where);
            }
            w = where;
        } else {
            w = null;
        }
        sel.andWhere(w);
        if (!matchScore.isEmpty())
            sel.getOrder().add(0, CollectionUtils.join(matchScore, " + ") + " DESC");

        final ITransformer<SQLSelect, SQLSelect> transf = this.getSelectTransf();
        return transf == null ? sel : transf.transformChecked(sel);
    }

    protected String createWhere(final FieldRef selF, final SQLSearchMode mode, final String searchQuery) {
        return "lower(" + selF.getFieldRef() + ") " + mode.generateSQL(selF.getField().getDBRoot(), searchQuery.toLowerCase());
    }

    public final synchronized ITransformer<SQLSelect, SQLSelect> getSelectTransf() {
        return this.selTransf;
    }

    /**
     * Allows to transform the SQLSelect returned by getFillRequest().
     * 
     * @param transf the transformer to apply, needs to be thread-safe.
     */
    public final void setSelectTransf(final ITransformer<SQLSelect, SQLSelect> transf) {
        synchronized (this) {
            checkFrozen();
            this.selTransf = transf;
        }
        this.fireWhereChange();
    }

    protected abstract FieldExpander getShowAs();

    public final SQLTable getPrimaryTable() {
        return this.primaryTable;
    }

    protected final void fireWhereChange() {
        // don't call unknown code with our lock
        assert !Thread.holdsLock(this);
        this.supp.firePropertyChange("where", null, null);
    }

    public final void addWhereListener(final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener("where", l);
    }

    public final void rmWhereListener(final PropertyChangeListener l) {
        this.supp.removePropertyChangeListener("where", l);
    }

    @Override
    public String toString() {
        return this.getClass().getName() + " on " + this.getPrimaryTable();
    }
}
