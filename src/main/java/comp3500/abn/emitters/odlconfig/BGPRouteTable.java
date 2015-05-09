/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;
import net.ripe.db.whois.common.rpsl.attrs.IPAddress;

import org.apache.commons.lang3.tuple.Pair;

/**
 * BGPRouteTable represents the a table taht can be exported to an ODL configuration
 * @author Benjamin George Roberts
 */
public class BGPRouteTable implements Iterable<BGPRoute> {
	String peerAddress, peerAutNum, speakerName, tableName;
	Set<BGPRoute> routeSet;
	
	/**
	 * Construct a new BGPRouteTable using the provided fields and route set
	 * @param peerAutNum AS the table is exported to
	 * @param peerAddress Address of the peer the table is exported to (can be {@link BGPRoute#ANY_ADDRESS})
	 * @param speakerName Name of the AutNum the speaker is associated with
	 * @param routeSet Routes to populate table with
	 * @throws AttributeParseException
	 */
	public BGPRouteTable(String peerAutNum, String peerAddress,
			String speakerName, Collection<BGPRoute> routeSet) {
		//Sanity check arguments, will throw AttributeParseException on bad arguments
		try {
			AutNum.parse(peerAutNum);
		} catch (AttributeParseException e) {
			throw new IllegalArgumentException("Illegal peer ASN: " + peerAutNum);
		}
		try {
			IPAddress.parse(peerAddress);
		} catch (AttributeParseException e)
		{
			throw new IllegalArgumentException("Illegal peer address: " + peerAddress);
		}
		
		//Store fields
		this.peerAddress = peerAddress;
		this.peerAutNum = peerAutNum;
		this.speakerName = speakerName;
		this.routeSet = new HashSet<BGPRoute>(routeSet);
		
		//Assign table name, using "ANY" if the peer address is 0.0.0.0
		if(peerAddress.equals(BGPRoute.ANY_ADDRESS)) {
			this.tableName = String.format("%s(ANY)-in-%s", peerAutNum, speakerName);
		} else {
			this.tableName = String.format("%s(%s)-in-%s",peerAutNum, peerAddress, speakerName);
		}
	}
	
	/**
	 * Construct a new BGPRouteTable by querying a {@link BGPAutNum} instance for routes
	 * @param peerAutNum AS the table is exported to
	 * @param peerAddress Address of the peer the table is exported to (can be {@link BGPRoute#ANY_ADDRESS})
	 * @param speakerAutNum AutNum object speaker originates from. Used to lookup route table
	 * @throws AttributeParseException
	 */
	public BGPRouteTable(String peerAutNum, String peerAddress, BGPAutNum speakerAutNum) {
		//Initialise with empty route set
		this(peerAutNum, peerAddress, speakerAutNum.name, new HashSet<BGPRoute>());
		
		//Attempt to retrieve table from speakerAutNum
		Pair<String, String> tableIdentifier = Pair.of(peerAutNum, peerAddress);
		if(speakerAutNum.includedRouteMap.containsKey(tableIdentifier)) 
			this.routeSet = new HashSet<BGPRoute>(speakerAutNum.includedRouteMap.get(tableIdentifier));
	}
	
	/**
 	 * Construct a new BGPRouteTable by querying a {@link BGPAutNum} instance for routes exported to an entire AS
	 * @param peerAutNum AS the table is exported to
	 * @param speakerAutNum AutNum object speaker originates from. Used to lookup route table
	 * @throws AttributeParseException
	 */
	public BGPRouteTable(String peerAutNum, BGPAutNum speakerAutNum) {
		this(peerAutNum, BGPRoute.ANY_ADDRESS, speakerAutNum);
	}
	
	@Override
	public String toString() {
		return tableName;
	}

	@Override
	public Iterator<BGPRoute> iterator() {
		return routeSet.iterator();
	}
	
	
}
