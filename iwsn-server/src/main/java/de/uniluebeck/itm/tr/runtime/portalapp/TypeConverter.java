package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.newoverlay.MessageDownstreamRequest;
import de.uniluebeck.itm.tr.iwsn.newoverlay.MessageUpstreamRequest;
import de.uniluebeck.itm.tr.iwsn.newoverlay.RequestFactory;
import de.uniluebeck.itm.tr.iwsn.newoverlay.RequestResult;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppDownstreamMessage;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppUpstreamMessage;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.ProgramMetaData;

import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * Helper class for this package that converts types from WSNApp representation to Web service representation and back.
 */
public class TypeConverter {

	public static List<SecretReservationKey> convertToSRKList(String secretReservationKey, final String urnPrefix) {

		List<SecretReservationKey> secretReservationKeyList = new LinkedList<SecretReservationKey>();

		SecretReservationKey key = new SecretReservationKey();
		key.setUrnPrefix(urnPrefix);
		key.setSecretReservationKey(secretReservationKey);

		secretReservationKeyList.add(key);

		return secretReservationKeyList;
	}

	public static List<eu.wisebed.api.rs.SecretReservationKey> convertSRKs(
			List<SecretReservationKey> secretReservationKey) {

		List<eu.wisebed.api.rs.SecretReservationKey> retList =
				new ArrayList<eu.wisebed.api.rs.SecretReservationKey>(secretReservationKey.size());
		for (SecretReservationKey reservationKey : secretReservationKey) {
			retList.add(convert(reservationKey));
		}
		return retList;
	}

	public static eu.wisebed.api.rs.SecretReservationKey convert(SecretReservationKey reservationKey) {
		eu.wisebed.api.rs.SecretReservationKey retSRK =
				new eu.wisebed.api.rs.SecretReservationKey();
		retSRK.setSecretReservationKey(reservationKey.getSecretReservationKey());
		retSRK.setUrnPrefix(reservationKey.getUrnPrefix());
		return retSRK;
	}

	public static ImmutableSet<String> convertToStringSet(final ImmutableSet<NodeUrn> nodeUrns) {
		ImmutableSet.Builder<String> set = ImmutableSet.builder();
		for (NodeUrn nodeUrn : nodeUrns) {
			set.add(nodeUrn.toString());
		}
		return set.build();
	}

	public static List<String> convert(final ImmutableSet<NodeUrn> nodeUrns) {
		final ArrayList<String> list = newArrayList();
		for (NodeUrn nodeUrn : nodeUrns) {
			list.add(nodeUrn.toString());
		}
		return list;
	}

	public static List<RequestStatus> convert(final RequestResult result, final String requestId) {

		List<RequestStatus> list = newArrayList();
		final RequestStatus requestStatus = new RequestStatus();
		list.add(requestStatus);

		for (Map.Entry<NodeUrn, Tuple<Integer, String>> entry : result.getResult().entrySet()) {

			requestStatus.setRequestId(requestId);

			final NodeUrn nodeUrn = entry.getKey();
			final Integer value = entry.getValue().getFirst();
			final String msg = entry.getValue().getSecond();

			final Status status = new Status();
			status.setNodeId(nodeUrn.toString());
			status.setValue(value);
			status.setMsg(msg);

			requestStatus.getStatus().add(status);
		}

		return list;
	}

	public static List<ChannelHandlerDescription> convert(
			final List<HandlerFactoryRegistry.ChannelHandlerDescription> descriptions) {

		final List<ChannelHandlerDescription> channelHandlerDescriptions = newArrayList();

		for (HandlerFactoryRegistry.ChannelHandlerDescription handlerDescription : descriptions) {
			channelHandlerDescriptions.add(convert(handlerDescription));
		}

		return channelHandlerDescriptions;
	}

	public static ChannelHandlerDescription convert(
			final HandlerFactoryRegistry.ChannelHandlerDescription handlerDescription) {

		ChannelHandlerDescription target = new ChannelHandlerDescription();
		target.setDescription(handlerDescription.getDescription());
		target.setName(handlerDescription.getName());
		for (Map.Entry<String, String> entry : handlerDescription.getConfigurationOptions().entries()) {
			final KeyValuePair keyValuePair = new KeyValuePair();
			keyValuePair.setKey(entry.getKey());
			keyValuePair.setValue(entry.getValue());
			target.getConfigurationOptions().add(keyValuePair);
		}
		return target;
	}

	public static ImmutableSet<NodeUrn> convertNodeUrns(final List<String> nodeIds) {
		final ImmutableSet.Builder<NodeUrn> builder = ImmutableSet.builder();
		for (String nodeId : nodeIds) {
			builder.add(new NodeUrn(nodeId));
		}
		return builder.build();
	}

	public static ImmutableList<Tuple<String, ImmutableMap<String, String>>> convertCHCs(
			final List<ChannelHandlerConfiguration> channelHandlerConfigurations) {

		final ImmutableList.Builder<Tuple<String, ImmutableMap<String, String>>> list = ImmutableList.builder();

		for (ChannelHandlerConfiguration config : channelHandlerConfigurations) {

			final String name = config.getName();
			final List<KeyValuePair> params = config.getConfiguration();
			final ImmutableMap.Builder<String, String> paramsMapBuilder = ImmutableMap.builder();

			if (params != null && params.size() > 0) {

				for (KeyValuePair param : params) {
					paramsMapBuilder.put(param.getKey(), param.getValue());
				}
			}

			list.add(new Tuple<String, ImmutableMap<String, String>>(name, paramsMapBuilder.build()));
		}

		return list.build();
	}

	public static RequestStatus convert(WSNAppMessages.RequestStatus requestStatus, String requestId) {
		RequestStatus retRequestStatus = new RequestStatus();
		retRequestStatus.setRequestId(requestId);
		WSNAppMessages.RequestStatus.Status status = requestStatus.getStatus();
		Status retStatus = new Status();
		retStatus.setMsg(status.getMsg());
		retStatus.setNodeId(status.getNodeId());
		retStatus.setValue(status.getValue());
		retRequestStatus.getStatus().add(retStatus);
		return retRequestStatus;
	}

	public static ImmutableSet<Tuple<ImmutableSet<NodeUrn>, byte[]>> convert(List<String> nodeIds,
																			 List<Integer> programIndices,
																			 List<Program> programs) {

		throw new RuntimeException("Not yet implemented!");
	}

	public static Map<String, WSNAppMessages.Program> convertFlashImage(List<String> nodeIds,
																		List<Integer> programIndices,
																		List<Program> programs) {

		Map<String, WSNAppMessages.Program> programsMap = new HashMap<String, WSNAppMessages.Program>();

		List<WSNAppMessages.Program> convertedPrograms = convertPrograms(programs);

		for (int i = 0; i < nodeIds.size(); i++) {
			programsMap.put(nodeIds.get(i), convertedPrograms.get(programIndices.get(i)));
		}

		return programsMap;
	}

	public static List<WSNAppMessages.Program> convertPrograms(List<Program> programs) {
		List<WSNAppMessages.Program> list = new ArrayList<WSNAppMessages.Program>(programs.size());
		for (Program program : programs) {
			list.add(convert(program));
		}
		return list;
	}

	public static WSNAppMessages.Program convert(Program program) {
		return WSNAppMessages.Program.newBuilder().setMetaData(convert(program.getMetaData())).setProgram(
				ByteString.copyFrom(program.getProgram())
		).build();
	}

	public static WSNAppMessages.Program.ProgramMetaData convert(ProgramMetaData metaData) {
		if (metaData == null) {
			metaData = new ProgramMetaData();
			metaData.setName("");
			metaData.setOther("");
			metaData.setPlatform("");
			metaData.setVersion("");
		}
		return WSNAppMessages.Program.ProgramMetaData.newBuilder().setName(metaData.getName()).setOther(
				metaData.getOther()
		).setPlatform(metaData.getPlatform()).setVersion(metaData.getVersion()).build();
	}

	public static de.uniluebeck.itm.tr.iwsn.newoverlay.RequestResult convertToRequestResult(
			final WSNAppMessages.RequestStatus requestStatus, final long requestId) {
		return new de.uniluebeck.itm.tr.iwsn.newoverlay.RequestResult(requestId, buildResultMap(requestStatus));
	}

	public static de.uniluebeck.itm.tr.iwsn.newoverlay.RequestStatus convertToRequestStatus(
			final WSNAppMessages.RequestStatus requestStatus, final long requestId) {
		return new de.uniluebeck.itm.tr.iwsn.newoverlay.RequestStatus(requestId, buildResultMap(requestStatus));
	}

	private static ImmutableMap<NodeUrn, Tuple<Integer, String>> buildResultMap(
			final WSNAppMessages.RequestStatus requestStatus) {
		final ImmutableMap.Builder<NodeUrn, Tuple<Integer, String>> resultMap = ImmutableMap.builder();
		final NodeUrn nodeUrn = new NodeUrn(requestStatus.getStatus().getNodeId());

		final Tuple<Integer, String> state = new Tuple<Integer, String>(
				requestStatus.getStatus().getValue(),
				requestStatus.getStatus().getMsg()
		);

		resultMap.put(nodeUrn, state);

		return resultMap.build();
	}

	public static Map<String, WSNAppMessages.Program> convert(final ImmutableSet<NodeUrn> nodeUrns,
															  final byte[] image) {
		Map<String, WSNAppMessages.Program> map = newHashMap();
		for (NodeUrn nodeUrn : nodeUrns) {

			final WSNAppMessages.Program program = WSNAppMessages.Program.newBuilder()
					.setProgram(ByteString.copyFrom(image))
					.build();

			map.put(nodeUrn.toString(), program);
		}
		return map;
	}

	public static WSNAppDownstreamMessage convert(final MessageDownstreamRequest request) {
		return new WSNAppDownstreamMessage(convertToStringSet(request.getTo()), request.getMessageBytes());
	}

	public static RequestResult convert(final WSNAppMessages.RequestStatus requestStatus, long requestId) {

		final ImmutableMap.Builder<NodeUrn, Tuple<Integer, String>> resultMap = ImmutableMap.builder();
		final NodeUrn nodeUrn = new NodeUrn(requestStatus.getStatus().getNodeId());
		final Tuple<Integer, String> status = new Tuple<Integer, String>(
				requestStatus.getStatus().getValue(),
				requestStatus.getStatus().getMsg()
		);
		resultMap.put(nodeUrn, status);
		return new RequestResult(requestId, resultMap.build());
	}

	public static MessageUpstreamRequest convert(final WSNAppUpstreamMessage request, RequestFactory requestFactory) {
		MessageUpstreamRequest overlayRequest = requestFactory.createMessageUpstreamRequest(
				new NodeUrn(request.getFrom()),
				request.getTimestamp(),
				request.getMessageBytes()
		);
		return overlayRequest;
	}
}
