/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sv.gob.mined.boleta.ejb;

import java.util.ResourceBundle;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

/**
 *
 * @author misanchez
 */
@Stateless
@LocalBean
public class EnvioDeBoletasFacade {
    private static final ResourceBundle RESOURCE_CUENTAS = ResourceBundle.getBundle("cuenta_office365");
    private static final ResourceBundle RESOURCE = ResourceBundle.getBundle("parametros");
}
