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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.iwsn.newoverlay.Overlay;
import de.uniluebeck.itm.tr.iwsn.newoverlay.OverlayEventBus;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.application.TestbedApplication;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufApiService;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.Portalapp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppModule;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppOverlayModule;
import de.uniluebeck.itm.tr.util.ExecutorUtils;
import de.uniluebeck.itm.tr.util.ForwardingScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;


public class PortalServerApplication extends AbstractService implements TestbedApplication {

	private static final Logger log = LoggerFactory.getLogger(PortalServerApplication.class);

	private final TestbedRuntime testbedRuntime;

	private final SessionManagementServiceConfig config;

	private final SessionManagementPreconditions preconditions;

	private final DeliveryManager deliveryManager;

	private ExecutorService executor;

	private ScheduledExecutorService scheduler;

	private ForwardingScheduledExecutorService forwardingScheduler;

	private SessionManagementService sessionManagementService;

	private SessionManagementSoapService sessionManagementSoapService;

	private ProtobufApiService protobufService;

	private Overlay overlay;

	private WSNApp wsnApp;

	public PortalServerApplication(final TestbedRuntime testbedRuntime, final Portalapp portalAppConfig) {

		this.testbedRuntime = testbedRuntime;

		this.config = new SessionManagementServiceConfig(portalAppConfig);

		this.preconditions = new SessionManagementPreconditions();
		this.preconditions.addServedUrnPrefixes(this.config.getUrnPrefix());
		this.preconditions.addKnownNodeUrns(this.config.getNodeUrnsServed());

		this.deliveryManager = new DeliveryManager();
	}

	@Override
	public String getName() {
		return PortalServerApplication.class.getSimpleName();
	}

	@Override
	protected void doStart() {

		scheduler = Executors.newScheduledThreadPool(1, buildThreadFactory("SessionManagement-Scheduler %d"));
		executor = Executors.newCachedThreadPool(buildThreadFactory("SessionManagement-Executor %d"));
		forwardingScheduler = new ForwardingScheduledExecutorService(scheduler, executor);

		try {

			final WSNAppFactory wsnAppFactory = Guice
					.createInjector(new WSNAppModule())
					.getInstance(WSNAppFactory.class);

			wsnApp = wsnAppFactory.create(testbedRuntime, config.getNodeUrnsServed());

		} catch (Exception e) {
			notifyFailed(e);
			return;
		}

		try {

			overlay = Guice
					.createInjector(new WSNAppOverlayModule(wsnApp))
					.getInstance(Overlay.class);

		} catch (Exception e) {
			notifyFailed(e);
			return;
		}

		final Injector injector = Guice.createInjector(
				new WSNAppOverlayModule(wsnApp),
				new PortalServerModule(scheduler)
		);

		final SessionManagementServiceFactory smServiceFactory =
				injector.getInstance(SessionManagementServiceFactory.class);

		final SessionManagementSoapServiceFactory smSoapServiceFactory =
				injector.getInstance(SessionManagementSoapServiceFactory.class);

		try {

			sessionManagementService = smServiceFactory.create(
					config,
					preconditions,
					forwardingScheduler
			);

			sessionManagementService.startAndWait();

		} catch (Exception e) {
			notifyFailed(e);
			return;
		}

		try {

			sessionManagementSoapService = smSoapServiceFactory.create(
					sessionManagementService,
					config,
					preconditions,
					deliveryManager,
					forwardingScheduler
			);

			sessionManagementSoapService.startAndWait();

		} catch (Exception e) {
			notifyFailed(e);
			return;
		}

		if (config.getProtobufinterface() != null) {

			try {

				protobufService = new ProtobufApiService(
						sessionManagementService,
						config.getProtobufinterface()
				);

				protobufService.startAndWait();

			} catch (Exception e) {
				notifyFailed(e);
				return;
			}
		}

		notifyStarted();
	}

	private ThreadFactory buildThreadFactory(final String nameFormat) {
		return new ThreadFactoryBuilder().setNameFormat(nameFormat).build();
	}

	@Override
	protected void doStop() {

		if (protobufService != null) {
			try {
				protobufService.stopAndWait();
			} catch (Exception e) {
				log.error("Exception while shutting down Session Management Protobuf service: {}", e);
				notifyFailed(e);
			}
		}

		try {
			sessionManagementSoapService.stopAndWait();
		} catch (Exception e) {
			log.error("Exception while shutting down Session Management SOAP web service: {}", e);
			notifyFailed(e);
		}

		try {
			sessionManagementService.stopAndWait();
		} catch (Exception e) {
			log.error("Exception while shutting down Session Management service: {}", e);
			notifyFailed(e);
		}

		try {
			wsnApp.stopAndWait();
		} catch (Exception e) {
			log.error("Exception while shutting down WSNApp: {}", e);
			notifyFailed(e);
		}

		try {
			overlay.stopAndWait();
		} catch (Exception e) {
			log.error("Exception while shutting down Overlay: {}", e);
			notifyFailed(e);
		}

		try {
			ExecutorUtils.shutdown(forwardingScheduler, 1, TimeUnit.SECONDS);
			ExecutorUtils.shutdown(scheduler, 1, TimeUnit.SECONDS);
			ExecutorUtils.shutdown(executor, 1, TimeUnit.SECONDS);
		} catch (Exception e) {
			notifyFailed(e);
		}

		notifyStopped();
	}
}
