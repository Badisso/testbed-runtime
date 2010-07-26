package eu.wisebed.testbed.api.rs.v1;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 */
@WebFault(name = "ReservationNotFoundFault", targetNamespace = "urn:RSService")
public class ReservervationNotFoundExceptionException
		extends Exception {

	/**
	 * Java type that goes as soapenv:Fault detail element.
	 */
	private ReservervationNotFoundException faultInfo;

	/**
	 * @param message
	 * @param faultInfo
	 */
	public ReservervationNotFoundExceptionException(String message, ReservervationNotFoundException faultInfo) {
		super(message);
		this.faultInfo = faultInfo;
	}

	/**
	 * @param message
	 * @param faultInfo
	 * @param cause
	 */
	public ReservervationNotFoundExceptionException(String message, ReservervationNotFoundException faultInfo, Throwable cause) {
		super(message, cause);
		this.faultInfo = faultInfo;
	}

	/**
	 * @return returns fault bean: eu.wisebed.testbed.api.rs.v1.ReservervationNotFoundException
	 */
	public ReservervationNotFoundException getFaultInfo() {
		return faultInfo;
	}

}
