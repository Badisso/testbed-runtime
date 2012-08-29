package de.uniluebeck.itm.tr.snaa.shiro;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

import de.uniluebeck.itm.tr.snaa.shiro.entity.Permissions;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Roles;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Users;

public class TRJPARealm extends AuthorizingRealm {

	private static final Logger log = LoggerFactory.getLogger(TRJPARealm.class);
	private final Injector injector;

	public TRJPARealm() {
		Properties properties = new Properties();
		try {
			properties.load(this.getClass().getClassLoader().getResourceAsStream("META-INF/hibernate.properties"));
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
		injector = Guice.createInjector(new JpaPersistModule("Default").properties(properties));
		injector.getInstance(PersistService.class).start();
	}

	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
		GenericDao<Users, String> userDao = new GenericDaoImpl<Users, String>() {};
		injector.injectMembers(userDao);
		Users user = userDao.find(token.getUsername());
		if (user != null) {
			return new SimpleAuthenticationInfo(user.getName(), user.getPassword(), getName());
		} else {
			return null;
		}
	}

	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String userId = (String) principals.fromRealm(getName()).iterator().next();
		GenericDaoImpl<Users, String> userDao = new GenericDaoImpl<Users, String>() {};
		injector.injectMembers(userDao);
		Users user = userDao.find(userId);
		if (user != null) {
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			for (Roles role : user.getRoleses()) {
				info.addRole(role.getName());
				Set<Permissions> permissionses = role.getPermissionses();
				Set<String> strPerm = toString(permissionses);
				info.addStringPermissions(strPerm);
			}
			return info;
		} else {
			return null;
		}
	}

	private Set<String> toString(Set<Permissions> permissionses) {
		Set<String> result = new HashSet<String>();
		for (Permissions permissions : permissionses) {
			result.add(permissions.getActions().getName()+":"+permissions.getResourcegroups().getName());
		}
		return result;
	}
	
	
	
	

}
