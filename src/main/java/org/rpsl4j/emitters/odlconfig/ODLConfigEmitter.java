/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.odlconfig;

import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import org.rpsl4j.emitters.OutputEmitter;
import org.rpsl4j.emitters.rpsldocument.BGPInetRtr;
import org.rpsl4j.emitters.rpsldocument.BGPPeer;
import org.rpsl4j.emitters.rpsldocument.BGPRpslDocument;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;

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
		BGPRpslDocument doc = new BGPRpslDocument(objects);
		
		//Generate the speaker and autnum sets
		speakerSet = doc.getInetRtrSet();
		
		//Generate the peer set
		peerSet = doc.getPeerSet();
		
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
}
