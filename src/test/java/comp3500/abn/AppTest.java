package comp3500.abn;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest //extends TestCase
{
//http://www.avajava.com/tutorials/general-java/how-do-i-run-another-application-from-java/RuntimeExecTest1.java
	final int timeoutLongs = 3000; //ms to wait before checking app state/killing app
	final boolean passthroughOutput = false; //pipe through stdout and stderr from tested app
	
	Path tempDirPath, inputPath, outputPath;
	String sampleArgsXML;
	Runtime runTime;
	final String appLaunchCommandWWhitespace = "java -cp target/classes:../ripe-rpsl/target/classes comp3500.abn.App ";
	
	/**
	 * prepare for tests..
	 * @throws IOException 
	 */
	public void setup() throws IOException //TODO: look at how to get junit to run this automatically before each test..
	{
		tempDirPath = Files.createTempDirectory(null);
    	inputPath = Files.createTempFile(tempDirPath, "rpslSample", ".txt");
    	outputPath = Files.createTempFile(tempDirPath, "parseOutput", ".xml");

    	sampleArgsXML = "-o " + outputPath.toString() + " -i " + inputPath.toString() + " -e " + "XMLEmitter";
    	
//    	System.out.println("DEBUG: generated inputPath: " + inputPath);
    	
    	//launch app, sleep test controller thread, then wakeup, kill the process if it hasn't finished yet, and check the return code..
    	runTime = Runtime.getRuntime();
    	
//    	Process pTest = runTime.exec("pwd"); //get us some clarity here.. nope.. how does one redirect stdout here..
//    	BufferedReader childReader1 = new BufferedReader(new InputStreamReader(pTest.getInputStream()));
//    	String line1;
//        while((line1 = childReader1.readLine()) != null)
//        	System.out.println("Child says: " + line1);
	}
    
    /**
     * 
     * @throws IOException 
     */
	@Test
    public void argParseTest() throws IOException {
		setup();
		
    	//http://stackoverflow.com/questions/4741878/redirect-runtime-getruntime-exec-output-with-system-setout
    	System.out.println("INFO: spawning app instance.. will kill after 5 secs..");
    	Process p = runTime.exec("java -cp target/classes:../ripe-rpsl/target/classes comp3500.abn.App " + sampleArgsXML);
    	
    	BufferedReader childReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    	String line;    	
    	while((line = childReader.readLine()) != null)
    		System.out.println("Child says: " + line);
    	
    	try {
			Thread.sleep(timeoutLongs); //suspend the test runner..
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    	
    	try {
    		int exitCode = p.exitValue(); //p.waitFor(); //on suggestion of Ben's - Nicer way to let it run for a limited time. Only minor detail is whether we could speed things up if it *did* finish quicker..
    		//https://docs.oracle.com/javase/7/docs/api/java/lang/Process.html#exitValue%28%29
    		if(exitCode!=0)
    		{
    			System.out.println("INFO: App terminated with error status: " + exitCode);
    			fail("Application exited with error status on valid input: " + exitCode);
    		}
    		else
    			System.out.println("INFO: app terminated successfully");
    	} catch (IllegalThreadStateException e) {
    		System.out.println("INFO: App failed to terminate in time");
    		fail("App failed to terminate within 5 sec");
    	}
    }
	
	/**
	 * Test response to being passed invalid args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	@Test
	public void badArgParseTest() throws IOException, InterruptedException, IllegalThreadStateException {
		setup();
		
		String badargs = "-o -e XMLEmitter nonexistentfilepath";
		Process p = runTime.exec(appLaunchCommandWWhitespace + badargs);
		
		BufferedReader childReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
		BufferedReader childReaderError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    	String line = null;
    	String lineErr = null;
    	while(((line = childReader.readLine()) != null) || ((lineErr = childReaderError.readLine()) !=null))
    	{
    		if(line!=null && passthroughOutput)
    			System.out.println("Child_out: " + line);
    		if(lineErr!=null && passthroughOutput)
    			System.out.println("Child_err: " + lineErr);
    	}
		
		Thread.sleep(timeoutLongs); //if this throws an exception.. then frankly I dunno.. I'll think about that later :P //TODO:
		int eCode = p.exitValue();
		if(eCode==0) { //if success return given (for invalid input), fail
			System.out.println("INFO: exit code: " + eCode);
			fail("App should exit with an error code on invalid arguments");
		}
		//assertTrue("App should exit with error code -1 on invalid arguments", p.exitValue()==-1);
	}
}
