/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author Benjamin George Roberts
 */
public class ODLReconnectStrategy {
	/*
	 * Template object fields
	 */
	int		BGP_RECONNECT_SLEEP_MIN 	= 1000,
			BGP_RECONNECT_SLEEP_MAX 	= 180000,
			BGP_RECONNCET_CONNECT_TIME 	= 5000;
	double 	BGP_RECONNCET_SLEEP_FACTOR 	= 2.0;
	
	/**
	 * Instantiate an {@link ODLReconnectStrategy} using the default arguments.
	 */
	public ODLReconnectStrategy() {}
	
	/**
	 * Instantiate an {@link ODLReconnectStrategy} using a map of arguments.
	 * @param arguments Map of values to update {@link ODLReconnectStrategy}'s fields
	 */
	public ODLReconnectStrategy(Map<String, String> arguments) {
		//Parse arguments for reconnect fields to update
		for(Entry<String, String> arg : arguments.entrySet()) {		
			try {
				switch(arg.getKey()) {
					case "BGP_RECONNECT_SLEEP_MIN": BGP_RECONNECT_SLEEP_MIN = Integer.parseInt(arg.getValue()); break;
					case "BGP_RECONNECT_SLEEP_MAX": BGP_RECONNECT_SLEEP_MAX = Integer.parseInt(arg.getValue()); break;
					case "BGP_RECONNCET_CONNECT_TIME": BGP_RECONNCET_CONNECT_TIME = Integer.parseInt(arg.getValue()); break;
					case "BGP_RECONNCET_SLEEP_FACTOR": BGP_RECONNCET_SLEEP_FACTOR = Double.parseDouble(arg.getValue()); break;
				}
			} catch(NumberFormatException e) {
				//We failed to parse an integer or double
				System.err.println("Failed to parse value: " + arg.getValue());
			}
		}
	}
}