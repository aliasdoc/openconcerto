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
 
 package org.openconcerto.sql.element;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.sql.request.RowItemDesc;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.ui.component.ITextArea;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.utils.i18n.TranslationManager;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class GroupSQLComponent extends BaseSQLComponent {

    private final Group group;
    private final int columns = 2;
    private final Map<String, JComponent> labels = new HashMap<String, JComponent>();
    private final Map<String, JComponent> editors = new HashMap<String, JComponent>();
    private String startTabAfter = null;
    private boolean tabGroup;
    private int tabDepth;
    private JTabbedPane pane;
    private final List<String> tabsGroupIDs = new ArrayList<String>();
    private Group additionnalFieldsGroup;
    private final boolean hasAdditionnalFields;

    public GroupSQLComponent(final SQLElement element) {
        this(element, element.getDefaultGroup());
    }

    public GroupSQLComponent(final SQLElement element, final Group group) {
        super(element);
        this.group = group;
        this.hasAdditionnalFields = this.getElement().getAdditionalFields().size() > 0;
        this.additionnalFieldsGroup = getAdditionalFieldsGroup(group.getDescendantGroups());
    }

    private Group getAdditionalFieldsGroup(Collection<Group> items) {
        for (Group g : items) {
            if (g.getId().endsWith("additionalElementFields")) {
                return g;
            }
        }
        return null;
    }

    protected final Group getGroup() {
        return this.group;
    }

    public void startTabGroupAfter(String id) {
        startTabAfter = id;
    }

    @Override
    protected void addViews() {
        this.setLayout(new GridBagLayout());
        final GridBagConstraints c = new DefaultGridBagConstraints();
        c.fill = GridBagConstraints.NONE;
        this.tabGroup = false;
        this.tabDepth = 0;
        // On laisse la place en haut pour les additionnals fields
        if (this.hasAdditionnalFields) {
            c.gridy = this.getElement().getAdditionalFields().size() / 2 + 1;
        }

        layout(this.group, 0, 0, 0, c, this);
    }

    public void layout(final Item currentItem, final Integer order, int x, final int level, GridBagConstraints c, JPanel panel) {
        final String id = currentItem.getId();

        final LayoutHints size = currentItem.getLocalHint();
        if (!size.isVisible()) {
            return;
        }

        if (size.isSeparated() || size.isSplit()) {
            x = 0;
            c.gridx = 0;
            c.gridy++;
        }
        if (currentItem instanceof Group) {
            final Group currentGroup = (Group) currentItem;
            final int stop = currentGroup.getSize();
            c.weighty = 0;
            if (this.tabGroup && level == this.tabDepth) {
                panel = new JPanel();
                panel.setLayout(new GridBagLayout());
                panel.setOpaque(false);
                c = new DefaultGridBagConstraints();
                x = 0;
                c.fill = GridBagConstraints.NONE;
                String label = TranslationManager.getInstance().getTranslationForItem(id);// getRIVDescForId(id).getLabel();
                if (label == null) {
                    label = id;
                }
                this.pane.addTab(label, panel);
                this.tabsGroupIDs.add(currentGroup.getId());
            } else {
                if (size.showLabel() && getLabel(id) != null) {
                    x = 0;
                    c.gridy++;
                    c.fill = GridBagConstraints.HORIZONTAL;
                    c.gridx = 0;
                    c.weightx = 1;
                    c.gridwidth = 4;
                    panel.add(getLabel(id), c);
                    c.gridy++;
                }
            }
            if (this.hasAdditionnalFields) {
                if ((currentGroup == this.group && this.additionnalFieldsGroup == null) || (currentGroup == this.additionnalFieldsGroup)) {
                    final Map<String, JComponent> additionalFields = this.getElement().getAdditionalFields();
                    for (String field : additionalFields.keySet()) {
                        Item item = new Item(field, new LayoutHints(false, false, true, false, true, false));
                        int fill = c.fill;
                        double weightx = c.weightx;
                        c.weightx = 1;
                        c.fill = GridBagConstraints.HORIZONTAL;
                        layout(item, 100, x, level + 1, c, panel);
                        c.weightx = weightx;
                        c.fill = fill;
                    }
                }
            }
            for (int i = 0; i < stop; i++) {
                final Item subGroup = currentGroup.getItem(i);
                final Integer subGroupOrder = currentGroup.getOrder(i);
                layout(subGroup, subGroupOrder, x, level + 1, c, panel);
            }
            if (this.tabGroup && level == this.tabDepth) {
                JPanel spacer = new JPanel();
                spacer.setOpaque(false);
                c.gridy++;
                c.weighty = 0.0001;
                panel.add(spacer, c);
            }

        } else {
            c.gridwidth = 1;
            if (size.showLabel()) {
                c.weightx = 0;
                c.weighty = 0;
                // Label
                if (size.isSplit()) {
                    c.gridwidth = 4;
                    c.weightx = 1;
                    c.fill = GridBagConstraints.NONE;
                } else {
                    c.fill = GridBagConstraints.HORIZONTAL;
                }
                panel.add(getLabel(id), c);
                if (size.isSplit()) {
                    c.gridy++;
                    c.gridx = 0;
                } else {
                    c.gridx++;
                }
            }
            // Editor
            final JComponent editor = getEditor(id);

            if (size.fillWidth() && size.fillHeight()) {
                c.fill = GridBagConstraints.BOTH;
            } else if (size.fillWidth()) {
                c.fill = GridBagConstraints.HORIZONTAL;
            } else if (size.fillHeight()) {
                c.fill = GridBagConstraints.VERTICAL;
            } else {
                c.fill = GridBagConstraints.NONE;
                DefaultGridBagConstraints.lockMinimumSize(editor);
            }
            if (size.fillHeight()) {
                c.weighty = 1;
            } else {
                c.weighty = 0;
            }
            if (size.largeWidth()) {
                if (size.isSplit() || !size.showLabel()) {
                    c.gridwidth = this.columns * 2;
                } else {
                    c.gridwidth = this.columns * 2 - 1;
                }
            } else {
                if (size.showLabel() && !size.isSplit()) {
                    c.gridwidth = 1;
                } else {
                    c.gridwidth = 2;
                }
            }
            if (c.gridx % 2 == 1) {
                c.weightx = 1;
            }

            panel.add(editor, c);

            try {
                JComponent comp = editor;
                if (editor instanceof JScrollPane) {
                    JScrollPane pane = (JScrollPane) editor;
                    comp = (JComponent) pane.getViewport().getView();
                }
                this.addView(comp, id);
                // avoid collapsing of Mode de réglement in client
                if (comp instanceof ElementSQLObject)
                    DefaultGridBagConstraints.lockMinimumSize(editor);
            } catch (final Exception e) {
                Log.get().warning(e.getMessage());
            }

            if (size.largeWidth()) {
                if (size.isSplit()) {
                    c.gridx += 4;
                } else {
                    c.gridx += 3;
                }
            } else {
                c.gridx++;
            }

            if (c.gridx >= this.columns * 2 || size.isSeparated()) {
                c.gridx = 0;
                c.gridy++;
                x = 0;
            }

        }
        if (id.equals(startTabAfter)) {
            if (tabGroup) {
                throw new IllegalArgumentException("ID " + id + " already set as tab");
            }
            tabGroup = true;
            tabDepth = level;
            pane = new JTabbedPane();
            c.gridx = 0;
            c.gridy++;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = 4;
            panel.add(pane, c);
        }

    }

    public final void setTabEnabledAt(final String groupID, final boolean enabled) {
        this.pane.setEnabledAt(this.tabsGroupIDs.indexOf(groupID), enabled);
    }

    public final boolean isTabEnabledAt(final String groupID) {
        return this.pane.isEnabledAt(this.tabsGroupIDs.indexOf(groupID));
    }

    public final void selectTabEnabled() {
        final int index = this.pane.getSelectedIndex();
        if (!this.pane.isEnabledAt(index)) {
            final int count = this.pane.getTabCount();
            // 1 since index is disabled
            for (int i = 1; i < count; i++) {
                final int mod = (index + i) % count;
                if (this.pane.isEnabledAt(mod)) {
                    this.pane.setSelectedIndex(mod);
                    return;
                }
            }
        }
    }

    @Override
    public Component addView(JComponent comp, String id) {
        final FieldMapper fieldMapper = PropsConfiguration.getInstance().getFieldMapper();
        SQLField field = null;
        if (fieldMapper != null) {
            field = fieldMapper.getSQLFieldForItem(id);
        }
        // Maybe the id is a field name (deprecated)
        if (field == null) {
            field = this.getTable().getFieldRaw(id);
        }
        // allow to add components in the UI which aren't in the request
        if (field != null)
            return super.addView(comp, field.getName());
        else
            return comp;
    }

    public JComponent createEditor(final String id) {
        if (id.startsWith("(") && id.endsWith(")*")) {
            try {
                final String table = id.substring(1, id.length() - 2).trim();
                final String idEditor = GlobalMapper.getInstance().getIds(table).get(0) + ".editor";
                final Class<?> cl = (Class<?>) GlobalMapper.getInstance().get(idEditor);
                return (JComponent) cl.newInstance();
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }

        final FieldMapper fieldMapper = PropsConfiguration.getInstance().getFieldMapper();
        SQLField field = null;
        if (fieldMapper != null) {
            field = fieldMapper.getSQLFieldForItem(id);
        }
        // Maybe the id is a field name (deprecated)
        if (field == null) {
            field = this.getTable().getFieldRaw(id);

        }
        if (field == null) {
            final JLabel jLabel = new JLabelBold("No field " + id);
            jLabel.setForeground(Color.RED.darker());
            String t = "<html>";

            final Set<SQLField> fields = this.getTable().getFields();

            for (final SQLField sqlField : fields) {
                t += sqlField.getFullName() + "<br>";
            }
            t += "</html>";
            jLabel.setToolTipText(t);
            return jLabel;
        }

        final SQLType type = field.getType();

        final JComponent comp;

        if (getElement().getPrivateElement(field.getName()) != null) {
            // private
            final SQLComponent sqlcomp = this.getElement().getPrivateElement(field.getName()).createDefaultComponent();
            final DefaultElementSQLObject dobj = new DefaultElementSQLObject(this, sqlcomp);
            dobj.setDecorated(false);
            dobj.showSeparator(false);
            DefaultGridBagConstraints.lockMinimumSize(sqlcomp);
            comp = dobj;
        } else if (field.isKey()) {
            // foreign

            final SQLElement foreignElement = getElement().getForeignElement(field.getName());
            if (foreignElement == null) {
                comp = new JLabelBold("no element for foreignd " + id);
                comp.setForeground(Color.RED.darker());
                Log.get().severe("no element for foreign " + field.getName());
            } else {
                comp = new ElementComboBox();
                ((ElementComboBox) comp).init(foreignElement);
            }
            comp.setOpaque(false);
        } else {
            if (Boolean.class.isAssignableFrom(type.getJavaType())) {
                // TODO hack to view the focus (should try to paint around the button)
                comp = new JCheckBox(" ");
                comp.setOpaque(false);
            } else if (Date.class.isAssignableFrom(type.getJavaType())) {
                comp = new JDate();
                comp.setOpaque(false);
            } else if (String.class.isAssignableFrom(type.getJavaType()) && type.getSize() >= 500) {
                comp = new ITextArea();
            } else {
                comp = new JTextField(Math.min(30, type.getSize()));
            }
        }

        return comp;
    }

    protected JComponent createLabel(final String id) {
        final JLabel jLabel = new JLabel();
        jLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        return jLabel;
    }

    private void registerPopupMenu(final JComponent label, final String id) {
        label.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(final MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3 && e.getModifiersEx() == 128) {

                    final JPopupMenu popMenu = new JPopupMenu();
                    final JMenu menuItemInfo = new JMenu("Information");
                    menuItemInfo.add(new JMenuItem("id: " + id));
                    menuItemInfo.add(new JMenuItem("label: " + getLabel(id).getClass().getName() + ":" + getLabel(id)));
                    menuItemInfo.add(new JMenuItem("editor: " + getEditor(id).getClass().getName() + ":" + getEditor(id)));
                    popMenu.add(menuItemInfo);
                    final JMenuItem menuItemDoc = new JMenuItem("Modifier la documentation");
                    menuItemDoc.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(final ActionEvent e) {
                            new DocumentationEditorFrame(GroupSQLComponent.this, id).setVisible(true);

                        }
                    });

                    popMenu.add(menuItemDoc);
                    popMenu.show(label, e.getX(), e.getY());
                }

            }

        });

    }

    @Override
    protected void updateUI(String id, RowItemDesc desc) {
        super.updateUI(id, desc);
        updateUI(id, getLabel(id), desc, Color.RED.darker());
    }

    public JComponent getLabel(final String id) {
        JComponent label = this.labels.get(id);
        if (label == null) {
            label = createLabel(id);
            if (!UserRightsManager.getCurrentUserRights().haveRight("GROUP_ITEM_SHOW", id)
                    || !UserRightsManager.getCurrentUserRights().haveRight("GROUP_ITEM_SHOW", getElement().getTable().getName() + "." + id)) {
                label.setVisible(false);
            }

            this.labels.put(id, label);
            registerPopupMenu(label, id);
            final RowItemDesc rivDesc = getRIVDescForId(id);
            updateUI(id, rivDesc);
        }
        return label;
    }

    private RowItemDesc getRIVDescForId(final String id) {
        if (TranslationManager.getInstance().getLocale() != null) {
            final String t = TranslationManager.getInstance().getTranslationForItem(id);
            if (t != null) {
                return new RowItemDesc(t, t);
            }
        }
        String fieldName = null;
        final FieldMapper fieldMapper = PropsConfiguration.getInstance().getFieldMapper();
        if (fieldMapper != null) {
            final SQLField sqlFieldForItem = fieldMapper.getSQLFieldForItem(id);
            if (sqlFieldForItem != null) {
                fieldName = sqlFieldForItem.getName();
            }
        }
        if (fieldName == null) {
            fieldName = id;
        }
        final RowItemDesc rivDesc = getRIVDesc(fieldName);
        return rivDesc;
    }

    public JComponent getEditor(final String id) {
        JComponent editor = this.editors.get(id);
        if (editor == null) {
            editor = createEditor(id);
            if (!UserRightsManager.getCurrentUserRights().haveRight("GROUP_ITEM_SHOW", id)
                    || !UserRightsManager.getCurrentUserRights().haveRight("GROUP_ITEM_SHOW", getElement().getTable().getName() + "." + id)) {
                editor.setVisible(false);
            }
            this.editors.put(id, editor);
        }
        return editor;
    }
}
