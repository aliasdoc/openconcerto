package org.openconcerto.modules.operation;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.SwingUtilities;

import org.jopencalendar.model.Flag;
import org.jopencalendar.model.JCalendarItem;
import org.jopencalendar.ui.JCalendarItemProvider;
import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.model.SQLRowValuesListFetcher;
import org.openconcerto.sql.model.SQLSelect;
import org.openconcerto.sql.model.Where;
import org.openconcerto.sql.model.graph.Path;
import org.openconcerto.sql.model.graph.PathBuilder;
import org.openconcerto.sql.users.User;
import org.openconcerto.sql.users.UserManager;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.cc.ITransformer;

public class OperationCalendarManager extends JCalendarItemProvider {
    public OperationCalendarManager(String name) {
        super(name);
    }

    private List<User> users;
    private List<String> states;
    private boolean hideLocked = false;
    private boolean hideUnlocked = false;

    /**
     * Set the filter to retrieve items
     * 
     * @param users if null don't limit to specific users
     * @param states if null don't limite to specific states
     */
    synchronized public void setFilter(List<User> users, List<String> states, boolean hideLocked, boolean hideUnlocked) {
        this.users = users;
        this.states = states;
        this.hideLocked = hideLocked;
        this.hideUnlocked = hideUnlocked;
    }

    @Override
    synchronized public List<JCalendarItem> getItemInWeek(int week, int year) {
        assert !SwingUtilities.isEventDispatchThread();
        return getItemInWeek(week, year, this.users, this.states);
    }

    private List<JCalendarItem> getItemInWeek(final int week, final int year, final List<User> selectedUsers, final List<String> selectedStates) {
        final Calendar c = Calendar.getInstance();
        c.clear();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.WEEK_OF_YEAR, week);
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        final Date date1 = c.getTime();
        c.add(Calendar.WEEK_OF_YEAR, 1);
        final Date date2 = c.getTime();
        return getItemIn(date1, date2, selectedUsers, selectedStates);
    }

    public List<JCalendarItem> getItemIn(final Date date1, final Date date2, List<User> selectedUsers, final List<String> selectedStates) {
        final List<User> users = new ArrayList<User>();
        if (selectedUsers == null) {
            users.addAll(UserManager.getInstance().getAllActiveUsers());
        } else {
            users.addAll(selectedUsers);
        }
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        if (selectedStates != null && selectedStates.isEmpty()) {
            return Collections.emptyList();
        }
        final DBRoot root = ComptaPropsConfiguration.getInstanceCompta().getRootSociete();

        final SQLRowValues valCalendarItems = new SQLRowValues(root.getTable("CALENDAR_ITEM"));
        valCalendarItems.putNulls("START", "END", "DURATION_S", "SUMMARY", "DESCRIPTION", "FLAGS", "STATUS", "SOURCE_ID", "SOURCE_TABLE");
        final SQLRowValues valsCalendarItemsGroup = valCalendarItems.putRowValues("ID_CALENDAR_ITEM_GROUP");
        valsCalendarItemsGroup.put("NAME", null);

        final SQLRowValues valSite = new SQLRowValues(root.getTable(ModuleOperation.TABLE_SITE));
        valSite.putNulls("NAME", "COMMENT");

        final SQLRowValues valOperation = new SQLRowValues(root.getTable(ModuleOperation.TABLE_OPERATION));
        final SQLRowValues userVals = valOperation.putRowValues("ID_USER_COMMON").putNulls("NOM");
        valOperation.put("ID_CALENDAR_ITEM_GROUP", valsCalendarItemsGroup);
        valOperation.put("ID_SITE", valSite);
        valOperation.putNulls("STATUS", "TYPE", "PLANNER_XML", "PLANNER_UID");

        final SQLRowValuesListFetcher fetcher = SQLRowValuesListFetcher.create(valCalendarItems);
        fetcher.setFullOnly(true);
        fetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {

            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                input.andWhere(new Where(valCalendarItems.getTable().getField("START"), date1, true, date2, false));
                return input;
            }
        });

        final Path item2Operation = new PathBuilder(valCalendarItems.getTable()).addForeignField("ID_CALENDAR_ITEM_GROUP").addReferentTable(ModuleOperation.TABLE_OPERATION).build();
        try {
            CollectionUtils.getSole(fetcher.getFetchers(item2Operation).allValues()).setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
                @Override
                public SQLSelect transformChecked(SQLSelect input) {
                    if (selectedStates != null) {
                        // Le status utilisé est celui de OPERATION
                        input.andWhere(new Where(input.getAlias(valOperation.getTable()).getField("STATUS"), selectedStates));
                    }
                    System.err.println("OperationCalendarManager.getItemIn(...).new ITransformer() {...}.transformChecked() " + input);
                    return input;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        final Path p = item2Operation.addForeignField("ID_USER_COMMON");
        final Path pItemToOperation = p.minusLast();
        final Collection<SQLRowValuesListFetcher> fetchers = fetcher.getFetchers(p).allValues();
        if (fetchers.size() != 1)
            throw new IllegalStateException("Not one fetcher : " + fetchers);
        final SQLRowValuesListFetcher userFetcher = fetchers.iterator().next();
        final ITransformer<SQLSelect, SQLSelect> prevTransf = userFetcher.getSelTransf();
        userFetcher.setSelTransf(new ITransformer<SQLSelect, SQLSelect>() {
            @Override
            public SQLSelect transformChecked(SQLSelect input) {
                final List<Number> userIDs = new ArrayList<Number>();
                for (User user : users) {
                    userIDs.add(user.getId());
                }
                input.andWhere(new Where(input.getAlias(userVals.getTable()).getKey(), userIDs));
                // Because it can be the same fetcher that the previous on OPERATION
                return prevTransf == null ? input : prevTransf.transformChecked(input);
            }
        });

        final List<SQLRowValues> rows = fetcher.fetch();

        final List<JCalendarItem> result = new ArrayList<JCalendarItem>(rows.size());
        for (SQLRowValues r : rows) {
            final SQLRowValues user = r.followPath(p);

            if (user != null) {
                final SQLRowValues operation = r.followPath(pItemToOperation);
                final JCalendarItemDB item = new JCalendarItemDB(r.getString("SOURCE_TABLE"), r.getLong("SOURCE_ID"), r.getForeign("ID_CALENDAR_ITEM_GROUP").getID());
                item.setDayOnly(false);
                item.setDtStart(r.getDate("START"));
                item.setDtEnd(r.getDate("END"));
                item.setSummary(r.getString("SUMMARY"));

                if (r.getString("FLAGS") != null) {
                    List<String> str = StringUtils.fastSplit(r.getString("FLAGS"), ',');

                    for (String fId : str) {
                        Flag f = Flag.getFlag(fId);
                        if (f == null) {
                            f = new Flag(fId, null, fId, "");
                            Flag.register(f);
                        }
                        item.addFlag(f);
                    }

                }
                String desc = "";
                if (r.getString("DESCRIPTION") != null) {
                    desc += r.getString("DESCRIPTION") + "\n";
                }
                item.setDescription(desc);
                item.setColor(UserColor.getInstance().getColor(user.getID()));
                item.setCookie(user);
                item.setUserId(user.getID());
                item.setOperationStatus(operation.getString("STATUS"));
                item.setOperationType(operation.getString("TYPE"));
                item.setPlannerXML(operation.getString("PLANNER_XML"));
                item.setPlannerUID(operation.getString("PLANNER_UID"));
                item.setSiteName(operation.getForeign("ID_SITE").getString("NAME"));
                item.setSiteId(operation.getForeign("ID_SITE").getIDNumber());
                item.setSiteComment(operation.getForeign("ID_SITE").getString("COMMENT"));
                boolean isLocked = item.hasFlag(Flag.getFlag("locked"));
                if (!this.hideLocked && isLocked) {
                    result.add(item);
                } else if (!this.hideUnlocked && !isLocked) {
                    result.add(item);
                }

            }
        }
        return result;
    }
}
