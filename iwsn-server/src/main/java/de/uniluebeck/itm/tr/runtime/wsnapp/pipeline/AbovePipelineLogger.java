package de.uniluebeck.itm.tr.runtime.wsnapp.pipeline;

import com.google.common.base.Joiner;
import de.uniluebeck.itm.netty.handlerstack.util.ChannelBufferTools;
import de.uniluebeck.itm.tr.runtime.wsnapp.WisebedMulticastAddress;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class AbovePipelineLogger extends SimpleChannelHandler {

	private static final Logger log = LoggerFactory.getLogger(AbovePipelineLogger.class);

	@Override
	public void writeRequested(final ChannelHandlerContext ctx, final MessageEvent e)
			throws Exception {

		if (log.isTraceEnabled()) {

			final Set<String> nodeUrns = ((WisebedMulticastAddress) e.getRemoteAddress()).getNodeUrns();
			final String nodeUrnsString = Joiner.on(", ").join(nodeUrns);
			final String msg = ChannelBufferTools.toPrintableString((ChannelBuffer) e.getMessage(), 200);

			log.trace("{} => Downstream to device above channel pipeline: {}", nodeUrnsString, msg);
		}

		super.writeRequested(ctx, e);
	}

	@Override
	public void messageReceived(final ChannelHandlerContext ctx, final MessageEvent e)
			throws Exception {

		if (log.isTraceEnabled()) {

			final Set<String> nodeUrns = ((WisebedMulticastAddress) e.getRemoteAddress()).getNodeUrns();
			final String nodeUrnsString = Joiner.on(", ").join(nodeUrns);
			final String msg = ChannelBufferTools.toPrintableString((ChannelBuffer) e.getMessage(), 200);

			log.trace("{} => Upstream from device above channel pipeline: {}", nodeUrnsString, msg);
		}

		super.messageReceived(ctx, e);
	}

}
