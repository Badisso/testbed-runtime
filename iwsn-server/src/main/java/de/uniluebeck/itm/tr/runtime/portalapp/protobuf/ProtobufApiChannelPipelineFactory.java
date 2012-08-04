package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import static org.jboss.netty.channel.Channels.pipeline;

import de.uniluebeck.itm.tr.runtime.portalapp.SessionManagementService;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.protobuf.ProtobufDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufEncoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import org.jboss.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;


public class ProtobufApiChannelPipelineFactory implements ChannelPipelineFactory {

	private ProtobufApiService protobufApiService;

	private SessionManagementService sessionManagement;

	public ProtobufApiChannelPipelineFactory(final ProtobufApiService protobufApiService,
											 final SessionManagementService sessionManagement) {
		this.protobufApiService = protobufApiService;
		this.sessionManagement = sessionManagement;
	}

	@Override
	public ChannelPipeline getPipeline() throws Exception {

		ChannelPipeline p = pipeline();

		p.addLast("frameDecoder", new ProtobufVarint32FrameDecoder());
		p.addLast("protobufDecoder", new ProtobufDecoder(WisebedMessages.Envelope.getDefaultInstance()));

		p.addLast("frameEncoder", new ProtobufVarint32LengthFieldPrepender());
		p.addLast("protobufEncoder", new ProtobufEncoder());

		ProtobufApiChannelHandler handler = new ProtobufApiChannelHandler(
				protobufApiService,
				sessionManagement
		);

		p.addLast("handler", handler);

		return p;
	}
}
