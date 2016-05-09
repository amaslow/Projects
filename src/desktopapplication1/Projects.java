/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package desktopapplication1;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

/**
 *
 * @author artur
 */
@Entity
@Table(name = "projects", catalog = "ELRO", schema = "")
@NamedQueries({
    @NamedQuery(name = "Projects.findAll", query = "SELECT p FROM Projects p"),
    @NamedQuery(name = "Projects.findByProject", query = "SELECT p FROM Projects p WHERE p.project = :project"),
    @NamedQuery(name = "Projects.findByOwner", query = "SELECT p FROM Projects p WHERE p.owner = :owner"),
    @NamedQuery(name = "Projects.findByDateStart", query = "SELECT p FROM Projects p WHERE p.dateStart = :dateStart"),
    @NamedQuery(name = "Projects.findByType", query = "SELECT p FROM Projects p WHERE p.type = :type"),
    @NamedQuery(name = "Projects.findByDateFinish", query = "SELECT p FROM Projects p WHERE p.dateFinish = :dateFinish"),
    @NamedQuery(name = "Projects.findByStatus", query = "SELECT p FROM Projects p WHERE p.status = :status"),
    @NamedQuery(name = "Projects.findByBuyer", query = "SELECT p FROM Projects p WHERE p.buyer = :buyer")})
public class Projects implements Serializable {
    @Transient
    private PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);
    private static final long serialVersionUID = 1L;
    @Id
    @Basic(optional = false)
    @Column(name = "PROJECT")
    private String project;
    @Basic(optional = false)
    @Column(name = "OWNER")
    private String owner;
    @Basic(optional = false)
    @Column(name = "DATE_START")
    @Temporal(TemporalType.DATE)
    private Date dateStart;
    @Basic(optional = false)
    @Column(name = "TYPE")
    private String type;
    @Column(name = "DATE_FINISH")
    @Temporal(TemporalType.DATE)
    private Date dateFinish;
    @Basic(optional = false)
    @Column(name = "STATUS")
    private String status;
    @Column(name = "BUYER")
    private String buyer;
    @OneToMany(mappedBy = "project")
    private List<Remarks> remarksList;

    public Projects() {
    }

    public Projects(String project) {
        this.project = project;
    }

    public Projects(String project, String owner, Date dateStart, String type, String status) {
        this.project = project;
        this.owner = owner;
        this.dateStart = dateStart;
        this.type = type;
        this.status = status;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        String oldProject = this.project;
        this.project = project;
        changeSupport.firePropertyChange("project", oldProject, project);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        String oldOwner = this.owner;
        this.owner = owner;
        changeSupport.firePropertyChange("owner", oldOwner, owner);
    }

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        Date oldDateStart = this.dateStart;
        this.dateStart = dateStart;
        changeSupport.firePropertyChange("dateStart", oldDateStart, dateStart);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        String oldType = this.type;
        this.type = type;
        changeSupport.firePropertyChange("type", oldType, type);
    }

    public Date getDateFinish() {
        return dateFinish;
    }

    public void setDateFinish(Date dateFinish) {
        Date oldDateFinish = this.dateFinish;
        this.dateFinish = dateFinish;
        changeSupport.firePropertyChange("dateFinish", oldDateFinish, dateFinish);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        String oldStatus = this.status;
        this.status = status;
        changeSupport.firePropertyChange("status", oldStatus, status);
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        String oldBuyer = this.buyer;
        this.buyer = buyer;
        changeSupport.firePropertyChange("buyer", oldBuyer, buyer);
    }

    public List<Remarks> getRemarksList() {
        return remarksList;
    }

    public void setRemarksList(List<Remarks> remarksList) {
        this.remarksList = remarksList;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (project != null ? project.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof Projects)) {
            return false;
        }
        Projects other = (Projects) object;
        if ((this.project == null && other.project != null) || (this.project != null && !this.project.equals(other.project))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "desktopapplication1.Projects[ project=" + project + " ]";
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }
    
}
