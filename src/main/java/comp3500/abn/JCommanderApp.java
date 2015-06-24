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

public class JCommanderApp {

	public final static String APP_NAME = "App";
	
	//CLI args
	@Parameter (names = {"-e", "--emitter"}, description = "Emitter to use to format output")
	protected String emitterName = null;
	
	@DynamicParameter (names = {"-m"}, description = "Emitter parameters")
	protected HashMap<String, String> emitterArguments = new HashMap<String, String>();
	
	@Parameter (names = {"-i", "--input"}, description = "[Input path (omit for stdin)]")
	protected String inputPath = null;
	
	@Parameter (names = {"-o", "--output"}, description = "[Output path (omit for stdout)]")
	protected String outputPath = null;
	
	//explanatory/help commands
	@Parameter (names = {"-h", "--help"}, help = true, description = "Dispaly usage information")
	protected boolean helpMode = false;
	
	@Parameter (names = {"--list-emitters"}, help = true, description = "List available emitters to format output with")
	protected boolean help_displayEmitters = false;
	
	
	protected OutputEmitter emitter;
	protected RpslObjectStreamReader reader;
	protected OutputWriter writer;
	
	public final int SETUP_EXIT_USAGE = 1;
	public final int SETUP_EXIT_EMITTER_LIST = 2;
	public final int SETUP_EXIT_READY = 0;
	public final int SETUP_EXIT_FAIL = -1;
	
	
	protected static String usageString;
	static {
		StringBuilder builder = new StringBuilder();
		new JCommander(new JCommanderApp()).usage(builder);
		usageString = builder.toString();
	}
	
	
	protected boolean setup(String args[]) throws ParameterException { //changed to bool to enable easier testing.. yes, I know..

		JCommander cliArgParser = new JCommander(this, args); //parse params
		

		//StringBuilder helpString = new StringBuilder();
		if(helpMode) {
			cliArgParser.setProgramName(APP_NAME);
			cliArgParser.usage(); //print usage info //TODO: work out a way to stop the default values of the help commands showing up :P

//			cliArgParser.usage(helpString);
//			helpString.toString();
		}
		
		if(help_displayEmitters) {
			System.out.println("Available emitters: " + StringUtils.join(OutputEmitters.values(), ", "));
		}
		
		//if either of the above triggered, halt.
		if(helpMode || help_displayEmitters)
			return false; //formerly System.exit(0);.. then testability happened
		
		//not in help mode, continue normal run
		
		
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
		
		reader = (inputPath != null) ?
				(new RpslObjectFileReader(inputPath)) :
				(new RpslObjectStreamReader(System.in));

		writer = new OutputWriter(emitter); //TODO: organise how to make this more extensible with relation to more elaborate output methods; eg ssh, restconf, etc. Not just file or stdout.
		
		return true; //setup succeeded and no help flags were passed. run() should be called next.. to continue this great work ;)
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
		//String arguments[] = {"-h"};
		//instantiate yourself here instead.. probably a better idea.. I feel like I'm looking at an instance nesting problem here.. o_O :P
		JCommanderApp launcher = new JCommanderApp();
		if(launcher.setup(args)) //if setup passes, and if we're required to do more than just display of usage info or emitter listing..
			launcher.run();
	}
}
