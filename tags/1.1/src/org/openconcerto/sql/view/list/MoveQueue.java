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

import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.SleepingQueue;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

final class MoveQueue extends SleepingQueue {

    private final ITableModel tableModel;

    public MoveQueue(ITableModel model) {
        super(MoveQueue.class.getSimpleName() + " on " + model);
        this.tableModel = model;
    }

    public void move(final int id, final int inc) {
        this.put(new Runnable() {
            public void run() {
                final AtomicReference<Integer> destID = new AtomicReference<Integer>();
                final CountDownLatch latch = new CountDownLatch(1);
                MoveQueue.this.tableModel.invokeLater(new Runnable() {
                    public void run() {
                        destID.set(MoveQueue.this.tableModel.getDestID(id, inc));
                        latch.countDown();
                    }
                });
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw ExceptionUtils.createExn(IllegalStateException.class, "move failed", e);
                }
                if (destID.get() != null) {
                    try {
                        moveQuick(id, destID.get());
                    } catch (SQLException e) {
                        throw ExceptionUtils.createExn(IllegalStateException.class, "move failed", e);
                    }
                }
            }
        });
    }

    final void moveQuick(final int srcId, final int destId) throws SQLException {
        final SQLRow srcRow = this.getTable().getRow(srcId);
        final SQLRow destRow = this.getTable().getRow(destId);

        final boolean after = srcRow.getOrder().compareTo(destRow.getOrder()) < 0;

        final SQLRowValues vals = srcRow.createEmptyUpdateRow();
        vals.setOrder(destRow, after).update();
    }

    private SQLTable getTable() {
        return this.tableModel.getTable();
    }

}
