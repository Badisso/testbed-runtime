package eu.wisebed.testbed.api.wsn.v211;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getNeighbourhood complex type.
 * <p/>
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="getNeighbourhood">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="node" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getNeighbourhood", propOrder = {
		"node"
})
public class GetNeighbourhood {

	@XmlElement(required = true)
	protected String node;

	/**
	 * Gets the value of the node property.
	 *
	 * @return possible object is
	 *         {@link String }
	 */
	public String getNode() {
		return node;
	}

	/**
	 * Sets the value of the node property.
	 *
	 * @param value allowed object is
	 *              {@link String }
	 */
	public void setNode(String value) {
		this.node = value;
	}

}
