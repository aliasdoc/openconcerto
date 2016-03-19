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

import java.awt.Color;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONObject;

public class LightUIElement implements Transferable {
    /**
     * 
     */
    private static final long serialVersionUID = 3272357171610073289L;
    // type
    public static final int TYPE_LABEL = 0;
    public static final int TYPE_TEXT_FIELD = 1;
    public static final int TYPE_DATE = 2;
    public static final int TYPE_COMBOBOX = 3;
    public static final int TYPE_TABLE = 4;
    public static final int TYPE_CHECKBOX = 5;
    public static final int TYPE_TABBED_UI = 6;
    public static final int TYPE_COMBOBOX_ELEMENT = 7;
    public static final int TYPE_PANEL = 8;
    public static final int TYPE_TREE = 9;
    public static final int TYPE_TEXT = 10;
    public static final int TYPE_SCROLLABLE = 11;
    public static final int TYPE_LIST = 12;
    public static final int TYPE_DROPDOWN_BUTTON = 13;
    public static final int TYPE_FRAME = 14;
    public static final int TYPE_BUTTON = 20;
    public static final int TYPE_BUTTON_WITH_CONTEXT = 21;
    public static final int TYPE_BUTTON_CANCEL = 22;
    public static final int TYPE_BUTTON_UNMANAGED = 23;
    public static final int TYPE_BUTTON_WITH_SELECTION_CONTEXT = 24;
    public static final int TYPE_BUTTON_LINK = 25;

    // valueType
    public static final int VALUE_TYPE_STRING = 0;
    public static final int VALUE_TYPE_INTEGER = 1;
    public static final int VALUE_TYPE_DATE = 2;
    public static final int VALUE_TYPE_REF = 3;
    public static final int VALUE_TYPE_LIST = 4;
    public static final int VALUE_TYPE_DECIMAL = 5;
    public static final int VALUE_TYPE_BOOLEAN = 6;
    // actionType
    public static final int ACTION_TYPE_SELECTION = 0;
    public static final int ACTION_TYPE_REMOVE = 1;
    public static final int ACTION_TYPE_REFRESH = 2;

    // commitMode
    public static final int COMMIT_ONCE = 0;
    public static final int COMMIT_INTERACTIVE = 1;
    // horizontalAlignement
    public static final int HALIGN_RIGHT = 0;
    public static final int HALIGN_CENTER = 1;
    public static final int HALIGN_LEFT = 2; // Default
    // verticalAlignement
    public static final int VALIGN_TOP = 0; // Default
    public static final int VALIGN_CENTER = 1;
    public static final int VALIGN_BOTTOM = 2;
    // font size
    public static final int FONT_XXSMALL = 0;
    public static final int FONT_XSMALL = 1;
    public static final int FONT_SMALL = 2; // Default
    public static final int FONT_MEDIUM = 3;
    public static final int FONT_LARGE = 4;
    public static final int FONT_XLARGE = 5;
    public static final int FONT_XXLARGE = 6;

    private Integer commitMode;
    private Integer fontSize = FONT_SMALL;
    private Integer gridWidth = 1;
    private Integer horizontalAlignement = HALIGN_LEFT;
    private Integer height = null;
    private Integer minInputSize;
    private Integer type;
    private Integer valueType;
    private Integer verticalAlignement = VALIGN_TOP;
    private Integer width = null;

    private boolean foldable = false;
    private boolean folded = false;
    private boolean fillWidth;
    private boolean horizontallyResizable;
    private boolean required;
    private boolean verticallyResizable;

    private String displayPrecision;// "(1,2)" means that 0.159 is shown as 0.16
    private String icon;
    private String id;
    private String label;
    private String toolTip;
    // Values
    private String value;
    private String valuePrecision;// "(6,2)" 999999.99 is the max
    private String valueRange; // [-3.14,3.14]

    private Color backgroundColor;
    private Color foreColor;

    private Map<Integer, String> actions = new HashMap<Integer, String>();

    public LightUIElement() {
    }

    // Init from json constructor
    public LightUIElement(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUIElement(final LightUIElement element) {
        this.commitMode = element.commitMode;
        this.gridWidth = element.gridWidth;
        this.horizontalAlignement = element.horizontalAlignement;
        this.height = element.height;
        this.minInputSize = element.minInputSize;
        this.type = element.type;
        this.valueType = element.valueType;
        this.verticalAlignement = element.verticalAlignement;
        this.width = element.width;
        this.fontSize = element.fontSize;
        this.foldable = element.foldable;
        this.folded = element.folded;
        this.fillWidth = element.fillWidth;
        this.horizontallyResizable = element.horizontallyResizable;
        this.required = element.required;
        this.verticallyResizable = element.verticallyResizable;
        this.displayPrecision = element.displayPrecision;
        this.icon = element.icon;
        this.id = element.id;
        this.label = element.label;
        this.toolTip = element.toolTip;
        this.value = element.value;
        this.valuePrecision = element.valuePrecision;
        this.valueRange = element.valueRange;
        this.backgroundColor = element.backgroundColor;
        this.foreColor = element.foreColor;
        this.actions = element.actions;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getGridWidth() {
        return this.gridWidth;
    }

    public void setGridWidth(int gridWidth) {
        this.gridWidth = gridWidth;
    }

    public boolean isFoldable() {
        return this.foldable;
    }

    public void setFoldable(boolean foldable) {
        this.foldable = foldable;
    }
    
    public boolean isFolded() {
        return this.folded;
    }

    public void setFolded(boolean folded) {
        this.folded = folded;
    }
    
    public boolean isFillWidth() {
        return this.fillWidth;
    }

    public void setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
    }

    public Color getBackgroundColor() {
        return this.backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Color getForeColor() {
        return this.foreColor;
    }

    public int getFontSize() {
        return this.fontSize;
    }

    public void setFontSize(final int fontSize) {
        this.fontSize = fontSize;
    }

    public void setForeColor(Color foreColor) {
        this.foreColor = foreColor;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLabel() {
        return this.label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getValue() {
        return this.value;
    }

    public int getCommitMode() {
        return this.commitMode;
    }

    public void setCommitMode(int commitMode) {
        this.commitMode = commitMode;
    }

    public String getDisplayPrecision() {
        return this.displayPrecision;
    }

    public void setDisplayPrecision(String displayPrecision) {
        this.displayPrecision = displayPrecision;
    }

    public String getValuePrecision() {
        return this.valuePrecision;
    }

    public void setValuePrecision(String valuePrecision) {
        this.valuePrecision = valuePrecision;
    }

    public String getValueRange() {
        return this.valueRange;
    }

    public void setValueRange(String valueRange) {
        this.valueRange = valueRange;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getValueType() {
        return this.valueType;
    }

    public void setValueType(int valueType) {
        this.valueType = valueType;
    }

    public int getMinInputSize() {
        return this.minInputSize;
    }

    public void setMinInputSize(int minInputSize) {
        this.minInputSize = minInputSize;
    }

    public Integer getHeight() {
        return this.height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return this.width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getToolTip() {
        return this.toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public final boolean isVerticallyResizable() {
        return this.verticallyResizable;
    }

    public final void setVerticalyResizable(boolean verticallyResizable) {
        this.verticallyResizable = verticallyResizable;
    }

    public final boolean isHorizontalyResizable() {
        return this.horizontallyResizable;
    }

    public final void setHorizontalyResizable(boolean horizontallyResizable) {
        this.horizontallyResizable = horizontallyResizable;
    }

    public final Integer getHorizontalAlignement() {
        return this.horizontalAlignement;
    }

    public final void setHorizontalAlignement(Integer horizontalAlignement) {
        this.horizontalAlignement = horizontalAlignement;
    }

    public final Integer getVerticalAlignement() {
        return this.verticalAlignement;
    }

    public final void setVerticalAlignement(Integer verticalAlignement) {
        this.verticalAlignement = verticalAlignement;
    }

    public final void addAction(final Integer actionType, final String actionId) {
        this.actions.put(actionType, actionId);
    }

    public void dump(PrintStream out) {
        String type = "?";
        if (this.type == TYPE_CHECKBOX) {
            type = "checkbox";
        } else if (this.type == TYPE_COMBOBOX) {
            type = "combobox";
        } else if (this.type == TYPE_LABEL) {
            type = "label";
        } else if (this.type == TYPE_TEXT_FIELD) {
            type = "textfield";
        } else if (this.type == TYPE_TABLE) {
            type = "list";
        } else if (this.type == TYPE_TABBED_UI) {
            type = "tabs";
        } else if (this.type == TYPE_TREE) {
            type = "tree";
        } else if (this.type == TYPE_BUTTON) {
            type = "button";
        } else if (this.type == TYPE_BUTTON_WITH_CONTEXT) {
            type = "button with context";
        } else if (this.type == TYPE_BUTTON_CANCEL) {
            type = "cancel button";
        } else if (this.type == TYPE_COMBOBOX_ELEMENT) {
            type = "combo element";
        } else if (this.type == TYPE_BUTTON_WITH_SELECTION_CONTEXT) {
            type = "button with selection context";
        }
        String valueType = "?";
        if (this.valueType == VALUE_TYPE_STRING) {
            valueType = "string";
        } else if (this.valueType == VALUE_TYPE_INTEGER) {
            valueType = "int";
        } else if (this.valueType == VALUE_TYPE_REF) {
            valueType = "ref";
        } else if (this.valueType == VALUE_TYPE_LIST) {
            valueType = "list";
        } else if (this.valueType == VALUE_TYPE_DECIMAL) {
            valueType = "decimal";
        }

        String str = "LightUIElement" + " " + type + " id:" + this.id + " w:" + this.gridWidth + " fill:" + this.fillWidth;
        str += " value:" + this.value + "(" + valueType + ")";
        if (this.valueRange != null) {
            str += "range: " + this.valueRange;
        }
        if (this.valuePrecision != null) {
            str += "precision: " + this.valuePrecision;
        }
        if (this.displayPrecision != null) {
            str += "display prec.: " + this.displayPrecision;
        }
        if (this.label != null) {
            str += " label:" + this.label;
        }
        if (this.horizontallyResizable) {
            str += "|- H ->";
        }
        if (this.verticallyResizable) {
            str += "|- V ->";
        }

        switch (this.fontSize) {
        case FONT_XXSMALL:
            str += " font: xx-small";
            break;
        case FONT_XSMALL:
            str += " font: x-small";
            break;
        case FONT_SMALL:
            str += " font: small";
            break;
        case FONT_MEDIUM:
            str += " font: medium";
            break;
        case FONT_LARGE:
            str += " font: large";
            break;
        case FONT_XLARGE:
            str += " font: x-large";
            break;
        case FONT_XXLARGE:
            str += " font: xx-large";
            break;
        }

        switch (this.horizontalAlignement) {
        case HALIGN_RIGHT:
            str += " horiz-align: right";
            break;
        case HALIGN_CENTER:
            str += " horiz-align: center";
            break;
        case HALIGN_LEFT:
            str += " horiz-align: left";
            break;
        }

        switch (this.verticalAlignement) {
        case HALIGN_RIGHT:
            str += " vert-align: top";
            break;
        case HALIGN_CENTER:
            str += " vert-align: center";
            break;
        case HALIGN_LEFT:
            str += " vert-align: bottom";
            break;
        }
        out.println(str);

    }

    @Override
    public LightUIElement clone() {
        return new LightUIElement(this);
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.id;
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();

        result.put("class", "LightUIElement");
        result.put("id", this.id);

        if (this.backgroundColor != null) {
            result.put("background-color", JSONConverter.getJSON(this.backgroundColor));
        }
        if (this.commitMode != null) {
            result.put("commit-mode", this.commitMode);
        }
        if (this.displayPrecision != null) {
            result.put("display-precision", this.displayPrecision);
        }
        if (this.foldable) {
            result.put("foldable", true);
        }
        if (this.folded) {
            result.put("folded", true);
        }
        if (this.fillWidth) {
            result.put("fill-width", true);
        }
        if (this.foreColor != null) {
            result.put("fore-color", JSONConverter.getJSON(this.foreColor));
        }
        if (this.fontSize != FONT_SMALL) {
            result.put("font-size", this.fontSize);
        }
        if (this.gridWidth != null) {
            result.put("grid-width", this.gridWidth);
        }
        if (this.horizontallyResizable) {
            result.put("horizontally-resizable", true);
        }
        if (this.verticallyResizable) {
            result.put("vertically-resizable", true);
        }
        if (this.icon != null) {
            result.put("icon", this.icon);
        }
        if (this.label != null) {
            result.put("label", this.label);
        }
        if (this.minInputSize != null) {
            result.put("min-input-size", this.minInputSize);
        }
        if (this.required) {
            result.put("required", true);
        }
        if (this.toolTip != null) {
            result.put("tool-tip", this.toolTip);
        }
        
        result.put("type", this.type);
        result.put("value", this.value);
        
        if (this.valuePrecision != null) {
            result.put("value-precision", this.valuePrecision);
        }
        if (this.valueRange != null) {
            result.put("value-range", this.valueRange);
        }
        if (this.valueType != null) {
            result.put("value-type", this.valueType);
        }
        if (this.width != null) {
            result.put("width", this.width);
        }
        if (this.height != null) {
            result.put("height", this.height);
        }
        if (this.horizontalAlignement != HALIGN_LEFT) {
            result.put("horizontal-alignement", this.horizontalAlignement);
        }
        if (this.verticalAlignement != VALIGN_TOP) {
            result.put("vertical-alignement", this.verticalAlignement);
        }
        if (!this.actions.isEmpty()) {
            result.put("actions", this.actions);
        }
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        this.id = (String) JSONConverter.getParameterFromJSON(json, "id", String.class);
        this.commitMode = (Integer) JSONConverter.getParameterFromJSON(json, "commit-mode", Integer.class);
        this.displayPrecision = (String) JSONConverter.getParameterFromJSON(json, "display-precision", String.class);
        this.foldable = (Boolean) JSONConverter.getParameterFromJSON(json, "foldable", Boolean.class, false);
        this.folded = (Boolean) JSONConverter.getParameterFromJSON(json, "folded", Boolean.class, false);
        this.fillWidth = (Boolean) JSONConverter.getParameterFromJSON(json, "fill-width", Boolean.class, Boolean.FALSE);
        this.fontSize = (Integer) JSONConverter.getParameterFromJSON(json, "font-size", Integer.class, FONT_SMALL);
        this.gridWidth = (Integer) JSONConverter.getParameterFromJSON(json, "grid-width", Integer.class);
        this.horizontallyResizable = (Boolean) JSONConverter.getParameterFromJSON(json, "horizontally-resizable", Boolean.class, Boolean.FALSE);
        this.verticallyResizable = (Boolean) JSONConverter.getParameterFromJSON(json, "vertically-resizable", Boolean.class, Boolean.FALSE);
        this.icon = (String) JSONConverter.getParameterFromJSON(json, "icon", String.class);
        this.label = (String) JSONConverter.getParameterFromJSON(json, "label", String.class);
        this.minInputSize = (Integer) JSONConverter.getParameterFromJSON(json, "min-input-size", Integer.class);
        this.required = (Boolean) JSONConverter.getParameterFromJSON(json, "required", Boolean.class, Boolean.FALSE);
        this.toolTip = (String) JSONConverter.getParameterFromJSON(json, "tool-tip", String.class);
        this.type = (Integer) JSONConverter.getParameterFromJSON(json, "type", Integer.class);
        this.value = (String) JSONConverter.getParameterFromJSON(json, "value", String.class);
        this.valuePrecision = (String) JSONConverter.getParameterFromJSON(json, "value-precision", String.class);
        this.valueRange = (String) JSONConverter.getParameterFromJSON(json, "value-range", String.class);
        this.valueType = (Integer) JSONConverter.getParameterFromJSON(json, "value-type", Integer.class);
        this.width = (Integer) JSONConverter.getParameterFromJSON(json, "width", Integer.class);
        this.height = (Integer) JSONConverter.getParameterFromJSON(json, "height", Integer.class);
        this.horizontalAlignement = (Integer) JSONConverter.getParameterFromJSON(json, "horizontal-alignement", Integer.class, HALIGN_LEFT);
        this.verticalAlignement = (Integer) JSONConverter.getParameterFromJSON(json, "vertical-alignement", Integer.class, VALIGN_TOP);

        final JSONObject jsonBackgroundColor = (JSONObject) JSONConverter.getParameterFromJSON(json, "background-color", JSONObject.class);
        if (jsonBackgroundColor != null) {
            if (!jsonBackgroundColor.containsKey("r") || !jsonBackgroundColor.containsKey("g") || !jsonBackgroundColor.containsKey("b")) {
                throw new IllegalArgumentException("value for 'background-color' is invalid, it must contains attribute r, g, b");
            }
            final int r = (Integer) JSONConverter.getParameterFromJSON(jsonBackgroundColor, "r", Integer.class);
            final int g = (Integer) JSONConverter.getParameterFromJSON(jsonBackgroundColor, "g", Integer.class);
            final int b = (Integer) JSONConverter.getParameterFromJSON(jsonBackgroundColor, "b", Integer.class);
            this.backgroundColor = new Color(r, g, b);
        }

        final JSONObject jsonForeColor = (JSONObject) JSONConverter.getParameterFromJSON(json, "fore-color", JSONObject.class);
        if (jsonForeColor != null) {
            if (!jsonForeColor.containsKey("r") || !jsonForeColor.containsKey("g") || !jsonForeColor.containsKey("b")) {
                throw new IllegalArgumentException("value for 'for-color' is invalid, it must contains attribute r, g, b");
            }
            final int r = (Integer) JSONConverter.getParameterFromJSON(jsonForeColor, "r", Integer.class);
            final int g = (Integer) JSONConverter.getParameterFromJSON(jsonForeColor, "g", Integer.class);
            final int b = (Integer) JSONConverter.getParameterFromJSON(jsonForeColor, "b", Integer.class);
            this.foreColor = new Color(r, g, b);
        }
    }
    
    public static LightUIElement createUIElementFromJSON(final JSONObject jsonElement) {
        final Integer elementType = (Integer) JSONConverter.getParameterFromJSON(jsonElement, "type", Integer.class, null);
        if(elementType == null) {
            throw new IllegalArgumentException("LightUIElement must contains attribute 'type'");
        }

        LightUIElement lightElement = null;
        if (elementType == LightUIElement.TYPE_PANEL || elementType == LightUIElement.TYPE_SCROLLABLE) {
            lightElement = new LightUIPanel(jsonElement);
        } else if (elementType == LightUIElement.TYPE_LIST) {
            lightElement = new LightUIList(jsonElement);
        } else if (elementType == LightUIElement.TYPE_TABLE) {
            lightElement = new LightUITable(jsonElement);
        } else if (elementType == LightUIElement.TYPE_LABEL) {
            lightElement = new LightUILabel(jsonElement);
        } else if (elementType == LightUIElement.TYPE_DROPDOWN_BUTTON) {
            lightElement = new LightUIDropDownButton(jsonElement);
        } else if (elementType == LightUIElement.TYPE_CHECKBOX) {
            lightElement = new LightUICheckBox(jsonElement);
        } else if (elementType == LightUIElement.TYPE_COMBOBOX) {
            lightElement = new LightUICombo(jsonElement);
        } else if (elementType == LightUIElement.TYPE_TABBED_UI) {
            lightElement = new LightUITabbed(jsonElement);
        } else {
            lightElement = new LightUIElement(jsonElement);
        }
        return lightElement;
    }
}
