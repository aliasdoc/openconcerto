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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.light.ComboValueConvertor;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.JSONToLightUIConvertor;
import org.openconcerto.ui.light.LightUICheckBox;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIDate;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.ui.light.StringValueConvertor;
import org.openconcerto.utils.io.JSONConverter;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;

import net.minidev.json.JSONObject;

public class LightEditFrame extends LightUIFrame {
    private static final String EDIT_MODE_JSON_KEY = "edit-mode";

    private Group group;
    private SQLRowValues sqlRow;

    private EditMode editMode = EditMode.READONLY;

    // Init from json constructor
    public LightEditFrame(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightEditFrame(final LightEditFrame frame) {
        super(frame);
        this.sqlRow = frame.sqlRow;
        this.group = frame.group;
        this.editMode = frame.editMode;
    }

    public LightEditFrame(final Configuration conf, final Group group, final SQLRowValues sqlRow, final LightUIFrame parentFrame, final EditMode editMode) {
        super(group.getId() + ".edit.frame");
        this.setType(TYPE_FRAME);
        this.setParent(parentFrame);

        this.sqlRow = sqlRow;
        this.group = group;

        this.setEditMode(editMode);
    }

    public void setEditMode(final EditMode editMode) {
        this.editMode = editMode;
        if (editMode.equals(EditMode.READONLY)) {
            this.setReadOnly(true);
        } else {
            this.setReadOnly(false);
        }
    }

    /**
     * Commit the SQLRowValues attached to this frame
     * 
     * @param configuration Current configuration
     * @return The inserted SQLRow
     */
    public SQLRow commitSqlRow(final Configuration configuration) {
        if (this.editMode.equals(EditMode.READONLY)) {
            throw new IllegalArgumentException("Impossible to commit values when the frame is read only");
        }
        final SQLElement sqlElement = configuration.getDirectory().getElement(this.sqlRow.getTable());
        try {
            return this.sqlRow.prune(sqlElement.getPrivateGraph()).commit();
        } catch (final SQLException ex) {
            throw new IllegalArgumentException("Unable to commit SQLRowValues, edit frame ID: " + this.getId());
        }
    }

    public EditMode getEditMode() {
        return this.editMode;
    }

    public Group getGroup() {
        return this.group;
    }

    public SQLRowValues getSqlRow() {
        return this.sqlRow;
    }

    /**
     * Update the SQLRowValues attached to this frame
     * 
     * @param conf
     * @param userId
     */
    public void updateRow(final Configuration configuration, final int userId) {
        if (this.editMode.equals(EditMode.READONLY)) {
            throw new IllegalArgumentException("Impossible to update values when the frame is read only");
        }
        this.updateRow(configuration, this.group, userId);
    }

    private void updateRow(final Configuration configuration, final Group group, final int userId) {
        final FieldMapper fieldMapper = configuration.getFieldMapper();
        if (fieldMapper == null) {
            throw new IllegalStateException("null field mapper");
        }

        final SQLElement sqlElement = configuration.getDirectory().getElement(this.sqlRow.getTable());
        final Map<String, ComboValueConvertor<?>> valueConvertors = sqlElement.getComboConvertors();

        Map<String, CustomEditorProvider> customEditors = null;
        if (this.editMode.equals(EditMode.CREATION)) {
            customEditors = sqlElement.getCustomEditorProviderForCreation(configuration, userId);
        } else {
            customEditors = sqlElement.getCustomEditorProviderForModification(configuration, this.sqlRow, userId);
        }

        this.createRowValues(configuration, fieldMapper, this.group, valueConvertors, customEditors);
        this.setMetaData(userId);
    }

    final protected void createRowValues(final Configuration conf, final FieldMapper fieldMapper, final Group group, final Map<String, ComboValueConvertor<?>> valueConvertors,
            final Map<String, CustomEditorProvider> customEditors) {
        final int itemCount = group.getSize();
        for (int i = 0; i < itemCount; i++) {
            final Item item = group.getItem(i);
            if (item instanceof Group) {
                this.createRowValues(conf, fieldMapper, (Group) item, valueConvertors, customEditors);
            } else {
                final SQLField field = fieldMapper.getSQLFieldForItem(item.getId());
                if (field != null) {
                    final LightUIElement uiElement = this.findChild(item.getId(), false);

                    if (uiElement == null) {
                        throw new IllegalArgumentException("Impossible to find UI Element with id: " + item.getId());
                    }

                    if (!valueConvertors.containsKey(item.getId()) && !customEditors.containsKey(item.getId())) {
                        this.putValueFromUserControl(conf, field, uiElement);
                    } else if (valueConvertors.containsKey(item.getId())) {
                        if (!(uiElement instanceof LightUIComboBox)) {
                            throw new IllegalArgumentException("The UI Element with ID " + item.getId() + ", must be an instance of LightUIComboBox");
                        }
                        final LightUIComboBox combo = (LightUIComboBox) uiElement;
                        if (combo.hasSelectedValue() && combo.getSelectedValue().getId() != 0) {
                            final ComboValueConvertor<?> valueConvertor = valueConvertors.get(item.getId());
                            if (valueConvertor instanceof StringValueConvertor) {
                                this.sqlRow.put(field.getFieldName(), ((StringValueConvertor) valueConvertor).getIdFromIndex(combo.getSelectedValue().getId()));
                            } else {
                                final int selectedId = combo.getSelectedValue().getId();
                                this.sqlRow.put(field.getFieldName(), selectedId);
                            }
                        } else {
                            this.sqlRow.put(field.getFieldName(), null);
                        }
                    } else if (customEditors.containsKey(item.getId())) {
                        Log.get().warning("Unable to save value of element: " + item.getId());
                    }
                } else {
                    Log.get().warning("No field attached to " + item.getId());
                }
            }
        }
    }

    final protected void putValueFromUserControl(final Configuration conf, final SQLField field, final LightUIElement uiElement) {
        final Class<?> fieldType = field.getType().getJavaType();
        if (field.isKey()) {
            if (!(uiElement instanceof LightUIComboBox)) {
                throw new IllegalArgumentException("Invalid UI Element for field: " + field.getName() + ". When field is foreign key, UI Element must be a LightUIDate");
            }
            final LightUIComboBox combo = (LightUIComboBox) uiElement;

            if (combo.hasSelectedValue()) {
                final SQLRowValues tmp = new SQLRowValues(this.sqlRow.getTable()).put(field.getName(), null);
                conf.getDirectory().getShowAs().expand(tmp);
                final SQLRowValues toFetch = (SQLRowValues) tmp.getForeign(field.getName());
                tmp.remove(field.getName());

                final SQLRowValues fetched = SQLRowValuesListFetcher.create(toFetch).fetchOne(combo.getSelectedValue().getId());
                if (fetched == null) {
                    throw new IllegalArgumentException("Impossible to find Row in database - table: " + field.getForeignTable().getName() + ", id: " + combo.getSelectedValue().getId());
                }

                this.sqlRow.put(field.getFieldName(), fetched);
            } else {
                this.sqlRow.put(field.getFieldName(), null);
            }
        } else if (fieldType.equals(String.class)) {
            this.sqlRow.put(field.getFieldName(), uiElement.getValue());
        } else if (fieldType.equals(Date.class)) {
            if (!(uiElement instanceof LightUIDate)) {
                throw new IllegalArgumentException("Invalid UI Element for field: " + field.getName() + ". When field is Date, UI Element must be a LightUIDate");
            }
            this.sqlRow.put(field.getFieldName(), ((LightUIDate) uiElement).getValueAsDate());
        } else if (fieldType.equals(Boolean.class)) {
            if (!(uiElement instanceof LightUICheckBox)) {
                throw new IllegalArgumentException("Invalid UI Element for field: " + field.getName() + ". When field is Boolean, UI Element must be a LightUICheckBox");
            }
            this.sqlRow.put(field.getFieldName(), ((LightUICheckBox) uiElement).isChecked());
        } else if (fieldType.equals(Timestamp.class)) {
            if (!(uiElement instanceof LightUIDate)) {
                throw new IllegalArgumentException("Invalid UI Element for field: " + field.getName() + ". When field is Date, UI Element must be a LightUIDate");
            }
            this.sqlRow.put(field.getFieldName(), ((LightUIDate) uiElement).getValueAsDate());
        } else if (fieldType.equals(Integer.class)) {
            if (uiElement.getValue() != null && !uiElement.getValue().trim().isEmpty()) {
                if (!uiElement.getValue().matches("^-?\\d+$")) {
                    throw new IllegalArgumentException("Invalid value for field: " + field.getName() + " value: " + uiElement.getValue());
                }
                this.sqlRow.put(field.getFieldName(), Integer.parseInt(uiElement.getValue()));
            } else {
                this.sqlRow.put(field.getFieldName(), null);
            }
        } else {
            Log.get().warning("unsupported type " + fieldType.getName());
        }
    }

    /**
     * Save all referent rows store in LightRowValuesTable
     * 
     * @param group Element edit group
     * @param frame Element edit frame
     * @param row Element saved row
     * @param customEditors List of custom editors used in element edit frame
     */
    final public void saveReferentRows(final Configuration configuration, final SQLRow parentSqlRow, final Map<String, CustomEditorProvider> customEditors) {
        this.saveReferentRows(configuration, this.group, parentSqlRow, customEditors);
    }

    final private void saveReferentRows(final Configuration configuration, final Group group, final SQLRow parentSqlRow, final Map<String, CustomEditorProvider> customEditors) {
        for (int i = 0; i < group.getSize(); i++) {
            final Item item = group.getItem(i);
            if (item instanceof Group) {
                this.saveReferentRows(configuration, (Group) item, parentSqlRow, customEditors);
            } else if (customEditors.containsKey(item.getId())) {
                final LightUIElement element = this.findChild(item.getId(), false);
                if (element instanceof LightRowValuesTable) {
                    final LightRowValuesTable rowValsTable = (LightRowValuesTable) element;
                    final int rowValuesCount = rowValsTable.getRowValuesCount();
                    for (int j = 0; j < rowValuesCount; j++) {
                        SQLRowValues rowValues = rowValsTable.getRowValues(j);
                        if (!rowValues.isFrozen()) {
                            rowValues.put(rowValsTable.getFieldRefName(), parentSqlRow.getID());
                            try {
                                final SQLElement el = configuration.getDirectory().getElement(rowValues.getTable());
                                boolean insertion = !rowValues.hasID();
                                SQLRow rowInserted = rowValues.prune(el.getPrivateGraph()).commit();
                                if (insertion) {
                                    ((SQLElement) el).doAfterLightInsert(rowInserted);
                                }
                            } catch (SQLException e) {
                                throw new IllegalArgumentException(e.getMessage(), e);
                            }
                        }
                    }
                    rowValsTable.archiveDeletedRows(configuration);
                }
            }
        }
    }

    final protected void setMetaData(final int userId) {
        final SQLTable sqlTable = this.sqlRow.getTable();
        // FIXME: Creation user not specified.
        if (this.sqlRow.getObject(sqlTable.getCreationUserField().getName()) == null || this.sqlRow.getObject(sqlTable.getCreationDateField().getName()) == null) {
            this.sqlRow.put(sqlTable.getCreationUserField().getName(), userId);
            this.sqlRow.put(sqlTable.getCreationDateField().getName(), new Date());
        }
        this.sqlRow.put(sqlTable.getModifUserField().getName(), userId);
        this.sqlRow.put(sqlTable.getModifDateField().getName(), new Date());
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {

            @Override
            public LightUIElement convert(JSONObject json) {
                return new LightEditFrame(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightEditFrame(this);
    }

    // TODO: implement JSONAble on SQLRowValues and Group
    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (!this.editMode.equals(EditMode.READONLY)) {
            if (this.editMode.equals(EditMode.CREATION)) {
                json.put(EDIT_MODE_JSON_KEY, 1);
            } else if (this.editMode.equals(EditMode.MODIFICATION)) {
                json.put(EDIT_MODE_JSON_KEY, 2);
            }
        }
        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        final int jsonEditMode = JSONConverter.getParameterFromJSON(json, EDIT_MODE_JSON_KEY, Integer.class, 3);
        if (jsonEditMode == 1) {
            this.editMode = EditMode.CREATION;
        } else if (jsonEditMode == 2) {
            this.editMode = EditMode.MODIFICATION;
        } else if (jsonEditMode == 3) {
            this.editMode = EditMode.READONLY;
        }
    }
}
