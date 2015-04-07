package comp3500.abn;

import java.util.List;

import comp3500.abn.emitters.OutputEmitter;
import net.ripe.db.whois.common.rpsl.RpslObject;

public class OutputWriter {

	OutputEmitter outputEmitter; //= defaultEmitter; //TODO:
	List<RpslObject> rpslObjects;
	
	public OutputWriter(List<RpslObject> rpslObjects) {
		// TODO Auto-generated constructor stub
		this.rpslObjects = rpslObjects;
	}
	
	public OutputWriter(List<RpslObject> rpslObjects, OutputEmitter emitter) {
		// TODO Auto-generated constructor stub
		this.rpslObjects = rpslObjects;
		this.outputEmitter = emitter;
	}
	
	public String toString() {
		return null;
		//TODO:
	}
	
	public void writeToFile(String path) { //TODO: See about the flag that was the second param here..
	//TODO:	
	}

}
