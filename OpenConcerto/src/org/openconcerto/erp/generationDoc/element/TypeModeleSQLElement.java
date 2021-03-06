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
 
 /*
 * Créé le 10 oct. 2011
 */
package org.openconcerto.erp.generationDoc.element;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.utils.ListMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TypeModeleSQLElement extends ConfSQLElement {

    public TypeModeleSQLElement(DBRoot root) {
        super(root.getTable("TYPE_MODELE"), "un type_modele ", "type_modeles");
    }

    public TypeModeleSQLElement() {
        this(Configuration.getInstance().getRoot());
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        l.add("TABLE");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("NOM");
        return l;
    }

    @Override
    public ListMap<String, String> getShowAs() {
        ListMap<String, String> map = new ListMap<String, String>();
        map.putCollection(null, "NOM");
        return map;
    }

    public SQLComponent createComponent() {
        return new UISQLComponent(this) {

            @Override
            protected Set<String> createRequiredNames() {
                final Set<String> s = new HashSet<String>();
                // s.add("NOM");
                // s.add("TABLE");
                return s;
            }

            public void addViews() {
                this.addView("NOM");
                this.addView("TABLE");
            }
        };
    }

    public String getDescription(SQLRow fromRow) {
        return fromRow.getString("NOM");
    }

    Map<String, String> template;

    public Map<String, String> getTemplateMapping() {

        if (this.template == null) {
            this.template = new HashMap<String, String>();
            SQLSelect sel = new SQLSelect(getTable().getBase());
            sel.addSelectStar(getTable());
            List<SQLRow> rows = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));
            for (SQLRow sqlRow : rows) {
                template.put(sqlRow.getString("TABLE"), sqlRow.getString("DEFAULT_MODELE"));
            }

        }

        return template;

    }

}
