package de.uniluebeck.itm.tr.runtime.portalapp;

import de.uniluebeck.itm.tr.iwsn.common.WSNPreconditions;

public interface WSNServiceFactory {

	WSNService create(WSNServiceConfig config,
					  WSNPreconditions preconditions);

}
