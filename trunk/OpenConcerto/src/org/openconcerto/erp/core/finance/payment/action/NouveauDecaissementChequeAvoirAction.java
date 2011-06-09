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
 
 package org.openconcerto.erp.core.finance.payment.action;

import org.openconcerto.erp.action.CreateFrameAbstractAction;
import org.openconcerto.erp.core.finance.payment.ui.GestionChequeFrame;
import org.openconcerto.erp.core.finance.payment.ui.ListeDesChequesAvoirADecaisserPanel;
import org.openconcerto.erp.model.LoadingTableListener;

import javax.swing.Action;
import javax.swing.JFrame;

public class NouveauDecaissementChequeAvoirAction extends CreateFrameAbstractAction {

    public NouveauDecaissementChequeAvoirAction() {
        super();
        this.putValue(Action.NAME, "Chèques d'avoir à décaisser");
    }

    @Override
    public JFrame createFrame() {
        final ListeDesChequesAvoirADecaisserPanel p = new ListeDesChequesAvoirADecaisserPanel();
        final GestionChequeFrame gestionChequeFrame = new GestionChequeFrame(p, p.getModel(), "Chèques d'avoir à décaisser");
        p.getModel().addLoadingListener(new LoadingTableListener() {

            @Override
            public void isLoading(boolean b) {
                gestionChequeFrame.setIsLoading(b);
            }
        });
        p.getModel().loadCheque();
        return gestionChequeFrame;
    }
}
