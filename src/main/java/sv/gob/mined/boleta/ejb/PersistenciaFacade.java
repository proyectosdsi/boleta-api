/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sv.gob.mined.boleta.ejb;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import sv.gob.mined.boleta.api.model.CodigoGenerado;
import sv.gob.mined.boleta.api.model.CorreoDocente;
import sv.gob.mined.boleta.api.model.DominiosCorreo;

/**
 *
 * @author misanchez
 */
@Stateless
@LocalBean
public class PersistenciaFacade {

    @PersistenceContext(unitName = "boletaPU")
    private EntityManager em;

    public List<CorreoDocente> getLstCorreoDocentes() {
        Query q = em.createQuery("SELECT c FROM CorreoDocente c ORDER BY c.idCorreo", CorreoDocente.class);
        return q.getResultList();
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public CodigoGenerado registrarFinDeProcesoDeEnvio(String pathRoot, String codigoDepartamento, String mesAnho) {
        CodigoGenerado codigoGenerado = getCodigoGenerado(codigoDepartamento, mesAnho);

        //calcular boletas no procesadas, no enviadas y enviadas
        File folderRoot = new File(pathRoot + File.separator + codigoDepartamento + File.separator + mesAnho + File.separator);
        int cantidadNoEncontrado = 0;
        int cantidadErrores = 0;
        int cantidadProcesado = 0;
        for (File folder : folderRoot.listFiles()) {
            if (folder.isDirectory()) {
                switch (folder.getName()) {
                    case "no_encontrado":
                        for (File listFile : folder.listFiles()) {
                            if (listFile.getName().toUpperCase().contains("PDF")) {
                                cantidadNoEncontrado += 1;
                            }
                        }
                        codigoGenerado.setSinCorreo(cantidadNoEncontrado);
                        break;
                    case "errores":
                        for (File listFile : folder.listFiles()) {
                            if (listFile.getName().toUpperCase().contains("PDF")) {
                                cantidadErrores += 1;
                            }
                        }
                        codigoGenerado.setError(cantidadErrores);
                        break;
                    case "procesado":
                        for (File listFile : folder.listFiles()) {
                            if (listFile.getName().toUpperCase().contains("PDF")) {
                                cantidadProcesado += 1;
                            }
                        }
                        codigoGenerado.setEnviado(cantidadProcesado);
                        break;
                }
            }
        }

        codigoGenerado.setFechaFin(new Date());

        em.merge(codigoGenerado);

        return codigoGenerado;
    }
    
    public CodigoGenerado getCodigoGenerado(String codigoDepartamento, String mesAnho) {
        Query q = em.createQuery("SELECT c FROM CodigoGenerado c WHERE c.codigoDepartamento=:codDepa and c.mesAnho=:mesAnho", CodigoGenerado.class);
        q.setParameter("codDepa", codigoDepartamento);
        q.setParameter("mesAnho", mesAnho);
        return q.getResultList().isEmpty() ? new CodigoGenerado() : (CodigoGenerado) q.getResultList().get(0);
    }
    
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public Integer getPkCodigoGeneradoByCodDepaAndMesAnho(String codDepa, String mesAnho) {
        CodigoGenerado codigoGenerado;

        Query q = em.createQuery("SELECT c FROM CodigoGenerado c WHERE c.codigoDepartamento=:codDepa AND c.mesAnho=:mesAnho", CodigoGenerado.class);
        q.setParameter("codDepa", codDepa);
        q.setParameter("mesAnho", mesAnho);
        if (q.getResultList().isEmpty()) {
            codigoGenerado = new CodigoGenerado();
            codigoGenerado.setMesAnho(mesAnho);
            codigoGenerado.setCodigoDepartamento(codDepa);
            codigoGenerado.setFechaInicio(new Date());
            em.persist(codigoGenerado);
            Logger.getLogger(PersistenciaFacade.class.getName()).log(Level.INFO, "{0} - {1}C\u00f3digo Generado: {2}", new Object[]{codDepa, mesAnho, codigoGenerado.getIdCodigo()});
        } else {
            codigoGenerado = (CodigoGenerado) q.getResultList().get(0);
        }
        return codigoGenerado.getIdCodigo();
    }
    
    public List<DominiosCorreo> getLstDominiosCorreo() {
        Query q = em.createQuery("SELECT d FROM DominiosCorreo d ORDER BY d.dominio", DominiosCorreo.class);
        return q.getResultList();
    }
    
    public List<CorreoDocente> getLstCorreoDocenteByCriterio(String criterio) {
        Query q = em.createQuery("SELECT c FROM CorreoDocente c WHERE c.nip like :nip or c.correoElectronico like :correo ORDER BY c.idCorreo", CorreoDocente.class);
        q.setParameter("nip", "%" + criterio + "%");
        q.setParameter("correo", "%" + criterio + "%");
        return q.getResultList();
    }
    
    public int guardarDocente(CorreoDocente correo, String usuario) {
        if (correo.getIdCorreo() == null) {
            Query q = em.createQuery("SELECT c FROM CorreoDocente c WHERE c.nip=:nip or c.correoElectronico=:correo", CorreoDocente.class);
            q.setParameter("nip", correo.getNip());
            q.setParameter("correo", correo.getCorreoElectronico());
            if (q.getResultList().isEmpty()) {
                correo.setFechaInsercion(new Date());
                correo.setUsuarioInsercion(usuario);
                em.persist(correo);
                return 1;
            } else {
                return 0;
            }
        } else {
            correo.setFechaModificacion(new Date());
            correo.setUsuarioModificacion(usuario);
            em.merge(correo);
            return 1;
        }
    }
   
}
