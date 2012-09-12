/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                  *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or promote *
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

package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.newoverlay.Testbed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

class WSNServiceHandleImpl extends AbstractService implements Service, WSNServiceHandle {

	private static final Logger log = LoggerFactory.getLogger(WSNServiceHandleImpl.class);

	private final Testbed testbed;

	private final WSNService wsnService;

	private final WSNSoapService wsnSoapService;

	@Inject
	WSNServiceHandleImpl(@Assisted final Testbed testbed,
						 @Assisted final WSNService wsnService,
						 @Assisted final WSNSoapService wsnSoapService) {

		this.testbed = checkNotNull(testbed);
		this.wsnService = checkNotNull(wsnService);
		this.wsnSoapService = checkNotNull(wsnSoapService);
	}

	@Override
	protected void doStart() {

		try {

			wsnService.startAndWait();
			wsnSoapService.startAndWait();

		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStarted();
	}

	@Override
	protected void doStop() {

		try {
			wsnSoapService.stopAndWait();
		} catch (Exception e) {
			log.error("Exception while stopping WSN SOAP Web service interface: ", e);
			notifyFailed(e);
		}

		try {
			wsnService.stopAndWait();
		} catch (Throwable e) {
			log.error("Exception while stopping WSN service: ", e);
			notifyFailed(e);
		}

		notifyStopped();
	}

	@Override
	public WSNService getWsnService() {
		return wsnService;
	}

	@Override
	public WSNSoapService getWsnSoapService() {
		return wsnSoapService;
	}

	@Override
	public Testbed getTestbed() {
		return testbed;
	}
}
