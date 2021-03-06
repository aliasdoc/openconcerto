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
 
 package org.openconcerto.erp.rights;

public enum NXRights {
    LOCK_MENU_TEST("LOCK_MENU_TEST"), LOCK_MENU_PAYE("LOCK_MENU_PAYE"), LOCK_MENU_ACHAT("LOCK_MENU_ACHAT"), ACCES_GENERATION_POINTAGE("ACCES_GENERATION_POINTAGE"), ACCES_LISTE_POINTAGE(
            "ACCES_LISTE_POINTAGE"), ACCES_ALL_SOCIETE("ACCES_ALL_SOCIETE"), ACCES_IMPORT_CIEL("ACCES_IMPORT_CIEL"), ACCES_MENU_STAT("ACCES_MENU_STAT"), ACCES_MENU_STRUCTURE("ACCES_MENU_STRUCTURE"), GESTION_ENCAISSEMENT(
            "GESTION_ENCAISSEMENT"), ACCES_HISTORIQUE("ACCES_HISTORIQUE"), ACCES_RETOUR_AFFACTURAGE("ACCES_RETOUR_AFFACTURAGE"), ACCES_ENVOI_AFFACTURAGE("ACCES_ENVOI_AFFACTURAGE"), POLE_PRODUIT_NOT_RESTRICTED(
            "ACCES_POLE_PRODUIT_NON_RESTREINT");

    private String code;

    private NXRights(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }
}
