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
import java.util.List;
import java.util.Random;

import javax.jws.WebParam;
import javax.jws.WebService;

import eu.wisebed.api.v3.common.NodeUrnPrefix;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;

import de.uniluebeck.itm.tr.snaa.SNAAHelper;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.AuthenticationExceptionException;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SNAAException;
import eu.wisebed.api.snaa.SNAAExceptionException;
import eu.wisebed.api.snaa.SecretAuthenticationKey;

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

	/**
	 * Access authorization for users is performed for nodes which uniform resource locator starts
	 * with this prefix.
	 */
	protected NodeUrnPrefix urnPrefix;

	/**
	 * A security component that can access application-specific security entities such as users,
	 * roles, and permissions to determine authentication and authorization operations.
	 */
	private final Realm realm;

	/** Used to generate {@link SecretAuthenticationKey}s*/
	private Random r = new SecureRandom();

	// ------------------------------------------------------------------------
	/**
	 * Constructor
	 * 
	 * @param realm
	 *            The security component that can access application-specific security entities such
	 *            as users, roles, and permissions to determine authentication and authorization
	 *            operations.
	 * @param urnPrefix
	 *            Access authorization for users is performed for nodes which uniform resource
	 *            locator starts with this prefix.
	 */
	public ShiroSNAA(Realm realm, NodeUrnPrefix urnPrefix) {
		this.realm = realm;
		this.urnPrefix = urnPrefix;
	}

	@Override
	public List<SecretAuthenticationKey> authenticate(
			@WebParam(name = "authenticationData", targetNamespace = "") List<AuthenticationTriple> authenticationData)
			throws AuthenticationExceptionException, SNAAExceptionException {

		assertAuthenticationCount(authenticationData, 1, 1);
		assertUrnPrefixServed(urnPrefix, authenticationData);

		AuthenticationTriple authenticationTriple = authenticationData.get(0);

		/* Authentication */
		Subject currentUser = SecurityUtils.getSubject();
		try {
			currentUser.login(new UsernamePasswordToken(authenticationTriple.getUsername(), authenticationTriple.getPassword()));
			currentUser.logout();
		} catch (AuthenticationException e) {
			throw new SNAAExceptionException("The user could not be authenticated: Wrong username and/or password.", new SNAAException(), e);
		}

		/* Create a secret authentication key for the authenticated user */
		SecretAuthenticationKey secretAuthenticationKey = new SecretAuthenticationKey();
		secretAuthenticationKey.setUrnPrefix(authenticationTriple.getUrnPrefix());
		secretAuthenticationKey.setSecretAuthenticationKey(Long.toString(r.nextLong()));
		secretAuthenticationKey.setUsername(authenticationTriple.getUsername());
		
		/* Return the single secret authentication key in a list (due to the federator) */
		List<SecretAuthenticationKey> keys = new ArrayList<SecretAuthenticationKey>(1);
		keys.add(secretAuthenticationKey);

		return keys;
	}

	@Override
	public boolean isAuthorized(@WebParam(name = "authenticationData", targetNamespace = "") List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "action", targetNamespace = "") Action action) throws SNAAExceptionException {

		SNAAHelper.assertAuthenticationKeyCount(authenticationData, 1, 1);
		SNAAHelper.assertSAKUrnPrefixServed(urnPrefix, authenticationData);

		PrincipalCollection principals = new SimplePrincipalCollection(authenticationData.get(0).getUsername(), realm.getName());
		Subject subject = new Subject.Builder().principals(principals).buildSubject();

		// TODO: After introducing wisebed API 3.0 node urns will be provided for an action.
		// (1) Map the provided node urns to a node group (e.g., EXPERIMENT_NODES)
		// (2) Concat the provided action and node type: "<action>:<node type>"
		// subject.isPermittedAll("WSN_FLASH_PROGRAMS:EXPERIMENT_NODES"))
		boolean isAuthorized = subject.isPermittedAll(action.getAction());
		subject.logout();

		return isAuthorized;
	}
}
