/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sv.gob.mined.boleta.api.model;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

/**
 *
 * @author misanchez
 */
@Entity
@Table(name = "CODIGO_GENERADO", schema = "BOLETA")
@NamedQueries({
    @NamedQuery(name = "CodigoGenerado.findAll", query = "SELECT c FROM CodigoGenerado c")})
public class CodigoGenerado implements Serializable {

    @Column(name = "FECHA_INICIO")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaInicio;
    @Column(name = "FECHA_FIN")
    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaFin;
    @Column(name = "ENVIADO")
    private Integer enviado;
    @Column(name = "SIN_CORREO")
    private Integer sinCorreo;
    @Column(name = "ERROR")
    private Integer error;

    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "ID_CODIGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "codigoGenerado")
    @SequenceGenerator(name = "codigoGenerado", sequenceName = "SEQ_CODIGO_GENERADO", allocationSize = 1, initialValue = 1)
    private Integer idCodigo;
    @Column(name = "CODIGO_DEPARTAMENTO")
    private String codigoDepartamento;
    @Column(name = "MES_ANHO")
    private String mesAnho;

    public CodigoGenerado() {
    }

    public CodigoGenerado(Integer idCodigo) {
        this.idCodigo = idCodigo;
    }

    public Integer getIdCodigo() {
        return idCodigo;
    }

    public void setIdCodigo(Integer idCodigo) {
        this.idCodigo = idCodigo;
    }

    public String getCodigoDepartamento() {
        return codigoDepartamento;
    }

    public void setCodigoDepartamento(String codigoDepartamento) {
        this.codigoDepartamento = codigoDepartamento;
    }

    public String getMesAnho() {
        return mesAnho;
    }

    public void setMesAnho(String mesAnho) {
        this.mesAnho = mesAnho;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idCodigo != null ? idCodigo.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof CodigoGenerado)) {
            return false;
        }
        CodigoGenerado other = (CodigoGenerado) object;
        if ((this.idCodigo == null && other.idCodigo != null) || (this.idCodigo != null && !this.idCodigo.equals(other.idCodigo))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.gob.mined.boleta.model.CodigoGenerado[ idCodigo=" + idCodigo + " ]";
    }

    public Date getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(Date fechaIniicio) {
        this.fechaInicio = fechaIniicio;
    }

    public Date getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(Date fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getEnviado() {
        return enviado;
    }

    public void setEnviado(Integer enviado) {
        this.enviado = enviado;
    }

    public Integer getSinCorreo() {
        return sinCorreo;
    }

    public void setSinCorreo(Integer sinCorreo) {
        this.sinCorreo = sinCorreo;
    }

    public Integer getError() {
        return error;
    }

    public void setError(Integer error) {
        this.error = error;
    }

}
