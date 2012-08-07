package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.newoverlay.Overlay;

public interface WSNServiceHandleFactory {

	WSNServiceHandle create(Overlay overlay,
							WSNService wsnService,
							WSNSoapService wsnSoapService);

}