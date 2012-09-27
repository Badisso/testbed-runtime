package de.uniluebeck.itm.tr.iwsn.nodeapi;


import javax.annotation.Nullable;

public interface NodeApiCallResult {

	/**
	 * Returns {@code true} if a reply from the sensor node with a result value of {@link
	 * de.uniluebeck.itm.tr.iwsn.nodeapi.ResponseType#COMMAND_SUCCESS} was received, {@code false} otherwise.
	 *
	 * @return see above
	 */
	boolean isSuccessful();

	/**
	 * Zero (0) if call was successful, the response code the node sent with the reply,
	 * indicating the type of failure otherwise.
	 *
	 * @return Zero (0) in case of success, a byte value indicating type of failure otherwise
	 */
	byte getResponseType();

	/**
	 * Returns the payload that is attached to the reply message, may be {@code null}.
	 *
	 * @return the payload that is attached to the reply message, may be {@code null}
	 */
	@Nullable
	byte[] getResponse();

}
