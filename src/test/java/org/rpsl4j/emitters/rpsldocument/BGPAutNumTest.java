/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import java.util.Set;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.rpsl4j.emitters.rpsldocument.BGPAutNum;
import org.rpsl4j.emitters.rpsldocument.BGPRoute;

public class BGPAutNumTest {

	@Test
	public void getExportPeers() {
		String exportPeerString = 
				"export: 	to AS1 at 3.3.3.3" +
				"			to AS2 1.1.1.1 2.2.2.2 at 3.3.3.3\n" +
				"			announce AS3",
				trueMessage = "List should contain ((%s, %s), %s)",
				falseMessage = "List should not contain ((%s, %s), %s)";
		
		RpslAttribute exportAttr = new RpslAttribute(AttributeType.EXPORT, exportPeerString);
		Set<Pair<Pair<Long, String>, String>> peerList = BGPAutNum.getExportPeers(exportAttr);		
	
		assertTrue(String.format(trueMessage, "1", "0.0.0.0", "3.3.3.3"), peerList.contains(Pair.of(Pair.of(1l, "0.0.0.0"), "3.3.3.3")));
		assertTrue(String.format(trueMessage, "2", "1.1.1.1", "3.3.3.3"), peerList.contains(Pair.of(Pair.of(2l, "1.1.1.1"), "3.3.3.3")));
		assertTrue(String.format(trueMessage, "2", "2.2.2.2", "3.3.3.3"), peerList.contains(Pair.of(Pair.of(2l, "2.2.2.2"), "3.3.3.3")));
		assertFalse(String.format(falseMessage, "2", "0.0.0.0", "3.3.3.3"), peerList.contains(Pair.of(Pair.of(2l, "0.0.0.0"), "3.3.3.3")));
	}
	
	@Test
	public void getExportPeersRequiresLocalRouter() {
		String exportPeerString = 
				"export: 	to AS1\n" +
			    "			to AS2 1.1.1.1\n" + 
			    "			announce AS3";
		
		RpslAttribute exportAttr = new RpslAttribute(AttributeType.EXPORT, exportPeerString);
		Set<Pair<Pair<Long, String>, String>> peerList = BGPAutNum.getExportPeers(exportAttr);
		assertTrue("Export Peers should not add peers with missing local routers", peerList.size() == 0);
	}
	
	@Test
	public void generateRouteMaps() {
		String autNumString = 
					"aut-num:  AS1\n"
					+ "as-name:  AARNET-NT-RNO\n"
					+ "export: 	to AS2 1.1.1.1 1.1.1.2 at 8.8.8.8\n"
					+ "			to AS3 1.1.1.3 at 8.8.8.8\n"
					+ "			to AS3 1.1.1.3 at 9.9.9.9\n"
					+ "			to AS4 at 8.8.8.8"
					+ "			announce 2.2.1.0/24 2.2.2.0/23^+",
			   message = "Route map should contain following entry: \"%s\" -> \"%s\"";
		RpslObject rpslAutNum = RpslObject.parse(autNumString);
		BGPAutNum bgpAutNum = new BGPAutNum(rpslAutNum);
		
		//Build routes that should be present
		BGPRoute routeOne 	= new BGPRoute(AddressPrefixRange.parse("2.2.1.0/24") , "8.8.8.8"),
				 routeTwo 	= new BGPRoute(AddressPrefixRange.parse("2.2.1.0/24") , "9.9.9.9"),
				 routeThree = new BGPRoute(AddressPrefixRange.parse("2.2.2.0/23^+") , "8.8.8.8"),
				 routeFour 	= new BGPRoute(AddressPrefixRange.parse("2.2.2.0/23^+") , "8.8.8.8");
		Pair<Long, String> peerOne 	= Pair.of(2l, "1.1.1.1"),
							 peerTwo 	= Pair.of(2l, "1.1.1.2"),
							 peerThree	= Pair.of(3l, "1.1.1.3"),
							 peerFour   = Pair.of(4l, "0.0.0.0");
		assertTrue(String.format(message, peerOne, routeOne), bgpAutNum.includedRouteMap.containsEntry(peerOne, routeOne));
		assertTrue(String.format(message, peerOne, routeThree), bgpAutNum.includedRouteMap.containsEntry(peerOne, routeThree));
		assertTrue(String.format(message, peerTwo, routeOne), bgpAutNum.includedRouteMap.containsEntry(peerTwo, routeOne));
		assertTrue(String.format(message, peerTwo, routeThree), bgpAutNum.includedRouteMap.containsEntry(peerTwo, routeThree));
		assertTrue(String.format(message, peerThree, routeTwo), bgpAutNum.includedRouteMap.containsEntry(peerThree, routeTwo));
		assertTrue(String.format(message, peerThree, routeFour), bgpAutNum.includedRouteMap.containsEntry(peerThree, routeFour));
		assertTrue(String.format(message, peerFour, routeOne), bgpAutNum.includedRouteMap.containsEntry(peerThree, routeOne));
		assertTrue(String.format(message, peerFour, routeThree), bgpAutNum.includedRouteMap.containsEntry(peerThree, routeThree));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void checkConstructorTypeAssertion() {
		(new BGPAutNum(RpslObject.parse("route: 1.0.0.0/8\norigin: AS1"))).toString();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void checkExportAttributeTypeAssertion() {
		BGPAutNum.getExportPeers(new RpslAttribute(AttributeType.DEFAULT, "default: to AS1"));
	}
	
	@Test
	public void checkToString() {
		String autNumString = 
				"aut-num:  AS1\n"
				+ "as-name:  AARNET-NT-RNO\n";
		BGPAutNum autNum = new BGPAutNum(RpslObject.parse(autNumString));
		assertTrue("AARNET-NT-RNO (AS1)".equals(autNum.toString()));
	}
	
	@Test
	public void checkGetAsOfPeer() {
		BGPAutNum autNum = new BGPAutNum(RpslObject.parse( 
				"aut-num: AS1\n"
				+ "as-name: TEST-AS\n"
				+ "export: to AS2 1.1.1.1 at 1.1.1.1 announce 1.1.1.0/24"));
		
		assertEquals("Should return AS of requested peer", 2, autNum.getASOfPeer("1.1.1.1"));
		assertTrue("Should return null for non-existant peer", autNum.getASOfPeer("1.2.3.4") == -1);
	}
	
	@Test
	public void resolveActions() {
		RpslAttribute exportAttr = new RpslAttribute(AttributeType.EXPORT, 
				"export: to AS1 1.1.1.1 at 1.1.1.1 action pref = 10; community .= 11\n"
				+ "to AS1 at 1.1.1.1 action pref = 11;\n"
				+ "to AS1 at 1.1.1.1;\n"
				+ "announce 0.0.0.0/0");
		Pair<Pair<Long, String>, String> exportPeer = Pair.of(Pair.of(1l, "1.1.1.1"), "2.2.2.2");
				
		BGPAutNum.resolveActions(exportAttr, exportPeer);
	}

}
