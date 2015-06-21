package comp3500.abn;

import org.apache.commons.cli.Options;

public class CommonsTest {

	public void parse(String args[]) {
		Options options = new Options();
		
		options.addOption("help", "Display usage information");
		options.addOption("list-emitters", "List emitters available to format output with");
	}
	
	public static void main(String args[]) {
		
	}
}
