/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import static org.junit.Assert.*;

import java.util.Set;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;

import org.junit.Test;

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
	
	
	

}
