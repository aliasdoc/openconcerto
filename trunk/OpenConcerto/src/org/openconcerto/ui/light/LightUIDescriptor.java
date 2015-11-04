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

import org.openconcerto.utils.io.JSONconverter;
import org.openconcerto.utils.io.Transferable;

public class LightUIDescriptor extends LightUIElement implements Transferable {

    public static final int TYPE_DIV = 0;
    public static final int TYPE_TABLE = 1;

    private static final long serialVersionUID = -3399395824294128572L;

    private String uUID;
    private String title;
    private int descriptorType = 0;
    private List<LightUILine> lines = new ArrayList<LightUILine>();
    private List<LightControler> controlers = new ArrayList<LightControler>();

    public LightUIDescriptor(String id) {
        this.setId(id);
        this.setType(TYPE_DESCRIPTOR);
    }

    public LightUIDescriptor(String id, int counterId) {
        this.setId(id);
        this.setUUID(id + ".list" + counterId);
        this.setType(TYPE_DESCRIPTOR);
    }

    public void addLine(LightUILine line) {
        this.lines.add(line);
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
        out.println("------LightUIDescriptor-----");
        out.println("ID : " + this.getId());
        out.println("UUID : " + this.uUID);
        out.println("Title : " + this.title);
        out.println(getId() + " : " + this.title);
        out.println("Line count : " + size + " lines ");
        for (int i = 0; i < size; i++) {
            LightUILine line = this.lines.get(i);
            out.println("LightUIDescriptor line " + i);
            line.dump(out);
            out.println();
        }
        out.println("------------------------");
    }

    public int getDescriptorType() {
        return this.descriptorType;
    }

    public void setDescriptorType(final int descriptorType) {
        this.descriptorType = descriptorType;
    }

    public LightUILine getLine(int i) {
        return this.lines.get(i);
    }

    public int getSize() {
        return this.lines.size();
    }

    public String getUUID() {
        return this.uUID;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUUID(String uUID) {
        this.uUID = uUID;
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
        out.println("LightUIDescriptor " + size + " lines ");
        for (int i = 0; i < size; i++) {
            final LightUILine line = this.lines.get(i);
            for (int j = 0; j < line.getSize(); j++) {
                final LightUIElement e = line.getElement(j);
                if (e instanceof LightUIDescriptor) {
                    ((LightUIDescriptor) e).dumpControllers(out, depth + 1);

                }
            }
        }
    }

    private void addSpacer(PrintStream out, int depth) {
        for (int i = 0; i < depth; i++) {
            out.print("  ");
        }

    }

    @Override
    public String toJSON() {
        final StringBuilder result = new StringBuilder("{");

        result.append("\"id\":" + JSONconverter.getJSON(this.getId()) + ",");
        result.append("\"uuid\":" + JSONconverter.getJSON(this.getId()) + ",");
        result.append("\"type\":" + JSONconverter.getJSON(this.getType()) + ",");
        result.append("\"title\":" + JSONconverter.getJSON(this.title) + ",");
        result.append("\"lines\":" + JSONconverter.getJSON(this.lines) + ",");
        result.append("\"controlers\":" + JSONconverter.getJSON(this.controlers) + ",");
        result.append("\"descriptorType\":" + JSONconverter.getJSON(this.descriptorType) + ",");
        result.append("\"element\":" + super.toJSON());

        result.append("}");
        return result.toString();
    }
}
