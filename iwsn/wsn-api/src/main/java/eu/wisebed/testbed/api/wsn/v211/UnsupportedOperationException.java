package eu.wisebed.testbed.api.wsn.v211;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for UnsupportedOperationException complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="UnsupportedOperationException">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="message" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "UnsupportedOperationException", propOrder = {
		"message"
})
public class UnsupportedOperationException {

	protected String message;

	/**
	 * Gets the value of the message property.
	 *
	 * @return possible object is
	 *         {@link String }
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Sets the value of the message property.
	 *
	 * @param value allowed object is
	 *              {@link String }
	 */
	public void setMessage(String value) {
		this.message = value;
	}

}
