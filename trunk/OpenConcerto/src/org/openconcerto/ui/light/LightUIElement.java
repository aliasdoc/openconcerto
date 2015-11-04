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
import java.util.ArrayList;
import java.util.List;

import org.openconcerto.utils.io.JSONconverter;
import org.openconcerto.utils.io.Transferable;

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
    public static final int TYPE_LIST = 4;
    public static final int TYPE_CHECKBOX = 5;
    public static final int TYPE_TABBED_UI = 6;
    public static final int TYPE_COMBOBOX_ELEMENT = 7;
    public static final int TYPE_DESCRIPTOR = 8;
    public static final int TYPE_TREE = 9;
    public static final int TYPE_TEXT = 10;
    public static final int TYPE_SCROLLABLE = 11;
    public static final int TYPE_BUTTON = 20;
    public static final int TYPE_BUTTON_WITH_CONTEXT = 21;
    public static final int TYPE_BUTTON_CANCEL = 22;
    public static final int TYPE_BUTTON_UNMANAGED = 23;
    public static final int TYPE_BUTTON_WITH_SELECTION_CONTEXT = 24;
    // valueType
    public static final int VALUE_TYPE_STRING = 0;
    public static final int VALUE_TYPE_INTEGER = 1;
    public static final int VALUE_TYPE_DATE = 2;
    public static final int VALUE_TYPE_REF = 3;
    public static final int VALUE_TYPE_LIST = 4;
    public static final int VALUE_TYPE_DECIMAL = 5;
    // commitMode
    public static final int COMMIT_ONCE = 0;
    public static final int COMMIT_INTERACTIVE = 1;

    // Type
    private int type;
    // Layout
    private int gridWidth;
    private boolean fillWidth;
    // Values
    private String id;
    private String label;
    private String value;
    private TableSpec rawContent;
    private int valueType;
    private int commitMode;
    private String valuePrecision;// "(6,2)" 999999.99 is the max
    private String displayPrecision;// "(1,2)" means that 0.159 is shown as 0.16
    private String valueRange; // [-3.14,3.14]

    private int minInputSize;
    private List<LightUIDescriptor> tabs;
    // Colors
    private Color color;
    // Icon
    private String icon;
    private boolean required;

    private String toolTip;
    private boolean verticalyResizable;
    private boolean horizontalyResizable;

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

    public boolean isFillWidth() {
        return this.fillWidth;
    }

    public void setFillWidth(boolean fillWidth) {
        this.fillWidth = fillWidth;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
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

    public boolean isRequired() {
        return this.required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<LightUIDescriptor> getTabs() {
        return this.tabs;
    }

    public String getToolTip() {
        return this.toolTip;
    }

    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }

    public void addTab(LightUIDescriptor desc) {
        if (this.tabs == null) {
            this.tabs = new ArrayList<LightUIDescriptor>(5);
        }
        this.tabs.add(desc);
    }

    public final boolean isVerticalyResizable() {
        return this.verticalyResizable;
    }

    public final void setVerticalyResizable(boolean verticalyResizable) {
        this.verticalyResizable = verticalyResizable;
    }

    public final boolean isHorizontalyResizable() {
        return this.horizontalyResizable;
    }

    public final void setHorizontalyResizable(boolean horizontalyResizable) {
        this.horizontalyResizable = horizontalyResizable;
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
        } else if (this.type == TYPE_LIST) {
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
        if (this.horizontalyResizable) {
            str += "|- H ->";
        }
        if (this.verticalyResizable) {
            str += "|- V ->";
        }
        out.println(str);

    }

    @Override
    public String toString() {
        return super.toString() + " " + this.id;
    }

    public TableSpec getRawContent() {
        return this.rawContent;
    }

    public void setRawContent(TableSpec rawContent) {
        this.rawContent = rawContent;
    }

    @Override
    public String toJSON() {
        final StringBuilder result = new StringBuilder("{");

        result.append("\"id\":" + JSONconverter.getJSON(this.id) + ",");
        if (this.color == null) {
            result.append("\"color\":null,");
        } else {
            result.append("\"color\":{\"r\":" + String.valueOf(this.color.getRed()) + ", \"g\":" + String.valueOf(this.color.getGreen()) + ",\"b\":" + String.valueOf(this.color.getBlue()) + "},");
        }
        result.append("\"commitMode\":" + JSONconverter.getJSON(this.commitMode) + ",");
        result.append("\"displayPrecision\":" + JSONconverter.getJSON(this.displayPrecision) + ",");
        result.append("\"fillWidth\":" + JSONconverter.getJSON(this.fillWidth) + ",");
        result.append("\"gridWidth\":" + JSONconverter.getJSON(this.gridWidth) + ",");
        result.append("\"horizontalyResizable\":" + JSONconverter.getJSON(this.horizontalyResizable) + ",");
        result.append("\"verticalyResizable\":" + JSONconverter.getJSON(this.verticalyResizable) + ",");
        result.append("\"icon\":" + JSONconverter.getJSON(this.icon) + ",");
        result.append("\"label\":" + JSONconverter.getJSON(this.label) + ",");
        result.append("\"minInputSize\":" + JSONconverter.getJSON(this.minInputSize) + ",");
        result.append("\"rawContent\":" + JSONconverter.getJSON(this.rawContent) + ",");
        result.append("\"required\":" + JSONconverter.getJSON(this.required) + ",");
        result.append("\"tabs\":" + JSONconverter.getJSON(this.tabs) + ",");
        result.append("\"toolTip\":" + JSONconverter.getJSON(this.toolTip) + ",");
        result.append("\"type\":" + JSONconverter.getJSON(this.type) + ",");
        result.append("\"value\":" + JSONconverter.getJSON(this.value) + ",");
        result.append("\"valuePrecision\":" + JSONconverter.getJSON(this.valuePrecision) + ",");
        result.append("\"valueRange\":" + JSONconverter.getJSON(this.valueRange) + ",");
        result.append("\"valueType\":" + JSONconverter.getJSON(this.valueType));

        result.append("}");

        return result.toString();
    }

}
