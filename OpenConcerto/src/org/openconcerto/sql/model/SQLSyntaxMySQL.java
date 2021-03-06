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
 
 package org.openconcerto.sql.model;

import org.openconcerto.sql.model.SQLField.Properties;
import org.openconcerto.sql.model.SQLTable.SQLIndex;
import org.openconcerto.sql.model.graph.Link.Rule;
import org.openconcerto.sql.model.graph.TablesMap;
import org.openconcerto.sql.utils.ChangeTable;
import org.openconcerto.sql.utils.ChangeTable.ClauseType;
import org.openconcerto.sql.utils.ChangeTable.OutsideClause;
import org.openconcerto.sql.utils.SQLUtils;
import org.openconcerto.sql.utils.SQLUtils.SQLFactory;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.ListMap;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.Tuple2;
import org.openconcerto.utils.cc.IClosure;
import org.openconcerto.utils.cc.ITransformer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.dbcp.DelegatingConnection;

/**
 * MySQL can enable compression with the "useCompression" connection property. Compression status
 * can be checked with "show global status like 'Compression';".
 * 
 * @author Sylvain CUAZ
 */
class SQLSyntaxMySQL extends SQLSyntax {

    private static final Pattern INT_PATTERN = Pattern.compile("(bigint|smallint|int)");

    SQLSyntaxMySQL() {
        super(SQLSystem.MYSQL);
        this.typeNames.addAll(Boolean.class, "boolean", "bool", "bit");
        this.typeNames.addAll(Short.class, "smallint");
        this.typeNames.addAll(Integer.class, "integer", "int");
        this.typeNames.addAll(Long.class, "bigint");
        this.typeNames.addAll(BigDecimal.class, "decimal", "numeric");
        this.typeNames.addAll(Float.class, "float");
        this.typeNames.addAll(Double.class, "double precision", "real");
        this.typeNames.addAll(Timestamp.class, "timestamp");
        this.typeNames.addAll(java.util.Date.class, "time");
        this.typeNames.addAll(Blob.class, "blob", "tinyblob", "mediumblob", "longblob", "varbinary", "binary");
        this.typeNames.addAll(Clob.class, "text", "tinytext", "mediumtext", "longtext", "varchar", "char");
        this.typeNames.addAll(String.class, "varchar", "char");
    }

    @Override
    public int getMaximumIdentifierLength() {
        // http://dev.mysql.com/doc/refman/5.7/en/identifiers.html
        return 64;
    }

    public String getIDType() {
        return " int";
    }

    @Override
    public boolean isAuto(SQLField f) {
        return "YES".equals(f.getMetadata("IS_AUTOINCREMENT"));
    }

    @Override
    public String getAuto() {
        return this.getIDType() + " AUTO_INCREMENT NOT NULL";
    }

    @Override
    public String getDateAndTimeType() {
        return "datetime";
    }

    @Override
    protected String getAutoDateType(SQLField f) {
        return "timestamp";
    }

    @Override
    public int getMaximumVarCharLength() {
        // http://dev.mysql.com/doc/refman/5.0/en/char.html
        return (65535 - 2) / SQLSyntaxPG.MAX_BYTES_PER_CHAR;
    }

    @Override
    protected Tuple2<Boolean, String> getCast() {
        return null;
    }

    @Override
    public String cast(String expr, String type) {
        // MySQL doesn't use types but keywords
        return super.cast(expr, INT_PATTERN.matcher(type).replaceAll("integer").replace("integer", "signed integer"));
    }

    @Override
    protected boolean supportsDefault(String typeName) {
        return !typeName.contains("text") && !typeName.contains("blob");
    }

    @Override
    public String transfDefaultJDBC2SQL(SQLField f) {
        final Class<?> javaType = f.getType().getJavaType();
        String res = f.getDefaultValue();
        if (res == null)
            // either no default or NULL default
            // see http://dev.mysql.com/doc/refman/5.0/en/data-type-defaults.html
            // (works the same way for 5.1 and 6.0)
            if (Boolean.FALSE.equals(f.isNullable()))
            res = null;
            else {
            res = "NULL";
            }
        else if (javaType == String.class)
            // this will be given to other db system, so don't use base specific quoting
            res = SQLBase.quoteStringStd(res);
        // MySQL 5.0.24a puts empty strings when not specifying default
        else if (res.length() == 0)
            res = null;
        // quote neither functions nor CURRENT_TIMESTAMP
        else if (Date.class.isAssignableFrom(javaType) && !res.trim().endsWith("()") && !res.toLowerCase().contains("timestamp"))
            res = SQLBase.quoteStringStd(res);
        else if (javaType == Boolean.class)
            res = res.equals("0") ? "FALSE" : "TRUE";
        return res;
    }

    @Override
    public String getCreateTableSuffix() {
        return " ENGINE = InnoDB ";
    }

    @Override
    public String disableFKChecks(DBRoot b) {
        return "SET FOREIGN_KEY_CHECKS=0;";
    }

    @Override
    public String enableFKChecks(DBRoot b) {
        return "SET FOREIGN_KEY_CHECKS=1;";
    }

    @Override
    public String getDropFK() {
        return "DROP FOREIGN KEY ";
    }

    @Override
    protected String getRuleSQL(Rule r) {
        if (r == Rule.SET_DEFAULT)
            throw new UnsupportedOperationException(r + " isn't supported");
        return super.getRuleSQL(r);
    }

    @Override
    public String getDropConstraint() {
        // in MySQL there's only 2 types of constraints : foreign keys and unique
        // fk are handled by getDropFK(), so this is just for unique
        // in MySQL UNIQUE constraint and index are one and the same thing
        return "DROP INDEX ";
    }

    @Override
    public Map<String, Object> normalizeIndexInfo(final Map m) {
        final Map<String, Object> res = copyIndexInfoMap(m);
        final Object nonUnique = res.get("NON_UNIQUE");
        // some newer versions of MySQL now return Boolean
        res.put("NON_UNIQUE", nonUnique instanceof Boolean ? nonUnique : Boolean.valueOf((String) nonUnique));
        res.put("COLUMN_NAME", res.get("COLUMN_NAME"));
        return res;
    }

    @Override
    public String getDropIndex(String name, SQLName tableName) {
        return "DROP INDEX " + SQLBase.quoteIdentifier(name) + " on " + tableName.quote() + ";";
    }

    @Override
    protected String getCreateIndex(String cols, SQLName tableName, SQLIndex i) {
        final String method = i.getMethod() != null ? " USING " + i.getMethod() : "";
        return super.getCreateIndex(cols, tableName, i) + method;
    }

    @Override
    public boolean isUniqueException(SQLException exn) {
        final SQLException e = SQLUtils.findWithSQLState(exn);
        // 1062 is the real "Duplicate entry" error, 1305 happens when we emulate partial unique
        // constraint
        return e.getErrorCode() == 1062 || (e.getErrorCode() == 1305 && e.getMessage().contains(ChangeTable.MYSQL_FAKE_PROCEDURE + " does not exist"));
    }

    @Override
    public boolean isDeadLockException(SQLException exn) {
        return SQLUtils.findWithSQLState(exn).getErrorCode() == 1213;
    }

    @Override
    public Map<ClauseType, List<String>> getAlterField(SQLField f, Set<Properties> toAlter, String type, String defaultVal, Boolean nullable) {
        final boolean newNullable = toAlter.contains(Properties.NULLABLE) ? nullable : getNullable(f);
        final String newType = toAlter.contains(Properties.TYPE) ? type : getType(f);
        String newDef = toAlter.contains(Properties.DEFAULT) ? defaultVal : getDefault(f, newType);
        // MySQL doesn't support "NOT NULL DEFAULT NULL" so use the equivalent "NOT NULL"
        if (!newNullable && newDef != null && newDef.trim().toUpperCase().equals("NULL"))
            newDef = null;

        return ListMap.singleton(ClauseType.ALTER_COL, "MODIFY COLUMN " + f.getQuotedName() + " " + getFieldDecl(newType, newDef, newNullable));
    }

    @Override
    public String getDropTable(SQLName name, boolean ifExists, boolean restrict) {
        // doesn't support cascade
        if (!restrict)
            return null;
        else
            return super.getDropTable(name, ifExists, restrict);
    }

    @Override
    public String getDropRoot(String name) {
        return "DROP DATABASE IF EXISTS " + SQLBase.quoteIdentifier(name) + " ;";
    }

    @Override
    public String getCreateRoot(String name) {
        return "CREATE DATABASE " + SQLBase.quoteIdentifier(name) + " ;";
    }

    @Override
    protected void _storeData(final SQLTable t, final File file) throws IOException {
        checkServerLocalhost(t);
        final ListMap<String, String> charsets = new ListMap<String, String>();
        for (final SQLField f : t.getFields()) {
            final Object charset = f.getInfoSchema().get("CHARACTER_SET_NAME");
            // non string field
            if (charset != null)
                charsets.add(charset.toString(), f.getName());
        }
        if (charsets.size() > 1)
            // MySQL dumps strings in binary, so fields must be consistent otherwise the
            // file is invalid
            throw new IllegalArgumentException(t + " has more than on character set : " + charsets);
        final SQLBase base = t.getBase();
        // if no string cols there should only be values within ASCII (eg dates, ints, etc)
        final String charset = charsets.size() == 0 ? "UTF8" : charsets.keySet().iterator().next();
        final String cols = CollectionUtils.join(t.getOrderedFields(), ",", new ITransformer<SQLField, String>() {
            @Override
            public String transformChecked(SQLField input) {
                return base.quoteString(input.getName());
            }
        });
        final File tmp = File.createTempFile(SQLSyntaxMySQL.class.getSimpleName() + "storeData", ".txt");
        // MySQL cannot overwrite files. Also on Windows tmp is in the user profile which the
        // service cannot access ; conversely tmpdir of MySQL is not readable by normal users,
        // in that case grant traverse and write permission to MySQL (e.g. Network Service).
        tmp.delete();
        final SQLSelect sel = new SQLSelect(true).addSelectStar(t);
        // store the data in the temp file
        base.getDataSource().execute("SELECT " + cols + " UNION " + sel.asString() + " INTO OUTFILE " + base.quoteString(tmp.getAbsolutePath()) + " " + getDATA_OPTIONS(base) + ";");
        // then read it to remove superfluous escape char and convert to utf8
        final BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(tmp), charset));
        Writer w = null;
        try {
            w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StringUtils.UTF8));
            normalizeData(r, w, 1000 * 1024);
        } finally {
            r.close();
            if (w != null)
                w.close();
            tmp.delete();
        }
    }

    // remove superfluous escape character
    static void normalizeData(final Reader r, final Writer w, final int bufferSize) throws IOException {
        int count;
        final char[] buf = new char[bufferSize];
        int offset = 0;
        final char[] wbuf = new char[buf.length];
        boolean wasBackslash = false;
        while ((count = r.read(buf, offset, buf.length - offset)) != -1) {
            int wbufLength = 0;
            for (int i = 0; i < offset + count; i++) {
                final char c = buf[i];
                // MySQL escapes the field delimiter (which other systems do as well)
                // but also "LINES TERMINATED BY" which others don't understand
                if (wasBackslash && c == '\n')
                    // overwrite the backslash
                    wbuf[wbufLength - 1] = c;
                else
                    wbuf[wbufLength++] = c;
                wasBackslash = c == '\\';
            }
            // the read buffer ends with a backslash, don't let it be written to w as we might
            // want to remove it
            if (wasBackslash) {
                // restore state one char before
                wbufLength--;
                wasBackslash = wbuf[wbufLength - 1] == '\\';
                buf[0] = '\\';
                offset = 1;
            } else {
                offset = 0;
            }
            w.write(wbuf, 0, wbufLength);
        }
    }

    private static String getDATA_OPTIONS(final SQLBase b) {
        return "FIELDS TERMINATED BY ',' ENCLOSED BY '\"' ESCAPED BY " + b.quoteString("\\") + " LINES TERMINATED BY '\n' ";
    }

    @Override
    public void _loadData(final File f, final SQLTable t) {
        // we always store in utf8 regardless of the encoding of the columns
        final SQLDataSource ds = t.getDBSystemRoot().getDataSource();
        try {
            SQLUtils.executeAtomic(ds, new SQLFactory<Object>() {
                @Override
                public Object create() throws SQLException {
                    final String charsetClause;
                    final Connection conn = ((DelegatingConnection) ds.getConnection()).getInnermostDelegate();
                    if (((com.mysql.jdbc.Connection) conn).versionMeetsMinimum(5, 0, 38)) {
                        charsetClause = "CHARACTER SET utf8 ";
                    } else {
                        // variable name is in the first column
                        final String dbCharset = ds.executeA1("show variables like 'character_set_database'")[1].toString().trim().toLowerCase();
                        if (dbCharset.equals("utf8")) {
                            charsetClause = "";
                        } else {
                            throw new IllegalStateException("the database charset is not utf8 and this version doesn't support specifying another one : " + dbCharset);
                        }
                    }
                    ds.execute(t.getBase().quote("LOAD DATA LOCAL INFILE %s INTO TABLE %f ", f.getAbsolutePath(), t) + charsetClause + getDATA_OPTIONS(t.getBase()) + " IGNORE 1 LINES;");
                    return null;
                }
            });
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't load " + f + " into " + t, e);
        }
    }

    @Override
    SQLBase createBase(SQLServer server, String name, final IClosure<? super DBSystemRoot> systemRootInit, String login, String pass, IClosure<? super SQLDataSource> dsInit) {
        return new MySQLBase(server, name, systemRootInit, login, pass, dsInit);
    }

    @Override
    public String getNullIsDataComparison(String x, boolean eq, String y) {
        final String nullSafe = x + " <=> " + y;
        if (eq)
            return nullSafe;
        else
            return "NOT (" + nullSafe + ")";
    }

    @Override
    public String getFormatTimestamp(String sqlTS, boolean basic) {
        return "DATE_FORMAT(" + sqlTS + ", " + SQLBase.quoteStringStd(basic ? "%Y%m%dT%H%i%s.%f" : "%Y-%m-%dT%H:%i:%s.%f") + ")";
    }

    private final void getRow(StringBuilder sb, List<String> row, final int requiredColCount, List<String> columnsAlias) {
        // should be OK since requiredColCount is computed from columnsAlias in getConstantTable()
        assert columnsAlias == null || requiredColCount == columnsAlias.size();
        final int actualColCount = row.size();
        if (actualColCount != requiredColCount)
            throw new IllegalArgumentException("Wrong number of columns, should be " + requiredColCount + " but row is " + row);
        for (int i = 0; i < actualColCount; i++) {
            sb.append(row.get(i));
            if (columnsAlias != null) {
                sb.append(" as ");
                sb.append(SQLBase.quoteIdentifier(columnsAlias.get(i)));
            }
            if (i < actualColCount - 1)
                sb.append(", ");
        }
    }

    @Override
    public String getConstantTable(List<List<String>> rows, String alias, List<String> columnsAlias) {
        final int rowCount = rows.size();
        if (rowCount < 1)
            throw new IllegalArgumentException("Empty rows will cause a syntax error");
        final int colCount = columnsAlias.size();
        if (colCount < 1)
            throw new IllegalArgumentException("Empty columns will cause a syntax error");
        final StringBuilder sb = new StringBuilder(rows.size() * 64);
        sb.append("( SELECT ");
        // aliases needed only for the first row
        getRow(sb, rows.get(0), colCount, columnsAlias);
        for (int i = 1; i < rowCount; i++) {
            sb.append("\nUNION ALL\nSELECT ");
            getRow(sb, rows.get(i), colCount, null);
        }
        sb.append(" ) as ");
        sb.append(SQLBase.quoteIdentifier(alias));
        return sb.toString();
    }

    @Override
    public String getFunctionQuery(SQLBase b, Set<String> schemas) {
        // MySQL puts the db name in schema
        return "SELECT null as \"schema\", ROUTINE_NAME as \"name\", ROUTINE_DEFINITION as \"src\" FROM \"information_schema\".ROUTINES where ROUTINE_CATALOG is null and ROUTINE_SCHEMA = '"
                + b.getMDName() + "'";
    }

    @Override
    public String getTriggerQuery(SQLBase b, TablesMap tables) {
        return "SELECT \"TRIGGER_NAME\", null as \"TABLE_SCHEMA\", EVENT_OBJECT_TABLE as \"TABLE_NAME\", ACTION_STATEMENT as \"ACTION\", null as \"SQL\" from INFORMATION_SCHEMA.TRIGGERS "
                + getMySQLTablesMapJoin(b, tables, "EVENT_OBJECT_SCHEMA", "EVENT_OBJECT_TABLE");
    }

    private String getMySQLTablesMapJoin(final SQLBase b, final TablesMap tables, final String schemaCol, final String tableCol) {
        // MySQL only has "null" schemas through JDBC
        assert tables.size() <= 1;
        // but in information_schema, the TABLE_CATALOG is always NULL and TABLE_SCHEMA has the JDBC
        // database name
        final TablesMap translated;
        if (tables.size() == 0) {
            translated = tables;
        } else {
            assert tables.keySet().equals(Collections.singleton(null)) : tables;
            translated = new TablesMap(1);
            translated.put(b.getMDName(), tables.get(null));
        }
        return getTablesMapJoin(b, translated, schemaCol, tableCol);
    }

    @Override
    public String getColumnsQuery(SQLBase b, TablesMap tables) {
        return "SELECT null as \"" + INFO_SCHEMA_NAMES_KEYS.get(0) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(1) + "\", \"" + INFO_SCHEMA_NAMES_KEYS.get(2)
                + "\" , \"CHARACTER_SET_NAME\", \"COLLATION_NAME\" from INFORMATION_SCHEMA.\"COLUMNS\" " + getMySQLTablesMapJoin(b, tables, "TABLE_SCHEMA", "TABLE_NAME");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getConstraints(SQLBase b, TablesMap tables) throws SQLException {
        final String sel = "SELECT null as \"TABLE_SCHEMA\", c.\"TABLE_NAME\", c.\"CONSTRAINT_NAME\", tc.\"CONSTRAINT_TYPE\", \"COLUMN_NAME\", c.\"ORDINAL_POSITION\", NULL as \"DEFINITION\"\n"
                // from
                + " FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE c\n"
                // "-- sub-select otherwise at least 15s\n" +
                + "JOIN (SELECT * FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS T " + getMySQLTablesMapJoin(b, tables, "TABLE_SCHEMA", "TABLE_NAME")
                + ") tc on tc.\"TABLE_SCHEMA\" = c.\"TABLE_SCHEMA\" and tc.\"TABLE_NAME\"=c.\"TABLE_NAME\" and tc.\"CONSTRAINT_NAME\"=c.\"CONSTRAINT_NAME\"\n"
                // requested tables
                + getMySQLTablesMapJoin(b, tables, "c.TABLE_SCHEMA", "c.TABLE_NAME")
                // order
                + "order by c.\"TABLE_SCHEMA\", c.\"TABLE_NAME\", c.\"CONSTRAINT_NAME\", c.\"ORDINAL_POSITION\"";
        // don't cache since we don't listen on system tables
        final List<Map<String, Object>> res = (List<Map<String, Object>>) b.getDBSystemRoot().getDataSource().execute(sel, new IResultSetHandler(SQLDataSource.MAP_LIST_HANDLER, false));
        mergeColumnNames(res);
        return res;
    }

    static void mergeColumnNames(final List<Map<String, Object>> res) {
        final Iterator<Map<String, Object>> listIter = res.iterator();
        List<String> l = null;
        while (listIter.hasNext()) {
            final Map<String, Object> m = listIter.next();
            // don't leave the meaningless position (it will always be equal to 1)
            final int pos = ((Number) m.remove("ORDINAL_POSITION")).intValue();
            if (pos == 1) {
                l = new ArrayList<String>();
                m.put("COLUMN_NAMES", l);
            } else {
                listIter.remove();
            }
            l.add((String) m.remove("COLUMN_NAME"));
        }
    }

    @Override
    public String getDropTrigger(Trigger t) {
        return "DROP TRIGGER " + new SQLName(t.getTable().getSchema().getName(), t.getName()).quote();
    }

    @Override
    public String getUpdate(final SQLTable t, List<String> tables, Map<String, String> setPart) {
        final List<String> l = new ArrayList<String>(tables);
        l.add(0, t.getSQLName().quote());
        return CollectionUtils.join(l, ", ") + "\nSET " + CollectionUtils.join(setPart.entrySet(), ",\n", new ITransformer<Entry<String, String>, String>() {
            @Override
            public String transformChecked(Entry<String, String> input) {
                // MySQL needs to prefix the fields, since there's no designated table to update
                return t.getField(input.getKey()).getSQLName(t).quote() + " = " + input.getValue();
            }
        });
    }

    public OutsideClause getSetTableComment(final String comment) {
        return new OutsideClause() {
            @Override
            public ClauseType getType() {
                return ClauseType.OTHER;
            }

            @Override
            public String asString(SQLName tableName) {
                return "ALTER TABLE " + tableName.quote() + " COMMENT = " + SQLBase.quoteStringStd(comment) + ";";
            }
        };
    }
}
