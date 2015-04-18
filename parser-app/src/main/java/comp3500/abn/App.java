/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */
package comp3500.abn;

import java.io.IOException;
import java.util.Set;

import comp3500.abn.emitters.OutputEmitter;
import comp3500.abn.emitters.OutputEmitters;
import net.ripe.db.whois.common.io.RpslObjectFileReader;
import net.ripe.db.whois.common.io.RpslObjectStreamReader;
import net.ripe.db.whois.common.rpsl.RpslObject;


public class App 
{
	String outputPath=null, inputPath=null;
	RpslObjectStreamReader reader; //RpslObjectFileReader extends this, so is also covered here
	OutputWriter writer = null;
	
	OutputEmitter emitter;
	Set<RpslObject> RpslObjects;
	
	boolean debug = true;
	
	private void dprint(String msg)
	{
		if(debug)
			System.out.println("DEBUG: " + msg);
	}
	
	public void launch(String[] args) //non-static start point
	{
		dprint("started");
		//parse input flags..
    	for(int i=0; i<args.length; i+=2)
    	{
    		if( args[i].equals("-i") || args[i].equals("--input") )
    		{
    			if(i+1 >= args.length) //if the index where we expect the input path, is out of bounds..
    				exitFlagError();

    			inputPath = args[i+1]; //set input
    		}
    		else if( args[i].equals("-o") || args[i].equals("--output") )
    		{
    			if(i+1 >= args.length)
    				exitFlagError();
    			
    			outputPath = args[i+1];
    		}
    		else if( args[i].equals("-e") || args[i].equals("--emitter") )
    		{
    			if(i+1 >= args.length)
    				exitFlagError();
    			
    			emitter = OutputEmitters.get(args[i+1]); //determine custom emitter type..
    		}
    		else //unrecognised arg
    			exitFlagError(); //print usage info and error, then exit(-1)
    	}
    	//flags parsed, start reading input
    	dprint("Flags parsed");
    	
    	if(inputPath==null) //if no input path specified, try to read from stdin
    		reader = new RpslObjectStreamReader(System.in);
    	else
    		reader = new RpslObjectFileReader(inputPath);
    	
    	dprint("reader set up");
    	//parse input into Rpsl objects..
    	for(String stringObject : reader)
    	{
    		//RpslObjects.add( (new RpslObjectBuilder(stringObject).get()) );
    		dprint("Calling parse..");
    		RpslObjects.add(RpslObject.parse(stringObject));
    	}
    	dprint("input parsed");
//    	//attempt to close input
//    	try
//    	{
//			reader.close();
//		} catch (IOException e) {e.printStackTrace();}
    	
    	//instantiate outputWriter, which will call the emitter on demand and take the resultant string, which will then be sent to stdout or a file (below):
    	writer = new OutputWriter(RpslObjects,emitter);
    	
    	dprint("Writer instantiated");
    	
    	if(outputPath==null)
    		System.out.println(writer.toString());
		else
		{
			try {
				writer.writeToFile(outputPath);
			} catch (IOException e) {
				System.err.println("Error writing to file");
				exitFlagError();
			}
		}
    	dprint("Done");
	} //end launch()
	
	
//    private int parseEmitter(String string)
//    {
//    	//determine index/id of emitter name passed in
//		for(int i=0; i<emitters.length; i++)
//		{
//			if(emitters[i].equals(string))
//				return i;
//		}
//		throw new IllegalArgumentException("Unknown emitter type");
//	}

	
    //optional: can be used to check a string for known arguments, so that exceptions can be raised early if a flag is given where a path is expected
//    private static boolean findDupeArg(String arg) {
//    	//TODO won't always return true for valid flags?
//    	return (arg.equals("-e") || arg.equals("--emitter") || arg.equals("-i") || arg.equals("--input") || arg.equals("-o") || arg.equals("--output"));
//    }
    
    /**
     * Print an error message and terminate the application
     */
    private static void exitFlagError() {
		System.err.println( "Unrecognised argument or flag was an even numbered param.\n" + 
				 			"Usage: [-i input path] [-o output path] [-e emitter]");
		System.exit(-1);
    }
    
	public static void main(String[] args) {
    	//instantiate yourself and jump out of this static context..
    	new App().launch(args);
    }
}
