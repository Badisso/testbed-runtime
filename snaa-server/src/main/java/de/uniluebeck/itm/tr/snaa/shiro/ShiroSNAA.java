/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote*
 *   products derived from this software without specific prior written permission.                                   *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.tr.snaa.shiro;

import static de.uniluebeck.itm.tr.snaa.SNAAHelper.assertAuthenticationCount;
import static de.uniluebeck.itm.tr.snaa.SNAAHelper.assertUrnPrefixServed;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.jws.WebParam;
import javax.jws.WebService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

import de.uniluebeck.itm.tr.snaa.SNAAHelper;
import de.uniluebeck.itm.tr.snaa.shiro.entity.UrnResourcegroups;
import de.uniluebeck.itm.tr.util.Logging;
import de.uniluebeck.itm.tr.util.TimedCache;
import eu.wisebed.api.v3.common.NodeUrn;
import eu.wisebed.api.v3.common.NodeUrnPrefix;
import eu.wisebed.api.v3.common.SecretAuthenticationKey;
import eu.wisebed.api.v3.common.UsernameNodeUrnsMap;
import eu.wisebed.api.v3.snaa.Action;
import eu.wisebed.api.v3.snaa.AuthenticationFault;
import eu.wisebed.api.v3.snaa.AuthenticationFault_Exception;
import eu.wisebed.api.v3.snaa.AuthenticationTriple;
import eu.wisebed.api.v3.snaa.AuthorizationResponse;
import eu.wisebed.api.v3.snaa.IsValidResponse;
import eu.wisebed.api.v3.snaa.SNAA;
import eu.wisebed.api.v3.snaa.SNAAFault_Exception;

/**
 * This authentication and authorization component is responsible for
 * <ol>
 * <li>authenticating users which intend to access nodes which urns' feature a certain prefix and
 * <li>authorizing their access to the nodes.
 * <li>
 * </ol>
 * The authentication and authorization is performed for a certain set of nodes. These nodes are
 * grouped by a shared uniform resource locator prefix.
 * 
 */
@WebService(endpointInterface = "eu.wisebed.api.snaa.SNAA", portName = "SNAAPort", serviceName = "SNAAService", targetNamespace = "http://testbed.wisebed.eu/api/snaa/v1/")
public class ShiroSNAA implements SNAA {

	static {
		Logging.setDebugLoggingDefaults();
	}

	/** Logs messages */
	private static final Logger log = LoggerFactory.getLogger(ShiroSNAA.class);

	/**
	 * Access authorization for users is performed for nodes which uniform resource locator starts
	 * with this prefix.
	 */
	protected final NodeUrnPrefix nodeUrnPrefix;

	/**
	 * A security component that can access application-specific security entities such as users,
	 * roles, and permissions to determine authentication and authorization operations.
	 */
	private final Realm realm;

	/** Used to generate {@link SecretAuthenticationKey}s */
	private final Random r = new SecureRandom();

	/**
	 * This cache keeps tack of all authenticated sessions for a certain amount of time.<br/>
	 * It maps a secret String which is only known to one authenticated user to the user's
	 * authentication details.<br/>
	 * This cache may be used to check whether the user was authenticated by this server recently
	 * whenever this secret String is provided.
	 */
	private final TimedCache<String, AuthenticationTriple> authenticatedSessions = new TimedCache<String, AuthenticationTriple>(30, TimeUnit.MINUTES);

	/** An object which provides access to the persisted groups of resources */
	private final UrnResourceGroupsDao urnResourceGroupsDAO;

	/**
	 * Constructor
	 * 
	 * @param securityManager
	 *            The Instance of the class which executes all security operations for <em>all</em>
	 *            Subjects across a single application.
	 * @param urnResourceGroupsDAO
	 *            DAO to access URN resource groups
	 * @param nodeUrnPrefix
	 *            Access authorization for users is performed for nodes which uniform resource
	 *            locator starts with this prefix.
	 */
	@Inject
	public ShiroSNAA(SecurityManager securityManager, UrnResourceGroupsDao urnResourceGroupsDAO, @Assisted NodeUrnPrefix nodeUrnPrefix) {
		Collection<Realm> realms = ((RealmSecurityManager) securityManager).getRealms();
		checkArgument(realms.size() == 1, "Too many realms configured");
		realm = realms.iterator().next();
		this.nodeUrnPrefix = nodeUrnPrefix;
		this.urnResourceGroupsDAO = urnResourceGroupsDAO;
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(
			@WebParam(name = "authenticationData", targetNamespace = "") List<AuthenticationTriple> authenticationTriples)
			throws AuthenticationFault_Exception, SNAAFault_Exception {

		assertAuthenticationCount(authenticationTriples, 1, 1);
		assertUrnPrefixServed(nodeUrnPrefix, authenticationTriples);

		AuthenticationTriple authenticationTriple = authenticationTriples.get(0);

		/* Authentication */
		Subject currentUser = SecurityUtils.getSubject();
		try {
			currentUser.login(new UsernamePasswordToken(authenticationTriple.getUsername(), authenticationTriple.getPassword()));
			currentUser.logout();
		} catch (AuthenticationException e) {
			AuthenticationFault fault = new AuthenticationFault();
			fault.setMessage("Wrong username and/or password");
			throw new AuthenticationFault_Exception("The user could not be authenticated: Wrong username and/or password.", fault, e);
		}

		String randomLongAsString = Long.toString(r.nextLong());
		authenticatedSessions.put(randomLongAsString, authenticationTriple);

		/* Create a secret authentication key for the authenticated user */
		SecretAuthenticationKey secretAuthenticationKey = new SecretAuthenticationKey();
		secretAuthenticationKey.setUrnPrefix(authenticationTriple.getUrnPrefix());
		secretAuthenticationKey.setSecretAuthenticationKey(randomLongAsString);
		secretAuthenticationKey.setUsername(authenticationTriple.getUsername());

		/* Return the single secret authentication key in a list (due to the federator) */
		List<SecretAuthenticationKey> keys = new ArrayList<SecretAuthenticationKey>(1);
		keys.add(secretAuthenticationKey);

		return keys;
	}

	@Override
	public IsValidResponse.ValidationResult isValid(
			@WebParam(name = "secretAuthenticationKey", targetNamespace = "") SecretAuthenticationKey secretAuthenticationKey)
			throws SNAAFault_Exception {

		// check whether the urn prefix associated to the key is served at all
		SNAAHelper.assertSAKUrnPrefixServed(nodeUrnPrefix, Lists.newArrayList(secretAuthenticationKey));

		// Get the session from the cache of authenticated sessions
		AuthenticationTriple authTriple = authenticatedSessions.get(secretAuthenticationKey.getSecretAuthenticationKey());

		IsValidResponse.ValidationResult result = new IsValidResponse.ValidationResult();

		if (authTriple == null) {
			result.setValid(false);
			result.setMessage("The provides secret authentication key is not found. It is either invalid or expired.");
		} else if (secretAuthenticationKey.getUsername() == null) {
			result.setValid(false);
			result.setMessage("The user name comprised in the secret authentication key must not be 'null'.");
		} else if (authTriple.getUsername() == null) {
			result.setValid(false);
			result.setMessage("The user name which was provided by the original authentication is not known.");
		} else if (!secretAuthenticationKey.getUsername().equals(authTriple.getUsername())) {
			result.setValid(false);
			result.setMessage("The user name which was provided by the original authentication does not match the one in the secret authentication key.");
		} else if (secretAuthenticationKey.getUrnPrefix() == null) {
			result.setValid(false);
			result.setMessage("The urn prefix comprised in the secret authentication key must not be 'null'.");
		} else if (!secretAuthenticationKey.getUrnPrefix().equals(authTriple.getUrnPrefix())) {
			result.setValid(false);
			result.setMessage("The urn prefix which was provided by the original authentication does not match the one in the secret authentication key.");
		} else {
			result.setValid(true);
		}

		return result;
	}

	@Override
	public AuthorizationResponse isAuthorized(
			@WebParam(name = "usernameNodeUrnsMapList", targetNamespace = "") List<UsernameNodeUrnsMap> usernameNodeUrnsMaps,
			@WebParam(name = "action", targetNamespace = "") Action action) throws SNAAFault_Exception {

		AuthorizationResponse authorizationResponse = new AuthorizationResponse();

		PrincipalCollection principals = new SimplePrincipalCollection(usernameNodeUrnsMaps.get(0).getUsername(), realm.getName());
		Subject subject = new Subject.Builder().principals(principals).buildSubject();

		Set<String> nodeGroups = getNodeGroupsForNodeURNs(usernameNodeUrnsMaps.get(0).getNodeUrns());

		// TODO:
		// (1) Map the provided node urns to a node group (e.g., EXPERIMENT_NODES)
		// (2) Concat the provided action and node type: "<action>:<node type>"
		// subject.isPermittedAll("WSN_FLASH_PROGRAMS:EXPERIMENT_NODES"))
		boolean isAuthorized = subject.isPermittedAll(action.name());
		subject.logout();
		authorizationResponse.setAuthorized(isAuthorized);

		return authorizationResponse;
	}

	/**
	 * Iterates over a collection or node urns and returns the groups of these nodes.
	 * 
	 * @param nodeUrns
	 *            A collection of node urns
	 * @return A set of those groups at least one of the provided node belongs to
	 */
	protected Set<String> getNodeGroupsForNodeURNs(final Collection<NodeUrn> nodeUrns) {

		Set<String> nodeGroups = new HashSet<String>();
		List<String> nodeUrnStringList = new ArrayList<String>();
		for (NodeUrn nodeUrn : nodeUrns) {
			nodeUrnStringList.add(nodeUrn.getPrefix().toString() + nodeUrn.getSuffix());
		}

		List<UrnResourcegroups> nodeUrnResourceGroups = urnResourceGroupsDAO.find();
		for (UrnResourcegroups grp : nodeUrnResourceGroups) {
			if (nodeUrnStringList.contains(grp.getId().getUrn())) {
				nodeGroups.add(grp.getId().getResourcegroup());
			}
		}

		return nodeGroups;
	}
}
