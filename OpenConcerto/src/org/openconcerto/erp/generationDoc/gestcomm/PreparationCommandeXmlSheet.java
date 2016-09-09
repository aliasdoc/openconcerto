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
 
 package org.openconcerto.erp.generationDoc.gestcomm;

import org.openconcerto.sql.model.SQLRow;

public class PreparationCommandeXmlSheet extends CommandeXmlSheet {

    public PreparationCommandeXmlSheet(SQLRow row) {
        super(row);
    }

    public static final String TEMPLATE_ID = "PreparationCommande";
    public static final String TEMPLATE_PROPERTY_NAME = "LocationCmd";

    @Override
    public String getDefaultTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public String getTemplateId() {
        return TEMPLATE_ID;
    }

    @Override
    public String getName() {
        return "PreparationCommande_" + row.getString("NUMERO");
    }
}
