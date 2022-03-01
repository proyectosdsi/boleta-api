/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sv.gob.mined.boleta.ejb;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Asynchronous;
import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import sv.gob.mined.boleta.api.model.CodigoGenerado;
import sv.gob.mined.boleta.api.model.CorreoDocente;
import sv.gob.mined.utils.jsf.JsfUtil;

/**
 *
 * @author misanchez
 */
@Stateless
@LocalBean
public class LeerBoletasFacade {

    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle("parametros");
    @EJB
    private PersistenciaFacade persistenciaFacade;
    @EJB
    private EMailFacade eMailFacade;

    @Asynchronous
    public void enviarUnSoloCorreo(String codDepa, String mesAnho,
            Session mailSession, String usuario,
            String mensajeCorreo, String tituloCorreo) {
        enviarCorreo(codDepa, mesAnho, mailSession, usuario, mensajeCorreo, tituloCorreo);
    }
   
    @TransactionAttribute(value = TransactionAttributeType.NOT_SUPPORTED)
    private void enviarCorreo(String codDepa, String mesAnho,
            Session mailSession, String usuario,
            String mensajeCorreo, String tituloCorreo) {
        Transport transport = null;
        try {
            String nombreMesAnho = getNombreMes(mesAnho.split("_")[0]).concat(" de ").concat(mesAnho.split("_")[1]);
            String pathRoot = RESOURCE_BUNDLE.getString("path_archivo");
            File carpeta = new File(pathRoot + File.separator + codDepa + File.separator + mesAnho + File.separator);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");
            Boolean errorEnvioEmail;
            String email;

            if (mensajeCorreo != null && !mensajeCorreo.isEmpty()) {

            } else {
                mensajeCorreo = MessageFormat.format(RESOURCE_BUNDLE.getString("mail.message"), nombreMesAnho);
                tituloCorreo = "Boleta de Pago de " + nombreMesAnho;
            }

            List<CorreoDocente> lstCorreo = persistenciaFacade.getLstCorreoDocentes();

            File folderError = new File(pathRoot + File.separator + codDepa + File.separator + mesAnho + File.separator + "errores" + File.separator);
            if (!folderError.exists()) {
                folderError.mkdir();
            }
            File folderProcesado = new File(pathRoot + File.separator + codDepa + File.separator + mesAnho + File.separator + "procesado" + File.separator);
            if (!folderProcesado.exists()) {
                folderProcesado.mkdir();
            }
            File folderNoEncontrado = new File(pathRoot + File.separator + codDepa + File.separator + mesAnho + File.separator + "no_encontrado" + File.separator);
            if (!folderNoEncontrado.exists()) {
                folderNoEncontrado.mkdir();
            }

            persistenciaFacade.getPkCodigoGeneradoByCodDepaAndMesAnho(codDepa, mesAnho);

            transport = mailSession.getTransport();

            try {
                for (File boleta : carpeta.listFiles()) {
                    if (boleta.isFile() && boleta.getName().toUpperCase().contains("PDF")) {
                        email = getCorreoByNip(boleta.getName().toUpperCase().replace(".PDF", ""), lstCorreo);

                        if (email != null) {
                            errorEnvioEmail = eMailFacade.enviarMail(email, usuario, tituloCorreo, mensajeCorreo, nombreMesAnho, boleta, mailSession, transport);

                            Logger.getLogger(LeerBoletasFacade.class.getName()).log(Level.INFO, "{0} - {1}", new Object[]{email, boleta.getName().toUpperCase().replace(".PDF", "")});
                            if (!errorEnvioEmail) {
                                //bitacoraDeProcesoEJB.correoNoEnviadoPorErrorGenerado(codDepa, mesAnho, pathRoot, boleta.getName().toUpperCase().replace(".PDF", ""));
                                moverBoletaAOtraUbicacion(boleta, folderError);
                            } else {
                                //mover archivo procesado
                                moverBoletaAOtraUbicacion(boleta, folderProcesado);
                            }
                        } else {
                            //bitacoraDeProcesoEJB.escribirEmpleadoNoEncontrado(codDepa, mesAnho, pathRoot, boleta.getName().toUpperCase().replace(".PDF", ""));
                            moverBoletaAOtraUbicacion(boleta, folderNoEncontrado);
                            Logger.getLogger(LeerBoletasFacade.class.getName()).log(Level.WARNING, "No existe este empleado: {0}", boleta.getName().toUpperCase().replace(".PDF", ""));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Error desconocido");
            }

            CodigoGenerado codigoGenerado = persistenciaFacade.registrarFinDeProcesoDeEnvio(pathRoot, codDepa, mesAnho);

            StringBuilder sb = new StringBuilder();
            sb = sb.append("Se han enviado boletas de pago del departamento ").append(JsfUtil.getNombreDepartamentoByCodigo(codDepa)).append(".").append("<br/>")
                    .append("Hora de inicio: ").append(sdf.format(codigoGenerado.getFechaInicio())).append("<br/>");

            sb = sb.append("Hora de fin: ").append(sdf.format(codigoGenerado.getFechaFin())).append("<br/>");
            sb = sb.append("Número de boletas enviadas: ").append(codigoGenerado.getEnviado()).append("<br/>");
            sb = sb.append("Número de docente no encontrados: ").append(codigoGenerado.getSinCorreo()).append("<br/>");
            sb = sb.append("Número de correos no enviados debido a un error: ").append(codigoGenerado.getError()).append("<br/>");

            eMailFacade.enviarMailDeConfirmacion("Envio de boletas de pago", sb.toString(), usuario, mailSession, transport);
            
            transport.close();
        } catch (MessagingException ex) {
            Logger.getLogger(LeerBoletasFacade.class.getName()).log(Level.SEVERE, null, ex);
            if (transport != null && transport.isConnected()) {
                try {
                    transport.close();
                } catch (MessagingException ex1) {
                    Logger.getLogger(LeerBoletasFacade.class.getName()).log(Level.SEVERE, null, ex1);
                }
            }
        }
    }
    

    private String getCorreoByNip(String nip, List<CorreoDocente> lstCorreo) {
        Optional<CorreoDocente> correoDoc = lstCorreo.stream().parallel().
                filter(cDoc -> cDoc.getNip().equals(nip)).findAny();
        if (correoDoc.isPresent()) {
            return correoDoc.get().getCorreoElectronico();
        } else {
            return null;
        }
    }

    private void moverBoletaAOtraUbicacion(File boleta, File folder) {
        try {
            Path temp = Files.move(Paths.get(boleta.getAbsolutePath()),
                    Paths.get(folder.getAbsolutePath() + File.separator + boleta.getName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            Logger.getLogger(LeerBoletasFacade.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getNombreMes(String mes) {
        switch (mes) {
            case "01":
                return "Enero";
            case "02":
                return "Febrero";
            case "03":
                return "Marzo";
            case "04":
                return "Abril";
            case "05":
                return "Mayo";
            case "06":
                return "Junio";
            case "07":
                return "Julio";
            case "08":
                return "Agosto";
            case "09":
                return "Septiembre";
            case "10":
                return "Octubre";
            case "11":
                return "Noviembre";
            default:
                return "Diciembre";
        }
    }
}