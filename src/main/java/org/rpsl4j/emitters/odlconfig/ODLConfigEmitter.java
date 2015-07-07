/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.odlconfig;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.rpsl4j.emitters.OutputEmitter;
import org.rpsl4j.emitters.rpsldocument.BGPAutNum;
import org.rpsl4j.emitters.rpsldocument.BGPInetRtr;
import org.rpsl4j.emitters.rpsldocument.BGPPeer;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslObject;

/**
 * Creates and emits an OpenDaylight BGP configuration file.
 * @author Benjamin George Roberts
 */
public class ODLConfigEmitter implements OutputEmitter {
	private static final String TEMPLATE_RESOURCE = "mustache/ODLConfigEmitter.mustache"; 
	Set<BGPInetRtr> speakerSet;
	Set<BGPPeer> peerSet;
	ODLReconnectStrategy reconnectStrategy = new ODLReconnectStrategy();
	
	@Override
	public String emit(Set<RpslObject> objects) {	
		//Generate the speaker and autnum sets
		speakerSet = generateSpeakers(objects, generateAutNums(objects));
		
		//Generate the peer set
		peerSet = generatePeers(speakerSet);
		
		//Instantiate the template engine and write the config
		Mustache templateRenderer = (new DefaultMustacheFactory()).compile(TEMPLATE_RESOURCE);
		StringWriter templateWriter = new StringWriter();
		
		templateRenderer.execute(templateWriter, this);
		templateWriter.flush();
		
		return templateWriter.toString();
	}

	@Override
	public void setArguments(Map<String, String> arguments) {
		//Update the reconnect strategy using provided arguments
		reconnectStrategy = new ODLReconnectStrategy(arguments);
	}
	
	/**
	 * Generate the set of peers declared by {@link BGPInetRtr}s.
	 * @param speakers Set of {@link BGPInetRtr}s
	 * @return Set of declared {@link BGPPeer}s
	 */
	static Set<BGPPeer> generatePeers(Set<BGPInetRtr> speakers) {
		HashSet<BGPPeer> peerSet = new HashSet<BGPPeer>();

		for(BGPInetRtr speaker : speakers) {
			peerSet.addAll(speaker.getPeers());
		}
		
		return peerSet;
	}
	
	/**
	 * Generate the set of {@link BGPInetRtr}s declared in the RPSL document.
	 * Peers must be members of declared {@link BGPAutNum} objects.
	 * @param objects Set of {@link RpslObject}s
	 * @param autNumMap Map of AS Numbers to {@link BGPAutNum}s to instantiate {@link BGPInetRtr} from
	 * @return Set of declared {@link BGPInetRtr}s
	 */
	static Set<BGPInetRtr> generateSpeakers(Set<RpslObject> objects, Map<String, BGPAutNum> autNumMap) {		
		HashSet<BGPInetRtr> speakerSet = new HashSet<BGPInetRtr>();
		
		//Iterate through object set to find inet-rtr objects
		for(RpslObject o : objects) {
			if(o.getType() != ObjectType.INET_RTR)
				continue;
			
			//get AS of inet-rtr
			String localAS = o.getValueForAttribute(AttributeType.LOCAL_AS).toString();
			
			if(!autNumMap.containsKey(localAS))
				continue; //TODO handle this case better
			
			speakerSet.addAll(BGPInetRtr.getSpeakerInstances(o, autNumMap.get(localAS)));
		}
		
		return speakerSet;

	}
	
	/**
	 * Instantiates {@link BGPAutNum} objects for each "aut-num" RPSL object
	 * in the provided set.
	 * @param objects Set of {@link RpslObject}s
	 * @return Map of AS Numbers to {@link BGPAutNum}s as declared in objects
	 */
	static Map<String, BGPAutNum> generateAutNums(Set<RpslObject> objects) {
		HashMap<String, BGPAutNum> autNumMap = new HashMap<String, BGPAutNum>();
		
		//Iterate through object set to find aut-num objects
		for(RpslObject o: objects) {
			if(o.getType() != ObjectType.AUT_NUM)
				continue;
			String asNumber = o.getTypeAttribute().getCleanValue().toString();
			
			//TODO check and handle case where asNumber is already inserted
			autNumMap.put(asNumber, new BGPAutNum(o));
		}
		
		return autNumMap;
	}
}
