package de.uniluebeck.itm.tr.snaa.shiro;

import eu.wisebed.api.v3.common.NodeUrnPrefix;

public interface ShiroSNAAFactory {

	ShiroSNAA create(NodeUrnPrefix nodeUrnPrefix);
	
}
