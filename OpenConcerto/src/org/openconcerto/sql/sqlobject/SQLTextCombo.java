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
 
 package org.openconcerto.sql.sqlobject;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.sqlobject.itemview.RowItemViewComponent;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.ITextComboCache;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An ITextCombo with the cache from COMPLETION.
 * 
 * @author Sylvain CUAZ
 */
public class SQLTextCombo extends org.openconcerto.ui.component.ITextCombo implements RowItemViewComponent {

    public SQLTextCombo() {
        super();
    }

    public SQLTextCombo(boolean locked) {
        super(locked);
    }

    public SQLTextCombo(ComboLockedMode mode) {
        super(mode);
    }

    public void init(SQLRowItemView v) {
        final ITextComboCacheSQL cache = new ITextComboCacheSQL(v.getField());
        if (cache.isValid())
            this.initCache(cache);
    }

    static class ITextComboCacheSQL implements ITextComboCache {

        private final SQLField field;
        private final SQLTable t;
        private final List<String> cache;
        private boolean loadedOnce;

        public ITextComboCacheSQL(final SQLField f) {
            this.field = f;
            this.t = this.field.getDBRoot().findTable("COMPLETION");
            if (!this.isValid())
                Log.get().warning("no completion found for " + this.field);
            this.cache = new ArrayList<String>();
            this.loadedOnce = false;
        }

        public final boolean isValid() {
            return this.t != null;
        }

        private final SQLDataSource getDS() {
            return this.t.getDBSystemRoot().getDataSource();
        }

        @SuppressWarnings("unchecked")
        public List<String> loadCache() {
            final SQLSelect sel = new SQLSelect(this.t.getBase());
            sel.addSelect(this.t.getField("LABEL"));
            sel.setWhere(new Where(this.t.getField("CHAMP"), "=", this.field.getFullName()));
            this.cache.clear();
            this.cache.addAll(this.getDS().executeCol(sel.asString()));

            return this.cache;
        }

        public List<String> getCache() {
            if (!this.loadedOnce) {
                this.loadCache();
                this.loadedOnce = true;
            }
            return this.cache;
        }

        public void addToCache(String string) {
            if (!this.cache.contains(string)) {
                final Map<String, Object> m = new HashMap<String, Object>();
                m.put("CHAMP", this.field.getFullName());
                m.put("LABEL", string);
                try {
                    // the primary key is not generated so don't let SQLRowValues remove it.
                    new SQLRowValues(this.t, m).insert(true, false);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                this.cache.add(string);
            }
        }

        public void deleteFromCache(String string) {
            String req = "DELETE FROM \"COMPLETION\" WHERE \"CHAMP\"= " + SQLBase.quoteStringStd(this.field.getFullName()) + " AND \"LABEL\"=" + SQLBase.quoteStringStd(string);
            this.getDS().executeScalar(req);
        }

        @Override
        public String toString() {
            return this.getClass().getName() + " on " + this.field;
        }

    }
}
