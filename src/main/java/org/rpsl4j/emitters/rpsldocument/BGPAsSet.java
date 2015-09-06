package org.rpsl4j.emitters.rpsldocument;

import java.util.Set;

import net.ripe.db.whois.common.domain.CIString;

public class BGPAsSet extends BGPRpslSet {

	@Override
	protected Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes) {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected BGPAsSet(CIString name) {
		super(name);
	}

}
