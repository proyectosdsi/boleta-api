/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sv.gob.mined.boleta.ejb;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.multipdf.Splitter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import sv.gob.mined.boleta.api.model.CorreoDocente;
import sv.gob.mined.boleta.util.Constante;

/**
 *
 * @author misanchez
 */
@Stateless
@LocalBean
public class SeparacionBoletasFacade {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("parametros");

    @EJB
    public PersistenciaFacade persistenciaFacade;

    @Asynchronous
    public void separacion(String mesAnho, String codDepa) {
        File carpetaRoot = new File(RESOURCE_BUNDLE.getString("path_archivo"));
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmssSSS");
        int TIPO_ARCHIVO;

        for (File carpetaDepa : carpetaRoot.listFiles()) {
            if (carpetaDepa.isDirectory() && carpetaDepa.getName().equals(codDepa)) {
                Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.INFO, "SEPARACION - Hora de inicio + {0}", new Date());
                for (File carpetaPorFecha : carpetaDepa.listFiles()) {
                    if (carpetaPorFecha.isDirectory() && carpetaPorFecha.getName().equals(mesAnho)) {
                        try {
                            for (File archivoBoleta : carpetaPorFecha.listFiles()) {
                                if (archivoBoleta.isFile() && (archivoBoleta.getName().toUpperCase().contains("PDF"))) {
                                    //verificar el nombre del archivo
                                    if (archivoBoleta.getName().toLowerCase().contains("renta" + codDepa)) {
                                        TIPO_ARCHIVO = Constante.PDF_RENTA;
                                    } else if (archivoBoleta.getName().toLowerCase().contains("constancia" + codDepa)) {
                                        TIPO_ARCHIVO = Constante.PDF_CONSTANCIA;
                                    } else {
                                        TIPO_ARCHIVO = Constante.PDF_BOLETA_PAGO;
                                    }

                                    //consolidar boletas por docente
                                    splitPages(archivoBoleta, codDepa, mesAnho, RESOURCE_BUNDLE.getString("path_archivo"), sdf, TIPO_ARCHIVO);

                                    File folderArchivoOriginal = new File(RESOURCE_BUNDLE.getString("path_archivo") + File.separator + codDepa + File.separator + mesAnho + File.separator + "archivo_original" + File.separator);
                                    if (!folderArchivoOriginal.exists()) {
                                        folderArchivoOriginal.mkdir();
                                    }
                                    Path temp = Files.move(Paths.get(archivoBoleta.getAbsolutePath()),
                                            Paths.get(folderArchivoOriginal.getAbsolutePath() + File.separator + archivoBoleta.getName()), StandardCopyOption.REPLACE_EXISTING);
                                }
                            }
                            Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.INFO, "Unificación de boletas - Hora de fin + {0}", new Date());
                            //unificar boletas en un solo archivo por docente
                            unirBoletasUnSolaArchivo(carpetaPorFecha);
                        } catch (IOException ex) {
                            Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.SEVERE, "Mensaje: {0}", ex.getMessage());
                            Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.SEVERE, "Causa: {0}", ex.getCause());
                            Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.SEVERE, "Clase: {0}", ex.getClass().getName());
                        }
                    }
                }
                Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.INFO, "SEPARACION - Hora de fin + {0}", new Date());
            }
        }
    }

    private void unirBoletasUnSolaArchivo(File carpetaPorFecha) throws FileNotFoundException, IOException {
        File[] lstPDf = carpetaPorFecha.listFiles();
        Arrays.sort(lstPDf);

        for (File carpetaDocente : lstPDf) {
            if (carpetaDocente.isDirectory()
                    && !carpetaDocente.getName().equals("procesado")
                    && !carpetaDocente.getName().equals("archivo_original")
                    && !carpetaDocente.getName().equals("DOCENTE_SIN_NIP")
                    && !carpetaDocente.getName().equals("errores")
                    && !carpetaDocente.getName().equals("no_encontrado")) {
                PDFMergerUtility PDFmerger = new PDFMergerUtility();
                PDFmerger.setDestinationFileName(carpetaPorFecha.getPath() + File.separator + carpetaDocente.getName() + ".pdf");

                File[] files = carpetaDocente.listFiles();

                Arrays.sort(files, NameFileComparator.NAME_COMPARATOR);

                for (File boleta : files) {
                    PDFmerger.addSource(boleta);
                }

                PDFmerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());

                for (File boleta : carpetaDocente.listFiles()) {
                    boleta.delete();
                }
                carpetaDocente.delete();
            }
        }

    }

    private void splitPages(File file, String codDepa, String mesAnho, String path, SimpleDateFormat sdf, int tipoArchivo) {
        PDDocument document = null;

        int interacion;
        int indexPosicion;
        int contadorDeCortes;
        int siguienteInteracion = 0;

        String nipTemp = "";
        String cadenaDeBusqueda;

        List<CorreoDocente> lstCorreo = new ArrayList();

        if (tipoArchivo == 1) {
            lstCorreo = persistenciaFacade.getLstCorreoDocentes();
        }

        try {
            document = PDDocument.load(file);
            Splitter splitter = new Splitter();
            splitter.setStartPage(1);

            if (document.getNumberOfPages() > 1000) {
                siguienteInteracion = 1000;
                splitter.setEndPage(siguienteInteracion);

                interacion = ((int) (document.getNumberOfPages() / 1000)) + 1;
            } else {
                splitter.setEndPage(document.getNumberOfPages());
                interacion = 1;
            }

            switch (tipoArchivo) {
                case Constante.PDF_RENTA:
                    cadenaDeBusqueda = "DURANTE EL PERIODO";
                    indexPosicion = 15;
                    break;
                case Constante.PDF_CONSTANCIA:
                    cadenaDeBusqueda = " en mi";
                    indexPosicion = 7;
                    break;
                default:
                    cadenaDeBusqueda = "         )";
                    indexPosicion = 15;
                    break;
            }

            do {
                interacion--;

                for (PDDocument pd : splitter.split(document)) {
                    String codTemp = "";

                    try {
                        codTemp = getCodigoDocente(pd, cadenaDeBusqueda, 0, indexPosicion, file.getName(), nipTemp);

                        switch (tipoArchivo) {
                            case Constante.PDF_RENTA:
                            case Constante.PDF_CONSTANCIA:
                                if (tipoArchivo == Constante.PDF_CONSTANCIA) {

                                } else {
                                    codTemp = codTemp.substring(0, 4).concat("-").
                                            concat(codTemp.substring(4, 10)).concat("-").
                                            concat(codTemp.substring(10, 13)).concat("-").
                                            concat(codTemp.substring(13, 14));
                                    codTemp = getNipByNit(codTemp, lstCorreo);
                                }

                                //se encontro el NIP
                                if (codTemp != null && !codTemp.isEmpty() && !codTemp.equals("DOCENTE_SIN_NIP")) {
                                    //Si es la primera iteración, se hace respalda el NIP encontrado para futuras compraciones
                                    if (nipTemp.isEmpty()) {
                                        nipTemp = codTemp;

                                        File carpetaCodigo = new File(path + File.separator + codDepa + File.separator + mesAnho + File.separator + nipTemp);
                                        if (!carpetaCodigo.exists()) {
                                            carpetaCodigo.mkdir();
                                        }

                                        pd.save(path + File.separator + codDepa + File.separator + mesAnho + File.separator + nipTemp + File.separator + sdf.format(new Date()) + ".pdf");
                                        //pdTemporal = pdfClone.getDestination();
                                    } else {//Se compara el NIP actual con el NIP bandera
                                        if (nipTemp.equals(codTemp)) {
                                            //El NIP es igual al NIP bandera, se agrega esta hoja al listado que se esta generando
                                        } else {
                                            //El NIP cambio, por lo tanto esta es una nueva hoja, se debe de mandar a persistir el listado 
                                            //del NIP anterior.
                                            nipTemp = codTemp;

                                            File carpetaCodigo = new File(path + File.separator + codDepa + File.separator + mesAnho + File.separator + nipTemp);
                                            if (!carpetaCodigo.exists()) {
                                                carpetaCodigo.mkdir();
                                            }
                                            //se limpia el listado y se agrega la hora actual
                                        }
                                        pd.save(path + File.separator + codDepa + File.separator + mesAnho + File.separator + nipTemp + File.separator + sdf.format(new Date()) + ".pdf");
                                    }
                                } else {
                                    nipTemp = "DOCENTE_SIN_NIP";
                                    File carpetaCodigo = new File(path + File.separator + codDepa + File.separator + mesAnho + File.separator + nipTemp);
                                    if (!carpetaCodigo.exists()) {
                                        carpetaCodigo.mkdir();
                                    }

                                    pd.save(path + File.separator + codDepa + File.separator + mesAnho + File.separator + nipTemp + File.separator + sdf.format(new Date()) + ".pdf");
                                }
                                break;
                            default:
                                //obtener codigo del empleado de la boleta
                                String codigo = codTemp.substring(8);

                                if (!codigo.isEmpty()) {
                                    //crear archivo con el codigo
                                    File carpetaCodigo = new File(path + File.separator + codDepa + File.separator + mesAnho + File.separator + codigo);
                                    if (!carpetaCodigo.exists()) {
                                        carpetaCodigo.mkdir();
                                    }

                                    //crear archivo pdf
                                    pd.save(path + File.separator + codDepa + File.separator + mesAnho + File.separator + codigo + File.separator + sdf.format(new Date()) + ".pdf");
                                }
                                break;
                        }

                    } catch (StringIndexOutOfBoundsException e) {
                        Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.SEVERE, "DEPA {0} - Error obteniendo el nip del docente{1}", new Object[]{codDepa, codTemp});
                    }

                    pd.close();
                }
                if (interacion > 0) {
                    contadorDeCortes = siguienteInteracion + 1;
                    siguienteInteracion = siguienteInteracion + 1000;
                    splitter = new Splitter();
                    splitter.setStartPage(contadorDeCortes);
                    if (document.getNumberOfPages() > siguienteInteracion) {
                        splitter.setEndPage(siguienteInteracion);
                    } else {
                        splitter.setEndPage(document.getNumberOfPages());
                    }
                }
            } while (interacion != 0);

            document.close();

        } catch (IOException ex) {
            try {
                if (document != null) {
                    document.close();
                }
            } catch (IOException ex1) {
                Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.SEVERE, "DEPA : {0} Error en el archivo: {1}", new Object[]{codDepa, file.getName()});
                Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.SEVERE, "Muy probablemente, este archivo no es de boletas de pagos");
                Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.SEVERE, "========== ERROR ==========", ex);
            }
        }
    }
    
    private String getCodigoDocente(PDDocument pDDocument, String strEndIdentifier, int offSet, int back, String nombreArchivo, String nipOld) throws IOException {
        String returnString;
        PDFTextStripper tStripper = new PDFTextStripper();
        tStripper.setStartPage(1);
        tStripper.setEndPage(1);
        String pdfFileInText = tStripper.getText(pDDocument);
        String strEnd = strEndIdentifier;
        int endInddex = pdfFileInText.indexOf(strEnd) + offSet;
        if (endInddex != -1) {
            int startInddex = endInddex - back;
            returnString = pdfFileInText.substring(startInddex, endInddex);
            if (returnString.contains("_______")) {
                returnString = "";
            }
        } else {
            returnString = nipOld;
        }

        return returnString;
    }
    
    private static String getNipByNit(String nit, List<CorreoDocente> lstCorreo) {
        Optional<CorreoDocente> correoDoc = lstCorreo.stream().parallel().
                filter(cDoc -> (cDoc.getNit() != null && cDoc.getNit().equals(nit))).findAny();
        if (correoDoc.isPresent()) {
            return correoDoc.get().getNip();
        } else {
            Logger.getLogger(SeparacionBoletasFacade.class.getName()).log(Level.INFO, "No esta el nip del NIT: {0}", nit);
            return "DOCENTE_SIN_NIP";
        }
    }
}
