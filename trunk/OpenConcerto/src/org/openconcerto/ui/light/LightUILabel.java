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

import net.minidev.json.JSONObject;

public class LightUILabel extends LightUIElement {
    boolean isTitle = false;

    // Init from json constructor
    public LightUILabel(final JSONObject json) {
        this.fromJSON(json);
    }

    // Clone constructor
    public LightUILabel(final LightUILabel labelElement) {
        super(labelElement);
        this.isTitle = labelElement.isTitle;
    }

    public LightUILabel(final String id) {
        this.setType(TYPE_LABEL);
        this.setId(id);
    }

    public LightUILabel(final String id, final boolean isTitle) {
        this.setType(TYPE_LABEL);
        this.setId(id);
        this.isTitle = isTitle;
    }

    public LightUILabel(final String id, final String label) {
        this.setType(TYPE_LABEL);
        this.setLabel(label);
        this.setId(id);
    }

    public LightUILabel(final String id, final String label, final boolean title) {
        this.setType(TYPE_LABEL);
        this.setLabel(label);
        this.setId(id);
        this.isTitle = title;
    }

    @Override
    public LightUIElement clone() {
        return new LightUILabel(this);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        if (this.isTitle) {
            json.put("title", true);
        }
        return json;
    }
}
