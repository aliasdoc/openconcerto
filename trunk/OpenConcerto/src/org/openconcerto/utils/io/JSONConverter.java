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
 
 package org.openconcerto.utils.io;

import org.openconcerto.utils.NumberUtils;

import java.awt.Color;
import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

public class JSONConverter {
    final static Pattern pattern = Pattern.compile("d{4}-[01]d-[0-3]dT[0-2]d:[0-5]d:[0-5]d.d+([+-][0-2]d:[0-5]d|Z)");

    public static Object getJSON(Object param) {
        Object result = null;

        if (param != null) {
            if (param instanceof HTMLable) {
                result = ((HTMLable) param).getHTML();
            } else if (param instanceof JSONAble) {
                result = ((JSONAble) param).toJSON();
            } else if (param instanceof Timestamp) {
                final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
                result = df.format((Timestamp) param);
            } else if (param instanceof Calendar) {
                final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'");
                result = df.format(((Calendar) param).getTime());
            } else if (param instanceof Class<?>) {
                result = ((Class<?>) param).getName();
            } else if (param instanceof Iterable) {
                final Iterable<?> tmp = (Iterable<?>) param;
                final JSONArray jsonArray = new JSONArray();
                for (Object o : tmp) {
                    jsonArray.add(getJSON(o));
                }
                result = jsonArray;
            } else if (param instanceof Color) {
                if (param != null) {
                    final Color paramColor = (Color) param;
                    final JSONObject jsonColor = new JSONObject();
                    jsonColor.put("r", paramColor.getRed());
                    jsonColor.put("g", paramColor.getGreen());
                    jsonColor.put("b", paramColor.getBlue());
                    result = jsonColor;
                }
            } else {
                result = param;
            }
        }

        return result;
    }

    public static <T extends Object> T getObjectFromJSON(final Object o, final Class<T> type) {
        return getObjectFromJSON(o, type, null);
    }

    public static <T extends Object> T getObjectFromJSON(final Object o, final Class<T> type, final Object defaultValue) {
        Object result = defaultValue;

        if (o != null && !o.equals("null")) {
            if (type.equals(Integer.class)) {
                if (!o.getClass().isAssignableFrom(Long.class)) {
                    throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "'");
                } else {
                    try {
                        result = NumberUtils.ensureInt((Long) o);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "', " + ex.getMessage());
                    }
                }
            } else if (type.equals(Date.class)) {
                if (!o.getClass().isAssignableFrom(String.class)) {
                    throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "'");
                }
                final String sparam = (String) o;
                final Matcher matcher = pattern.matcher(sparam);
                if (matcher.find()) {
                    final Calendar c = DatatypeConverter.parseDateTime(sparam);
                    result = c.getTime();
                } else {
                    throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "', the format is not valid");
                }
            } else {
                if (!o.getClass().isAssignableFrom(type)) {
                    throw new IllegalArgumentException("object (" + o.getClass().getName() + ") is not assignable for '" + type + "'");
                }
                result = o;
            }
        } else {
            result = null;
        }

        return (T) result;
    }

    public static <T extends Object> T getParameterFromJSON(final JSONObject json, final String key, final Class<T> type) {
        return getParameterFromJSON(json, key, type, null);
    }

    public static <T extends Object> T getParameterFromJSON(final JSONObject json, final String key, final Class<T> type, Object defaultValue) {
        Object o = defaultValue;
        if (json.containsKey(key)) {
            o = json.get(key);
            if (o == null || o.equals("null")) {
                o = null;
            } else {
                if (type.equals(Integer.class)) {
                    if (!o.getClass().isAssignableFrom(Long.class)) {
                        throw new IllegalArgumentException("value  " + o + " for '" + key + "' is invalid");
                    } else {
                        try {
                            o = NumberUtils.ensureInt((Long) o);
                        } catch (IllegalArgumentException ex) {
                            throw new IllegalArgumentException("value  " + o + " for '" + key + "' is invalid, " + ex.getMessage());
                        }
                    }
                } else if (type.equals(Color.class)) {
                    if (!(o instanceof JSONObject)) {
                        throw new IllegalArgumentException("value  " + o + " for '" + key + "' is invalid");
                    }

                    final JSONObject jsonColor = (JSONObject) o;
                    final int r = (Integer) JSONConverter.getParameterFromJSON(jsonColor, "r", Integer.class);
                    final int g = (Integer) JSONConverter.getParameterFromJSON(jsonColor, "g", Integer.class);
                    final int b = (Integer) JSONConverter.getParameterFromJSON(jsonColor, "b", Integer.class);
                    o = new Color(r, g, b);
                } else {
                    if (!o.getClass().isAssignableFrom(type)) {
                        throw new IllegalArgumentException("value  " + o + " for '" + key + "' is invalid");
                    }
                }
            }
        }
        return (T) o;
    }

    public static JSONObject convertStringToJsonObject(final String jsonString) {
        final JSONParser parser = new JSONParser(JSONParser.USE_HI_PRECISION_FLOAT);
        final JSONObject json;
        try {
            json = (JSONObject) parser.parse(jsonString);
        } catch (final ParseException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        return json;
    }

    public static JSONArray convertStringToJsonArray(final String jsonString) {
        final JSONParser parser = new JSONParser(JSONParser.USE_HI_PRECISION_FLOAT);
        final JSONArray json;
        try {
            json = (JSONArray) parser.parse(jsonString);
        } catch (final ParseException ex) {
            throw new IllegalArgumentException(ex.getMessage(), ex);
        }
        return json;
    }

    // TODO move to another class
    public static boolean listInstanceOf(Object object, Class<?> c) {
        if (object instanceof List<?>) {
            final List<?> list = (List<?>) object;
            final int listSize = list.size();
            for (int i = 0; i < listSize; i++) {
                if (!c.isInstance(list.get(i))) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }
}
