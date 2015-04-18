/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import comp3500.abn.emitters.OutputEmitter;
import comp3500.abn.emitters.OutputEmitters;
import net.ripe.db.whois.common.rpsl.RpslObject;

public class OutputWriter {

	OutputEmitter outputEmitter = OutputEmitters.defaultEmitter.get();
	Set<RpslObject> rpslObjects = new HashSet<RpslObject>();

	public OutputWriter(Set<RpslObject> rpslObjects) {
		this.rpslObjects = rpslObjects;
	}
	
	public OutputWriter(OutputEmitter emitter) {
		this.outputEmitter = emitter;
	}
	
	public OutputWriter(Set<RpslObject> rpslObjects, OutputEmitter emitter) {
		this(emitter);
		this.outputEmitter = emitter;
	}
	
	public String toString() {
		return outputEmitter.emit(rpslObjects);
	}
	
	public void writeToFile(String path) throws IOException { 
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		writer.write(outputEmitter.emit(rpslObjects));
		writer.close();
	}
	
	public void addObjects(Set<RpslObject> objects) {
		rpslObjects.addAll(objects);
	}
	
	public void addObject(RpslObject ... objects) {
		for(RpslObject object : objects)
			rpslObjects.add(object);	
	}
	
	public void removeObjects(Set<RpslObject> objects) {
		rpslObjects.removeAll(objects);
	}
	
	public void removeObject(RpslObject ... objects) {
		for(RpslObject object : objects)
			rpslObjects.remove(object);
	}
	
	public Set<RpslObject> getObjects() {
		return (new HashSet<>(rpslObjects));
	}
	
	
	
}
