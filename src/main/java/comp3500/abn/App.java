/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */
package comp3500.abn;

import java.util.List;

import net.ripe.db.whois.common.io.RpslObjectFileReader;
import net.ripe.db.whois.common.io.RpslObjectStreamReader;
import net.ripe.db.whois.common.rpsl.RpslObject;


public class App 
{
	@Deprecated
	String[] emitters = {"odlbgp", "dummy"}; //TODO: remove dummy values
	
	List<RpslObject> RpslObjects;
	
	String outputPath=null, inputPath=null;
	
	@Deprecated
	int ODL_BGP = 0;
	@Deprecated
	int emitterType = ODL_BGP; //default emitter type
	
	RpslObjectStreamReader reader; //RpslObjectFileReader extends this, so is also covered here
	
	OutputWriter writer = null;
	
	public void launch(String[] args) //non-static start point
	{
		//parse input flags..
    	for(int i=0; i<args.length; i+=2)
    	{
    		if( args[i].equals("-i") || args[i].equals("--input") )
    		{
    			if(i+1 >= args.length) //if the index where we expect the input path, is out of bounds..
    				exitFlagError();
//    			if(findDupeArg(args[i+1])) //if expected index of input path contains a flag instead, exit. ..is that actually illegal on most filesystems..? maybe not. It's a pain though, so I think this is for the best
//    				exitError();
    			inputPath = args[i+1]; //set input
    			//i++; //advance iterator to second half of pair, so that (after the loop's i++) the next loop iteration won't hit the input_path value
    		}
    		else if( args[i].equals("-o") || args[i].equals("--output") )
    		{
    			if(i+1 >= args.length)
    				exitFlagError();
//    			if(findDupeArg(args[i+1]))
//    				exitError();
    			outputPath = args[i+1];
    			//i++;
    		}
    		else if( args[i].equals("-e") || args[i].equals("--emitter") )
    		{
    			if(i+1 >= args.length)
    				exitFlagError();
    			emitterType = parseEmitter(args[i+1]); //determine custom emitter type..
    		}
    		else //unrecognised arg
    			exitFlagError(); //print usage info and error, then exit(-1)
    	}
    	//flags parsed, start reading input
    	
    	if(inputPath==null) //if no input path specified, try to read from stdin
    		reader = new RpslObjectStreamReader(System.in);
    	else
    		reader = new RpslObjectFileReader(inputPath);
    	
    	//parse input into Rpsl objects..
    	for(String stringObject : reader)
    	{
    		//RpslObjects.add( (new RpslObjectBuilder(stringObject).get()) );
    		RpslObjects.add(RpslObject.parse(stringObject));
    	}
    	
//    	//attempt to close input
//    	try
//    	{
//			reader.close();
//		} catch (IOException e) {e.printStackTrace();}
    	
    	//instantiate outputWriter, which will call the default emitter on demand, and take the resultant string, which will then be sent to stdout or a file (below):
    	if(emitterType==ODL_BGP)
    		writer = new OutputWriter(RpslObjects);
    	else
    	{
    		System.err.println("Not yet implemented");
    		throw new UnsupportedOperationException("Non odl bgp emitters not implemented yet");
    	}
    	
    	if(outputPath==null)
    		System.out.println(writer.toString());
    	else
    		writer.writeToFile(outputPath); //TODO: check the bool that was formerly the second param here..
	}
	
    private int parseEmitter(String string)
    {
    	//determine index/id of emitter name passed in
		for(int i=0; i<emitters.length; i++)
		{
			if(emitters[i].equals(string))
				return i;
		}
		throw new IllegalArgumentException("Unknown emitter type");
	}

    //optional: can be used to check a string for known arguments, so that exceptions can be raised early if a flag is given where a path is expected
    private static boolean findDupeArg(String arg) {
    	//TODO won't always return true for valid flags?
    	return (arg.equals("-e") || arg.equals("--emitter") || arg.equals("-i") || arg.equals("--input") || arg.equals("-o") || arg.equals("--output"));
    }
    
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
