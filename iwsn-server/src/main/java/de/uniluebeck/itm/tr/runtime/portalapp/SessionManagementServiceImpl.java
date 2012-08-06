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
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
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

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractService;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.assistedinject.Assisted;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.runtime.portalapp.protobuf.ProtobufDeliveryManager;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppModule;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import eu.wisebed.api.WisebedServiceHelper;
import eu.wisebed.api.rs.ConfidentialReservationData;
import eu.wisebed.api.rs.RS;
import eu.wisebed.api.rs.RSExceptionException;
import eu.wisebed.api.rs.ReservervationNotFoundExceptionException;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import eu.wisebed.wiseml.Setup;
import eu.wisebed.wiseml.WiseMLHelper;
import eu.wisebed.wiseml.Wiseml;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.uniluebeck.itm.tr.iwsn.common.SessionManagementHelper.createExperimentNotRunningException;

public class SessionManagementServiceImpl extends AbstractService implements SessionManagementService {

	private final WSNServiceHandleFactory wsnServiceHandleFactory;

	/**
	 * Job that is scheduled to clean up resources after a reservations end in time has been reached.
	 */
	private class CleanUpWSNInstanceJob implements Runnable {

		private List<SecretReservationKey> secretReservationKeys;

		public CleanUpWSNInstanceJob(List<SecretReservationKey> secretReservationKeys) {
			this.secretReservationKeys = secretReservationKeys;
		}

		@Override
		public void run() {
			try {
				free(secretReservationKeys);
			} catch (ExperimentNotRunningException_Exception expected) {
				// if user called free before this is expected
			} catch (UnknownReservationIdException_Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}


	/**
	 * The logger for this service.
	 */
	private static final Logger log = LoggerFactory.getLogger(SessionManagementService.class);

	/**
	 * The configuration object of this session management instance.
	 */
	@Nonnull
	private final SessionManagementServiceConfig config;

	/**
	 * An instance of a preconditions checker initiated with the URN prefix of this instance. Used for checking
	 * preconditions of the public Session Management API.
	 */
	@Nonnull
	private final SessionManagementPreconditions preconditions;

	/**
	 * Used to generate secure random IDs to append them to newly created WSN API instances.
	 */
	@Nonnull
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	/**
	 * The {@link TestbedRuntime} instance used to communicate with over the overlay
	 */
	@Nonnull
	private final TestbedRuntime testbedRuntime;

	/**
	 * Holds all currently instantiated WSN API instances that are not yet removed by {@link
	 * de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementService#free(java.util.List)}.
	 */
	@Nonnull
	private final Map<String, WSNServiceHandle> wsnInstances = new HashMap<String, WSNServiceHandle>();

	@Nonnull
	private final ScheduledExecutorService scheduler;

	@Nonnull
	private final Map<String, ScheduledFuture<?>> scheduledCleanUpWSNInstanceJobs =
			new HashMap<String, ScheduledFuture<?>>();

	@Inject
	SessionManagementServiceImpl(final TestbedRuntime testbedRuntime,
								 final WSNServiceHandleFactory wsnServiceHandleFactory,
								 @Assisted final SessionManagementServiceConfig config,
								 @Assisted final SessionManagementPreconditions preconditions,
								 @Assisted final ScheduledExecutorService scheduler) throws MalformedURLException {

		this.testbedRuntime = checkNotNull(testbedRuntime);
		this.wsnServiceHandleFactory = checkNotNull(wsnServiceHandleFactory);
		this.config = checkNotNull(config);
		this.preconditions = checkNotNull(preconditions);
		this.scheduler = checkNotNull(scheduler);
	}

	@Override
	protected void doStart() {
		log.debug("Starting session management service...");
		notifyStarted();
	}

	@Override
	protected void doStop() {

		try {

			log.debug("Stopping session management service...");

			synchronized (wsnInstances) {

				// copy key set to not cause ConcurrentModificationExceptions
				final Set<String> secretReservationKeys = new HashSet<String>(wsnInstances.keySet());

				for (String secretReservationKey : secretReservationKeys) {
					try {
						freeInternal(secretReservationKey);
					} catch (ExperimentNotRunningException_Exception e) {
						log.error("ExperimentNotRunningException while shutting down all WSN instances: " + e, e);
					}
				}
			}

			log.debug("Stopped session management service!");
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Nullable
	public WSNServiceHandle getWsnServiceHandle(@Nonnull final String secretReservationKey) {
		checkNotNull(secretReservationKey);
		return wsnInstances.get(secretReservationKey);
	}

	@Override
	public String getInstance(String secretReservationKey, String controller)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		log.debug("SessionManagementServiceImpl.getInstance({})", secretReservationKey);

		// check if wsnInstance already exists and return it if that's the case
		WSNServiceHandle wsnServiceHandleInstance;
		synchronized (wsnInstances) {

			wsnServiceHandleInstance = wsnInstances.get(secretReservationKey);

			if (wsnServiceHandleInstance != null) {

				if (!"NONE".equals(controller)) {
					log.debug("Adding new controller to the list: {}", controller);
					wsnServiceHandleInstance.getWsnService().addController(controller);
				}

				return wsnServiceHandleInstance.getWsnSoapService().getEndpointUrl().toString();
			}

			// no existing wsnInstance was found, so create new wsnInstance

			// query reservation system for reservation data if reservation system is to be used (i.e.
			// reservationEndpointUrl is not null)
			List<ConfidentialReservationData> confidentialReservationDataList;
			Set<String> reservedNodes = null;
			if (config.getReservationEndpointUrl() != null) {

				// integrate reservation system
				List<SecretReservationKey> keys = generateSecretReservationKeyList(secretReservationKey);
				confidentialReservationDataList = getReservationDataFromRS(keys);
				reservedNodes = new HashSet<String>();

				// since only one secret reservation key is allowed only one piece of confidential reservation data is expected 
				com.google.common.base.Preconditions.checkArgument(confidentialReservationDataList.size() == 1,
						"There must be exactly one secret reservation key as this is a single URN-prefix implementation."
				);

				// assure that wsnInstance creation doesn't happen before reservation time slot
				assertReservationIntervalMet(confidentialReservationDataList);

				ConfidentialReservationData data = confidentialReservationDataList.get(0);

				// convert all node URNs to lower case so that we can do easy string-based comparisons
				for (String nodeURN : data.getNodeURNs()) {
					reservedNodes.add(nodeURN.toLowerCase());
				}


				// assure that nodes are in TestbedRuntime
				assertNodesInTestbed(reservedNodes);


				//Creating delay for CleanUpJob
				long delay = data.getTo().toGregorianCalendar().getTimeInMillis() - System.currentTimeMillis();

				//stop and remove invalid instances after their expiration time
				synchronized (scheduledCleanUpWSNInstanceJobs) {

					final CleanUpWSNInstanceJob job = new CleanUpWSNInstanceJob(keys);
					final ScheduledFuture<?> schedule = scheduler.schedule(job, delay, TimeUnit.MILLISECONDS);

					scheduledCleanUpWSNInstanceJobs.put(secretReservationKey, schedule);
				}


			} else {
				log.info("Information: No reservation system found! All existing nodes will be used.");
			}

			URL wsnInstanceEndpointUrl;
			try {
				wsnInstanceEndpointUrl = new URL(config.getWsnInstanceBaseUrl() + secureIdGenerator.getNextId());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}

			final ImmutableSet<String> reservedNodesSet = reservedNodes == null ?
					null :
					ImmutableSet.<String>builder().add(reservedNodes.toArray(new String[reservedNodes.size()])).build();

			// TODO implement
			final ProtobufDeliveryManager protobufDeliveryManager =
					new ProtobufDeliveryManager(config.getMaximumDeliveryQueueSize());

			wsnServiceHandleInstance = createWsnServiceHandle(reservedNodes, wsnInstanceEndpointUrl, reservedNodesSet);

			// start the WSN instance
			try {

				wsnServiceHandleInstance.startAndWait();

			} catch (Exception e) {
				log.error("Exception while creating WSN API wsnInstance: " + e, e);
				throw new RuntimeException(e);
			}

			wsnInstances.put(secretReservationKey, wsnServiceHandleInstance);

			if (!"NONE".equals(controller)) {
				wsnServiceHandleInstance.getWsnService().addController(controller);
			}

			return wsnServiceHandleInstance.getWsnSoapService().getEndpointUrl().toString();

		}

	}

	private WSNServiceHandle createWsnServiceHandle(final Set<String> reservedNodes,
													final URL wsnInstanceEndpointUrl,
													final ImmutableSet<String> reservedNodesSet) {

		// De-serialize original WiseML and strip out all nodes that are not part of this reservation
		Wiseml wiseML = WiseMLHelper.deserialize(WiseMLHelper.readWiseMLFromFile(config.getWiseMLFilename()));
		List<Setup.Node> node = wiseML.getSetup().getNode();
		Iterator<Setup.Node> nodeIterator = node.iterator();

		while (nodeIterator.hasNext()) {
			Setup.Node currentNode = nodeIterator.next();
			if (!reservedNodes.contains(currentNode.getId())) {
				nodeIterator.remove();
			}
		}

		final ImmutableSet<String> servedUrnPrefixes = ImmutableSet.<String>builder().add(config.getUrnPrefix()).build();
		final WSNServiceConfig config = new WSNServiceConfig(reservedNodesSet, wsnInstanceEndpointUrl, wiseML);
		final WSNPreconditions preconditions = new WSNPreconditions(servedUrnPrefixes, reservedNodes);

		final Injector injector = Guice.createInjector(new WSNAppModule());
		final WSNApp wsnApp = injector.getInstance(WSNAppFactory.class).create(testbedRuntime, reservedNodesSet);

		final Injector wsnServiceInjector = Guice.createInjector(new WSNServiceModule());
		final WSNServiceFactory wsnServiceFactory = wsnServiceInjector.getInstance(WSNServiceFactory.class);
		final WSNService wsnService = wsnServiceFactory.create(config, preconditions);

		final WSNSoapService wsnSoapService = new WSNSoapService(wsnService, config);

		return wsnServiceHandleFactory.create(wsnService, wsnSoapService, wsnApp);
	}

	/**
	 * Checks if all reserved nodes are known to {@code testbedRuntime}.
	 *
	 * @param reservedNodes
	 * 		the set of reserved node URNs
	 */
	private void assertNodesInTestbed(Set<String> reservedNodes) {

		for (String node : reservedNodes) {

			boolean isLocal = testbedRuntime.getLocalNodeNameManager().getLocalNodeNames().contains(node);
			boolean isRemote = testbedRuntime.getRoutingTableService().getEntries().keySet().contains(node);

			if (!isLocal && !isRemote) {
				throw new RuntimeException("Node URN " + node + " unknown to testbed runtime environment.");
			}
		}
	}

	@Override
	public void free(List<SecretReservationKey> secretReservationKeyList)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		preconditions.checkFreeArguments(secretReservationKeyList);

		// extract the one and only relevant secret reservation key
		String secretReservationKey = secretReservationKeyList.get(0).getSecretReservationKey();

		log.debug("SessionManagementServiceImpl.free({})", secretReservationKey);

		freeInternal(secretReservationKey);

	}

	private void freeInternal(final String secretReservationKey) throws ExperimentNotRunningException_Exception {

		synchronized (scheduledCleanUpWSNInstanceJobs) {

			final ScheduledFuture<?> schedule = scheduledCleanUpWSNInstanceJobs.get(secretReservationKey);

			if (schedule != null) {
				schedule.cancel(true);
			}
		}

		synchronized (wsnInstances) {

			// search for the existing instance
			WSNServiceHandle wsnServiceHandleInstance = wsnInstances.get(secretReservationKey);

			// stop it if it is existing (it may have been freed before or its lifetime may have been reached)
			if (wsnServiceHandleInstance != null) {

				try {
					wsnServiceHandleInstance.stopAndWait();
				} catch (Exception e) {
					log.error("Error while stopping WSN service instance: " + e, e);
				}

				wsnInstances.remove(secretReservationKey);
				log.debug(
						"Removing WSNServiceHandle for WSN service endpoint {}. {} WSN service endpoints running.",
						wsnServiceHandleInstance.getWsnSoapService().getEndpointUrl(),
						wsnInstances.size()
				);

			} else {
				throw createExperimentNotRunningException(secretReservationKey);
			}

		}
	}

	private List<eu.wisebed.api.rs.SecretReservationKey> convert(
			List<SecretReservationKey> secretReservationKey) {

		List<eu.wisebed.api.rs.SecretReservationKey> retList =
				new ArrayList<eu.wisebed.api.rs.SecretReservationKey>(secretReservationKey.size());
		for (SecretReservationKey reservationKey : secretReservationKey) {
			retList.add(convert(reservationKey));
		}
		return retList;
	}

	private eu.wisebed.api.rs.SecretReservationKey convert(SecretReservationKey reservationKey) {
		eu.wisebed.api.rs.SecretReservationKey retSRK =
				new eu.wisebed.api.rs.SecretReservationKey();
		retSRK.setSecretReservationKey(reservationKey.getSecretReservationKey());
		retSRK.setUrnPrefix(reservationKey.getUrnPrefix());
		return retSRK;
	}

	/**
	 * Tries to fetch the reservation data from {@link de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementServiceConfig#getReservationEndpointUrl()}
	 * and returns the list of reservations.
	 *
	 * @param secretReservationKeys
	 * 		the list of secret reservation keys
	 *
	 * @return the list of reservations
	 *
	 * @throws UnknownReservationIdException_Exception
	 * 		if the reservation could not be found
	 */
	private List<ConfidentialReservationData> getReservationDataFromRS(
			List<SecretReservationKey> secretReservationKeys) throws UnknownReservationIdException_Exception {

		try {

			RS rsService = WisebedServiceHelper.getRSService(config.getReservationEndpointUrl().toString());
			return rsService.getReservation(convert(secretReservationKeys));

		} catch (RSExceptionException e) {
			String msg = "Generic exception occurred in the federated reservation system.";
			log.warn(msg + ": " + e, e);
			throw WisebedServiceHelper.createUnknownReservationIdException(msg, null, e);
		} catch (ReservervationNotFoundExceptionException e) {
			log.debug("Reservation was not found. Message from RS: {}", e.getMessage());
			throw WisebedServiceHelper.createUnknownReservationIdException(e.getMessage(), null, e);
		}

	}

	/**
	 * Checks the reservations' time intervals if they have already started or have already stopped and throws an
	 * exception
	 * if that's the case.
	 *
	 * @param reservations
	 * 		the reservations to check
	 *
	 * @throws ExperimentNotRunningException_Exception
	 * 		if now is not inside the reservations' time interval
	 */
	private void assertReservationIntervalMet(List<ConfidentialReservationData> reservations)
			throws ExperimentNotRunningException_Exception {

		for (ConfidentialReservationData reservation : reservations) {

			DateTime from = new DateTime(reservation.getFrom().toGregorianCalendar());
			DateTime to = new DateTime(reservation.getTo().toGregorianCalendar());

			if (from.isAfterNow()) {
				throw WisebedServiceHelper
						.createExperimentNotRunningException("Reservation time interval for node URNs " +
								Arrays.toString(reservation.getNodeURNs().toArray())
								+ " lies in the future.", null
						);
			}

			if (to.isBeforeNow()) {
				throw WisebedServiceHelper
						.createExperimentNotRunningException("Reservation time interval for node URNs " +
								Arrays.toString(reservation.getNodeURNs().toArray())
								+ " lies in the past.", null
						);
			}

		}

	}

	private List<SecretReservationKey> generateSecretReservationKeyList(String secretReservationKey) {

		List<SecretReservationKey> secretReservationKeyList = new LinkedList<SecretReservationKey>();

		SecretReservationKey key = new SecretReservationKey();
		key.setUrnPrefix(config.getUrnPrefix());
		key.setSecretReservationKey(secretReservationKey);

		secretReservationKeyList.add(key);

		return secretReservationKeyList;
	}
}
