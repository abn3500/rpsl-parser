/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import java.util.Set;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.io.RpslObjectStringReader;

import org.junit.Test;

public class BGPRouteSetTest {

	@Test
	public void byRefMembersTest() {
		String  routeMaintainedMember 	= "route: 1.1.1.0/24\norigin: AS1\nmnt-by: MNTR-ONE\nmember-of: rs-set\n\n",
				routeMember				= "route: 1.1.2.0/24\norigin: AS1\nmember-of: rs-set\n\n",
				routeOrphan				= "route: 1.1.3.0/24\norigin: AS1\n\n",
				routeSetByRef			= "route-set: rs-set\nmbrs-by-ref: MNTR-ONE\n\n",
				routeSetAny				= "route-set: rs-set\nmbrs-by-ref: ANY\n\n",
				routeSetEmpty			= "route-set: rs-set";
		
		//Check that mbrs-by-ref with a maintainer only gets the maintained, member-of route
		BGPRpslDocument doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				routeMaintainedMember + routeMember + routeOrphan + routeSetByRef));
		
		assertEquals("only routes with matching member-of and mnt-by should be added to set with restrictive mbrs-by-ref", 1,
				doc.routeSets.get("rs-set").resolve(doc).size());

		//double check it was the right one (I think I get the convoluted call award :L) 
		assertTrue(((BGPRpslRoute)doc.routeSets.get("rs-set").resolve(doc).iterator().next()).getMaintainer().equals(CIString.ciString("MNTR-ONE")));
		
		//Check that all routes with member-of are added to unrestricted set
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				routeMaintainedMember + routeMember + routeOrphan + routeSetAny));
		assertTrue("all routes with matching member-of should be added to set with mbrs-by-ref: ANY",
				doc.routeSets.get("rs-set").resolve(doc).size() == 2);
		
		
		
		//Check that set w/ no mbrs-by-ref is empty
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				routeMaintainedMember + routeMember + routeOrphan + routeSetEmpty));
		assertTrue("route set with no mbrs-by-ref should not load any member-of routes",
				doc.routeSets.get("rs-set").resolve(doc).size() == 0);
		
	}
	
	@Test
	public void explicitMembersTest() {
		String routeSetWithMember				= "route-set: rs-mem\nmembers: 1.1.1.1/16\n";
		String routeSetWithMemberAndRefMember	= "route-set: rs-mem\nmembers: 1.1.1.1/16\nmbrs-by-ref:MNTR-FOO\n";
		String routeMemberByRef					= "route: 2.2.2.2/8\norigin: AS5\nmnt-by: MNTR-FOO\nmember-of: rs-mem\n";
		
		BGPRpslDocument doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(routeSetWithMember + routeMemberByRef));
		//doc contains one route set which doesn't permit mbrsByRef. There should only be the explicitly declared route 1.1.1.1
		
		Set<BGPRoute> flattenedRoutes = doc.getRouteSet("rs-mem").resolve(doc);
		
		assertEquals(1, flattenedRoutes.size());
		assertEquals("1.1.1.1/16 via null", flattenedRoutes.iterator().next().toString());
		
		//test with mbrs-by-ref 
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(routeSetWithMemberAndRefMember + routeMemberByRef));
		flattenedRoutes = doc.getRouteSet("rs-mem").resolve(doc);
		
		//expect two routes this time
		boolean memRouteFound = false;
		boolean refRouteFound = false;
		assertEquals(2, flattenedRoutes.size());
		for(BGPRoute r : flattenedRoutes) {
			if(r.toString().equals("1.1.1.1/16 via null"))
				memRouteFound=true;
			if(r.toString().equals("2.2.2.2/8 via null"))
				refRouteFound=true;
		}
		//ensure we found those and only those
		assertTrue(memRouteFound && refRouteFound && flattenedRoutes.size()==2);
	}

}
