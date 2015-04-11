/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;


/**
 * Enumeration of the classes implementing the {@link OutputEmitter} interface.
 * Construct and return new instances of these classes.
 * @author Benjamin George Roberts
 */
public enum OutputEmitters {
	NULL(NullEmitter.class),
	XML(XMLEmitter.class),
	;

	private Class<OutputEmitter> emitterClass;
	public static final OutputEmitters defaultEmitter = NULL;
	
	private OutputEmitters(Class emitterClass) {
		this.emitterClass = emitterClass;
	}
	
	/**
	 * Instantiates an {@link OutputEmitter} corresponding with the Enumerator's value.
	 * If the emitter cannot be instantiated an instance of the default emitter ({@link NullEmitter}) is returned. 
	 * @return A new instance of the corresponding {@link OutputEmitter}
	 */
	public OutputEmitter get() {		
		OutputEmitter emitter;

		try {
			emitter = this.emitterClass.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			//If we can't instantiate we will return the default emitter
			System.err.println("Illegal OutputEmitter in OutputEmitters (" +
							   this.name() + ")");
			e.printStackTrace();
			emitter = defaultEmitter.get();
		}
		
		return emitter;
	}
	
	/**
	 * Instantiates an {@link OutputEmitter} corresponding with the enumerator matching the name parameter.
	 * If the emitter cannot be instantiated an instance of the default emitter ({@link NullEmitter}) is returned. 
	 * @param name name of {@link OutputEmitter} to instantiate
	 * @return A new instance of the corresponding {@link OutputEmitter}
	 */
	public static OutputEmitter get(String name) {
		try {
			return OutputEmitters.valueOf(name.toUpperCase()).get();
		} catch (IllegalArgumentException e) {
			return defaultEmitter.get();
		}
	}
	
}
