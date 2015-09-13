/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.Set;


import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.io.RpslObjectStringReader;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;

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
	
	
	//member of rs-bar
	private final String route_2_1 = "route: 1.1.1.0/24\n"
			+ "origin: AS2\n"
			+ "member-of: rs-bar\n";
			//+ "\n"; //a trailing newline really upsets the parser
	
	private final String ROUTE_OBJECTS =  "route: 2.1.1.1/8\n" //already explicitly in rs-foo
			+ "origin: AS1\n"
			+ "withdrawn: 30101231\n" //dec 31st, 3010
			+ "member-of: rs-foo\n"
			+ "\n"
			+ route_2_1 + "\n"
			+ "route: 100.1.1.0/24\n"
			+ "origin: AS5\n"
			+ "\n"
			+ "route: 200.0.0.0/16\n" //route originated by AS30
			+ "origin: AS30\n"
			+ "\n"
			+ "route: 150.0.0.10/8\n"
			+ "origin: AS200\n"
			+ "\n";
	

	
	private final String AUTNUM_OBJECTS =  "aut-num: AS30\n" //route 200.0.0.0/16\n above is implicitly a member of this AS
			+ "member-of: as-asRef\n"
			+ "mnt-by: MNTR-foobar\n";
	
	//TODO: as expression matching is NOT supported yet: https://tools.ietf.org/html/rfc2622#page-20
	
	//query: what should we do when we have a route from multiple sources: .. I guess keep both.. eg
	//eg. route 1.1.1.0/24 claims membership in rs-bar. rs-bar could reference a set using a prefix, which the route was also a member of.. now we have two copies with different prefixes.. spose that's ok. In any case, it's a user error realistically..
	
	
	//mappings:
	//rs-foo: rs-bar^, as-foo^
	
	private final String SET_OBJECTS =  "route-set: rs-foo\n"
			+ "members: 2.1.1.1/8, 10.0.0.0/8^16-24, rs-bar^16-24, as-foo^+\n"
			+ "\n"
			+ "route-set: rs-small\n"
			+ "members: 20.0.0.0/16^16-24\n"
			+ "\n"
			+ "route-set: rs-bar\n"
			+ "mbrs-by-ref: ANY\n" //expect to see (due to ref) route: 1.1.1.0/24 //TODO: does the main code actually do anything useful with a prefixed route; presumably ODL understands them..
			+ "\n"
			+ "as-set: as-asRef\n"
			+ "mbrs-by-ref: MNTR-foobar\n" //expect 200.0.0.0/16 of AS30
			+ "\n"
			+ "as-set: as-foo\n"
			+ "members: AS200\n" //expect 150.0.0.10/8 of AS200
			+ "\n";
	

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

	@Test
	public void dropWithdrawnRoute() {
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				"route: 1.1.1.0/24\n"
				+ "origin: AS1\n"
				+ "withdrawn: 19960624\n"));
		assertTrue("withdrawn route should not be added to as routes", doc.getASRoutes(1).size() == 0);
	}

	@Test
	public void processSets() {
		final String RPSL_STRING = ROUTE_OBJECTS + AUTNUM_OBJECTS + SET_OBJECTS;
		BGPRpslDocument doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(RPSL_STRING));
		//route objects, then set objects get parsed
		
		//check set names are being parsed correctly, and that basic mappings work
		CIString setName = CIString.ciString("as-asRef");
		BGPRpslSet set = doc.asSets.get(setName);
		assertEquals(setName, set.name);
		
		setName = CIString.ciString("rs-bar");
		set = doc.routeSets.get(setName);
		assertEquals(setName, set.name);
		
		
		//test simple route-set with single, prefixed member
		Set<BGPRoute> rs_small_contents = doc.getRouteSet("rs-small").resolve(doc); //expect 20.0.0.0/16^16-24
		assertEquals(1, rs_small_contents.size());
		assertEquals(new BGPRoute(AddressPrefixRange.parse("20.0.0.0/16^16-24"), null), rs_small_contents.iterator().next());
		
		
		//routes should be added by ref to sets with mbrs-by-ref: ANY
		Set<BGPRoute> rs_bar_contents = doc.getRouteSet("rs-bar").resolve(doc);
		assertEquals("resolving a route-set with no explicit members and one member added by ref, should yeild one route total", 1, rs_bar_contents.size());
		BGPRpslRoute expectedRoute = new BGPRpslRoute(RpslObject.parse(route_2_1)); //1.1.1.0/24
		assertEquals("Route in resolved route set should match route aded to the set by reference", expectedRoute, rs_bar_contents.iterator().next());
			
		//TODO: routes should be selectively added based on maintainer
	}

}
