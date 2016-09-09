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

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.MutableRowItemView;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.sqlobject.itemview.BaseRowItemView;
import org.openconcerto.sql.view.IListPanel;
import org.openconcerto.ui.EnhancedTable;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.ui.state.JTableStateManager;
import org.openconcerto.ui.table.AlternateTableCellRenderer;
import org.openconcerto.ui.table.FocusAwareEditor;
import org.openconcerto.ui.table.XTableColumnModel;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;

import java.applet.Applet;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.text.JTextComponent;

public class RowValuesTable extends EnhancedTable implements AncestorListener, MutableRowItemView {

    private JTableStateManager stateManager;

    private RowValuesTableModel model;
    private XTableColumnModel columnModel = new XTableColumnModel();

    private boolean editorAndRendererDone;
    private List<String> clearElementList = new ArrayList<String>();

    public RowValuesTable(RowValuesTableModel model, File f) {
        this(model, f, false);
    }

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component c = super.prepareEditor(editor, row, column);
        if (c instanceof JTextComponent) {
            ((JTextComponent) c).selectAll();
        }
        return c;
    }

    public void addClearCloneTableElement(String elt) {
        this.clearElementList.add(elt);
    }

    public List<String> getClearCloneTableElement() {
        return this.clearElementList;
    }

    public RowValuesTable(RowValuesTableModel model, File f, boolean tiny) {
        this(model, f, tiny, new XTableColumnModel());
    }

    public RowValuesTable(RowValuesTableModel model, File f, boolean tiny, XTableColumnModel colModel) {
        super(model, colModel);

        this.setTableHeader(new RowValuesTableHeader(colModel));

        if (f == null) {
            f = IListPanel.getConfigFile(model.getSQLElement(), this.getClass());
        }
        this.stateManager = new JTableStateManager(this, f, true);
        this.columnModel = colModel;
        this.model = model;

        // Force the header to resize and repaint itself
        this.createDefaultColumnsFromModel();

        this.addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent event) {
                updateEditorAndRenderer();
            }

            public void ancestorMoved(AncestorEvent event) {
            }

            public void ancestorRemoved(AncestorEvent event) {
            }
        });

        // Set the minimum height
        int height = 150;
        if (tiny) {
            height = 80;
        }
        this.setMinimumSize(new Dimension(getMinimumSize().width, Math.max(height, getMinimumSize().height)));

        this.getModel().addTableModelListener(this);

        this.getTableHeader().setReorderingAllowed(false);

        this.addAncestorListener(this);

        // ALT ENTER pour ajouter une nouvelle ligne
        this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_DOWN_MASK), "addLine");
        this.getActionMap().put("addLine", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                getRowValuesTableModel().addNewRow();
            }
        });

        /**
         * En remplacement de : this.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
         *
         * Arret de l'edition si la table perd le focus La perte de focus est ignorée sur le
         * CellEditor gère lui même la perte de focus (FocusAwareEditor)
         *
         */
        final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        focusManager.addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                Component c = focusManager.getPermanentFocusOwner();
                while (c != null) {
                    if (c == RowValuesTable.this) {
                        // focus remains inside the table
                        return;
                    } else if ((c instanceof Window) || (c instanceof Applet && c.getParent() == null)) {
                        if (c == SwingUtilities.getRoot(RowValuesTable.this)) {
                            if (getCellEditor() != null && !(getCellEditor() instanceof FocusAwareEditor))
                                if (!getCellEditor().stopCellEditing()) {
                                    getCellEditor().cancelCellEditing();
                                }
                        }
                        break;
                    }
                    c = c.getParent();
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        updateEditorAndRenderer();
        super.paint(g);
    }

    public boolean getScrollableTracksViewportWidth() {
        if (this.autoResizeMode != AUTO_RESIZE_OFF) {
            if (getParent() instanceof JViewport) {
                return (((JViewport) getParent()).getWidth() > getPreferredSize().width);
            }
        }
        return false;
    }

    // when the viewport shrinks below the preferred size, return the minimum size
    // so that scrollbars will be shown
    public Dimension getPreferredSize() {
        final Dimension preferredSize = super.getPreferredSize();
        if (getParent() instanceof JViewport) {
            if (((JViewport) getParent()).getWidth() < preferredSize.width) {
                return new Dimension(getMinimumSize().width, preferredSize.height);
            }
        }

        return preferredSize;
    }

    /**
     * @param list
     */
    private synchronized final void updateEditorAndRenderer() {
        if (!this.editorAndRendererDone) {
            this.editorAndRendererDone = true;

            // Liste des colonnes visibles
            List<TableColumn> list = this.columnModel.getColumns(false);

            // Liste des SQLTAbleElement dans l'ordre
            List<SQLTableElement> listReal = this.model.getList();

            for (TableColumn aColumn : list) {

                // sqlTableElement correspondant à la colonne
                SQLTableElement sqlTableElement = listReal.get(aColumn.getModelIndex());

                TableCellRenderer renderer = sqlTableElement.getTableCellRenderer();
                aColumn.setCellRenderer(renderer);
                AlternateTableCellRenderer.setRendererAndListen(aColumn);
                TableCellEditor editor = sqlTableElement.getTableCellEditor(this);
                if (editor != null) {
                    aColumn.setCellEditor(editor);
                }
            }
        }
    }

    /*
     * Permet de reforcer l'application des renderers et des Editors. (Si ajout/suppression de
     * colonnes).
     */
    public void setEditorAndRendererDone(boolean b) {
        this.editorAndRendererDone = false;
    }

    public void loadState(String filename) {
        this.stateManager.loadState(new File(filename));
    }

    public void tableChanged(TableModelEvent e) {
        super.tableChanged(e);
        // Scroll à la ligne insérée
        if (e.getType() == TableModelEvent.INSERT) {
            scrollRectToVisible(new Rectangle(getCellRect(e.getFirstRow(), 0, true)));
        }
    }

    public RowValuesTableModel getRowValuesTableModel() {
        return this.model;
    }

    public void updateField(String field, SQLRowValues rowVals) {
        this.model.updateField(field, rowVals);
        // Clear pour fixer le probleme avec les editframe et ne pas fermer la fenetre
        // sinon les elements pointront sur la nouveau devis et l'ancien les perdra
        clear();
    }

    public void updateField(String field, int id) {
        this.model.updateField(field, id);
        // Clear pour fixer le probleme avec les editframe et ne pas fermer la fenetre
        // sinon les elements pointront sur la nouveau devis et l'ancien les perdra
        clear();
    }

    public void updateField(String field, int id, String fieldCondition) {
        this.model.updateField(field, id, fieldCondition);
        // Clear pour fixer le probleme avec les editframe et ne pas fermer la fenetre
        // sinon les elements pointront sur la nouveau devis et l'ancien les perdra
        clear();
    }

    public void updateField(String field, SQLRowValues rowVals, String fieldCondition) {
        this.model.updateField(field, rowVals, fieldCondition);
        // Clear pour fixer le probleme avec les editframe et ne pas fermer la fenetre
        // sinon les elements pointront sur la nouveau devis et l'ancien les perdra
        clear();
    }

    public void insertFrom(String field, int id, int exceptID) {
        this.model.insertFrom(field, id, exceptID);
        this.revalidate();
        this.repaint();
    }

    public void insertFrom(String field, int id) {
        this.model.insertFrom(field, id);
        this.revalidate();
        this.repaint();
    }

    public void insertFrom(SQLRowAccessor rowVals) {
        this.model.insertFrom(rowVals);
        this.revalidate();
        this.repaint();
    }

    public void insertFrom(String field, SQLRowValues rowVals) {
        this.model.insertFrom(rowVals);
        this.revalidate();
        this.repaint();
    }

    // Pour remplir tout l'espace
    public boolean getScrollableTracksViewportHeight() {
        return getPreferredSize().height < getParent().getHeight();
    }

    public void resizeAndRepaint() {
        // Ne pas virer, car on l'appelle hors du package
        super.resizeAndRepaint();
    }

    @Override
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
        // permet d'edition en 1 clic
        if (editCellAt(rowIndex, columnIndex)) {
            getEditorComponent().requestFocusInWindow();
            getEditorComponent().requestFocus();
        }
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        TableColumn tableColumn = getColumnModel().getColumn(column);
        TableCellEditor editor = tableColumn.getCellEditor();
        if (editor == null) {
            editor = getDefaultEditor(getColumnClass(column));
        }
        return editor;
    }

    /**
     * Suppression de toutes les lignes du model et du cache des CellDynamicModifiers
     */
    public final void clear() {
        this.model.clearRows();
        List<SQLTableElement> l = this.model.getList();
        for (SQLTableElement tableElement : l) {
            tableElement.clear();
        }
    }

    public void readState() {
        this.stateManager.loadState();
    }

    public void writeState() {
        try {
            this.stateManager.saveState();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ancestorAdded(AncestorEvent event) {
        updateEditorAndRenderer();
        this.stateManager.loadState();
    }

    public void ancestorMoved(AncestorEvent event) {

    }

    public void ancestorRemoved(AncestorEvent event) {

    }

    public SQLRowValues getSelectedRowValues() {
        try {
            return this.model.getRowValuesAt(this.getSelectedRow());
        } catch (Exception e) {
            // can happen when no selected row
            return null;
        }
    }

    public XTableColumnModel getColumnModel() {
        return (XTableColumnModel) super.getColumnModel();
    }

    @Override
    public void addValueListener(PropertyChangeListener l) {

    }

    @Override
    public void removeValueListener(PropertyChangeListener l) {
    }

    @Override
    public Component getComp() {
        return this;
    }

    private List<SQLField> fields;
    private String sqlName;

    @Override
    public final SQLField getField() {
        return BaseRowItemView.getOnlyOne(this.fields);
    }

    @Override
    public List<SQLField> getFields() {
        return this.fields;
    }

    @Override
    public String getSQLName() {
        return this.sqlName;
    }

    @Override
    public void insert(SQLRowValues vals) {
        System.err.println("Insert");
    }

    @Override
    public void resetValue() {

    }

    @Override
    public void setEditable(InteractionMode mode) {
        this.model.setEditable(mode.isEditable());
        super.setEnabled(mode.isEnabled());
    }

    public void setEditable(boolean b) {
        this.setEnabled(b);
    }

    @Override
    public void setEnabled(boolean enabled) {
        // don't overwrite R/O mode
        if (enabled != this.isEnabled())
            this.setEditable(enabled ? InteractionMode.READ_WRITE : InteractionMode.DISABLED);
    }

    @Override
    public void show(SQLRowAccessor r) {

    }

    @Override
    public void update(SQLRowValues vals) {

    }

    @Override
    public void addEmptyListener(EmptyListener l) {

    }

    @Override
    public void removeEmptyListener(EmptyListener l) {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void addValidListener(ValidListener l) {

    }

    @Override
    public void removeValidListener(ValidListener l) {

    }

    @Override
    public ValidState getValidState() {
        if (this.model.isValidated()) {
            return ValidState.getTrueInstance();
        } else {
            final SQLFieldTranslator trans = Configuration.getInstance().getTranslator();
            String fields = "(";
            for (SQLField field : this.model.getRequiredsField()) {
                fields += trans.getLabelFor(field) + ",";
            }
            fields += ")";
            final String text = "au moins " + this.model.getSQLElement().getSingularName() + " n'a pas le(s) champ(s) requis \"" + fields + "\" rempli(s)";
            return new ValidState(false, text);
        }
    }

    @Override
    public void init(String sqlName, Set<SQLField> fields) {
        if (fields.size() > 0) {
            this.fields = new ArrayList<SQLField>(fields);
        } else {
            this.fields = Collections.singletonList(this.model.getRequiredField());
        }
        this.sqlName = sqlName;
    }
}
