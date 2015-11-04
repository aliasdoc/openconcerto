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
 
 package org.openconcerto.erp.core.finance.accounting.model;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.Order;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.utils.DecimalUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class CurrencyConverter {
    private String companyCurrencyCode;
    private final DBRoot root;

    private String baseCurrencyCode;

    public CurrencyConverter(DBRoot rootSociete, String companyCurrencyCode, String baseCurrencyCode) {
        if (companyCurrencyCode == null || companyCurrencyCode.isEmpty()) {
            this.companyCurrencyCode = "EUR";
        } else {
            this.companyCurrencyCode = companyCurrencyCode.trim().toUpperCase();
        }
        this.baseCurrencyCode = baseCurrencyCode;
        this.root = rootSociete;
    }

    public CurrencyConverter() {
        this(ComptaPropsConfiguration.getInstanceCompta().getRootSociete(), ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getForeign("ID_DEVISE").getString("CODE"), "EUR");
    }

    public String getCompanyCurrencyCode() {
        return companyCurrencyCode;
    }

    public String getBaseCurrencyCode() {
        return baseCurrencyCode;
    }

    /**
     * Converter an amount to an other currency
     * */
    public BigDecimal convert(BigDecimal amount, String from, String to) {
        return convert(amount, from, to, Calendar.getInstance().getTime());
    }

    /**
     * Converter an amount to an other currency at a precise date
     * */
    public BigDecimal convert(BigDecimal amount, String from, String to, Date date) {
        return convert(amount, from, to, date, false);
    }

    /**
     * Converter an amount to an other currency at a precise date
     * */
    public BigDecimal convert(BigDecimal amount, String from, String to, Date date, boolean useBiased) {

        if (from.equalsIgnoreCase(to)) {
            return amount;
        }

        // Clean date
        final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        c.setTimeInMillis(date.getTime());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        final Date d = c.getTime();

        // Get conversion info
        final SQLSelect select = new SQLSelect();
        final SQLTable t = this.root.getTable("DEVISE_HISTORIQUE");
        select.addAllSelect(t, Arrays.asList("ID", "DATE", "SRC", "DST", "TAUX", "TAUX_COMMERCIAL"));
        Where w = new Where(t.getField("SRC"), "=", baseCurrencyCode);
        w = w.and(new Where(t.getField("DST"), true, Arrays.asList(from, to)));
        w = w.and(new Where(t.getField("DATE"), "<=", d));
        select.setWhere(w);
        select.addFieldOrder(t.getField("DATE"), Order.desc());
        select.setLimit(2);
        final List<SQLRow> rows = SQLRowListRSH.execute(select);
        if (rows.isEmpty()) {
            System.err.println("CurrencyConverter.convert() no data to convert " + amount + " " + from + " to " + to + " at " + date + " biased:" + useBiased);
            return null;
        }
        BigDecimal r1 = null;
        BigDecimal r2 = null;
        for (SQLRow sqlRow : rows) {
            if (sqlRow.getString("DST").equals(from)) {
                if (useBiased) {
                    r1 = sqlRow.getBigDecimal("TAUX_COMMERCIAL");
                } else {
                    r1 = sqlRow.getBigDecimal("TAUX");
                }
            }
            if (sqlRow.getString("DST").equals(to)) {
                if (useBiased) {
                    r2 = sqlRow.getBigDecimal("TAUX_COMMERCIAL");
                } else {
                    r2 = sqlRow.getBigDecimal("TAUX");
                }
            }
        }
        if (from.equals(this.baseCurrencyCode)) {
            r1 = BigDecimal.ONE;
        }
        if (to.equals(this.baseCurrencyCode)) {
            r2 = BigDecimal.ONE;
        }
        if (r1 == null) {
            throw new IllegalStateException("No conversion rate for " + from);
        }
        if (r2 == null) {
            throw new IllegalStateException("No conversion rate for " + to);
        }
        final BigDecimal result = amount.multiply(r2, DecimalUtils.HIGH_PRECISION).divide(r1, DecimalUtils.HIGH_PRECISION);
        return result;
    }
}
