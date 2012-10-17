package de.uniluebeck.itm.tr.snaa.shiro;

import java.util.HashSet;
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

import com.google.inject.Inject;

import de.uniluebeck.itm.tr.snaa.shiro.entity.Permissions;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Roles;
import de.uniluebeck.itm.tr.snaa.shiro.entity.Users;

/**
 * JPA based authorization realm used by Apache Shiro
 */
public class TRJPARealm extends AuthorizingRealm {

	/**
	 * Object use to access persisted user information
	 */
	@Inject
	private UsersDao usersDao;

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken authcToken) throws AuthenticationException {
		UsernamePasswordToken token = (UsernamePasswordToken) authcToken;
		Users user = usersDao.find(token.getUsername());
		if (user != null) {
			return new SimpleAuthenticationInfo(user.getName(), user.getPassword(), getName());
		}

		return null;

	}

	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		String userId = (String) principals.fromRealm(getName()).iterator().next();
		Users user = usersDao.find(userId);
		if (user != null) {
			SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();
			for (Roles role : user.getRoleses()) {
				info.addRole(role.getName());
				Set<Permissions> permissionses = role.getPermissionses();
				Set<String> strPerm = toString(permissionses);
				info.addStringPermissions(strPerm);
			}
			return info;
		}

		return null;
	}

	// ------------------------------------------------------------------------
	/**
	 * Converts a set of {@link Permissions} objects into a set of Strings and returns the result.
	 * 
	 * @param permissionses
	 *            A set of persisted permission objects which indicate which action is allowed for
	 *            which resource groups
	 * @return A set of permission stings which indicate which action is allowed for which resource
	 *         groups
	 */
	private Set<String> toString(final Set<Permissions> permissionses) {
		Set<String> result = new HashSet<String>();
		for (Permissions permissions : permissionses) {
			result.add(permissions.getActions().getName() + ":" + permissions.getResourcegroups().getName());
		}
		return result;
	}

}
