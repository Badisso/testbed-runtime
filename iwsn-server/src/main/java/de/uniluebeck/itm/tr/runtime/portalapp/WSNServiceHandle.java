package de.uniluebeck.itm.tr.runtime.portalapp;

import com.google.common.util.concurrent.Service;
import de.uniluebeck.itm.tr.iwsn.newoverlay.Overlay;

public interface WSNServiceHandle extends Service {

	WSNService getWsnService();

	WSNSoapService getWsnSoapService();

	Overlay getOverlay();
}
