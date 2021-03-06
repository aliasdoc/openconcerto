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
 
 package org.openconcerto.erp.core.finance.accounting.element;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.common.element.ComptaSQLConfElement;
import org.openconcerto.erp.core.finance.accounting.ui.SaisieKmItemTable;
import org.openconcerto.erp.generationEcritures.GenerationMvtSaisieKm;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.BaseSQLComponent;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.UndefinedRowValuesCache;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.view.list.RowValuesTableModel;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.warning.JLabelWarning;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.checks.ValidState;
import org.openconcerto.utils.text.SimpleDocumentListener;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.apache.commons.dbutils.handlers.ArrayListHandler;

public class SaisieKmSQLElement extends ComptaSQLConfElement {

    public SaisieKmSQLElement() {
        super("SAISIE_KM", "une saisie au kilomètre", "saisies au kilomètre");
    }

    protected List<String> getListFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("NOM");
        l.add("ID_JOURNAL");
        return l;
    }

    protected List<String> getComboFields() {
        final List<String> l = new ArrayList<String>();
        l.add("DATE");
        l.add("NOM");

        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.openconcerto.devis.SQLElement#getComponent()
     */
    public SQLComponent createComponent() {

        return new SaisieKmComponent();
    }

    /**
     * Genere une saisie au kilometre à partir d'un mouvement
     * 
     * @param idMvt
     * 
     * @return l'id de la saisie au kilometre créé
     * @throws SQLException
     */
    public static int createSaisie(int idMvt) throws SQLException {
        int idSaisie = 1;
        SQLBase base = ((ComptaPropsConfiguration) Configuration.getInstance()).getSQLBaseSociete();
        SQLTable ecrTable = base.getTable("ECRITURE");
        SQLTable compteTable = base.getTable("COMPTE_PCE");
        SQLTable saisieKmTable = base.getTable("SAISIE_KM_ELEMENT");

        SQLRowValues vals = new SQLRowValues(base.getTable("SAISIE_KM"));
        vals.put("ID_MOUVEMENT", new Integer(idMvt));

        SQLRow rowSaisieKm = vals.insert();
        idSaisie = rowSaisieKm.getID();

        SQLSelect selEcriture = new SQLSelect();
        selEcriture.addSelect(ecrTable.getField("ID"));

        Where w = new Where(ecrTable.getField("ID_MOUVEMENT"), "=", idMvt);
        selEcriture.setWhere(w);

        String reqEcriture = selEcriture.asString();

        Object obEcriture = base.getDataSource().execute(reqEcriture, new ArrayListHandler());

        List myListEcriture = (List) obEcriture;

        if (myListEcriture.size() != 0) {

            for (int i = 0; i < myListEcriture.size(); i++) {
                Object[] objTmp = (Object[]) myListEcriture.get(i);

                SQLRow rowEcrTmp = ecrTable.getRow(Integer.parseInt(objTmp[0].toString()));
                SQLRow rowCompteTmp = compteTable.getRow(rowEcrTmp.getInt("ID_COMPTE_PCE"));

                SQLRowValues valsTmp = new SQLRowValues(saisieKmTable);
                valsTmp.put("ID_SAISIE_KM", new Integer(rowSaisieKm.getID()));
                valsTmp.put("NUMERO", rowCompteTmp.getString("NUMERO"));
                valsTmp.put("NOM", rowCompteTmp.getString("NOM"));
                valsTmp.put("NOM_ECRITURE", rowEcrTmp.getString("NOM"));
                if (ecrTable.contains("NOM_PIECE")) {
                    valsTmp.put("NOM_PIECE", rowEcrTmp.getString("NOM_PIECE"));
                }
                valsTmp.put("DEBIT", rowEcrTmp.getObject("DEBIT"));
                valsTmp.put("CREDIT", rowEcrTmp.getObject("CREDIT"));
                valsTmp.put("ID_ECRITURE", new Integer(rowEcrTmp.getID()));
                valsTmp.insert();
            }

            Object[] objTmp = (Object[]) myListEcriture.get(0);
            SQLRow rowEcrTmp = ecrTable.getRow(Integer.parseInt(objTmp[0].toString()));
            vals.put("NOM", rowEcrTmp.getString("NOM"));
            vals.put("DATE", rowEcrTmp.getObject("DATE"));
            vals.put("ID_JOURNAL", rowEcrTmp.getObject("ID_JOURNAL"));

            vals.update(idSaisie);
        }

        SQLTable mouvementTable = base.getTable("MOUVEMENT");
        SQLRow rowMvt = mouvementTable.getRow(idMvt);

        if ((rowMvt.getString("SOURCE").trim().length() == 0) || (rowMvt.getInt("IDSOURCE") == 1)) {
            SQLRowValues valsMouvement = new SQLRowValues(mouvementTable);
            valsMouvement.put("SOURCE", "SAISIE_KM");
            valsMouvement.put("IDSOURCE", new Integer(rowSaisieKm.getID()));
            valsMouvement.update(idMvt);
        }

        return idSaisie;
    }

    public static void loadContrePassation(SQLComponent comp, int idMvt) {
        SaisieKmComponent compKm = (SaisieKmComponent) comp;
        compKm.loadContrepassation(idMvt);
    }

    class SaisieKmComponent extends BaseSQLComponent {

        private JTextField textNom;
        private JDate date;
        private RowValuesTableModel model;
        private ElementComboBox comboJrnl;
        private JLabel labelTotalDebit;
        private JLabel labelTotalCredit;
        private long totalCred;
        private long totalDeb;
        private int debitIndex, creditIndex;
        private JCheckBox checkCreateCompte;
        private JCheckBox checkCreateLineAuto;
        private boolean isCompteExist = false;
        private boolean allLineValid = true;
        private SaisieKmItemTable tableKm;
        private final JLabel labelMotifWarning = new JLabelWarning();
        private SQLElement eltKmItem = Configuration.getInstance().getDirectory().getElement("SAISIE_KM_ELEMENT");
        private SQLRowValues defaultEcritureRowVals = new SQLRowValues(UndefinedRowValuesCache.getInstance().getDefaultRowValues(this.eltKmItem.getTable()));

        // depends on inner table, at creation it's empty and thus valid
        private ValidState validState = ValidState.getTrueInstance();

        private ValidState validStateCloture = ValidState.getTrueInstance();
        private final SQLRow rowExercice = ComptaPropsConfiguration.getInstanceCompta().getRowSociete().getForeign("ID_EXERCICE_COMMON");

        private TableModelListener tableListener = new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                SaisieKmComponent.this.tableChanged(e);
            }
        };

        public SaisieKmComponent() {
            super(SaisieKmSQLElement.this);
        }

        public void addViews() {

            this.setLayout(new GridBagLayout());

            GridBagConstraints c = new DefaultGridBagConstraints();

            this.textNom = new JTextField();
            this.labelTotalCredit = new JLabel("0.00", SwingConstants.RIGHT);
            this.labelTotalDebit = new JLabel("0.00", SwingConstants.RIGHT);
            this.date = new JDate();
            this.date.addValueListener(new PropertyChangeListener() {

                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    Calendar cDeb = rowExercice.getDate("DATE_DEB");
                    Calendar cClo = rowExercice.getDate("DATE_CLOTURE");
                    if (date.getValue() != null && cDeb.getTime().after(date.getValue())) {
                        validStateCloture = new ValidState(false, "La date de saisie doit être supérieure à celle du début de l'exercice!");
                    } else if (date.getValue() != null && cClo != null && cClo.getTime().after(date.getValue())) {
                        validStateCloture = new ValidState(false, "La date de saisie doit être supérieure à celle de la période de clôture définie dans l'exercice courant!");
                    } else {
                        validStateCloture = ValidState.getTrueInstance();
                    }
                    fireValidChange();
                }
            });
            this.comboJrnl = new ElementComboBox(false, 20);

            // Libellé
            this.add(new JLabel("Libellé", SwingConstants.RIGHT), c);

            c.gridx++;
            c.weightx = 1;
            c.gridwidth = 1;
            this.add(this.textNom, c);

            // Date
            JLabel labelDate = new JLabel("Date");
            c.gridx++;
            c.weightx = 0;
            c.gridwidth = 1;
            this.add(labelDate, c);
            c.gridx++;
            c.gridwidth = 1;
            this.add(this.date, c);

            // Journal
            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 1;
            c.weightx = 0;
            this.add(new JLabel("Journal", SwingConstants.RIGHT), c);

            c.gridx++;
            c.gridwidth = 3;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 1;
            this.add(this.comboJrnl, c);

            // Km ItemTable
            this.tableKm = new SaisieKmItemTable(this.defaultEcritureRowVals);
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weighty = 1;
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            this.add(this.tableKm, c);

            // Initialisation du panel des Totaux
            JPanel panelTotal = new JPanel();
            panelTotal.setLayout(new GridBagLayout());
            panelTotal.setBorder(BorderFactory.createTitledBorder("Totaux"));
            final GridBagConstraints cc = new DefaultGridBagConstraints();
            cc.anchor = GridBagConstraints.EAST;

            // Total Debit
            cc.fill = GridBagConstraints.NONE;
            panelTotal.add(new JLabel("Débit"), cc);
            cc.fill = GridBagConstraints.HORIZONTAL;
            cc.gridx++;
            cc.weightx = 1;
            panelTotal.add(this.labelTotalDebit, cc);

            // Total Credit
            cc.gridy++;
            cc.gridx = 0;
            cc.weightx = 0;
            cc.fill = GridBagConstraints.NONE;
            panelTotal.add(new JLabel("Crédit"), cc);
            cc.weightx = 1;
            cc.gridx++;
            cc.fill = GridBagConstraints.HORIZONTAL;
            panelTotal.add(this.labelTotalCredit, cc);

            // Création auto des comptes
            this.checkCreateCompte = new JCheckBox("Création automatique des comptes");

            c.gridy++;
            c.gridx = 0;
            c.gridwidth = 2;
            c.weightx = 0;
            c.weighty = 0;
            this.add(this.checkCreateCompte, c);

            // Création de ligne auto
            this.checkCreateLineAuto = new JCheckBox("Ligne de contrepartie automatique");
            c.gridy++;
            this.checkCreateLineAuto.setSelected(true);
            this.add(this.checkCreateLineAuto, c);

            // Ligne : Warning

            c.gridy++;
            c.weightx = 0;
            c.gridwidth = 2;
            this.labelMotifWarning.setText("Le solde des écritures n'est pas nul!");
            DefaultGridBagConstraints.lockMinimumSize(this.labelMotifWarning);
            this.add(this.labelMotifWarning, c);
            this.labelMotifWarning.setVisible(false);
            c.gridwidth = 1;

            c.gridy--;
            c.gridx = 2;

            c.anchor = GridBagConstraints.EAST;
            c.weightx = 0;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            c.gridwidth = 2;
            c.gridheight = 2;
            panelTotal.setPreferredSize(new Dimension(250, panelTotal.getPreferredSize().height));
            DefaultGridBagConstraints.lockMinimumSize(panelTotal);
            this.add(panelTotal, c);

            this.model = this.tableKm.getModel();
            this.creditIndex = this.model.getColumnIndexForElement(this.tableKm.getCreditElement());
            this.debitIndex = this.model.getColumnIndexForElement(this.tableKm.getDebitElement());

            // Listeners

            this.tableKm.getModel().addTableModelListener(this.tableListener);

            this.addSQLObject(this.textNom, "NOM");
            this.addRequiredSQLObject(this.date, "DATE");
            this.addRequiredSQLObject(this.comboJrnl, "ID_JOURNAL");
            this.comboJrnl.setButtonsVisible(false);
            this.textNom.getDocument().addDocumentListener(new SimpleDocumentListener() {
                String previousName = "";

                @Override
                public void update(DocumentEvent e) {
                    SaisieKmComponent.this.defaultEcritureRowVals.put("NOM_ECRITURE", SaisieKmComponent.this.textNom.getText());
                    tableKm.fillEmptyEntryLabel(previousName, textNom.getText());
                    previousName = textNom.getText();
                }
            });

            this.checkCreateCompte.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    SaisieKmComponent.this.tableKm.setCreateAutoActive(SaisieKmComponent.this.checkCreateCompte.isSelected());
                    updateValidState();
                }
            });

            // Lock (after adding to this *and* after adding to the request since some items
            // initialize themselves when added)
            DefaultGridBagConstraints.lockMinimumSize(this.comboJrnl);
        }

        public int insert(SQLRow order) {
            final int id = super.insert(order);

            this.tableKm.updateField("ID_SAISIE_KM", id);

            try {
                SQLUtils.executeAtomic(Configuration.getInstance().getSystemRoot().getDataSource(), new SQLUtils.SQLFactory<Object>() {
                    @Override
                    public Object create() throws SQLException {

                        GenerationMvtSaisieKm gen = new GenerationMvtSaisieKm(id);
                        int idMvt = gen.genereMouvement();

                        // maj de l'id du mouvement correspondant
                        SQLRowValues valEcriture = new SQLRowValues(SaisieKmSQLElement.this.getTable());
                        valEcriture.put("ID_MOUVEMENT", new Integer(idMvt));
                        if (valEcriture.getInvalid() == null) {

                            valEcriture.update(id);
                        }

                        SQLElement eltMvt = Configuration.getInstance().getDirectory().getElement("MOUVEMENT");
                        final SQLRow rowMvt = eltMvt.getTable().getRow(idMvt);
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(SaisieKmComponent.this, "Numéro de mouvement associé : " + rowMvt.getObject("NUMERO"));
                            }
                        });
                        return null;
                    }
                });
                this.updateEcriture(getTable().getRow(id));
            } catch (SQLException exn) {
                ExceptionHandler.handle("Erreur lors de la création des écritures associées à la saisie au kilometre.", exn);
            }
            return id;
        }

        public void loadMouvement(int idMvt) {
            this.tableKm.loadMouvement(idMvt, false);
        }

        public void loadContrepassation(int idMvt) {
            this.tableKm.loadMouvement(idMvt, true);
        }

        public void select(SQLRowAccessor r) {
            if (r == null) {
                this.dTemp = this.date.getDate();
            }
            super.select(r);
            if (r != null) {
                this.tableKm.insertFrom(r);
            }
        }

        public void update() {
            super.update();
            this.tableKm.updateField("ID_SAISIE_KM", getSelectedID());
            System.err.println("UPDATE ECRITURE");
            this.updateEcriture(getElement().getTable().getRow(getSelectedID()));
        }

        private Date dTemp = null;

        @Override
        protected SQLRowValues createDefaults() {
            assert SwingUtilities.isEventDispatchThread();
            SQLRowValues rowVals = new SQLRowValues(this.getTable());

            if (this.dTemp != null) {
                rowVals.put("DATE", this.dTemp);
            }
            this.tableKm.getModel().clearRows();
            this.tableKm.revalidate();
            this.tableKm.repaint();

            return rowVals;
        }

        public void updateEcriture(final SQLRow rowSaisieKm) {
            try {
                SQLUtils.executeAtomic(Configuration.getInstance().getSystemRoot().getDataSource(), new SQLUtils.SQLFactory<Object>() {
                    @Override
                    public Object create() throws SQLException {

                        SQLTable ecritureTable = getTable().getBase().getTable("ECRITURE");
                        SQLElement assocElt = Configuration.getInstance().getDirectory().getElement("ASSOCIATION_ANALYTIQUE");
                        final SQLTable tableElt = getTable().getBase().getTable("SAISIE_KM_ELEMENT");
                        List<SQLRow> myListKmItem = rowSaisieKm.getReferentRows(tableElt);

                        List<SQLRow> listEcr = rowSaisieKm.getForeignRow("ID_MOUVEMENT").getReferentRows(ecritureTable);

                        if (myListKmItem != null) {

                            for (SQLRow rowKmElement : myListKmItem) {

                                int idCpt = ComptePCESQLElement.getId(rowKmElement.getString("NUMERO"), rowKmElement.getString("NOM"));

                                if (rowKmElement.getID() > 1) {
                                    SQLRowValues vals = new SQLRowValues(ecritureTable);
                                    vals.put("ID_COMPTE_PCE", idCpt);
                                    vals.put("COMPTE_NUMERO", rowKmElement.getString("NUMERO"));
                                    vals.put("COMPTE_NOM", rowKmElement.getString("NOM"));
                                    vals.put("DEBIT", rowKmElement.getObject("DEBIT"));
                                    vals.put("CREDIT", rowKmElement.getObject("CREDIT"));
                                    vals.put("DATE", rowSaisieKm.getObject("DATE"));
                                    SQLRow rowJournal = rowSaisieKm.getForeignRow("ID_JOURNAL");
                                    vals.put("ID_JOURNAL", rowJournal.getID());
                                    vals.put("JOURNAL_NOM", rowJournal.getString("NOM"));
                                    vals.put("JOURNAL_CODE", rowJournal.getString("CODE"));
                                    vals.put("NOM", rowKmElement.getObject("NOM_ECRITURE"));
                                    if (tableElt.contains("NOM_PIECE")) {
                                        vals.put("NOM_PIECE", rowKmElement.getObject("NOM_PIECE"));
                                    }
                                    if (rowKmElement.getInt("ID_ECRITURE") > 1) {
                                        SQLRow rowTmp = ecritureTable.getRow(rowKmElement.getInt("ID_ECRITURE"));

                                        if (!rowTmp.getBoolean("VALIDE")) {
                                            vals.update(rowKmElement.getInt("ID_ECRITURE"));
                                        } else {
                                            System.err.println("Impossible de modifier une ecriture valide");
                                        }

                                    } else {

                                        vals.put("ID_MOUVEMENT", rowSaisieKm.getObject("ID_MOUVEMENT"));

                                        if (MouvementSQLElement.isEditable(rowSaisieKm.getInt("ID_MOUVEMENT"))) {
                                            SQLRow rowEcr = vals.insert();
                                            SQLRowValues rowElementVals = rowKmElement.createEmptyUpdateRow();
                                            rowElementVals.put("ID_ECRITURE", rowEcr.getID());
                                            rowKmElement = rowElementVals.update();
                                        }

                                    }

                                    for (SQLRow sqlRow : rowKmElement.getReferentRows(assocElt.getTable())) {
                                        SQLRowValues rowVals = sqlRow.asRowValues();
                                        rowVals.put("ID_ECRITURE", rowKmElement.getInt("ID_ECRITURE"));
                                        rowVals.commit();
                                    }
                                    List<SQLRow> l = new ArrayList<SQLRow>(listEcr);
                                    for (SQLRow sqlRow : l) {
                                        if (sqlRow.getID() == rowKmElement.getInt("ID_ECRITURE")) {
                                            listEcr.remove(sqlRow);
                                        }
                                    }
                                }

                            }
                        }

                        if (!listEcr.isEmpty()) {
                            final EcritureSQLElement e = (EcritureSQLElement) Configuration.getInstance().getDirectory().getElement(ecritureTable);
                            for (SQLRow sqlRow : listEcr) {
                                e.archiveEcriture(sqlRow);
                            }
                        }
                        return null;
                    }
                });
            } catch (SQLException exn) {
                ExceptionHandler.handle("Erreur lors de la mise à jour des écritures associées à la saisie au kilometre.", exn);
            }
        }

        @Override
        public synchronized ValidState getValidState() {
            assert SwingUtilities.isEventDispatchThread();
            return super.getValidState().and(this.validState).and(this.validStateCloture);
        }

        private void updateValidState() {
            assert SwingUtilities.isEventDispatchThread();
            ValidState state = ValidState.create(!this.labelMotifWarning.isVisible(), this.labelMotifWarning.getText());
            if (!this.isCompteExist && !this.checkCreateCompte.isSelected())
                state = state.and(ValidState.createCached(false, "Certains comptes n'existent pas"));
            if (!this.allLineValid)
                state = state.and(ValidState.createCached(false, "Certaines lignes n'ont pas de crédit ni de débit"));
            this.setValidState(state);
        }

        private void setValidState(final ValidState state) {
            assert SwingUtilities.isEventDispatchThread();
            if (!state.equals(this.validState)) {
                this.validState = state;
                fireValidChange();
            }
        }

        private void setTotals(final long totalCred, final long totalDeb) {
            assert SwingUtilities.isEventDispatchThread();
            this.totalCred = totalCred;
            this.totalDeb = totalDeb;
            this.labelTotalCredit.setText(GestionDevise.currencyToString(this.totalCred));
            this.labelTotalDebit.setText(GestionDevise.currencyToString(this.totalDeb));

            final long diff = this.totalDeb - this.totalCred;
            final String reason;
            if (diff == 0) {
                reason = null;
            } else if (diff > 0) {
                reason = "Le solde des écritures n'est pas nul! Il manque " + GestionDevise.currencyToString(diff) + " en crédit.";
            } else {
                reason = "Le solde des écritures n'est pas nul! Il manque " + GestionDevise.currencyToString(diff) + " en débit.";
            }
            this.labelMotifWarning.setVisible(reason != null);
            if (reason != null)
                this.labelMotifWarning.setText(reason);
        }

        private void tableChanged(TableModelEvent e) {
            assert SwingUtilities.isEventDispatchThread();
            int col = e.getColumn();
            if (e.getType() == TableModelEvent.UPDATE && (col == this.model.getColumnCount() - 1 || col == this.model.getColumnCount() - 2) && e.getFirstRow() >= 0
                    && e.getFirstRow() < this.model.getRowCount()) {
                SQLRowValues rowVals = this.model.getRowValuesAt(e.getFirstRow());
                Long longValue = (Long) this.model.getValueAt(e.getFirstRow(), col);
                if (rowVals.getReferentRows(getTable().getTable("ASSOCIATION_ANALYTIQUE")).size() > 0 && longValue != null && longValue != 0) {
                    JOptionPane.showMessageDialog(SwingUtilities.getRootPane(this.labelTotalCredit), "Pensez à mettre à jour la répartition analytique!");
                }
            }

            long totalCred = 0;
            long totalDeb = 0;
            long totalCredWithNoValid = 0;
            long totalDebWithNoValid = 0;
            boolean isCompteExist = true;
            boolean allLineValid = true;
            for (int i = 0; i < this.model.getRowCount(); i++) {
                final boolean rowValid = this.model.isRowValid(i);
                final long fTc = ((Number) this.model.getValueAt(i, this.creditIndex)).longValue();
                final long fTd = ((Number) this.model.getValueAt(i, this.debitIndex)).longValue();
                String numCpt = this.model.getValueAt(i, this.model.getColumnIndexForElement(this.tableKm.getNumeroCompteElement())).toString();
                isCompteExist &= ComptePCESQLElement.isExist(numCpt);
                // see SaisieKmItemTable RowValuesTableModel, one of the values will be zeroed
                if (fTc != 0 && fTd != 0)
                    return;

                if (rowValid) {
                    totalCred += fTc;
                    totalDeb += fTd;
                }
                totalCredWithNoValid += fTc;

                totalDebWithNoValid += fTd;

                // Les lignes à 0 sont permises
            }
            this.tableKm.revalidate();
            this.tableKm.repaint();
            this.isCompteExist = isCompteExist;
            this.allLineValid = allLineValid;
            this.setTotals(totalCred, totalDeb);
            updateValidState();

            // add a row to balance totals
            final long diffWithNoValid = totalDebWithNoValid - totalCredWithNoValid;
            if (diffWithNoValid != 0) {
                if (diffWithNoValid > 0) {
                    this.defaultEcritureRowVals.put("DEBIT", Long.valueOf(0));
                    this.defaultEcritureRowVals.put("CREDIT", Long.valueOf(diffWithNoValid));

                } else {
                    this.defaultEcritureRowVals.put("DEBIT", Long.valueOf(-diffWithNoValid));
                    this.defaultEcritureRowVals.put("CREDIT", Long.valueOf(0));

                }
                if (this.model.isLastRowValid() && checkCreateLineAuto.isSelected()) {
                    this.tableKm.getModel().addRow(new SQLRowValues(this.defaultEcritureRowVals));
                }
            } else {
                this.defaultEcritureRowVals.put("DEBIT", Long.valueOf(0));
                this.defaultEcritureRowVals.put("CREDIT", Long.valueOf(0));
            }
        }

    }

    @Override
    protected String createCode() {
        return createCodeFromPackage() + ".userentry";
    }
}
