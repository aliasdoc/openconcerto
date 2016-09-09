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
 
 package org.openconcerto.erp.core.sales.shipment.ui;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.ui.AbstractVenteArticleItemTable;
import org.openconcerto.erp.core.common.ui.Acompte;
import org.openconcerto.erp.core.common.ui.AcompteCellEditor;
import org.openconcerto.erp.core.common.ui.DeviseNumericHTConvertorCellEditor;
import org.openconcerto.erp.core.common.ui.DeviseTableCellRenderer;
import org.openconcerto.erp.core.common.ui.QteCellEditor;
import org.openconcerto.erp.core.common.ui.Remise;
import org.openconcerto.erp.core.finance.accounting.model.CurrencyConverter;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.ui.ArticleRowValuesRenderer;
import org.openconcerto.erp.core.sales.product.ui.QteUnitRowValuesRenderer;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ITextArticleWithCompletionCellEditor;
import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.sql.view.list.SQLTextComboTableCellEditor;
import org.openconcerto.sql.view.list.ValidStateChecker;
import org.openconcerto.ui.table.XTableColumnModel;
import org.openconcerto.utils.DecimalUtils;

import java.awt.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

public class BonDeLivraisonItemTable extends AbstractVenteArticleItemTable {

    // private final Map<Integer, Boolean> map = new HashMap<Integer, Boolean>();

    private SQLTableElement tableElementPoidsTotalLivree;
    private SQLTable tableArticle = Configuration.getInstance().getBase().getTable("ARTICLE");

    public BonDeLivraisonItemTable(List<JButton> l) {
        super(l);
    }

    @Override
    protected void init() {
        final SQLElement e = getSQLElement();

        SQLPreferences prefs = new SQLPreferences(getSQLElement().getTable().getDBRoot());
        final boolean selectArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.USE_CREATED_ARTICLE, false);
        final boolean createAuto = prefs.getBoolean(GestionArticleGlobalPreferencePanel.CREATE_ARTICLE_AUTO, true);
        final boolean filterFamilleArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.FILTER_BY_FAMILY, false);

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        list.add(new SQLTableElement(e.getTable().getField("ID_STYLE")));

        final SQLTableElement tableFamille = new SQLTableElement(e.getTable().getField("ID_FAMILLE_ARTICLE"));
        list.add(tableFamille);

        // Article
        final SQLTableElement tableElementArticle = new SQLTableElement(e.getTable().getField("ID_ARTICLE"), true, true, true);
        list.add(tableElementArticle);

        // Code article
        final SQLTableElement tableElementCode = new SQLTableElement(e.getTable().getField("CODE"), String.class,
                new ITextArticleWithCompletionCellEditor(e.getTable().getTable("ARTICLE"), e.getTable().getTable("ARTICLE_FOURNISSEUR")));
        list.add(tableElementCode);
        // Désignation de l'article
        final SQLTableElement tableElementNom = new SQLTableElement(e.getTable().getField("NOM"));
        list.add(tableElementNom);

        // Désignation de l'article
        final SQLTableElement tableElementDesc = new SQLTableElement(e.getTable().getField("DESCRIPTIF"));
        list.add(tableElementDesc);

        // Valeur des métriques
        final SQLTableElement tableElement_ValeurMetrique2 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_2"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                // int mode = vals.getInt("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null && (modeNumber.intValue() == ReferenceArticleSQLElement.A_LA_PIECE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE
                        || modeNumber.intValue() == ReferenceArticleSQLElement.AU_METRE_LONGUEUR)) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };

        list.add(tableElement_ValeurMetrique2);
        final SQLTableElement tableElement_ValeurMetrique3 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_3"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {

                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null && (!(modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE))) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };

        list.add(tableElement_ValeurMetrique3);
        final SQLTableElement tableElement_ValeurMetrique1 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_1"), Float.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {

                Number modeNumber = (Number) vals.getObject("ID_MODE_VENTE_ARTICLE");
                if (modeNumber != null && (modeNumber.intValue() == ReferenceArticleSQLElement.A_LA_PIECE || modeNumber.intValue() == ReferenceArticleSQLElement.AU_POID_METRECARRE
                        || modeNumber.intValue() == ReferenceArticleSQLElement.AU_METRE_LARGEUR)) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {

                return new ArticleRowValuesRenderer(null);
            }
        };

        list.add(tableElement_ValeurMetrique1);
        // Prix de vente HT de la métrique 1
        SQLField field = e.getTable().getField("PRIX_METRIQUE_VT_1");
        final DeviseNumericHTConvertorCellEditor editorPVHT = new DeviseNumericHTConvertorCellEditor(field);
        final SQLTableElement tableElement_PrixMetrique1_VenteHT = new SQLTableElement(field, BigDecimal.class, editorPVHT);
        tableElement_PrixMetrique1_VenteHT.setRenderer(new DeviseTableCellRenderer());
        // {
        // @Override
        // public TableCellRenderer getTableCellRenderer() {
        //
        // List<Integer> l = new ArrayList<Integer>();
        // l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_CARRE));
        // l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_LARGEUR));
        // l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_METRE_LONGUEUR));
        // l.add(Integer.valueOf(ReferenceArticleSQLElement.AU_POID_METRECARRE));
        // return new ArticleRowValuesRenderer(l);
        // }
        // };
        list.add(tableElement_PrixMetrique1_VenteHT);
        // Prix d'achat HT de la métrique 1
        // final SQLTableElement tableElement_PrixMetrique1_AchatHT = new
        // SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_1"), Long.class, new
        // DeviseCellEditor());
        // list.add(tableElement_PrixMetrique1_AchatHT);

        // Prix de vente HT de la métrique 3
        // final SQLTableElement tableElement_PrixMetrique3_VenteHT = new
        // SQLTableElement(e.getTable().getField("PRIX_METRIQUE_VT_3"), Long.class, new
        // DeviseCellEditor());
        // list.add(tableElement_PrixMetrique3_VenteHT);
        // Prix d'achat HT de la métrique 3
        // final SQLTableElement tableElement_PrixMetrique3_AchatHT = new
        // SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_3"), Long.class, new
        // DeviseCellEditor());
        // list.add(tableElement_PrixMetrique3_AchatHT);

        SQLTableElement eltDevise = null;
        SQLTableElement eltUnitDevise = null;
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Devise
            eltDevise = new SQLTableElement(e.getTable().getField("ID_DEVISE"));
            list.add(eltDevise);

            // Prix vente devise
            eltUnitDevise = new SQLTableElement(e.getTable().getField("PV_U_DEVISE"), BigDecimal.class) {
                @Override
                public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                    return isCellNiveauEditable(vals, rowIndex, columnIndex);
                }
            };
            eltUnitDevise.setRenderer(new DeviseTableCellRenderer());
            list.add(eltUnitDevise);
        }

        SQLTableElement qteU = new SQLTableElement(e.getTable().getField("QTE_UNITAIRE"), BigDecimal.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {

                SQLRowAccessor row = vals.getForeign("ID_UNITE_VENTE");
                if (row != null && !row.isUndefined() && row.getBoolean("A_LA_PIECE")) {
                    return false;
                } else {
                    return super.isCellEditable(vals, rowIndex, columnIndex);
                }
            }

            protected Object getDefaultNullValue() {
                return BigDecimal.ZERO;
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {
                return new QteUnitRowValuesRenderer();
            }
        };
        list.add(qteU);

        SQLTableElement uniteVente = new SQLTableElement(e.getTable().getField("ID_UNITE_VENTE"));
        list.add(uniteVente);

        // Quantité
        this.qte = new SQLTableElement(e.getTable().getField("QTE"), Integer.class, new QteCellEditor()) {
            protected Object getDefaultNullValue() {
                return Integer.valueOf(0);
            }
        };
        list.add(this.qte);

        // Mode de vente
        final SQLTableElement tableElement_ModeVente = new SQLTableElement(e.getTable().getField("ID_MODE_VENTE_ARTICLE"));
        list.add(tableElement_ModeVente);

        // Prix d'achat unitaire HT
        // final SQLTableElement tableElement_PrixAchat_HT = new
        // SQLTableElement(e.getTable().getField("PA_HT"), Long.class, new DeviseCellEditor());
        // list.add(tableElement_PrixAchat_HT);
        // Prix de vente unitaire HT
        final SQLTableElement tableElement_PrixVente_HT = new SQLTableElement(e.getTable().getField("PV_HT"), BigDecimal.class);
        // , new DeviseCellEditor()) {
        // @Override
        // public TableCellRenderer getTableCellRenderer() {
        // List<Integer> l = new ArrayList<Integer>();
        // l.add(Integer.valueOf(ReferenceArticleSQLElement.A_LA_PIECE));
        // return new ArticleRowValuesRenderer(l);
        // }
        // };
        tableElement_PrixVente_HT.setRenderer(new DeviseTableCellRenderer());
        list.add(tableElement_PrixVente_HT);

        // TVA
        this.tableElementTVA = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        list.add(this.tableElementTVA);

        // Quantité Livrée
        final SQLTableElement tableElement_QuantiteLivree = new SQLTableElement(e.getTable().getField("QTE_LIVREE"), Integer.class);
        list.add(tableElement_QuantiteLivree);

        // Poids piece
        SQLTableElement tableElementPoids = new SQLTableElement(e.getTable().getField("POIDS"), Float.class);
        list.add(tableElementPoids);

        // Poids total
        this.tableElementPoidsTotal = new SQLTableElement(e.getTable().getField("T_POIDS"), Float.class);
        list.add(this.tableElementPoidsTotal);

        // Poids total Livré
        this.tableElementPoidsTotalLivree = new SQLTableElement(e.getTable().getField("T_POIDS_LIVREE"), Float.class);
        list.add(this.tableElementPoidsTotalLivree);

        // Packaging
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.ITEM_PACKAGING, false)) {

            SQLTableElement poidsColis = new SQLTableElement(e.getTable().getField("POIDS_COLIS_NET"), BigDecimal.class);
            list.add(poidsColis);

            SQLTableElement nbColis = new SQLTableElement(e.getTable().getField("NB_COLIS"), Integer.class);
            list.add(nbColis);

            final SQLTableElement totalPoidsColis = new SQLTableElement(e.getTable().getField("T_POIDS_COLIS_NET"), BigDecimal.class);
            list.add(totalPoidsColis);

            poidsColis.addModificationListener(totalPoidsColis);
            nbColis.addModificationListener(totalPoidsColis);
            totalPoidsColis.setModifier(new CellDynamicModifier() {
                public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {
                    final Object o2 = row.getObject("POIDS_COLIS_NET");
                    final Object o3 = row.getObject("NB_COLIS");
                    if (o2 != null && o3 != null) {
                        BigDecimal poids = (BigDecimal) o2;
                        int nb = (Integer) o3;
                        return poids.multiply(new BigDecimal(nb), DecimalUtils.HIGH_PRECISION).setScale(totalPoidsColis.getDecimalDigits(), RoundingMode.HALF_UP);
                    } else {
                        return row.getObject("T_POIDS_COLIS_NET");
                    }
                }
            });

        }

        // Service
        // this.service = new SQLTableElement(e.getTable().getField("SERVICE"), Boolean.class);
        // list.add(this.service);

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            // Total HT
            this.tableElementTotalDevise = new SQLTableElement(e.getTable().getField("PV_T_DEVISE"), BigDecimal.class) {
                @Override
                public boolean isCellEditable(SQLRowValues vals, int rowIndex, int columnIndex) {
                    return isCellNiveauEditable(vals, rowIndex, columnIndex);
                }
            };
            this.tableElementTotalDevise.setRenderer(new DeviseTableCellRenderer());
            list.add(tableElementTotalDevise);
        }

        final SQLField fieldRemise = e.getTable().getField("POURCENT_REMISE");

        if (e.getTable().getFieldsName().contains("MONTANT_REMISE")) {
            tableElementRemise = new SQLTableElement(e.getTable().getField("POURCENT_REMISE"), Acompte.class, new AcompteCellEditor("POURCENT_REMISE", "MONTANT_REMISE")) {
                @Override
                public void setValueFrom(SQLRowValues row, Object value) {

                    if (value != null) {
                        Acompte a = (Acompte) value;
                        row.put("MONTANT_REMISE", a.getMontant());
                        row.put("POURCENT_REMISE", a.getPercent());
                    } else {
                        row.put("MONTANT_REMISE", null);
                        row.put("POURCENT_REMISE", BigDecimal.ZERO);
                    }
                    fireModification(row);
                }
            };
            tableElementRemise.setRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    SQLRowValues rowVals = ((RowValuesTable) table).getRowValuesTableModel().getRowValuesAt(row);
                    JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    BigDecimal percent = rowVals.getBigDecimal("POURCENT_REMISE");
                    BigDecimal amount = rowVals.getBigDecimal("MONTANT_REMISE");
                    Remise a = new Remise(percent, amount);
                    label.setText(a.toPlainString(true));
                    return label;
                }
            });
        } else {
            tableElementRemise = new SQLTableElement(fieldRemise) {
                protected Object getDefaultNullValue() {
                    return BigDecimal.ZERO;
                }
            };
        }
        list.add(tableElementRemise);

        // Total HT
        this.totalHT = new SQLTableElement(e.getTable().getField("T_PV_HT"), BigDecimal.class);
        this.totalHT.setRenderer(new DeviseTableCellRenderer());
        list.add(this.totalHT);
        // Total TTC
        this.tableElementTotalTTC = new SQLTableElement(e.getTable().getField("T_PV_TTC"), BigDecimal.class);
        this.tableElementTotalTTC.setRenderer(new DeviseTableCellRenderer());
        list.add(this.tableElementTotalTTC);

        SQLRowValues defautRow = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(e.getTable()));
        defautRow.put("ID_TAXE", TaxeCache.getCache().getFirstTaxe().getID());
        defautRow.put("CODE", "");
        defautRow.put("NOM", "");
        final RowValuesTableModel model = new RowValuesTableModel(e, list, e.getTable().getField("NOM"), false, defautRow);
        this.setModel(model);
        this.table = new RowValuesTable(model, getConfigurationFile());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        if (filterFamilleArticle) {
            ((SQLTextComboTableCellEditor) tableElementArticle.getTableCellEditor(this.table)).setDynamicWhere(e.getTable().getTable("ARTICLE").getField("ID_FAMILLE_ARTICLE"));
        }

        List<String> completionField = new ArrayList<String>();
        final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {

            completionField.add("CODE_DOUANIER");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("ID_PAYS");
        }
        completionField.add("ID_UNITE_VENTE");
        completionField.add("PA_HT");
        completionField.add("PV_HT");
        completionField.add("ID_TAXE");
        completionField.add("POIDS");
        completionField.add("PRIX_METRIQUE_HA_1");
        completionField.add("PRIX_METRIQUE_HA_2");
        completionField.add("PRIX_METRIQUE_HA_3");
        completionField.add("VALEUR_METRIQUE_1");
        completionField.add("VALEUR_METRIQUE_2");
        completionField.add("VALEUR_METRIQUE_3");
        completionField.add("ID_MODE_VENTE_ARTICLE");
        completionField.add("PRIX_METRIQUE_VT_1");
        completionField.add("PRIX_METRIQUE_VT_2");
        completionField.add("PRIX_METRIQUE_VT_3");
        completionField.add("SERVICE");
        completionField.add("ID_FAMILLE_ARTICLE");
        if (getSQLElement().getTable().getFieldsName().contains("DESCRIPTIF")) {
            completionField.add("DESCRIPTIF");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("ID_DEVISE");
        }
        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            completionField.add("PV_U_DEVISE");
        }
        if (getSQLElement().getTable().getFieldsName().contains("QTE_ACHAT") && sqlTableArticle.getTable().getFieldsName().contains("QTE_ACHAT")) {
            completionField.add("QTE_ACHAT");
        }

        // Autocompletion
        final AutoCompletionManager m = new AutoCompletionManager(tableElementCode, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.CODE"), this.table,
                this.table.getRowValuesTableModel()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field, rowDest, true);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }
        };
        m.fill("NOM", "NOM");
        m.fill("ID", "ID_ARTICLE");
        for (String string : completionField) {
            m.fill(string, string);
        }
        final Where w = new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE);
        m.setWhere(w);

        final AutoCompletionManager m2 = new AutoCompletionManager(tableElementNom, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                this.table.getRowValuesTableModel()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field, rowDest, true);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }
        };
        m2.fill("CODE", "CODE");
        m2.fill("ID", "ID_ARTICLE");
        for (String string : completionField) {
            m2.fill(string, string);
        }

        m2.setWhere(w);

        final AutoCompletionManager m3 = new AutoCompletionManager(tableElementArticle, sqlTableArticle.getField("NOM"), this.table, this.table.getRowValuesTableModel(),
                ITextWithCompletion.MODE_CONTAINS, true, true, new ValidStateChecker()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field, rowDest, true);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }
        };
        m3.fill("CODE", "CODE");
        m3.fill("NOM", "NOM");
        for (String string : completionField) {
            m3.fill(string, string);
        }

        m3.setWhere(w);

        // Calcul automatique du total HT
        this.qte.addModificationListener(tableElement_PrixMetrique1_VenteHT);
        this.qte.addModificationListener(totalHT);
        qteU.addModificationListener(totalHT);
        tableElementRemise.addModificationListener(this.totalHT);
        tableElement_PrixVente_HT.addModificationListener(totalHT);
        this.totalHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {
                System.out.println("Compute totalHT");
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                BigDecimal f = (BigDecimal) row.getObject("PV_HT");
                System.out.println("Qte:" + qte + " et PV_HT:" + f);
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                BigDecimal r = b.multiply(f.multiply(BigDecimal.valueOf(qte)), DecimalUtils.HIGH_PRECISION).setScale(totalHT.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);

                if (row.getTable().getFieldsName().contains("MONTANT_REMISE")) {
                    final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_REMISE");
                    final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_REMISE");
                    Remise remise = new Remise(acomptePercent, acompteMontant);
                    r = remise.getResultFrom(r);
                }
                return r;
            }

        });
        // Calcul automatique du total TTC
        this.qte.addModificationListener(tableElementTotalTTC);
        qteU.addModificationListener(tableElementTotalTTC);
        tableElement_PrixVente_HT.addModificationListener(tableElementTotalTTC);
        this.tableElementTVA.addModificationListener(tableElementTotalTTC);
        this.tableElementTotalTTC.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                BigDecimal f = (BigDecimal) row.getObject("T_PV_HT");
                int idTaux = Integer.parseInt(row.getObject("ID_TAXE").toString());

                Float resultTaux = TaxeCache.getCache().getTauxFromId(idTaux);

                // PrixHT pHT = new PrixHT(f.longValue());
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();
                // Long r = new Long(pHT.calculLongTTC(taux / 100f));
                editorPVHT.setTaxe(taux);
                BigDecimal r = f.multiply(BigDecimal.ONE.add(BigDecimal.valueOf(taux).movePointLeft(2)), DecimalUtils.HIGH_PRECISION).setScale(6, BigDecimal.ROUND_HALF_UP);

                return r.setScale(tableElementTotalTTC.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);
            }

        });
        // Calcul automatique du poids unitaire
        tableElement_ValeurMetrique1.addModificationListener(tableElementPoids);
        tableElement_ValeurMetrique2.addModificationListener(tableElementPoids);
        tableElement_ValeurMetrique3.addModificationListener(tableElementPoids);
        tableElementPoids.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                return new Float(ReferenceArticleSQLElement.getPoidsFromDetails(row));
            }

        });
        // Calcul automatique du poids total
        tableElementPoids.addModificationListener(this.tableElementPoidsTotal);
        this.qte.addModificationListener(this.tableElementPoidsTotal);
        qteU.addModificationListener(this.tableElementPoidsTotal);
        this.tableElementPoidsTotal.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                Number f = (Number) row.getObject("POIDS");
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                // FIXME convertir en float autrement pour éviter une valeur non valeur transposable
                // avec floatValue ou passer POIDS en bigDecimal
                return b.multiply(new BigDecimal(f.floatValue() * qte)).floatValue();
            }

        });

        // Calcul automatique du poids total livrée
        tableElementPoids.addModificationListener(this.tableElementPoidsTotalLivree);
        tableElement_QuantiteLivree.addModificationListener(this.tableElementPoidsTotalLivree);
        this.tableElementPoidsTotalLivree.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                Number f = (Number) row.getObject("POIDS");

                Object qteOb = row.getObject("QTE_LIVREE");
                int qte = (qteOb == null) ? 0 : Integer.parseInt(qteOb.toString());

                float fValue = (f == null) ? 0.0F : f.floatValue();
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                // FIXME convertir en float autrement pour éviter une valeur non transposable
                // avec floatValue ou passer POIDS en bigDecimal
                return b.multiply(new BigDecimal(fValue * qte)).floatValue();
            }
        });

        tableElement_PrixMetrique1_VenteHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (source != null && source.getField().getName().equals("PRIX_METRIQUE_VT_1")) {
                    return row.getObject("PRIX_METRIQUE_VT_1");
                } else {
                    if (source != null && source.getField().getName().equals("PV_U_DEVISE")) {
                        if (!row.isForeignEmpty("ID_DEVISE")) {
                            String devCode = row.getForeign("ID_DEVISE").getString("CODE");
                            BigDecimal bigDecimal = (BigDecimal) row.getObject("PV_U_DEVISE");
                            CurrencyConverter c = new CurrencyConverter();
                            BigDecimal result = c.convert(bigDecimal, devCode, c.getCompanyCurrencyCode(), new Date(), true);
                            return result;
                        } else {
                            return row.getObject("PRIX_METRIQUE_VT_1");
                        }
                    }
                    return tarifCompletion(row.getForeign("ID_ARTICLE"), "PRIX_METRIQUE_VT_1", row);
                }
            }

        });

        if (DefaultNXProps.getInstance().getBooleanValue(ARTICLE_SHOW_DEVISE, false)) {
            if (eltUnitDevise != null) {
                eltUnitDevise.addModificationListener(tableElement_PrixMetrique1_VenteHT);
            }

            if (eltUnitDevise != null) {
                tableElement_PrixMetrique1_VenteHT.addModificationListener(eltUnitDevise);
                tableElementRemise.addModificationListener(this.tableElementTotalDevise);
                eltUnitDevise.setModifier(new CellDynamicModifier() {
                    public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                        if (source != null && source.getField().getName().equals("PV_U_DEVISE")) {
                            return row.getObject("PV_U_DEVISE");
                        } else {
                            if (!row.isForeignEmpty("ID_DEVISE")) {
                                String devCode = row.getForeign("ID_DEVISE").getString("CODE");
                                BigDecimal bigDecimal = (BigDecimal) row.getObject("PRIX_METRIQUE_VT_1");
                                CurrencyConverter c = new CurrencyConverter();
                                BigDecimal result = c.convert(bigDecimal, c.getCompanyCurrencyCode(), devCode, new Date(), true);
                                return result;
                            } else if (source != null && source.getField().getName().equalsIgnoreCase("PRIX_METRIQUE_VT_1")) {
                                return row.getObject("PRIX_METRIQUE_VT_1");
                            }
                            return row.getObject("PV_U_DEVISE");
                        }
                    }

                });
                tableElementRemise.addModificationListener(this.tableElementTotalDevise);
                tableElementTotalDevise.setModifier(new CellDynamicModifier() {
                    @Override
                    public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                        int qte = row.getInt("QTE");
                        BigDecimal prixDeVenteUnitaireDevise = (row.getObject("PV_U_DEVISE") == null) ? BigDecimal.ZERO : (BigDecimal) row.getObject("PV_U_DEVISE");
                        BigDecimal qUnitaire = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                        // r = prixUnitaire x qUnitaire x qte
                        BigDecimal prixVente = qUnitaire.multiply(prixDeVenteUnitaireDevise.multiply(BigDecimal.valueOf(qte), DecimalUtils.HIGH_PRECISION), DecimalUtils.HIGH_PRECISION);

                        if (row.getTable().getFieldsName().contains("MONTANT_REMISE")) {
                            final BigDecimal acomptePercent = row.getBigDecimal("POURCENT_REMISE");
                            final BigDecimal acompteMontant = row.getBigDecimal("MONTANT_REMISE");
                            Remise remise = new Remise(acomptePercent, acompteMontant);
                            prixVente = remise.getResultFrom(prixVente);
                        }

                        // if (lremise.compareTo(BigDecimal.ZERO) > 0 &&
                        // lremise.compareTo(BigDecimal.valueOf(100)) < 100) {
                        // r = r.multiply(BigDecimal.valueOf(100).subtract(lremise),
                        // DecimalUtils.HIGH_PRECISION).movePointLeft(2);
                        // }
                        return prixVente.setScale(tableElementTotalDevise.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);
                    }
                });
            }
        }

        // Calcul automatique du prix de vente unitaire HT
        tableElement_ValeurMetrique1.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique2.addModificationListener(tableElement_PrixVente_HT);
        tableElement_ValeurMetrique3.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixMetrique1_VenteHT.addModificationListener(tableElement_PrixVente_HT);
        tableElement_PrixVente_HT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
                    System.err.println("Don't computeValue PV_HT --> " + row.getObject("PV_HT") + row);
                    return row.getObject("PRIX_METRIQUE_VT_1");
                } else {

                    final BigDecimal prixVTFromDetails = ReferenceArticleSQLElement.getPrixVTFromDetails(row);
                    System.out.println("Prix de vente calculé au détail:" + prixVTFromDetails);
                    return prixVTFromDetails.setScale(tableElement_PrixVente_HT.getDecimalDigits(), RoundingMode.HALF_UP);
                }
            }
        });

        this.table.readState();

        tableFamille.addModificationListener(tableElementArticle);
        tableElementCode.addModificationListener(tableElementArticle);
        tableElementArticle.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                if (filterFamilleArticle) {

                    if (row.isForeignEmpty("ID_FAMILLE_ARTICLE")) {
                        m.setWhere(w);
                        m2.setWhere(w);

                    } else {
                        m.setWhere(w.and(new Where(sqlTableArticle.getField("ID_FAMILLE_ARTICLE"), "=", row.getForeignID("ID_FAMILLE_ARTICLE"))));
                        m2.setWhere(w.and(new Where(sqlTableArticle.getField("ID_FAMILLE_ARTICLE"), "=", row.getForeignID("ID_FAMILLE_ARTICLE"))));

                    }
                }
                SQLRowAccessor foreign = row.getForeign("ID_ARTICLE");
                if (foreign != null && !foreign.isUndefined() && foreign.getObject("CODE") != null && foreign.getString("CODE").equals(row.getString("CODE"))) {
                    return foreign.getID();
                } else {
                    return tableArticle.getUndefinedID();
                }
            }
        });

        // Mode Gestion article avancé
        String valModeAvanceVt = DefaultNXProps.getInstance().getStringProperty("ArticleModeVenteAvance");
        Boolean bModeAvance = Boolean.valueOf(valModeAvanceVt);
        if (bModeAvance != null && !bModeAvance.booleanValue()) {
            hideColumn(model.getColumnForField("VALEUR_METRIQUE_1"));
            hideColumn(model.getColumnForField("VALEUR_METRIQUE_2"));
            hideColumn(model.getColumnForField("VALEUR_METRIQUE_3"));
            hideColumn(model.getColumnForField("PV_HT"));
            hideColumn(model.getColumnForField("ID_MODE_VENTE_ARTICLE"));
        }

        // Packaging
        if (prefs.getBoolean(GestionArticleGlobalPreferencePanel.ITEM_PACKAGING, false)) {
            setColumnVisible(model.getColumnForField("T_POIDS_COLIS_NET"), false);
        }

        setColumnVisible(model.getColumnForField("ID_ARTICLE"), selectArticle);
        setColumnVisible(model.getColumnForField("CODE"), !selectArticle || (selectArticle && createAuto));
        setColumnVisible(model.getColumnForField("NOM"), !selectArticle || (selectArticle && createAuto));

        setColumnVisible(getModel().getColumnForField("ID_FAMILLE_ARTICLE"), filterFamilleArticle);

        // Gestion des unités de vente
        final boolean gestionUV = prefs.getBoolean(GestionArticleGlobalPreferencePanel.UNITE_VENTE, true);
        setColumnVisible(model.getColumnForField("QTE_UNITAIRE"), gestionUV);
        setColumnVisible(model.getColumnForField("ID_UNITE_VENTE"), gestionUV);

        // On réécrit la configuration au cas ou les preferences aurait changé
        this.table.writeState();

        // Calcul automatique du prix d'achat unitaire HT
        // tableElement_ValeurMetrique1.addModificationListener(tableElement_PrixAchat_HT);
        // tableElement_ValeurMetrique2.addModificationListener(tableElement_PrixAchat_HT);
        // tableElement_ValeurMetrique3.addModificationListener(tableElement_PrixAchat_HT);
        // //tableElement_PrixMetrique1_AchatHT.addModificationListener(tableElement_PrixAchat_HT);
        // tableElement_PrixAchat_HT.setModifier(new CellDynamicModifier() {
        // public Object computeValueFrom(SQLRowValues row) {
        // if (row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
        // return new Long(((Number) row.getObject("PA_HT")).longValue());
        // } else {
        // return new Long(ReferenceArticleSQLElement.getPrixHAFromDetails(row));
        // }
        // }
        // });

        // Mode de vente non éditable
        // int modeVenteColumn = model.getColumnForField("ID_MODE_VENTE_ARTICLE");
        // if (modeVenteColumn >= 0) {
        // model.setEditable(false, modeVenteColumn);
        // }

        // for (int i = 0; i < model.getColumnCount(); i++) {
        //
        // this.table.getColumnModel().getColumn(i).setCellRenderer(new
        // ArticleRowValuesRenderer(model));
        // }
    }

    @Override
    public float getPoidsTotal() {

        float poids = 0.0F;
        int poidsTColIndex = getModel().getColumnIndexForElement(this.tableElementPoidsTotalLivree);
        if (poidsTColIndex >= 0) {
            for (int i = 0; i < table.getRowCount(); i++) {
                Number tmp = (Number) getModel().getValueAt(i, poidsTColIndex);
                if (tmp != null) {
                    poids += tmp.floatValue();
                }
            }
        }
        return poids;
    }

    @Override
    protected String getConfigurationFileName() {
        return "Table_Bon_Livraison.xml";
    }

    @Override
    public SQLElement getSQLElement() {
        return Configuration.getInstance().getDirectory().getElement("BON_DE_LIVRAISON_ELEMENT");
    }

    private void hideColumn(int col) {
        if (col >= 0) {
            // this.table.getColumnModel().getColumn(col).setResizable(false);
            // this.table.getColumnModel().getColumn(col).setMinWidth(0);
            // this.table.getColumnModel().getColumn(col).setMaxWidth(0);
            // this.table.getColumnModel().getColumn(col).setPreferredWidth(0);
            // this.table.getColumnModel().getColumn(col).setWidth(0);
            // this.table.getMaskTableModel().hideColumn(col);
            XTableColumnModel columnModel = this.table.getColumnModel();

            columnModel.setColumnVisible(columnModel.getColumnByModelIndex(col), false);

        }
    }
}
