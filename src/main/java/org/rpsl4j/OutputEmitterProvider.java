/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j;

import java.util.HashSet;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.rpsl4j.emitters.NullEmitter;
import org.rpsl4j.emitters.OutputEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enumeration of the classes implementing the {@link OutputEmitter} interface.
 * Construct and return new instances of these classes.
 * @author Benjamin George Roberts
 */
public class OutputEmitterProvider {

	private static ServiceLoader<OutputEmitter> emitterLoader = ServiceLoader.load(OutputEmitter.class);
	
    final static Logger log = LoggerFactory.getLogger(OutputEmitterProvider.class);



	/**
	 * Instantiates an {@link OutputEmitter} corresponding to the emitter service of matching class name.
	 * If the emitter cannot be instantiated an instance of the default emitter ({@link NullEmitter}) is returned.
	 * @return A new instance of the corresponding {@link OutputEmitter}
	 */
	public static OutputEmitter get(String className) {
		if(className != null) {
			for(OutputEmitter emitter : emitterLoader) {
				if(emitter.getClass().getName().equals(className))
					return emitter;
			}
			log.warn(className + " not found, falling back to NullEmitter");
		}
		
		return new NullEmitter();
	}

	public static OutputEmitter get(String className, Map<String, String> arguments) {
		OutputEmitter emitter = get(className);
		emitter.setArguments(arguments);
		return emitter;
	}

    public static Set<String> getEmitterList() {
        HashSet<String> emitterNames = new HashSet<>();
        for(OutputEmitter emitter : emitterLoader)
        	emitterNames.add(emitter.getClass().getName());
        
        return emitterNames;
    }
}
