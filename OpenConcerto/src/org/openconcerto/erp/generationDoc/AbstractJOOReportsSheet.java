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
 
 package org.openconcerto.erp.generationDoc;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.erp.config.Gestion;
import org.openconcerto.erp.preferences.PrinterNXProps;
import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.odtemplate.Template;
import org.openconcerto.odtemplate.engine.OGNLDataModel;
import org.openconcerto.openoffice.ODSingleXMLDocument;
import org.jopendocument.link.Component;
import org.jopendocument.link.OOConnexion;
import org.openconcerto.sql.Configuration;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public abstract class AbstractJOOReportsSheet {
    private static final String defaultLocationTemplate = SpreadSheetGenerator.defaultLocationTemplate;
    protected final DateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy");
    protected final DateFormat dateFormat2 = new SimpleDateFormat("dd/MM/yy");
    protected final DateFormat yearFormat = new SimpleDateFormat("yyyy");
    private String year;
    protected String locationTemplate = TemplateNXProps.getInstance().getStringProperty("LocationTemplate");
    protected String templateId;
    private String printer;
    protected boolean askOverwriting = false;

    /**
     * @return une Map contenant les valeurs à remplacer dans la template
     */
    abstract protected Map createMap();

    abstract protected String getName();

    public String getFileName() {
        return getValidFileName(getName());
    }

    MetaDataSheet meta;

    /**
     * MetaData à inclure lors de la génération
     * 
     * @param meta
     */
    public void setMetaGeneration(MetaDataSheet meta) {
        this.meta = meta;
    }

    /**
     * MetaData à inclure lors de la génération
     * 
     * @param meta
     */

    public MetaDataSheet getMetaGeneration() {
        return this.meta;
    }

    /**
     * Remplace tous les caracteres non alphanumeriques (seul le _ est autorisé) par un -. Cela
     * permet d'avoir toujours un nom de fichier valide.
     * 
     * @param fileName nom du fichier à créer ex:FACTURE_2007/03/001
     * @return un nom fichier valide ex:FACTURE_2007-03-001
     */
    static String getValidFileName(String fileName) {
        final StringBuffer result = new StringBuffer(fileName.length());
        for (int i = 0; i < fileName.length(); i++) {
            char ch = fileName.charAt(i);

            // Si c'est un caractere alphanumerique
            if (Character.isLetterOrDigit(ch) || (ch == '_') || (ch == ' ')) {
                result.append(ch);
            } else {
                result.append('-');
            }
        }
        return result.toString();
    }

    public abstract String getDefaultTemplateID();

    public abstract String getDefaultLocationProperty();

    protected void init(String year, String templateId, String attributePrinter) {
        this.year = year;
        this.templateId = templateId;
        this.printer = PrinterNXProps.getInstance().getStringProperty(attributePrinter);
    }

    public final void generate(boolean print, boolean show, String printer) {
        generate(print, show, printer, false);
    }

    /**
     * Genere le document OO, le pdf, et ouvre le document OO
     * 
     * @param print
     * @param show
     * @param printer
     */
    public void generate(boolean print, boolean show, String printer, boolean overwrite) {

        if (this.locationTemplate.trim().length() == 0) {
            this.locationTemplate = defaultLocationTemplate;
        }
        try {

            String fileName = getFileName();
            final InputStream fileTemplate = TemplateManager.getInstance().getTemplate(this.templateId);
            File outputDir = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(getDefaultTemplateID());
            File fileOutOO = getDocumentFile();
            if (fileOutOO.exists() && overwrite) {
                if (this.askOverwriting) {
                    int answer = JOptionPane.showConfirmDialog(null, "Voulez vous écraser le document ?", "Remplacement d'un document", JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        SheetUtils.convertToOldFile(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete(), fileName, outputDir, fileOutOO, ".odt");
                    }
                } else {
                    SheetUtils.convertToOldFile(((ComptaPropsConfiguration) Configuration.getInstance()).getRootSociete(), fileName, outputDir, fileOutOO, ".odt");
                }
            }

            if (!fileOutOO.exists()) {
                fileOutOO.getParentFile().mkdirs();
                Template template = new Template(new BufferedInputStream(fileTemplate));

                // creation du document
                final Map createMap = createMap();
                OGNLDataModel model = new OGNLDataModel(createMap);

                model.putAll(createMap);

                final ODSingleXMLDocument createDocument = template.createDocument(model);
                if (this.meta != null) {
                    this.meta.applyTo(createDocument.getPackage().getMeta(true));
                }
                createDocument.saveToPackageAs(fileOutOO);

            }

            // ouverture de OO
            if (show || print) {

                try {
                    final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                    if (ooConnexion == null) {
                        return;
                    }
                    final Component doc = ooConnexion.loadDocument(fileOutOO, !show);

                    if (this.savePDF()) {
                        File pdfOutputDir = DocumentLocalStorageManager.getInstance().getPDFOutputDirectory(getDefaultTemplateID());
                        doc.saveToPDF(new File(pdfOutputDir, fileName + ".pdf"), "writer_pdf_Export");
                    }
                    if (print) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("Name", printer);
                        doc.printDocument(map);
                    }
                    if (!show) {
                        doc.close();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
                }

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            ExceptionHandler.handle("Impossible de trouver le modéle.", e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (org.openconcerto.odtemplate.TemplateException e) {
            e.printStackTrace();
        }
    }

    protected boolean savePDF() {
        return false;
    }

    public void showDocument() {
        File fileOutOO = getDocumentFile();
        if (fileOutOO.exists()) {
            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                ooConnexion.loadDocument(fileOutOO, false);
            } catch (LinkageError e) {
                JOptionPane.showMessageDialog(new JFrame(), "Merci d'installer OpenOffice ou LibreOffice");
            } catch (Exception e) {
                e.printStackTrace();
                ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
            }
        } else {
            generate(false, true, "");
        }
    }

    private File getDocumentFile() {
        File outputDir = DocumentLocalStorageManager.getInstance().getDocumentOutputDirectory(getDefaultTemplateID());
        return new File(outputDir, getFileName() + ".odt");
    }

    public void printDocument() {
        File fileOutOO = getDocumentFile();
        if (fileOutOO.exists()) {
            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                final Component doc = ooConnexion.loadDocument(fileOutOO, true);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("Name", printer);
                doc.printDocument(map);
                doc.close();
            } catch (LinkageError e) {
                JOptionPane.showMessageDialog(new JFrame(), "Merci d'installer OpenOffice ou LibreOffice");
            } catch (Exception e) {
                e.printStackTrace();
                ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
            }
        } else {
            generate(true, false, this.printer);
        }
    }

    public void fastPrintDocument() {
        final File f = getDocumentFile();
        if (!f.exists()) {
            generate(true, false, this.printer);
        } else {
            try {
                final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
                if (ooConnexion == null) {
                    return;
                }
                final Component doc = ooConnexion.loadDocument(f, true);
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("Name", this.printer);
                Map<String, Object> map2 = new HashMap<String, Object>();
                map2.put("CopyCount", 1);
                doc.printDocument(map, map2);
                doc.close();
            } catch (LinkageError e) {
                JOptionPane.showMessageDialog(new JFrame(), "Merci d'installer OpenOffice ou LibreOffice");
            } catch (Exception e) {

                ExceptionHandler.handle("Impossible de charger le document OpentOffice", e);
                e.printStackTrace();
            }
        }
    }

    protected String getInitiales(SQLRow row) {
        String init = "";
        if (row != null) {
            final String stringPrenom = row.getString("PRENOM");
            if (stringPrenom != null && stringPrenom.trim().length() != 0) {
                init += stringPrenom.trim().charAt(0);
            }
            final String stringNom = row.getString("NOM");
            if (stringNom != null && stringNom.trim().length() != 0) {
                init += stringNom.trim().charAt(0);
            }
        }
        return init;
    }

    public void exportToPdf() {
        // Export vers PDF
        final String fileName = getFileName();
        final File fileOutOO = getDocumentFile();
        final File outputPDFDirectory = DocumentLocalStorageManager.getInstance().getPDFOutputDirectory(this.templateId);
        final File fileOutPDF = new File(outputPDFDirectory, fileName + ".pdf");

        if (!fileOutOO.exists()) {
            generate(false, false, "");
        }
        try {
            final OOConnexion ooConnexion = ComptaPropsConfiguration.getOOConnexion();
            if (ooConnexion == null) {
                return;
            }
            final Component doc = ooConnexion.loadDocument(fileOutOO, true);
            doc.saveToPDF(fileOutPDF, "writer_pdf_Export");
            doc.close();
        } catch (LinkageError e) {
            JOptionPane.showMessageDialog(new JFrame(), "Merci d'installer OpenOffice ou LibreOffice");
        } catch (Exception e) {
            e.printStackTrace();
            ExceptionHandler.handle("Impossible de charger le document OpenOffice", e);
        }
        // Ouverture
        int result = JOptionPane.showOptionDialog(null, "Ouvrir le pdf ?", "Ouverture du PDF", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);

        if (result == JOptionPane.YES_OPTION) {
            Gestion.openPDF(fileOutPDF);
        } else {
            try {
                FileUtils.openFile(fileOutPDF.getParentFile());
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Impossible d'ouvrir le dossier : " + fileOutPDF.getParentFile() + ".");
            }
        }

    }

    public String getYear() {
        return year;
    }
}
