/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package desktopapplication1;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.Date;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 *
 * @author artur
 */
@Entity
@Table(name = "remarks", catalog = "ELRO", schema = "")
@NamedQueries({
    @NamedQuery(name = "Remarks.findAll", query = "SELECT r FROM Remarks r"),
    @NamedQuery(name = "Remarks.findByDateRemark", query = "SELECT r FROM Remarks r WHERE r.dateRemark = :dateRemark"),
    @NamedQuery(name = "Remarks.findByRemark", query = "SELECT r FROM Remarks r WHERE r.remark = :remark"),
    @NamedQuery(name = "Remarks.findByRemarkId", query = "SELECT r FROM Remarks r WHERE r.remarkId = :remarkId")})
public class Remarks implements Serializable {
    @Transient
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private static final long serialVersionUID = 1L;
    @Column(name = "DATE_REMARK")
    @Temporal(TemporalType.DATE)
    private Date dateRemark;
    @Column(name = "REMARK")
    private String remark;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "REMARK_ID")
    private Integer remarkId;
    @JoinColumn(name = "PROJECT", referencedColumnName = "PROJECT")
    @ManyToOne
    private Projects project;

    public Remarks() {
    }

    public Remarks(Integer remarkId) {
        this.remarkId = remarkId;
    }

    public Date getDateRemark() {
        return dateRemark;
    }

    public void setDateRemark(Date dateRemark) {
        Date oldDateRemark = this.dateRemark;
        this.dateRemark = dateRemark;
        changeSupport.firePropertyChange("dateRemark", oldDateRemark, dateRemark);
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        String oldRemark = this.remark;
        this.remark = remark;
        changeSupport.firePropertyChange("remark", oldRemark, remark);
    }

    public Integer getRemarkId() {
        return remarkId;
    }

    public void setRemarkId(Integer remarkId) {
        Integer oldRemarkId = this.remarkId;
        this.remarkId = remarkId;
        changeSupport.firePropertyChange("remarkId", oldRemarkId, remarkId);
    }

    public Projects getProject() {
        return project;
    }

    public void setProject(Projects project) {
        Projects oldProject = this.project;
        this.project = project;
        changeSupport.firePropertyChange("project", oldProject, project);
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (remarkId != null ? remarkId.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Remarks)) {
            return false;
        }
        Remarks other = (Remarks) object;
        if ((this.remarkId == null && other.remarkId != null) || (this.remarkId != null && !this.remarkId.equals(other.remarkId))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "desktopapplication1.Remarks[ remarkId=" + remarkId + " ]";
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
}
