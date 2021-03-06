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
 
 package org.openconcerto.erp.core.customerrelationship.customer.action;

import org.openconcerto.erp.action.CreateListFrameAbstractAction;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.core.sales.invoice.ui.EcheanceRenderer;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.IListFrame;
import org.openconcerto.sql.view.ListeAddPanel;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.sql.view.list.SQLTableModelSource;

import java.util.Set;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JTable;

public class ListeDesClientsAction extends CreateListFrameAbstractAction {

    public ListeDesClientsAction() {
        super();
        this.putValue(Action.NAME, "Liste des clients");
    }

    @Override
    public String getTableName() {
        return "CLIENT";
    }

    protected SQLTableModelSource getTableSource() {
        SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
        return Configuration.getInstance().getDirectory().getElement(tableClient).getTableSource(true);
    }

    public JFrame createFrame() {
        SQLTable tableClient = ((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete().getTable("CLIENT");
        SQLTable tableModeReglement = Configuration.getInstance().getDirectory().getElement("MODE_REGLEMENT").getTable();

        final ListeAddPanel panel = new ListeAddPanel(Configuration.getInstance().getDirectory().getElement(tableClient), new IListe(getTableSource()));
        IListFrame frame = new IListFrame(panel);

        // Renderer
        final EcheanceRenderer rend = EcheanceRenderer.getInstance();
        JTable jTable = frame.getPanel().getListe().getJTable();
        for (int i = 0; i < jTable.getColumnCount(); i++) {
            int realColIndex = frame.getPanel().getListe().getJTable().getColumnModel().getColumn(i).getModelIndex();
            final Set<SQLField> fields = frame.getPanel().getListe().getSource().getColumn(realColIndex).getFields();
            // System.err.println("Column " + column + " Fields : " + fields);
            if (fields.contains(tableModeReglement.getField("AJOURS"))) {

                // if (jTable.getColumnClass(i) == Long.class || jTable.getColumnClass(i) ==
                // BigInteger.class) {
                jTable.getColumnModel().getColumn(i).setCellRenderer(rend);
            }
        }

        panel.setSearchFullMode(true);
        panel.setSelectRowOnAdd(false);
        return frame;
    }
}
