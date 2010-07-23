
package eu.wisebed.testbed.api.rs.v1;

import javax.xml.ws.WebFault;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebFault(name = "RSFault", targetNamespace = "urn:RSService")
public class RSExceptionException
    extends Exception
{

    /**
     * Java type that goes as soapenv:Fault detail element.
     * 
     */
    private RSException faultInfo;

    /**
     * 
     * @param message
     * @param faultInfo
     */
    public RSExceptionException(String message, RSException faultInfo) {
        super(message);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @param message
     * @param faultInfo
     * @param cause
     */
    public RSExceptionException(String message, RSException faultInfo, Throwable cause) {
        super(message, cause);
        this.faultInfo = faultInfo;
    }

    /**
     * 
     * @return
     *     returns fault bean: eu.wisebed.testbed.api.rs.v1.RSException
     */
    public RSException getFaultInfo() {
        return faultInfo;
    }

}
