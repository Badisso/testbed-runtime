package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.newoverlay.Testbed;

public interface WSNServiceHandleFactory {

	WSNServiceHandle create(Testbed testbed,
							WSNService wsnService,
							WSNSoapService wsnSoapService);

}