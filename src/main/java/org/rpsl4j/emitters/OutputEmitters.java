/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters;

import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import org.rpsl4j.emitters.NullEmitter;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;

import org.rpsl4j.emitters.odlconfig.ODLConfigEmitter;


/**
 * Enumeration of the classes implementing the {@link OutputEmitter} interface.
 * Construct and return new instances of these classes.
 * @author Benjamin George Roberts
 */
public class OutputEmitters {

	public static final String defaultEmitter = NullEmitter.class.getName();
	public static final Map<String, Class<OutputEmitter>> emitterRegistry = new HashMap<String, Class<OutputEmitter>>();
    public static final FastClasspathScanner cps = new FastClasspathScanner();
    

    static {
        scanClasspathForEmitters();
    }
    
    /**
     * Search the classpath for classes implementing {@link OutputEmitter} and add them to the emitter registry
     */
    public static Map<String, Class<OutputEmitter>> scanClasspathForEmitters() {
        //Clear registry and rescan classpath
        emitterRegistry.clear();
        cps.scan();
        
        for(String className : cps.getClassesImplementing(OutputEmitter.class)) {
            try {
                emitterRegistry.put(className, (Class<OutputEmitter>) Class.forName(className));
            } catch (ClassNotFoundException e) {
                System.err.println("Could not add class to emitterRegistry despite being subclass of OutputEmitter");
                e.printStackTrace();
            }
        }
        
        return emitterRegistry;
    }
    
    
	/**
	 * Instantiates an {@link OutputEmitter} corresponding to the class with the provided name in the emitter registry.
	 * If the emitter cannot be instantiated an instance of the default emitter ({@link NullEmitter}) is returned. 
	 * @return A new instance of the corresponding {@link OutputEmitter}
	 */
	public static OutputEmitter get(String className) {		
		OutputEmitter emitter;
        
        //Check if class exists in registry
        if(!emitterRegistry.containsKey(className)) {
			System.err.println("Illegal OutputEmitter in OutputEmitters (" +
							   className + ")");
            return get(defaultEmitter);
        }

		try {
            
			emitter = emitterRegistry.get(className).newInstance();
		} catch (InstantiationException | IllegalAccessException | NullPointerException e) {
			//If we can't instantiate we will return the default emitter
			System.err.println("Illegal OutputEmitter in OutputEmitters (" +
							   className + ")");
			e.printStackTrace();
			emitter = get(defaultEmitter);
		}
		
		return emitter;
	}
	
	public static OutputEmitter get(String className, Map<String, String> arguments) {
		OutputEmitter emitter = get(className);
		emitter.setArguments(arguments);
		return emitter;
	}
    
    public static Set<String> getEmitterList() {
        return emitterRegistry.keySet();
    }
}
