package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;


import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementService;
import de.uniluebeck.itm.tr.runtime.portalapp.xml.ProtobufInterface;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class ProtobufApiService extends AbstractService implements Service {

	private static final Logger log = LoggerFactory.getLogger(ProtobufApiService.class);

	private final ChannelGroup clientChannels = new DefaultChannelGroup();

	private SessionManagementService sessionManagement;

	private ProtobufInterface config;

	private ServerBootstrap bootstrap;

	private Channel serverChannel;

	public ProtobufApiService(SessionManagementService sessionManagement, ProtobufInterface config) {
		this.sessionManagement = sessionManagement;
		this.config = config;
	}

	@Override
	protected void doStart() {

		try {

			if (log.isInfoEnabled()) {
				log.info("Starting protocol buffer interface on {}:{}.",
						config.getHostname() != null ?
								config.getHostname() :
								config.getIp(),
						config.getPort()
				);
			}

			bootstrap = new ServerBootstrap(
					new NioServerSocketChannelFactory(
							Executors.newCachedThreadPool(),
							Executors.newCachedThreadPool()
					)
			);
			bootstrap.setPipelineFactory(new ProtobufApiChannelPipelineFactory(this, sessionManagement));
			serverChannel = bootstrap.bind(new InetSocketAddress(
					config.getHostname() != null ? config.getHostname() : config.getIp(),
					config.getPort()
			)
			);

			notifyStarted();

		} catch (Exception e) {
			notifyFailed(e);
		}
	}

	@Override
	protected void doStop() {

		try {

			serverChannel.close().await();
			bootstrap.releaseExternalResources();
			notifyStopped();

		} catch (Exception e) {
			notifyFailed(e);
		}

	}

	public ChannelGroup getClientChannels() {
		return clientChannels;
	}
}
