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
	BGPSpeaker speaker;
	String name,
		   peerAddress,
		   rib,
		   tableName;	
	/**
	 * Construct BGP Peer for a AS and with a given speaker
	 * @param speaker
	 */
	public BGPPeer(BGPSpeaker speaker, AutNum asn) {
		this.speaker = speaker;
		
		this.name = String.format("AS%s-in-%s", asn.getValue(), speaker.name);
		this.rib = name + "-rib";
		this.tableName = name + "-export";
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