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
 
 package org.openconcerto.erp.core.common.ui;

import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ToolTipManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableCellRenderer;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.finance.accounting.model.CurrencyConverter;
import org.openconcerto.erp.core.finance.tax.model.TaxeCache;
import org.openconcerto.erp.core.sales.product.element.ReferenceArticleSQLElement;
import org.openconcerto.erp.core.sales.product.element.UniteVenteArticleSQLElement;
import org.openconcerto.erp.core.sales.product.ui.CurrencyWithSymbolRenderer;
import org.openconcerto.erp.core.sales.product.ui.QteUnitRowValuesRenderer;
import org.openconcerto.erp.preferences.DefaultNXProps;
import org.openconcerto.erp.preferences.GestionArticleGlobalPreferencePanel;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.preferences.SQLPreferences;
import org.openconcerto.sql.sqlobject.ITextArticleWithCompletionCellEditor;
import org.openconcerto.sql.sqlobject.ITextWithCompletion;
import org.openconcerto.sql.view.list.AutoCompletionManager;
import org.openconcerto.sql.view.list.CellDynamicModifier;
import org.openconcerto.sql.view.list.RowValuesTable;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.sql.view.list.SQLTableElement;
import org.openconcerto.sql.view.list.ValidStateChecker;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.i18n.TranslationManager;

public abstract class AbstractAchatArticleItemTable extends AbstractArticleItemTable {

    private AutoCompletionManager m;
    private AutoCompletionManager m2, m3;
    private AutoCompletionManager m4;
    private final SQLTable tableArticle = getSQLElement().getTable().getTable("ARTICLE");
    private SQLRowAccessor rowDevise;
    private boolean supplierCode;

    public AbstractAchatArticleItemTable() {
        super();
    }

    @Override
    protected void setModel(RowValuesTableModel model) {
        super.setModel(model);
        model.addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {

                calculTarifNomenclature();

            }
        });
    }

    protected void init() {

        final SQLElement e = getSQLElement();

        SQLPreferences prefs = SQLPreferences.getMemCached(getSQLElement().getTable().getDBRoot());
        final boolean selectArticle = prefs.getBoolean(GestionArticleGlobalPreferencePanel.USE_CREATED_ARTICLE, false);
        final boolean createAuto = prefs.getBoolean(GestionArticleGlobalPreferencePanel.CREATE_ARTICLE_AUTO, true);
        this.supplierCode = prefs.getBoolean(GestionArticleGlobalPreferencePanel.SUPPLIER_PRODUCT_CODE, false);

        final List<SQLTableElement> list = new Vector<SQLTableElement>();
        final SQLTableElement eNiveau = new SQLTableElement(e.getTable().getField("NIVEAU")) {
            @Override
            public void setValueFrom(SQLRowValues row, Object value) {
                super.setValueFrom(row, value);
            }
        };
        eNiveau.setRenderer(new NiveauTableCellRender());
        eNiveau.setEditor(new NiveauTableCellEditor());
        list.add(eNiveau);
        list.add(new SQLTableElement(e.getTable().getField("ID_STYLE")));

        SQLTableElement tableElementCodeFournisseur = null;

        if (e.getTable().contains("ID_CODE_FOURNISSEUR") && supplierCode) {
            tableElementCodeFournisseur = new SQLTableElement(e.getTable().getField("ID_CODE_FOURNISSEUR"), true, true, true);
            list.add(tableElementCodeFournisseur);
        }

        SQLTableElement tableElementArticle = new SQLTableElement(e.getTable().getField("ID_ARTICLE"), true, true, true);
        list.add(tableElementArticle);

        if (e.getTable().getFieldsName().contains("ID_FAMILLE_ARTICLE")) {
            final SQLTableElement tableFamille = new SQLTableElement(e.getTable().getField("ID_FAMILLE_ARTICLE"));
            list.add(tableFamille);
        }

        if (e.getTable().getFieldsName().contains("INCOTERM")) {
            final SQLTableElement tableElementInco = new SQLTableElement(e.getTable().getField("INCOTERM"));
            tableElementInco.setEditable(false);
            list.add(tableElementInco);
        }

        if (e.getTable().getFieldsName().contains("PREBILAN")) {
            final SQLTableElement tableElementPre = new SQLTableElement(e.getTable().getField("PREBILAN"), BigDecimal.class);
            tableElementPre.setRenderer(new DeviseTableCellRenderer());
            list.add(tableElementPre);
        }

        // Code article
        final SQLTableElement tableElementCode = new SQLTableElement(e.getTable().getField("CODE"), String.class,
                new ITextArticleWithCompletionCellEditor(e.getTable().getTable("ARTICLE"), e.getTable().getTable("ARTICLE_FOURNISSEUR")));
        list.add(tableElementCode);
        // Désignation de l'article
        final SQLTableElement tableElementNom = new SQLTableElement(e.getTable().getField("NOM"));
        list.add(tableElementNom);
        if (e.getTable().getFieldsName().contains("DESCRIPTIF")) {
            final SQLTableElement tableElementDesc = new SQLTableElement(e.getTable().getField("DESCRIPTIF"));
            list.add(tableElementDesc);
        }
        if (e.getTable().getFieldsName().contains("COLORIS")) {
            final SQLTableElement tableElementColoris = new SQLTableElement(e.getTable().getField("COLORIS"));
            list.add(tableElementColoris);
        }
        // Valeur des métriques
        final SQLTableElement tableElement_ValeurMetrique2 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_2"), Float.class);
        list.add(tableElement_ValeurMetrique2);
        final SQLTableElement tableElement_ValeurMetrique3 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_3"), Float.class);
        list.add(tableElement_ValeurMetrique3);
        final SQLTableElement tableElement_ValeurMetrique1 = new SQLTableElement(e.getTable().getField("VALEUR_METRIQUE_1"), Float.class);
        list.add(tableElement_ValeurMetrique1);
        // Prix d'achat HT de la métrique 1
        final SQLTableElement tableElement_PrixMetrique1_AchatHT = new SQLTableElement(e.getTable().getField("PRIX_METRIQUE_HA_1"), BigDecimal.class);
        tableElement_PrixMetrique1_AchatHT.setRenderer(new CurrencyWithSymbolRenderer());
        list.add(tableElement_PrixMetrique1_AchatHT);

        final SQLTableElement tableElement_Devise = new SQLTableElement(e.getTable().getField("ID_DEVISE"));
        tableElement_Devise.setEditable(false);
        final SQLTableElement tableElement_PA_Devise = new SQLTableElement(e.getTable().getField("PA_DEVISE"), BigDecimal.class);
        Path p = new Path(getSQLElement().getTable()).addForeignField("ID_DEVISE");
        tableElement_PA_Devise.setRenderer(new CurrencyWithSymbolRenderer(new FieldPath(p, "CODE")));
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {
            // Devise
            list.add(tableElement_Devise);

            // Prix d'achat HT devise
            list.add(tableElement_PA_Devise);
        }
        // Mode de vente
        final SQLTableElement tableElement_ModeVente = new SQLTableElement(e.getTable().getField("ID_MODE_VENTE_ARTICLE"));
        list.add(tableElement_ModeVente);

        // Prix d'achat unitaire HT
        this.ha = new SQLTableElement(e.getTable().getField("PA_HT"), BigDecimal.class);
        this.ha.setRenderer(new CurrencyWithSymbolRenderer());
        list.add(this.ha);

        SQLTableElement qteU = new SQLTableElement(e.getTable().getField("QTE_UNITAIRE"), BigDecimal.class) {
            @Override
            public boolean isCellEditable(SQLRowValues vals) {

                SQLRowAccessor row = vals.getForeign("ID_UNITE_VENTE");
                if (row != null && !row.isUndefined() && row.getBoolean("A_LA_PIECE")) {
                    return false;
                } else {
                    return super.isCellEditable(vals);
                }
            }

            @Override
            public TableCellRenderer getTableCellRenderer() {
                return new QteUnitRowValuesRenderer();
            }

            protected Object getDefaultNullValue() {
                return BigDecimal.ZERO;
            }
        };
        list.add(qteU);

        SQLTableElement uniteVente = new SQLTableElement(e.getTable().getField("ID_UNITE_VENTE"));
        list.add(uniteVente);

        // Quantité
        final SQLTableElement qteElement = new SQLTableElement(e.getTable().getField("QTE"), Integer.class) {
            protected Object getDefaultNullValue() {
                return Integer.valueOf(0);
            }
        };
        list.add(qteElement);
        // TVA
        this.tableElementTVA = new SQLTableElement(e.getTable().getField("ID_TAXE"));
        list.add(this.tableElementTVA);
        // Poids piece
        SQLTableElement tableElementPoids = new SQLTableElement(e.getTable().getField("POIDS"), Float.class);
        list.add(tableElementPoids);

        // Poids total
        this.tableElementPoidsTotal = new SQLTableElement(e.getTable().getField("T_POIDS"), Float.class);
        list.add(this.tableElementPoidsTotal);

        // Service
        String val = DefaultNXProps.getInstance().getStringProperty("ArticleService");
        Boolean b = Boolean.valueOf(val);
        if (b != null && b.booleanValue()) {
            this.service = new SQLTableElement(e.getTable().getField("SERVICE"), Boolean.class);
            list.add(this.service);
        }

        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {
            // Prix d'achat HT devise
            this.tableElementTotalDevise = new SQLTableElement(e.getTable().getField("PA_DEVISE_T"), BigDecimal.class);
            this.tableElementTotalDevise.setRenderer(new CurrencyWithSymbolRenderer(new FieldPath(p, "CODE")));
            list.add(tableElementTotalDevise);
        }

        SQLTableElement tableElementRemise = null;
        if (e.getTable().contains("POURCENT_REMISE")) {
            tableElementRemise = new SQLTableElement(e.getTable().getField("POURCENT_REMISE"));
            list.add(tableElementRemise);
        }

        // Total HT
        this.totalHT = new SQLTableElement(e.getTable().getField("T_PA_HT"), BigDecimal.class);
        this.totalHT.setRenderer(new DeviseTableCellRenderer());
        this.totalHT.setEditable(false);
        if (e.getTable().contains("POURCENT_REMISE") && tableElementRemise != null) {
            tableElementRemise.addModificationListener(this.totalHT);
        }
        list.add(this.totalHT);
        this.totalHA = this.totalHT;
        // Total TTC
        this.tableElementTotalTTC = new SQLTableElement(e.getTable().getField("T_PA_TTC"), BigDecimal.class);
        this.tableElementTotalTTC.setRenderer(new DeviseTableCellRenderer());
        list.add(this.tableElementTotalTTC);

        this.defaultRowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(e.getTable()));
        this.defaultRowVals.put("ID_TAXE", TaxeCache.getCache().getFirstTaxe().getID());
        this.defaultRowVals.put("CODE", "");
        this.defaultRowVals.put("NOM", "");
        this.defaultRowVals.put("QTE", 1);
        this.defaultRowVals.put("QTE_UNITAIRE", BigDecimal.ONE);
        this.defaultRowVals.put("ID_UNITE_VENTE", UniteVenteArticleSQLElement.A_LA_PIECE);
        this.defaultRowVals.put("ID_MODE_VENTE_ARTICLE", ReferenceArticleSQLElement.A_LA_PIECE);
        final RowValuesTableModel model = new RowValuesTableModel(e, list, e.getTable().getField("NOM"), false, this.defaultRowVals);
        setModel(model);

        this.table = new RowValuesTable(model, getConfigurationFile());
        ToolTipManager.sharedInstance().unregisterComponent(this.table);
        ToolTipManager.sharedInstance().unregisterComponent(this.table.getTableHeader());

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                final int rowindex = table.getSelectedRow();
                if (rowindex < 0)
                    return;
                if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(new AbstractAction(TranslationManager.getInstance().getTranslationForItem("product.bom.expand")) {

                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            expandNomenclature(rowindex, m, EXPAND_TYPE.EXPAND);
                        }
                    });
                    popup.add(new AbstractAction(TranslationManager.getInstance().getTranslationForItem("product.bom.expose")) {

                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            expandNomenclature(rowindex, m, EXPAND_TYPE.VIEW_ONLY);
                        }
                    });
                    popup.add(new AbstractAction(TranslationManager.getInstance().getTranslationForItem("product.bom.flat")) {

                        @Override
                        public void actionPerformed(ActionEvent arg0) {
                            expandNomenclature(rowindex, m, EXPAND_TYPE.FLAT);
                        }
                    });

                    for (AbstractAction action : getAdditionnalMouseAction(rowindex)) {
                        popup.add(action);
                    }

                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        // Autocompletion
        List<String> completionFields = new ArrayList<String>();
        completionFields.add("ID_UNITE_VENTE");
        completionFields.add("PA_HT");
        completionFields.add("PV_HT");
        completionFields.add("POIDS");
        completionFields.add("ID_TAXE");
        completionFields.add("PRIX_METRIQUE_HA_1");
        completionFields.add("PRIX_METRIQUE_HA_2");
        completionFields.add("PRIX_METRIQUE_HA_3");
        completionFields.add("VALEUR_METRIQUE_1");
        completionFields.add("VALEUR_METRIQUE_2");
        completionFields.add("VALEUR_METRIQUE_3");
        completionFields.add("ID_MODE_VENTE_ARTICLE");
        completionFields.add("PRIX_METRIQUE_VT_1");
        completionFields.add("PRIX_METRIQUE_VT_2");
        completionFields.add("PRIX_METRIQUE_VT_3");
        completionFields.add("SERVICE");
        completionFields.add("ID_DEVISE");
        completionFields.add("PA_DEVISE");
        if (e.getTable().getFieldsName().contains("COLORIS")) {
            completionFields.add("COLORIS");
        }
        if (e.getTable().getFieldsName().contains("INCOTERM")) {
            completionFields.add("INCOTERM");
        }
        if (e.getTable().getFieldsName().contains("DESCRIPTIF")) {
            completionFields.add("DESCRIPTIF");
        }
        if (e.getTable().getFieldsName().contains("ID_FAMILLE_ARTICLE")) {
            completionFields.add("ID_FAMILLE_ARTICLE");
        }

        this.m = new AutoCompletionManager(tableElementCode, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.CODE"), this.table,
                this.table.getRowValuesTableModel()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }
        };
        m.fill("NOM", "NOM");
        m.fill("ID", "ID_ARTICLE");
        for (String string : completionFields) {
            m.fill(string, string);
        }
        final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        final Where w = new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE);
        m.setWhere(w);

        this.m2 = new AutoCompletionManager(tableElementNom, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                this.table.getRowValuesTableModel()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }
        };
        m2.fill("CODE", "CODE");
        m2.fill("ID", "ID_ARTICLE");
        for (String string : completionFields) {
            m2.fill(string, string);
        }
        m2.setWhere(w);

        this.m3 = new AutoCompletionManager(tableElementArticle, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                this.table.getRowValuesTableModel(), ITextWithCompletion.MODE_CONTAINS, true, true, new ValidStateChecker()) {
            @Override
            protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                Object res = tarifCompletion(row, field);
                if (res == null) {
                    return super.getValueFrom(row, field, rowDest);
                } else {
                    return res;
                }
            }
        };
        m3.fill("CODE", "CODE");
        m3.fill("NOM", "NOM");
        for (String string : completionFields) {
            m3.fill(string, string);
        }
        m3.setWhere(w);

        if (e.getTable().contains("ID_CODE_FOURNISSEUR") && supplierCode) {
            this.m4 = new AutoCompletionManager(tableElementCodeFournisseur, ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete().getField("ARTICLE.NOM"), this.table,
                    this.table.getRowValuesTableModel(), ITextWithCompletion.MODE_CONTAINS, true, true, new ValidStateChecker()) {
                @Override
                protected Object getValueFrom(SQLRow row, String field, SQLRowAccessor rowDest) {
                    Object res = tarifCompletion(row, field);
                    if (res == null) {
                        return super.getValueFrom(row, field, rowDest);
                    } else {
                        return res;
                    }
                }
            };
            m4.fill("CODE", "CODE");
            m4.fill("NOM", "NOM");
            for (String string : completionFields) {
                m4.fill(string, string);
            }
        }

        tableElementCode.addModificationListener(tableElementArticle);
        tableElementArticle.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                try {
                    SQLRowAccessor foreign = row.getForeign("ID_ARTICLE");
                    if (foreign != null && !foreign.isUndefined() && foreign.getObject("CODE") != null && foreign.getString("CODE").equals(row.getString("CODE"))) {
                        return foreign.getID();
                    } else {
                        return tableArticle.getUndefinedID();
                    }
                } catch (Exception e) {
                    return tableArticle.getUndefinedID();
                }
            }
        });

        // Calcul automatique du total HT
        qteElement.addModificationListener(this.totalHT);
        qteU.addModificationListener(this.totalHT);
        this.ha.addModificationListener(this.totalHT);
        this.totalHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {

                int qte = Integer.parseInt(row.getObject("QTE").toString());
                BigDecimal f = (row.getObject("PA_HT") == null) ? BigDecimal.ZERO : (BigDecimal) row.getObject("PA_HT");
                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                BigDecimal r = b.multiply(f.multiply(BigDecimal.valueOf(qte)), DecimalUtils.HIGH_PRECISION).setScale(totalHT.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);

                if (row.getTable().contains("POURCENT_REMISE")) {
                    final Object o2 = row.getObject("POURCENT_REMISE");

                    BigDecimal lremise = (o2 == null) ? BigDecimal.ZERO : ((BigDecimal) o2);
                    if (lremise.compareTo(BigDecimal.ZERO) >= 0 && lremise.compareTo(BigDecimal.valueOf(100)) < 0) {

                        r = r.multiply(new BigDecimal(100).subtract(lremise).movePointLeft(2)).setScale(totalHT.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);
                    }
                }

                return r;
            }

        });
        if (DefaultNXProps.getInstance().getBooleanValue(AbstractVenteArticleItemTable.ARTICLE_SHOW_DEVISE, false)) {

            if (tableElement_PA_Devise != null) {
                tableElement_PA_Devise.addModificationListener(tableElement_PrixMetrique1_AchatHT);
            }

            if (tableElement_PA_Devise != null) {
                tableElement_PrixMetrique1_AchatHT.addModificationListener(tableElement_PA_Devise);

                tableElement_PA_Devise.setModifier(new CellDynamicModifier() {
                    public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                        if (source != null && source.getField().getName().equals("PA_DEVISE")) {
                            return row.getObject("PA_DEVISE");
                        } else {
                            if (!row.isForeignEmpty("ID_DEVISE")) {
                                String devCode = row.getForeign("ID_DEVISE").getString("CODE");
                                BigDecimal bigDecimal = (BigDecimal) row.getObject("PRIX_METRIQUE_HA_1");

                                CurrencyConverter c = new CurrencyConverter();
                                BigDecimal result = convert(bigDecimal, devCode, true);
                                if (result == null) {
                                    JOptionPane.showMessageDialog(AbstractAchatArticleItemTable.this, "Unable to convert " + bigDecimal + " from " + c.getCompanyCurrencyCode() + " to " + devCode);
                                    return BigDecimal.ZERO;
                                }
                                return result;
                            } else if (source != null && source.getField().getName().equalsIgnoreCase("PRIX_METRIQUE_HA_1")) {
                                return row.getObject("PRIX_METRIQUE_HA_1");
                            }
                            return row.getObject("PA_DEVISE");
                        }
                    }

                });
            }

            qteElement.addModificationListener(this.tableElementTotalDevise);
            qteU.addModificationListener(this.tableElementTotalDevise);
            tableElement_PA_Devise.addModificationListener(this.tableElementTotalDevise);
            if (e.getTable().contains("POURCENT_REMISE") && tableElementRemise != null) {
                tableElementRemise.addModificationListener(this.tableElementTotalDevise);
            }
            this.tableElementTotalDevise.setModifier(new CellDynamicModifier() {
                public Object computeValueFrom(final SQLRowValues row, SQLTableElement source) {
                    int qte = Integer.parseInt(row.getObject("QTE").toString());
                    BigDecimal f = (BigDecimal) row.getObject("PA_DEVISE");
                    BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                    BigDecimal r = b.multiply(f.multiply(BigDecimal.valueOf(qte)), DecimalUtils.HIGH_PRECISION).setScale(tableElementTotalDevise.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);

                    if (row.getTable().contains("POURCENT_REMISE")) {
                        final Object o2 = row.getObject("POURCENT_REMISE");
                        BigDecimal lremise = (o2 == null) ? BigDecimal.ZERO : ((BigDecimal) o2);
                        if (lremise.compareTo(BigDecimal.ZERO) >= 0 && lremise.compareTo(BigDecimal.valueOf(100)) < 0) {

                            r = r.multiply(new BigDecimal(100).subtract(lremise).movePointLeft(2)).setScale(tableElementTotalDevise.getDecimalDigits(), BigDecimal.ROUND_HALF_UP);
                        }
                    }
                    return r;
                }

            });
        }
        // Calcul automatique du total TTC
        qteElement.addModificationListener(this.tableElementTotalTTC);
        qteU.addModificationListener(this.tableElementTotalTTC);
        this.ha.addModificationListener(this.tableElementTotalTTC);
        this.tableElementTVA.addModificationListener(this.tableElementTotalTTC);
        this.tableElementTotalTTC.setModifier(new CellDynamicModifier() {
            @Override
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                int qte = Integer.parseInt(row.getObject("QTE").toString());
                BigDecimal f = (BigDecimal) row.getObject("PA_HT");
                int idTaux = Integer.parseInt(row.getObject("ID_TAXE").toString());
                if (idTaux < 0) {
                    System.out.println(row);
                }
                Float resultTaux = TaxeCache.getCache().getTauxFromId(idTaux);

                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                BigDecimal r = b.multiply(f.multiply(BigDecimal.valueOf(qte), DecimalUtils.HIGH_PRECISION), DecimalUtils.HIGH_PRECISION).setScale(tableElementTotalTTC.getDecimalDigits(),
                        BigDecimal.ROUND_HALF_UP);
                float taux = (resultTaux == null) ? 0.0F : resultTaux.floatValue();

                BigDecimal total = r.multiply(BigDecimal.ONE.add(new BigDecimal(taux / 100f))).setScale(tableElementTotalTTC.getDecimalDigits(), RoundingMode.HALF_UP);
                return total;
            }

        });

        this.table.readState();

        // Mode Gestion article avancé
        String valModeAvanceVt = DefaultNXProps.getInstance().getStringProperty("ArticleModeVenteAvance");
        Boolean bModeAvance = Boolean.valueOf(valModeAvanceVt);
        boolean view = !(bModeAvance != null && !bModeAvance.booleanValue());
        setColumnVisible(model.getColumnForField("VALEUR_METRIQUE_1"), view);
        setColumnVisible(model.getColumnForField("VALEUR_METRIQUE_2"), view);
        setColumnVisible(model.getColumnForField("VALEUR_METRIQUE_3"), view);
        setColumnVisible(model.getColumnForField("PRIX_METRIQUE_VT_1"), view);
        setColumnVisible(model.getColumnForField("ID_MODE_VENTE_ARTICLE"), view);
        setColumnVisible(model.getColumnForField("PA_HT"), view);

        // Gestion des unités de vente
        final boolean gestionUV = prefs.getBoolean(GestionArticleGlobalPreferencePanel.UNITE_VENTE, true);
        setColumnVisible(model.getColumnForField("QTE_UNITAIRE"), gestionUV);
        setColumnVisible(model.getColumnForField("ID_UNITE_VENTE"), gestionUV);

        setColumnVisible(model.getColumnForField("ID_STYLE"), DefaultNXProps.getInstance().getBooleanValue("ArticleShowStyle", true));

        setColumnVisible(model.getColumnForField("ID_ARTICLE"), selectArticle);
        setColumnVisible(model.getColumnForField("CODE"), !selectArticle || (selectArticle && createAuto));
        setColumnVisible(model.getColumnForField("NOM"), !selectArticle || (selectArticle && createAuto));

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
        qteElement.addModificationListener(this.tableElementPoidsTotal);
        qteU.addModificationListener(this.tableElementPoidsTotal);
        this.tableElementPoidsTotal.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                Number f = (row.getObject("POIDS") == null) ? 0 : (Number) row.getObject("POIDS");
                int qte = Integer.parseInt(row.getObject("QTE").toString());

                BigDecimal b = (row.getObject("QTE_UNITAIRE") == null) ? BigDecimal.ONE : (BigDecimal) row.getObject("QTE_UNITAIRE");
                // FIXME convertir en float autrement pour éviter une valeur non transposable
                // avec floatValue ou passer POIDS en bigDecimal
                return b.multiply(new BigDecimal(f.floatValue() * qte)).floatValue();
            }

        });

        tableElement_PrixMetrique1_AchatHT.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                if (source != null && source.getField().getName().equals("PRIX_METRIQUE_HA_1")) {
                    return row.getObject("PRIX_METRIQUE_HA_1");
                } else {
                    if (source != null && source.getField().getName().equals("PA_DEVISE")) {
                        if (!row.isForeignEmpty("ID_DEVISE")) {
                            String devCode = row.getForeign("ID_DEVISE").getString("CODE");
                            BigDecimal bigDecimal = (BigDecimal) row.getObject("PA_DEVISE");

                            return convert(bigDecimal, devCode, false);
                        } else {
                            return row.getObject("PRIX_METRIQUE_HA_1");
                        }
                    }
                    return row.getObject("PRIX_METRIQUE_HA_1");
                }
            }

        });

        // Calcul automatique du prix d'achat unitaire HT
        tableElement_ValeurMetrique1.addModificationListener(this.ha);
        tableElement_ValeurMetrique2.addModificationListener(this.ha);
        tableElement_ValeurMetrique3.addModificationListener(this.ha);
        tableElement_PrixMetrique1_AchatHT.addModificationListener(this.ha);
        this.ha.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {

                if (row.isForeignEmpty("ID_MODE_VENTE_ARTICLE") || row.getInt("ID_MODE_VENTE_ARTICLE") == ReferenceArticleSQLElement.A_LA_PIECE) {
                    return row.getObject("PRIX_METRIQUE_HA_1");
                } else {

                    final BigDecimal prixHAFromDetails = ReferenceArticleSQLElement.getPrixHAFromDetails(row);
                    return prixHAFromDetails.setScale(ha.getDecimalDigits(), RoundingMode.HALF_UP);
                }

            }

        });

        uniteVente.addModificationListener(qteU);
        qteU.setModifier(new CellDynamicModifier() {
            public Object computeValueFrom(SQLRowValues row, SQLTableElement source) {
                SQLRowAccessor rowUnite = row.getForeign("ID_UNITE_VENTE");
                if (rowUnite != null && !rowUnite.isUndefined() && rowUnite.getBoolean("A_LA_PIECE")) {
                    return BigDecimal.ONE;
                } else {
                    return row.getObject("QTE_UNITAIRE");
                }
            }

        });

        for (String string : visibilityMap.keySet()) {
            setColumnVisible(model.getColumnForField(string), visibilityMap.get(string));
        }

        // On réécrit la configuration au cas ou les preferences aurait changé
        this.table.writeState();
    }

    private static Map<String, Boolean> visibilityMap = new HashMap<String, Boolean>();

    public static Map<String, Boolean> getVisibilityMap() {
        return visibilityMap;
    }

    protected List<AbstractAction> getAdditionnalMouseAction(final int rowIndex) {
        return Collections.emptyList();
    }

    public void setFournisseur(SQLRow rowFournisseur) {

        if (getSQLElement().getTable().contains("ID_CODE_FOURNISSEUR") && this.supplierCode) {

            if (rowFournisseur != null && !rowFournisseur.isUndefined()) {
                Where w = new Where(getSQLElement().getTable().getTable("CODE_FOURNISSEUR").getField("ID_FOURNISSEUR"), "=", rowFournisseur.getID());
                this.m4.setWhere(w);
            } else {
                this.m4.setWhere(null);
            }
        }
    }

    private BigDecimal getPrice(final SQLRowAccessor r, List<String> list) {
        BigDecimal result = r.getBigDecimal(list.get(0));

        for (int i = 1; i < list.size(); i++) {
            BigDecimal m0 = r.getBigDecimal(list.get(i));
            if (m0 != null && m0.floatValue() > 0) {
                result = result.divide(m0, 2, RoundingMode.HALF_UP);
            }
        }
        return result;
    }

    private Object tarifCompletion(SQLRow row, String field) {
        final SQLTable tTarifFournisseur = row.getTable().getDBRoot().getTable("ARTICLE_TARIF_FOURNISSEUR");

        if (field.equalsIgnoreCase("PRIX_METRIQUE_HA_1") && tTarifFournisseur != null) {
            List<SQLRow> rows = row.getReferentRows(tTarifFournisseur);
            if (!rows.isEmpty()) {
                final SQLRow sqlRow0 = rows.get(0);

                List<String> incoTerms;
                final String conditionsInco = sqlRow0.getString("CONDITIONS");

                if (conditionsInco != null && conditionsInco.equalsIgnoreCase("CPT")) {
                    incoTerms = Arrays.asList("PRIX_ACHAT", "COEF_TRANSPORT_PORT");
                } else if (conditionsInco != null && conditionsInco.equalsIgnoreCase("DDP")) {
                    incoTerms = Arrays.asList("PRIX_ACHAT", "COEF_TRANSPORT_PORT", "COEF_TAXE_D");
                } else {
                    incoTerms = Arrays.asList("PRIX_ACHAT");
                }

                BigDecimal min = getPrice(sqlRow0, incoTerms);

                for (SQLRow sqlRow : rows) {
                    BigDecimal price = getPrice(sqlRow, incoTerms);
                    if (price.compareTo(min) < 0) {
                        min = price;
                    }
                }
                return min.setScale(2, RoundingMode.HALF_UP);
            }
        }
        if (field.equalsIgnoreCase("INCOTERM")) {
            if (tTarifFournisseur != null) {
                List<SQLRow> rows = row.getReferentRows(tTarifFournisseur);
                if (!rows.isEmpty()) {
                    final SQLRow sqlRow0 = rows.get(0);
                    return sqlRow0.getString("CONDITIONS");
                }
            }
            return "";
        }

        if (getDevise() != null && !getDevise().isUndefined()) {
            if ((field.equalsIgnoreCase("ID_DEVISE") || field.equalsIgnoreCase("ID_DEVISE_HA"))) {
                return getDevise().getID();
            } else if ((field.equalsIgnoreCase("PA_DEVISE"))) {
                if (row.getBigDecimal("PA_DEVISE") != null && row.getBigDecimal("PA_DEVISE").signum() != 0) {
                    return row.getBigDecimal("PA_DEVISE");
                } else {
                    if (row.getBigDecimal("PA_DEVISE") != null && row.getBigDecimal("PA_DEVISE").signum() != 0) {
                        return row.getBigDecimal("PA_DEVISE");
                    } else {
                        String devCode = getDevise().getString("CODE");

                        CurrencyConverter c = new CurrencyConverter();
                        BigDecimal tarifCompletion = (BigDecimal) tarifCompletion(row, "PRIX_METRIQUE_HA_1");
                        if (tarifCompletion == null) {
                            tarifCompletion = row.getBigDecimal("PRIX_METRIQUE_HA_1");
                        }
                        if (tarifCompletion == null) {
                            return null;
                        } else {
                            return convert(tarifCompletion, devCode, true).setScale(2, RoundingMode.HALF_UP);
                        }
                    }
                }
            }
        } else {
            if ((field.equalsIgnoreCase("ID_DEVISE") || field.equalsIgnoreCase("ID_DEVISE_HA"))) {
                return Configuration.getInstance().getDirectory().getElement("DEVISE").getTable().getUndefinedID();
            } else if ((field.equalsIgnoreCase("PA_DEVISE"))) {

                return BigDecimal.ZERO;
            }
        }
        return null;
    }

    @Override
    protected void refreshDeviseAmount() {
        int count = getRowValuesTable().getRowCount();
        final int columnForField = getRowValuesTable().getRowValuesTableModel().getColumnForField("PA_DEVISE");
        if (columnForField >= 0) {
            SQLTableElement eltDevise = getRowValuesTable().getRowValuesTableModel().getSQLTableElementAt(columnForField);
            for (int i = 0; i < count; i++) {
                SQLRowValues rowVals = getRowValuesTable().getRowValuesTableModel().getRowValuesAt(i);
                // getRowValuesTable().getRowValuesTableModel().putValue(rowVals.getObject("PV_U_DEVISE"),
                // i, "PV_U_DEVISE", true);
                eltDevise.setValueFrom(rowVals, rowVals.getObject("PA_DEVISE"));
                getRowValuesTable().getRowValuesTableModel().fireTableChanged(new TableModelEvent(getRowValuesTable().getRowValuesTableModel(), i, i, columnForField));
            }
        }
    }

    public SQLRowAccessor getDevise() {
        return this.rowDevise;
    }

    public void setDevise(SQLRowAccessor deviseRow) {
        this.rowDevise = deviseRow;
        if (deviseRow == null) {
            getDefaultRowValues().put("ID_DEVISE", null);
        } else {
            getDefaultRowValues().put("ID_DEVISE", this.rowDevise.getID());
        }
    }

    private BigDecimal tauxConversion = null;

    public void setTauxConversion(BigDecimal tauxConversion) {
        if (tauxConversion != null && tauxConversion.signum() == 0) {
            tauxConversion = null;
        }
        this.tauxConversion = tauxConversion;
        refreshDeviseAmount();
    }

    public BigDecimal convert(BigDecimal val, String devCode, boolean fromCompanyCurrency) {
        if (val == null) {
            return val;
        }
        if (this.tauxConversion == null) {
            CurrencyConverter c = new CurrencyConverter();
            if (fromCompanyCurrency) {
                return c.convert(val, c.getCompanyCurrencyCode(), devCode, getDateDevise(), isUsedBiasedDevise());
            } else {
                return c.convert(val, devCode, c.getCompanyCurrencyCode(), getDateDevise(), isUsedBiasedDevise());
            }
        } else {
            if (fromCompanyCurrency) {
                return val.divide(this.tauxConversion, DecimalUtils.HIGH_PRECISION).setScale(val.scale(), RoundingMode.HALF_UP);
            } else {
                return val.multiply(this.tauxConversion, DecimalUtils.HIGH_PRECISION).setScale(val.scale(), RoundingMode.HALF_UP);
            }
        }
    }

    public void setFournisseurFilterOnCompletion(SQLRow row) {
        final SQLTable sqlTableArticle = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("ARTICLE");
        Where w = new Where(sqlTableArticle.getField("OBSOLETE"), "=", Boolean.FALSE);
        if (row != null && !row.isUndefined()) {
            w = w.and(new Where(this.tableArticle.getField("ID_FOURNISSEUR"), "=", row.getID()));
        }
        this.m.setWhere(w);
        this.m2.setWhere(w);
        this.m3.setWhere(w);
    }

    public void insertFromReliquat(List<SQLRowValues> reliquats) {

        for (SQLRowValues reliquat : reliquats) {

            final SQLRowValues row2Insert = new SQLRowValues(getRowValuesTable().getRowValuesTableModel().getDefaultRowValues());

            // Completion depuis l'article trouvé
            final SQLRowAccessor article = reliquat.getForeign("ID_ARTICLE").asRow();

            row2Insert.put("ID_ARTICLE", article.getID());
            row2Insert.put("CODE", article.getObject("CODE"));
            row2Insert.put("NOM", article.getObject("NOM"));

            row2Insert.put("QTE", reliquat.getObject("QTE"));
            row2Insert.put("QTE_UNITAIRE", reliquat.getObject("QTE_UNITAIRE"));
            row2Insert.put("ID_UNITE_VENTE", reliquat.getForeignID("ID_UNITE_VENTE"));
            getRowValuesTable().getRowValuesTableModel().addRowAt(0, row2Insert);

        }
    }

}
