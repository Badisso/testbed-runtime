package de.uniluebeck.itm.wisebed.cmdlineclient.wrapper;

import com.google.common.util.concurrent.ValueFuture;
import de.uniluebeck.itm.wisebed.cmdlineclient.DelegatingController;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.AsyncJobObserver;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.Job;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResult;
import de.uniluebeck.itm.wisebed.cmdlineclient.jobs.JobResultListener;
import eu.wisebed.testbed.api.wsn.v211.*;

import javax.jws.WebParam;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.lang.UnsupportedOperationException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.*;


public class WSNAsyncWrapper {

	private static class FutureJobResultListener implements JobResultListener {

		private ValueFuture<JobResult> future;

		private FutureJobResultListener(final ValueFuture<JobResult> future) {
			this.future = future;
		}

		@Override
		public void receiveJobResult(final JobResult result) {
			future.set(result);
		}

		@Override
		public void receiveMessage(final Message msg) throws IOException {
			// nothing to do
		}

		@Override
		public void timeout() {
			future.setException(new TimeoutException());
		}
	}

	private ExecutorService executor = Executors.newCachedThreadPool();

	private AsyncJobObserver jobs = new AsyncJobObserver(10, TimeUnit.SECONDS);

	private WSN wsn;

	private WSNAsyncWrapper(final WSN wsn, final String localControllerEndpointURL) {
		this.wsn = wsn;
		DelegatingController delegator = new DelegatingController(new Controller() {
			@Override
			public void receive(@WebParam(name = "msg", targetNamespace = "") final Message msg) {
				// nothing to do
			}
			@Override
			public void receiveStatus(@WebParam(name = "status", targetNamespace = "") final RequestStatus status) {
				jobs.receive(status);
			}
		});
		try {
			delegator.publish(localControllerEndpointURL);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static WSNAsyncWrapper of(final WSN wsn, final String localControllerEndpointURL) {
		return new WSNAsyncWrapper(wsn, localControllerEndpointURL);
	}

	public Future<Void> addController(final String controllerEndpointUrl) {
		return executor.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				wsn.addController(controllerEndpointUrl);
				return null;
			}
		}
		);
	}

	public Future<Void> removeController(final String controllerEndpointUrl) {
		return executor.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				wsn.removeController(controllerEndpointUrl);
				return null;
			}
		}
		);
	}

	public Future<JobResult> send(List<String> nodeIds, Message message) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("send", wsn.send(nodeIds, message), nodeIds, Job.JobType.send);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<?> getVersion() {
		return executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return wsn.getVersion();
			}
		}
		);
	}

	public Future<JobResult> areNodesAlive(List<String> nodes) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("areNodesAlive", wsn.areNodesAlive(nodes), nodes, Job.JobType.areNodesAlive);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<JobResult> defineNetwork(String newNetwork) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	public Future<String> describeCapabilities(String capability) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	public Future<JobResult> destroyVirtualLink(String sourceNode, String targetNode) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("destroyVirtualLink", wsn.destroyVirtualLink(sourceNode, targetNode), sourceNode,
				Job.JobType.destroyVirtualLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<JobResult> disableNode(String node) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("disableNode", wsn.disableNode(node), node, Job.JobType.disableNode);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<JobResult> disablePhysicalLink(String nodeA, String nodeB) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("disablePhysicalLink", wsn.disablePhysicalLink(nodeA, nodeB), nodeA,
				Job.JobType.disablePhysicalLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<JobResult> enableNode(String node) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("enableNode", wsn.enableNode(node), node, Job.JobType.enableNode);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<JobResult> enablePhysicalLink(String nodeA, String nodeB) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("enablePhysicalLink", wsn.enablePhysicalLink(nodeA, nodeB), nodeA,
				Job.JobType.enablePhysicalLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<JobResult> flashPrograms(List<String> nodeIds, List<Integer> programIndices, List<Program> programs) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("flashPrograms", wsn.flashPrograms(nodeIds, programIndices, programs), nodeIds,
				Job.JobType.flashPrograms
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 180, TimeUnit.SECONDS);
		return future;
	}

	public Future<List<String>> getFilters() {
		return executor.submit(new Callable<List<String>>() {
			@Override
			public List<String> call() throws Exception {
				return wsn.getFilters();
			}
		}
		);
	}

	public Future<List<String>> getNeighbourhood(final String node) {
		return executor.submit(new Callable<List<String>>() {
			@Override
			public List<String> call() throws Exception {
				return wsn.getNeighbourhood(node);
			}
		}
		);
	}

	public Future<String> getNetwork() {
		return executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return wsn.getNetwork();
			}
		}
		);
	}

	public Future<String> getPropertyValueOf(final String node, final String propertyName) {
		return executor.submit(new Callable<String>() {
			@Override
			public String call() throws Exception {
				return wsn.getPropertyValueOf(node, propertyName);
			}
		}
		);
	}

	public Future<JobResult> resetNodes(List<String> nodes) {
		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job("resetNodes", wsn.resetNodes(nodes), nodes, Job.JobType.resetNodes);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

	public Future<?> setStartTime(XMLGregorianCalendar time) {
		throw new UnsupportedOperationException("Not yet implemented!");
	}

	public Future<JobResult> setVirtualLink(String sourceNode, String targetNode, String remoteServiceInstance,
											List<String> parameters, List<String> filters) {

		ValueFuture<JobResult> future = ValueFuture.create();
		Job job = new Job(
				"setVirtualLink",
				wsn.setVirtualLink(
						sourceNode,
						targetNode,
						remoteServiceInstance,
						parameters,
						filters
				),
				sourceNode,
				Job.JobType.setVirtualLink
		);
		job.addListener(new FutureJobResultListener(future));
		jobs.submit(job, 10, TimeUnit.SECONDS);
		return future;
	}

}
