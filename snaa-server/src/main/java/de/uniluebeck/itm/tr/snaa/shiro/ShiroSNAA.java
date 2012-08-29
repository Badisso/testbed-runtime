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

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import javax.jws.WebParam;
import javax.jws.WebService;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.config.IniSecurityManagerFactory;
import org.apache.shiro.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.jpa.JpaPersistModule;

import de.uniluebeck.itm.tr.snaa.ApplicationInitializer;
import de.uniluebeck.itm.tr.snaa.SNAAServer;
import de.uniluebeck.itm.tr.util.Logging;
import eu.wisebed.api.snaa.Action;
import eu.wisebed.api.snaa.AuthenticationExceptionException;
import eu.wisebed.api.snaa.AuthenticationTriple;
import eu.wisebed.api.snaa.SNAA;
import eu.wisebed.api.snaa.SNAAExceptionException;
import eu.wisebed.api.snaa.SecretAuthenticationKey;

@WebService(
		endpointInterface = "eu.wisebed.api.snaa.SNAA",
		portName = "SNAAPort",
		serviceName = "SNAAService",
		targetNamespace = "http://testbed.wisebed.eu/api/snaa/v1/"
)
public class ShiroSNAA implements SNAA {

    static {
        Logging.setDebugLoggingDefaults();
    }
    
	private Random r = new SecureRandom();

	public ShiroSNAA(String shiroConfigPath, String ehCachePath) {
	    // set up Shiro framework
	    Factory<org.apache.shiro.mgt.SecurityManager> factory = new IniSecurityManagerFactory(shiroConfigPath);
	    org.apache.shiro.mgt.SecurityManager securityManager = factory.getInstance();
	    SecurityUtils.setSecurityManager(securityManager);
	    
	   
	    
	   
	    
	    
	    
    }

    @Override
	public List<SecretAuthenticationKey> authenticate(
			@WebParam(name = "authenticationData", targetNamespace = "") List<AuthenticationTriple> authenticationData)
			throws AuthenticationExceptionException, SNAAExceptionException {

		List<SecretAuthenticationKey> keys = new ArrayList<SecretAuthenticationKey>(authenticationData.size());

		for (AuthenticationTriple triple : authenticationData) {
			SecretAuthenticationKey secretAuthenticationKey = new SecretAuthenticationKey();
			secretAuthenticationKey.setUrnPrefix(triple.getUrnPrefix());
			secretAuthenticationKey.setSecretAuthenticationKey(Long.toString(r.nextLong()));
			secretAuthenticationKey.setUsername(triple.getUsername());
			keys.add(secretAuthenticationKey);
		}

		return keys;
	}

	@Override
	public boolean isAuthorized(
			@WebParam(name = "authenticationData", targetNamespace = "") List<SecretAuthenticationKey> authenticationData,
			@WebParam(name = "action", targetNamespace = "") Action action)
			throws SNAAExceptionException {

		return true;
	}
}
