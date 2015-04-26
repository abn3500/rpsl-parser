/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

/**
 * Represents the peer connections of the OpenDaylight configuration file.
 * @author Benjamin George Roberts
 */
public class BGPPeer {
	/*
	 * Template values
	 */
	AutNum peerASN = null;
	String peerName = null,
		   peerHost = "MISSING IP ADDRESS",
		   serverRegistry = null;
	
	/**
	 * Construct BGP Peer for a AS and with a given speaker
	 * @param speaker
	 */
	public BGPPeer(BGPSpeaker speaker, AutNum asn) {
		this.peerASN = asn;
		this.peerName = "AS" + asn.getValue();
		this.serverRegistry = speaker.serverRegistry; //Add to the parent speakers registry
	}
	
	/**
	 * Parses the attribute to extract routes imported or exported by this peer.
	 * @param attr Parsed attribute to retrieve routes from
	 * @param type type of route attribute (import, export, default)
	 */
	public void addRoutes(List<Pair<String, List<String>>> attr, AttributeType type) {
		//TODO implement
	}
}