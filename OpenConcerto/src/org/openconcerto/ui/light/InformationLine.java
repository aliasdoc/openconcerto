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

public class InformationLine extends LightUILine {
    public InformationLine(final String id, final String key, final String value) {
        this.setGridAlignment(ALIGN_GRID);
        final LightUILabel keyElement = new LightUILabel(id + ".key." + key.replace(" ", ".").toLowerCase(), key, true);
        keyElement.setHorizontalAlignement(LightUIElement.HALIGN_RIGHT);
        add(keyElement);

        final LightUILabel valueElement = new LightUILabel(id + ".value." + key.replace(" ", ".").toLowerCase(), value);
        valueElement.setHorizontalAlignement(LightUIElement.HALIGN_LEFT);
        add(valueElement);
    }
}
