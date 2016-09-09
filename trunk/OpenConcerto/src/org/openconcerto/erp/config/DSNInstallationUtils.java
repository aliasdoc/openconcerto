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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowListRSH;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSyntax;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.Inserter;
import org.openconcerto.sql.request.Inserter.Insertion;
import org.openconcerto.sql.utils.AlterTable;
import org.openconcerto.sql.utils.ReOrder;
import org.openconcerto.sql.utils.SQLCreateTable;
import org.openconcerto.utils.Tuple2;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DSNInstallationUtils {

    private void insertUndef(final SQLCreateTable ct) throws SQLException {
        // check that we can use insertReturnFirstField()
        if (ct.getPrimaryKey().size() != 1)
            throw new IllegalStateException("Not one and only one field in the PK : " + ct.getPrimaryKey());
        final Insertion<?> insertion = new Inserter(ct).insertReturnFirstField("(" + SQLBase.quoteIdentifier(SQLSyntax.ORDER_NAME) + ") VALUES(" + ReOrder.MIN_ORDER + ")", false);
        assert insertion.getCount() == 1;
        if (insertion.getRows().size() != 1)
            throw new IllegalStateException("Missing ID " + insertion.getRows());
        SQLTable.setUndefID(ct.getRoot().getSchema(), ct.getName(), ((Number) insertion.getRows().get(0)).intValue());
    }

    private void insertValues(List<Tuple2<String, String>> values, SQLTable table) throws SQLException {
        for (Tuple2<String, String> tuple2 : values) {
            SQLRowValues rowVals = new SQLRowValues(table);
            rowVals.put("CODE", tuple2.get0());
            rowVals.put("NOM", tuple2.get1());
            rowVals.commit();
        }
    }

    public void updateDSNCommonTable(final DBRoot root) throws SQLException {

        SQLTable societeCommonT = root.getTable("SOCIETE_COMMON");
        if (!societeCommonT.contains("IBAN")) {
            AlterTable t = new AlterTable(societeCommonT);
            t.addVarCharColumn("IBAN", 256);
            t.addVarCharColumn("BIC", 256);
            root.getBase().getDataSource().execute(t.asString());
            root.refetchTable("SOCIETE_COMMON");
            root.getSchema().updateVersion();
        }
        if (!societeCommonT.contains("ORG_PROTECTION_SOCIAL_ID")) {
            AlterTable t = new AlterTable(societeCommonT);
            t.addVarCharColumn("ORG_PROTECTION_SOCIAL_ID", 256);
            root.getBase().getDataSource().execute(t.asString());
            root.refetchTable("SOCIETE_COMMON");
            root.getSchema().updateVersion();
        }

        SQLTable tableRubCot = root.getTable("RUBRIQUE_COTISATION");
        if (!tableRubCot.contains("ASSIETTE_PLAFONNEE")) {
            AlterTable tableRub = new AlterTable(tableRubCot);
            tableRub.addBooleanColumn("ASSIETTE_PLAFONNEE", false, false);
            root.getBase().getDataSource().execute(tableRub.asString());
            root.refetchTable("RUBRIQUE_COTISATION");
            root.getSchema().updateVersion();
        }

        if (!root.contains("CODE_CAISSE_TYPE_RUBRIQUE")) {
            final SQLCreateTable createTableCode = new SQLCreateTable(root, "CODE_CAISSE_TYPE_RUBRIQUE");
            createTableCode.addVarCharColumn("CODE", 25);
            createTableCode.addVarCharColumn("NOM", 512);
            createTableCode.addVarCharColumn("CAISSE_COTISATION", 512);

            try {
                root.getBase().getDataSource().execute(createTableCode.asString());
                insertUndef(createTableCode);
                root.refetchTable("CODE_CAISSE_TYPE_RUBRIQUE");

                final SQLTable table = root.getTable("CODE_CAISSE_TYPE_RUBRIQUE");

                DsnUrssafCode codeUrssaf = new DsnUrssafCode();
                codeUrssaf.insertCode(table);

                List<String> tableRubName = Arrays.asList("RUBRIQUE_BRUT", "RUBRIQUE_COTISATION", "RUBRIQUE_NET");
                for (String t : tableRubName) {
                    AlterTable tableRub = new AlterTable(root.getTable(t));
                    tableRub.addForeignColumn("ID_CODE_CAISSE_TYPE_RUBRIQUE", table);
                    root.getBase().getDataSource().execute(tableRub.asString());
                    root.refetchTable(t);
                }
                root.getSchema().updateVersion();

            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_CAISSE_TYPE_RUBRIQUE", ex);
            }
        }

        if (!root.contains("MOTIF_ARRET_TRAVAIL")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "MOTIF_ARRET_TRAVAIL");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("MOTIF_ARRET_TRAVAIL");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("MOTIF_ARRET_TRAVAIL");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "maladie"));
                v.add(Tuple2.create("02", "maternité /adoption"));
                v.add(Tuple2.create("03", "paternité / accueil de l’enfant"));
                v.add(Tuple2.create("04", "congé suite à un accident de trajet"));
                v.add(Tuple2.create("05", "congé suite à maladie professionnelle"));
                v.add(Tuple2.create("06", "congé suite à accident de travail ou de service"));
                v.add(Tuple2.create("07", "femme enceinte dispensée de travail"));
                v.add(Tuple2.create("99", "annulation"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "MOTIF_ARRET_TRAVAIL", ex);
            }
        }

        if (!root.contains("CODE_BASE_ASSUJETTIE")) {
            final SQLCreateTable createTableCodeBase = new SQLCreateTable(root, "CODE_BASE_ASSUJETTIE");
            createTableCodeBase.addVarCharColumn("CODE", 25);
            createTableCodeBase.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableCodeBase.asString());
                insertUndef(createTableCodeBase);
                root.refetchTable("CODE_BASE_ASSUJETTIE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CODE_BASE_ASSUJETTIE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();

                v.add(Tuple2.create("02", "Assiette brute plafonnée"));
                v.add(Tuple2.create("03", "Assiette brute déplafonnée"));
                v.add(Tuple2.create("04", "Assiette de la contribution sociale généralisée"));
                v.add(Tuple2.create("07", "Assiette des contributions d'Assurance Chômage"));
                v.add(Tuple2.create("08", "Assiette retraite CPRP SNCF"));
                v.add(Tuple2.create("09", "Assiette de compensation bilatérale maladie CPRP SNCF"));
                v.add(Tuple2.create("10", "Base brute fiscale"));
                v.add(Tuple2.create("11", "Base forfaitaire soumise aux cotisations de Sécurité Sociale"));
                v.add(Tuple2.create("12", "Assiette du crédit d'impôt compétitivité-emploi"));
                v.add(Tuple2.create("13", "Assiette du forfait social à 8%"));
                v.add(Tuple2.create("14", "Assiette du forfait social à 20%"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_BASE_ASSUJETTIE", ex);
            }
        }
        if (!tableRubCot.contains("ID_CODE_BASE_ASSUJETTIE")) {
            AlterTable alterTableCot = new AlterTable(tableRubCot);
            alterTableCot.addForeignColumn("ID_CODE_BASE_ASSUJETTIE", root.getTable("CODE_BASE_ASSUJETTIE"));
            root.getBase().getDataSource().execute(alterTableCot.asString());
            root.refetchTable("RUBRIQUE_COTISATION");
            root.getSchema().updateVersion();
        }

        if (!root.contains("CODE_TYPE_RUBRIQUE_BRUT")) {
            final SQLCreateTable createTableCodeBase = new SQLCreateTable(root, "CODE_TYPE_RUBRIQUE_BRUT");
            createTableCodeBase.addVarCharColumn("CODE", 25);
            createTableCodeBase.addVarCharColumn("NOM", 512);
            createTableCodeBase.addVarCharColumn("TYPE", 512);

            try {
                root.getBase().getDataSource().execute(createTableCodeBase.asString());
                insertUndef(createTableCodeBase);
                root.refetchTable("CODE_TYPE_RUBRIQUE_BRUT");
                root.getSchema().updateVersion();

                DsnBrutCode brutCode = new DsnBrutCode();
                brutCode.insertCode(root.getTable("CODE_TYPE_RUBRIQUE_BRUT"));
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CODE_BASE_ASSUJETTIE", ex);
            }
        }

        SQLTable tableRubBrut = root.getTable("RUBRIQUE_BRUT");
        if (!tableRubBrut.contains("ID_CODE_TYPE_RUBRIQUE_BRUT")) {

            AlterTable alterTableBrut = new AlterTable(tableRubBrut);
            alterTableBrut.addForeignColumn("ID_CODE_TYPE_RUBRIQUE_BRUT", root.getTable("CODE_TYPE_RUBRIQUE_BRUT"));
            root.getBase().getDataSource().execute(alterTableBrut.asString());
            root.refetchTable("RUBRIQUE_BRUT");
            root.getSchema().updateVersion();
        }

        if (!root.contains("DSN_REGIME_LOCAL")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "DSN_REGIME_LOCAL");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("DSN_REGIME_LOCAL");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("DSN_REGIME_LOCAL");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("99", "non applicable"));
                v.add(Tuple2.create("01", "régime local Alsace Moselle"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DSN_REGIME_LOCAL", ex);
            }
        }

        if (!root.contains("CONTRAT_MODALITE_TEMPS")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_MODALITE_TEMPS");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_MODALITE_TEMPS");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_MODALITE_TEMPS");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("10", "temps plein"));
                v.add(Tuple2.create("20", "temps partiel"));
                v.add(Tuple2.create("21", "temps partiel thérapeutique"));
                v.add(Tuple2.create("99", "salarié non concerné"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_MODALITE_TEMPS", ex);
            }
        }

        if (!root.contains("CONTRAT_REGIME_MALADIE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_REGIME_MALADIE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_REGIME_MALADIE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_REGIME_MALADIE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("134", "régime spécial de la SNCF"));
                v.add(Tuple2.create("135", "régime spécial de la RATP"));
                v.add(Tuple2.create("136", "établissement des invalides de la marine (ENIM)"));
                v.add(Tuple2.create("137", "mineurs ou assimilés (CANMSS)"));
                v.add(Tuple2.create("138", "militaires de carrière (CNMSS)"));
                v.add(Tuple2.create("140", "clercs et employés de notaires (CRPCEN)"));
                v.add(Tuple2.create("141", "chambre de commerce et d'industrie de Paris"));
                v.add(Tuple2.create("144", "Assemblée Nationale"));
                v.add(Tuple2.create("145", "Sénat"));
                v.add(Tuple2.create("146", "port autonome de Bordeaux"));
                v.add(Tuple2.create("147", "industries électriques et gazières (CAMIEG)"));
                v.add(Tuple2.create("149", "régimes des cultes (CAVIMAC)"));
                v.add(Tuple2.create("200", "régime général (CNAM)"));
                v.add(Tuple2.create("300", "régime agricole (MSA)"));
                v.add(Tuple2.create("400", "régime spécial Banque de France"));
                v.add(Tuple2.create("900", "autre régime (réservé Polynésie Française, Nouvelle Calédonie)"));
                v.add(Tuple2.create("999", "autre"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_REGIME_MALADIE", ex);
            }
        }

        if (!root.contains("CONTRAT_REGIME_VIEILLESSE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_REGIME_VIEILLESSE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_REGIME_VIEILLESSE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_REGIME_VIEILLESSE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("120", "retraite des agents des collectivités locales (CNRACL)"));
                v.add(Tuple2.create("121", "pensions des ouvriers des établissements industriels de l'Etat (FSPOEIE)"));
                v.add(Tuple2.create("122", "pensions civiles et militaires de retraite de l'Etat (SRE)"));
                v.add(Tuple2.create("134", "régime spécial de la SNCF"));
                v.add(Tuple2.create("135", "régime spécial de la RATP"));
                v.add(Tuple2.create("136", "établissement des invalides de la marine (ENIM)"));
                v.add(Tuple2.create("137", "mineurs ou assimilés (fonds Caisse des Dépôts)"));
                v.add(Tuple2.create("139", "Banque de France"));
                v.add(Tuple2.create("140", "clercs et employés de notaires (CRPCEN)"));
                v.add(Tuple2.create("141", "chambre de commerce et d'industrie de Paris"));
                v.add(Tuple2.create("144", "Assemblée Nationale"));
                v.add(Tuple2.create("145", "Sénat"));
                v.add(Tuple2.create("147", "industries électriques et gazières (CNIEG)"));
                v.add(Tuple2.create("149", "régime des cultes (CAVIMAC)"));
                v.add(Tuple2.create("157", "régime de retraite des avocats (CNBF)"));
                v.add(Tuple2.create("158", "SEITA"));
                v.add(Tuple2.create("159", "Comédie Française"));
                v.add(Tuple2.create("160", "Opéra de Paris"));
                v.add(Tuple2.create("200", "régime général (CNAV)"));
                v.add(Tuple2.create("300", "régime agricole (MSA)"));
                v.add(Tuple2.create("900", "autre régime (réservé Polynésie Française, Nouvelle Calédonie, Principauté de Monaco)"));
                v.add(Tuple2.create("903", "salariés étrangers exemptés d'affiliation pour le risque vieillesse"));
                v.add(Tuple2.create("999", "cas particuliers d'affiliation"));

                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_REGIME_VIEILLESSE", ex);
            }
        }

        if (!root.contains("CONTRAT_MOTIF_RECOURS")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_MOTIF_RECOURS");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_MOTIF_RECOURS");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_MOTIF_RECOURS");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "Remplacement d'un salarié"));
                v.add(Tuple2.create("02", "Accroissement temporaire de l'activité de l'entreprise"));
                v.add(Tuple2.create("03", "Emplois à caractère saisonnier"));
                v.add(Tuple2.create("04", "Contrat vendanges"));
                v.add(Tuple2.create("05", "Contrat à durée déterminée d’usage"));
                v.add(Tuple2.create("06", "Contrat à durée déterminée à objet défini"));
                v.add(Tuple2.create("07", "Remplacement d'un chef d'entreprise"));
                v.add(Tuple2.create("08", "Remplacement du chef d'une exploitation agricole"));
                v.add(Tuple2.create("09", "Recrutement de personnes sans emploi rencontrant des difficultés sociales et professionnelles particulières"));
                v.add(Tuple2.create("10", "Complément de formation professionnelle au salarié"));
                v.add(Tuple2.create("11",
                        "Formation professionnelle au salarié par la voie de l'apprentissage, en vue de l'obtention d'une qualification professionnelle sanctionnée par un diplôme ou un titre à finalité professionnelle enregistré au répertoire national des certifications professionnelles"));
                v.add(Tuple2.create("12", "Remplacement d’un salarié passé provisoirement à temps partiel"));
                v.add(Tuple2.create("13", "Attente de la suppression définitive du poste du salarié ayant quitté définitivement l’entreprise"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_MOTIF_RECOURS", ex);
            }
        }

        if (!root.contains("CONTRAT_DETACHE_EXPATRIE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_DETACHE_EXPATRIE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_DETACHE_EXPATRIE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_DETACHE_EXPATRIE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "Détaché"));
                v.add(Tuple2.create("02", "Expatrié"));
                v.add(Tuple2.create("03", "Frontalier"));
                v.add(Tuple2.create("99", "Salarié non concerné"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_DETACHE_EXPATRIE", ex);
            }
        }

        if (!root.contains("DSN_NATURE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "DSN_NATURE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("DSN_NATURE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("DSN_NATURE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "DSN Mensuelle"));
                v.add(Tuple2.create("02", "Signalement Fin du contrat de travail"));
                v.add(Tuple2.create("04", "Signalement Arrêt de travail"));
                v.add(Tuple2.create("05", "Signalement Reprise suite à arrêt de travail"));
                v.add(Tuple2.create("06", "DSN reprise d'historique"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DSN_NATURE", ex);
            }
        }

        if (!root.contains("DSN_TYPE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "DSN_TYPE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("DSN_TYPE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("DSN_TYPE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "déclaration normale"));
                v.add(Tuple2.create("02", "déclaration normale néant"));
                v.add(Tuple2.create("03", "déclaration annule et remplace intégral"));
                v.add(Tuple2.create("04", "déclaration annule"));
                v.add(Tuple2.create("05", "annule et remplace néant"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DSN_TYPE", ex);
            }
        }

        if (!root.contains("CONTRAT_DISPOSITIF_POLITIQUE")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "CONTRAT_DISPOSITIF_POLITIQUE");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("CONTRAT_DISPOSITIF_POLITIQUE");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("CONTRAT_DISPOSITIF_POLITIQUE");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("21", "CUI - Contrat Initiative Emploi"));
                v.add(Tuple2.create("41", "CUI - Contrat d'Accompagnement dans l'Emploi"));
                v.add(Tuple2.create("42", "CUI - Contrat d'accès à l'emploi - DOM"));
                v.add(Tuple2.create("50", "Emploi d'avenir secteur marchand"));
                v.add(Tuple2.create("51", "Emploi d'avenir secteur non marchand"));
                v.add(Tuple2.create("61", "Contrat de Professionnalisation"));
                v.add(Tuple2.create("64", "Contrat d'apprentissage entreprises artisanales ou de moins de 11 salariés (loi du 3 janvier 1979)"));
                v.add(Tuple2.create("65", "Contrat d’apprentissage entreprises non inscrites au répertoire des métiers d’au moins 11 salariés (loi de 1987)"));
                v.add(Tuple2.create("70", "Contrat à durée déterminée pour les séniors"));
                v.add(Tuple2.create("71", "Contrat à durée déterminée d’insertion"));
                v.add(Tuple2.create("80", "Contrat de génération"));
                v.add(Tuple2.create("81", "Contrat d'apprentissage secteur public (Loi de 1992)"));
                v.add(Tuple2.create("82", "Contrat à durée indéterminée intérimaire"));
                v.add(Tuple2.create("99", "Non concerné"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "CONTRAT_DISPOSITIF_POLITIQUE", ex);
            }
        }

        if (!root.contains("MOTIF_REPRISE_ARRET_TRAVAIL")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "MOTIF_REPRISE_ARRET_TRAVAIL");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("MOTIF_REPRISE_ARRET_TRAVAIL");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("MOTIF_REPRISE_ARRET_TRAVAIL");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("01", "reprise normale"));
                v.add(Tuple2.create("02", "reprise temps partiel thérapeutique"));
                v.add(Tuple2.create("03", "reprise temps partiel raison personnelle"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "MOTIF_REPRISE_ARRET_TRAVAIL", ex);
            }
        }

        if (!root.contains("MOTIF_FIN_CONTRAT")) {
            final SQLCreateTable createTableMotif = new SQLCreateTable(root, "MOTIF_FIN_CONTRAT");
            createTableMotif.addVarCharColumn("CODE", 25);
            createTableMotif.addVarCharColumn("NOM", 512);

            try {
                root.getBase().getDataSource().execute(createTableMotif.asString());
                insertUndef(createTableMotif);
                root.refetchTable("MOTIF_FIN_CONTRAT");
                root.getSchema().updateVersion();

                final SQLTable table = root.getTable("MOTIF_FIN_CONTRAT");
                List<Tuple2<String, String>> v = new ArrayList<Tuple2<String, String>>();
                v.add(Tuple2.create("011", "licenciement suite à liquidation judiciaire ou à redressement judiciaire"));
                v.add(Tuple2.create("012", "licenciement suite à fermeture définitive de l'établissement"));
                v.add(Tuple2.create("014", "licenciement pour motif économique"));
                v.add(Tuple2.create("015", "licenciement pour fin de chantier"));
                v.add(Tuple2.create("020", "licenciement pour autre motif"));
                v.add(Tuple2.create("025", "autre fin de contrat pour motif économique"));
                v.add(Tuple2.create("026", "rupture pour motif économique dans le cadre d’un contrat de sécurisation professionnelle CSP"));
                v.add(Tuple2.create("031", "fin de contrat à durée déterminée ou fin d'accueil occasionnel"));
                v.add(Tuple2.create("032", "fin de mission d'intérim"));
                v.add(Tuple2.create("033", "rupture anticipée d’un CDD ou d’un contrat de mission en cas d’inaptitude physique constatée par le médecin du travail"));
                v.add(Tuple2.create("034", "fin de période d'essai à l'initiative de l'employeur"));
                v.add(Tuple2.create("035", "fin de période d'essai à l'initiative du salarié"));
                v.add(Tuple2.create("036", " rupture anticipée d'un CDD, d'un contrat d'apprentissage ou d’un contrat de mission à l'initiative de l'employeur"));
                v.add(Tuple2.create("037", "rupture anticipée d'un CDD, d'un contrat d'apprentissage ou d’un contrat de mission à l'initiative du salarié"));
                v.add(Tuple2.create("038", "mise à la retraite par l'employeur"));
                v.add(Tuple2.create("039", "départ à la retraite à l'initiative du salarié"));
                v.add(Tuple2.create("043", "rupture conventionnelle"));
                v.add(Tuple2.create("058", "prise d'acte de la rupture de contrat de travail"));
                v.add(Tuple2.create("059", "démission"));
                v.add(Tuple2.create("065", "décès de l'employeur ou internement / conduit à un licenciement autre motif"));
                v.add(Tuple2.create("066", "décès du salarié / rupture force majeure"));
                v.add(Tuple2.create("081", "fin de contrat d'apprentissage"));
                v.add(Tuple2.create("082", "résiliation judiciaire du contrat de travail"));
                v.add(Tuple2.create("083", "rupture de contrat de travail ou d’un contrat de mission pour force majeure"));
                v.add(Tuple2.create("084", "rupture d'un commun accord du CDD, du contrat d'apprentissage ou d’un contrat de mission"));
                v.add(Tuple2.create("085", "fin de mandat"));
                v.add(Tuple2.create("086", "licenciement convention CATS"));
                v.add(Tuple2.create("087", "licenciement pour faute grave"));
                v.add(Tuple2.create("088", "licenciement pour faute lourde"));
                v.add(Tuple2.create("089", "licenciement pour force majeure"));
                v.add(Tuple2.create("091", "licenciement pour inaptitude physique d'origine non professionnelle"));
                v.add(Tuple2.create("092", "licenciement pour inaptitude physique d'origine professionnelle"));
                v.add(Tuple2.create("093", "licenciement suite à décision d'une autorité administrative"));
                v.add(Tuple2.create("094", "rupture anticipée du contrat de travail pour arrêt de tournage"));
                v.add(Tuple2.create("095", "rupture anticipée du contrat de travail ou d’un contrat de mission pour faute grave"));
                v.add(Tuple2.create("096", "rupture anticipée du contrat de travail ou d’un contrat de mission pour faute lourde"));
                v.add(Tuple2.create("097", "rupture anticipée d’un contrat de travail ou d’un contrat de mission suite à fermeture de l'établissement"));
                v.add(Tuple2.create("098", "retrait d'enfant"));
                v.add(Tuple2.create("998", "transfert du contrat de travail sans rupture du contrat vers un autre établissement n'effectuant pas encore de DSN"));
                v.add(Tuple2.create("999", "fin de relation avec l’employeur (autres que contrat de travail) pour les cas ne portant aucun impact sur l’Assurance chômage"));
                insertValues(v, table);
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "MOTIF_FIN_CONTRAT", ex);
            }
        }

        DSNUpdateRubrique dsnUpdateRubrique = new DSNUpdateRubrique(root);
        dsnUpdateRubrique.updateRubriqueCotisation();
    }

    public void updateDSN(final DBRoot root) throws SQLException {
        final SQLTable tableCodeStatutCat = root.getTable("CODE_STATUT_CATEGORIEL");
        SQLRow rowNonCadre = tableCodeStatutCat.getRow(4);
        if (rowNonCadre != null) {
            rowNonCadre.createEmptyUpdateRow().put("CODE", "04").commit();
        }
        SQLRow rowSansStatut = tableCodeStatutCat.getRow(4);
        if (rowSansStatut != null) {
            rowSansStatut.createEmptyUpdateRow().put("CODE", "99").commit();
        }

        if (!root.contains("ARRET_TRAVAIL")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "ARRET_TRAVAIL");
            createTable.addForeignColumn("SALARIE");
            createTable.addDateAndTimeColumn("DATE_DERNIER_JOUR_TRAV");
            createTable.addDateAndTimeColumn("DATE_FIN_PREV");
            createTable.addBooleanColumn("SUBROGATION", Boolean.FALSE, false);
            createTable.addDateAndTimeColumn("DATE_DEBUT_SUBROGATION");
            createTable.addDateAndTimeColumn("DATE_FIN_SUBROGATION");
            createTable.addForeignColumn("ID_MOTIF_ARRET_TRAVAIL", root.findTable("MOTIF_ARRET_TRAVAIL"));
            createTable.addForeignColumn("ID_MOTIF_REPRISE_ARRET_TRAVAIL", root.findTable("MOTIF_REPRISE_ARRET_TRAVAIL"));
            createTable.addDateAndTimeColumn("DATE_REPRISE");
            createTable.addDateAndTimeColumn("DATE_ACCIDENT");

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("ARRET_TRAVAIL");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "ARRET_TRAVAIL", ex);
            }
        }

        if (!root.contains("REPRISE_TRAVAIL")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "REPRISE_TRAVAIL");
            createTable.addForeignColumn("SALARIE");
            createTable.addDateAndTimeColumn("DATE_DERNIER_JOUR_TRAV");
            createTable.addDateAndTimeColumn("DATE_FIN_PREV");
            createTable.addBooleanColumn("SUBROGATION", Boolean.FALSE, false);
            createTable.addDateAndTimeColumn("DATE_DEBUT_SUBROGATION");
            createTable.addDateAndTimeColumn("DATE_FIN_SUBROGATION");
            createTable.addForeignColumn("ID_MOTIF_ARRET_TRAVAIL", root.findTable("MOTIF_ARRET_TRAVAIL"));
            createTable.addForeignColumn("ID_MOTIF_REPRISE_ARRET_TRAVAIL", root.findTable("MOTIF_REPRISE_ARRET_TRAVAIL"));
            createTable.addDateAndTimeColumn("DATE_REPRISE");
            createTable.addDateAndTimeColumn("DATE_ACCIDENT");
            createTable.addDateAndTimeColumn("DATE");
            createTable.addVarCharColumn("COMMENTAIRES", 2048);

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("REPRISE_TRAVAIL");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "REPRISE_TRAVAIL", ex);
            }
        }

        if (!root.contains("DECLARATION_DSN")) {

            final SQLCreateTable createTable = new SQLCreateTable(root, "DECLARATION_DSN");
            createTable.addForeignColumn("ID_DSN_NATURE", root.findTable("DSN_NATURE"));

            createTable.addDateAndTimeColumn("DATE");
            createTable.addDateAndTimeColumn("DATE_ENVOI");
            createTable.addBooleanColumn("TEST", Boolean.FALSE, false);
            createTable.addBooleanColumn("ENVOYE", Boolean.FALSE, false);
            createTable.addVarCharColumn("COMMENTAIRE", 1024);
            createTable.addVarCharColumn("DSN_FILE", 75000);
            createTable.addIntegerColumn("NUMERO", 1);
            createTable.addIntegerColumn("ANNEE", 2016);
            createTable.addForeignColumn("MOIS");

            try {
                root.getBase().getDataSource().execute(createTable.asString());
                insertUndef(createTable);
                root.refetchTable("DECLARATION_DSN");
                root.getSchema().updateVersion();
            } catch (SQLException ex) {
                throw new IllegalStateException("Erreur lors de la création de la table " + "DECLARATION_DSN", ex);
            }
        }

        SQLTable tableArret = root.getTable("ARRET_TRAVAIL");
        if (!tableArret.contains("DATE")) {
            AlterTable alter = new AlterTable(tableArret);
            alter.addDateAndTimeColumn("DATE");
            alter.addVarCharColumn("COMMENTAIRES", 2048);
            root.getBase().getDataSource().execute(alter.asString());
            root.refetchTable("ARRET_TRAVAIL");
            root.getSchema().updateVersion();
        }

        SQLTable tableDsn = root.getTable("DECLARATION_DSN");
        if (!tableDsn.contains("ID_ARRET_TRAVAIL")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_ARRET_TRAVAIL", root.findTable("ARRET_TRAVAIL"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableDsn.contains("ID_REPRISE_TRAVAIL")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_REPRISE_TRAVAIL", root.findTable("REPRISE_TRAVAIL"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableDsn.contains("ID_MOIS_REGUL")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addForeignColumn("ID_MOIS_REGUL", root.findTable("MOIS"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (!tableDsn.contains("ANNULE_REMPLACE")) {
            AlterTable alter = new AlterTable(tableDsn);
            alter.addBooleanColumn("ANNULE_REMPLACE", Boolean.FALSE, false);
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        // if (!root.contains("FIN_CONTRAT")) {
        //
        // final SQLCreateTable createTable = new SQLCreateTable(root, "FIN_CONTRAT");
        // createTable.addForeignColumn("SALARIE");
        // createTable.addDateAndTimeColumn("DATE_FIN");
        // createTable.addDateAndTimeColumn("DATE_FIN_PREV");
        // createTable.addBooleanColumn("SUBROGATION", Boolean.FALSE, false);
        // createTable.addDateAndTimeColumn("DATE_DEBUT_SUBROGATION");
        // createTable.addDateAndTimeColumn("DATE_FIN_SUBROGATION");
        // createTable.addForeignColumn("ID_MOTIF_ARRET_TRAVAIL",
        // root.findTable("MOTIF_ARRET_TRAVAIL"));
        // createTable.addForeignColumn("ID_MOTIF_REPRISE_ARRET_TRAVAIL",
        // root.findTable("MOTIF_REPRISE_ARRET_TRAVAIL"));
        // createTable.addDateAndTimeColumn("DATE_REPRISE");
        // createTable.addDateAndTimeColumn("DATE_ACCIDENT");
        //
        // try {
        // root.getBase().getDataSource().execute(createTable.asString());
        // insertUndef(createTable);
        // root.refetchTable("FIN_CONTRAT");
        // root.getSchema().updateVersion();
        // } catch (SQLException ex) {
        // throw new IllegalStateException("Erreur lors de la création de la table " +
        // "FIN_CONTRAT", ex);
        // }
        // }

        SQLTable tableContrat = root.getTable("CONTRAT_SALARIE");
        if (!tableContrat.contains("NUMERO")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addColumn("NUMERO", "varchar(" + 128 + ") default '00000' NOT NULL");
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (!tableContrat.contains("CODE_REGIME_RETRAITE_DSN")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addColumn("CODE_REGIME_RETRAITE_DSN", "varchar(" + 128 + ") default '00000' NOT NULL");
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (!tableContrat.contains("DATE_PREV_FIN")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addDateAndTimeColumn("DATE_PREV_FIN");
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        boolean updateContrat = false;
        if (!tableContrat.contains("ID_CONTRAT_MODALITE_TEMPS")) {
            updateContrat = true;
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_MODALITE_TEMPS", root.findTable("CONTRAT_MODALITE_TEMPS"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_REGIME_MALADIE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_REGIME_MALADIE", root.findTable("CONTRAT_REGIME_MALADIE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_REGIME_VIEILLESSE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_REGIME_VIEILLESSE", root.findTable("CONTRAT_REGIME_VIEILLESSE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_MOTIF_RECOURS")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_MOTIF_RECOURS", root.findTable("CONTRAT_MOTIF_RECOURS"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_DETACHE_EXPATRIE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_DETACHE_EXPATRIE", root.findTable("CONTRAT_DETACHE_EXPATRIE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }
        if (!tableContrat.contains("ID_CONTRAT_DISPOSITIF_POLITIQUE")) {
            AlterTable alter = new AlterTable(tableContrat);
            alter.addForeignColumn("ID_CONTRAT_DISPOSITIF_POLITIQUE", root.findTable("CONTRAT_DISPOSITIF_POLITIQUE"));
            root.getBase().getDataSource().execute(alter.asString());
            root.getSchema().updateVersion();
        }

        if (updateContrat) {
            root.refetchTable("CONTRAT_SALARIE");
            tableContrat = root.findTable("CONTRAT_SALARIE");
            SQLSelect sel = new SQLSelect();
            sel.addSelectStar(tableContrat);
            List<SQLRow> contrats = SQLRowListRSH.execute(sel);

            SQLSelect selModal = new SQLSelect();
            final SQLTable tableModaliteTemps = root.findTable("CONTRAT_MODALITE_TEMPS");
            selModal.addSelectStar(tableModaliteTemps);
            selModal.setWhere(new Where(tableModaliteTemps.getField("CODE"), "=", "10"));
            SQLRow contratModalite = SQLRowListRSH.execute(selModal).get(0);

            SQLSelect selMaladie = new SQLSelect();
            final SQLTable tableMaladie = root.findTable("CONTRAT_REGIME_MALADIE");
            selMaladie.addSelectStar(tableMaladie);
            selMaladie.setWhere(new Where(tableMaladie.getField("CODE"), "=", "200"));
            SQLRow contratMaladie = SQLRowListRSH.execute(selMaladie).get(0);

            SQLSelect selViel = new SQLSelect();
            final SQLTable tableViel = root.findTable("CONTRAT_REGIME_VIEILLESSE");
            selViel.addSelectStar(tableViel);
            selViel.setWhere(new Where(tableViel.getField("CODE"), "=", "200"));
            SQLRow contratViel = SQLRowListRSH.execute(selViel).get(0);

            SQLSelect selDetacheExp = new SQLSelect();
            final SQLTable tableDetacheExp = root.findTable("CONTRAT_DETACHE_EXPATRIE");
            selDetacheExp.addSelectStar(tableDetacheExp);
            selDetacheExp.setWhere(new Where(tableDetacheExp.getField("CODE"), "=", "99"));
            SQLRow contratDetacheExp = SQLRowListRSH.execute(selDetacheExp).get(0);

            SQLSelect selDispoPolitique = new SQLSelect();
            final SQLTable tableDispoPol = root.findTable("CONTRAT_DISPOSITIF_POLITIQUE");
            selDispoPolitique.addSelectStar(tableDispoPol);
            selDispoPolitique.setWhere(new Where(tableDispoPol.getField("CODE"), "=", "99"));
            SQLRow contratDispoPol = SQLRowListRSH.execute(selDispoPolitique).get(0);

            for (SQLRow contrat : contrats) {

                final SQLRowValues createEmptyUpdateRow = contrat.createEmptyUpdateRow();
                createEmptyUpdateRow.put("ID_CONTRAT_MODALITE_TEMPS", contratModalite.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_REGIME_MALADIE", contratMaladie.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_REGIME_VIEILLESSE", contratViel.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_DETACHE_EXPATRIE", contratDetacheExp.getID());
                createEmptyUpdateRow.put("ID_CONTRAT_DISPOSITIF_POLITIQUE", contratDispoPol.getID());
                createEmptyUpdateRow.commit();
            }
        }

    }

}
