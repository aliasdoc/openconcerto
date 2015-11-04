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

import java.math.BigDecimal;

public class JSONconverter {
    public static String getJSON(Object param) {
        final StringBuilder result = new StringBuilder();

        if (param instanceof JSONAble) {
            result.append(((JSONAble) param).toJSON());
        } else if (param instanceof Byte || param instanceof Integer || param instanceof Long || param instanceof Short || param instanceof Boolean) {
            result.append(param.toString());
        } else if (param instanceof BigDecimal || param instanceof Float || param instanceof Double) {
            result.append(param.toString().replace(",", "."));
        } else if (param instanceof Character || param instanceof String) {
            result.append("\"" + param.toString().replace("\"", "\\\"").replace("'", "\\'").replace("\r\n", " ") + "\"");
        } else if (param instanceof Class<?>) {
            result.append("\"" + param.getClass().getName() + "\"");
        } else if (param instanceof Iterable) {
            final Iterable<?> tmp = (Iterable<?>) param;
            result.append("[");
            for (Object o : tmp) {
                result.append(getJSON(o) + ",");
            }
            if (result.length() > 1) {
                result.deleteCharAt(result.length() - 1);
            }
            result.append("]");
        } else if (param == null) {
            result.append("null");
        } else {
            throw new IllegalArgumentException("Type inconnue: " + param.getClass().getName());
        }

        return result.toString();
    }

    public static Object getObject(String param) {
        return null;
    }
}
