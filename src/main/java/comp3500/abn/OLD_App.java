/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */
package comp3500.abn;

import java.io.IOException; 
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

import comp3500.abn.emitters.OutputEmitter;
import comp3500.abn.emitters.OutputEmitters;
import net.ripe.db.whois.common.io.RpslObjectFileReader;
import net.ripe.db.whois.common.io.RpslObjectStreamReader;
import net.ripe.db.whois.common.rpsl.RpslObject;


public class OLD_App 
{
	private static final String USAGE_STRING  = "Usage: \n"
											+ "[-i/--input input path] [-o/--output output path] [-e/--emitter emitter] [[-t/--option key=value]] \n" 
											+ "--list-emitters\n "
											+ "-h/--help";
	String outputPath = null;
	RpslObjectStreamReader reader; //RpslObjectFileReader extends this class, so is also covered here
	OutputWriter writer;
	
	OutputEmitter emitter;
	
	/**
	 * Initialise an instance of the application.
	 * @param args command line arguments to parse
	 */
	public OLD_App(String args[]) {
		parseArguments(args);
	}
	
	/**
	 * Main parser application logic
	 */
	public void run() {    	
    	//parse input into Rpsl objects..
    	for(String stringObject : reader)
    	{
    		//parse can return null or throw exceptions
    		try {
        		RpslObject object = RpslObject.parse(stringObject);
    			if (object == null)
    				throw new NullPointerException("Object failed to parse");
        		
        		writer.addObject(object);
    		} catch (NullPointerException | IllegalArgumentException e) {
    			//Object failed to parse, print error with excerpt of object
    			String[] splitObject = stringObject.split("\n");
    			System.err.println("Unable to parse following object, skipping... ");
    			
    			//Print object excerpt
    			for(int i = 0; i < 3 && i < splitObject.length; i++) {
    				System.err.println(splitObject[i]);
    				if(i == 2) //We only printed part of the object
    					System.err.println("...");
    			}
    		}
    	}
    	    	
    	//Emit objects to stdout or file depending on outputPath
    	if(outputPath==null) {
    		System.out.println(writer.toString());
    	} else {
			try {
				writer.writeToFile(outputPath);
			} catch (IOException e) {
				System.err.println("Error writing to file");
				System.exit(-1);
			}
		}
	}

	/**
	 * Parses the command line flags and initialises the emitter, reader, writer and output path of the app.
	 * Duplicate arguments cause the application to exit
	 * @param args Array of command line arguments to parse
	 */
	private void parseArguments(String[] args) {
		String inputPath = null, emitterName = null;
		Map<String, String> emitterArguments = new HashMap<String, String>();
		int i = 0;

		//parse arguments, terminate when index is out of argument array bounds
		while (i < args.length) {
			switch(args[i]) {
				case "-i":
				case "--input":
					if(i + 1 >= args.length || inputPath != null)
						exitFlagError();
					else
						inputPath = args[i+1];
					i += 2;
					break;
				case "-o":
				case "--output":
					if(i + 1 >= args.length || outputPath != null)
						exitFlagError();
					else
						outputPath = args[i+1];
					i += 2;
					break;
				case "-e":
				case "--emitter":
					if(i+1 >= args.length || emitterName != null)
						exitFlagError();
					else
						emitterName = args[i+1];
					i += 2;
					break;
				case "-t":
				case "--option":
					if(i+1 >= args.length || !(args[i+1].split("=").length != 2)) {
						exitFlagError();
					} else {
						String[] splitArg = args[i+1].split("=");
						emitterArguments.put(splitArg[0], splitArg[1]);
					}
					i += 2;
					break;
				case "--list-emitters":
					exitListEmitters();
				case "-h":
				case "--help":
					exitUsage();
				default:
					exitFlagError();
			}
		}
		
		//Get the emitter and instantiate with arguments if present
		if(emitterArguments.size() > 0)
			emitter = (emitterName != null) ? 
					(OutputEmitters.get(emitterName, emitterArguments)) : 
					(OutputEmitters.defaultEmitter.get(emitterArguments));
		else
			emitter = (emitterName != null) ? 
					(OutputEmitters.get(emitterName)) :
					(OutputEmitters.defaultEmitter.get());
		
		//Initialise the correct reader
		if(inputPath != null)
			reader = new RpslObjectFileReader(inputPath);
		else
			reader = new RpslObjectStreamReader(System.in);	
		
		//Initialise a writer
		writer = new OutputWriter(emitter);
	}
    
    /**
     * Print an error message and terminate the application
     */
    private static void exitFlagError() {
		System.err.println( "Unrecognised or duplicate argument, or flag was an *even* numbered param.\n" + USAGE_STRING);
		System.exit(-1);
    }
    
    /**
     * List the emitters available to the user and exit the application
     */
    private static void exitListEmitters() {
		System.out.println("Available emitters: " + StringUtils.join(OutputEmitters.values(), ", "));
		System.exit(0);
    }
    
    /**
     * Print the usage string
     */
    private static void exitUsage() {
    	System.out.println(USAGE_STRING);
    	System.exit(0);
    }
    
    /**
     * Application entrypoint
     * @param args command line arguments
     */
	public static void main(String[] args) {
    	//instantiate yourself and jump out of this static context..
    	new OLD_App(args).run();
    }
}
