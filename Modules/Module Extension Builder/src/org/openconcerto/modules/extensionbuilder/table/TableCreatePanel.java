package org.openconcerto.modules.extensionbuilder.table;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;

import org.openconcerto.modules.extensionbuilder.Extension;
import org.openconcerto.ui.DefaultGridBagConstraints;
import org.openconcerto.ui.DefaultListModel;
import org.openconcerto.ui.JLabelBold;
import org.openconcerto.utils.Tuple2;

public class TableCreatePanel extends JPanel {
    private final Extension extension;

    public TableCreatePanel(final TableDescritor desc, final Extension extension) {
        this.extension = extension;
        this.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        this.add(new JLabelBold("Table " + desc.getName()), c);
        c.weightx = 1;
        c.gridy++;
        final Tuple2<JPanel, GridBagConstraints> l = createEditorList(desc);
        this.add(l.get0(), c);
        c.gridy++;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.WEST;
        JButton buttonAdd = new JButton("Ajouter un champs");

        this.add(buttonAdd, c);
        c.gridy++;
        JPanel spacer = new JPanel();
        c.weighty = 1;
        this.add(spacer, c);
        buttonAdd.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                FieldDescriptor f = new FieldDescriptor(desc.getName(), "field" + ((l.get0().getComponentCount() / 2) + 1), "string", "", "200", "");
                GridBagConstraints c1 = l.get1();
                desc.add(f);
                addField(desc, l.get0(), c1, f);
                l.get0().revalidate();
                extension.setChanged();
            }
        });

    }

    Tuple2<JPanel, GridBagConstraints> createEditorList(final TableDescritor desc) {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        GridBagConstraints c = new DefaultGridBagConstraints();
        List<FieldDescriptor> fields = desc.getFields();
        for (final FieldDescriptor field : fields) {
            addField(desc, p, c, field);
        }
        Tuple2<JPanel, GridBagConstraints> result = Tuple2.create(p, c);
        return result;

    }

    private void addField(final TableDescritor desc, final JPanel p, GridBagConstraints c, final FieldDescriptor field) {

        c.weightx = 1;
        c.gridx = 0;
        final FieldDescriptorEditor editor = new FieldDescriptorEditor(extension, field);
        p.add(editor, c);

        c.gridx++;
        c.weightx = 0;
        final JButton close = new JButton(new ImageIcon(DefaultListModel.class.getResource("close_popup.png")));

        p.add(close, c);
        close.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                desc.remove(field);
                p.remove(editor);
                p.remove(close);
                p.revalidate();
                extension.setChanged();
            }
        });
        c.gridy++;
    }

}
