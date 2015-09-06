/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

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
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
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
	
    private Multimap<CIString, BGPRpslRoute>	setRoutes	= HashMultimap.create(); //routes by the set(s) they say they are members of (no double checking by listings in rs-set members attribute, and no validation against mbrsByRef maintainers)
    private Multimap<Long, BGPRpslRoute>		asRoutes	= HashMultimap.create(); //routes by the ASs the route states as its origin
	
    //Maps of route-set/as-set RPSL objects to java representations
    private Map<CIString, BGPRpslSet>		routeSets   = new HashMap<>(),
    										asSets		= new HashMap<>();
    
	private Map<String, BGPAutNum> autNumMap = new HashMap<String, BGPAutNum>();
	
	public BGPRpslDocument(Set<RpslObject> rpslObjects) {
		this.rpslObjects = rpslObjects;
		parseRpslRouteObjects();
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
	
	private void parseRpslRouteObjects() {
		for(RpslObject o : this.rpslObjects) {
			if(o.getType() != ObjectType.ROUTE)
				continue; //skip non Route objects

			//parse as BGPRoute, grab key info and add to index
			BGPRpslRoute bgpRoute = new BGPRpslRoute(o);
			
			if(bgpRoute.isWithdrawn())
				continue;
			
			asRoutes.put(bgpRoute.asNumber, bgpRoute);
			for(CIString set : bgpRoute.parentSets) {
				setRoutes.put(set, bgpRoute);
			}
		}
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
		
		HashMap<String, BGPAutNum> autNumMap = new HashMap<String, BGPAutNum>();
		
		//Iterate through object set to find aut-num objects
		for(RpslObject o: this.rpslObjects) {
			if(o.getType() != ObjectType.AUT_NUM)
				continue;
			String asNumber = o.getTypeAttribute().getCleanValue().toString();
			
			//TODO check and handle case where asNumber is already inserted
			autNumMap.put(asNumber, new BGPAutNum(o, this));
		}
		
		//cache and return
		this.autNumMap = autNumMap;
		return autNumMap;
	}
	
	/**
	 * Generate or return the BGPRpslSet object representing the route-set of the provided name
	 * @param setName name of route-set object to retrieve
	 * @return route-set object or null
	 */
	BGPRpslSet getRouteSet(String setName) {
		//Initialise routeSet mapping if empty
		if(routeSets.size() != 0) {
			for(RpslObject o : this.rpslObjects) {
				if(o.getType() != ObjectType.ROUTE_SET)
					continue;
				//TODO construct routeset object and add to routeSets
				
				//add routeset name to index if it doesn't exist. Get routeset object from mapping, fill in members as either route objects, as or route-set references (creating entries in route-set and as-set indexes if they don't already exist)
				CIString currentRSName = o.getValueForAttribute(AttributeType.ROUTE_SET);
				BGPRpslSet currentRS = addOrGetSetByName(currentRSName); //assuming here that the name starts with 'rs-', which it should given this is a route-set
				
				//can both be empty sets
				Set<CIString> membersStr = o.getValuesForAttribute(AttributeType.MEMBERS);
				Set<CIString> mbrsByRefStr = o.getValuesForAttribute(AttributeType.MBRS_BY_REF);
				
				Set<CIString> processedRoutes = new HashSet<CIString>();
				Set<CIString> withdrawnRoutes = new HashSet<CIString>();
				
				//early thinking:
				//check for Route objects that can become members of this set.. for those that can't.. we want to know if they are already explicitly members, in which case we add them.. else we should remove them from the mapping of setRoutes. Later, if we find a member route that matches a Route object we added earlier, we should skip adding that (minimal) version of the route.
				
				//for all routes that claim membership in this set
				for(BGPRoute route : setRoutes.get(currentRSName)) {
					
					//check if route expired TODO
//					if(route.isWithdrawn()) {
//						withdrawnRoutes.add(CIString.ciString(route.routePrefixObject.toString()));
//						continue;
//					}
						
					
					//check if that route already has explicit membership
					CIString routeSting = CIString.ciString(route.routePrefixObject.toString().toLowerCase().trim()); //perhaps some redundant string cleaning there..
					if(membersStr.contains(routeSting)) { //already an explicit member
						currentRS.addMember(new BGPSetMember(route));
						processedRoutes.add(routeSting); //note that this member has already been processed //TODO: could perhaps just remove from members list (only issue there is if we see duplicate routes here)
						continue;
					}
					
					//check maintainers
					if(mbrsByRefStr.contains("ANY")) { //if all membership requests granted regardless of maintainer
						currentRS.addMember(new BGPSetMember(route));
						continue;
					}
					
					//figure out who the route's maintainers are.. hmm TODO:
					currentRS.addMember(new BGPSetMember(route));
					System.out.println("WARNING: Filtering addition of routes to route-sets based on maintainers is unsupported. The route '" + route + "' has been added without this check");
				}
				
				//all explicitly listed members
				for(CIString memberStr : membersStr) {
					memberStr = CIString.ciString(memberStr.toLowerCase().trim()); //clean input
					
					//make sure this isn't a route we've already added a *proper* Route object for..
					if(processedRoutes.contains(memberStr))
						continue;
					
					//or that we found an expired Route object for.. the latter is an interesting case; should the route always be explicitly allowed because it's a member, or should we assume that the Route object is giving extra info, so use it's withdrawn date..
					if(withdrawnRoutes.contains(memberStr)) //TODO
						continue;
					
					CIString postFix = null;
					CIString memberWithoutPostfix = memberStr;
					if(memberStr.contains("^")) { //look for postfix
						String s = memberStr.toString();
						int prefixStartIndex = s.indexOf('^');
						postFix = CIString.ciString(s.substring(prefixStartIndex));
						System.out.println("DEBUG: postfix read as:" + postFix); //TODO:
						memberWithoutPostfix = CIString.ciString(s.substring(0, prefixStartIndex));
						System.out.println("DEBUG: member name read as:" + memberWithoutPostfix); //TODO:
					}

					try{
						BGPRpslSet referencedSet = addOrGetSetByName(memberWithoutPostfix); //try to parse member as route-set or as-set reference
						//if(set!=null) //TODO: this check depends on how we error out of the above method call
						currentRS.addMember(new BGPSetMember(referencedSet, postFix));
						continue;
					} catch (IllegalArgumentException e) { //didn't begin with as- or rs-
						//TODO
					}
					
					//parsing as a set reference failed. Try parsing as a route
					try {
						AddressPrefixRange addr = AddressPrefixRange.parse(memberStr); //include postfix this time; AddressPrefixRange knows exactly what to do with it
						BGPRoute route = new BGPRoute(addr, null); //nexthop unknown.. and we are adding a route we didn't find a corresponding Route object for. Issue a warning
						System.out.println("WARNING: " + currentRSName + " contains a member route (" + addr + ") with no corresponding Route object. This route has been maintained.");
						currentRS.addMember(new BGPSetMember(route));
						continue;
					} catch (AttributeParseException e) {
						//TODO
					}
					
					//not a set reference or a route. Could still be an AS
					try { //can we have AS1^+ ?? better safe than sorry.. //TODO
						AutNum as = AutNum.parse(memberWithoutPostfix);
						currentRS.addMember(new BGPSetMember(as, postFix));
						System.out.println("DEBUG: added AS member to set '" + currentRSName + "': " + as + " with postfix: " + postFix);
						continue;
					} catch (AttributeParseException e) {
						
					}
					
					//shit happened
					System.out.println("DEBUG: shit happens");
					
				} //end for (route-set members)
			
				routeSets.put(currentRSName, currentRS);
			} //end for (route set)
			
		} //end if (routeSets empty)
		
		return routeSets.get(setName);
	}
	
	/**
	 * Get set reference by name (to allow references to *other* sets to be inserted as members of a set)
	 * @param name
	 * @return
	 */
	private BGPRpslSet addOrGetSetByName(CIString name) {
		if(name.startsWith("rs-")) { //approach 1
			if(routeSets.containsKey(name))
				return routeSets.get(name);
			else {
				BGPRpslSet tempSet = new BGPRouteSet(name);
				routeSets.put(name, tempSet);
				return tempSet;
			}
		}
		else if(name.startsWith("as-")) { //approach 2; neater, but perhaps slightly less efficient (saves on a memory allocation though)
			if(asSets.containsKey(name))
				asSets.put(name, new BGPAsSet(name));
			
			return asSets.get(name);
		} else {
			//TODO: complain
			throw new IllegalArgumentException("Recieved unsupported rpsl set prefix; only 'rs-' and 'as-' are currently supported");
		}
			
			
	}
	
	/**
	 * Generate or return the BGPRpslSet object representing the as-set of the provided name
	 * @param setName name of as-set object to retrieve
	 * @return as-set object or null
	 */
	BGPRpslSet getASSet(String setName) {
		//Initialise routeSet mapping if empty
		if(routeSets.size() != 0) {
			for(RpslObject o : this.rpslObjects) {
				if(o.getType() == ObjectType.AS_SET)
					continue;
				//TODO construct asSet object and add to asSets
			}
		}
		
		return asSets.get(setName);
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
		if(asRoutes.containsKey(autNum)) {
			for(BGPRoute r : asRoutes.get(autNum))
				routeSet.add(r.clone());
		}
		return routeSet;
	}
}
