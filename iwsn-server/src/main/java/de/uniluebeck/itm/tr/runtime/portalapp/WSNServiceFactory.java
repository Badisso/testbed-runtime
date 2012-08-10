package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;

public interface WSNServiceFactory {

	WSNService createWSNService(WSNServiceConfig config, WSNPreconditions preconditions);

	WSNSoapService createWSNSoapService(WSNServiceConfig config, WSNService wsnService);

}
