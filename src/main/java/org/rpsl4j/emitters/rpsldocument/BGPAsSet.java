package org.rpsl4j.emitters.rpsldocument;

import java.util.Set;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.RpslObject;

public class BGPAsSet extends BGPRpslSet {

	@Override
	protected Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected BGPAsSet(RpslObject rpslObject) {
		super(rpslObject);
	}

}
