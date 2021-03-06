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
 
 package org.openconcerto.sql.ui.light;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.request.RowItemDesc;
import org.openconcerto.sql.view.EditPanel.EditMode;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightUICheckBox;
import org.openconcerto.ui.light.LightUIComboBox;
import org.openconcerto.ui.light.LightUIDate;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.ui.light.LightUILabel;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.LightUITextArea;
import org.openconcerto.ui.light.LightUITextField;
import org.openconcerto.utils.i18n.TranslationManager;

import java.awt.Color;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class GroupToLightUIConvertor {
    private final int maxColumnCount;
    private PropsConfiguration configuration;
    private FieldMapper mapper;
    private Map<String, CustomEditorProvider> customEditorProviders = new HashMap<String, CustomEditorProvider>();
    private Map<String, ConvertorModifer> modifers = new HashMap<String, ConvertorModifer>();

    public GroupToLightUIConvertor(PropsConfiguration conf) {
        this(conf, 4);
    }

    public GroupToLightUIConvertor(PropsConfiguration conf, int columns) {
        this.maxColumnCount = columns;
        this.configuration = conf;
        this.mapper = this.configuration.getFieldMapper();
        if (this.mapper == null) {
            throw new IllegalArgumentException("null mapper");
        }
    }

    public LightEditFrame convert(final Group group, final SQLRowValues defaultRow, final LightUIFrame parentFrame, final EditMode editMode) {
        if (group == null) {
            throw new IllegalArgumentException("Null Group");
        }
        if (defaultRow == null) {
            throw new IllegalArgumentException("Null default SQLRowValues");
        }

        final SQLElement sqlElement = this.configuration.getDirectory().getElement(defaultRow.getTable());
        if (!sqlElement.getGroupForCreation().equals(group) && sqlElement.getGroupForModification().equals(group)) {
            throw new IllegalArgumentException("This group isn't attached to this SQLElement, group ID: " + group.getId() + " element code: " + sqlElement.getCode());
        }

        final LightEditFrame editFrame = new LightEditFrame(this.configuration, group, defaultRow, parentFrame, editMode);
        final LightUIPanel framePanel = editFrame.getFirstChild(LightUIPanel.class);
        append(framePanel, group);

        String frameTitle = TranslationManager.getInstance().getTranslationForItem(group.getId());
        if (frameTitle == null) {
            frameTitle = group.getId();
        }
        editFrame.setTitle(frameTitle);

        Log.get().warning("No translation for " + group.getId());
        return editFrame;
    }

    private void append(final LightUIPanel panel, final Item item) {
        if (item instanceof Group) {
            final Group gr = (Group) item;
            int size = gr.getSize();

            final String groupTitle = TranslationManager.getInstance().getTranslationForItem(gr.getId());

            if (gr.getLocalHint().isFoldable()) {
                final LightUIPanel childPanel = new LightUIPanel(gr.getId());
                childPanel.setTitle(groupTitle);
                childPanel.setFoldable(true);
                childPanel.setGridWidth(4);
                childPanel.setFillWidth(true);
                for (int i = 0; i < size; i++) {
                    this.append(childPanel, gr.getItem(i));
                }
                if (this.modifers.containsKey(gr.getId())) {
                    this.modifers.get(gr.getId()).process(childPanel);
                }
                final LightUILine line = new LightUILine();
                line.addChild(childPanel);
                panel.addChild(line);
            } else {
                if (groupTitle != null) {
                    final LightUILine titleLine = new LightUILine();
                    final LightUILabel titleLabel = new LightUILabel(gr.getId() + ".title.label");
                    titleLabel.setFontBold(true);
                    titleLabel.setLabel(groupTitle);
                    titleLabel.setFillWidth(true);
                    titleLine.addChild(titleLabel);
                    panel.addChild(titleLine);
                    final LightUILine line = new LightUILine();
                    panel.addChild(line);
                }
                for (int i = 0; i < size; i++) {
                    final Item it = gr.getItem(i);
                    this.append(panel, it);
                }
            }
        } else {
            final LayoutHints localHint = item.getLocalHint();
            LightUILine currentLine = panel.getLastLine();
            currentLine.setMarginTop(1);
            currentLine.setMarginBottom(1);
            if (localHint.isSeparated()) {
                if (currentLine.getWidth() > 0) {
                    currentLine = new LightUILine();
                    panel.addChild(currentLine);
                }
            }
            if (localHint.fillHeight()) {
                currentLine.setFillHeight(true);
            }

            if (localHint.largeHeight()) {
                currentLine.setWeightY(1);
            }

            if (currentLine.getWidth() >= this.maxColumnCount) {
                currentLine = new LightUILine();
                panel.addChild(currentLine);
            }

            final SQLField field = this.mapper.getSQLFieldForItem(item.getId());
            LightUILabel elementLabel = null;
            if (localHint.showLabel()) {
                currentLine.setElementPadding(5);

                elementLabel = new LightUILabel(item.getId() + ".label");
                elementLabel.setHorizontalAlignement(LightUIElement.HALIGN_RIGHT);
                String label = TranslationManager.getInstance().getTranslationForItem(item.getId());

                if (label == null && field != null) {
                    final RowItemDesc desc = this.configuration.getTranslator().getDescFor(field.getTable(), field.getName());
                    if (desc != null) {
                        label = desc.getLabel();
                    }
                }

                if (label == null) {
                    label = item.getId();
                    elementLabel.setBackgroundColor(Color.ORANGE);
                    elementLabel.setToolTip("No translation for " + item.getId());
                    Log.get().warning("No translation for " + item.getId());
                }

                elementLabel.setLabel(label);
                if (localHint.isSplit()) {
                    elementLabel.setHorizontalAlignement(LightUIElement.HALIGN_LEFT);
                    if (currentLine.getChildrenCount() != 0) {
                        currentLine = new LightUILine();
                        panel.addChild(currentLine);
                    }
                    elementLabel.setGridWidth(4);
                } else {
                    elementLabel.setGridWidth(1);
                }

                currentLine.addChild(elementLabel);
            }
            LightUIElement elementEditor = this.getCustomEditor(item.getId());
            if (elementEditor == null) {
                if (field != null) {
                    Class<?> javaType = field.getType().getJavaType();
                    if (field.isKey()) {
                        elementEditor = new LightUIComboBox(item.getId());
                        elementEditor.setMinInputSize(20);
                    } else if (javaType.equals(String.class)) {
                        if (field.getType().getSize() > 1000) {
                            elementEditor = new LightUITextArea(item.getId());
                            elementEditor.setValue("");
                            elementEditor.setMinInputSize(10);
                        } else {
                            elementEditor = new LightUITextField(item.getId());
                            elementEditor.setValue("");
                            elementEditor.setMinInputSize(10);
                        }
                    } else if (javaType.equals(Date.class)) {
                        elementEditor = new LightUIDate(item.getId());
                    } else if (javaType.equals(Boolean.class)) {
                        elementEditor = new LightUICheckBox(item.getId(), "");
                    } else if (javaType.equals(Timestamp.class)) {
                        elementEditor = new LightUIDate(item.getId());
                    } else if (javaType.equals(Integer.class)) {
                        elementEditor = new LightUITextField(item.getId());
                        elementEditor.setValueType(LightUIElement.VALUE_TYPE_INTEGER);
                    } else {
                        elementEditor = new LightUITextField(item.getId());
                        Log.get().warning("unsupported type " + javaType.getName());
                        elementEditor.setValue("unsupported type " + javaType.getName());
                    }
                } else {
                    elementEditor = new LightUITextField(item.getId());
                    elementEditor.setMinInputSize(10);
                    elementEditor.setToolTip("No field attached to " + item.getId());
                    Log.get().warning("No field attached to " + item.getId());
                    if (elementLabel != null) {
                        elementLabel.setBackgroundColor(Color.ORANGE);
                        elementLabel.setToolTip("No field attached to " + item.getId());
                    }
                }
            }

            if (elementEditor != null) {
                elementEditor.setWeightX(1);
                if (this.modifers.containsKey(item.getId())) {
                    this.modifers.get(item.getId()).process(elementEditor);
                }
            }
            if (localHint.isSplit()) {
                if (currentLine.getWidth() > 0) {
                    currentLine = new LightUILine();
                    panel.addChild(currentLine);
                }
            }

            if (localHint.isSplit()) {
                elementEditor.setGridWidth(4);
            } else if (localHint.largeWidth()) {

                if (localHint.showLabel()) {
                    elementEditor.setGridWidth(3);
                } else {
                    elementEditor.setGridWidth(4);
                }
            } else {
                elementEditor.setGridWidth(1);
            }
            elementEditor.setFillWidth(localHint.fillWidth());
            currentLine.addChild(elementEditor);

        }
    }

    private LightUIElement getCustomEditor(final String id) {
        final CustomEditorProvider customEditorProvider = this.customEditorProviders.get(id);
        if (customEditorProvider != null) {
            final LightUIElement element = customEditorProvider.createUIElement(id);
            if (element.getId() == null) {
                throw new IllegalStateException("Null id for custom editor for id: " + id);
            }
            return element;
        }
        return null;
    }

    public void putCustomEditorProvider(final String id, final CustomEditorProvider provider) {
        this.customEditorProviders.put(id, provider);
    }

    public void putAllCustomEditorProvider(final Map<String, CustomEditorProvider> map) {
        this.customEditorProviders.putAll(map);
    }

    public void addModifer(final String itemId, final ConvertorModifer modifer) {
        this.modifers.put(itemId, modifer);
    }

    public void addAllModifer(final Map<String, ConvertorModifer> modifers) {
        this.modifers.putAll(modifers);
    }
}
