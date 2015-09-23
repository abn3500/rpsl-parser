/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import static org.junit.Assert.*;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.*;

public class BGPRpslSetTest {

	@Test
	public void splitPrefixTest() {
		String SIMPLE_REFERENCE = "rs-foo";
//		String SIMPLE_REFERENCE2 = "as-bar";
//		String SIMPLE_REFERENCE3 = "AS2";
		
		String PREFIXED_REFERENCE = "as-bar^+";
		//String PREFIXED_REFERENCE2 = "AS5^16-32";
		
		Pair<String, String> result;
		
		result = BGPRpslSet.splitPrefix(SIMPLE_REFERENCE);
		assertTrue("Reference should be extracted correctly", result.getLeft().equals("rs-foo"));
		assertTrue("Simple reference should have no prefix", result.getRight()==null);
		
		result = BGPRpslSet.splitPrefix(PREFIXED_REFERENCE);
		assertTrue("Set name should be extracted correctly", result.getLeft().equals("as-bar"));
		assertTrue("Prefix should be extracted correctly", result.getRight().equals("^+"));
	}
}
