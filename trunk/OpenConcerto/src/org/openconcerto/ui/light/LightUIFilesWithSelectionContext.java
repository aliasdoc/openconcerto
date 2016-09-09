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

import org.openconcerto.utils.io.JSONConverter;

import net.minidev.json.JSONObject;

public class LightUIFilesWithSelectionContext extends LightUIElement {

    String tableId;

    public LightUIFilesWithSelectionContext(final JSONObject json) {
        super(json);
    }

    public LightUIFilesWithSelectionContext(final LightUIFilesWithSelectionContext file) {
        super(file);
    }

    public LightUIFilesWithSelectionContext(final String id, final String label, final String tableId) {
        super(id);
        this.setType(LightUIElement.TYPE_FILE);

        this.setGridWidth(1);
        this.setLabel(label);

        this.tableId = tableId;
    }

    @Override
    public JSONToLightUIConvertor getConvertor() {
        return new JSONToLightUIConvertor() {
            @Override
            public LightUIElement convert(final JSONObject json) {
                return new LightUIFilesWithSelectionContext(json);
            }
        };
    }

    @Override
    public LightUIElement clone() {
        return new LightUIFilesWithSelectionContext(this);
    }

    @Override
    protected void copy(LightUIElement element) {
        super.copy(element);
        if (!(element instanceof LightUIFilesWithSelectionContext)) {
            throw new InvalidClassException(this.getClassName(), element.getClassName(), element.getId());
        }

        final LightUIFilesWithSelectionContext files = (LightUIFilesWithSelectionContext) element;
        this.tableId = files.tableId;
    }

    @Override
    public void fromJSON(final JSONObject json) {
        super.fromJSON(json);

        this.tableId = (String) JSONConverter.getParameterFromJSON(json, "table-id", String.class);
    }

    @Override
    public JSONObject toJSON() {
        final JSONObject json = super.toJSON();
        json.put("table-id", this.tableId);
        return json;
    }
}
