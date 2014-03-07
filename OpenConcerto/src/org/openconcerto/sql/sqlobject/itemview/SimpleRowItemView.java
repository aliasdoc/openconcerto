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
 
 package org.openconcerto.sql.sqlobject.itemview;

import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.valuewrapper.ValueWrapper;

public class SimpleRowItemView<T> extends VWRowItemView<T> {

    public SimpleRowItemView(ValueWrapper<T> wrapper) {
        super(wrapper);
    }

    protected final SQLTable getTable() {
        return this.getField().getTable();
    }

    @Override
    public void setEditable(boolean b) {
        if (this.getComp() != null)
            this.getComp().setEnabled(b);
    }
}
