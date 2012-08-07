package de.uniluebeck.itm.tr.runtime.portalapp.protobuf;

import com.google.protobuf.ByteString;
import de.uniluebeck.itm.tr.iwsn.newoverlay.MessageUpstreamRequest;
import eu.wisebed.api.common.Message;
import eu.wisebed.api.controller.RequestStatus;
import eu.wisebed.api.controller.Status;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.util.GregorianCalendar;

import static com.google.common.base.Throwables.propagate;

class ProtobufApiTypeConverter {

	private static DatatypeFactory DATATYPE_FACTORY;

	static {
		try {
			DATATYPE_FACTORY = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw propagate(e);
		}
	}

	static WisebedMessages.Envelope convert(final String notification) {

		WisebedMessages.Message.Backend.Builder backendBuilder = WisebedMessages.Message.Backend.newBuilder()
				.setText(notification);

		WisebedMessages.Message.Builder backendMessageBuilder = WisebedMessages.Message.newBuilder()
				.setType(WisebedMessages.Message.Type.BACKEND)
				.setTimestamp(DATATYPE_FACTORY.newXMLGregorianCalendar(new GregorianCalendar()).toXMLFormat())
				.setBackend(backendBuilder);

		return WisebedMessages.Envelope.newBuilder()
				.setBodyType(WisebedMessages.Envelope.BodyType.MESSAGE)
				.setMessage(backendMessageBuilder)
				.build();
	}

	static WisebedMessages.Envelope convert(Message message) {

		WisebedMessages.Message.Builder messageBuilder = WisebedMessages.Message.newBuilder()
				.setTimestamp(message.getTimestamp().toXMLFormat());

		WisebedMessages.Message.NodeBinary.Builder nodeBinaryBuilder = WisebedMessages.Message.NodeBinary.newBuilder()
				.setSourceNodeUrn(message.getSourceNodeId())
				.setData(ByteString.copyFrom(message.getBinaryData()));

		messageBuilder.setNodeBinary(nodeBinaryBuilder);
		messageBuilder.setType(WisebedMessages.Message.Type.NODE_BINARY);

		return WisebedMessages.Envelope.newBuilder()
				.setBodyType(WisebedMessages.Envelope.BodyType.MESSAGE)
				.setMessage(messageBuilder)
				.build();
	}

	static WisebedMessages.Envelope convert(RequestStatus requestStatus) {

		WisebedMessages.RequestStatus.Builder requestStatusBuilder = WisebedMessages.RequestStatus.newBuilder()
				.setRequestId(requestStatus.getRequestId());

		for (Status status : requestStatus.getStatus()) {
			requestStatusBuilder.addStatus(WisebedMessages.RequestStatus.Status.newBuilder()
					.setValue(status.getValue())
					.setMessage(status.getMsg())
					.setNodeUrn(status.getNodeId())
			);
		}

		return WisebedMessages.Envelope.newBuilder()
				.setBodyType(WisebedMessages.Envelope.BodyType.REQUEST_STATUS)
				.setRequestStatus(requestStatusBuilder)
				.build();
	}

	static WisebedMessages.Envelope convert(final MessageUpstreamRequest request) {

		WisebedMessages.Message.Builder messageBuilder = WisebedMessages.Message.newBuilder()
				.setTimestamp(request.getTimestamp());

		WisebedMessages.Message.NodeBinary.Builder nodeBinaryBuilder = WisebedMessages.Message.NodeBinary.newBuilder()
				.setSourceNodeUrn(request.getFrom().toString())
				.setData(ByteString.copyFrom(request.getMessageBytes()));

		messageBuilder.setNodeBinary(nodeBinaryBuilder);
		messageBuilder.setType(WisebedMessages.Message.Type.NODE_BINARY);

		return WisebedMessages.Envelope.newBuilder()
				.setBodyType(WisebedMessages.Envelope.BodyType.MESSAGE)
				.setMessage(messageBuilder)
				.build();
	}
}
