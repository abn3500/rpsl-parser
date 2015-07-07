/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import static org.junit.Assert.*;

import net.ripe.db.whois.common.rpsl.RpslObject;

import org.junit.Test;

public class BGPInetRtrTest {
	private static final String autNumString = 
			"aut-num: AS1\n"
			+ "as-name: TEST-AS\n"
			+ "export: to AS2 at 1.1.1.1 announce 1.1.1.0/24"
			+ "export: to AS2 2.2.2.2 at 1.1.1.1 announce 1.1.2.0/24"
			+ "export: to AS3 3.3.3.3.3 at 1.1.1.1 announce 1.1.3.0/24",
								inetRtrString = 
			"inet-rtr: rtr1\n"
			+ "local-as: AS1\n"
			+ "ifaddr: 1.1.1.1 masklen 24\n"
			+ "ifaddr: 1.1.1.2 masklen 24\n"
			+ "peer: BGP4 2.2.2.2";
	
	private static final RpslObject inetRtrObject = RpslObject.parse(inetRtrString); 
	private static final BGPAutNum  autNum = new BGPAutNum(RpslObject.parse(autNumString));

	 
	@Test
	public void checkGetSpeakerInstance() {
		assertTrue("Should return an intance for each unique ifaddr address", BGPInetRtr.getSpeakerInstances(inetRtrObject, autNum).size() == 2);
	}
	
	@Test
	public void checkEquality() {
		assertFalse("Different speaker instances should not be equal", 
				new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum).equals(
				new BGPInetRtr(inetRtrObject, "1.1.1.2", autNum)));
		assertTrue("Equal speaker instances should be equal", 
				new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum).equals(
				new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum)));
		assertFalse("InetRtr should not equal to other types", 
				(new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum)).equals(new Object()));
	}
	
	@Test
	public void checkToString() {
		BGPInetRtr speaker = new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum);
		assertEquals("TEST-AS(1.1.1.1)", speaker.toString());
	}
	
	@Test
	public void checkConstructorAddPeer() {
		BGPInetRtr speaker = new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum);
		assertTrue("speaker should create declared BGP peer", speaker.peers.contains(new BGPPeer(2, "2.2.2.2", speaker)));
	}

}
