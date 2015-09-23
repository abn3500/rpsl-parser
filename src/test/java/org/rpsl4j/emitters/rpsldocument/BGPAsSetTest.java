/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import java.util.Set;

import net.ripe.db.whois.common.io.RpslObjectStringReader;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;

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
	
	@Test
	public void recursiveResolveTest() {
		final String 	asOneRoute				= 	"aut-num: AS1\nas-name: AS1\n\n" +
													"route: 1.1.1.0/24\norigin: AS1\n\n",
						asTwoRouteDifferent		=	"aut-num: AS2\nas-name: AS2\n\n" +
													"route: 1.1.2.0/24\norigin: AS2\n\n",
						asTwoRouteSame			=	"aut-num: AS2\nas-name: AS2\n\n" +
													"route: 1.1.1.0/24\norigin: AS2\n\n",							
						asSetRoot				=	"as-set: as-root\nmembers: AS1, as-recur\n\n",
						asSetRecur				= 	"as-set: as-recur\nmembers: AS2\n\n",
						asSetRecurCyclic		= 	"as-set: as-recur\nmembers: AS2, as-root\n\n";
		
		//Test that set resolves child sets
		BGPRpslDocument doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(asOneRoute + asTwoRouteDifferent + asSetRoot + asSetRecur));
		Set<BGPRoute> resolvedRoutes = doc.getASSet("as-root").resolve(doc);
		
		assertEquals("AS set with recursive member should contain route from each", 2, resolvedRoutes.size());
		assertTrue("Root as set route is resolved", resolvedRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), null)));
		assertTrue("Root as set includes recursive members route", resolvedRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.2.0/24"),null)));
		
		//Test that unique route is only added once even if two sets
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(asOneRoute + asTwoRouteSame + asSetRoot + asSetRecur));
		resolvedRoutes = doc.getASSet("as-root").resolve(doc);
		
		assertEquals("Unique route should only be added to set once even if included in parent and child set", 1, resolvedRoutes.size());
		assertTrue(resolvedRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), null)));
		
		//Test that recursive and cyclic sets resolve correctly
		doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(asOneRoute + asTwoRouteDifferent + asSetRoot + asSetRecurCyclic));
		resolvedRoutes = doc.getASSet("as-root").resolve(doc);
		
		assertTrue("As sets with cyclic dependencies should contain all unique member routes", 
				resolvedRoutes.size() == 2 && 
				resolvedRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), null)) &&
				resolvedRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.2.0/24"),null)));
		assertEquals("Equivilent cycles of as sets contain the same members", doc.getASSet("as-root").resolve(doc), doc.getASSet("as-recur").resolve(doc));
	}
	
	@Test
	public void explicitMembersTest() {
		final String 	asOneRoute	= 	"aut-num: AS1\nas-name: AS1\n\n" +
										"route: 1.1.1.0/24\norigin: AS1\n\n",
						asTwoRoute	=	"aut-num: AS2\nas-name: AS2\n\n" +
										"route: 1.1.2.0/24\norigin: AS2\n\n",
						asSetOne 	= 	"as-set: as-setone\nmembers: AS1\n\n",
						asSetBoth	=	"as-set: as-setboth\nmembers: AS1, AS2\n\n";
		
		BGPRpslDocument doc = BGPRpslDocument.parseRpslDocument(new RpslObjectStringReader(asOneRoute + asTwoRoute + asSetOne + asSetBoth));
		Set<BGPRoute> 	setOneRoutes = doc.getASSet("as-setone").resolve(doc),
						setBothRoutes = doc.getASSet("as-setboth").resolve(doc);
		
		//Test that only included AS is resolved
		assertTrue("Only routes with origin of included AS should be resolved in as-set", setOneRoutes.size() == 1);
		assertTrue("Only routes with origin of included AS should be resolved in as-set", 
				setOneRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), null)) &&
				!setOneRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.2.0/24"), null)));
		
		//Test that set with two AS's has routes from both
		assertTrue("Set should resolve routes from all member AS's", setBothRoutes.size() == 2);
		assertTrue("Set should resolve routes from all member AS's",
				setBothRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.1.0/24"), null)) &&
				setBothRoutes.contains(new BGPRoute(AddressPrefixRange.parse("1.1.2.0/24"), null)));		
	}
}
