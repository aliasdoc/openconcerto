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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.openconcerto.utils.io.JSONConverter;
import org.openconcerto.utils.io.Transferable;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;

public class LightUILine implements Transferable {

    public static final int ALIGN_GRID = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;

    private static final long serialVersionUID = 4132718509484530435L;

    private boolean elementMargin = false;
    private boolean fillHeight = false;
    private boolean footer = false;

    private int gridAlignment = ALIGN_GRID;
    private int marginBottom = 0;
    private int marginLeft = 0;
    private int marginRight = 0;
    private int marginTop = 0;
    private int weightY = 0;

    private final List<LightUIElement> elements = new ArrayList<LightUIElement>();

    public LightUILine() {
    }

    public LightUILine(final JSONObject json) {
        this.fromJSON(json);
    }

    public void setElementMargin(final boolean elementMargin) {
        this.elementMargin = elementMargin;
    }

    public boolean isElementMargin() {
        return this.elementMargin;
    }

    public void setFillHeight(final boolean fillHeight) {
        this.fillHeight = fillHeight;
    }

    public boolean isFillHeight() {
        return this.fillHeight;
    }

    public void setFooter(final boolean footer) {
        this.footer = footer;
    }

    public boolean isFooter() {
        return this.footer;
    }

    public int getGridAlignment() {
        return this.gridAlignment;
    }

    public void setGridAlignment(final int gridAlignment) {
        this.gridAlignment = gridAlignment;
    }

    public int getMarginBottom() {
        return this.marginBottom;
    }

    public void setMarginBottom(final int marginBottom) {
        this.marginBottom = marginBottom;
    }

    public int getMarginLeft() {
        return this.marginLeft;
    }

    public void setMarginLeft(final int marginLeft) {
        this.marginLeft = marginLeft;
    }

    public int getMarginRight() {
        return this.marginRight;
    }

    public void setMarginRight(final int marginRight) {
        this.marginRight = marginRight;
    }

    public int getMarginTop() {
        return this.marginTop;
    }

    public void setMarginTop(final int marginTop) {
        this.marginTop = marginTop;
    }

    public int getWeightY() {
        return this.weightY;
    }

    public void setWeightY(final int weightY) {
        this.weightY = weightY;
    }

    public int getSize() {
        return this.elements.size();
    }

    public void add(final LightUIElement element) {
        this.elements.add(element);
    }

    public LightUIElement getElement(final int i) {
        return this.elements.get(i);
    }

    public int getWidth() {
        int w = 0;
        final int size = this.elements.size();
        for (int i = 0; i < size; i++) {
            w += this.elements.get(i).getGridWidth();
        }
        return w;
    }

    public boolean replaceElement(final LightUIElement pElement) {
        final int cellSize = this.elements.size();
        for (int i = 0; i < cellSize; i++) {
            final LightUIElement element = this.elements.get(i);
            if (element.getId() != null && element.getId().equals(pElement.getId())) {
                this.elements.set(i, pElement);
                return true;
            }
            if (element.getType() == LightUIElement.TYPE_PANEL) {
                if (((LightUIPanel) element).replaceElement(pElement)) {
                    return true;
                }
            }
        }
        return false;
    }

    public LightUIElement getElementById(final String id) {
        final int cellSize = this.elements.size();
        LightUIElement result = null;
        for (int i = 0; i < cellSize; i++) {
            final LightUIElement element = this.elements.get(i);
            if (element.getId() != null && element.getId().equals(id)) {
                return element;
            }
            if (element.getType() == LightUIElement.TYPE_PANEL) {
                result = ((LightUIPanel) element).getElementById(id);
                if (result != null) {
                    return result;
                }
            } else if (element.getType() == LightUIElement.TYPE_TABLE) {
                result = ((LightUITable) element).getElementById(id);
                if (result != null) {
                    return result;
                }
            } else if (element.getType() == LightUIElement.TYPE_TABBED_UI) {
                result = ((LightUITabbed) element).getElementById(id);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    public void dump(final PrintStream out) {
        int size = this.elements.size();
        out.println("LightUILine " + size + " elements, weightY: " + this.weightY + " fillHeight: " + this.fillHeight);
        for (int i = 0; i < size; i++) {
            LightUIElement element = this.elements.get(i);
            out.print("Element " + i + " : ");
            element.dump(out);
        }
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = new JSONObject();
        result.put("class", "LightUILine");
        if (this.elementMargin) {
            result.put("element-margin", this.elementMargin);
        }
        if (this.fillHeight) {
            result.put("fill-height", true);
        }
        if (this.footer) {
            result.put("footer", true);
        }
        if (this.weightY != 0) {
            result.put("weight-y", this.weightY);
        }
        if (this.marginBottom != 0) {
            result.put("margin-bottom", this.marginBottom);
        }
        if (this.marginLeft != 0) {
            result.put("margin-left", this.marginLeft);
        }
        if (this.marginRight != 0) {
            result.put("margin-right", this.marginRight);
        }
        if (this.marginTop != 0) {
            result.put("margin-top", this.marginTop);
        }
        if (this.gridAlignment != ALIGN_GRID) {
            result.put("grid-alignment", this.gridAlignment);
        }
        if (!elements.isEmpty()) {
            result.put("elements", JSONConverter.getJSON(this.elements));
        }
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {

        this.elementMargin = (Boolean) JSONConverter.getParameterFromJSON(json, "element-margin", Boolean.class, Boolean.FALSE);
        this.fillHeight = (Boolean) JSONConverter.getParameterFromJSON(json, "fill-height", Boolean.class, Boolean.FALSE);
        this.footer = (Boolean) JSONConverter.getParameterFromJSON(json, "footer", Boolean.class, Boolean.FALSE);
        this.gridAlignment = (Integer) JSONConverter.getParameterFromJSON(json, "grid-alignment", Integer.class, ALIGN_GRID);
        this.marginBottom = (Integer) JSONConverter.getParameterFromJSON(json, "margin-bottom", Integer.class, 0);
        this.marginLeft = (Integer) JSONConverter.getParameterFromJSON(json, "margin-left", Integer.class, 0);
        this.marginRight = (Integer) JSONConverter.getParameterFromJSON(json, "margin-right", Integer.class, 0);
        this.marginTop = (Integer) JSONConverter.getParameterFromJSON(json, "margin-top", Integer.class, 0);
        this.weightY = (Integer) JSONConverter.getParameterFromJSON(json, "weight-y", Integer.class, 0);

        final JSONArray jsonElements = (JSONArray) JSONConverter.getParameterFromJSON(json, "elements", JSONArray.class);
        if (jsonElements != null) {
            for (final Object o : jsonElements) {
                final JSONObject jsonElement = (JSONObject) JSONConverter.getObjectFromJSON(o, JSONObject.class);
                if (jsonElement == null) {
                    throw new IllegalArgumentException("null element in json parameter");
                }
                final LightUIElement lightElement = LightUIElement.createUIElementFromJSON(jsonElement);
                this.elements.add(lightElement);
            }
        }
    }
}
