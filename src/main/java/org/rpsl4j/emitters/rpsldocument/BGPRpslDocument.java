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

import net.ripe.db.whois.common.io.RpslObjectStreamReader;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslObject;

public class BGPRpslDocument {

	/**
	 * Object sets available for templates
	 */
	Set<RpslObject> rpslObjects = new HashSet<RpslObject>();
	Set<BGPAutNum> autNumSet = new HashSet<BGPAutNum>();
	Set<BGPInetRtr> inetRtrSet = new HashSet<BGPInetRtr>();
	Set<BGPPeer> peerSet = new HashSet<BGPPeer>();
	
	final static Logger log = LoggerFactory.getLogger(BGPRpslDocument.class);
	
	private Map<String, BGPAutNum> autNumMap = new HashMap<String, BGPAutNum>();
	
	public BGPRpslDocument(Set<RpslObject> rpslObjects) {
		this.rpslObjects = rpslObjects;
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
    			log.error("Unable to parse following object, skipping... ");
    			
    			//Print object excerpt
    			for(int i = 0; i < 3 && i < splitObject.length; i++) {
    				
    				if(i == 2) //We only printed part of the object
    					log.error("{}...", splitObject[i]);
    			}
    		}
    	}
		
		return new BGPRpslDocument(rpslObjectSet);
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
			autNumMap.put(asNumber, new BGPAutNum(o));
		}
		
		//cache and return
		this.autNumMap = autNumMap;
		return autNumMap;
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
}
