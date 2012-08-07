package de.uniluebeck.itm.tr.snaa.shiro.entity;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * Created by IntelliJ IDEA.
 * User: massel
 * Date: 12.06.12
 * Time: 10:59
 * To change this template use File | Settings | File Templates.
 */
public class UsersRolesEntityPK implements Serializable {
    private String roleName;

    @Id
    @Column(name = "ROLE_NAME")
    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    private String userName;

    @Id
    @Column(name = "USER_NAME")
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UsersRolesEntityPK that = (UsersRolesEntityPK) o;

        if (roleName != null ? !roleName.equals(that.roleName) : that.roleName != null) return false;
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = roleName != null ? roleName.hashCode() : 0;
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        return result;
    }
}
