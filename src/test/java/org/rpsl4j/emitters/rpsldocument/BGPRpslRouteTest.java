/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import org.junit.Test;

import net.ripe.db.whois.common.rpsl.RpslObject;

public class BGPRpslRouteTest {
	private final BGPRpslRoute 	routeOne = new BGPRpslRoute(RpslObject.parse("route: 1.1.1.0/24\norigin: AS1")),
								routeTwo = new BGPRpslRoute(RpslObject.parse("route: 1.1.2.0/24\norigin: AS2")),
								withdrawn = new BGPRpslRoute(RpslObject.parse("route: 1.1.1.0/8\norigin: AS1\nwithdrawn: 19960624")),
								notWithdrawn = new BGPRpslRoute(RpslObject.parse("route: 1.1.1.0/8\norigin: AS1\nwithdrawn: 30150101"));

	
	@Test
	public void testClone() {
		assertEquals("Clone should return equal object", routeOne, routeOne.clone());
		assertTrue("Clone should return new object", routeOne != routeOne.clone());
	}
	
	@Test
	public void testEquality() {
		assertTrue("Cloned object should be equal to original", routeOne.equals(routeOne.clone()));
		assertFalse("Different objects should not be equal", routeOne.equals(routeTwo));
	}
	
	@Test
	public void testWithdrawnRoute() {		
		assertTrue("route with withdrawn date in the past should return withdrawn", withdrawn.isWithdrawn());
		assertFalse("route with withdrawn date in future should return not withdrawn",notWithdrawn.isWithdrawn());	
		assertFalse("route with no withdrawn date should return not withdrawn", routeOne.isWithdrawn());	
	}
	
}
