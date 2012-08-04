package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;

import javax.sound.midi.SysexMessage;

public class OverlayMain {

	public static void main(String[] args) {

		final Injector injector = Guice.createInjector(new OverlayModule());
		final RequestFactory requestFactory = injector.getInstance(RequestFactory.class);

		final NodeUrn nodeUrn1 = new NodeUrn("urn:local:0x0001");
		final NodeUrn nodeUrn2 = new NodeUrn("urn:local:0x0002");

		final ImmutableSet<NodeUrn> nodeUrns = ImmutableSet.of(nodeUrn1, nodeUrn2);
		final ImmutableList<Tuple<String, ImmutableMap<String, String>>> pipeline = ImmutableList.of();

		System.out.println("************************************");
		System.out.println(requestFactory.createAreNodesAliveRequest(nodeUrns));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createAreNodesAliveSmRequest(nodeUrns));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createDestroyVirtualLinkRequest(nodeUrn1, nodeUrn2));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createDisableNodeRequest(nodeUrn1));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createDisablePhysicalLinkRequest(nodeUrn1, nodeUrn2));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createEnableNodeRequest(nodeUrn1));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createEnablePhysicalLinkRequest(nodeUrn1, nodeUrn2));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createFlashDefaultImageRequest(nodeUrns));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createFlashImageRequest(nodeUrns, new byte[]{0x01}));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createResetNodesRequest(nodeUrns));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createSetChannelPipelineRequest(nodeUrns, pipeline));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createSetDefaultChannelPipelineRequest(nodeUrns));
		System.out.println();

		System.out.println("************************************");
		System.out.println(requestFactory.createSetVirtualLinkRequest(nodeUrn1, nodeUrn2));
		System.out.println();
	}
}
