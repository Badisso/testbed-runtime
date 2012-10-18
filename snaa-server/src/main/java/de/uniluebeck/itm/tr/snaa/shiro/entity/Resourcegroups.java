package de.uniluebeck.itm.tr.snaa.shiro.entity;
// Generated 28.08.2012 17:24:47 by Hibernate Tools 3.2.2.GA


import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * Resourcegroups generated by hbm2java
 */
@Entity
@Table(name="RESOURCEGROUPS"
    ,catalog="trauth"
)
public class Resourcegroups  implements java.io.Serializable {


     private String name;
     private Set<UrnResourcegroups> urnResourcegroupses = new HashSet<UrnResourcegroups>(0);
     private Set<Permissions> permissionses = new HashSet<Permissions>(0);

    public Resourcegroups() {
    }

	
    public Resourcegroups(String name) {
        this.name = name;
    }
    public Resourcegroups(String name, Set<UrnResourcegroups> urnResourcegroupses, Set<Permissions> permissionses) {
       this.name = name;
       this.urnResourcegroupses = urnResourcegroupses;
       this.permissionses = permissionses;
    }
   
     @Id 
    
    @Column(name="NAME", unique=true, nullable=false, length=40)
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="resourcegroups")
    public Set<UrnResourcegroups> getUrnResourcegroupses() {
        return this.urnResourcegroupses;
    }
    
    public void setUrnResourcegroupses(Set<UrnResourcegroups> urnResourcegroupses) {
        this.urnResourcegroupses = urnResourcegroupses;
    }
@OneToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY, mappedBy="resourcegroups")
    public Set<Permissions> getPermissionses() {
        return this.permissionses;
    }
    
    public void setPermissionses(Set<Permissions> permissionses) {
        this.permissionses = permissionses;
    }




}

