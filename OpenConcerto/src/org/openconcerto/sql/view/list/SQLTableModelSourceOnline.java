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
 
 package org.openconcerto.sql.view.list;

import org.openconcerto.sql.model.IFieldPath;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.BaseFillSQLRequest;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.change.ListChangeIndex;

import java.util.Collections;

/**
 * A SQLTableModelSource directly tied to the database. Any changes to its lines are propagated to
 * the database without any delay.
 * 
 * @author Sylvain
 */
public class SQLTableModelSourceOnline extends SQLTableModelSource {

    private final ListSQLRequest req;

    public SQLTableModelSourceOnline(ListSQLRequest req) {
        super(req.getGraph());
        this.req = req;
    }

    public SQLTableModelSourceOnline(SQLTableModelSourceOnline src) {
        super(src);
        this.req = src.req;
    }

    public final ListSQLRequest getReq() {
        return this.req;
    }

    @Override
    protected SQLTableModelSourceState createState() {
        return new SQLTableModelSourceStateOnline(this.getAllColumns(), this.getReq());
    }

    @Override
    protected void colsChanged(final ListChangeIndex<SQLTableModelColumn> change) {
        super.colsChanged(change);
        // add needed fields for each new column
        this.getReq().changeGraphToFetch(new IClosure<SQLRowValues>() {
            @Override
            public void executeChecked(SQLRowValues g) {
                for (final SQLTableModelColumn col : change.getItemsAdded()) {
                    // DebugRow should uses all *fetched* fields, but since it cannot know them
                    // getPaths() return all fields in the table. So don't fetch all fields just for
                    // this debug column
                    if (!(col instanceof DebugRow)) {
                        for (final IFieldPath p : col.getPaths()) {
                            BaseFillSQLRequest.addToFetch(g, p.getPath(), Collections.singleton(p.getFieldName()));
                        }
                    }
                }
            }
        });
    }

    @Override
    protected SQLTableModelLinesSourceOnline _createLinesSource(final ITableModel model) {
        return new SQLTableModelLinesSourceOnline(this, model);
    }

    @Override
    public SQLRowValues getMaxGraph() {
        return this.getReq().getGraphToFetch();
    }
}
