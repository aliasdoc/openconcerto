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
 
 /*
 * Created on 5 nov. 2004
 */
package org.openconcerto.sql.element;

import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.Log;
import org.openconcerto.sql.TM;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLFieldsSet;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowAccessor;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.SQLSelect.LockStrength;
import org.openconcerto.sql.model.SQLType;
import org.openconcerto.sql.request.MutableRowItemView;
import org.openconcerto.sql.request.RowItemDesc;
import org.openconcerto.sql.request.RowNotFound;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.sql.request.SQLForeignRowItemView;
import org.openconcerto.sql.request.SQLRowItemView;
import org.openconcerto.sql.request.SQLRowView;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLSearchableTextCombo;
import org.openconcerto.sql.sqlobject.SQLTextCombo;
import org.openconcerto.sql.sqlobject.itemview.SimpleRowItemView;
import org.openconcerto.sql.users.rights.UserRightsManager;
import org.openconcerto.ui.DisplayabilityListener;
import org.openconcerto.ui.FormLayouter;
import org.openconcerto.ui.JDate;
import org.openconcerto.ui.component.ComboLockedMode;
import org.openconcerto.ui.component.InteractionMode;
import org.openconcerto.ui.component.text.TextBehaviour;
import org.openconcerto.ui.component.text.TextComponentUtils;
import org.openconcerto.ui.coreanimation.Animator;
import org.openconcerto.ui.valuewrapper.BooleanValueWrapper;
import org.openconcerto.ui.valuewrapper.ValidatedValueWrapper;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.ui.valuewrapper.ValueWrapperFactory;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.DecimalUtils;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.ITransformer;
import org.openconcerto.utils.cc.Transformer;
import org.openconcerto.utils.checks.EmptyListener;
import org.openconcerto.utils.checks.EmptyObj;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidObject;
import org.openconcerto.utils.checks.ValidState;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.text.JTextComponent;

/**
 * Base class for SQLComponent. This implements the ValidObject by defining that a component is
 * valid when all its objects are valid and all its required objects are not empty : <br/>
 * <img src="doc-files/listeners.png"/>. Also this class uses SQLRowItemView to represent items of a
 * row.
 * 
 * @author ilm
 */
public abstract class BaseSQLComponent extends SQLComponent implements Scrollable, RIVPanel {
    protected static final String REQ = "required";
    protected static final String DEC = "notdecorated";
    protected static final String SEP = "noseparator";

    /**
     * Syntactic sugar for
     * {@link BaseSQLComponent#createSimpleRowItemView(String, Class, ITransformer)}.
     * 
     * @author Sylvain CUAZ
     * @param <T> type parameter
     */
    public static interface VWTransformer<T> extends ITransformer<ValueWrapper<? extends T>, ValueWrapper<? extends T>> {
    }

    private final SQLRowView requete;
    // whether the application (as opposed to us, the framework) allow the edition
    private final Map<String, Boolean> allowEditable;

    private final Set<SQLRowItemView> required;
    // contains the SQL name of required SQLRowItemView
    private final Set<String> requiredNames;

    // [ValidListener]
    private final List<ValidListener> listeners;

    private InteractionMode editable;
    private boolean alwaysEditable;
    private final Set<SQLField> hide;
    private FormLayouter additionalFieldsPanel;
    private boolean displayFieldName;

    public BaseSQLComponent(SQLElement element) {
        super(element);
        // Obligatoire pour L&F Nymbus
        this.setOpaque(true);

        // pouvoir prendre le focus
        // ATTN marche pas toujours : qd disabled, clic sur ITextArea ne remonte pas jusqu'à nous.
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                BaseSQLComponent.this.requestFocusInWindow();
            }
        });
        this.required = new HashSet<SQLRowItemView>();
        this.requiredNames = this.createRequiredNames();
        this.listeners = new ArrayList<ValidListener>();
        this.hide = new HashSet<SQLField>();
        this.editable = InteractionMode.READ_WRITE;
        this.setNonExistantEditable(false);
        this.requete = new SQLRowView(this.getElement());
        // enable or disable our views and forward event
        this.requete.addListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                updateChildrenEditable();
                firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
            }
        }, SQLComponent.READ_ONLY_PROP);
        this.allowEditable = new HashMap<String, Boolean>();
        this.displayFieldName = false;
    }

    private final SQLRowView getRequest() {
        return this.requete;
    }

    protected final SQLField getField(String field) {
        return this.getTable().getField(field);
    }

    // *** add*

    public Component addView(String field) {
        return this.addView(field, null);
    }

    public Component addView(String field, String spec) {
        return this.addViewJComponent(field, spec);
    }

    private Component addViewJComponent(String field, Object spec) {
        if (getElement().getPrivateElement(field) != null) {
            // private
            final SQLComponent comp = this.getElement().getPrivateElement(field).createDefaultComponent();

            // TODO add a callback so that RowItemView is notified when added to a SQLComponent
            // avoiding the 'if instanceof ElementSQLObject' in addInitedView()
            final SpecParser parser = SpecParser.create(spec);
            DefaultElementSQLObject dobj = new DefaultElementSQLObject(this, comp);
            dobj.setDecorated(parser.isDecorated());
            dobj.showSeparator(parser.showSeparator());
            return this.addView((MutableRowItemView) dobj, field, parser);
        } else {
            return this.addView(createRowItemView(getComp(field, Object.class)), field, spec);
        }
    }

    @SuppressWarnings("unchecked")
    static private <T, U> ValueWrapper<? extends T> castVW(final ValueWrapper<? extends U> vw, final Class<U> vwType, final Class<T> wantedType) {
        if (wantedType.isAssignableFrom(vwType))
            return (ValueWrapper<? extends T>) vw;
        else
            throw new IllegalArgumentException("Value wrapper isn't of the wanted type");
    }

    private <T> ValueWrapper<? extends T> getComp(final String field, final Class<T> wantedType) {
        if (getElement().getPrivateElement(field) != null)
            // we create a MutableRowItemView and need SpecParser
            throw new IllegalArgumentException("Private fields not supported");

        final ValueWrapper<? extends T> comp;
        final SQLType type = getField(field).getType();
        if (getField(field).isKey()) {
            // foreign
            comp = createSubValueWrapper(new ElementComboBox(), type, wantedType);
        } else {
            if (Boolean.class.isAssignableFrom(type.getJavaType())) {
                // TODO hack to view the focus (should try to paint around the button)
                final JCheckBox cb = new JCheckBox(" ");
                cb.setOpaque(false);
                final JPanel panel = new JPanel(new BorderLayout()) {
                    @Override
                    public void setEnabled(boolean enabled) {
                        super.setEnabled(enabled);
                        cb.setEnabled(enabled);
                    }
                };
                panel.add(cb, BorderLayout.LINE_START);
                comp = addValidatedValueWrapper(castVW(new BooleanValueWrapper(panel, cb), Boolean.class, wantedType), type);
            } else if (Date.class.isAssignableFrom(type.getJavaType())) {
                comp = createSubValueWrapper(new JDate(), type, wantedType);
            } else if (String.class.isAssignableFrom(type.getJavaType()) && type.getSize() >= 512) {
                comp = createSubValueWrapper(new SQLSearchableTextCombo(ComboLockedMode.UNLOCKED, true), type, wantedType);
            } else {
                // regular
                comp = createSubValueWrapper(new SQLTextCombo(), type, wantedType);
            }
        }
        comp.getComp().setOpaque(false);
        return comp;
    }

    public final void addSQLObject(JComponent obj, String field) {
        this.addSQLObject(obj, field, null);
    }

    public Component addSQLObject(JComponent obj, String field, Object spec) {
        return this.addView(obj, field, spec);
    }

    public void addRequiredSQLObject(JComponent obj, String field, Object spec) {
        this.addSQLObject(obj, field, REQ + ";" + spec);
    }

    public void addRequiredSQLObject(JComponent obj, String field) {
        this.addSQLObject(obj, field, REQ);
    }

    public Component addView(JComponent comp, String fields) {
        return this.addView(comp, fields, null);
    }

    public Component addView(JComponent comp, String fields, Object specObj) {
        final MutableRowItemView rowItemView;
        if (comp instanceof MutableRowItemView) {
            rowItemView = (MutableRowItemView) comp;
        } else {
            final SQLField field = this.getField(SQLRow.toList(fields).get(0));
            rowItemView = createRowItemView(comp, field);
        }
        return this.addView(rowItemView, fields, specObj);
    }

    static public SimpleRowItemView<?> createRowItemView(JComponent comp, final SQLField field) {
        if (comp == null)
            throw new NullPointerException("comp for " + field + " is null");
        if (comp instanceof MutableRowItemView)
            throw new IllegalStateException("Comp is a MutableRowItemView, creating a SimpleRowItemView would ignore its methods : " + comp);
        return createRowItemView(createSubValueWrapper(comp, field.getType(), Object.class));
    }

    // just to make javac happy (type parameter for SimpleRowItemView)
    static private <T> SimpleRowItemView<T> createRowItemView(ValueWrapper<T> vw) {
        if (vw instanceof MutableRowItemView)
            throw new IllegalStateException("Comp is a MutableRowItemView, creating a SimpleRowItemView would ignore its methods : " + vw);
        return new SimpleRowItemView<T>(vw);
    }

    // useful when the exact type of field is known (e.g. String) and setValue() is needed
    static public final <T> ValueWrapper<T> createValueWrapper(JComponent comp, final SQLType type, final Class<T> wantedType) {
        // equality allow returned ValueWrapper to both produce and consume T
        if (!wantedType.equals(type.getJavaType()))
            throw new ClassCastException("wanted type " + wantedType + " is not " + type);
        final ValueWrapper<T> res = ValueWrapperFactory.create(comp, wantedType);
        return addValidatedValueWrapper(res, type);
    }

    // useful when the exact type of field isn't known (e.g. Number) and only getValue() is needed
    static public final <T> ValueWrapper<? extends T> createSubValueWrapper(JComponent comp, final SQLType type, final Class<T> wantedSuperType) {
        final Class<?> fieldClass = type.getJavaType();
        final ValueWrapper<? extends T> res = ValueWrapperFactory.create(comp, fieldClass.asSubclass(wantedSuperType));
        return addValidatedValueWrapper(res, type);
    }

    static private <T> ValueWrapper<T> addValidatedValueWrapper(ValueWrapper<T> res, final SQLType type) {
        final Class<?> fieldClass = type.getJavaType();
        if (String.class.isAssignableFrom(fieldClass)) {
            res = ValidatedValueWrapper.add(res, new ITransformer<T, ValidState>() {
                @Override
                public ValidState transformChecked(T t) {
                    final String s = (String) t;
                    final boolean ok = s == null || s.length() <= type.getSize();
                    // only compute string if needed
                    return ok ? ValidState.getTrueInstance() : ValidState.create(ok, TM.tr("sqlComp.stringValueTooLong", s.length() - type.getSize()));
                }
            });
            // other numeric SQL types are fixed size like their java counterparts
        } else if (BigDecimal.class.isAssignableFrom(fieldClass)) {
            final Integer decimalDigits = type.getDecimalDigits();
            final int intDigits = type.getSize() - decimalDigits;
            final String reason = TM.tr("sqlComp.bdTooHigh", intDigits, decimalDigits);
            res = ValidatedValueWrapper.add(res, new ITransformer<T, ValidState>() {
                @Override
                public ValidState transformChecked(T t) {
                    final BigDecimal bd = (BigDecimal) t;
                    // round first to get the correct number of integer digits, see
                    // http://www.postgresql.org/docs/8.4/interactive/datatype-numeric.html
                    return ValidState.create(bd == null || DecimalUtils.intDigits(DecimalUtils.round(bd, decimalDigits)) <= intDigits, reason);
                }
            });
        }
        return res;
    }

    public final <T> SimpleRowItemView<? extends T> createSimpleRowItemView(String fields, Class<T> clazz) {
        return this.createSimpleRowItemView(fields, clazz, Transformer.<ValueWrapper<? extends T>> nopTransformer());
    }

    /**
     * Create and initialize a SimpleRowItemView.
     * 
     * @param <T> type of field.
     * @param field field name.
     * @param clazz java type for the field.
     * @param init to initialize the value wrapper.
     * @return the created row item view.
     */
    public final <T> SimpleRowItemView<? extends T> createSimpleRowItemView(String field, Class<T> clazz, final ITransformer<? super ValueWrapper<? extends T>, ValueWrapper<? extends T>> init) {
        final ValueWrapper<? extends T> vw = this.getComp(field, clazz);
        if (vw instanceof MutableRowItemView)
            throw new IllegalStateException("Comp is a MutableRowItemView, creating a SimpleRowItemView would ignore its methods : " + vw);
        return initRIV(createRowItemView(init.transformChecked(vw)), field);
    }

    public Component addView(MutableRowItemView rowItemView, String fields, Object specObj) {
        return this.addInitedView(initRIV(rowItemView, fields), specObj);
    }

    private final <R extends MutableRowItemView> R initRIV(R rowItemView, String fields) {
        final List<String> fieldListS = SQLRow.toList(fields);
        final Set<SQLField> fieldList = new LinkedHashSet<SQLField>(fieldListS.size());
        for (final String fieldName : fieldListS) {
            fieldList.add(this.getField(fieldName));
        }

        // sqlName
        final String sqlName = fields;
        rowItemView.init(sqlName, fieldList);
        return rowItemView;
    }

    public Component addInitedView(SQLRowItemView v, Object specObj) {
        // if (obj == null)
        // throw new IllegalArgumentException("obj is null");

        final Spec spec = SpecParser.create(specObj);

        // ParentForeignField is always required
        final Set<String> reqNames = this.getRequiredNames();
        if (spec.isRequired() || v.getFields().contains(getElement().getParentForeignField()) || reqNames == null || reqNames.contains(v.getSQLName())) {
            this.required.add(v);
            if (v instanceof ElementSQLObject)
                ((ElementSQLObject) v).setRequired(true);
            if (reqNames != null)
                this.requiredNames.add(v.getSQLName());
        }
        this.getRequest().add(v);

        if (v instanceof SQLComponentItem) {
            ((SQLComponentItem) v).added(this, v);
        }
        if (v.getComp() instanceof SQLComponentItem) {
            ((SQLComponentItem) v.getComp()).added(this, v);
        }
        // reset just before adding to the UI :
        // - avoid updating value while displayed
        // - but still wait the longest to be sure the RIV is initialized (SQLComponentItem)
        v.resetValue();

        if (!CollectionUtils.containsAny(this.hide, v.getFields())) {
            if (spec.isAdditional()) {
                if (this.additionalFieldsPanel == null)
                    Log.get().warning("No additionalFieldsPanel for " + v.getFields() + " : " + v);
                else
                    this.additionalFieldsPanel.add(getDesc(v), v.getComp());
            } else {
                this.addToUI(v, spec.getWhere());
            }
        }

        final JTextComponent textComp = TextComponentUtils.getTextComp(v.getComp());
        if (textComp != null)
            TextBehaviour.manage(textComp);

        return v.getComp();
    }

    private boolean dontEdit(SQLRowItemView v) {
        final Set<String> fieldsNames = new SQLFieldsSet(v.getFields()).getFieldsNames(getTable());
        return this.getMode() == Mode.READ_ONLY || CollectionUtils.containsAny(this.getElement().getReadOnlyFields(), fieldsNames)
                || (this.getMode() != Mode.INSERTION && CollectionUtils.containsAny(this.getElement().getInsertOnlyFields(), fieldsNames));
    }

    protected final void inited() {
        super.inited();
        if (!(this instanceof GroupSQLComponent)) {

            for (final Entry<String, JComponent> e : this.getElement().getAdditionalFields().entrySet()) {
                final SpecParser spec = new SpecParser(null, true);
                final JComponent comp = e.getValue();
                if (comp == null)
                    // infer component
                    this.addViewJComponent(e.getKey(), spec);
                else
                    this.addView(comp, e.getKey(), spec);
            }
        }
        // assure that added views are consistent with our editable status
        this.updateChildrenEditable();
        for (final SQLRowItemView v : this.getRequest().getViews()) {
            v.addEmptyListener(new EmptyListener() {
                public void emptyChange(EmptyObj src, boolean newValue) {
                    emptyOrValidChanged((SQLRowItemView) src);
                }
            });
            v.addValidListener(new ValidListener() {
                public void validChange(ValidObject src, ValidState newValue) {
                    emptyOrValidChanged((SQLRowItemView) src);
                }
            });
            // initial status
            updateAnimate(v);
        }
        this.addHierarchyListener(new DisplayabilityListener() {
            @Override
            protected void displayabilityChanged(Component c) {
                getRequest().activate(c.isDisplayable());
            }
        });
        getRequest().activate(this.isDisplayable());
        this.fireValidChange();
        this.initDone();
    }

    protected void initDone() {

    }

    private void updateAnimate(final SQLRowItemView v) {
        if (v.getComp() != null)
            Animator.getInstance().animate(v.getComp(), !isItemViewValid(v));
    }

    protected void emptyOrValidChanged(SQLRowItemView v) {
        this.fireValidChange();
        updateAnimate(v);
    }

    protected void addToUI(SQLRowItemView v, String where) {
        // implement it to do nothing since subclass may choose not to use it
    }

    protected final void setAdditionalFieldsPanel(FormLayouter panel) {
        this.additionalFieldsPanel = panel;
    }

    public final void setViewsOrder(final Collection<String> names) {
        this.getRequest().setViewsOrder(names);
    }

    public final SQLRowItemView getView(String name) {
        return this.getRequest().getView(name);
    }

    protected final SQLRowItemView getView(Component comp) {
        return this.getRequest().getView(comp);
    }

    public final Map<String, SQLRowItemView> getViews() {
        return this.getRequest().getViewsMap();
    }

    /**
     * Return each view that has at least one of {@link SQLRowItemView#getFields() its fields} in
     * the passed set.
     * 
     * @param fields the fields to search for.
     * @return all views that contains <code>fields</code>.
     */
    public final List<SQLRowItemView> getViews(final Set<SQLField> fields) {
        final List<SQLRowItemView> res = new ArrayList<SQLRowItemView>();
        for (final SQLRowItemView v : this.getRequest().getViews()) {
            if (!Collections.disjoint(fields, v.getFields())) {
                res.add(v);
            }
        }
        return res;
    }

    protected final SQLForeignRowItemView getForeignView(SQLRowItemView v) {
        if (v instanceof SQLForeignRowItemView)
            return (SQLForeignRowItemView) v;
        else if (v.getComp() instanceof SQLForeignRowItemView)
            return (SQLForeignRowItemView) v.getComp();
        else
            throw new IllegalArgumentException("no SQLForeignRowItemView found for " + v);
    }

    public void addValidListener(ValidListener l) {
        this.listeners.add(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.listeners.remove(l);
    }

    protected synchronized final void fireValidChange() {
        // ATTN called very often during a select() (for each SQLObject empty & value change)
        final ValidState validated = this.getValidState();
        for (final ValidListener l : this.listeners) {
            l.validChange(this, validated);
        }
    }

    private boolean isItemViewValid(final SQLRowItemView v) {
        return v.getValidState().isValid() && !(this.getRequired().contains(v) && v.isEmpty());
    }

    @Override
    public synchronized ValidState getValidState() {
        boolean res = true;
        final List<String> pbs = new ArrayList<String>();
        // tous nos objets sont valides ?
        for (final SQLRowItemView obj : this.getRequest().getViews()) {
            final ValidState state = obj.getValidState();
            if (!state.isValid()) {
                final String txt = state.getValidationText();
                final String explanation = TM.tr("sqlComp.invalidItem", "'" + getDesc(obj) + "'", txt != null ? 1 : 0, txt);
                pbs.add(explanation);
                res = false;
                // ne regarder si vide que pour les valides (souvent les non-valides sont vides car
                // il ne peuvent renvoyer de valeur)
            } else if (this.getRequired().contains(obj) && obj.isEmpty()) {
                pbs.add(TM.tr("sqlComp.emptyItem", "'" + getDesc(obj) + "'"));
                res = false;
            }
        }
        return ValidState.create(res, CollectionUtils.join(pbs, "\n"));
    }

    protected final String getDesc(final SQLRowItemView obj) {
        return getLabelFor(obj.getSQLName());
    }

    public final String getLabelFor(String itemName) {
        return getDesc(itemName, getRIVDesc(itemName)).get0();
    }

    // not public since desc could be different from getRIVDesc(itemName)
    protected final Tuple2<String, Boolean> getDesc(final String itemName, final RowItemDesc desc) {
        final boolean emptyLabel = desc.getLabel() == null || desc.getLabel().trim().length() == 0;
        final String l;
        if (emptyLabel) {
            l = itemName;
        } else {
            l = getLabel(itemName, desc) + (this.displayFieldName ? " [" + itemName + "]" : "");
        }
        return Tuple2.create(l, !emptyLabel);
    }

    protected final void toggleDisplayFieldsNames() {
        this.displayFieldName = !this.displayFieldName;
        this.updateUIAll();
    }

    protected String getLabel(final String itemName, final RowItemDesc desc) {
        return desc.getLabel();
    }

    /**
     * Allow the views of this component to be edited. For this to be editable other framework
     * predicates must be <code>true</code> (e.g. {@link #isSelectionReadOnly()}).
     * 
     * @param mode how the application allow the views to be edited.
     * @return <code>true</code> if the mode was changed.
     */
    @Override
    public final boolean setEditable(InteractionMode mode) {
        final boolean changed = mode != this.editable;
        if (changed) {
            this.editable = mode;
            this.interactionModeChanged(mode);
        }
        return changed;
    }

    protected void interactionModeChanged(InteractionMode mode) {
        this.updateChildrenEditable();
    }

    private final void updateChildrenEditable() {
        for (final SQLRowItemView o : this.getRequest().getViews()) {
            updateEditable(o);
        }
    }

    public final InteractionMode getEditable() {
        return this.editable;
    }

    public void allowEditable(final String name, final boolean b) {
        final SQLRowItemView view = this.getView(name);
        if (view == null)
            throw new IllegalArgumentException("No view named " + name);
        this.allowEditable(view, b);
    }

    /**
     * Allow the passed view to be edited. For the view to be editable other framework predicates
     * must allow it (e.g. {@link #getEditable()}).
     * 
     * @param view the view.
     * @param b <code>true</code> if the application allow the view to be edited.
     */
    public void allowEditable(final SQLRowItemView view, final boolean b) {
        if (view == null)
            throw new NullPointerException();
        this.allowEditable.put(view.getSQLName(), b);
        updateEditable(view);
    }

    @Override
    protected void modeChanged() {
        super.modeChanged();
        this.updateChildrenEditable();
    }

    private void updateEditable(final SQLRowItemView o) {
        // a view can only be editable if its parent is too and if the selected row is
        // if nonExistantEditable : always editable, otherwise id must exist
        // a view can be disabled for other arbitrary application (non framework) reasons
        final InteractionMode mode;
        if (this.getEditable() != InteractionMode.READ_WRITE) {
            mode = this.getEditable();
        } else {
            final boolean editable = !this.dontEdit(o) && !this.isSelectionReadOnly() && (this.isNonExistantEditable() || this.getSelectedID() != SQLRow.NONEXISTANT_ID)
                    && this.allowEditable.get(o.getSQLName()) != Boolean.FALSE;
            mode = editable ? InteractionMode.READ_WRITE : InteractionMode.READ_ONLY;
        }
        o.setEditable(mode);
    }

    // final : overload select() if need be
    public final void resetValue() {
        this.select(null);
    }

    public final void partialReset() {
        final Set<String> names = this.getPartialResetNames();
        if (names == null || names.size() > 0)
            this.select(null, names);
    }

    public final int insert() {
        return this.insert(null);
    }

    public int insert(SQLRow order) {
        try {
            if (!UserRightsManager.getCurrentUserRights().canAdd(getTable()))
                throw new SQLException("forbidden");
            if (order == null)
                return this.getRequest().insert().getID();
            else
                return this.getRequest().insert(order).getID();
        } catch (SQLException e) {
            ExceptionHandler.handle(this, TM.tr("sqlComp.insertError"), e);
            return -1;
        }
    }

    /**
     * Return the last known values of the {@link #getSelectedID() selection} in the DB.
     * 
     * @return the last known values.
     */
    @Override
    public final SQLRowValues getLastKnownDBVals() {
        return this.getRequest().getLastKnownDBVals();
    }

    @Override
    public final boolean updateLastKnownDBVals(final LockStrength ls) {
        final SQLRowValues current = this.getLastKnownDBVals();
        if (current == null)
            throw new IllegalStateException("No values to update");
        return this.getRequest().setLastKnownDBVals(current, this.getRequest().fetchRow(current.getID(), ls));
    }


    @Override
    public final void select(int id) {
        // TODO in an executor outside of the EDT
        this.select(this.getRequest().fetchRow(id, LockStrength.NONE));
    }

    @Override
    public void select(SQLRowAccessor r) {
        this.select(r, null);
    }

    public void select(SQLRowAccessor r, Set<String> views) {
        // FIXME force loading
        if (r instanceof SQLRow)
            ((SQLRow) r).exists();
        try {
            // allow null to pass, ease some code (eg new ListOfSomething().getTable() even if we
            // can't see the table)
            if (r != null && !UserRightsManager.getCurrentUserRights().canView(getTable()))
                throw new IllegalStateException("forbidden");
            // MAYBE prevent repaints of each SQLObject with
            // this.setIgnoreRepaint(true) or RepaintManager
            this.getRequest().select(r, views);
            this.updateChildrenEditable();

            // don't put defaults if non-editable (eg bottom part of ListeModifyPanel)
            if (r == null && this.isNonExistantEditable()) {
                // selectDefaults() after select() so that we have the final word
                // eg default DESIGNATION of OBS is "ok", but default DESIGNATION of TRANSFO.ID_OBS
                // is "good".
                this.selectDefaults(views);
            }
        } catch (RowNotFound e) {
            // l'id demandé n'existe pas : prévenir tout le monde
            this.getTable().fireRowDeleted(e.getRow().getID());
            ExceptionHandler.handle(this, TM.tr("sqlComp.deletedRow", e.getRow()), e);
        } catch (IllegalStateException e) {
            ExceptionHandler.handle(this, TM.tr("sqlComp.selectError", r), e);
        }
    }

    // private since it only changes views having default values : use #resetValue()
    private final void selectDefaults(Set<String> views) {
        final SQLRowValues defaults = this.createValidDefaults();
        if (defaults != null && defaults.getFields().size() > 0)
            this.getRequest().select(defaults, views);
    }

    public final void addFillingListener(final PropertyChangeListener l) {
        this.getRequest().addListener(l, "filling");
    }

    public final void rmFillingListener(final PropertyChangeListener l) {
        this.getRequest().rmListener(l, "filling");
    }

    public final void addSelectionListener(final PropertyChangeListener l) {
        this.getRequest().addListener(l, "selectedID");
    }

    public final void rmSelectionListener(final PropertyChangeListener l) {
        this.getRequest().rmListener(l, "selectedID");
    }

    @Override
    public void detach() {
        this.getRequest().detach();
    }

    /**
     * Are we filling our objects.
     * 
     * @return <code>true</code> if we're filling in values from the DB.
     */
    public final boolean isFilling() {
        return this.getRequest().isFilling();
    }

    public int getSelectedID() {
        assert (SwingUtilities.isEventDispatchThread());
        return this.getRequest().getSelectedID();
    }

    @Override
    public final boolean isSelectionReadOnly() {
        return this.getRequest().isReadOnlySelection();
    }

    public void update() {
        try {
            if (!UserRightsManager.getCurrentUserRights().canModify(getTable()))
                throw new SQLException("forbidden");
            this.getRequest().update();
        } catch (SQLException e) {
            ExceptionHandler.handle(this, TM.tr("sqlComp.updateError"), e);
        }
    }

    @Override
    public void archive() {
        try {
            if (!UserRightsManager.getCurrentUserRights().canDelete(getTable()))
                throw new SQLException("forbidden");
            if (this.isSelectionReadOnly())
                throw new SQLException("read only");
            // MAYBE for performance (avoid searching for references to cut):
            // if (this.isPrivate()) {
            // ((BaseSQLComponent) this.getSQLParent().getSQLParent()).getSelectedID();
            // this.getElement().archivePrivate(this.getSelectedID(), parentID);
            // } else
            this.getElement().archive(this.getSelectedID());
            this.resetValue();
        } catch (SQLException e) {
            ExceptionHandler.handle(this, TM.tr("sqlComp.archiveError", this), e);
        }
    }

    /**
     * The SQL names that are always reset after an insert. This implementation returns an empty set
     * : the component is only reset on {@link #getResetMode() visibility change}.
     * 
     * @return the names to reset, <code>null</code> meaning all.
     */
    public Set<String> getPartialResetNames() {
        return Collections.emptySet();
    }

    // ** required

    protected final Set<SQLRowItemView> getRequired() {
        return this.required;
    }

    /**
     * The SQL names that are required.
     * 
     * @return the required SQL names, <code>null</code> meaning all of them.
     */
    protected final Set<String> getRequiredNames() {
        return this.requiredNames == null ? null : Collections.unmodifiableSet(this.requiredNames);
    }

    /**
     * The sql names that are required. This implementation includes "DESIGNATION".
     * 
     * @return the required sql names, <code>null</code> meaning all of them.
     */
    protected Set<String> createRequiredNames() {
        return new HashSet<String>(Collections.singleton("DESIGNATION"));
    }

    public String toString() {
        return this.getClass() + " on " + this.getTable() + " " + this.getSelectedID();
    }

    public final boolean isNonExistantEditable() {
        return this.alwaysEditable;
    }

    public final void setNonExistantEditable(boolean alwaysEditable) {
        this.alwaysEditable = alwaysEditable;
    }

    public final RowItemDesc getRIVDesc(String field) {
        final Configuration conf = Configuration.getInstance();
        if (conf == null)
            return SQLFieldTranslator.NULL_DESC;
        else
            return conf.getTranslator().getDescFor(this.getTable(), getCode(), getElement().getMDPath(), field);
    }

    public final void setRIVDesc(String itemName, RowItemDesc desc) {
        try {
            Configuration.getTranslator(this.getTable()).storeDescFor(this.getTable(), getCode(), itemName, desc);
            updateUI(itemName, desc);
        } catch (SQLException e) {
            ExceptionHandler.handle(this, TM.tr("sqlComp.saveDocError", itemName), e);
        }
    }

    protected void updateUI(final String itemName, final RowItemDesc desc) {
    }

    protected final void updateUI(final String itemName, final JComponent label, final RowItemDesc desc) {
        updateUI(itemName, label, desc, null);
    }

    protected final void updateUI(final String itemName, final JComponent label, final RowItemDesc desc, final Color emptyLabelColor) {
        label.setToolTipText(desc.getDocumentation().trim().length() == 0 ? null : desc.getDocumentation());
        final Tuple2<String, Boolean> tuple = getDesc(itemName, desc);
        final String s = tuple.get0();
        if (label instanceof JLabel) {
            ((JLabel) label).setText(s);
        } else if (label instanceof JTextComponent) {
            ((JTextComponent) label).setText(s);
        } else if (label.getBorder() instanceof TitledBorder) {
            ((TitledBorder) label.getBorder()).setTitle(s);
        } else {
            Log.get().warning("Couldn't change label for " + itemName);
        }
        if (emptyLabelColor != null && !tuple.get1())
            label.setForeground(emptyLabelColor);
        label.repaint();
    }

    protected final void updateUIAll() {
        for (final SQLRowItemView v : this.getRequest().getViews()) {
            final String sqlName = v.getSQLName();
            updateUI(sqlName, getRIVDesc(sqlName));
        }
    }

    public void doNotShow(SQLField f) {
        this.hide.add(f);
    }

    private static interface Spec {

        boolean isRequired();

        String getWhere();

        boolean isAdditional();
    }

    private static final class SpecParser implements Spec {

        static public SpecParser create(Object specObj) {
            final SpecParser spec;
            if (specObj == null || specObj instanceof String) {
                spec = new SpecParser((String) specObj);
            } else {
                spec = (SpecParser) specObj;
            }
            return spec;
        }

        private boolean isRequired;
        private String where;
        private boolean showSeparator = true;
        private boolean isDecorated = true;
        private final boolean isAdditional;

        public SpecParser(String spec) {
            this(spec, false);
        }

        public SpecParser(String spec, final boolean isAdditional) {
            this.isAdditional = isAdditional;
            // empty string treated as null
            if (spec == null || spec.length() == 0) {
                this.isRequired = false;
                this.where = null;
            } else {
                final String[] specs = spec.split(";");
                if (specs.length > 4)
                    throw new IllegalArgumentException(spec);
                for (int i = 0; i < specs.length; i++) {
                    final String sp = specs[i];
                    if (sp.equals(REQ)) {
                        this.isRequired = true;
                    } else if (sp.equals(DEC)) {
                        this.isDecorated = false;
                    } else if (sp.equals(SEP)) {
                        this.showSeparator = false;
                    } else {
                        this.where = sp;
                    }
                }
            }
        }

        public boolean showSeparator() {
            return this.showSeparator;
        }

        public boolean isDecorated() {
            return this.isDecorated;
        }

        @Override
        public final boolean isRequired() {
            return this.isRequired;
        }

        @Override
        public final String getWhere() {
            return this.where;
        }

        @Override
        public final boolean isAdditional() {
            return this.isAdditional;
        }
    }

    @Override
    public SQLElementDirectory getDirectory() {
        return this.getElement().getDirectory();
    }

    @Override
    public SQLComponent getSQLComponent() {
        return this;
    }

    // *** scrollable

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return this.getPreferredSize();
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        if (getParent() instanceof JViewport) {
            // since getPreferredScrollableViewportSize() returns getPreferredSize() this component
            // will be shown by default at its preferred size, but if the viewport's height is
            // reduced this component will first shrink to its minimum size, and then below that
            // scrollbars will be displayed.
            return (((JViewport) getParent()).getHeight() >= getMinimumSize().height);
        }
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        // no horizontal scroll, too much pb with JTextArea when setLineWrap(true) : its minimum
        // width grows but never shrinks
        return true;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        // difficult to tell because of the textAreas and the private components
        return 15;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        final double length = orientation == SwingConstants.VERTICAL ? visibleRect.getHeight() : visibleRect.getWidth();
        // keep some common area
        return (int) (length - 30);
    }

}
