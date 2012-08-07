package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.Basic;
import javax.persistence.Entity;

/**
 * Created by IntelliJ IDEA.
 * User: massel
 * Date: 12.06.12
 * Time: 10:59
 * To change this template use File | Settings | File Templates.
 */
@javax.persistence.Table(name = "PERMISSIONS", catalog = "enterprise_it_eit_books")
@Entity
public class PermissionsEntity {
    private String roleName;

    @javax.persistence.Column(name = "ROLE_NAME")
    @Basic
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    private String actionName;

    @javax.persistence.Column(name = "ACTION_NAME")
    @Basic
    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    private String resourcegroupName;

    @javax.persistence.Column(name = "RESOURCEGROUP_NAME")
    @Basic
    public String getResourcegroupName() {
        return resourcegroupName;
    }

    public void setResourcegroupName(String resourcegroupName) {
        this.resourcegroupName = resourcegroupName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PermissionsEntity that = (PermissionsEntity) o;

        if (actionName != null ? !actionName.equals(that.actionName) : that.actionName != null) return false;
        if (resourcegroupName != null ? !resourcegroupName.equals(that.resourcegroupName) : that.resourcegroupName != null)
            return false;
        if (roleName != null ? !roleName.equals(that.roleName) : that.roleName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = roleName != null ? roleName.hashCode() : 0;
        result = 31 * result + (actionName != null ? actionName.hashCode() : 0);
        result = 31 * result + (resourcegroupName != null ? resourcegroupName.hashCode() : 0);
        return result;
    }
}
