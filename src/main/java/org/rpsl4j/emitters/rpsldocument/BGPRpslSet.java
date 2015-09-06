/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

/**
 * Abstract parent class for RPSL set objects that can be resolved to a set of routes
 * @author Benjamin George Roberts
 */
public abstract class BGPRpslSet {
	
	CIString name;
	Set<BGPSetMember> members = new HashSet<BGPSetMember>();
	Set<CIString> mbrsByRef = new HashSet<CIString>();
	
	/**
	 * Recursively resolve the set of BGPRoute objects contained within this set
	 * @return clone of {@link BGPRoute} objects contained by set
	 */
	public Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument) {
		return resolve(parentRpslDocument, new HashSet<BGPRpslSet>());
	}
	
	/**
	 * Recursively resolve the set, stopping if the called object is already in the set of visited nodes
	 * @param visitedNodes Set of nodes that have already been resolved
	 * @return clone of {@link BGPRoute} objects contained by set
	 */
	protected abstract Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes);
	
	
	
	
//	protected static Set<BGPSetMember> applyPostfix(Set<BGPSetMember> members , CIString postfix) {
//		HashSet<BGPSetMember> processedMembers = new HashSet<BGPSetMember>();
//		for(BGPSetMember member : members) {
//			if(member.type==BGPSetMember.ROUTE) {
//				BGPRoute route = member.getValue_Route();
//				
//				//String rawAddress = route.routePrefixObject.toString().toLowerCase().trim();
//				//make a new route object with the composite prefix (or just the new one if none existed).
//				//Encapsulate it in a fresh BGPSetMember with no prefix (given we no longer have one we need to apply)
//				processedMembers.add(new BGPSetMember(route.cloneAppendingNewPostfix(postfix.toString())));
//				
//			} //end if Route
//			//else if(member.type==BGPSetMember.SET) //this is leading toward recursive shit.. //TODO
//				
//		} //end for
//		return processedMembers;
//	}
	
	protected BGPRpslSet(CIString name) {
		this.name = name;
	}
	
	protected void addMember(BGPSetMember member) {
		members.add(member);
	}
	
	public String toString() {
		return name + ":\n    members: " + members + "\n    mbrsByRef: " + mbrsByRef;
	}
}
