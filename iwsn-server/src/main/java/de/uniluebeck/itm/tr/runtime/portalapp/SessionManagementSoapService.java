package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.common.DeliveryManager;
import de.uniluebeck.itm.tr.iwsn.common.SessionManagementPreconditions;
import de.uniluebeck.itm.tr.runtime.wsnapp.UnknownNodeUrnsException;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.NetworkUtils;
import de.uniluebeck.itm.tr.util.SecureIdGenerator;
import de.uniluebeck.itm.tr.util.UrlUtils;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.sm.ExperimentNotRunningException_Exception;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.sm.SessionManagement;
import eu.wisebed.api.sm.UnknownReservationIdException_Exception;
import eu.wisebed.wiseml.WiseMLHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Holder;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@WebService(
		serviceName = "SessionManagementService",
		targetNamespace = "urn:SessionManagementService",
		portName = "SessionManagementPort",
		endpointInterface = "eu.wisebed.api.sm.SessionManagement"
)
public class SessionManagementSoapService extends AbstractService implements Service, SessionManagement {

	private static final Logger log = LoggerFactory.getLogger(SessionManagementSoapService.class);

	@Nonnull
	private final SessionManagementService sm;

	@Nonnull
	private final SessionManagementServiceConfig config;

	@Nonnull
	private final SessionManagementPreconditions preconditions;

	@Nonnull
	private final SecureIdGenerator secureIdGenerator = new SecureIdGenerator();

	@Nonnull
	private final DeliveryManager deliveryManager;

	@Nonnull
	private final WSNApp wsnApp;

	@Nonnull
	private final ScheduledExecutorService scheduledExecutorService;

	@Nullable
	private Endpoint endpoint;

	public SessionManagementSoapService(final SessionManagementService sm,
										final SessionManagementServiceConfig config,
										final SessionManagementPreconditions preconditions,
										final WSNApp wsnApp,
										final DeliveryManager deliveryManager,
										final ScheduledExecutorService scheduledExecutorService) {

		this.sm = checkNotNull(sm);
		this.config = checkNotNull(config);
		this.preconditions = checkNotNull(preconditions);
		this.wsnApp = checkNotNull(wsnApp);
		this.deliveryManager = checkNotNull(deliveryManager);
		this.scheduledExecutorService = checkNotNull(scheduledExecutorService);
	}

	@Override
	protected void doStart() {

		log.debug("Starting session management SOAP service on {}", config.getSessionManagementEndpointUrl());

		try {

			String bindAllInterfacesUrl = System.getProperty("disableBindAllInterfacesUrl") != null ?
					config.getSessionManagementEndpointUrl().toString() :
					UrlUtils.convertHostToZeros(config.getSessionManagementEndpointUrl().toString());

			log.info("Starting session management SOAP service on binding URL {} for endpoint URL {}",
					bindAllInterfacesUrl,
					config.getSessionManagementEndpointUrl().toString()
			);

			endpoint = Endpoint.publish(bindAllInterfacesUrl, this);

		} catch (MalformedURLException e) {
			notifyFailed(e);
		}

		try {
			deliveryManager.startAndWait();
		} catch (Exception e) {
			notifyFailed(e);
		}

		log.debug("Started session management SOAP service on {}", config.getSessionManagementEndpointUrl());
		notifyStarted();
	}

	@Override
	protected void doStop() {

		log.debug("Stopping session management SOAP service on {}", config.getSessionManagementEndpointUrl());

		try {
			deliveryManager.stopAndWait();
		} catch (Exception e) {
			log.error("Exception while shutting down delivery manager: ", e);
			notifyFailed(e);
		}

		try {
			if (endpoint != null) {
				try {
					endpoint.stop();
				} catch (NullPointerException expectedWellKnownBug) {
					// do nothing
				}
			}
		} catch (Exception e) {
			notifyFailed(e);
		}

		log.debug("Stopped session management SOAP service on {}!", config.getSessionManagementEndpointUrl());
		notifyStopped();
	}

	@Override
	public String areNodesAlive(@WebParam(name = "nodes", targetNamespace = "") final List<String> nodes,
								@WebParam(name = "controllerEndpointUrl", targetNamespace = "") final
								String controllerEndpointUrl) {

		preconditions.checkAreNodesAliveArguments(nodes, controllerEndpointUrl);

		log.debug("SessionManagementServiceImpl.checkAreNodesAlive({})", nodes);

		deliveryManager.addController(controllerEndpointUrl);
		final String requestId = secureIdGenerator.getNextId();

		try {
			wsnApp.areNodesAliveSm(new HashSet<String>(nodes), new WSNApp.Callback() {

				@Override
				public void receivedRequestStatus(WSNAppMessages.RequestStatus requestStatus) {
					deliveryManager.receiveStatus(TypeConverter.convert(requestStatus, requestId));
				}

				@Override
				public void failure(Exception e) {
					deliveryManager.receiveFailureStatusMessages(nodes, requestId, e, -1);
				}
			}
			);
		} catch (UnknownNodeUrnsException e) {
			deliveryManager.receiveUnknownNodeUrnRequestStatus(e.getNodeUrns(), e.getMessage(), requestId);
			deliveryManager.removeController(controllerEndpointUrl);
		}

		scheduledExecutorService.schedule(
				new Runnable() {
					@Override
					public void run() {
						deliveryManager.removeController(controllerEndpointUrl);
					}
				}, 10, TimeUnit.SECONDS
		);

		return requestId;
	}

	@Override
	public void free(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKey)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		sm.free(secretReservationKey);
	}

	@Override
	public void getConfiguration(
			@WebParam(name = "rsEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> rsEndpointUrl,
			@WebParam(name = "snaaEndpointUrl", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<String> snaaEndpointUrl,
			@WebParam(name = "options", targetNamespace = "", mode = WebParam.Mode.OUT) final
			Holder<List<KeyValuePair>> options) {

		if (config.getReservationEndpointUrl() != null) {
			rsEndpointUrl.value = config.getReservationEndpointUrl().toString();
		} else {
			rsEndpointUrl.value = "";
		}

		if (config.getSnaaEndpointUrl() != null) {
			snaaEndpointUrl.value = config.getSnaaEndpointUrl().toString();
		} else {
			snaaEndpointUrl.value = "";
		}
	}

	@Override
	public String getInstance(
			@WebParam(name = "secretReservationKey", targetNamespace = "") final
			List<SecretReservationKey> secretReservationKeys,
			@WebParam(name = "controller", targetNamespace = "") final String controller)
			throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception {

		preconditions.checkGetInstanceArguments(secretReservationKeys, controller);

		// check if controller endpoint URL is a valid URL and connectivity is given
		// (i.e. endpoint is not behind a NAT or firewalled)
		try {

			// the user may pass NONE to indicate the wish to not add a controller endpoint URL for now
			if (!"NONE".equals(controller)) {
				new URL(controller);
				try {
					NetworkUtils.checkConnectivity(controller);
				} catch (Exception e) {
					throw new RuntimeException("The testbed backend system could not connect to host/port of the given "
							+ "controller endpoint URL: \"" + controller + "\". Please make sure: \n"
							+ " 1) your host is not behind a firewall or the firewall is configured to allow incoming connections\n"
							+ " 2) your host is not behind a Network Address Translation (NAT) system or the NAT system is configured to forward incoming connections\n"
							+ " 3) the domain in the endpoint URL can be resolved to an IP address and\n"
							+ " 4) the Controller endpoint Web service is already started.\n"
							+ "\n"
							+ "The testbed backend system needs an implementation of the Wisebed APIs Controller "
							+ "Web service to run on the client side. It uses this as a feedback channel to deliver "
							+ "sensor node outputs to the client application.\n"
							+ "\n"
							+ "Please note: If this testbed runs the unofficial Protocol buffers based API you might "
							+ "try to use this method to connect to the testbed as it doesn't require a feedback "
							+ "channel but delivers the node output using the TCP connection initiated by the client."
					);
				}
			}

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		// extract the one and only relevant secretReservationKey
		String secretReservationKey = secretReservationKeys.get(0).getSecretReservationKey();

		return sm.getInstance(secretReservationKey, controller);
	}

	@Override
	public String getNetwork() {
		return WiseMLHelper.prettyPrintWiseML(WiseMLHelper.readWiseMLFromFile(config.getWiseMLFilename()));
	}
}
