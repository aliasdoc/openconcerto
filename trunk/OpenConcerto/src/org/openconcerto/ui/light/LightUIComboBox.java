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
 
 package org.openconcerto.ui.light;

import org.openconcerto.utils.i18n.TranslationManager;
import org.openconcerto.utils.io.JSONConverter;

import java.util.ArrayList;
import java.util.List;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUIComboBox extends LightUIElement implements IUserControl {
    public static final int TYPE_COMBO_STANDARD = 1;
    public static final int TYPE_COMBO_AUTOCOMPLETE = 2;
    public static final int TYPE_COMBO_AUTOCOMPLETE_WITH_ACTION = 3;

    public static final int COMBO_ACTION_SEARCH = 1;
    public static final int COMBO_ACTION_ADD = 2;
    public static final int COMBO_ACTION_MODIFY = 3;

    private int comboType = TYPE_COMBO_STANDARD;
    private boolean fillFromConvertor = false;

    private List<LightUIComboBoxElement> values = new ArrayList<LightUIComboBoxElement>();

    private LightUIComboBoxElement selectedValue = null;

    private List<Integer> comboActions = new ArrayList<Integer>();

    // Init from json constructor
    public LightUIComboBox(final JSONObject json) {
        super(json);
    }

    // Clone constructor
    public LightUIComboBox(final LightUIComboBox combo) {
        super(combo);
    }

    public LightUIComboBox(final String id) {
        super(id);
        this.setType(TYPE_COMBOBOX);
    }

    public int getComboType() {
        return this.comboType;
    }

    public void setComboType(final int comboType) {
        this.comboType = comboType;
    }

    public void addValue(final LightUIComboBoxElement values) {
        this.values.add(values);
    }

    public void addValues(final List<LightUIComboBoxElement> values) {
        this.values.addAll(values);
    }

    public List<Integer> getComboActions() {
        return this.comboActions;
    }

    public void addComboAction(final Integer comboAction) {
        this.comboActions.add(comboAction);
    }

    public static LightUIComboBoxElement getDefaultValue() {
        final String defaultLabelKey = "not.specified.label";
        final String defaultLabel = TranslationManager.getInstance().getTranslationForItem(defaultLabelKey);

        final LightUIComboBoxElement defaultElement = new LightUIComboBoxElement(0);
        if (defaultLabel != null) {
            defaultElement.setValue1(defaultLabel);
        } else {
            defaultElement.setValue1(defaultLabelKey);
        }

        return defaultElement;
    }

    public List<LightUIComboBoxElement> getValues() {
        return this.values;
    }

    public boolean hasSelectedValue() {
        return this.selectedValue != null && this.selectedValue.getId() != 0;
    }

    public LightUIComboBoxElement getSelectedValue() {
        return this.selectedValue;
    }

    public void setSelectedValue(final LightUIComboBoxElement selectedValue) {
        this.selectedValue = selectedValue;
    }

    public void clearValues() {
        this.selectedValue = null;
        this.values.clear();
    }

    public void setFillFromConvertor(final boolean fillFromConvertor) {
        this.fillFromConvertor = fillFromConvertor;
    }

    public boolean isFillFromConvertor() {
        return this.fillFromConvertor;
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);

        if (!(element instanceof LightUIComboBox)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUIComboBox combo = (LightUIComboBox) element;
        this.comboType = combo.comboType;
        this.fillFromConvertor = combo.fillFromConvertor;
        this.values = combo.values;
        this.selectedValue = combo.selectedValue;
        this.comboActions = combo.comboActions;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIComboBox(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIComboBox(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();

        if (this.values != null && this.values.size() > 0) {
            final JSONArray jsonValues = new JSONArray();
            for (final LightUIComboBoxElement value : this.values) {
                jsonValues.add(value.toJSON());
            }
            json.put("values", jsonValues);
        }

        if (this.comboType != TYPE_COMBO_STANDARD) {
            json.put("combo-type", this.comboType);
        }
        if (this.fillFromConvertor) {
            json.put("fill-from-convertor", true);
        }
        if (this.selectedValue != null) {
            json.put("selected-value", this.selectedValue.toJSON());
        }

        return json;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        this.comboType = (Integer) JSONConverter.getParameterFromJSON(json, "combo-type", Integer.class, TYPE_COMBO_STANDARD);
        this.fillFromConvertor = (Boolean) JSONConverter.getParameterFromJSON(json, "fill-from-convertor", Boolean.class, false);
        final JSONObject jsonSelectedValue = (JSONObject) JSONConverter.getParameterFromJSON(json, "", JSONObject.class);
        if (jsonSelectedValue != null) {
            this.selectedValue = new LightUIComboBoxElement(jsonSelectedValue);
        }

        final JSONArray jsonValues = (JSONArray) JSONConverter.getParameterFromJSON(json, "values", JSONArray.class);
        this.values = new ArrayList<LightUIComboBoxElement>();
        if (jsonValues != null) {
            for (final Object jsonValue : jsonValues) {
                if (!(jsonValue instanceof JSONObject)) {
                    throw new IllegalArgumentException("value for 'values' is invalid");
                }
                this.values.add(new LightUIComboBoxElement((JSONObject) jsonValue));
            }
        }
    }

    @Override
    public Object getValueFromContext() {
        if (this.hasSelectedValue()) {
            return this.getSelectedValue();
        } else {
            return null;
        }
    }

    @Override
    public void setValueFromContext(Object value) {
        if (value != null) {
            final JSONObject jsonSelectedValue = (JSONObject) JSONConverter.getObjectFromJSON(value, JSONObject.class);
            this.selectedValue = new LightUIComboBoxElement(jsonSelectedValue);
        } else {
            this.selectedValue = null;
        }
    }
}
