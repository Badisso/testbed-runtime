package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.ByteString;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.newoverlay.RequestResult;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppMessages;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;
import eu.wisebed.api.wsn.ProgramMetaData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Helper class for this package that converts types from WSNApp representation to Web service representation and back.
 */
class TypeConverter {

	static List<String> convert(final ImmutableSet<NodeUrn> nodeUrns) {
		final ArrayList<String> list = newArrayList();
		for (NodeUrn nodeUrn : nodeUrns) {
			list.add(nodeUrn.toString());
		}
		return list;
	}

	static List<RequestStatus> convert(final RequestResult result, final String requestId) {

		List<RequestStatus> list = newArrayList();

		for (Map.Entry<NodeUrn, Tuple<Integer, String>> entry : result.getResult().entrySet()) {

			final RequestStatus requestStatus = new RequestStatus();
			requestStatus.setRequestId(requestId);

			final NodeUrn nodeUrn = entry.getKey();
			final Integer value = entry.getValue().getFirst();
			final String msg = entry.getValue().getSecond();

			final Status status = new Status();
			status.setNodeId(nodeUrn.toString());
			status.setValue(value);
			status.setMsg(msg);

			requestStatus.getStatus().add(status);

			list.add(requestStatus);
		}

		return list;
	}

	static List<ChannelHandlerDescription> convert(
			final List<HandlerFactoryRegistry.ChannelHandlerDescription> descriptions) {

		final List<ChannelHandlerDescription> channelHandlerDescriptions = newArrayList();

		for (HandlerFactoryRegistry.ChannelHandlerDescription handlerDescription : descriptions) {
			channelHandlerDescriptions.add(convert(handlerDescription));
		}

		return channelHandlerDescriptions;
	}

	static ChannelHandlerDescription convert(
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

	static ImmutableSet<NodeUrn> convertNodeUrns(final List<String> nodeIds) {
		final ImmutableSet.Builder<NodeUrn> builder = ImmutableSet.builder();
		for (String nodeId : nodeIds) {
			builder.add(new NodeUrn(nodeId));
		}
		return builder.build();
	}

	static ImmutableList<Tuple<String, ImmutableMap<String, String>>> convertCHCs(
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

	static RequestStatus convert(WSNAppMessages.RequestStatus requestStatus, String requestId) {
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

	static ImmutableSet<Tuple<ImmutableSet<NodeUrn>, byte[]>> convert(List<String> nodeIds,
																	  List<Integer> programIndices,
																	  List<Program> programs) {

		throw new RuntimeException("Not yet implemented!");
	}

	static Map<String, WSNAppMessages.Program> convertFlashImage(List<String> nodeIds, List<Integer> programIndices,
																 List<Program> programs) {

		Map<String, WSNAppMessages.Program> programsMap = new HashMap<String, WSNAppMessages.Program>();

		List<WSNAppMessages.Program> convertedPrograms = convertPrograms(programs);

		for (int i = 0; i < nodeIds.size(); i++) {
			programsMap.put(nodeIds.get(i), convertedPrograms.get(programIndices.get(i)));
		}

		return programsMap;
	}

	static List<WSNAppMessages.Program> convertPrograms(List<Program> programs) {
		List<WSNAppMessages.Program> list = new ArrayList<WSNAppMessages.Program>(programs.size());
		for (Program program : programs) {
			list.add(convert(program));
		}
		return list;
	}

	static WSNAppMessages.Program convert(Program program) {
		return WSNAppMessages.Program.newBuilder().setMetaData(convert(program.getMetaData())).setProgram(
				ByteString.copyFrom(program.getProgram())
		).build();
	}

	static WSNAppMessages.Program.ProgramMetaData convert(ProgramMetaData metaData) {
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

}
