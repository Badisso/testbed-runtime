package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.uniluebeck.itm.netty.handlerstack.HandlerFactoryRegistry;
import de.uniluebeck.itm.tr.iwsn.NodeUrn;
import de.uniluebeck.itm.tr.iwsn.newoverlay.MessageDownstreamRequest;
import de.uniluebeck.itm.tr.iwsn.newoverlay.MessageUpstreamRequest;
import de.uniluebeck.itm.tr.iwsn.newoverlay.RequestFactory;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppDownstreamMessage;
import de.uniluebeck.itm.tr.runtime.wsnapp.WSNAppUpstreamMessage;
import de.uniluebeck.itm.tr.util.Tuple;
import eu.wisebed.api.common.KeyValuePair;
import eu.wisebed.api.sm.SecretReservationKey;
import eu.wisebed.api.wsn.ChannelHandlerConfiguration;
import eu.wisebed.api.wsn.ChannelHandlerDescription;
import eu.wisebed.api.wsn.Program;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

	public static List<String> convertNodeUrns(final ImmutableSet<NodeUrn> nodeUrns) {
		final ArrayList<String> strings = newArrayList();
		for (NodeUrn nodeUrn : nodeUrns) {
			strings.add(nodeUrn.toString());
		}
		return strings;
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

	public static ImmutableMap<byte[], ImmutableSet<NodeUrn>> convert(List<String> nodeIds,
																	  List<Integer> programIndices,
																	  List<Program> programs) {

		final ImmutableMap.Builder<byte[], ImmutableSet<NodeUrn>> mapBuilder = ImmutableMap.builder();

		for (int i = 0; i < programs.size(); i++) {

			final ImmutableSet.Builder<NodeUrn> nodeUrnSetBuilder = ImmutableSet.builder();

			for (int j = 0; j < programIndices.size(); j++) {

				if (i == programIndices.get(j)) {
					nodeUrnSetBuilder.add(new NodeUrn(nodeIds.get(j)));
				}
			}

			final ImmutableSet<NodeUrn> nodeUrnSet = nodeUrnSetBuilder.build();

			if (!nodeUrnSet.isEmpty()) {
				mapBuilder.put(programs.get(i).getProgram(), nodeUrnSet);
			}
		}

		return mapBuilder.build();
	}

	public static Map<String, byte[]> convert(final ImmutableSet<NodeUrn> nodeUrns,
											  final byte[] image) {
		Map<String, byte[]> map = newHashMap();
		for (NodeUrn nodeUrn : nodeUrns) {
			map.put(nodeUrn.toString(), image);
		}
		return map;
	}

	public static WSNAppDownstreamMessage convert(final MessageDownstreamRequest request) {
		return new WSNAppDownstreamMessage(convertToStringSet(request.getTo()), request.getMessageBytes());
	}

	public static MessageUpstreamRequest convert(final WSNAppUpstreamMessage request, RequestFactory requestFactory) {
		return requestFactory.createMessageUpstreamRequest(
				new NodeUrn(request.getFrom()),
				request.getTimestamp(),
				request.getMessageBytes()
		);
	}
}
