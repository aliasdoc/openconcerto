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
 
 package org.openconcerto.erp.core.common.element;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.config.Log;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.sql.model.SQLTable.VirtualFields;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.request.ListSQLRequest;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.ui.light.GroupToLightUIConvertor;
import org.openconcerto.sql.ui.light.LightEditFrame;
import org.openconcerto.sql.ui.light.LightUIPanelFiller;
import org.openconcerto.sql.ui.light.SearchInfo;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.EditFrame;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelColumnPath;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.ui.AutoHideListener;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.light.ColumnSpec;
import org.openconcerto.ui.light.ColumnsSpec;
import org.openconcerto.ui.light.InformationLine;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.LightUITable;
import org.openconcerto.ui.light.Row;
import org.openconcerto.ui.light.RowSelectionSpec;
import org.openconcerto.ui.light.RowsBulk;
import org.openconcerto.ui.light.SearchSpec;
import org.openconcerto.ui.light.SimpleTextLine;
import org.openconcerto.ui.light.TableContent;
import org.openconcerto.ui.light.TableSpec;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.convertor.ValueConvertor;

import java.awt.Color;
import java.awt.Component;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdom2.Document;
import org.jdom2.input.DOMBuilder;

/**
 * SQLElement de la base société
 * 
 * @author Administrateur
 * 
 */
public abstract class SocieteSQLConfElement extends SQLElement {

    {
        this.setL18nLocation(Gestion.class);
    }

    public SocieteSQLConfElement(SQLTable table, String singular, String plural) {
        super(singular, plural, table);
    }

    public SocieteSQLConfElement(SQLTable table) {
        this(table, null);
    }

    public SocieteSQLConfElement(SQLTable table, String code) {
        super(table, null, code);
    }

    public static final TableCellRenderer CURRENCY_RENDERER = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final Component res = super.getTableCellRendererComponent(table, GestionDevise.currencyToString((BigDecimal) value), isSelected, hasFocus, row, column);
            // this renderer can be decorated by e.g. ListeFactureRenderer which does a
            // setBackground(), thus always reset the colors
            // MAYBE always use ProxyComp as in AlternateTableCellRenderer to leave the decorated
            // renderer as found
            TableCellRendererUtils.setColors(res, table, isSelected);
            ((JLabel) res).setHorizontalAlignment(SwingConstants.RIGHT);
            return res;
        }
    };

    static public final JPanel createAdditionalPanel() {
        return AutoHideListener.listen(new JPanel());
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage();
    }

    public SQLRowValues createDefaultRowValues() {
        return new SQLRowValues(getTable());
    }

    public RowsBulk doSearch(final PropsConfiguration configuration, final SearchSpec searchSpec, final ColumnsSpec columnsSpec, final int startIndex, final int limit) {
        long t1 = System.currentTimeMillis();

        final SQLTableModelSourceOnline tableSource = this.getTableSource();
        final SearchInfo sInfo = (searchSpec != null) ? new SearchInfo(searchSpec) : null;

        final ListSQLRequest req = tableSource.getReq();
        req.setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(final SQLSelect sel) {
                setWhere(sel, tableSource, sInfo, startIndex, -1);
                return sel;
            }
        });

        long t2 = System.currentTimeMillis();
        int count = req.getValuesCount();
        long t3 = System.currentTimeMillis();

        req.setSelectTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(final SQLSelect sel) {
                setWhere(sel, tableSource, sInfo, startIndex, limit);
                return sel;
            }
        });

        // get values
        long t4 = System.currentTimeMillis();
        final List<SQLRowValues> rowValues = req.getValues();
        final int size = rowValues.size();
        long t5 = System.currentTimeMillis();
        System.err.println("DefaultTableContentHandler.handle() getReq :" + (t2 - t1) + " ms");
        System.err.println("DefaultTableContentHandler.handle() getValuesCount() :" + count + " : " + (t3 - t2) + " ms");
        System.err.println("DefaultTableContentHandler.handle() setWhere : " + (t4 - t3) + " ms");
        System.err.println("DefaultTableContentHandler.handle() getValues() :" + size + " : " + (t5 - t4) + " ms");

        final List<SQLTableModelColumn> allCols = tableSource.getColumns();
        final List<Row> matchingRows = new ArrayList<Row>();

        // FIXME: Dégager la conf xml si les nombre de colonnes ne match plus
        final int columnsCount = allCols.size();
        if (columnsSpec.getColumnCount() == columnsCount) {
            for (int i = 0; i < size; i++) {
                final SQLRowValues rowV = rowValues.get(i);
                final Row row = new Row(rowV.getID());
                final List<Object> l = new ArrayList<Object>();
                for (int j = 0; j < columnsCount; j++) {
                    final String columnId = columnsSpec.getColumn(j).getId();
                    final SQLTableModelColumn col = this.getColumnFromId(allCols, columnId);

                    if (col != null) {
                        Object value = col.show(rowV);
                        if (col.getLightUIrenderer() != null) {
                            value = col.getLightUIrenderer().getLightUIElement(value, i, j);
                        }
                        if (value instanceof SQLRowValues) {
                            value = ((SQLRowValues) value).getIDNumber();
                        }
                        l.add(value);
                    } else {
                        throw new IllegalArgumentException("column " + columnId + " is in xmlPref but it is not found in SQLTableModelColumn");
                    }
                }
                row.setValues(l);
                matchingRows.add(row);
            }
        } else {
            for (int i = 0; i < size; i++) {
                final SQLRowValues rowV = rowValues.get(i);
                final Row row = new Row(rowV.getID());
                final List<Object> l = new ArrayList<Object>();
                for (int j = 0; j < columnsCount; j++) {
                    final SQLTableModelColumn tableModelColumn = allCols.get(j);
                    Object value = tableModelColumn.show(rowV);
                    if (tableModelColumn.getLightUIrenderer() != null) {
                        value = tableModelColumn.getLightUIrenderer().getLightUIElement(value, i, j);
                    }
                    if (value instanceof SQLRowValues) {
                        value = ((SQLRowValues) value).getIDNumber();
                    }

                    l.add(value);
                }
                row.setValues(l);
                matchingRows.add(row);
            }
        }

        return new RowsBulk(matchingRows, startIndex, count);
    }

    private void setWhere(final SQLSelect sel, final SQLTableModelSourceOnline tableSource, final SearchInfo sInfo, final int startIndex, final int limit) {
        if (sInfo != null) {
            final List<SQLTableModelColumn> cols = tableSource.getColumns();
            final Set<SQLField> fields = new HashSet<SQLField>();
            for (final SQLTableModelColumn sqlTableModelColumn : cols) {
                fields.addAll(sqlTableModelColumn.getFields());
            }
            final List<SQLField> strFields = new ArrayList<SQLField>();
            final List<Where> wheres = new ArrayList<Where>();
            for (final SQLField sqlField : fields) {
                if (sqlField.getType().getJavaType().equals(String.class)) {
                    strFields.add(sqlField);
                    final List<String> texts = sInfo.getTexts();
                    for (String string : texts) {
                        final Where w = new Where(sel.getAlias(sqlField), "LIKE", "%" + string + "%");
                        wheres.add(w);
                    }

                }
            }
            sel.setWhere(Where.or(wheres));
        }
        if (limit != -1) {
            sel.setLimit(limit);
            sel.setOffset(startIndex);
        }
    }

    protected String getReadOnlyFrameTitle(final SQLRowValues sqlRow) {
        return EditFrame.getReadOnlyMessage(this);
    }

    protected String getModificationFrameTitle(final SQLRowValues sqlRow) {
        return EditFrame.getModifyMessage(this);
    }

    protected String getCreationFrameTitle() {
        return EditFrame.getCreateMessage(this);
    }

    /**
     * Return a code that doesn't change when subclassing to allow to easily change a SQLElement
     * while keeping the same code. To achieve that, the code isn't
     * {@link #createCodeFromPackage(Class) computed} with <code>this.getClass()</code>. We iterate
     * up through our superclass chain, and as soon as we find an abstract class, we stop and use
     * the previous class (i.e. non abstract). E.g. any direct subclass of
     * {@link ComptaSQLConfElement} will still use <code>this.getClass()</code>, but so is one of
     * its subclass.
     * 
     * @return a code computed from the superclass just under the first abstract superclass.
     * @see #createCodeFromPackage(Class)
     */
    protected final String createCodeFromPackage() {
        return createCodeFromPackage(getLastNonAbstractClass());
    }

    private final Class<? extends ComptaSQLConfElement> getLastNonAbstractClass() {
        Class<?> prev = null;
        Class<?> cl = this.getClass();
        // test loop
        assert !Modifier.isAbstract(cl.getModifiers()) && ComptaSQLConfElement.class.isAssignableFrom(cl) && Modifier.isAbstract(ComptaSQLConfElement.class.getModifiers());
        while (!Modifier.isAbstract(cl.getModifiers())) {
            prev = cl;
            cl = cl.getSuperclass();
        }
        assert ComptaSQLConfElement.class.isAssignableFrom(prev);
        @SuppressWarnings("unchecked")
        final Class<? extends ComptaSQLConfElement> res = (Class<? extends ComptaSQLConfElement>) prev;
        return res;
    }

    static protected String createCodeFromPackage(final Class<? extends ComptaSQLConfElement> cl) {
        String canonicalName = cl.getName();
        if (canonicalName.contains("erp.core") && canonicalName.contains(".element")) {
            int i = canonicalName.indexOf("erp.core") + 9;
            int j = canonicalName.indexOf(".element");
            canonicalName = canonicalName.substring(i, j);
        }
        return canonicalName;
    }

    @Override
    protected void _initTableSource(SQLTableModelSourceOnline res) {
        super._initTableSource(res);
        for (final SQLTableModelColumn col : res.getColumns()) {
            // TODO getDeviseFields()
            if (col.getValueClass() == Long.class || col.getValueClass() == BigInteger.class) {
                col.setConverter(new ValueConvertor<Number, BigDecimal>() {
                    @Override
                    public BigDecimal convert(Number o) {
                        if (o == null) {
                            System.err.println("ComptaSQLConfElement._initTableSource: Warning null Number conversion (" + this + ")");
                            return BigDecimal.ZERO;
                        }
                        return new BigDecimal(o.longValue()).movePointLeft(2);
                    }

                    @Override
                    public Number unconvert(BigDecimal o) {

                        if (o == null) {
                            System.err.println("ComptaSQLConfElement._initTableSource: Warning null BigDecimal conversion (" + this + ")");
                            return 0;
                        }
                        return o.movePointRight(2);
                    }
                }, BigDecimal.class);
                col.setRenderer(CURRENCY_RENDERER);
            }
        }
    }

    /**
     * Create a new panel which contains the table
     * 
     * @return The LightUIPanel which contains the LightUITable
     * @throws IllegalArgumentException
     */
    public LightUIPanel createUIPanelForTable(final Configuration configuration) throws IllegalArgumentException {
        final LightUIPanel panel = new LightUIPanel(this.getCode());

        final LightUILine listLine = new LightUILine();
        listLine.addChild(createUIElementForTable(configuration));

        panel.setWeightX(1);
        panel.setFillWidth(true);
        panel.addChild(listLine);
        return panel;
    }

    /**
     * Get columns user preferences for a specific table
     * 
     * @param userId - Id of the user who want view the table
     * @param tableId - Id of table to show
     * @param sqlColumns - List of columns to be displayed
     * @return the XML which contains user preferences
     */
    protected Document getColumnsUserPerfs(final Configuration configuration, final int userId, final String tableId, final List<SQLTableModelColumn> sqlColumns) {
        Document columnsPrefs = null;
        try {
            final DOMBuilder in = new DOMBuilder();
            final org.w3c.dom.Document w3cDoc = configuration.getXMLConf(userId, tableId);
            if (w3cDoc != null) {
                columnsPrefs = in.build(w3cDoc);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to get ColumnPrefs for table " + tableId + " and for user " + userId + "\n" + ex.getMessage());
        }

        return columnsPrefs;
    }

    /**
     * Create ColumnsSpec from list of SQLTableModelColumn and apply user preferences
     * 
     * @param columns - list of SQLTableModelColumn (columns to be displayed)
     * @param columnsPrefs - user preferences for table columns
     * @return New ColumnsSpec with user preferences application
     */
    protected ColumnsSpec createColumnsSpec(final Configuration configuration, final List<SQLTableModelColumn> columns, final Document columnsPrefs) {
        final List<String> possibleColumnIds = new ArrayList<String>();
        final List<String> sortedIds = new ArrayList<String>();
        final List<ColumnSpec> columnsSpec = new ArrayList<ColumnSpec>();

        final int columnsCount = columns.size();

        for (int i = 0; i < columnsCount; i++) {
            final SQLTableModelColumn sqlColumn = columns.get(i);
            // TODO : creer la notion d'ID un peu plus dans le l'esprit sales.invoice.amount
            final String columnId = sqlColumn.getIdentifier();

            possibleColumnIds.add(columnId);
            Class<?> valueClass = sqlColumn.getValueClass();
            if (sqlColumn.getLightUIrenderer() != null) {
                valueClass = LightUIElement.class;
            }

            // FIXME: bad code, if column is composed of more than one SQLField, this code could be
            // print a wrong column name
            String columnName = "";
            if (sqlColumn.getFields().size() == 1) {
                final SQLField field = sqlColumn.getFields().iterator().next();
                columnName = SQLTableModelColumnPath.getDescFor(field, configuration).getTitleLabel();
            }

            if (columnName == null || columnName.isEmpty()) {
                columnName = sqlColumn.getName();
            }
            /***********************/

            columnsSpec.add(new ColumnSpec(columnId, valueClass, columnName, null, false, null));
        }
        // FIXME : recuperer l'info sauvegardée sur le serveur par user (à coder)
        sortedIds.add(columnsSpec.get(0).getId());

        final ColumnsSpec cSpec = new ColumnsSpec(this.getCode(), columnsSpec, possibleColumnIds, sortedIds);
        cSpec.setAllowMove(true);
        cSpec.setAllowResize(true);
        cSpec.setUserPrefs(columnsPrefs);

        return cSpec;
    }

    /**
     * Create the LightUITable associated to this ComptaSQLConfElement
     * 
     * @return The LightUITable associated to this ComptaSQLConfElement
     * @throws IllegalArgumentException
     */
    public LightUITable createUIElementForTable(final Configuration configuration) throws IllegalArgumentException {
        final String tableId = this.getCode() + ".table";

        final SQLTableModelSourceOnline source = this.getTableSource();
        final List<SQLTableModelColumn> columns = source.getColumns();

        // FIXME: replace UserManager.getUserID() by session.getUser().getId()
        final Document columnsPrefs = this.getColumnsUserPerfs(configuration, UserManager.getUserID(), tableId, columns);
        final RowSelectionSpec selection = new RowSelectionSpec(tableId);
        final ColumnsSpec columnsSpec = this.createColumnsSpec(configuration, columns, columnsPrefs);
        final TableSpec tSpec = new TableSpec(tableId, selection, columnsSpec);

        final LightUITable table = new LightUITable(tableId);
        table.setFillWidth(true);
        table.setWeightX(1);
        table.setElementCode(this.getCode());

        table.setTableSpec(tSpec);
        table.addAction(LightUIElement.ACTION_TYPE_SELECTION, "table.infos");

        return table;
    }

    /**
     * Create buttons from SQLElement secondary row actions
     */
    public LightUILine createSecondaryRowActionLine(final RowSelectionSpec selection) {
        return null;
    }

    /**
     * Create information ui panel for selected lines. By default, all fields in SQLRowValues are
     * displayed
     * 
     * @param selection - SQLRowValues attach to selected lines
     * @return LightUIPanel
     */
    public LightUIPanel createDataPanel(final List<SQLRowValues> selection, Configuration configuration) {
        if (selection == null) {
            return null;
        }
        final LightUIPanel panel = new LightUIPanel(this.getCode() + ".data.panel");
        panel.setVerticallyScrollable(true);
        panel.setWeightX(1);
        final SQLFieldTranslator translator = configuration.getTranslator();

        for (final SQLRowValues row : selection) {
            final int rowId = row.getID();
            final LightUILine mainLine = new LightUILine();
            final LightUIPanel mainLinePanel = new LightUIPanel(panel.getId() + ".main.line." + rowId);
            mainLinePanel.setWeightX(1);
            mainLinePanel.addChild(new SimpleTextLine("Information sur l'élément n°" + rowId, true, LightUIElement.HALIGN_CENTER));
            final LightUILine lineData = new LightUILine();
            final LightUIPanel dataPanel = new LightUIPanel(this.getCode() + ".data.panel." + rowId);
            dataPanel.setWeightX(1);
            for (String fieldName : row.getFields()) {
                this.addFieldToPanel(fieldName, dataPanel, row, translator);
            }
            lineData.addChild(dataPanel);
            mainLinePanel.addChild(lineData);
            mainLine.addChild(mainLinePanel);
            panel.addChild(mainLine);
        }
        return panel;
    }

    public void addFieldToPanel(final String fieldName, final LightUIPanel dataPanel, final SQLRowValues row, final SQLFieldTranslator translator) {
        addFieldToPanel(fieldName, dataPanel, row, translator, false, "");
    }

    static private final VirtualFields FIELDS_TO_IGNORE = VirtualFields.PRIMARY_KEY.union(VirtualFields.ARCHIVE).union(VirtualFields.ORDER);

    /**
     * Add the field name translation and it's value to the information panel
     * 
     * @param fieldName - Field to be translate
     * @param dataPanel - Information panel
     * @param row - Row which contains data
     * @param translator - Field translator
     */
    public void addFieldToPanel(final String fieldName, final LightUIPanel dataPanel, final SQLRowValues row, final SQLFieldTranslator translator, boolean addEmpty, String defaultValue) {
        final SQLField field = this.getTable().getField(fieldName);
        if (!this.getTable().getFields(FIELDS_TO_IGNORE).contains(field)) {
            String key = translator.getLabelFor(field);
            boolean error = false;
            if (key == null) {
                error = true;
                key = field.getFieldName();
            }

            String value = "";
            if (field.isKey()) {
                final List<FieldPath> fieldsPath = getListExpander().expand(field);
                for (FieldPath fieldPath : fieldsPath) {
                    final SQLRowValues foreignRow = row.followPath(fieldPath.getPath());
                    if (foreignRow != null) {
                        value += foreignRow.getString(fieldPath.getField().getName()) + " ";
                    }
                }
            } else {
                value = row.getString(fieldName);
            }
            boolean isDefault = false;
            if (value == null || value.isEmpty()) {
                isDefault = true;
                value = defaultValue;
            }
            if (!value.isEmpty() || addEmpty) {
                final InformationLine line = new InformationLine(key, value);
                if (error) {
                    line.setLabelColor(Color.RED);
                }
                line.setItalicOnValue(isDefault);
                dataPanel.addChild(line);
            }
        }
    }

    public Group getEditGroup(final EditMode editMode) {
        if (editMode.equals(EditMode.CREATION)) {
            return this.getGroupForCreation();
        } else {
            return this.getGroupForModification();
        }
    }

    public GroupToLightUIConvertor getGroupToLightUIConvertor(final PropsConfiguration configuration, final EditMode editMode, final SQLRowValues sqlRow, final long userId) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        if (editMode.equals(EditMode.CREATION)) {
            convertor.putAllCustomEditorProvider(this.getCustomEditorProviderForCreation(configuration, userId));
        } else {
            convertor.putAllCustomEditorProvider(this.getCustomEditorProviderForModification(configuration, sqlRow, userId));
        }
        convertor.addAllModifer(this.getConvertorModifiers());

        return convertor;
    }

    /**
     * Create the edition frame for this SQLElement
     * 
     * @param configuration current configuration
     * @param parentFrame parent frame of the edit frame
     * @param editMode edition mode (CREATION, MODIFICATION, READONLY)
     * @param sqlRow SQLRowValues use for fill the edition frame
     * @param userId ID of current user
     * @return the edition frame of this SQLElement
     */
    public LightEditFrame createEditFrame(final PropsConfiguration configuration, final LightUIFrame parentFrame, final EditMode editMode, final SQLRowValues sqlRow, final long userId) {
        final Group editGroup = this.getEditGroup(editMode);
        if (editGroup == null) {
            Log.get().severe("The edit group is null for this element : " + this);
            return null;
        }

        final GroupToLightUIConvertor convertor = this.getGroupToLightUIConvertor(configuration, editMode, sqlRow, userId);
        final LightEditFrame editFrame = convertor.convert(editGroup, sqlRow, parentFrame, editMode);
        if (editMode.equals(EditMode.CREATION)) {
            editFrame.setTitle(this.getCreationFrameTitle());
        } else if (editMode.equals(EditMode.MODIFICATION)) {
            editFrame.setTitle(this.getModificationFrameTitle(sqlRow));
            new LightUIPanelFiller(editFrame.getFirstChild(LightUIPanel.class)).fillFromRow(configuration, sqlRow);
        } else if (editMode.equals(EditMode.READONLY)) {
            editFrame.setTitle(this.getReadOnlyFrameTitle(sqlRow));
            new LightUIPanelFiller(editFrame.getFirstChild(LightUIPanel.class)).fillFromRow(configuration, sqlRow);
        }

        return editFrame;
    }

    public List<SQLRowValues> getRowValues(final String fieldName, final long id) {
        final SQLTableModelSourceOnline tableSource = this.getTableSource(true);

        final ListSQLRequest req = tableSource.getReq();
        req.setWhere(new Where(this.getTable().getField(fieldName), "=", id));
        return req.getValues();
    }

    public TableContent createTableContent(final LightUITable uiTable, final String fieldName, final long id) {
        final SQLTableModelSourceOnline tableSource = this.getTableSource(true);
        final List<SQLTableModelColumn> allCols = tableSource.getColumns();

        final List<SQLRowValues> listRowValues = this.getRowValues(fieldName, id);
        final List<Row> matchingRows = new ArrayList<Row>();
        final int size = listRowValues.size();
        for (int i = 0; i < size; i++) {
            final SQLRowValues rowV = listRowValues.get(i);
            matchingRows.add(this.createRowFromSQLRow(rowV, allCols, uiTable.getTableSpec().getColumns()));
        }

        final TableContent tableContent = new TableContent(uiTable.getId(), matchingRows);

        return tableContent;
    }
}
