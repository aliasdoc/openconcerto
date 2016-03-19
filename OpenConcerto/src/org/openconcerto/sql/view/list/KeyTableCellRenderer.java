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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable.ListenerAndConfig;
import org.openconcerto.sql.model.SQLTableEvent;
import org.openconcerto.sql.model.SQLTableEvent.Mode;
import org.openconcerto.sql.model.SQLTableModifiedListener;
import org.openconcerto.sql.sqlobject.IComboSelectionItem;

import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

public class KeyTableCellRenderer extends DefaultTableCellRenderer {

    private String lastStringValue;
    private Object toSelect;
    private boolean isLoading = false;
    private final SQLElement el;
    private JTable t;
    static private final Map<SQLElement, Map<Integer, IComboSelectionItem>> cacheMap = new HashMap<SQLElement, Map<Integer, IComboSelectionItem>>();

    public KeyTableCellRenderer(final SQLElement el) {
        super();
        this.el = el;

        if (cacheMap.get(this.el) == null) {
            loadCacheAsynchronous();
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        this.t = table;
        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }

    public void setValue(Object value) {

        if (this.isLoading) {
            this.toSelect = value;
            setText("Chargement ...");
            return;
        }

        String newValue = "id non trouvé pour:" + value;
        if (value == null) {
            setText("");
            return;
        }
        try {

            if (value instanceof SQLRowValues) {
                newValue = ((SQLRowValues) value).getString("CODE");
            } else {

                final int id = Integer.parseInt(value.toString());
                Number undefID = this.el.getTable().getUndefinedIDNumber();
                if (undefID == null || id > undefID.intValue()) {
                    IComboSelectionItem item = cacheMap.get(this.el).get(id);
                    if (item != null) {
                        newValue = item.getLabel();
                    }

                    // else {

                    // TODO créer une liste des ids à reloader
                    // this.toSelect = value;
                    // setText("Chargement ...");
                    // loadCacheAsynchronous();
                    // }
                } else {
                    newValue = SQLTableElement.UNDEFINED_STRING;
                }
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();

        }

        this.lastStringValue = newValue;
        setText(newValue);
    }

    private void loadCacheAsynchronous() {
        this.isLoading = true;
        Configuration.getInstance().getNonInteractiveSQLExecutor().execute(new Runnable() {
            public void run() {

                List<IComboSelectionItem> items = KeyTableCellRenderer.this.el.getComboRequest().getComboItems();
                final Map<Integer, IComboSelectionItem> m = new HashMap<Integer, IComboSelectionItem>();
                for (IComboSelectionItem comboSelectionItem : items) {
                    m.put(comboSelectionItem.getId(), comboSelectionItem);
                }
                cacheMap.put(KeyTableCellRenderer.this.el, m);
                KeyTableCellRenderer.this.el.getTable().addPremierTableModifiedListener(new ListenerAndConfig(new SQLTableModifiedListener() {
                    @Override
                    public void tableModified(SQLTableEvent evt) {
                        final int id = evt.getId();
                        if (evt.getMode() == Mode.ROW_DELETED)
                            m.remove(id);
                        else
                            m.put(id, KeyTableCellRenderer.this.el.getComboRequest().getComboItem(id));
                    }
                }, true));

                KeyTableCellRenderer.this.isLoading = false;
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        setValue(KeyTableCellRenderer.this.toSelect);
                        if (t != null)
                            t.repaint();
                    }
                });

            }
        });

    }
}
