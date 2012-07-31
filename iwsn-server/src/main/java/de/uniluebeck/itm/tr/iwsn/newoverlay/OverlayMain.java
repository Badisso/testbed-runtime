package de.uniluebeck.itm.tr.iwsn.newoverlay;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Guice;
import com.google.inject.Injector;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.util.Tuple;

public class OverlayMain {

	public static void main(String[] args) {

		final Injector injector = Guice.createInjector(new OverlayModule());
		final RequestFactory requestFactory = injector.getInstance(RequestFactory.class);

		final NodeUrn nodeUrn1 = new NodeUrn("urn:local:0x0001");
		final NodeUrn nodeUrn2 = new NodeUrn("urn:local:0x0002");

		final ImmutableSet<NodeUrn> nodeUrns = ImmutableSet.of(nodeUrn1, nodeUrn2);
		final ImmutableList<Tuple<String, ImmutableMap<String, String>>> pipeline = ImmutableList.of();

		Request request;
		ProgressRequest progressRequest;

		request = requestFactory.createAreNodesAliveRequest(nodeUrns);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createAreNodesAliveSmRequest(nodeUrns);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createDestroyVirtualLinkRequest(nodeUrn1, nodeUrn2);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createDisableNodeRequest(nodeUrn1);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createDisablePhysicalLinkRequest(nodeUrn1, nodeUrn2);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createEnableNodeRequest(nodeUrn1);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createEnablePhysicalLinkRequest(nodeUrn1, nodeUrn2);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createFlashDefaultImageRequest(nodeUrns);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createFlashImageRequest(nodeUrns, new byte[]{0x01});
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createResetNodesRequest(nodeUrns);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createSetChannelPipelineRequest(nodeUrns, pipeline);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");

		request = requestFactory.createSetDefaultChannelPipelineRequest(nodeUrns);
		progressRequest = requestFactory.createProgressRequest(request);

		System.out.println(request);
		System.out.println(progressRequest);
		System.out.println("************************************");
	}

}
