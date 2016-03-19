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

import java.awt.Color;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.openconcerto.sql.Log;
import org.openconcerto.sql.PropsConfiguration;
import org.openconcerto.sql.model.FieldMapper;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.ui.group.Group;
import org.openconcerto.ui.group.Item;
import org.openconcerto.ui.group.LayoutHints;
import org.openconcerto.ui.light.CustomEditorProvider;
import org.openconcerto.ui.light.LightUICheckBox;
import org.openconcerto.ui.light.LightUICombo;
import org.openconcerto.ui.light.LightUIDate;
import org.openconcerto.ui.light.LightUIElement;
import org.openconcerto.ui.light.LightUIFrame;
import org.openconcerto.ui.light.LightUILabel;
import org.openconcerto.ui.light.LightUILine;
import org.openconcerto.ui.light.LightUIPanel;
import org.openconcerto.ui.light.LightUITextField;
import org.openconcerto.utils.i18n.TranslationManager;

public class GroupToLightUIConvertor {
    private final int maxColumnCount;
    private PropsConfiguration configuration;
    private Map<String, CustomEditorProvider> customEditorProviders = new HashMap<String, CustomEditorProvider>();
    private Map<String, ConvertorModifer> modifers = new HashMap<String, ConvertorModifer>();

    public GroupToLightUIConvertor(PropsConfiguration conf) {
        this(conf, 4);
    }

    public GroupToLightUIConvertor(PropsConfiguration conf, int columns) {
        this.maxColumnCount = columns;
        this.configuration = conf;
    }

    public LightUIFrame convert(Group group) {
        final LightUIFrame frame = new LightUIFrame(group.getId());
        final LightUIPanel mainPanel = new LightUIPanel(frame.getId() + ".panel");
        append(mainPanel, group);
        frame.setMainPanel(mainPanel);
        final String frameTitle = TranslationManager.getInstance().getTranslationForItem(group.getId());
        frame.setTitle(frameTitle);
        return frame;
    }

    private void append(LightUIPanel panel, Item item) {
        if (item instanceof Group) {
            final Group gr = (Group) item;
            int size = gr.getSize();
            
            if(gr.getLocalHint().isFoldable()) {
                panel.setFoldable(true);
                String title = TranslationManager.getInstance().getTranslationForItem(item.getId());
                if (title == null) {
                    title = item.getId();
                    Log.get().warning("No translation for " + item.getId());
                }
                panel.setTitle(title);
            }
            
            for (int i = 0; i < size; i++) {
                final Item it = gr.getItem(i);
                if(it instanceof Group) {
                    final LightUIPanel childPanel = new LightUIPanel(it.getId());
                    this.append(childPanel, it);
                    final LightUILine currentLine = new LightUILine();
                    currentLine.add(childPanel);
                    panel.addLine(currentLine);
                } else {
                    append(panel, it);
                }
            }
            
            if(this.modifers.containsKey(gr.getId())) {
                this.modifers.get(gr.getId()).process(panel);
            }
        } else {
            final LayoutHints localHint = item.getLocalHint();
            LightUILine currentLine = panel.getLastLine();
            if (localHint.isSeparated()) {
                if (currentLine.getWidth() > 0) {
                    currentLine = new LightUILine();
                    panel.addLine(currentLine);
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
                panel.addLine(currentLine);
            }
            LightUILabel elementLabel = null;
            if (localHint.showLabel()) {
                elementLabel = new LightUILabel(item.getId());
                String label = TranslationManager.getInstance().getTranslationForItem(item.getId());
                if (label == null) {
                    label = item.getId();
                    elementLabel.setBackgroundColor(Color.ORANGE);
                    elementLabel.setToolTip("No translation for " + item.getId());
                    Log.get().warning("No translation for " + item.getId());
                }

                elementLabel.setLabel(label);
                if (localHint.isSplit()) {
                    elementLabel.setGridWidth(4);
                } else {
                    elementLabel.setGridWidth(1);
                }

                currentLine.add(elementLabel);
            }
            LightUIElement elementEditor = this.getCustomEditor(item.getId());
            if (elementEditor == null) {
                FieldMapper fieldMapper = this.configuration.getFieldMapper();
                if (fieldMapper == null) {
                    throw new IllegalStateException("null field mapper");
                }

                final SQLField field = fieldMapper.getSQLFieldForItem(item.getId());
                
                if (field != null) {
                    Class<?> javaType = field.getType().getJavaType();
                    if (field.isKey()) {
                        elementEditor = new LightUICombo(item.getId());
                        elementEditor.setMinInputSize(20);
                    } else if (javaType.equals(String.class)) {
                        elementEditor = new LightUITextField(item.getId());
                        elementEditor.setValue("");
                        elementEditor.setMinInputSize(10);
                    } else if (javaType.equals(Date.class)) {
                        elementEditor = new LightUIDate(item.getId());
                    } else if(javaType.equals(Boolean.class)) {
                        elementEditor = new LightUICheckBox(item.getId(), "");
                    } else if(javaType.equals(Timestamp.class)) {
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
            
            if(elementEditor != null) {
                if(this.modifers.containsKey(item.getId())) {
                    this.modifers.get(item.getId()).process(elementEditor);
                }
            }
            if (localHint.isSplit()) {
                if (currentLine.getWidth() > 0) {
                    currentLine = new LightUILine();
                    panel.addLine(currentLine);
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
            currentLine.add(elementEditor);

        }
    }

    private LightUIElement getCustomEditor(String id) {
        final CustomEditorProvider customEditorProvider = this.customEditorProviders.get(id);
        if (customEditorProvider != null) {
            LightUIElement element = customEditorProvider.createUIElement(id);
            if (element.getId() == null) {
                throw new IllegalStateException("Null id for custom editor for id: " + id);
            }
            return element;
        }
        return null;
    }

    public void setCustomEditorProvider(String id, CustomEditorProvider provider) {
        this.customEditorProviders.put(id, provider);
    }
    
    public void addModifer(final String itemId, final ConvertorModifer modifer) {
        this.modifers.put(itemId, modifer);
    }
}
