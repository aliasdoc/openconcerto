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
 
 package org.openconcerto.erp.utils;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.map.model.DatabaseAccessor;
import org.openconcerto.map.model.Ville;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// TODO use the one from Nego
public class NXDatabaseAccessor implements DatabaseAccessor {

    private final SQLTable tableVille;

    public NXDatabaseAccessor(ComptaPropsConfiguration comptaConf) {
        super();
        if (comptaConf.getRootSociete().contains("VILLE")) {
            this.tableVille = ComptaPropsConfiguration.getInstanceCompta().getRootSociete().getTable("VILLE");
        } else {
            this.tableVille = comptaConf.getBase().getTable("VILLE");
        }
    }

    @SuppressWarnings("unchecked")
    public List<Ville> read() {
        SQLSelect sel = new SQLSelect();
        sel.addSelectStar(this.tableVille);
        List<SQLRow> l = (List<SQLRow>) Configuration.getInstance().getBase().getDataSource().execute(sel.asString(), SQLRowListRSH.createFromSelect(sel));

        List<Ville> lResult = new ArrayList<Ville>();
        for (SQLRow row : l) {
            final String nom = row.getString("NOM");
            final String cp = row.getString("CODE_POSTAL");
            long pop = ((Number) row.getObject("POPULATION")).longValue();
            long x = ((Number) row.getObject("X_LAMBERT")).longValue();
            long y = ((Number) row.getObject("Y_LAMBERT")).longValue();
            Ville v = new Ville(nom, pop, x, y, cp);
            lResult.add(v);
        }

        return lResult;
    }

    public void store(Ville v) {
        SQLRowValues rowVals = new SQLRowValues(this.tableVille);
        rowVals.put("NOM", v.getName());
        rowVals.put("CODE_POSTAL", v.getCodepostal());
        rowVals.put("X_LAMBERT", v.getXLambert());
        rowVals.put("Y_LAMBERT", v.getYLambert());
        rowVals.put("POPULATION", v.getPopulation());
        try {
            rowVals.insert();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void delete(Ville v) {
        final Where w = new Where(this.tableVille.getField("NOM"), "=", v.getName()).and(new Where(this.tableVille.getField("CODE_POSTAL"), "=", v.getCodepostal()));
        this.tableVille.getDBSystemRoot().getDataSource().execute("DELETE FROM " + this.tableVille.getSQLName().quote() + " WHERE " + w);
    }

}
