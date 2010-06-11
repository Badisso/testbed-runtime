
package eu.wisebed.testbed.api.wsn.v211;

import java.util.List;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebService(name = "SessionManagement", targetNamespace = "urn:SessionManagementService")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface SessionManagement {


    /**
     * 
     * @param parameters
     * @return
     *     returns java.lang.String
     * @throws ExperimentNotRunningException_Exception
     * @throws UnknownReservationIdException_Exception
     */
    @WebMethod
    @WebResult(name = "getInstanceResponse", targetNamespace = "urn:SessionManagementService", partName = "parameters")
    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public String getInstance(
        @WebParam(name = "getInstance", targetNamespace = "urn:SessionManagementService", partName = "parameters")
        GetInstance parameters)
        throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception
    ;

    /**
     * 
     * @param secretReservationKey
     * @throws ExperimentNotRunningException_Exception
     * @throws UnknownReservationIdException_Exception
     */
    @WebMethod
    @RequestWrapper(localName = "free", targetNamespace = "urn:SessionManagementService", className = "eu.wisebed.testbed.api.wsn.v211.Free")
    @ResponseWrapper(localName = "freeResponse", targetNamespace = "urn:SessionManagementService", className = "eu.wisebed.testbed.api.wsn.v211.FreeResponse")
    public void free(
        @WebParam(name = "secretReservationKey", targetNamespace = "")
        List<SecretReservationKey> secretReservationKey)
        throws ExperimentNotRunningException_Exception, UnknownReservationIdException_Exception
    ;

    /**
     * 
     * @return
     *     returns java.lang.String
     */
    @WebMethod
    @WebResult(targetNamespace = "")
    @RequestWrapper(localName = "getNetwork", targetNamespace = "urn:CommonTypes", className = "eu.wisebed.testbed.api.wsn.v211.GetNetwork")
    @ResponseWrapper(localName = "getNetworkResponse", targetNamespace = "urn:CommonTypes", className = "eu.wisebed.testbed.api.wsn.v211.GetNetworkResponse")
    public String getNetwork();

}