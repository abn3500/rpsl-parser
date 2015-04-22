/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import static org.junit.Assert.*;

import org.junit.Test;

import comp3500.abn.emitters.NullEmitter;
import comp3500.abn.emitters.OutputEmitter;
import comp3500.abn.emitters.OutputEmitters;


public class OutputEmittersTest {

	@Test
	public void getReturnsInstance() {
		for(OutputEmitters enumValue: OutputEmitters.values()) {
			assertTrue(enumValue.name() + " must be instantiable",
					enumValue.get() instanceof OutputEmitter);
		}
	}
	
	@Test
	public void getInvalidNameReturnsNullEmitter() {
		assertTrue("Invalid named enum values should return NullEmitter",
			OutputEmitters.get("NOTATRUEVALUE") instanceof NullEmitter);
	}

}
