/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sv.gob.mined.boleta.api.model;

import java.io.Serializable;
import java.math.BigDecimal;
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

/**
 *
 * @author misanchez
 */
@Entity
@Table(name = "DETALLE_CODIGO", schema = "BOLETA")
@NamedQueries({
    @NamedQuery(name = "DetalleCodigo.findAll", query = "SELECT d FROM DetalleCodigo d")})
public class DetalleCodigo implements Serializable {

    private static final long serialVersionUID = 1L;
    // @Max(value=?)  @Min(value=?)//if you know range of your decimal fields consider using these annotations to enforce field validation
    @Id
    @Basic(optional = false)
    @Column(name = "ID_DETALLE_CODIGO")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "detalleCodigo")
    @SequenceGenerator(name = "detalleCodigo", sequenceName = "SEQ_DETALLE_COD", allocationSize = 1, initialValue = 1)
    private BigDecimal idDetalleCodigo;
    @Column(name = "ID_CODIGO")
    private Integer idCodigo;
    @Column(name = "NIP")
    private String nip;
    @Column(name = "CODIGO_GENERADO")
    private String codigoGenerado;

    public DetalleCodigo() {
    }

    public DetalleCodigo(BigDecimal idDetalleCodigo) {
        this.idDetalleCodigo = idDetalleCodigo;
    }

    public BigDecimal getIdDetalleCodigo() {
        return idDetalleCodigo;
    }

    public void setIdDetalleCodigo(BigDecimal idDetalleCodigo) {
        this.idDetalleCodigo = idDetalleCodigo;
    }

    public Integer getIdCodigo() {
        return idCodigo;
    }

    public void setIdCodigo(Integer idCodigo) {
        this.idCodigo = idCodigo;
    }

    public String getNip() {
        return nip;
    }

    public void setNip(String nip) {
        this.nip = nip;
    }

    public String getCodigoGenerado() {
        return codigoGenerado;
    }

    public void setCodigoGenerado(String codigoGenerado) {
        this.codigoGenerado = codigoGenerado;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (idDetalleCodigo != null ? idDetalleCodigo.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof DetalleCodigo)) {
            return false;
        }
        DetalleCodigo other = (DetalleCodigo) object;
        if ((this.idDetalleCodigo == null && other.idDetalleCodigo != null) || (this.idDetalleCodigo != null && !this.idDetalleCodigo.equals(other.idDetalleCodigo))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "sv.gob.mined.boleta.model.DetalleCodigo[ idDetalleCodigo=" + idDetalleCodigo + " ]";
    }
    
}
