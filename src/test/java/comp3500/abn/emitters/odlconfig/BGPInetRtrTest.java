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
			+ "export: to AS2 at 1.1.1.1 announce 1.1.1.0/24" //TODO: Should that be AS1, to test that we don't add a peer with no AS whatsoever? Also, isn't this syntax invalid? 'at' shouldn't be first.. update.. probably ok; optional attributes
			+ "export: to AS2 2.2.2.2 at 1.1.1.1 announce 1.1.2.0/24"
			+ "export: to AS3 3.3.3.3 at 1.1.1.1 announce 1.1.3.0/24"
			+ "export: to AS9 5.5.5.5 at 1.1.1.1 announce 1.1.3.0/24", //deliberately erronious asno
								inetRtrString = 
			"inet-rtr: rtr1\n"
			+ "local-as: AS1\n"
			+ "ifaddr: 1.1.1.1 masklen 24\n" //main link.. lets say (advertising all routes we care about on it)
			+ "ifaddr: 1.1.1.2 masklen 24\n"
			+ "peer: BGP4 2.2.2.2\n"
			+ "peer: BGP4 3.3.3.3 asno(AS3)\n" //would testing a contradictory ASNO be a bad idea.. hmm //TODO
			+ "peer: BGP4 4.4.4.4 flap_damp(), asno(AS4)\n" //hitherto unknown ASno. Also simulate other attrs
			+ "peer: BGP4 5.5.5.5 asno(AS5)";
	
	//AS2: ASno provided by route declaration "export: to AS2 at 1.1.1.1"
	//AS3: asno provided by both route declaration and peer asno attribute
	//AS4: asno provided only by peer asno attribute, which is also not the first attribute after ip this time
	//AS5: contradictory ASnos presented by route export above (suggesting AS9), and peer attr (specifying AS5). peer attr should be favoured. //TODO: yes?
	
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
	public void checkHashCode() { //TODO: baseline implementation. Come back later
		final BGPInetRtr speaker = new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum);
		final BGPInetRtr speaker2 = new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum);
		final BGPInetRtr speaker3 = new BGPInetRtr(inetRtrObject, "1.1.1.2", autNum); //slightly different.. hmm
		assertTrue("Hashcodes of equal object should match", speaker.hashCode()==speaker2.hashCode());
		assertFalse("Hashcodes of unequal object shouldn't match", speaker.hashCode()==speaker3.hashCode());
	}
	
	@Test
	public void checkConstructorAddPeer() {
		BGPInetRtr speaker = new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum);
		assertTrue("speaker should create declared BGP peer", speaker.peers.contains(new BGPPeer(2, "2.2.2.2", speaker)));
	}
	
	@Test
	public void checkPeerAsResolution() { //TODO: check over this, possibly extend it
		BGPInetRtr speaker = new BGPInetRtr(inetRtrObject, "1.1.1.1", autNum); //test speaker.. How do we decide *which* interface address is used in this constructor? Plainly there could be multiple, as demonstrated above
		assertTrue("speaker should utilise peer attrs to determine peer asno if possible", speaker.peers.contains(new BGPPeer(4, "4.4.4.4", speaker))); //check asno4 was picked up
		assertTrue("speaker should utilise known routes to determine peer asno if possible", speaker.peers.contains(new BGPPeer(2, "2.2.2.2", speaker))); //check asno2 was picked up from cache
		assertTrue("speaker should tolerate multiple asno sources", speaker.peers.contains(new BGPPeer(3, "3.3.3.3", speaker)));
		assertTrue("speaker should prefer peer attr asno source over resolution from known routes", speaker.peers.contains(new BGPPeer(5, "5.5.5.5", speaker)));
	}

}
