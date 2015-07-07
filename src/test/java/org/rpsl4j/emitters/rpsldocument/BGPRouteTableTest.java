/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;

import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;

import org.junit.BeforeClass;
import org.junit.Test;
import org.rpsl4j.emitters.rpsldocument.BGPAutNum;
import org.rpsl4j.emitters.rpsldocument.BGPRoute;
import org.rpsl4j.emitters.rpsldocument.BGPRouteTable;

public class BGPRouteTableTest {
	private static BGPAutNum autNum;
	
	@BeforeClass
	public static void createAutNum() {
		String autNumString = "aut-num: AS1\n"
				+ "as-name: TEST-AS\n"
				+ "export: to AS2 at 1.1.1.1\n"
				+ "		announce 2.2.2.0/24\n"
				+ "export: to AS2 1.2.3.4 at 1.1.1.1\n"
				+ "		announce 2.2.3.0/24\n";
		BGPRouteTableTest.autNum = new BGPAutNum(RpslObject.parse(autNumString));
	}

	@Test
	public void explicitConstructor() {
		BGPRoute routeOne = new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), "1.1.1.1"),
				 routeTwo = new BGPRoute(AddressPrefixRange.parse("1.1.2.0/24"), "2.2.2.2");
		Collection<BGPRoute> routes = new HashSet<BGPRoute>();
		routes.add(routeOne);
		routes.add(routeTwo);
		
		BGPRouteTable table = new BGPRouteTable(1, "3.3.3.3", "TEST-AS", routes);
		
		assertTrue("Table shoud contain routes passed in constructor", table.routeSet.contains(routeOne) && table.routeSet.contains(routeTwo));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void peerASSyntaxCheck() {
		(new BGPRouteTable(-1, "1.1.1.1", "speakerName", new HashSet<BGPRoute>())).toString();
	}
	@Test(expected=IllegalArgumentException.class)
	public void peerAddressSyntaxCheck() {
		(new BGPRouteTable(1, "1.1.1.1.1.1.1", "speakerName", new HashSet<BGPRoute>())).toString();
	}
	
	@Test
	public void bgpAutNumGetter() {
		BGPRouteTable peerTable = autNum.getTableForPeer(2, "1.2.3.4"),
					  asTable   = autNum.getTableForAS(2),
					  emptyPeerTable = autNum.getTableForPeer(2, "1.1.1.2");
		BGPRoute 	   asRoute = new BGPRoute(AddressPrefixRange.parse("2.2.2.0/24"), "1.1.1.1"),
					   peerRoute   = new BGPRoute(AddressPrefixRange.parse("2.2.3.0/24"), "1.1.1.1");
		
		assertTrue("Should return table containing peers route", peerTable.routeSet.contains(peerRoute));
		assertTrue("Should return table of routes shared by AS", asTable.routeSet.contains(asRoute) && ! asTable.routeSet.contains(peerRoute));
		assertTrue("Should return empty table for nonexistant peer", emptyPeerTable.routeSet.size() == 0);
	}
	
	@Test
	public void toStringTest() {
		BGPRouteTable peerTable = autNum.getTableForPeer(2, "1.2.3.4"),
					  asTable = autNum.getTableForAS(2);
		assertEquals("AS2(1.2.3.4)-in-TEST-AS", peerTable.toString());
		assertEquals("AS2(ANY)-in-TEST-AS", asTable.toString());
	}

}
