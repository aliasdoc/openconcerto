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
 
 /*
 * ListeAddFrame created on 23 oct. 2003
 */
package org.openconcerto.sql.view;

import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.view.list.IListe;
import org.openconcerto.ui.FrameUtil;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;

import javax.swing.JButton;

/**
 * @author ILM Informatique
 */
public class ListeAddPanel extends IListPanel {

    private EditFrame editFrame;

    public ListeAddPanel(SQLElement component) {
        super(component);
    }

    public ListeAddPanel(SQLElement component, IListe list) {
        super(component, list);
    }

    public ListeAddPanel(SQLElement component, IListe list, String variant) {
        super(component, list, variant);
    }

    @Override
    protected void addComponents(Container container, GridBagConstraints c) {
        super.addComponents(container, c);
        // If write protected (because of rights or locked row) still allow to display
        this.btnMngr.setFallback(this.buttonModifier, "modify", "display");
    }

    @Override
    protected void handleAction(JButton source, ActionEvent evt) {
        if (source == this.buttonModifier) {
            // can't change mode of EditPanel as a function of our FALLBACK_KEY (i.e. modify or
            // display). Would have to change our EditFrame instance, but some of our caller will
            // need to be changed. For now, rely on SQLComponent safeties.
            this.getEditFrame().selectionId(this.getListe().getSelectedId(), -1);
            FrameUtil.show(getEditFrame());
        } else {
            super.handleAction(source, evt);
        }
    }

    @Override
    protected boolean modifyIsImmediate() {
        // EditFrame is displayed
        return false;
    }

    @Override
    public SQLComponent getModifComp() {
        return this.getEditFrame().getPanel().getSQLComponent();
    }

    protected final EditFrame getEditFrame() {
        if (this.editFrame == null) {
            this.editFrame = new EditFrame(this.element, EditPanel.MODIFICATION);
            this.editFrame.getPanel().setIListe(getListe());
        }
        return this.editFrame;
    }

}
