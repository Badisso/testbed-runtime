package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by IntelliJ IDEA.
 * User: massel
 * Date: 12.06.12
 * Time: 10:59
 * To change this template use File | Settings | File Templates.
 */
@javax.persistence.IdClass(de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourcegroupsEntityPK.class)
@javax.persistence.Table(name = "URN_RESOURCEGROUPS", catalog = "enterprise_it_eit_books")
@Entity
public class UrnResourcegroupsEntity {
    private String urn;

    @javax.persistence.Column(name = "URN")
    @Id
    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    private String resourcegroup;

    @javax.persistence.Column(name = "RESOURCEGROUP")
    @Id
    public String getResourcegroup() {
        return resourcegroup;
    }

    public void setResourcegroup(String resourcegroup) {
        this.resourcegroup = resourcegroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UrnResourcegroupsEntity that = (UrnResourcegroupsEntity) o;

        if (resourcegroup != null ? !resourcegroup.equals(that.resourcegroup) : that.resourcegroup != null)
            return false;
        if (urn != null ? !urn.equals(that.urn) : that.urn != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = urn != null ? urn.hashCode() : 0;
        result = 31 * result + (resourcegroup != null ? resourcegroup.hashCode() : 0);
        return result;
    }
}
