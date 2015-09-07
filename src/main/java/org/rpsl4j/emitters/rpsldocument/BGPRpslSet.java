/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

/**
 * Abstract parent class for RPSL set objects that can be resolved to a set of routes
 * @author Benjamin George Roberts
 */
public abstract class BGPRpslSet {
	
	//TODO add slf4j logger
	
	protected final CIString name;
	protected Set<CIString> members = new HashSet<CIString>();
	protected Set<CIString> mbrsByRef = new HashSet<CIString>();

	/**
	 * Build set object and extract names of member sets etc.
	 */
	public BGPRpslSet(RpslObject setObject) {
		name = setObject.getTypeAttribute().getCleanValue();
		
		if(setObject.containsAttribute(AttributeType.MEMBERS))
			members = setObject.getValuesForAttribute(AttributeType.MEMBERS);
		if(setObject.containsAttribute(AttributeType.MBRS_BY_REF))
			mbrsByRef = setObject.getValuesForAttribute(AttributeType.MBRS_BY_REF);
	}
	
	
	/**
	 * Separate address prefix (eg. '^+') from referenced route-set, as-set or AS
	 * @param referencedObject
	 * @return A pair of: (referenced item, prefix or null)
	 */
	public static Pair<String, String> splitPrefix(String referencedObject) {
		String s = referencedObject.toString(); //TODO Make sure this data has been cleaned
		int prefixStartIndex = s.lastIndexOf('^');
		if(prefixStartIndex==-1) //no prefix
			return Pair.of(referencedObject, null);
		else
			return Pair.of(s.substring(0, prefixStartIndex), s.substring(prefixStartIndex));
	}
	
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
	protected Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes) {
		HashSet<BGPRoute> flattenedRoutes = new HashSet<BGPRoute>();
		
		if (visitedNodes.contains(this)) //ensure we're not retracing our footsteps
			return flattenedRoutes;
		visitedNodes.add(this); //add this set to the index
		
		for(CIString member : members) {
			//Test if member is a as-set
			if(member.startsWith("as-"))  {
				BGPRpslSet memberSetObject = parentRpslDocument.asSets.get(member);
				Set<BGPRoute> resolvedRoutes = memberSetObject.resolve(parentRpslDocument, visitedNodes);
				//TODO Apply prefix operation to these routes without clone
				flattenedRoutes.addAll(resolvedRoutes);
			} else {
				//Try resolve it as an AS
				try {
					AutNum autNum = AutNum.parse(member);
					Set<BGPRoute> resolvedRoutes = parentRpslDocument.getASRoutes(autNum.getValue());
					//TODO Apply prefix operation to these routes without clone
					flattenedRoutes.addAll(resolvedRoutes);
				} catch(AttributeParseException e) {}
			}
				
		}
		
		
		//Remove ourselves from visitors so set specific code can run
		visitedNodes.remove(this);
		
		return flattenedRoutes;
	}
	
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
	
//	protected void addMember(BGPSetMember member) {
//		members.add(member);
//	}
//	
	public String toString() {
		return name + ":\n    members: " + members + "\n    mbrsByRef: " + mbrsByRef;
	}
}
