
package eu.wisebed.testbed.api.wsn.v211;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.ws.RequestWrapper;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.1
 * 
 */
@WebService(name = "Controller", targetNamespace = "urn:ControllerService")
@XmlSeeAlso({
    ObjectFactory.class
})
public interface Controller {


    /**
     * 
     * @param msg
     */
    @WebMethod
    @Oneway
    @RequestWrapper(localName = "receive", targetNamespace = "urn:ControllerService", className = "eu.wisebed.testbed.api.wsn.v211.Receive")
    public void receive(
        @WebParam(name = "msg", targetNamespace = "")
        Message msg);

    /**
     * 
     * @param status
     */
    @WebMethod
    @Oneway
    @RequestWrapper(localName = "receiveStatus", targetNamespace = "urn:ControllerService", className = "eu.wisebed.testbed.api.wsn.v211.ReceiveStatus")
    public void receiveStatus(
        @WebParam(name = "status", targetNamespace = "")
        RequestStatus status);

}
