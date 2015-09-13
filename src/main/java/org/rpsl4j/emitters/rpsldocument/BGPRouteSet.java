/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;

public class BGPRouteSet extends BGPRpslSet {
	
	//TODO add slf4j logger
	
	public BGPRouteSet(RpslObject obj) {
		super(obj);
	}	

	@Override
	protected Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes) {
		HashSet<BGPRoute> flattenedRoutes = new HashSet<BGPRoute>();
		
		if (visitedNodes.contains(this)) //ensure we're not retracing our footsteps
			return flattenedRoutes;
		visitedNodes.add(this); //add this set to the index
		
		//Resolve route prefixes or route-sets in members list
		for(CIString member : members) {
			//Try it as a prefix
			try {
				AddressPrefixRange prefix = AddressPrefixRange.parse(member);
				flattenedRoutes.add(new BGPRoute(prefix, null));
				continue;
			} catch(AttributeParseException e) {}
			
			//Try member as a route set
			if(member.startsWith("rs-")) {
				BGPRpslSet memberSetObject = parentRpslDocument.routeSets.get(member);
				Set<BGPRoute> resolvedRoutes = memberSetObject.resolve(parentRpslDocument, visitedNodes);
				//TODO Apply prefix operation to these routes without clone
				flattenedRoutes.addAll(resolvedRoutes);
			}
			
		}				
		
		//Resolve mbrs-by-ref routes
		if(mbrsByRef.size() == 1 && mbrsByRef.contains(CIString.ciString("ANY"))) {
			//Take all routes that are member-of this set
			flattenedRoutes.addAll(parentRpslDocument.getSetMemberRoutes(name));
		} else if(mbrsByRef.size() > 0) {
			//Take intersection of routes that are member-of this set, and routes mnt-by a mbr-by-ref maintainer
			Set<BGPRoute> 	setMembers 		= parentRpslDocument.getSetMemberRoutes(name),
							byRefMembers 	= new HashSet<>();
			
			for(CIString maintainer : mbrsByRef)
				byRefMembers.addAll(parentRpslDocument.getMntByRoutes(maintainer));
			
			flattenedRoutes.addAll(Sets.intersection(setMembers, byRefMembers));
		}
		
		return flattenedRoutes;
	}
}
