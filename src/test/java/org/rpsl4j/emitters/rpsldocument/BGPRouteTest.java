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
	
	@Test
	public void testClone() {
		BGPRoute route = new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), "1.1.1.1");
		assertEquals("Clone should return equal object", route, route.clone());
		assertTrue("Clone should return new object", route != route.clone());
		//TODO test route-object/action constructors
	}
	
	@Test
	public void appendPostFix() {
		final String ROUTE_NO_POSTFIX = "1.1.1.1/8";
		final String ROUTE_WITH_POSTFIX = "1.1.1.1/8^15-25";
		
		BGPRoute r;
		BGPRoute rp; //route with prefix

		//append prefix to non-prefixed route
		r = new BGPRoute(AddressPrefixRange.parse(ROUTE_NO_POSTFIX), null);
		r.appendPostfix("^16-24");
		assertEquals("1.1.1.1/8^16-24", r.routePrefixObject.toString());
		
		
		r = new BGPRoute(AddressPrefixRange.parse(ROUTE_NO_POSTFIX), null);
		r.appendPostfix("^+");
		
		//assertEquals("1.1.1.1/8^8-32", r.routePrefixObject.toString()); //valid if calculating all values
		assertEquals(new Integer(8), r.routePrefixObject.getRangeOperation().getN()); //valid if simply concatenating prefix..hmm
		assertEquals(new Integer(32), r.routePrefixObject.getRangeOperation().getM());
		
		r = new BGPRoute(AddressPrefixRange.parse(ROUTE_NO_POSTFIX), null);
		r.appendPostfix("^-");
		assertEquals("1.1.1.1/8^-", r.routePrefixObject.toString()); //"1.1.1.1/8^9-32" would also be valid
		//ensure internal value is correct; that this wasn't simply a string concat with no underlying effect
		assertEquals(new Integer(9), r.routePrefixObject.getRangeOperation().getN()); //n-m (again, it's not alphabetic - WHY I DO NOT KNOW)
		assertEquals(new Integer(32), r.routePrefixObject.getRangeOperation().getM());
		
		
		/************/
		
		//more specific (inclusive)
		rp = new BGPRoute(AddressPrefixRange.parse(ROUTE_WITH_POSTFIX), null);
		rp.appendPostfix("^+"); //expect ^15-25 to become ^15-32
		assertEquals("1.1.1.1/8^15-32", rp.routePrefixObject.toString());

		//more specific (exclusive) //TODO: find a clearer source on what this actually means - current interpretation is wrong, or addressprefixrange has a bug..
		rp = new BGPRoute(AddressPrefixRange.parse(ROUTE_WITH_POSTFIX), null);
		rp.appendPostfix("^-"); //expect ^15-25 to become ^16-32
		assertEquals("1.1.1.1/8^16-32", rp.routePrefixObject.toString());
		
		//^^This will fail if ^- isn't handled specifically 
		
		
		//test trying to make prefix less specific
		rp = new BGPRoute(AddressPrefixRange.parse("1.1.1.1/16^16"), null);
		rp.appendPostfix("^10-20"); //expect ^16-20
		assertEquals("1.1.1.1/16^16-20", rp.routePrefixObject.toString());
		
		//test out of range postfix
		rp = new BGPRoute(AddressPrefixRange.parse(ROUTE_WITH_POSTFIX), null);
		try {
			rp.appendPostfix("^8-10"); //8 is < 10, but max(8,15) is > 10. Our range is going backwards; the postfix should be dropped - edit; no, the spec could've been slightly clearer there: the ENTIRE address prefix.. meaning not just the prefix (yeah, totally clear :P) gets dropped. Ie {1.1.1.1/8^10-20}^7-9 should result in null, effectively.
			fail("Out of range prefix should throw exception");
		} catch (IllegalArgumentException e) {}
		//assertEquals(null, rp.routePrefixObject);
	
		//broaden range
		rp = new BGPRoute(AddressPrefixRange.parse(ROUTE_WITH_POSTFIX), null);
		rp.appendPostfix("^10-28");
		assertEquals("1.1.1.1/8^15-28", rp.routePrefixObject.toString());
		
		//shrink range
		rp = new BGPRoute(AddressPrefixRange.parse(ROUTE_WITH_POSTFIX), null);
		rp.appendPostfix("^17-22");
		assertEquals("1.1.1.1/8^17-22", rp.routePrefixObject.toString());

		//test single value prefix
		rp = new BGPRoute(AddressPrefixRange.parse(ROUTE_WITH_POSTFIX), null);
		rp.appendPostfix("^17");
		assertEquals("1.1.1.1/8^17", rp.routePrefixObject.toString()); //"^17 and ^17-17" are equivalent
		
		//test +/- change example from (updated) rfc: https://tools.ietf.org/html/rfc2622#page-6
		//{128.9.0.0/16^+}^-     == {128.9.0.0/16^-}
        //{128.9.0.0/16^-}^+     == {128.9.0.0/16^-}
		rp = new BGPRoute(AddressPrefixRange.parse("128.9.0.0/16^+"), null);
		rp.appendPostfix("^-");
		assertEquals("128.9.0.0/16^-", rp.routePrefixObject.toString());
		
		rp = new BGPRoute(AddressPrefixRange.parse("128.9.0.0/16^-"), null);
		rp.appendPostfix("^+");
		assertEquals("128.9.0.0/16^-", rp.routePrefixObject.toString());
		
		
		//equivalent to last
		rp = new BGPRoute(AddressPrefixRange.parse("128.9.0.0/16^17-32"), null);
		rp.appendPostfix("^+"); // this turns ^n-m into n-32. /16^17-32 is equivalent to /16^-
		assertEquals("128.9.0.0/16^-", rp.routePrefixObject.toString());
		
		//changing m (top of range) (^n-m) here should have no effect
		rp = new BGPRoute(AddressPrefixRange.parse("128.9.0.0/16^17-18"), null);
		rp.appendPostfix("^+"); //
		assertEquals("128.9.0.0/16^-", rp.routePrefixObject.toString());
		
		
		//^+ onto address where /n^n (n is mask length) - we expect just ^+
		rp = new BGPRoute(AddressPrefixRange.parse("128.9.0.0/16^16-18"), null);
		rp.appendPostfix("^+"); //expect ^16-x to become ^16-32 --> ^+
		assertEquals("128.9.0.0/16^+", rp.routePrefixObject.toString());
		
		//try to apply ^- to something that can't become any more specific
		rp = new BGPRoute(AddressPrefixRange.parse("128.9.0.0/16^32"), null); //TODO: Should ^32 be considered a valid final prefix.. seems logical; accept any exact address starting with 128.9.x.x
		try{
			rp.appendPostfix("^-");
			fail("Applying ^- prefix to ^32 address should invalidate it");
		} catch (IllegalArgumentException e) {}
		
		
		//logic notes:
		//^n-m
		//N can only go up. M is dictated by the outer prefix.
		//If however m becomes smaller than the new n, we have an invalid address. Throw everything out. Address == null
	}
}
