/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import java.util.Set;

import net.ripe.db.whois.common.rpsl.RpslObject;

/**
 * Empty implementation of {@link OutputEmitter}. Ignores all input and returns the empty string.
 * Used as the fall back emitter in {@link OutputEmitters}.
 * @author Benjamin George Roberts
 */
public class NullEmitter implements OutputEmitter {

	@Override
	public String emit(Set<RpslObject> objects) {
		return "";
	}

}
