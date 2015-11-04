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
 
 package org.openconcerto.ui;

import org.openconcerto.ui.table.TimestampTableCellEditor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.UIManager;

import org.jopencalendar.ui.DatePickerPanel;

public class TimestampEditorPanel extends JPanel implements ActionListener {

    private TimeTextField time;
    private JPanel panelHour;
    private DatePickerPanel pickerPanel;
    private List<ActionListener> listeners = new Vector<ActionListener>();
    private TimestampTableCellEditor aCellEditor;
    private Calendar c = Calendar.getInstance();

    public TimestampEditorPanel() {
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 3, 0, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0;
        c.weighty = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.HORIZONTAL;

        this.panelHour = new JPanel(new GridBagLayout());

        final JLabel labelHour = new JLabel("Heure : ");
        labelHour.setFont(labelHour.getFont().deriveFont(Font.BOLD));
        this.panelHour.add(labelHour, c);
        c.gridx++;
        this.time = new TimeTextField();
        this.time.setMinimumSize(new Dimension(time.getPreferredSize()));
        this.panelHour.add(this.time, c);
        c.gridx++;

        final JButton buttonClose = new JButton(new ImageIcon(TimestampEditorPanel.class.getResource("close_popup_gray.png")));
        buttonClose.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (TimestampEditorPanel.this.aCellEditor != null) {
                    TimestampEditorPanel.this.aCellEditor.hidePopup();
                    TimestampEditorPanel.this.aCellEditor.stopCellEditing();
                }
            }
        });
        buttonClose.setBorderPainted(false);
        buttonClose.setOpaque(false);
        buttonClose.setFocusPainted(false);
        buttonClose.setContentAreaFilled(false);
        buttonClose.setMargin(new Insets(1, 1, 1, 1));
        c.gridx = 0;
        this.panelHour.setOpaque(false);
        this.add(this.panelHour, c);

        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.NORTHEAST;
        c.weightx = 1;
        c.gridx++;
        this.add(buttonClose, c);
        c.gridy++;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(4, 0, 0, 0);
        c.fill = GridBagConstraints.HORIZONTAL;
        this.add(new JSeparator(JSeparator.HORIZONTAL), c);
        setBackground(Color.WHITE);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.insets = new Insets(0, 0, 0, 0);
        this.pickerPanel = new DatePickerPanel();

        add(this.pickerPanel, c);
        c.gridy++;

        setBorder(BorderFactory.createLineBorder(Color.BLACK));

        this.pickerPanel.addPropertyChangeListener("timeInMillis", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                stateChanged();
                fireTimeChangedPerformed();
            }
        });
        this.time.addPropertyChangeListener("value", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                stateChanged();
                fireTimeChangedPerformed();
            }
        });

    }

    public void setTime(Date time) {
        c.setTimeInMillis(time.getTime());
        // update UI
        final int hour = c.get(Calendar.HOUR_OF_DAY);
        final int minute = c.get(Calendar.MINUTE);
        this.pickerPanel.setSelectedDate(c);
        this.time.setTime(hour, minute);
    }

    public Timestamp getTime() {
        return new Timestamp(c.getTimeInMillis());
    }

    public void actionPerformed(ActionEvent e) {
        stateChanged();
        fireTimeChangedPerformed();
    }

    public void stateChanged() {
        c.setTime(pickerPanel.getSelectedDate());
        c.set(Calendar.HOUR_OF_DAY, time.getHours());
        c.set(Calendar.MINUTE, time.getMinutes());

    }

    private void fireTimeChangedPerformed() {
        final int size = this.listeners.size();
        for (int i = 0; i < size; i++) {
            final ActionListener element = (ActionListener) this.listeners.get(i);
            element.actionPerformed(null);
        }

    }

    public void addActionListener(ActionListener listener) {
        this.listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        this.listeners.remove(listener);
    }

    public void setCellEditor(TimestampTableCellEditor editor) {
        this.aCellEditor = editor;
    }

    public void setHourVisible(boolean b) {
        this.panelHour.setVisible(b);
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        final TimestampEditorPanel t = new TimestampEditorPanel();
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, 2014);
        c.set(Calendar.DAY_OF_YEAR, 8);
        c.set(Calendar.HOUR_OF_DAY, 13);
        c.set(Calendar.MINUTE, 14);

        t.setTime(c.getTime());
        t.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("TimestampEditorPanel got :" + t.getTime());

            }
        });
        f.setContentPane(t);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
