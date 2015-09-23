/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.io.RpslObjectStreamReader;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

public class BGPRpslDocument {

	/**
	 * Object sets available for templates
	 */
	Set<RpslObject> rpslObjects = new HashSet<RpslObject>();
	Set<BGPAutNum> autNumSet = new HashSet<BGPAutNum>();
	Set<BGPInetRtr> inetRtrSet = new HashSet<BGPInetRtr>();
	Set<BGPPeer> peerSet = new HashSet<BGPPeer>();

	final static Logger log = LoggerFactory.getLogger(BGPRpslDocument.class);
	
	
    private Multimap<CIString, BGPRpslRoute>	setMemberRoutes	= HashMultimap.create(), //routes by the set(s) they say they are members of
    											mntByRoutes	= HashMultimap.create(); //routes grouped by their maintainer
    private Multimap<Long, BGPRpslRoute>		asOriginRoutes	= HashMultimap.create(); //routes by the ASs the route states as its origin
	
    private Multimap<CIString, Long>			setMemberAutNum = HashMultimap.create(),
												mntByAutNum = HashMultimap.create();
    
    //Maps of route-set/as-set RPSL objects to java representations
    Map<String, BGPRpslSet>		routeSets   = new HashMap<>(),
    							asSets		= new HashMap<>();
    
	private Map<String, BGPAutNum> autNumMap = new HashMap<String, BGPAutNum>();
	
	public BGPRpslDocument(Set<RpslObject> rpslObjects) {
		this.rpslObjects = rpslObjects;
		
		//Route and AutNum objects need to be parsed first due to member-of relatioshis
		parseRpslRouteObjects();
		parseRpslAutNumObjects();
		//Can now resolve sets with member-of relationships
		parseRpslSetObjects(); 
	}


	/**
	 * Construct an RPSL document by iterating through {@link RpslObjectStreamReader} objects
	 * @param rpslDocumentReader Stream to read {@link RpslObject}s from
	 * @return {@link BGPRpslDocument} containing {@link RpslObject}s
	 */
	public static BGPRpslDocument parseRpslDocument(RpslObjectStreamReader rpslDocumentReader) {
		HashSet<RpslObject> rpslObjectSet = new HashSet<RpslObject>();
		
		for(String stringObject : rpslDocumentReader)
    	{
    		//parse can return null or throw exceptions
    		try {
        		RpslObject object = RpslObject.parse(stringObject);
    			if (object == null)
    				throw new NullPointerException("Object failed to parse");
        		
    			rpslObjectSet.add(object);
    		} catch (NullPointerException | IllegalArgumentException e) {
    			//Object failed to parse, print error with excerpt of object
    			String[] splitObject = stringObject.split("\n");
    			log.warn("Unable to parse following object, skipping... ");
    			
    			//Print object excerpt
    			for(int i = 0; i < 3 && i < splitObject.length; i++) {
    				  log.error("{}", splitObject[i]);
    				  if(i == 2 && splitObject.length>3 ) //on last iteration, and there are lines of the object we haven't printed
    				    log.error("...");
    			}
    		}
    	}
		
		return new BGPRpslDocument(rpslObjectSet);
	}
	
	/**
	 * Parse Route type objects to identify member-of and mbrs-by-ref relationships
	 */
	private void parseRpslRouteObjects() {
		for(RpslObject o : this.rpslObjects) {
			if(o.getType() != ObjectType.ROUTE)
				continue; //skip non Route objects

			BGPRpslRoute bgpRoute = new BGPRpslRoute(o);
			
			if(bgpRoute.isWithdrawn())
				continue;
			
			asOriginRoutes.put(bgpRoute.asNumber, bgpRoute);
			for(CIString set : bgpRoute.parentSets) {
				setMemberRoutes.put(set, bgpRoute);
			}

			if(bgpRoute.getMaintainer() != null)
				mntByRoutes.put(bgpRoute.getMaintainer(), bgpRoute);

		}
	}
	
	/**
	 *	Parse and build {@link BGPRouteSet} type objects
	 */
	private void parseRpslSetObjects() {
		for(RpslObject o : this.rpslObjects) {
			if(o.getType() == ObjectType.ROUTE_SET) {
				routeSets.put(o.getValueForAttribute(AttributeType.ROUTE_SET).toLowerCase(), new BGPRouteSet(o));
			} else if(o.getType() == ObjectType.AS_SET) {
				asSets.put(o.getValueForAttribute(AttributeType.AS_SET).toLowerCase(), new BGPAsSet(o));
			}
		}
	}
	
	/**
	 * Parse AutNum type objects to identify member-of and mbrs-by-ref relationships
	 */
	private void parseRpslAutNumObjects() {
		for(RpslObject o: this.rpslObjects) {
			if(o.getType() != ObjectType.AUT_NUM)
				continue;
			Long autNum = AutNum.parse(o.getTypeAttribute().getCleanValue()).getValue();
            
			//Add to member-of etc sets
            if(o.containsAttribute(AttributeType.MNT_BY))
                mntByAutNum.put(o.getValueForAttribute(AttributeType.MNT_BY), autNum);
            if(o.containsAttribute(AttributeType.MEMBER_OF)) {
                for(CIString parentSet : o.getValuesForAttribute(AttributeType.MEMBER_OF))
                    setMemberAutNum.put(parentSet, autNum);
            }
		}
	}
	
	
	/**
	 * Get route objects that claim membership in the given set. (Used for processing mbrs-by-ref)
	 * @param setName
	 * @return
	 */
	public Collection<BGPRpslRoute> getSetRoutes(CIString setName) {
		return setMemberRoutes.get(setName);
	}
	
	/**
	 * Generate or return cached set of {@link BGPAutNum}s declared in rpsl document
	 * @return Set of document's {@link BGPAutNum}s 
	 */
	public Set<BGPAutNum> getAutNumSet() {
		//Cache autnum set
		if(this.autNumSet.size() != 0) 
			return this.autNumSet;
		
		this.autNumSet = new HashSet<BGPAutNum>(getAutNumMap().values());
		return this.autNumSet;
		
	}
	
	/**
	 * Generate or return cached map of Autonomous System Numbers to {@link BGPAutNum}s decalred in rpsl document.
	 * @return Map of AS Numbers to {@link BGPAutNum}s as declared in objects
	 */
	Map<String, BGPAutNum> getAutNumMap() {
		//Cache autnum map
		if(this.autNumMap.size() != 0)
			return this.autNumMap;
				
		//Iterate through object set to find aut-num objects
		for(RpslObject o: this.rpslObjects) {
			if(o.getType() != ObjectType.AUT_NUM)
				continue;
			String asNumber = o.getTypeAttribute().getCleanValue().toString();
			
			//TODO check and handle case where asNumber is already inserted
			autNumMap.put(asNumber, new BGPAutNum(o, this));
			
			//Add to member-of etc sets
			
		}
		
		return autNumMap;
	}
	
	/**
	 * Generate or return the BGPRpslSet object representing the as-set of the provided name
	 * @param setName name of as-set object to retrieve
	 * @return as-set object or null
	 */
	BGPRpslSet getASSet(String setName) {
		//asSets initialised at construct time
		return asSets.get(CIString.ciString(setName));
	}
	
	BGPRpslSet getRouteSet(String setName) {
		//routeSets initialised at construct time
		return routeSets.get(CIString.ciString(setName));
	}
	
	/**
	 * Generate or return cached  set of {@link BGPInetRtr}s declared in the RPSL document.
	 * Peers must be members of declared {@link BGPAutNum} objects.
	 * @return Set of declared {@link BGPInetRtr}s
	 */
	public Set<BGPInetRtr> getInetRtrSet() {
		if(this.inetRtrSet.size() != 0)
			return this.inetRtrSet;
		
		HashSet<BGPInetRtr> inetRtrSet = new HashSet<BGPInetRtr>();
		Map<String, BGPAutNum> autNumMap = getAutNumMap();
		
		//Iterate through object set to find inet-rtr objects
		for(RpslObject o : this.rpslObjects) {
			if(o.getType() != ObjectType.INET_RTR)
				continue;
			
			//get AS of inet-rtr
			String localAS = o.getValueForAttribute(AttributeType.LOCAL_AS).toString();
			
			if(!autNumMap.containsKey(localAS))
				continue; //TODO handle this case better
			
			inetRtrSet.addAll(BGPInetRtr.getSpeakerInstances(o, autNumMap.get(localAS)));
		}
		
		//cache and return
		this.inetRtrSet = inetRtrSet;
		return inetRtrSet;	
	}
	
	/**
	 * Generate the set of peers declared by {@link BGPInetRtr}s.
	 * @return Set of declared {@link BGPPeer}s
	 */
	public Set<BGPPeer> getPeerSet() {
		if(this.peerSet.size() != 0)
			return this.peerSet;
		
		HashSet<BGPPeer> peerSet = new HashSet<BGPPeer>();
		
		for(BGPInetRtr speaker : getInetRtrSet()) {
			peerSet.addAll(speaker.getPeers());
		}
		
		//cache and return
		this.peerSet = peerSet;
		return peerSet;
	}
	
	/**
	 * Return a copy of the {@link BGPRoute}s of a particular autnum; declared as RPSL Route objects.
	 * @param autNum autnum to query routes from
	 * @return copy of autnum's routes
	 */
	public Set<BGPRoute> getASRoutes(long autNum) {
		Set<BGPRoute> routeSet = new HashSet<>();
		//Check if AS has declared route objects
		if(asOriginRoutes.containsKey(autNum)) {
			for(BGPRoute r : asOriginRoutes.get(autNum))
				routeSet.add(r.clone());
		}
		return routeSet;
	}
	
	/**
	 * Return a copy of the {@link BGPRoute}s maintained by a particular maintainer; declared as RPSL Route objects.
	 * @return copy of maintainer's routes
	 */
	public Set<BGPRoute> getMntByRoutes(CIString maintainer) {
		Set<BGPRoute> routeSet = new HashSet<>();
		if(mntByRoutes.containsKey(maintainer)) {
			for(BGPRoute r : mntByRoutes.get(maintainer))
				routeSet.add(r.clone());
		}
		return routeSet;			
	}
	
	/**
	 * Return a copy of the {@link BGPRoute}s that are members-of a route set
	 * @param maintainer set name to query routes for
	 * @return copy of set's member routes
	 */
	public Set<BGPRoute> getSetMemberRoutes(CIString setName) {
		Set<BGPRoute> routeSet = new HashSet<>();
		if(setMemberRoutes.containsKey(setName)) {
			for(BGPRoute r : setMemberRoutes.get(setName))
				routeSet.add(r.clone());
		}
		return routeSet;			
	}
	
	/**
	 * Return the set of AutNums maintained by a provided maintainer
	 * @param maintainer set name to query autnums for
	 * @return maintainers autnums
	 */
	public Set<Long> getMntByAutNums(CIString maintainer) {
		Set<Long> autNumSet = new HashSet<>();
		if(mntByAutNum.containsKey(maintainer)) {
			for(Long autNum : mntByAutNum.get(maintainer))
				autNumSet.add(autNum);
		}
		return autNumSet;			
	}
	
	/**
	 * Return the set of AutNums that are members-of an as-set
	 * @param setName name of as-set to query
	 * @return set's member autnums
	 */
	public Set<Long> getSetMemberAutNums(CIString setName) {
		Set<Long> autNumSet = new HashSet<>();
		if(setMemberAutNum.containsKey(setName)) {
			for(Long autNum : setMemberAutNum.get(setName))
				autNumSet.add(autNum);
		}
		return autNumSet;	
	}
}
