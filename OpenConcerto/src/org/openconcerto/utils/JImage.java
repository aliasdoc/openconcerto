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
 
 package org.openconcerto.utils;

import org.openconcerto.utils.i18n.TM;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URI;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * Un composant très simple qui affiche une image.
 * 
 * @author ILM Informatique 4 juin 2004
 */
public class JImage extends JComponent {

    private Image image;
    private ImageIcon icon;

    private boolean centered;
    private URI hyperlink;
    private MouseListener hyperlinkL;

    /**
     * Cree une JImage a partir d'un nom de fichier.
     * 
     * @param fileName le nom du fichier image, eg "dir/pic.png".
     */
    public JImage(String fileName) {
        this(new ImageIcon(fileName));
    }

    public JImage(URL url) {
        this(new ImageIcon(url));
    }

    public JImage(ImageIcon img) {
        this(img.getImage());
        this.icon = img;
    }

    public JImage(Image img) {
        this.image = img;
        this.icon = null;
        this.setOpaque(true);
    }

    public void check() {
        if (this.image == null || this.image.getHeight(null) <= 0) {
            throw new IllegalStateException();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (this.isOpaque()) {
            g.setColor(this.getBackground());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
        }
        final int dx;
        final int dy;
        if (!this.centered) {
            dx = dy = 0;
        } else {
            dx = (this.getWidth() - this.image.getWidth(null)) / 2;
            dy = (this.getHeight() - this.image.getHeight(null)) / 2;
        }
        g.drawImage(this.image, dx, dy, null);
    }

    public ImageIcon getImageIcon() {
        if (this.icon == null) {
            this.icon = new ImageIcon(this.image);
        }
        return this.icon;
    }

    public Dimension getPreferredSize() {
        return this.getMinimumSize();
    }

    public Dimension getMinimumSize() {
        return new Dimension(this.image.getWidth(null), this.image.getHeight(null));
    }

    public void setCenterImage(boolean t) {
        this.centered = true;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public final URI getHyperlink() {
        return this.hyperlink;
    }

    public final void setHyperLink(final URI uri) {
        if (!CompareUtils.equals(uri, this.hyperlink)) {
            if (this.hyperlink == null) {
                this.hyperlinkL = new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        try {
                            Desktop.getDesktop().browse(getHyperlink());
                        } catch (Exception ex) {
                            ExceptionHandler.handle(e.getComponent(), TM.tr("linkOpenError", getHyperlink()), ex);
                        }
                        e.consume();
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                    }
                };
                this.addMouseListener(this.hyperlinkL);
            }
            this.hyperlink = uri;
            if (this.hyperlink == null) {
                this.removeMouseListener(this.hyperlinkL);
                this.hyperlinkL = null;
            }
        }
    }
}
