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

package de.uniluebeck.itm.tr.iwsn.overlay.messaging.server;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import de.uniluebeck.itm.tr.iwsn.overlay.LocalNodeNameManager;
import de.uniluebeck.itm.tr.iwsn.overlay.TestbedRuntime;
import de.uniluebeck.itm.tr.iwsn.overlay.connection.*;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.MessageTools;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.Messages;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.event.MessageEventService;
import de.uniluebeck.itm.tr.iwsn.overlay.messaging.unreliable.UnreliableMessagingService;
import de.uniluebeck.itm.tr.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;


@Singleton
class MessageServerServiceImpl implements MessageServerService {

	private static final Logger log = LoggerFactory.getLogger(MessageServerServiceImpl.class);

	private static final long DEFAULT_RETRY_TIMEOUT = 30;

	private static final TimeUnit DEFAULT_RETRY_TIMEUNIT = TimeUnit.SECONDS;

	private final MessageEventService messageEventService;

	private final UnreliableMessagingService unreliableMessagingService;

	private final ScheduledExecutorService scheduler;

	private volatile boolean running;

	/**
	 * Maps a connection type (e.g. tcp, udp, ...) to a set of {@link de.uniluebeck.itm.tr.iwsn.overlay.connection.ServerConnectionFactory}
	 * instances that can create connections of the corresponding type.
	 */
	private ImmutableMap<String, ImmutableSet<ServerConnectionFactory>> serverConnectionFactories;

	/**
	 * Maps from a configuration tuple (containing type and address of the server connection) to the corresponding {@link
	 * de.uniluebeck.itm.tr.iwsn.overlay.connection.ServerConnection} instance. The server connection may both be bound or
	 * unbound.
	 */
	private final Map<Tuple<String, String>, ServerConnection> serverConnections =
			new HashMap<Tuple<String, String>, ServerConnection>();

	/**
	 * Maps from a configuration tuple (containing type and address of the server connection) to the corresponding filter
	 * chain to be applied to incoming messages.
	 */
	private final Map<Tuple<String, String>, MessageFilterChain> messageFilterChains =
			new HashMap<Tuple<String, String>, MessageFilterChain>();

	/**
	 * Maps from a given {@link de.uniluebeck.itm.tr.iwsn.overlay.connection.ServerConnection} instance to the set of
	 * currently open
	 * connections that were initiated by clients. Used to gracefully close the client connections if this service is
	 * stopped.
	 */
	private final Map<ServerConnection, Set<Connection>> openClientConnections =
			new HashMap<ServerConnection, Set<Connection>>();

	private final MessageTools.MessageCallback messageCallback = new MessageTools.MessageCallback() {
		public synchronized void receivedMessage(ServerConnection serverConnection, Connection conn, Messages.Msg msg) {

			// run message through filter
			Tuple<String, String> config =
					new Tuple<String, String>(serverConnection.getType(), serverConnection.getAddress());
			MessageFilterChain chain = messageFilterChains.get(config);

			if (chain != null) {
				msg = chain.filter(msg);
			}

			// only post event or forward if no filter dropped the message
			if (msg != null) {

				if (localNodeNameManager.getLocalNodeNames().contains(msg.getTo())) {

					messageEventService.received(msg);

				} else {

					log.debug("Forwarding message: {}", msg);
					unreliableMessagingService.sendAsync(msg);
					// unreliable messaging will generate event for sent
				}
			}
		}
	};

	/**
	 * It should not be necessary to bind the ThreadPoolExecutor to a maximum number of threads as it implicitly will
	 * only start as many threads as there are connections to this node in the overlay network.
	 */
	private final ExecutorService messageReaderThreadPool = Executors.newCachedThreadPool(
			new ThreadFactoryBuilder().setNameFormat("MessageServerService-MessageReaderThread %d").build()
	);

	private final LocalNodeNameManager localNodeNameManager;

	private final EventBus eventBus;

	@Inject
	public MessageServerServiceImpl(final MessageEventService messageEventService,
									final UnreliableMessagingService unreliableMessagingService,
									@Named(TestbedRuntime.INJECT_MESSAGE_SERVER_SCHEDULER)
									final ScheduledExecutorService scheduler,
									final Set<ServerConnectionFactory> serverConnectionFactories,
									final LocalNodeNameManager localNodeNameManager,
									final EventBus eventBus) {

		this.messageEventService = messageEventService;
		this.unreliableMessagingService = unreliableMessagingService;
		this.scheduler = scheduler;
		this.localNodeNameManager = localNodeNameManager;
		this.eventBus = eventBus;

		// remember ServerConnectionFactory instances according to type
		Map<String, Set<ServerConnectionFactory>> scfs = new HashMap<String, Set<ServerConnectionFactory>>();
		for (ServerConnectionFactory serverConnectionFactory : serverConnectionFactories) {
			Set<ServerConnectionFactory> list = scfs.get(serverConnectionFactory.getType());
			if (list == null) {
				list = new HashSet<ServerConnectionFactory>();
				scfs.put(serverConnectionFactory.getType(), list);
			}
			list.add(serverConnectionFactory);
		}
		// now transform them to immutable data structures to reflect the semantics as they wont change anyway
		ImmutableMap.Builder<String, ImmutableSet<ServerConnectionFactory>> mapBuilder =
				new ImmutableMap.Builder<String, ImmutableSet<ServerConnectionFactory>>();

		for (Map.Entry<String, Set<ServerConnectionFactory>> entry : scfs.entrySet()) {
			mapBuilder.put(entry.getKey(), ImmutableSet.copyOf(entry.getValue()));
		}

		this.serverConnectionFactories = mapBuilder.build();

	}

	@Override
	public synchronized void start() throws Exception {
		eventBus.register(this);
		running = true;
		openConnections();
	}

	private synchronized void openConnections() {

		for (Map.Entry<Tuple<String, String>, ServerConnection> entry : serverConnections.entrySet()) {
			if (entry.getValue() == null || !entry.getValue().isBound()) {
				new EstablishServerConnectionRunnable(entry.getKey()).run();
			}
		}
	}

	private synchronized ServerConnection tryCreateServerConnection(Tuple<String, String> config)
			throws IOException, ConnectionInvalidAddressException, ConnectionTypeUnavailableException {

		String type = config.getFirst();
		String address = config.getSecond();

		log.debug("MessageServerServiceImpl.tryCreateServerConnection({}, {})", type, address);

		ImmutableSet<ServerConnectionFactory> connectionFactories = serverConnectionFactories.get(type);

		if (connectionFactories == null) {
			throw new ConnectionTypeUnavailableException(type);
		}

		ServerConnectionFactory serverConnectionFactory = connectionFactories.iterator().next();
		return serverConnectionFactory.create(address, eventBus);
	}

	@Override
	public synchronized void stop() {
		closeConnections();
		running = false;
		eventBus.unregister(this);
	}

	@Subscribe
	public synchronized void onServerConnectionOpenedEvent(ServerConnectionOpenedEvent event) {

		final String type = event.getServerConnection().getType();
		final String address = event.getServerConnection().getAddress();
		final Tuple<String, String> key = new Tuple<String, String>(type, address);

		serverConnections.put(key, event.getServerConnection());
	}

	@Subscribe
	public synchronized void onServerConnectionClosedEvent(ServerConnectionClosedEvent event) {

		final String type = event.getServerConnection().getType();
		final String address = event.getServerConnection().getAddress();
		final Tuple<String, String> key = new Tuple<String, String>(type, address);

		serverConnections.remove(key);
	}

	@Subscribe
	public synchronized void onConnectionAcceptedEvent(ConnectionAcceptedEvent event) {

		final ServerConnection serverConnection = event.getServerConnection();
		final Connection connection = event.getConnection();

		log.trace("MessageServerServiceImpl.onConnectionAcceptedEvent({}, {})", serverConnection, connection);
		messageReaderThreadPool.execute(new MessageTools.MessageReader(serverConnection, connection, messageCallback));

		Set<Connection> set = openClientConnections.get(serverConnection);
		if (set == null) {
			set = new HashSet<Connection>();
			openClientConnections.put(serverConnection, set);
		}
		set.add(connection);

	}

	@Subscribe
	public synchronized void onConnectionOpenedEvent(ConnectionOpenedEvent event) {
		// nothing to do, we only care about accepted incoming connection from which we read incoming messages
	}

	@Subscribe
	public synchronized void onConnectionClosedEvent(ConnectionClosedEvent event) {

		for (Map.Entry<ServerConnection, Set<Connection>> entry : openClientConnections.entrySet()) {
			for (Iterator<Connection> iterator = entry.getValue().iterator(); iterator.hasNext(); ) {
				Connection conn = iterator.next();
				if (conn == event.getConnection()) {
					iterator.remove();
				}
			}
		}
	}

	private boolean tryBindServerConnection(Tuple<String, String> config)
			throws ConnectionInvalidAddressException, ConnectionTypeUnavailableException {

		try {

			ServerConnection serverConnection = serverConnections.get(config);

			// if this is the first time we try to bind a server connection the instance must be null
			if (serverConnection == null) {
				// try to create the server connection
				serverConnection = tryCreateServerConnection(config);
			}

			// if there's already a server connection instance or construction succeeded try to bind it
			if (serverConnection != null) {

				// remember the instance
				serverConnections.put(config, serverConnection);

				// try to bind the server connection
				log.debug("Trying to bind serverConnection: {}", serverConnection);
				serverConnection.bind();
				log.debug("Successfully bound serverConnection: {}", serverConnection);

				// if server connection is not successfully bound we have to reschedule here
				return serverConnection.isBound();
			}

			return false;

		} catch (ConnectionTypeUnavailableException e) {
			log.warn("Unavailable connection type \"{}\" found while creating server connection! {}", e.getType());
			throw e;
		} catch (ConnectionInvalidAddressException e) {
			log.warn("Invalid address \"" + e.getAddress() + "\" found while creating server connection: " + e, e);
			throw e;
		} catch (IOException e) {
			log.info("IOException while binding ServerConnection!", e);
			return false;
		}
	}

	private class CloseServerConnectionRunnable implements Runnable {

		private Tuple<String, String> config;

		private CloseServerConnectionRunnable(Tuple<String, String> config) {
			this.config = config;
		}

		@Override
		public void run() {
			ServerConnection serverConnection = serverConnections.get(config);
			serverConnection.unbind();
		}
	}

	private class EstablishServerConnectionRunnable implements Runnable {

		private Tuple<String, String> config;

		private EstablishServerConnectionRunnable(Tuple<String, String> config) {
			this.config = config;
		}

		@Override
		public void run() {

			try {
				if (!tryBindServerConnection(config)) {
					reschedule();
				}
			} catch (ConnectionInvalidAddressException e) {
				// errors were logged before so exit
			} catch (ConnectionTypeUnavailableException e) {
				// errors were logged before so exit
			}

		}

		private void reschedule() {
			MessageServerServiceImpl.this.scheduler.schedule(this, DEFAULT_RETRY_TIMEOUT, DEFAULT_RETRY_TIMEUNIT);
		}

	}

	@Override
	public synchronized void addMessageServer(String type, String address, MessageFilter... filters) {

		checkNotNull(type);
		checkNotNull(address);

		Tuple<String, String> config = new Tuple<String, String>(type, address);
		serverConnections.put(config, null);
		messageFilterChains.put(config, new MessageFilterChain(filters));

		if (running) {
			scheduler.schedule(new EstablishServerConnectionRunnable(config), 0, TimeUnit.MILLISECONDS);
		}

	}

	@Override
	public synchronized void removeMessageServer(String type, String address) {

		checkNotNull(type);
		checkNotNull(address);

		Tuple<String, String> config = new Tuple<String, String>(type, address);
		scheduler.schedule(new CloseServerConnectionRunnable(config), 0, TimeUnit.MILLISECONDS);

		// removal from serverConnections will happen through being notified by the connection itself
	}

	private synchronized void closeConnections() {

		final Collection<ServerConnection> serverConnections = newArrayList(this.serverConnections.values());
		final Collection<Set<Connection>> clientConnections = newArrayList(openClientConnections.values());

		for (ServerConnection serverConnection : serverConnections) {
			serverConnection.unbind();
		}

		for (Set<Connection> set : clientConnections) {
			for (Connection connection : set) {
				connection.disconnect();
			}
		}
	}
}
