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

public class LightUIPanel extends LightUIElement implements Transferable {

    public static final int STYLE_DIV = 0;
    public static final int STYLE_TABLE = 1;

    private static final long serialVersionUID = -3399395824294128572L;

    private String title;
    private int panelStyle = STYLE_DIV;
    private List<LightUILine> lines = new ArrayList<LightUILine>();
    private final List<LightControler> controlers = new ArrayList<LightControler>();

    public LightUIPanel() {
        // Serialization
    }

    public LightUIPanel(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUIPanel(final LightUIPanel panelElement) {
        super(panelElement);
        this.title = panelElement.title;
        this.panelStyle = panelElement.panelStyle;
        this.lines = panelElement.lines;
        this.controlers.addAll(panelElement.controlers);
    }

    public LightUIPanel(String id) {
        this.setId(id);
        this.setType(TYPE_PANEL);
    }

    public void addLine(LightUILine line) {
        this.lines.add(line);
    }

    public void insertLine(final LightUILine line, final int index) {
        final int linesSize = this.lines.size();
        if (index < 0 || index > linesSize - 1) {
            throw new IllegalArgumentException("index is out of bounds, it must be in [0 - " + String.valueOf(linesSize) + "] but this was found (" + String.valueOf(index) + ")");
        }
        LightUILine swap = this.lines.get(index);
        this.lines.set(index, line);
        for (int i = index + 1; i < linesSize; i++) {
            final LightUILine tmpLine = this.lines.get(i);
            this.lines.set(i, swap);
            swap = tmpLine;
        }
        this.lines.add(swap);
    }

    public LightUILine getLastLine() {
        if (this.lines.size() == 0) {
            final LightUILine l = new LightUILine();
            this.lines.add(l);
            return l;
        }
        return this.lines.get(this.lines.size() - 1);
    }

    public void dump(PrintStream out) {
        final int size = this.lines.size();
        out.println("------LightUIPanel-----");
        out.println("ID : " + this.getId());
        out.println("Title : " + this.title);
        out.println(getId() + " : " + this.title);
        out.println("Line count : " + size + " lines ");
        for (int i = 0; i < size; i++) {
            LightUILine line = this.lines.get(i);
            out.println("LightUIPanel line " + i);
            line.dump(out);
            out.println();
        }
        out.println("------------------------");
    }

    public int getPanelType() {
        return this.panelStyle;
    }

    public void setPanelType(final int panelStyle) {
        this.panelStyle = panelStyle;
    }

    public LightUILine getLine(int i) {
        return this.lines.get(i);
    }

    public int getSize() {
        return this.lines.size();
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addControler(LightControler controler) {
        this.controlers.add(controler);
    }

    public List<LightControler> getControlers() {
        return this.controlers;
    }

    public void dumpControllers(PrintStream out) {
        dumpControllers(out, 0);
    }

    public void dumpControllers(PrintStream out, int depth) {
        addSpacer(out, depth);
        out.println("Contollers for id:" + this.getId() + " title: " + this.title);
        for (LightControler controler : this.controlers) {
            addSpacer(out, depth);
            out.println(controler);
        }
        final int size = this.lines.size();
        addSpacer(out, depth);
        out.println(getId() + " : " + this.title);
        addSpacer(out, depth);
        out.println("LightUIPanel " + size + " lines ");
        for (int i = 0; i < size; i++) {
            final LightUILine line = this.lines.get(i);
            for (int j = 0; j < line.getSize(); j++) {
                final LightUIElement e = line.getElement(j);
                if (e instanceof LightUIPanel) {
                    ((LightUIPanel) e).dumpControllers(out, depth + 1);

                }
            }
        }
    }

    private void addSpacer(PrintStream out, int depth) {
        for (int i = 0; i < depth; i++) {
            out.print("  ");
        }

    }

    public boolean replaceElement(final LightUIElement pelement) {
        if (this.lines != null) {
            final int lineSize = this.lines.size();
            for (int i = 0; i < lineSize; i++) {
                if (this.lines.get(i).replaceElement(pelement)) {
                    return true;
                }
            }
        }
        return false;
    }

    public LightUIElement getElementById(final String id) {
        if (this.lines != null) {
            final int lineSize = this.lines.size();
            for (int i = 0; i < lineSize; i++) {
                final LightUIElement element = this.lines.get(i).getElementById(id);
                if (element != null) {
                    return element;
                }
            }
        }
        return null;
    }

    @Override
    public LightUIElement clone() {
        return new LightUIPanel(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject result = super.toJSON();
        if (this.title != null) {
            result.put("title", this.title);
        }
        if(!this.lines.isEmpty()) {
            result.put("lines", JSONConverter.getJSON(this.lines));
        }
        if (!this.controlers.isEmpty()) {
            result.put("controlers", JSONConverter.getJSON(this.controlers));
        }
        if(this.panelStyle != STYLE_DIV) {
            result.put("panel-style", this.panelStyle);
        }
        return result;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);
        this.title = (String) JSONConverter.getParameterFromJSON(json, "title", String.class, null);
        this.panelStyle = (Integer) JSONConverter.getParameterFromJSON(json, "panel-style", Integer.class, STYLE_DIV);

        final JSONArray jsonLines = (JSONArray) JSONConverter.getParameterFromJSON(json, "lines", JSONArray.class, null);
        this.lines.clear();
        if (jsonLines != null) {
            final int linesSize = jsonLines.size();
            for (int i = 0; i < linesSize; i++) {
                final JSONObject jsonLine = (JSONObject) JSONConverter.getObjectFromJSON(jsonLines.get(i), JSONObject.class);
                this.lines.add(new LightUILine(jsonLine));
            }
        }

        final JSONArray jsonControlers = (JSONArray) JSONConverter.getParameterFromJSON(json, "controlers", JSONArray.class);
        this.controlers.clear();
        if (jsonControlers != null) {
            final int controlersSize = jsonControlers.size();
            for (int i = 0; i < controlersSize; i++) {
                final JSONObject jsonControler = (JSONObject) JSONConverter.getObjectFromJSON(jsonControlers.get(i), JSONObject.class);
                this.controlers.add(new LightControler((JSONObject) jsonControler));
            }
        }
    }
}
