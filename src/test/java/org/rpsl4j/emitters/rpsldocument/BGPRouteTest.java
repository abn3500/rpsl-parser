/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import java.util.Set;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;

import org.junit.Test;
import org.rpsl4j.emitters.rpsldocument.BGPRoute;

public class BGPRouteTest {
	String  routeOneString = "1.1.1.0/24",
			routeTwoString = "1.1.2.0/23^+";
	AddressPrefixRange routeOneRange = AddressPrefixRange.parse(routeOneString),
					   routeTwoRange = AddressPrefixRange.parse(routeTwoString);

	@Test
	public void equality() {
		assertTrue("Same routes should be equal", (new BGPRoute(routeOneRange, "1.1.1.1")).equals(new BGPRoute(routeOneRange, "1.1.1.1")));
		assertTrue("Same routes should be equal", (new BGPRoute(routeTwoRange, "1.1.1.2")).equals(new BGPRoute(routeTwoRange, "1.1.1.2")));
		assertFalse("Different routes should not equal", (new BGPRoute(routeOneRange, "1.1.1.1")).equals(new BGPRoute(routeTwoRange, "1.1.1.1")));
		assertFalse("Routes with same prefix but different next hops should not equal", 
				(new BGPRoute(routeOneRange, "1.1.1.1")).equals(new BGPRoute(routeOneRange, "1.1.1.2")));
	}
	
	@Test
	public void resolveRoutes() {
		String exportAttrString = 
					"export:"
					+ "		to AS1 1.1.1.1 at 1.1.1.2"
					+ "		announce 1.1.1.0/24 1.1.2.0/23^+",
			   message = "Route Set should contain declared routes";
		RpslAttribute exportAttr = new RpslAttribute(AttributeType.EXPORT, exportAttrString);
		Set<BGPRoute> routeSet = BGPRoute.resolveRoutes(exportAttr, "1.1.1.2");
		
		assertTrue(message, routeSet.contains(new BGPRoute(routeOneRange, "1.1.1.2")));
		assertTrue(message, routeSet.contains(new BGPRoute(routeTwoRange, "1.1.1.2")));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void checkConstructorTypeAssertion() {
		BGPRoute.resolveRoutes(new RpslAttribute(AttributeType.DEFAULT, "default: to AS1"), "1.1.1.1");
	}
	
	@Test
	public void failOnFilterExpression() {
		String  andNotFilter = "export to AS1 announce ASSET AND NOT 1.1.2.0/24",
				orFilter 	 = "export to AS1 announce ASSET OR 1.1.2.0/24";
		assertTrue("Filter expressions are not supported, they should return no routes",
					BGPRoute.resolveRoutes(new RpslAttribute(AttributeType.EXPORT, andNotFilter), "1.1.1.1").size() == 0);
		assertTrue("Filter expressions are not supported, they should return no routes",
				BGPRoute.resolveRoutes(new RpslAttribute(AttributeType.EXPORT, orFilter), "1.1.1.1").size() == 0);
		
	}
	
	@Test
	public void testEquality() {
		Object o = new Object();
		BGPRoute route = new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), "1.1.1.1"),
				 routeMatch = new BGPRoute(AddressPrefixRange.parse("1.1.1.1/24"), "1.1.1.1"),
				 routeDifferentSubnet = new BGPRoute(AddressPrefixRange.parse("1.1.1.0/25"), "1.1.1.1"),
				 routeDifferentNetwork = new BGPRoute(AddressPrefixRange.parse("1.1.2.0/24"), "1.1.1.1"),
				 routeDifferentNextHop = new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), "1.1.1.2");
		
		assertEquals(route, routeMatch);
		assertNotEquals(route, o);
		assertNotEquals(route, routeDifferentSubnet);
		assertNotEquals(route, routeDifferentNetwork);
		assertNotEquals(route, routeDifferentNextHop);
	}

}
