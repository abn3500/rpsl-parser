package comp3500.abn.emitters;

import java.io.StringWriter;
import java.util.Set;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import net.ripe.db.whois.common.rpsl.RpslObject;

public class ODLConfigEmitter implements OutputEmitter {
	private static final String TEMPLATE_RESOURCE = "mustache/ODLConfigEmitter.mustache"; 
	
	/*
	 * BGP Config file constants. These are configured by passing emitter arguments.
	 */
	//speaker/peer configuration
	public String  BGP_PEER_REGISTRY = "peer-registry",
					BGP_DISPATCHER = "global-bgp-dispatcher"; 
	// BGP connection attributes 
	public int  BGP_RECONNECT_SLEEP_MIN = 1000,
				 BGP_RECONNECT_SLEEP_MAX = 180000,
				 BGP_RECONNCET_CONNECT_TIME = 5000;
	public double BGP_RECONNCET_SLEEP_FACTOR = 2.0;
	public String BGP_RECONNECT_STRATEGY_NAME = "reconnect-strategy-factory",
			       BGP_EXECUTOR_NAME = "global-event-executor";
	
	@Override
	public String emit(Set<RpslObject> rpslObjects) {
		Mustache templateRenderer = (new DefaultMustacheFactory()).compile(TEMPLATE_RESOURCE);
		StringWriter templateWriter = new StringWriter();
		
		//Render template and flush stream
		templateRenderer.execute(templateWriter, this);
		templateWriter.flush();
		
		return templateWriter.toString();
	}
	
	@Override
	public void setArguments(Set<String> arguments) {
		//Assume arguments are x=y format		
		//Split the arguments by the '=' character and assign to instance variables
		for(String argument : arguments) {
			String[] splitArguments = argument.split("=");
			
			//Make sure there is only a key and a value
			if(splitArguments.length != 2) {
				System.err.println("Invalid argument: " + argument);
			} else {
				//Switch on the argument name and set the matching variable
				try {
					switch(splitArguments[0]) {
						case "BGP_PEER_REGISTRY": BGP_PEER_REGISTRY = splitArguments[1]; break;
						case "BGP_DISPATCHER": BGP_DISPATCHER = splitArguments[1]; break;
						case "BGP_RECONNECT_SLEEP_MIN": BGP_RECONNECT_SLEEP_MIN = Integer.parseInt(splitArguments[1]); break;
						case "BGP_RECONNECT_SLEEP_MAX": BGP_RECONNECT_SLEEP_MAX = Integer.parseInt(splitArguments[1]); break;
						case "BGP_RECONNCET_CONNECT_TIME": BGP_RECONNCET_CONNECT_TIME = Integer.parseInt(splitArguments[1]); break;
						case "BGP_RECONNCET_SLEEP_FACTOR": BGP_RECONNCET_SLEEP_FACTOR = Double.parseDouble(splitArguments[1]); break;
						case "BGP_EXECUTOR_NAME": BGP_EXECUTOR_NAME = splitArguments[1]; break;
						case "BGP_RECONNECT_STRATEGY_NAME": BGP_RECONNECT_STRATEGY_NAME = splitArguments[1]; break;
						default:
							System.err.println("Unknown argument: " + argument);
							break;
					}
				} catch(NumberFormatException e) {
					//We failed to parse an integer or double
					System.err.println("Failed to parse value: " + argument);
				}
			}
		}
	}
}
