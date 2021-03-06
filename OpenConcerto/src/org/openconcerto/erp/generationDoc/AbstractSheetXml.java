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
import org.jopendocument.link.Component;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StringUtils;
import org.openconcerto.utils.sync.SyncClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public abstract class AbstractSheetXml extends SheetXml {
    private File generatedOpenDocumentFile;

    public AbstractSheetXml(SQLRow row) {
        this.row = row;
    }

    @Override
    public final Future<SheetXml> createDocumentAsynchronous() {
        Callable<SheetXml> c = new Callable<SheetXml>() {
            @Override
            public SheetXml call() throws Exception {
                try {
                    String templateId = getTemplateId();
                    final String modeleFinal = templateId;

                    String langage = getRowLanguage() != null ? getRowLanguage().getString("CHEMIN") : null;
                    InputStream templateStream = TemplateManager.getInstance().getTemplate(templateId, langage, getType());
                    if (templateStream == null) {
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                JOptionPane.showMessageDialog(null, "Impossible de trouver le modèle " + modeleFinal + ". \n Le modèle par défaut sera utilisé!");
                            }
                        });
                        templateId = getDefaultTemplateId();
                    } else {
                        templateStream.close();
                    }
                    final OOgenerationXML oXML = new OOgenerationXML(AbstractSheetXml.this.row);
                    AbstractSheetXml.this.generatedOpenDocumentFile = oXML.createDocument(templateId, getType(), getDocumentOutputDirectory(), getValidFileName(getName()), getRowLanguage(),
                            AbstractSheetXml.this.getMetaGeneration());
                    if (isRefreshFormulasRequired()) {
                        final Component doc = ComptaPropsConfiguration.getOOConnexion().loadDocument(AbstractSheetXml.this.generatedOpenDocumentFile, true);
                        // Remove from code, better use the pref in LO
                        // doc.refreshFormulas();
                        doc.save();
                        doc.close();
                    }

                } catch (Exception e) {
                    DEFAULT_HANDLER.uncaughtException(null, e);
                    // rethrow exception so that the unsuspecting caller can use this as the
                    // original task
                    throw e;
                } catch (Throwable e) {
                    DEFAULT_HANDLER.uncaughtException(null, e);

                }
                return AbstractSheetXml.this;
            }
        };
        return runnableQueue.submit(c);
    }

    public String getType() {
        return null;
    }

    @Override
    public String getStoragePathP() {
        return StringUtils.firstUp(elt.getPluralName());
    }

    @Override
    public File getGeneratedFile() {
        ComptaPropsConfiguration config = ComptaPropsConfiguration.getInstanceCompta();

        final File outputDirectory = getDocumentOutputDirectory();
        try {
            FileUtils.mkdir_p(outputDirectory);
        } catch (IOException e1) {
            ExceptionHandler.handle("Impossible de créer le dossier " + outputDirectory.getAbsolutePath(), e1);
        }

        if (this.generatedOpenDocumentFile == null)
            this.generatedOpenDocumentFile = new File(outputDirectory, getValidFileName(getName()) + ".ods");
        if (config.isOnCloud()) {
            if (generatedOpenDocumentFile.exists()) {
                long t = generatedOpenDocumentFile.lastModified();
                if (System.currentTimeMillis() - t < 1000 * 10) {
                    return generatedOpenDocumentFile;
                }

            }

            String remotePath = config.getSocieteID() + "/" + getStoragePath();
            remotePath = remotePath.replace('\\', '/');
            final SyncClient client = new SyncClient("https://" + config.getStorageServer());

            client.setVerifyHost(false);

            try {

                System.out.println("AbstractSheet: getFromCloud: " + remotePath + " " + generatedOpenDocumentFile.getName() + " to " + outputDirectory.getAbsolutePath());
                client.retrieveFile(outputDirectory, remotePath, generatedOpenDocumentFile.getName(), config.getToken());
                generatedOpenDocumentFile.setLastModified(System.currentTimeMillis());
            } catch (FileNotFoundException e) {
                // Not an error
                System.out.println("AbstractSheet: getFromCloud: " + remotePath + " " + generatedOpenDocumentFile.getName() + " not found on server");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return generatedOpenDocumentFile;
    }
}
