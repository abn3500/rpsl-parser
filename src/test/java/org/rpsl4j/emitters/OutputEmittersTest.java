/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters;

import static org.junit.Assert.*;

import org.junit.Test;
import org.rpsl4j.emitters.NullEmitter;
import org.rpsl4j.emitters.OutputEmitter;
import org.rpsl4j.emitters.OutputEmitters;


public class OutputEmittersTest {

	@Test
	public void getReturnsInstance() {
		for(String emitterName: OutputEmitters.scanClasspathForEmitters().keySet()) {
			assertTrue(emitterName + " must be instantiable",
					OutputEmitters.get(emitterName) instanceof OutputEmitter);
		}
	}
	
	@Test
	public void getInvalidNameReturnsNullEmitter() {
		assertTrue("Invalid named enum values should return NullEmitter",
			OutputEmitters.get("NOTATRUEVALUE") instanceof NullEmitter);
	}
}
