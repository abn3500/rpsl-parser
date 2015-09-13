/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j;

import static org.junit.Assert.*;

import org.junit.Test;
import org.rpsl4j.OutputEmitterProvider;
import org.rpsl4j.emitters.NullEmitter;


public class OutputEmitterProviderTest {
	
	@Test
	public void getInvalidNameReturnsNullEmitter() {
		assertTrue("Invalid named enum values should return NullEmitter",
			OutputEmitterProvider.get("NOTATRUEVALUE") instanceof NullEmitter);
	}
}
