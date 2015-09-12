/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;
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
		
		assertTrue("only routes with matching member-of and mnt-by should be added to set with restrictive mbrs-by-ref",
				doc.routeSets.get("rs-set").resolve(doc).size() == 1);
		
		//Check that all routes with member-of are added to unrestricted set
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				routeMaintainedMember + routeMaintainedMember + routeOrphan + routeSetAny));
		assertTrue("all routes with matching member-of should be added to set with mbrs-by-ref: ANY",
				doc.routeSets.get("rs-set").resolve(doc).size() == 2);
		
		//Check that set w/ no mbrs-by-ref is empty
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				routeMaintainedMember + routeMaintainedMember + routeOrphan + routeSetEmpty));
		assertTrue("route set with no mbrs-by-ref should not load any member-of routes",
				doc.routeSets.get("rs-set").resolve(doc).size() == 0);
		
	}

}
