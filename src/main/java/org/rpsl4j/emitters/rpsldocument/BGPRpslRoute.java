/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

/**
 * This class extends the standard BGPRoute to represent an rpsl route object.
 * This allows the mnt-by, withdrawn and other attributes unique to this rpsl object
 * to be used when resolving RPSL documents
 * @author Benjamin George Roberts
 */
public class BGPRpslRoute extends BGPRoute implements Cloneable {

	Set<CIString> parentSets = new HashSet<CIString>(); //route-sets this route (says) it's a member of (no double checking and mbrsByRef validation yet) //TODO
	long asNumber = -1;
	
	/**
	 * Create a BGPRpslRoute object from a rpsl route
	 * @param object rpsl route object
	 */
	BGPRpslRoute(RpslObject object) {
		super(AddressPrefixRange.parse(object.getTypeAttribute().getCleanValue()), null);
		
		this.asNumber = AutNum.parse(object.getValueForAttribute(AttributeType.ORIGIN)).getValue();
		this.parentSets.addAll(object.getValuesForAttribute(AttributeType.MEMBER_OF));

		//TODO: deal with withdrawn dates. Complicating the problem, AttributeType lists no WITHDRAWN constant.
		//CIString withdrawnDate = object.getValueOrNullForAttribute(AttributeType.)
	}
	
	@Override
	public BGPRpslRoute clone() {
		BGPRpslRoute clone = (BGPRpslRoute) super.clone();
		clone.parentSets = new HashSet<CIString>(parentSets);
		return clone;
	}
	
	@Override
	public boolean equals(Object other) {
		if(!super.equals(other) || !(other instanceof BGPRpslRoute))
			return false;
		
		BGPRpslRoute otherRoute = (BGPRpslRoute) other;
		
		return asNumber == otherRoute.asNumber && parentSets.equals(otherRoute.parentSets);
	}

}
