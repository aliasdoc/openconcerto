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
 
 package org.openconcerto.erp.generationDoc.provider;

import org.openconcerto.erp.generationDoc.SpreadSheetCellValueContext;
import org.openconcerto.erp.generationDoc.SpreadSheetCellValueProviderManager;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.DecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

public class RemiseTotalProvider extends UserInitialsValueProvider {

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        SQLRowAccessor row = context.getRow();

        final SQLTable table = row.getTable();
        Collection<? extends SQLRowAccessor> cols = row.getReferentRows(table.getTable(table.getName() + "_ELEMENT"));
        BigDecimal total = BigDecimal.ZERO;
        for (SQLRowAccessor sqlRowAccessor : cols) {

            final BigDecimal montant = sqlRowAccessor.getBigDecimal("MONTANT_REMISE");
            BigDecimal remise = (BigDecimal) sqlRowAccessor.getObject("POURCENT_REMISE");

            if (montant != null) {
                total = total.add(montant.setScale(2, RoundingMode.HALF_UP));
            } else if (remise != null && remise.signum() != 0) {
                final BigDecimal subtract = BigDecimal.ONE.subtract(remise.movePointLeft(2));
                if (subtract.signum() == 0) {
                    total = total.add(sqlRowAccessor.getBigDecimal("PV_HT").multiply(sqlRowAccessor.getBigDecimal("QTE_UNITAIRE").multiply(new BigDecimal(sqlRowAccessor.getInt("QTE")))));
                } else {
                    total = total.add((sqlRowAccessor.getBigDecimal("T_PV_HT").divide(subtract, DecimalUtils.HIGH_PRECISION)).subtract(sqlRowAccessor.getBigDecimal("T_PV_HT")).setScale(2,
                            RoundingMode.HALF_UP));
                }
            }
        }
        return total;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("sales.discount.total", new RemiseTotalProvider());
    }
}
