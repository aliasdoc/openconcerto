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
 
 package org.openconcerto.sql.ui;

import org.openconcerto.sql.TM;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.SystemInfoPanel;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A panel displaying various informations.
 */
public class InfoPanel extends JPanel {

    public InfoPanel() {
        super(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.weightx = 1;
        c.weighty = 1;
        this.add(createTitle("infoPanel.softwareTitle"), c);
        c.gridy++;
        this.add(new SoftwareInfoPanel(), c);
        c.gridy++;
        this.add(createTitle("infoPanel.systemTitle"), c);
        c.gridy++;
        this.add(new SystemInfoPanel(), c);
    }

    private JLabel createTitle(final String text) {
        final JLabel res = new JLabel(TM.tr(text));
        final Font font = res.getFont();
        res.setFont(font.deriveFont(font.getSize2D() * 1.2f).deriveFont(Font.BOLD));
        res.setToolTipText(TM.tr("infoPanel.refresh"));
        res.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                refresh(Arrays.asList(InfoPanel.this.getComponents()).indexOf(res) + 1);
            }
        });
        return res;
    }

    public final void refresh() {
        this.refresh(-1);
    }

    private final void refresh(final int index) {
        if (index < 0 || index == 1)
            ((SoftwareInfoPanel) this.getComponent(1)).refresh();
        if (index < 0 || index == 3)
            ((SystemInfoPanel) this.getComponent(3)).refresh();
    }
}
