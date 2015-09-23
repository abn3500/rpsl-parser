/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters;

import java.util.Map;
import java.util.Set;
import org.rpsl4j.OutputEmitterProvider;

import net.ripe.db.whois.common.rpsl.RpslObject;

/**
 * Interface for {@link OutputEmitterProvider}
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
	
	/**
	 * Provide a set of arguments to the emitter instance. Implementers don't nessecarily have to accept arguments
	 * @param arguments Set of arguments to apply
	 */
	void setArguments(Map<String, String> arguments);

	/**
	 * Provide a map of valid arguments and their descriptions
	 * @return map of argument name to argument description
	 */
	public Map<String, String> validArguments();
}
