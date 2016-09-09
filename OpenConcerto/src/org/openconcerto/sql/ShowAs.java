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
 * Créé le 25 févr. 2005
 */
package org.openconcerto.sql;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBStructureItemNotFound;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLName;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;

/**
 * Gère la représentation des clefs externes. TODO Pour ne pas avoir la désignation du site :
 * pouvoir spécifier ELEMENT_TABLEAU.ID_TABLEAU_ELECTRIQUE => DESIGNATION,
 * ID_LOCAL.ID_BATIMENT.DESIGNATION, ID_LOCAL.DESIGNATION
 * 
 * @author Sylvain CUAZ
 */
@ThreadSafe
public class ShowAs extends FieldExpander {

    @GuardedBy("this")
    private DBRoot root;
    // eg /OBSERVATION/ -> [ID_ARTICLE_1, DESIGNATION]
    @GuardedBy("this")
    private final Map<SQLTable, List<SQLField>> byTables;
    // eg |TABLEAU.ID_OBSERVATION| -> [DESIGNATION]
    @GuardedBy("this")
    private final Map<SQLField, List<SQLField>> byFields;

    public ShowAs(DBRoot root) {
        super();
        this.byTables = new HashMap<SQLTable, List<SQLField>>();
        this.byFields = new HashMap<SQLField, List<SQLField>>();

        this.setRoot(root);
    }

    public ShowAs(ShowAs src) {
        this(src.getRoot());
        this.putAll(src);
    }

    public final void putAll(ShowAs s) {
        synchronized (this) {
            synchronized (s) {
                CollectionUtils.addIfNotPresent(this.byFields, s.byFields);
                CollectionUtils.addIfNotPresent(this.byTables, s.byTables);
            }
            // s might have replaced some of our entries
            this.clearCache();
        }
    }

    public synchronized List<SQLField> getFieldExpand(SQLTable table) {
        return this.byTables.get(table);
    }

    /**
     * Set the base which is used when passing String in lieu of SQLField or SQLTable.
     * 
     * @param root the base to use.
     */
    public synchronized final void setRoot(DBRoot root) {
        this.root = root;
    }

    public synchronized final DBRoot getRoot() {
        return this.root;
    }

    private synchronized SQLField getField(String fieldName) {
        return this.root.getDesc(SQLName.parse(fieldName), SQLField.class);
    }

    private synchronized SQLTable getTable(String tableName) {
        try {
            return this.root.getDesc(SQLName.parse(tableName), SQLTable.class);
        } catch (DBStructureItemNotFound e) {
            return null;
        }
    }

    static private final List<SQLField> namesToFields(final List<String> names, final SQLTable table) {
        final List<SQLField> res = new ArrayList<SQLField>(names.size());
        for (final String fieldName : names) {
            res.add(table.getField(fieldName));
        }
        return res;
    }

    // TODO a listener to remove tables and fields as they are dropped
    public synchronized final void removeTable(SQLTable t) {
        this.byTables.remove(t);
        for (final Iterator<Entry<SQLField, List<SQLField>>> iterator = this.byFields.entrySet().iterator(); iterator.hasNext();) {
            final Entry<SQLField, List<SQLField>> e = iterator.next();
            if (e.getKey().getTable().equals(t)) {
                iterator.remove();
            } else {
                final SQLField f = CollectionUtils.getFirst(e.getValue());
                if (f != null && f.getTable().equals(t))
                    iterator.remove();
            }
        }
        this.clearCache();
    }

    /**
     * Make sure that no fields of the passed table will be expanded. I.e. it
     * {@link #removeTable(SQLTable) removes} all reference to the passed table and then re-add the
     * table so that it is expanded to nothing instead of throwing an exception.
     * 
     * @param removedTable the table to remove.
     */
    public synchronized void neverShow(final SQLTable removedTable) {
        this.removeTable(removedTable);
        this.show(removedTable, Collections.<String> emptyList());
    }

    public synchronized final void clear() {
        this.setRoot(null);
        this.byTables.clear();
        this.byFields.clear();
        this.clearCache();
    }

    // *** byTables

    public void show(String tableName, String... fields) {
        this.show(tableName, Arrays.asList(fields));
    }

    /**
     * Spécifie que la table tableName doit être représenté par les champs fields. Si la table
     * n'existe pas, cette méthode n'a pas d'effet.
     * 
     * @param tableName le nom de la table, eg "ETABLISSEMENT".
     * @param fields les noms des champs, eg ["DESCRIPTION", "NUMERO"].
     */
    public synchronized void show(String tableName, List<String> fields) {
        final SQLTable table = this.getTable(tableName);
        if (table != null) {
            this.show(table, fields);
        } else {
            Log.get().warning(this.root + " does not contain the table:" + tableName);
        }
    }

    public void show(SQLTable table, String... fields) {
        this.show(table, Arrays.asList(fields));
    }

    public synchronized void show(SQLTable table, List<String> fields) {
        this.byTables.put(table, namesToFields(fields, table));
        this.clearCache();
    }

    // *** byFields

    public void showField(String fieldName, String... fields) {
        this.showField(fieldName, Arrays.asList(fields));
    }

    /**
     * Spécifie que le champ fieldName doit être représenté par les champs fields. Permet de
     * contrôler plus en finesse.
     * 
     * @param fieldName le nom du champ, eg "CONTACT.ID_ETABLISSEMENT".
     * @param fields les noms des champs, eg ["DESCRIPTION"].
     */
    public synchronized void showField(String fieldName, List<String> fields) {
        this.show(getField(fieldName), fields);
    }

    public synchronized final void show(SQLField field, List<String> fields) {
        this.byFields.put(field, namesToFields(fields, field.getTable().getBase().getGraph().getForeignTable(field)));
        this.clearCache();
    }

    // *** expand

    @Override
    protected synchronized List<SQLField> expandOnce(SQLField field) {
        // c'est une clef externe, donc elle pointe sur une table
        final SQLTable foreignTable = field.getTable().getBase().getGraph().getForeignTable(field);
        final List<SQLField> res;
        if (this.byFields.containsKey(field)) {
            res = this.byFields.get(field);
        } else if (this.byTables.containsKey(foreignTable)) {
            res = this.byTables.get(foreignTable);
        } else {
            // on ne sait pas comment l'expandre
            throw new IllegalStateException(field + " cannot be expanded by " + this + "\nforeign table of " + field.getSQLName() + ":" + foreignTable.getSQLName());
        }
        return res;
    }

    /**
     * Expand le champ.
     * 
     * @param fieldName le nom du champ à expandre, eg "SITE.ID_ETABLISSEMENT".
     * @return la liste des champs, eg [|ETABLISSEMENT.DESCRIPTION|, |ETABLISSEMENT.NUMERO|].
     */
    public synchronized List<SQLField> simpleExpand(String fieldName) {
        return this.simpleExpand(getField(fieldName));
    }

    @Override
    public synchronized String toString() {
        return super.toString() + " byTables: " + this.byTables + " byFields: " + this.byFields;
    }

}
