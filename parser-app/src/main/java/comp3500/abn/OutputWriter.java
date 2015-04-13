/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;

import comp3500.abn.emitters.OutputEmitter;
import net.ripe.db.whois.common.rpsl.RpslObject;

public class OutputWriter {

	OutputEmitter outputEmitter; //= defaultEmitter; //TODO:
	Set<RpslObject> rpslObjects;

	//TODO:Not implemented
//	public OutputWriter(Set<RpslObject> rpslObjects) {
//		this.rpslObjects = rpslObjects;
//	}
	
	public OutputWriter(Set<RpslObject> rpslObjects, OutputEmitter emitter) {
		this.rpslObjects = rpslObjects;
		this.outputEmitter = emitter;
	}
	
	public String toString() {
		return outputEmitter.emit(rpslObjects);
	}
	
	public void writeToFile(String path) throws IOException { //TODO: See about the flag that was the second param here..
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		writer.write(outputEmitter.emit(rpslObjects));
		writer.close();
	}

}
