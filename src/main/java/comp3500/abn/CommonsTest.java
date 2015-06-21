package comp3500.abn;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class CommonsTest {

	
	public static final String HELP_TEXT = "Usage info...";
	
	private boolean helpMode = false;
	private boolean help_listEmitters = false;
	
	
	
	public void parse(String args[]) {
		Options options = new Options();
		
		options.addOption("help", "Display usage information");
		options.addOption("listEmitters", "List emitters available to format output with");
		
		CommandLineParser parser = new DefaultParser();
		
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("Parse exception: " + e.getMessage());
		}
		
		if(cmd == null) {
			System.err.println("CLI parser failed to initialise");
			System.exit(-1);
		}
		
		if(cmd.hasOption("help")) {
			System.out.println(HELP_TEXT);
		}
		
	}
	
	public static void main(String args[]) {
		final String testArgs[] = {"-help"};
		
		CommonsTest commonsTest = new CommonsTest();
		commonsTest.parse(testArgs);
	}
}
