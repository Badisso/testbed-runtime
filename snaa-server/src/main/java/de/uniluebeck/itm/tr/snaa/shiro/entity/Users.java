package de.uniluebeck.itm.tr.snaa.shiro.entity;
// Generated 28.08.2012 17:24:47 by Hibernate Tools 3.2.2.GA


import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Table;

/**
 * Users generated by hbm2java
 */
@Entity
@Table(name="USERS"
    ,catalog="trauth"
)
public class Users  implements java.io.Serializable {


     private String name;
     private String password;
     private String salt;
     private Set<Roles> roleses = new HashSet<Roles>(0);

    public Users() {
    }

	
    public Users(String name) {
        this.name = name;
    }
    public Users(String name, String password, String salt, Set<Roles> roleses) {
       this.name = name;
       this.password = password;
       this.salt = salt;
       this.roleses = roleses;
    }
   
     @Id 
    
    @Column(name="NAME", unique=true, nullable=false, length=150)
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @Column(name="PASSWORD", length=1500)
    public String getPassword() {
        return this.password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    @Column(name="SALT", length=1500)
    public String getSalt() {
        return this.salt;
    }
    
    public void setSalt(String salt) {
        this.salt = salt;
    }
@ManyToMany(cascade=CascadeType.ALL, fetch=FetchType.LAZY)
    @JoinTable(name="USERS_ROLES", catalog="trauth", joinColumns = { 
        @JoinColumn(name="USER_NAME", nullable=false, updatable=false) }, inverseJoinColumns = { 
        @JoinColumn(name="ROLE_NAME", nullable=false, updatable=false) })
    public Set<Roles> getRoleses() {
        return this.roleses;
    }
    
    public void setRoleses(Set<Roles> roleses) {
        this.roleses = roleses;
    }




}

