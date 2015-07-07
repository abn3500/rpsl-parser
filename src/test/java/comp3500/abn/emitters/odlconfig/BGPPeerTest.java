/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;

import org.junit.Test;

public class BGPPeerTest {
	private static final String autNumString =
			"aut-num: AS1\n"
			+ "as-name: TEST-AS\n"
			+ "export: to AS2 at 1.1.1.1 announce 1.1.1.0/24\n"
			+ "export: to AS2 2.2.2.2 at 1.1.1.1 announce 1.1.2.0/24",
								inetRtrString =
			"inet-rtr: rtr1\n"
			+ "local-as: AS1\n"
			+ "ifaddr: 1.1.1.1 masklen 24\n"
			+ "ifaddr: 1.1.1.2 masklen 24\n"
			+ "peer: BGP4 2.2.2.2\n"
			+ "peer: BGP4 2.2.2.3";
	private static final BGPAutNum  autNum    = new BGPAutNum(RpslObject.parse(autNumString));
	private static final BGPInetRtr inetRtr   = new BGPInetRtr(RpslObject.parse(inetRtrString), "1.1.1.1", autNum);
	private static final BGPRoute   asRoute   = new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), "1.1.1.1"),
			                        peerRoute = new BGPRoute(AddressPrefixRange.parse("1.1.2.0/24"), "1.1.1.1"),
			                        extraRoute = new BGPRoute(AddressPrefixRange.parse("1.1.3.0/24"), "1.1.1.2");


	@Test(expected=IllegalArgumentException.class)
	public void checksSyntaxOfAS() {
		(new BGPPeer(-1, "2.2.2.2", inetRtr)).toString();
	}

	@Test(expected=IllegalArgumentException.class)
	public void checkSyntaxOfAddress() {
		(new BGPPeer(2, "2.2.2.2.2.2.2.2", inetRtr)).toString();
	}

	@Test
	public void checkToString() {
		assertEquals("AS2(2.2.2.2)-peer-of-TEST-AS(1.1.1.1)", (new BGPPeer(2, "2.2.2.2", inetRtr)).toString());
	}

	@Test
	public void checkAddRouteTable() {
		BGPPeer bgpPeerOne = new BGPPeer(2, "2.2.2.2", inetRtr),
				bgpPeerTwo = new BGPPeer(2, "2.2.2.3", inetRtr);

		//Test tables added in constructor
		assertTrue("Peer should contain own routes", bgpPeerOne.routes.contains(peerRoute));
		assertTrue("Peers in AS should contain AS common routes", bgpPeerOne.routes.contains(asRoute) && bgpPeerTwo.routes.contains(asRoute));
		assertFalse("Peers should not contain routes of other peers", bgpPeerTwo.routes.contains(peerRoute));

		//Test manunally adding new table
		Set<BGPRoute> routeSet = new HashSet<BGPRoute>();
		routeSet.add(extraRoute);
		BGPRouteTable table = new BGPRouteTable(2, "2.2.2.2", bgpPeerOne.speaker.speakerName, routeSet);
		assertTrue("Adding table that speaker doesn't have should suceed", bgpPeerOne.addRouteTable(table));
		assertTrue("Adding new routing table with unique route should increase size of peer routes", bgpPeerOne.routes.size() == 3);
		assertTrue("Adding table with new route should add route to peer", bgpPeerOne.routes.contains(extraRoute));
		assertFalse("Adding repeated table should have no effect", bgpPeerOne.addRouteTable(table));
	}

	@Test
	public void checkEquality() {
		assertEquals("Same peers should be equal", (new BGPPeer(2, "2.2.2.2", inetRtr)), (new BGPPeer(2, "2.2.2.2", inetRtr)));
		assertNotEquals("Different peers should not be equal", (new BGPPeer(2, "2.2.2.2", inetRtr)), (new BGPPeer(2, "2.2.2.3", inetRtr)));
	}

}
