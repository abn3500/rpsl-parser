/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import java.util.Map;

import net.ripe.db.whois.common.io.RpslObjectStringReader;

import org.junit.Test;

public class BGPRpslDocumentTest {
	private final String AUTNUM_EXAMPLE = "aut-num: AS1\n"
			+ "as-name: First AS\n"
			+ "export: to AS3 2.2.2.1 at 1.1.1.1 announce 3.3.3.0/24\n\n"
			+ "aut-num: AS2\n"
			+ "as-name: Second AS\n"
			+ "export: to AS3 2.2.2.2 2.2.2.3 at 1.1.1.1 announce 3.3.3.0/24\n\n";
	private final String SPEAKER_EXAMPLE = "inet-rtr: router1\n"
			+ "local-as: AS1\n"
			+ "ifaddr: 1.1.1.1 masklen 24\n"
			+ "peer: BGP4 2.2.2.1\n\n"
			+ "inet-rtr: router2\n"
			+ "local-as: AS2\n"
			+ "ifaddr: 1.1.1.2 masklen 24\n"
			+ "ifaddr: 1.1.1.3 masklen 24\n"
			+ "peer: BGP4 2.2.2.2\n"
			+ "peer: BGP4 2.2.2.3\n\n";
	
	private final String AUTNUM_ROUTE =  "aut-num: AS1\n"
			+ "as-name: First AS\n"
			+ "export: to AS3 2.2.2.1 at 1.1.1.1 announce AS1\n\n"
			+ "route: 1.1.1.0/24\n"
			+ "origin: AS1\n";

	BGPRpslDocument doc;

	@Test
	public void generatePeers() {
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(AUTNUM_EXAMPLE + SPEAKER_EXAMPLE));
		
		assertEquals("Should generate a peer object for peer of each speaker declared in the RPSL document", 5, doc.getPeerSet().size());
	}
	
	@Test
	public void generatesAutNums() {
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(AUTNUM_EXAMPLE));
		
		Map<String, BGPAutNum> autNums = doc.getAutNumMap();
		
		assertEquals("Should generate autnum object for each in rpsl document", 2, autNums.size());
		assertTrue("AutNum Map should contain AS from RPSL document", autNums.containsKey("AS1"));
		assertTrue("AutNum Map should contain AS from RPSL document", autNums.containsKey("AS2"));
	}
	
	@Test 
	public void generatesSpeakers() {
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(AUTNUM_EXAMPLE + SPEAKER_EXAMPLE));
		
		assertEquals("Should generate speaker for each interface of RPSL inet-rtrs", 3, doc.getInetRtrSet().size());
	}
	
	@Test
	public void autNumRoutes() {
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(AUTNUM_ROUTE));
		assertTrue("Should return BGPRoute object for route with origin AS1", doc.getASRoutes(1).size() == 1);
		
		//Test that autnum route got added to peer
		BGPAutNum autNum = doc.getAutNumMap().get("AS1");
		assertTrue("Peer route table should contain route with origin AS1", 
				autNum.getTableForPeer(3, "2.2.2.1").routeSet.size() == 1);
	}


}
