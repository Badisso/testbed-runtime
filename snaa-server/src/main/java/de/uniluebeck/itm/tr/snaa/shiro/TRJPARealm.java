package de.uniluebeck.itm.tr.snaa.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

import de.uniluebeck.itm.tr.snaa.shiro.entity.Roles;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Users;

public class TRJPARealm extends AuthorizingRealm {
    
    
    
    
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {
        UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
        // TODO: The entity manager has to be injected
        GenericDao<Users, String> userDao = new GenericDaoImpl<Users, String>() {};
        Users user = userDao.find(token.getUsername());
        if( user != null ) {
            return new SimpleAuthenticationInfo(user.getName(), user.getPassword(), getName());
        } else {
            return null;
        }
    }


    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        String userId = (String) principals.fromRealm(getName()).iterator().next();
        Users user = new GenericDaoImpl<Users, String>() {}.find(userId);
        if( user != null ) {
            SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
            for( Roles role : user.getRoleses() ) {
                info.addRole(role.getName());
//                info.addStringPermissions( role.getPermissionses() );
            }
            return info;
        } else {
            return null;
        }
    }

}
