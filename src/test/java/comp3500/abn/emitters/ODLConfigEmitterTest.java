/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.ripe.db.whois.common.io.RpslObjectStringReader;
import net.ripe.db.whois.common.rpsl.RpslObject;

import org.junit.Before;
import org.junit.Test;

import comp3500.abn.emitters.odlconfig.BGPAutNum;
import comp3500.abn.emitters.odlconfig.BGPInetRtr;
import comp3500.abn.emitters.odlconfig.BGPPeer;

public class ODLConfigEmitterTest {
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
	
	private ODLConfigEmitter e;

	@Before
	public void newEmitter() {
		e = new ODLConfigEmitter();
	}
	
	@Test
	public void outputSanityTest() {
		assertTrue("Emitter outputs some sort of string",
				e.emit(getObjects(AUTNUM_EXAMPLE + SPEAKER_EXAMPLE)).length() > 0);
	}
	
	@Test
	public void generatePeers() {
		Set<RpslObject> objects = getObjects(AUTNUM_EXAMPLE + SPEAKER_EXAMPLE);
		Map<String, BGPAutNum> autNums = ODLConfigEmitter.generateAutNums(objects);
		Set<BGPPeer> peerSet = ODLConfigEmitter.generatePeers(
				ODLConfigEmitter.generateSpeakers(objects, autNums));
		
		assertEquals("Should generate a peer object for peer of each speaker declared in the RPSL document", 5, peerSet.size());
		
	}
	
	@Test
	public void generatesAutNums() {
		Set<RpslObject> objects = getObjects(AUTNUM_EXAMPLE);
		
		Map<String, BGPAutNum> autNums = ODLConfigEmitter.generateAutNums(objects);
		
		assertEquals("Should generate autnum object for each in rpsl document", 2, autNums.size());
		assertTrue("AutNum Map should contain AS from RPSL document", autNums.containsKey("AS1"));
		assertTrue("AutNum Map should contain AS from RPSL document", autNums.containsKey("AS2"));
	}
	
	@Test 
	public void generatesSpeakers() {
		Set<RpslObject> objects = getObjects(AUTNUM_EXAMPLE + SPEAKER_EXAMPLE);
		
		Map<String, BGPAutNum> autNums = ODLConfigEmitter.generateAutNums(objects);
		Set<BGPInetRtr> speakerSet = ODLConfigEmitter.generateSpeakers(objects, autNums);
		
		assertEquals("Should generate speaker for each interface of RPSL inet-rtrs", 3, speakerSet.size());
	}
	
	/**
	 * Parse an RPSL document (multiple objects) and return the set
	 * of declared objects
	 * @param rpslString rpsl document as string
	 * @return Set of {@link RpslObject}s
	 */
	private static Set<RpslObject> getObjects(String rpslString) {
		Set<RpslObject> rpslSet = new HashSet<RpslObject>();

		for(String objString : (new RpslObjectStringReader(rpslString)))
			rpslSet.add(RpslObject.parse(objString));
		
		return rpslSet;
	}

}
