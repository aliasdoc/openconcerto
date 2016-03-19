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

import java.awt.Component;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;

import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.config.Log;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.FieldPath;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.ui.light.GroupToLightUIConvertor;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.sql.view.list.IListeAction;
import org.openconcerto.sql.view.list.RowAction;
import org.openconcerto.sql.view.list.SQLTableModelColumn;
import org.openconcerto.sql.view.list.SQLTableModelSourceOnline;
import org.openconcerto.task.config.ComptaBasePropsConfiguration;
import org.openconcerto.ui.AutoHideListener;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.light.ColumnSpec;
import org.openconcerto.ui.light.ColumnsSpec;
import org.openconcerto.ui.light.InformationLine;
import org.openconcerto.ui.light.LightUIButtonWithSelectionContext;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.LightUITable;
import org.openconcerto.ui.light.RowSelectionSpec;
import org.openconcerto.ui.light.SimpleTextLine;
import org.openconcerto.ui.light.TableSpec;
import org.openconcerto.ui.table.TableCellRendererUtils;
import org.openconcerto.utils.GestionDevise;
import org.openconcerto.utils.convertor.ValueConvertor;
import org.openconcerto.utils.i18n.TranslationManager;

/**
 * SQLElement de la base société
 * 
 * @author Administrateur
 * 
 */
public abstract class ComptaSQLConfElement extends SQLElement {

    private static DBRoot baseSociete;
    
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

    private static DBRoot getBaseSociete() {
        if (baseSociete == null)
            baseSociete = ((ComptaBasePropsConfiguration) Configuration.getInstance()).getRootSociete();
        return baseSociete;
    }

    private Group groupForCreation;
    private Group groupForModification;

    {
        this.setL18nLocation(Gestion.class);
    }

    public ComptaSQLConfElement(String tableName, String singular, String plural) {
        super(singular, plural, getBaseSociete().findTable(tableName, true));
    }

    public ComptaSQLConfElement(String tableName) {
        this(tableName, null);
    }

    public ComptaSQLConfElement(String tableName, String code) {
        super(getBaseSociete().findTable(tableName, true), null, code);
    }

    @Override
    protected String createCode() {
        return createCodeFromPackage();
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
     * @return The LightUIPanel which contains the LightUITable
     * @throws IllegalArgumentException
     */
    public LightUIPanel createUIPanelForTable() throws IllegalArgumentException {
        final LightUILine listLine = new LightUILine();
        listLine.add(createUIElementForTable());

        final LightUIPanel panel = new LightUIPanel(this.getCode());
        panel.addLine(listLine);
        return panel;
    }
    
    /**
     * Get columns user preferences for a specific table
     * @param userId - Id of the user who want view the table
     * @param tableId - Id of table to show
     * @param sqlColumns - List of columns to be displayed
     * @return the XML which contains user preferences
     */
    protected Document getColumnsUserPerfs(final int userId, final String tableId, final List<SQLTableModelColumn> sqlColumns) {
        Document columnsPrefs = null;
        try {
            final DOMBuilder in = new DOMBuilder();
            final org.w3c.dom.Document w3cDoc = Configuration.getInstance().getXMLConf(userId, tableId);
            if (w3cDoc != null) {
                columnsPrefs = in.build(w3cDoc);
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to get ColumnPrefs for table " + tableId + " and for user " + userId + "\n" + ex.getMessage());
        }
        
        if (columnsPrefs != null) {
            final Element rootElement = columnsPrefs.getRootElement();
            if (!rootElement.getName().equals("list")) {
                throw new IllegalArgumentException("invalid xml, roots node list expected but " + rootElement.getName() + " found");
            }

            final int columnsCount = sqlColumns.size();
            final List<Element> xmlColumns = rootElement.getChildren();
            if (xmlColumns.size() != columnsCount) {
                columnsPrefs = null;
            }
        }
        
        return columnsPrefs;
    }
    
    /**
     * Create ColumnsSpec from list of SQLTableModelColumn and apply user preferences
     * @param columns - list of SQLTableModelColumn (columns to be displayed)
     * @param columnsPrefs - user preferences for table columns 
     * @return New ColumnsSpec with user preferences application
     */
    protected ColumnsSpec createColumnsSpec(final List<SQLTableModelColumn> columns, final Document columnsPrefs) {
        final List<String> possibleColumnIds = new ArrayList<String>();
        final List<String> sortedIds = new ArrayList<String>();
        final List<ColumnSpec> columnsSpec = new ArrayList<ColumnSpec>();
        
        final int columnsCount = columns.size(); 
        
        for(int i = 0; i < columnsCount; i++) {
            final SQLTableModelColumn sqlColumn = columns.get(i);
            // TODO : creer la notion d'ID un peu plus dans le l'esprit sales.invoice.amount
            final String columnId = sqlColumn.getIdentifier();
            
            possibleColumnIds.add(columnId);
            columnsSpec.add(new ColumnSpec(columnId, sqlColumn.getValueClass(), sqlColumn.getName(), null, false, null));
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
     * @return The LightUITable associated to this ComptaSQLConfElement
     * @throws IllegalArgumentException
     */
    public LightUITable createUIElementForTable() throws IllegalArgumentException {
        final String tableId = this.getCode() + ".table";
        
        final SQLTableModelSourceOnline source = this.getTableSource();
        final List<SQLTableModelColumn> columns = source.getColumns();
        
        Document columnsPrefs = this.getColumnsUserPerfs(UserManager.getUserID(), tableId, columns);
        final RowSelectionSpec selection = new RowSelectionSpec(tableId);
        final ColumnsSpec columnsSpec = this.createColumnsSpec(columns, columnsPrefs);
        final TableSpec tSpec = new TableSpec(tableId, selection, columnsSpec);
        
        final LightUITable table = new LightUITable(tableId);
        table.setVerticallyScrollable(true);
        
        table.setTableSpec(tSpec);
        table.addAction(LightUIElement.ACTION_TYPE_SELECTION, "table.infos");

        return table;
    }

    /**
     * Create buttons from SQLElement row actions
     * @param selection - user selection
     * @return ui line with all available buttons
     */
    public LightUILine createRowAction(final RowSelectionSpec selection) {
        final LightUILine actionLine = new LightUILine();
        final Collection<IListeAction> actions = this.getRowActions();

        actionLine.setGridAlignment(LightUILine.ALIGN_LEFT);
        for (final Iterator<?> iterator = actions.iterator(); iterator.hasNext();) {
            final RowAction iListeAction = (RowAction) iterator.next();
            if (iListeAction.inHeader()) {

                final LightUIElement button = new LightUIButtonWithSelectionContext(iListeAction.getID(), iListeAction.getID(), selection.getTableId());
                button.setType(LightUIElement.TYPE_BUTTON_WITH_SELECTION_CONTEXT);

                final String label = TranslationManager.getInstance().getTranslationForAction(iListeAction.getID());
                button.setLabel(label);

                actionLine.add(button);
                // TODO: implement
                // desc.addControler(new ActivationOnSelectionControler("sales.quote.list",
                // element2.getId()));
            }
        }
        return actionLine;
    }

    /**
     * Create information ui line for selected lines. By default, all fields in SQLRowValues are displayed 
     * @param selection - SQLRowValues attach to selected lines
     * @return ui 
     */
    public LightUILine createDataLine(final List<SQLRowValues> selection) {
        if (selection == null) {
            return null;
        }
        final LightUILine dataLine = new LightUILine();
        final LightUIPanel panel = new LightUIPanel(this.getCode() + ".data.panel");
        final SQLFieldTranslator translator = Configuration.getTranslator(this.getTable());

        for (final SQLRowValues row : selection) {
            final int rowId = row.getID();
            final LightUILine mainLine = new LightUILine();
            final LightUIPanel mainLinePanel = new LightUIPanel(panel.getId() + ".main.line." + rowId);
            mainLinePanel.addLine(new SimpleTextLine(mainLinePanel.getId() + ".title", "Information sur l'élément n°" + rowId, true, LightUIElement.HALIGN_CENTER));
            final LightUILine lineData = new LightUILine();
            final LightUIPanel dataPanel = new LightUIPanel(this.getCode() + ".data.panel." + rowId);
            dataPanel.setPanelType(LightUIPanel.TYPE_TABLE);
            for (String fieldName : row.getFields()) {
                this.addFieldToPanel(fieldName, dataPanel, row, translator);
            }
            lineData.add(dataPanel);
            mainLinePanel.addLine(lineData);
            mainLine.add(mainLinePanel);
            panel.addLine(mainLine);
        }
        dataLine.add(panel);
        return dataLine;
    }

    /**
     * Add the field name translation and it's value to the infomation panel
     * @param fieldName - Field to be translate
     * @param dataPanel - Information panel
     * @param row - Row which contains data
     * @param translator - Field translator 
     */
    public void addFieldToPanel(final String fieldName, final LightUIPanel dataPanel, final SQLRowValues row, final SQLFieldTranslator translator) {
        if (!fieldName.equals("ID") && !fieldName.equals("ARCHIVE") && !fieldName.equals("ORDRE")) {
            final SQLField field = this.getTable().getField(fieldName);
            final String key = translator.getLabelFor(field);
            if (key != null) {
                String value = "";
                if (field.isKey()) {
                    final List<FieldPath> fieldsPath = Configuration.getInstance().getShowAs().expand(field);
                    for (FieldPath fieldPath : fieldsPath) {
                        final SQLRowValues foreignRow = row.followPath(fieldPath.getPath());
                        if (foreignRow != null) {
                            value += foreignRow.getString(fieldPath.getField().getName()) + " ";
                        }
                    }
                } else {
                    value = row.getString(fieldName);
                }

                if (value != null && !value.equals("")) {
                    dataPanel.addLine(new InformationLine(dataPanel.getId(), key, value));
                }
            }
        }
    }

    
    public LightUIFrame createUIFrameForCreation(final PropsConfiguration configuration, final long userId) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        final Group group = getGroupForCreation();
        if (group == null) {
            Log.get().severe("The group for creation is null for this element : " + this);
            return null;
        }
        final LightUIFrame frame = convertor.convert(group); 
        return frame;
    }

    public LightUIFrame createUIFrameForModification(final PropsConfiguration configuration, long id, final long userId) {
        final GroupToLightUIConvertor convertor = new GroupToLightUIConvertor(configuration);
        final Group group = getGroupForModification();
        if (group == null) {
            Log.get().severe("The group for modification is null for this element : " + this);
            return null;
        }
        final LightUIFrame frame = convertor.convert(getGroupForModification());
        return frame;
    }

    public Group getGroupForCreation() {
        if (this.groupForCreation != null) {
            return this.groupForCreation;
        }
        return getDefaultGroup();
    }

    public Group getGroupForModification() {
        if (this.groupForModification != null) {
            return this.groupForModification;
        }
        return getDefaultGroup();
    }
}
