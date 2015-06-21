package comp3500.abn;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;

import net.ripe.db.whois.common.io.RpslObjectFileReader;
import net.ripe.db.whois.common.io.RpslObjectStreamReader;
import net.ripe.db.whois.common.rpsl.RpslObject;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import comp3500.abn.emitters.OutputEmitter;
import comp3500.abn.emitters.OutputEmitters;

public class JCommanderTest {

	public final static String APP_NAME = "App";
	
	//CLI args
	@Parameter (names = {"-e", "--emitter"}, description = "Emitter to use to format output")
	private String emitterName = null;
	
	@DynamicParameter (names = {"-m"}, description = "Emitter parameters")
	private HashMap<String, String> emitterArguments = new HashMap<String, String>();
	
	@Parameter (names = {"-i", "--input"}, description = "[Input path (omit for stdin)]")
	private String inputPath = null;
	
	@Parameter (names = {"-o", "--output"}, description = "[Output path (omit for stdout)]")
	private String outputPath = null;
	
	//explanatory/help commands
	@Parameter (names = {"-h", "--help"}, help = true, description = "Dispaly usage information")
	private boolean helpMode = false;
	
	@Parameter (names = {"--list-emitters"}, help = true, description = "List available emitters to format output with")
	private boolean help_displayEmitters = false;
	
	
	OutputEmitter emitter;
	RpslObjectStreamReader reader;
	OutputWriter writer;
	
	
	private void setup(String args[]) {
		
		//this feels weird.. instantiating yourself within a private, non-static method.. :/
		//AppLauncher launcher = new AppLauncher(); //instantiate parameter bucket
		//ok, that's better.. :P
		
		JCommander cliArgParser = null;
		try {
			cliArgParser = new JCommander(this, args); //parse params
		} catch (ParameterException e) {
			System.err.println("Error parsing parameter" + e.getMessage());
		}
		
		if(cliArgParser==null) {
			throw new RuntimeException("Cli argument parser failed to initialise");
		}
		
		
		//app logic core
		
		
		//handle help flags..
		
		//slight hax to work around JCommander formatting :P
		
		if(helpMode) {
			cliArgParser.setProgramName(APP_NAME);
			cliArgParser.usage(); //print usage info //TODO: work out a way to stop the default values of the help commands showing up :P 
		}
		
		if(help_displayEmitters) {
			System.out.println("Available emitters: " + StringUtils.join(OutputEmitters.values(), ", "));
		}
		
		//if either of the above triggered, halt.
		if(helpMode || help_displayEmitters)
			System.exit(0);
		
		
		//normal run
		
		//get emitter, initialise with arguments if they exist
		if(emitterArguments.size() > 0) {
			emitter = (emitterName != null) ? 
					(OutputEmitters.get(emitterName, emitterArguments)) :
					(OutputEmitters.defaultEmitter.get(emitterArguments));
		} else {
			emitter = (emitterName != null) ? 
					(OutputEmitters.get(emitterName)) :
					(OutputEmitters.defaultEmitter.get());
		}
		
		//initialise input
		reader = (inputPath != null) ?
				(new RpslObjectFileReader(inputPath)) :
				(new RpslObjectStreamReader(System.in));

		writer = new OutputWriter(emitter); //TODO: organise how to make this more extensible with relation to more elaborate output methods; eg ssh, restconf, etc. Not just file or stdout.
	}
	
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
	
	public static void main(String args[]) {
		//String arguments[] = {"-i", "inputRPSL", "-o", "outputPath", "-e", "odlConfig", "-m", "a=1", "-m", "b=2"}; //dummy params
		
		//String arguments[] = {"--list-emitters"};
		String arguments[] = {"-h"};
		//instantiate yourself here instead.. probably a better idea.. I feel like I'm looking at an instance nesting problem here.. o_O :P
		JCommanderTest launcher = new JCommanderTest();
		launcher.setup(arguments);
		launcher.run();
	}
}
