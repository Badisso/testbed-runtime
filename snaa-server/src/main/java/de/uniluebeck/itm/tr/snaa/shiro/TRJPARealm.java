package de.uniluebeck.itm.tr.snaa.shiro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.jdbc.JdbcRealm;

import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.JdbcUtils;

import de.uniluebeck.itm.tr.snaa.shiro.entity.UsersEntity;

public class TRJPARealm extends AuthorizingRealm {
    
    
    
    
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
//        UsersEntity user = userDAO.findUser(token.getUsername());
//        if( user != null ) {
//            return new SimpleAuthenticationInfo(user.getId(), user.getPassword(), getName());
//        } else {
            return null;
//        }
    }


    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        Long userId = (Long) principals.fromRealm(getName()).iterator().next();
//        UsersEntity user = userDAO.getUser(userId);
//        if( user != null ) {
//            SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
//            for( Role role : user.getRoles() ) {
//                info.addRole(role.getName());
//                info.addStringPermissions( role.getPermissions() );
//            }
//            return info;
//        } else {
            return null;
//        }
    }

}
