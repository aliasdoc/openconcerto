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

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.sqlobject.ElementComboBoxUtils;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIComboBoxElement;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.io.JSONConverter;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Fill value from default or database
 */
public class LightUIPanelFiller {
    private final LightUIPanel panel;

    public LightUIPanelFiller(LightUIPanel panel) {
        this.panel = panel;
    }

    public void fillWithDefaultValues() {
        final int panelChildCount = this.panel.getChildrenCount();
        for (int i = 0; i < panelChildCount; i++) {
            final LightUILine panelChild = this.panel.getChild(i, LightUILine.class);
            final int lineChildCount = panelChild.getChildrenCount();
            for (int j = 0; j < lineChildCount; j++) {
                final LightUIElement element = panelChild.getChild(j);
                if (element.getType() == LightUIElement.TYPE_DATE) {
                    // Set date to current server date
                    element.setValue(JSONConverter.getJSON(new Date(System.currentTimeMillis())).toString());
                }
            }
        }
    }

    public void fillFromRow(final PropsConfiguration configuration, final SQLRowAccessor row) {
        this.fillFromRow(this.panel, configuration, row);
    }

    private void fillFromRow(final LightUIPanel panel, final PropsConfiguration configuration, SQLRowAccessor row) {
        final int panelChildCount = panel.getChildrenCount();
        // Convert as sqlrow if possible to get all values from db
        if (row.hasID()) {
            row = row.asRow();
        }
        for (int i = 0; i < panelChildCount; i++) {
            final LightUILine panelChild = panel.getChild(i, LightUILine.class);
            final int lineChildCount = panelChild.getChildrenCount();
            for (int j = 0; j < lineChildCount; j++) {
                final LightUIElement element = panelChild.getChild(j);
                final SQLField field = configuration.getFieldMapper().getSQLFieldForItem(element.getId());

                int type = element.getType();
                if (type == LightUIElement.TYPE_TEXT_FIELD || type == LightUIElement.TYPE_TEXT_AREA) {

                    if (field == null) {
                        Log.get().severe("No field found for text field : " + element.getId());
                        continue;
                    }
                    element.setValue(row.getString(field.getName()));
                } else if (type == LightUIElement.TYPE_COMBOBOX) {
                    // send: id,value
                    final LightUIComboBox combo = (LightUIComboBox) element;
                    if (!combo.isFillFromConvertor()) {
                        SQLTable foreignTable = field.getForeignTable();
                        final List<SQLField> fieldsToFetch = configuration.getDirectory().getElement(foreignTable).getComboRequest().getFields();

                        if (row.getObject(field.getName()) != null) {
                            final Where where = new Where(foreignTable.getKey(), "=", row.getForeignID(field.getName()));
                            final SQLRowValues graph = ElementComboBoxUtils.getGraphToFetch(configuration, foreignTable, fieldsToFetch);
                            final List<Tuple2<Path, List<FieldPath>>> expanded = ElementComboBoxUtils.expandGroupBy(graph, configuration.getDirectory());
                            List<SQLRowValues> fetchedRows = ElementComboBoxUtils.fetchRows(graph, where);
                            if (fetchedRows.size() > 1) {
                                throw new IllegalStateException("multiple rows fetched, id: " + ((row.hasID()) ? row.getID() : "undefined") + " table: " + row.getTable().getName());
                            }

                            for (final SQLRowValues vals : fetchedRows) {
                                LightUIComboBoxElement value = ElementComboBoxUtils.createLightUIItem(expanded, vals);
                                combo.setSelectedValue(value);
                            }
                        } else {
                            element.setValue(null);
                        }
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
                        element.setValue(JSONConverter.getJSON(date).toString());
                    }
                } else if (type == LightUIElement.TYPE_PANEL) {
                    this.fillFromRow((LightUIPanel) element, configuration, row);
                }
            }
        }
    }
}
