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
 
 package org.openconcerto.ui;

import org.openconcerto.ui.component.text.TextComponent;
import org.openconcerto.ui.valuewrapper.ValueWrapper;
import org.openconcerto.utils.TimeUtils;
import org.openconcerto.utils.checks.ValidChangeSupport;
import org.openconcerto.utils.checks.ValidListener;
import org.openconcerto.utils.checks.ValidState;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.text.DateFormatter;
import javax.swing.text.JTextComponent;

/**
 * Allow to edit a time. The day of the returned date value is always the epoch, but
 * {@link #setValue(Date)} accepts any day.
 * 
 * @author Sylvain CUAZ
 */
public final class JTime extends JPanel implements ValueWrapper<Date>, TextComponent {

    // no need to synchronize, always used in the EDT
    private static final Calendar CAL = Calendar.getInstance();
    private static final long DAY_LENGTH = 24 * 60 * 60 * 1000;

    static private final Date dateFromTimeInMillis(final Long val) {
        if (val == null) {
            return null;
        } else {
            return dateFromTimeInMillis(val.intValue());
        }
    }

    /**
     * The epoch <i>day</i> at a specific time in the default calendar.
     * 
     * @param val a number of ms, e.g. for 8:30 this would be 8.5*60*60*1000.
     * @return the date representing the epoch day at the passed time, e.g. 08:30:00 CET 1970.
     */
    static private final Date dateFromTimeInMillis(final int val) {
        if (val >= DAY_LENGTH)
            throw new IllegalArgumentException("Val is greater than a day : " + val);
        JTime.CAL.clear();
        JTime.CAL.add(Calendar.MILLISECOND, val);
        return JTime.CAL.getTime();
    }

    private final boolean fillWithCurrentHour;
    private final JFormattedTextField text;
    private final ValidChangeSupport validSupp;

    /**
     * Create the component, empty.
     */
    public JTime() {
        this(false);
    }

    /**
     * Create the component.
     * 
     * @param fillWithCurrentHour <code>true</code> if this should be filled with the current hour,
     *        else empty.
     */
    public JTime(final boolean fillWithCurrentHour) {
        this(fillWithCurrentHour, false);
    }

    public JTime(final boolean fillWithCurrentHour, final boolean withSeconds) {
        super(new BorderLayout());
        this.fillWithCurrentHour = fillWithCurrentHour;

        final DateFormatter formatter = new DateFormatter(new SimpleDateFormat(withSeconds ? "HH:mm:ss" : "HH:mm"));
        formatter.setOverwriteMode(true);
        // don't setAllowsInvalid(false) otherwise we can't replace 07:00 by 21:00
        formatter.setMaximum(dateFromTimeInMillis(DAY_LENGTH - 1));

        this.text = new JFormattedTextField(formatter);
        final int h = this.text.getPreferredSize().height;
        this.text.setMinimumSize(new Dimension(40, h));
        this.text.setPreferredSize(new Dimension(50, h));
        this.add(this.text, BorderLayout.CENTER);

        this.text.addPropertyChangeListener("editValid", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                setValidated((Boolean) evt.getNewValue());
            }
        });
        // initial value
        this.validSupp = new ValidChangeSupport(this, ValidState.getNoReasonInstance(this.text.isEditValid()));

        this.resetValue();
    }

    private JTextComponent getEditor() {
        return this.text;
    }

    @Override
    public final void resetValue() {
        if (this.fillWithCurrentHour) {
            this.setTimeInMillis(((long) Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) * 3600 * 1000);
        } else {
            this.setValue(null);
        }
    }

    @Override
    public final void setValue(final Date val) {
        final Long time;
        if (val == null) {
            time = null;
        } else {
            JTime.CAL.setTime(val);
            TimeUtils.clearTime(JTime.CAL);
            // midnight
            final long woTime = JTime.CAL.getTimeInMillis();
            time = val.getTime() - woTime;
        }
        this.setTimeInMillis(time);
    }

    public final void setTimeInMillis(final Long val) {
        this.text.setValue(dateFromTimeInMillis(val));
    }

    @Override
    public final Date getValue() {
        return (Date) this.text.getValue();
    }

    public final Long getTimeInMillis() {
        final Date txtVal = this.getValue();
        if (txtVal == null) {
            return null;
        } else {
            return txtVal.getTime() + JTime.CAL.getTimeZone().getOffset(txtVal.getTime());
        }
    }

    @Override
    public final void addValueListener(PropertyChangeListener l) {
        this.getEditor().addPropertyChangeListener("value", l);
    }

    @Override
    public void rmValueListener(PropertyChangeListener l) {
        this.getEditor().removePropertyChangeListener("value", l);
    }

    // useful since by default this commits on focus lost, as is a table cell editor. So sometimes
    // the table cell editor is called back before the commit and thus takes the original value.
    public void commitEdit() throws ParseException {
        this.text.commitEdit();
    }

    @Override
    public JComponent getComp() {
        return this;
    }

    protected final void setValidated(boolean newValue) {
        this.validSupp.fireValidChange(ValidState.getNoReasonInstance(newValue));
    }

    @Override
    public ValidState getValidState() {
        return this.validSupp.getValidState();
    }

    @Override
    public void addValidListener(ValidListener l) {
        this.validSupp.addValidListener(l);
    }

    @Override
    public void removeValidListener(ValidListener l) {
        this.validSupp.removeValidListener(l);
    }

    @Override
    public JTextComponent getTextComp() {
        return this.getEditor();
    }
}
