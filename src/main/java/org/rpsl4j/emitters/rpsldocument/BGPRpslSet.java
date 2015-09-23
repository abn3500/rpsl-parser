/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslObject;

/**
 * Abstract parent class for RPSL set objects that can be resolved to a set of routes
 * @author Benjamin George Roberts, Nathan Kelly
 */
public abstract class BGPRpslSet {
	
	final static Logger log = LoggerFactory.getLogger(BGPRpslSet.class);
	
	protected final CIString name;
	protected Set<CIString> members = new HashSet<CIString>();
	protected Set<CIString> mbrsByRef = new HashSet<CIString>();

	/**
	 * Build set object and extract names of member sets etc.
	 * @param setObject as-set or route-set object to instantiate from 
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
	 * @param referencedObject name of reference
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
	 * @param parentRpslDocument document that set resolves members from
	 * @return clone of {@link BGPRoute} objects contained by set
	 */
	public Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument) {
		return resolve(parentRpslDocument, new HashSet<BGPRpslSet>());
	}
	
	/**
	 * Recursively resolve the set, stopping if the called object is already in the set of visited nodes
	 * @param parentRpslDocument document used to resolve members
	 * @param visitedNodes Set of nodes that have already been resolved
	 * @return clone of {@link BGPRoute} objects contained by set
	 */
	abstract Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes);
	
	
	public String toString() {
		return name + ":\n    members: " + members + "\n    mbrsByRef: " + mbrsByRef;
	}
}
