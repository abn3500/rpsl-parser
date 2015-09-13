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
	protected static Pair<String, String> splitPrefix(String referencedObject) {
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
	abstract Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes);
	
	
	/**
	 * Applies the given prefix to all routes in the given set, deleting routes for which the combined prefix is invalid (as per the spec)
	 * @param routeSet
	 * @param prefix
	 */
	protected void applyPrefix(Set<BGPRoute> routeSet, String prefix) {
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
