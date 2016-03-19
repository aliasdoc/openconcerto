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

public class SimpleTextLine extends LightUILine {

    public SimpleTextLine(final String cellId, final String text) {
        this(cellId, text, false, LightUIElement.HALIGN_LEFT);
    }

    public SimpleTextLine(final String cellId, final String text, final boolean title, final int horizontalAlignement) {
        final LightUILabel element = new LightUILabel(cellId, text, title);
        element.setHorizontalAlignement(horizontalAlignement);
        element.setFillWidth(true);
        this.add(element);
        this.setGridAlignment(LightUILine.ALIGN_LEFT);
    }

    public SimpleTextLine(final String cellId, final String text, final boolean title, final int horizontalAlignement, final int gridWidth) {
        final LightUILabel element = new LightUILabel(cellId, text, title);
        element.setHorizontalAlignement(horizontalAlignement);
        element.setFillWidth(true);
        element.setGridWidth(gridWidth);
        this.add(element);
        this.setGridAlignment(LightUILine.ALIGN_LEFT);
    }

}
