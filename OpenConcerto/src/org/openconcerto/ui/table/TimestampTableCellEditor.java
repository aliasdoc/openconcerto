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
 
 package org.openconcerto.ui.table;

import org.openconcerto.ui.FormatEditor;
import org.openconcerto.ui.TimestampEditorPanel;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.EventObject;

import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class TimestampTableCellEditor extends FormatEditor implements ActionListener {

    private Calendar calendar;
    private Date currentvalue, initialvalue;
    private JPopupMenu aPopup;
    private boolean popupOpen = false;
    private final TimestampEditorPanel content = new TimestampEditorPanel();
    private boolean allowNull = true;

    public TimestampTableCellEditor(boolean showHour) {
        this();
        this.content.setHourVisible(showHour);
    }

    public TimestampTableCellEditor() {
        super(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT));
        this.calendar = Calendar.getInstance();
        this.content.setBorder(null);
    }

    public void setAllowNull(boolean b) {
        this.allowNull = b;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        final Component c = super.getTableCellEditorComponent(table, value, isSelected, row, column);
        Date time = (Date) value;
        if (time == null) {
            time = new Timestamp(System.currentTimeMillis());
        }
        this.content.setTime(time);
        this.calendar.setTime(time);
        this.currentvalue = time;
        this.initialvalue = time;

        final Point p = new Point(0, 0 + table.getRowHeight(row));

        if (this.aPopup != null) {
            this.content.removeActionListener(this);
            this.aPopup.hide();
            this.aPopup = null;
        }

        JTextField t = (JTextField) c;

        this.aPopup = new JPopupMenu();
        this.aPopup.add(this.content);

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Invoke later to avoid paint issue
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        showPopup(c, p);
                    }
                });
            }
        });
        t.addKeyListener(new KeyListener() {

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    // Invoke later to avoid paint issue
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            showPopup(c, p);
                        }
                    });
                }

            }
        });

        this.content.setCellEditor(this);
        this.content.addActionListener(this);
        return c;
    }

    public void cancelCellEditing() {
        hidePopup();
        this.currentvalue = this.initialvalue;
        super.cancelCellEditing();
    }

    public void hidePopup() {
        this.popupOpen = false;
        this.content.removeActionListener(this);
        if (this.aPopup != null) {
            this.aPopup.setVisible(false);

        }
    }

    public void showPopup(Component c, Point p) {
        this.popupOpen = true;
        this.aPopup.show(c, p.x, p.y);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                content.requestFocus();

            }
        });
    }

    public boolean isPopupOpen() {
        return popupOpen;
    }

    public Object getCellEditorValue() {
        final Date v = (Date) super.getCellEditorValue();
        long t = System.currentTimeMillis();
        if (v != null) {
            t = v.getTime();
        } else if (this.allowNull) {
            return null;
        }
        return new Timestamp(t);
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public void actionPerformed(ActionEvent e) {
        this.currentvalue = this.content.getTime();
        this.delegate.setValue(this.currentvalue);
    }
}
