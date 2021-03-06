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

import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.IFieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.ui.light.LightUIColumnCellRenderer;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.convertor.ValueConvertor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

public abstract class SQLTableModelColumn {

    private final String name;
    private ValueConvertor converter;
    private Class convClass;
    private TableCellRenderer renderer;
    private LightUIColumnCellRenderer lightUIrenderer;
    private IClosure<TableColumn> installer;

    public SQLTableModelColumn(String name) {
        super();
        this.name = name;
        // don't call createRenderer() now as our subclass might not yet be inited
        this.renderer = null;
        this.installer = null;
        this.lightUIrenderer = null;
    }

    /**
     * The name to be displayed in the header.
     * 
     * @return the name, eg "Site".
     */
    public final String getName() {
        return this.name;
    }

    /**
     * The tool tip displayed in the column header.
     * 
     * @return the tool tip string.
     */
    public String getToolTip() {
        return this.getName();
    }

    /**
     * The unique (among a source) identifier of this column.
     * 
     * @return the identifier, eg "SITE.DESIGNATION".
     */
    public abstract String getIdentifier();

    /**
     * All the fields this column needs to show().
     * 
     * @return a set of fields this depends on.
     */
    public Set<SQLField> getFields() {
        final Set<SQLField> res = new HashSet<SQLField>();
        for (final IFieldPath fp : this.getPaths())
            res.add(fp.getField());
        return res;
    }

    // not IFieldPath so that we can rely on FieldPath.equals() (to use
    // Set<FieldPath>.contains())
    /**
     * The paths needed to {@link #show(SQLRowAccessor)}.
     * 
     * @return a set of paths.
     */
    public abstract Set<FieldPath> getPaths();

    /**
     * The fields modified in {@link #put(ListSQLLine, Object)}. This implementation returns
     * {@link #getFields()}.
     * 
     * @return a set of paths.
     */
    public SQLFieldsSet getWriteFields() {
        return new SQLFieldsSet(this.getFields());
    }

    /**
     * The tables for which rows can be inserted or deleted in {@link #put(ListSQLLine, Object)}.
     * This implementation returns an empty set.
     * 
     * @return the tables.
     */
    public Set<SQLTable> getWriteTables() {
        return Collections.emptySet();
    }

    /**
     * Which columns does show() needs.
     * 
     * @return a set of identifier.
     * @see #getIdentifier()
     */
    // not supported for now :
    // 1. SQLTableModelColumn.show() should take a map of dependent columns' values
    // 2. ListSQLLine.loadCache() and updateValueAt() should load in order (and throw exception if
    // there's a cycle in the dependencies).
    public final Set<String> getUsedCols() {
        return Collections.emptySet();
    }

    /**
     * If this column depends on some data outside rows. The returned identifiers are to be passed
     * to {@link UpdateQueue#putExternalUpdated(String, org.openconcerto.utils.cc.IPredicate)}.
     * 
     * @return the external identifiers used by this column.
     */
    public Set<String> getUsedExternals() {
        return Collections.emptySet();
    }

    /**
     * The class of the object returned by {@link #show(SQLRowAccessor)}.
     * 
     * @return the class of our value.
     */
    public final Class getValueClass() {
        return this.convClass != null ? this.convClass : this.getValueClass_();
    }

    public abstract boolean isEditable();

    /**
     * Extract the value to be passed to the renderer.
     * 
     * @param r the line to display.
     * @return the value for this column.
     */
    public final Object show(SQLRowAccessor r) {
        Object res = this.show_(r);
        assert res == null || getValueClass_().isInstance(res) : getValueClass_() + " is not the class of " + res;
        if (this.converter != null)
            res = this.converter.convert(res);
        assert res == null || getValueClass().isInstance(res);
        return res;
    }

    /**
     * Update the value of this column for the passed line to <code>obj</code>.
     * 
     * @param r the line to modify.
     * @param obj a value of this column.
     */
    public final void put(ListSQLLine r, Object obj) {
        this.put_(r, this.converter == null ? obj : this.converter.unconvert(obj));
    }

    /**
     * The class of the object returned by {@link #show_(SQLRowAccessor)}.
     * 
     * @return the class of our value.
     */
    protected abstract Class getValueClass_();

    protected abstract Object show_(SQLRowAccessor r);

    protected abstract void put_(ListSQLLine r, Object obj);

    public <C> void setConverter(ValueConvertor<?, C> vc, Class<C> c) {
        this.converter = vc;
        this.convClass = c;
    }

    public final TableCellRenderer getRenderer() {
        if (this.renderer == null)
            this.renderer = this.createDefaultRenderer();
        return this.renderer;
    }

    /**
     * Called by <code>getRenderer()</code> if {@link #setRenderer(TableCellRenderer)} wasn't called
     * or was called with <code>null</code>. This implementation returns <code>null</code>.
     * 
     * @return the renderer that {@link #getRenderer()} should return.
     */
    protected TableCellRenderer createDefaultRenderer() {
        return null;
    }

    public final void setRenderer(TableCellRenderer renderer) {
        this.renderer = renderer;
    }

    public void install(TableColumn col) {
        // always set the renderer, so we can remove one (set to null)
        col.setCellRenderer(this.getRenderer());
        if (this.installer != null)
            this.installer.executeChecked(col);
    }

    public final void setColumnInstaller(IClosure<TableColumn> installer) {
        this.installer = installer;
    }

    public LightUIColumnCellRenderer getLightUIrenderer() {
        return lightUIrenderer;
    }

    public void setLightUIrenderer(LightUIColumnCellRenderer lightUIrenderer) {
        this.lightUIrenderer = lightUIrenderer;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + this.getIdentifier();
    }
}
