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
 
 package org.openconcerto.sql.ui.light;

import java.util.Calendar;
import java.util.List;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.sqlobject.ElementComboBoxUtils;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.utils.ui.StringWithId;

/**
 * Fill value from default or database
 */
public class LightUIPanelFiller {
    private final LightUIPanel panel;

    public LightUIPanelFiller(LightUIPanel panel) {
        this.panel = panel;
    }

    public void fillWithDefaultValues() {
        final int lineCount = this.panel.getSize();
        for (int i = 0; i < lineCount; i++) {
            final LightUILine l = this.panel.getLine(i);
            final int elementCount = l.getSize();
            for (int j = 0; j < elementCount; j++) {
                final LightUIElement element = l.getElement(j);
                if (element.getType() == LightUIElement.TYPE_DATE) {
                    // Set date to current server date
                    element.setValue(String.valueOf(System.currentTimeMillis()));
                }
            }

        }
    }

    public void fillFromId(final PropsConfiguration configuration, final SQLTable table, final long id) {
        System.err.println("LightUIFrameFiller.fillFromId() " + id);
        final SQLRow row = table.getRow((int) id);
        this.fillFromRow(this.panel, configuration, table, row);
    }
    
    public void fillFromRow(final LightUIPanel panel, final PropsConfiguration configuration, final SQLTable table, final SQLRow row) {
        final int lineCount = panel.getSize();
        for (int i = 0; i < lineCount; i++) {
            final LightUILine l = panel.getLine(i);
            final int elementCount = l.getSize();
            for (int j = 0; j < elementCount; j++) {
                final LightUIElement element = l.getElement(j);
                final SQLField field = configuration.getFieldMapper().getSQLFieldForItem(element.getId());

                int type = element.getType();
                if (type == LightUIElement.TYPE_TEXT_FIELD) {

                    if (field == null) {
                        Log.get().severe("No field found for text field : " + element.getId());
                        continue;
                    }
                    element.setValue(row.getString(field.getName()));
                } else if (type == LightUIElement.TYPE_COMBOBOX_ELEMENT) {
                    // send: id,value
                    SQLTable foreignTable = field.getForeignTable();
                    final List<SQLField> fieldsToFetch = configuration.getDirectory().getElement(foreignTable).getComboRequest().getFields();

                    if (row.getObject(field.getName()) != null) {
                        final Where where = new Where(foreignTable.getKey(), "=", row.getLong(field.getName()));
                        List<SQLRowValues> fetchedRows = ElementComboBoxUtils.fetchRows(configuration, foreignTable, fieldsToFetch, where);
                        if (fetchedRows.size() > 1) {
                            throw new IllegalStateException("multiple rows fetched for id " + row.getID() + " on table " + table.getName());
                        }

                        for (final SQLRowValues vals : fetchedRows) {
                            StringWithId s = ElementComboBoxUtils.createItem(configuration, foreignTable, vals, fieldsToFetch);
                            element.setValue(s.toCondensedString());
                        }
                    } else {
                        element.setValue(null);
                    }

                } else if (type == LightUIElement.TYPE_CHECKBOX) {
                    if (row.getBoolean(field.getName())) {
                        element.setValue("true");
                    } else {
                        element.setValue("false");
                    }
                } else if (type == LightUIElement.TYPE_DATE) {
                    Calendar date = row.getDate(field.getName());
                    if (date != null) {
                        element.setValue(String.valueOf(row.getDate(field.getName()).getTimeInMillis()));
                    }
                } else if(type == LightUIElement.TYPE_PANEL) {
                    this.fillFromRow((LightUIPanel) element, configuration, table, row);
                }
            }
        }
    }
}
