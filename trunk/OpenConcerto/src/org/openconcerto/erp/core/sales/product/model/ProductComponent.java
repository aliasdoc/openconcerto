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
 
 package org.openconcerto.erp.core.sales.product.model;

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.DecimalUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ProductComponent {
    private final SQLRowAccessor product;
    private BigDecimal qty;

    public ProductComponent(SQLRowAccessor product, BigDecimal qty) {
        this.product = product;
        this.qty = qty;
    }

    public SQLRowAccessor getProduct() {
        return product;
    }

    public BigDecimal getQty() {
        return qty;
    }

    public void setQty(BigDecimal qty) {
        this.qty = qty;
    }

    public void addQty(BigDecimal b) {
        this.qty = qty.add(b);
    }

    /**
     * permet de valoriser les mouvements de stocks
     * 
     * @return
     */
    public BigDecimal getPRC(Date d) {
        if (product.getTable().getDBRoot().contains("ARTICLE_PRIX_REVIENT")) {
            SQLTable table = product.getTable().getDBRoot().getTable("ARTICLE_PRIX_REVIENT");
            Collection<SQLRow> prcs = product.asRow().getReferentRows(table);

            BigDecimal result = null;
            final List<PriceByQty> prices = new ArrayList<PriceByQty>();
            for (SQLRow row : prcs) {
                Calendar date = Calendar.getInstance();
                if (row.getObject("DATE") != null) {
                    date = row.getDate("DATE");
                }
                prices.add(new PriceByQty(row.getLong("QTE"), row.getBigDecimal("PRIX"), date.getTime()));
            }

            result = PriceByQty.getPriceForQty(qty.setScale(0, RoundingMode.HALF_UP).intValue(), prices, d);
            if (result == null) {
                // Can occur during editing
                result = BigDecimal.ZERO;
            }
            return result;
        }
        return null;
    }

    public static ProductComponent createFrom(SQLRowAccessor rowVals) {
        return createFrom(rowVals, 1);
    }

    public static ProductComponent createFrom(SQLRowAccessor rowVals, int qteMultiple) {

        final int qte = rowVals.getInt("QTE") * qteMultiple;
        final BigDecimal qteUV = rowVals.getBigDecimal("QTE_UNITAIRE");
        BigDecimal qteFinal = qteUV.multiply(new BigDecimal(qte), DecimalUtils.HIGH_PRECISION);
        return new ProductComponent(rowVals.getForeign("ID_ARTICLE"), qteFinal);
    }
}
