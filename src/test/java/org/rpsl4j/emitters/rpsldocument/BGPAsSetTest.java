/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;
import net.ripe.db.whois.common.io.RpslObjectStringReader;

import org.junit.Test;

public class BGPAsSetTest {

	@Test
	public void byRefMembersTest() {
		String  autNumMaintainedMember 	= "aut-num: AS1\nas-name:AS1\nmnt-by: MNTR-ONE\nmember-of: as-set\n\n"
											+ "route: 1.1.1.0/24\norigin: AS1\n\n",
				autNumMember			= "aut-num: AS2\nas-name:AS2\nmember-of: as-set\n\n"
											+ "route: 1.1.2.0/24\norigin: AS2\n\n",
				autNumOrphan			= "aut-num: AS3\nas-name:AS3\n\n"
											+ "route: 1.1.3.0/24\norigin: AS3\n\n",
				asSetByRef				= "as-set: as-set\nmbrs-by-ref: MNTR-ONE\n\n",
				asSetAny				= "as-set: as-set\nmbrs-by-ref: ANY\n\n",
				asSetEmpty				= "as-set: rs-set";
		
		//Check that mbrs-by-ref with a maintainer only gets the maintained, member-of aut-nums and routes
		BGPRpslDocument doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				autNumMaintainedMember + autNumMember + autNumOrphan + asSetByRef));
		
		assertEquals("only routes with origin matching member-of and mnt-by should be added to set with restrictive mbrs-by-ref", 1,
				doc.asSets.get("as-set").resolve(doc).size());
		
		//Check that all routes with member-of are added to unrestricted set
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				autNumMaintainedMember + autNumMember + autNumOrphan + asSetAny));
		assertTrue("all routes with origin matching member-of should be added to set with mbrs-by-ref: ANY",
				doc.asSets.get("as-set").resolve(doc).size() == 2);
		
		
		
		//Check that set w/ no mbrs-by-ref is empty
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(
				autNumMaintainedMember + autNumMember + autNumOrphan + asSetEmpty));
		assertTrue("as set with no mbrs-by-ref should not load any member-of routes",
				doc.asSets.get("rs-set").resolve(doc).size() == 0);
	}

}
