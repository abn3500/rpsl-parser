/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
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
	final long asNumber;
	
	private boolean isWithdrawn = false;
	private CIString maintainedBy;

	
	/**
	 * Create a BGPRpslRoute object from a rpsl route
	 * @param object rpsl route object
	 */
	BGPRpslRoute(RpslObject object) {
		super(AddressPrefixRange.parse(object.getTypeAttribute().getCleanValue()), null);
		
		this.asNumber = AutNum.parse(object.getValueForAttribute(AttributeType.ORIGIN)).getValue();
		this.parentSets.addAll(object.getValuesForAttribute(AttributeType.MEMBER_OF));

		//Get maintainer string
		if(object.containsAttribute(AttributeType.MNT_BY))
			maintainedBy = object.getValueForAttribute(AttributeType.MNT_BY);
		
		// Check if route has been withdrawn, there is no AttributeType for withdrawn
		for(RpslAttribute attr : object.getAttributes()) {
			if(!attr.getKey().equals("withdrawn"))
				continue;
			
			try {
				Date withdrawnDate = new SimpleDateFormat("yyyymmdd").parse(attr.getCleanValue().toString());
				isWithdrawn = withdrawnDate.before(new Date()); //check against now
				break;
			} catch (ParseException e) {
				log.warn("route object \""
						+ object.getTypeAttribute().getCleanValue().toString()
						+ "\" has invalid withdrawn date \"" 
						+ attr.getCleanValue().toString() + "\"", e);
			}
		}
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
		
		//We don't compare AS as it results in doubles when resolving as sets
		return parentSets.equals(otherRoute.parentSets);
	}
	
	/**
	 * Check if the route has been withdrawn
	 * @return route is withdrawn
	 */
	public boolean isWithdrawn() {
		return isWithdrawn;
	}
	
	/**
	 * Return the name of the maintainer of the route object
	 * @return name of maintainer object or null
	 */
	public CIString getMaintainer() {
		return maintainedBy;
	}

}
