/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import java.util.Set;

import comp3500.abn.OutputWriter;

import net.ripe.db.whois.common.rpsl.RpslObject;

/**
 * Interface for {@link OutputEmitter}s used with {@link OutputWriter}
 * @author Benjamin George Roberts
 * @author Nathan Kelly
 */
public interface OutputEmitter {
	/**
	 * Emits the set of {@link RpslObject}'s in the format of the implementing OutputEmitter.
	 * @param objects The set of objects to emit
	 * @return String of formated objects
	 */
	public String emit(Set<RpslObject> objects);
}
