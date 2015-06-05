/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import java.util.HashSet;
import java.util.Set;

import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;
import net.ripe.db.whois.common.rpsl.attrs.IPAddress;

/**
 * Representation of a BGP peer that can be emitted into an ODL configuration
 * @author Benjamin George Roberts
 */
public class BGPPeer {
	protected Set<BGPRouteTable> routeTables = new HashSet<BGPRouteTable>();
	protected Set<BGPRoute> routes = new HashSet<BGPRoute>();
	protected BGPInetRtr speaker;
	protected String peerAddress, peerRegistry, name;
	protected long peerAutNum;

	/**
	 * Construct a new BGPPeer of a given InetRtr/Speaker
	 * @param peerAutNum AS of the peer
	 * @param peerAddress Address of the peer
	 * @param speaker BGP Speaker peered with this object
	 */
	public BGPPeer(long peerAutNum, String peerAddress, BGPInetRtr speaker) {
		//sanity check parameters
		if(peerAutNum <= 0)
			throw new IllegalArgumentException("Illegal peer ASN: " + peerAutNum);
		try {
			IPAddress.parse(peerAddress);
		} catch (AttributeParseException e)
		{
			throw new IllegalArgumentException("Illegal peer address: " + peerAddress);
		}

		//Store fields
		this.peerAutNum = peerAutNum;
		this.peerAddress = peerAddress;
		this.peerRegistry = speaker.peerRegistry;
		this.speaker = speaker;
		this.name = String.format("AS%d(%s)-peer-of-%s(%s)", peerAutNum, peerAddress, speaker.autNumObject.name, speaker.speakerAddress);

		//Add routes
		addRouteTable(speaker.autNumObject.getTableForAS(peerAutNum));
		addRouteTable(speaker.autNumObject.getTableForPeer(peerAutNum, peerAddress));
	}

	/**
	 * Add a routing table to the BGPPeer. Will not add tables already included in peer
	 * Will update both the set of routing tables and set of routes.
	 * @param newTable table to add to peer
	 * @return false if peer already had table
	 */
	public boolean addRouteTable(BGPRouteTable newTable) {
		if(routeTables.contains(newTable))
			return false;

		routeTables.add(newTable);
		routes.addAll(newTable.routeSet);
		return true;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if(!(o instanceof BGPPeer))
			return false;

		BGPPeer other = (BGPPeer) o;
		return this.speaker.equals(other.speaker) && this.peerAddress.equals(other.peerAddress) && this.peerAutNum == other.peerAutNum;
	}

}
