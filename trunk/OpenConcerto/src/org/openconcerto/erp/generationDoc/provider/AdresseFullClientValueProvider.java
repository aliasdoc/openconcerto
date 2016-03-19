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

public class AdresseFullClientValueProvider extends AdresseClientProvider {

    private final int type;
    private final boolean withName;

    public AdresseFullClientValueProvider(int type, boolean withName) {
        this.type = type;
        this.withName = withName;
    }

    @Override
    public Object getValue(SpreadSheetCellValueContext context) {
        final SQLRowAccessor r = getAdresse(context.getRow(), this.type);
        String result = "";
        if (this.withName) {
            result = context.getRow().getForeign("ID_CLIENT").getString("NOM") + "\n";
        }
        if (r.getString("LIBELLE").trim().length() > 0) {
            result = r.getString("LIBELLE") + "\n";
        }
        if (r.getString("DEST").trim().length() > 0) {
            result = r.getString("DEST") + "\n";
        }
        if (r.getString("RUE").trim().length() > 0) {
            result += r.getString("RUE") + "\n";
        }
        result += "\n" + r.getString("CODE_POSTAL");
        result += " ";
        if (r.getTable().contains("DISTRICT")) {
            result += r.getString("DISTRICT") + " ";
        }
        result += r.getString("VILLE");
        if (r.getBoolean("HAS_CEDEX")) {
            result += " Cedex";
            String cedex = r.getString("CEDEX");
            if (cedex != null && cedex.trim().length() > 0) {
                result += " " + cedex;
            }
        }
        if (r.getTable().contains("PROVINCE")) {
            result += "\n";
            if (r.getString("PROVINCE").trim().length() > 0) {
                result += r.getString("PROVINCE") + " ";
            }

            if (r.getTable().contains("DEPARTEMENT")) {
                result += r.getString("DEPARTEMENT");
            }
        }

        if (r.getString("PAYS").trim().length() > 0) {
            result += "\n" + r.getString("PAYS");
        }

        return result;
    }

    public static void register() {
        SpreadSheetCellValueProviderManager.put("address.customer.full", new AdresseFullClientValueProvider(ADRESSE_PRINCIPALE, false));
        SpreadSheetCellValueProviderManager.put("address.customer.invoice.full", new AdresseFullClientValueProvider(ADRESSE_FACTURATION, false));
        SpreadSheetCellValueProviderManager.put("address.customer.shipment.full", new AdresseFullClientValueProvider(ADRESSE_LIVRAISON, false));
        SpreadSheetCellValueProviderManager.put("address.customer.full.withname", new AdresseFullClientValueProvider(ADRESSE_PRINCIPALE, true));
        SpreadSheetCellValueProviderManager.put("address.customer.invoice.full.withname", new AdresseFullClientValueProvider(ADRESSE_FACTURATION, true));
        SpreadSheetCellValueProviderManager.put("address.customer.shipment.full.withname", new AdresseFullClientValueProvider(ADRESSE_LIVRAISON, true));
    }
}
