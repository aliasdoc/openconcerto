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
 
 package org.openconcerto.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author ilm
 * 
 */
public class Nombre {
    private int nb;

    static int[] puissanceMille = { 0, 1000, 1000000, 1000000000 };
    public static int FR = 0;
    public static int EN = 1;
    public static int ES = 2;
    public static int PL = 3;
    private final NombreLocal local;

    public Nombre(int i) {
        this(i, FR);
    }

    public Nombre(int i, int language) {
        this.nb = i;
        if (language == EN) {
            this.local = new NombreLocalEN();
        } else if (language == PL) {
            this.local = new NombreLocalPL();
        } else if (language == ES) {
            this.local = new NombreLocalES();
        } else {
            this.local = new NombreLocalFR();
        }
    }

    public Nombre(int i, NombreLocal local) {
        this.nb = i;
        this.local = local;
    }

    public String getText() {
        StringBuffer result = new StringBuffer();
        if (this.nb < 0)
            result.append(this.local.negatifLabel);

        if (this.local.getNombrePredefini(this.nb) != null) {
            result.append(this.local.getNombrePredefini(this.nb));
        } else if (this.nb < 100) {

            int decimal = this.nb / 10;
            int unit = this.nb % 10;

            result.append(this.local.getDizaine(decimal));

            if (unit > 0) { // trente, quarante..
                if (this.local.separateurDizaineLabel != null && this.local.separateurDizaineLabel.length() > 0) {
                    result.append(" " + this.local.separateurDizaineLabel);
                }
                result.append(" " + this.local.getNombrePredefini(unit));
            }

        } else {

            if (this.nb < 1000) {

                int cent = this.nb / 100;

                if (this.local.getNombrePredefini(cent * 100) != null) {
                    result.append(this.local.getNombrePredefini(cent * 100));
                } else {
                    result.append(this.local.getNombrePredefini(cent) + " " + this.local.getDizaine(10));
                }
                int reste = this.nb - (cent * 100);
                if (reste > 0) {
                    Nombre d = new Nombre(reste, this.local);
                    if (this.local.separateurDizaineLabel != null && this.local.separateurDizaineLabel.length() > 0) {
                        result.append(" " + this.local.separateurDizaineLabel);
                    }
                    result.append(" " + d.getText());
                }
            } else {

                int longueur = new Double(Math.ceil((String.valueOf(this.nb)).length() / 3.0)).intValue();

                int cumul = 0;
                for (int i = longueur - 1; i > 0; i--) {
                    int puissancei = puissanceMille[i];
                    int val = (this.nb - cumul) / puissancei;
                    if (val > 0) {
                        if (val > 1 && (i - 1) > 0) {

                            result.append(new Nombre(val, this.local).getText() + " " + this.local.getMult(i - 1) + "s ");
                        } else {
                            result.append(new Nombre(val, this.local).getText() + " " + this.local.getMult(i - 1) + " ");
                        }
                    }
                    cumul += val * puissancei;
                }

                int val = this.nb % 1000;
                if (val > 0) {
                    result.append(new Nombre(val, this.local).getText());
                }
            }
        }

        System.err.println(result.toString().trim());
        return result.toString().trim();
    }

    String getText(String r) {

        return String.valueOf(r);
    }

    public NombreLocal getLocal() {
        return local;
    }

    public class NombreLocal {
        protected final Map<Integer, String> nombrePredefini = new HashMap<Integer, String>();
        protected final List<String> dizaine = new ArrayList<String>();
        protected final List<String> mult = new ArrayList<String>();
        protected final String separateurLabel, separateurDizaineLabel, negatifLabel;

        public NombreLocal(String sep, String sepDizaine, String neg) {
            this.negatifLabel = neg;
            this.separateurLabel = sep;
            this.separateurDizaineLabel = sepDizaine;
        }

        public String getDizaine(int index) {
            return dizaine.get(index);
        }

        public String getMult(int index) {
            return mult.get(index);
        }

        public String getNombrePredefini(int value) {
            return nombrePredefini.get(value);
        }

        public String getSeparateurLabel() {
            return separateurLabel;
        }

    }

    private class NombreLocalFR extends NombreLocal {
        final String[] ref0 = { "zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix", "onze", "douze", "treize", "quatorze", "quinze", "seize", "dix sept", "dix huit",
                "dix neuf", "vingt" };
        final String[] ref10 = { "zéro", "dix", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre vingt", "quatre vingt", "cent" };
        final String[] refmult = { "mille", "million", "milliard", "billion", "trillion" };// 3,6,9,12,15

        public NombreLocalFR() {
            super("et", "", "moins");
            for (int i = 0; i < ref0.length; i++) {
                nombrePredefini.put(i, ref0[i]);
            }
            for (int i = 0; i < ref10.length; i++) {
                dizaine.add(ref10[i]);
            }
            for (int i = 0; i < refmult.length; i++) {
                mult.add(refmult[i]);
            }

            nombrePredefini.put(21, "vingt et un");
            nombrePredefini.put(31, "trente et un");
            nombrePredefini.put(41, "quarante et un");
            nombrePredefini.put(51, "cinquante et un");
            nombrePredefini.put(61, "soixante et un");

            nombrePredefini.put(70, "soixante dix");
            nombrePredefini.put(71, "soixante et onze");
            nombrePredefini.put(72, "soixante douze");
            nombrePredefini.put(73, "soixante treize");
            nombrePredefini.put(74, "soixante quatorze");
            nombrePredefini.put(75, "soixante quinze");
            nombrePredefini.put(76, "soixante seize");
            nombrePredefini.put(77, "soixante dix sept");
            nombrePredefini.put(78, "soixante dix huit");
            nombrePredefini.put(79, "soixante dix neuf");

            nombrePredefini.put(90, "quatre vingt dix");
            nombrePredefini.put(91, "quatre vingt onze");
            nombrePredefini.put(92, "quatre vingt douze");
            nombrePredefini.put(93, "quatre vingt treize");
            nombrePredefini.put(94, "quatre vingt quatorze");
            nombrePredefini.put(95, "quatre vingt quinze");
            nombrePredefini.put(96, "quatre vingt seize");
            nombrePredefini.put(97, "quatre vingt dix sept");
            nombrePredefini.put(98, "quatre vingt dix huit");
            nombrePredefini.put(99, "quatre vingt dix neuf");

            nombrePredefini.put(100, "cent");
        }
    }

    private class NombreLocalEN extends NombreLocal {
        final String[] ref0 = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen",
                "eighteen", "nineteen", "twenty" };
        final String[] ref10 = { "zero", "ten", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety", "hundred" };

        final String[] refmult = { "thousand", "million", "billion", "billion", "trillion" };// 3,6,9,12,15

        public NombreLocalEN() {
            super("and", "", "moins");
            for (int i = 0; i < ref0.length; i++) {
                nombrePredefini.put(i, ref0[i]);
            }
            for (int i = 0; i < ref10.length; i++) {
                dizaine.add(ref10[i]);
            }
            for (int i = 0; i < refmult.length; i++) {
                mult.add(refmult[i]);
            }
            nombrePredefini.put(100, "one hundred");
        }
    }

    private class NombreLocalPL extends NombreLocal {
        final String[] ref0 = { "zero", "jeden", "dwa", "trzy", "cztery", "pięć", "sześć", "siedem", "osiem", "dziewięć", "diesięć", "jedenaście", "dwanaście", "trzynaście", "czternaście",
                "piętnaście", "szesnaście", "siedemnaście", "osiemnaście", "dziewiętnaście", "dwadzieścia" };
        final String[] ref10 = { "zero", "dziesięć", "dwadzieścia", "trzydzieści", "czterdzieści", "pięćdziesiąt", "sześćdziesiąt", "siedemdziesiąt", "osiemdziesiąt", "dziewięćdziesiąt", "sto" };
        final String[] refmult = { "tysiąć", "milion", "miliard", "bilion", "trilion" };

        public NombreLocalPL() {
            super("i", "", "moins");
            for (int i = 0; i < ref0.length; i++) {
                nombrePredefini.put(i, ref0[i]);
            }
            for (int i = 0; i < ref10.length; i++) {
                dizaine.add(ref10[i]);
            }
            for (int i = 0; i < refmult.length; i++) {
                mult.add(refmult[i]);
            }
            nombrePredefini.put(100, "sto");
            nombrePredefini.put(200, "dwiescie");
            nombrePredefini.put(300, "trysta");
            nombrePredefini.put(400, "czterysta");
            nombrePredefini.put(500, "pięćdziesiąt");
            nombrePredefini.put(600, "seiscientos");
            nombrePredefini.put(700, "setecientos");
            nombrePredefini.put(800, "ochocientos");
            nombrePredefini.put(900, "novecientos");
        }
    }

    private class NombreLocalES extends NombreLocal {

        final String[] ref0 = { "cero", "uno", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho", "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis", "diecisiete",
                "dieciocho", "diecinueve", "veinte" };
        final String[] ref10 = { "cero", "diez", "veinte", "treinta", "cuarenta", "cincuenta", "sesenta", "setenta", "ochenta", "noventa", "cien" };
        final String[] refmult = { "mil", "millón", "millar", "billón", "trillón" };

        public NombreLocalES() {
            super("y", "y", "moins");
            for (int i = 0; i < ref0.length; i++) {
                nombrePredefini.put(i, ref0[i]);
            }
            for (int i = 0; i < ref10.length; i++) {
                dizaine.add(ref10[i]);
            }
            for (int i = 0; i < refmult.length; i++) {
                mult.add(refmult[i]);
            }
            nombrePredefini.put(100, "cien");
            nombrePredefini.put(21, "veintiuno");
            nombrePredefini.put(22, "veintidos");
            nombrePredefini.put(23, "veintitrés");
            nombrePredefini.put(24, "veinticuatro");
            nombrePredefini.put(25, "veinticinco");
            nombrePredefini.put(26, "veintiséis");
            nombrePredefini.put(27, "veintisiete");
            nombrePredefini.put(28, "veintiocho");
            nombrePredefini.put(29, "veintinueve");
            nombrePredefini.put(200, "doscientos");
            nombrePredefini.put(300, "trescientos");
            nombrePredefini.put(400, "cuatrocientos");
            nombrePredefini.put(500, "quinientos");
            nombrePredefini.put(600, "seiscientos");
            nombrePredefini.put(700, "setecientos");
            nombrePredefini.put(800, "ochocientos");
            nombrePredefini.put(900, "novecientos");
        }
    }
}
