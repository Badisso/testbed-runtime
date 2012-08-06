package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.runtime.wsnapp.WSNApp;

public interface WSNServiceHandleFactory {

	WSNServiceHandle create(WSNService wsnService,
							WSNSoapService wsnSoapService,
							WSNApp wsnApp);
}