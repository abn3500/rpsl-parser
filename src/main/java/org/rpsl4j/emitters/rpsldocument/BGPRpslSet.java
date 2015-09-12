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
 * @author Benjamin George Roberts, Nathan Kelly
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
		//find autnums and routes that have requested membership in this set. //TODO: perhaps break out into subclasses to prevent a type ending up in the wrong type of set
		if(mbrsByRef.contains(CIString.ciString("ANY"))) { //if we're not fussy about what we add by reference, speed things up
			flattenedRoutes.addAll(parentRpslDocument.getSetRoutes(name));
		}
		else {
			for(BGPRpslRoute r : parentRpslDocument.getSetRoutes(name)) { //for all routes that claim membership in this set
				if(mbrsByRef.contains(r.getMaintainer())) //if the route's maintainer is one of those we accept routes from
					flattenedRoutes.add(r);
			}
		}
		
		for(CIString member : members) {
			member = CIString.ciString(member.toLowerCase().trim()); //clean //TODO: check if these values are already clean
			Pair<String, String> refMember = splitPrefix(member.toString());
			String refMemberName = refMember.getLeft();
			String prefix = refMember.getRight();
			
			//Test if member is a as-set
			if(member.startsWith("as-"))  {
				BGPRpslSet memberSetObject = parentRpslDocument.asSets.get(refMemberName);
				Set<BGPRoute> resolvedRoutes = memberSetObject.resolve(parentRpslDocument, visitedNodes);
				
				applyPrefix(resolvedRoutes, prefix); //apply prefix, if any

				flattenedRoutes.addAll(resolvedRoutes);
			} else {
				//Try resolve it as an AS
				try {
					AutNum autNum = AutNum.parse(refMemberName);
					Set<BGPRoute> resolvedRoutes = parentRpslDocument.getASRoutes(autNum.getValue());
					applyPrefix(resolvedRoutes, prefix); //TODO: is this even a legal option in an as-set??
					flattenedRoutes.addAll(resolvedRoutes);
				} catch(AttributeParseException e) {}
			}
		}
		
		
		//Remove ourselves from visitors so set specific code can run
		visitedNodes.remove(this);
		
		return flattenedRoutes;
	}
	
	
	/**
	 * Applies the given prefix to all routes in the given set, deleting routes for which the combined prefix is invalid (as per the spec)
	 * @param routeSet
	 * @param prefix
	 */
	private void applyPrefix(Set<BGPRoute> routeSet, String prefix) {
		if(prefix==null)
			return;
		
		for(BGPRoute r : routeSet) {
			try {
				r.appendPostfix(prefix);
			} catch (IllegalArgumentException e) { //failed to mix prefixes
				routeSet.remove(r); //TODO: check this is an acceptable time to get rid of it
			}
		}
	}
	
	public String toString() {
		return name + ":\n    members: " + members + "\n    mbrsByRef: " + mbrsByRef;
	}
}
