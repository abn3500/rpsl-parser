package comp3500.abn;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.internal.runners.statements.Fail;

/**
 * Unit test for simple App.
 */
public class AppTest //extends TestCase
{
//http://www.avajava.com/tutorials/general-java/how-do-i-run-another-application-from-java/RuntimeExecTest1.java
	private static final int TIMEOUT_STEP_MS = 300; //ms increment to wait while process is running
	private static final int TIMEOUT_MAX_MS = TIMEOUT_STEP_MS * 10; //max time MS to wait
	final boolean passthroughOutput = false; //pipe through stdout and stderr from tested app
	
	Path tempDirPath, inputPath, outputPath;
	String sampleArgsXML;
	Runtime runTime;
	private static final String EXEC_COMMAND = "java -cp " + System.getProperty("java.class.path") + " comp3500.abn.App ";
	
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
    	Process p = runTime.exec(EXEC_COMMAND + sampleArgsXML);
    	
    	int exitCode = processWait(p);
    	assertTrue("Application should exit with code 0", exitCode == 0);
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
		Process p = runTime.exec(EXEC_COMMAND + badargs);
		
		int eCode = processWait(p);
		if(eCode==0) { //if success return given (for invalid input), fail
			System.out.println("INFO: exit code: " + eCode);
			fail("App should exit with an error code on invalid arguments");
		}
		//assertTrue("App should exit with error code -1 on invalid arguments", p.exitValue()==-1);
	}
	
	/**
	 * Waits for the provided process to terminate or, if it fails to terminate within TIMEOUT_MAX_MS,
	 * forcefully terminates it and calls {@link Fail}. It will also print out the stdout/stderr of the process
	 * before returning its exit code
	 * @param p process to wait for
	 * @return exit code of process
	 */
	private int processWait(Process p) {
		int timeWaited = 0,
			retVal = -256;
		
		//Repeatedly try to get exit value and sleep on failure (not terminated yet)
		while(timeWaited < TIMEOUT_MAX_MS) {
			try {
				retVal = p.exitValue();
				break;
			} catch (IllegalThreadStateException e) {
				try{
					Thread.sleep(TIMEOUT_STEP_MS);
				} catch(InterruptedException ee) {
					//don't really care about thread interrupts at this point
				}
				timeWaited += TIMEOUT_STEP_MS;
			}
		}
		
		//Clear and print the stdout/stderr buffers
		BufferedReader stdout = new BufferedReader(new InputStreamReader(p.getInputStream())),
					   stderr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		String line;
		try{
			while((line = stdout.readLine()) != null)
				System.out.println("Child stdout: " + line);
		} catch(IOException e) {
			System.err.println("IOException while reading child stdout");
		}
		try {
			while((line = stderr.readLine()) != null)
				System.out.println("Child stdout: " + line);
		} catch(IOException e) {
			System.err.println("IOException while reading child stderr");
		}
		
		//If it didn't terminate, kill process and fail
		if(timeWaited >= TIMEOUT_MAX_MS) {
			p.destroy();
			fail("Child process did not terminate");
		}
		
		return retVal;
	}
}
