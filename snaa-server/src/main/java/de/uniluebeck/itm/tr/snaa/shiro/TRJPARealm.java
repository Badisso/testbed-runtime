package de.uniluebeck.itm.tr.snaa.shiro;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

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
