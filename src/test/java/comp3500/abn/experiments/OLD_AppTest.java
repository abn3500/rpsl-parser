package comp3500.abn.experiments;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.internal.runners.statements.Fail;

/**
 * Unit test for simple App.
 */
public class OLD_AppTest
{
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

    	BufferedWriter sampleWriter = new BufferedWriter(new FileWriter(inputPath.toFile()));
    	sampleWriter.write("aut-num:        AS1\n" +
			"as-name:        Example-IIX-AS\n" + 
			"descr:          Example AS acting as IIX\n" +
			"export:         to AS2 announce AS5\n" +
			"export:         to AS3 announce AS4\n" +
			"admin-c:        IIX-ADMIN\n" +
			"tech-c:         IIX-TECH\n" +
			"mnt-by:         IIX-MAINT-AS1\n" +
			"changed:        noc@iix.net 20150420\n" +
			"source:         TEST\n" +
			"\n" +
			"aut-num:        AS2\n" +
			"as-name:        Example-Speaker1\n" +
			"descr:          Example speaker for client1\n" +
			"import:         from AS1 accept ANY\n" +
			"export:         to AS4 announce AS5\n" +
			"admin-c:        IIX-ADMIN\n" +
			"tech-c:         IIX-TECH\n" +
			"mnt-by:         IIX-MAINT-AS2\n" +
			"changed:        noc@iix.net 20150420\n" +
			"source:         TEST\n" +
			"\n" +
			"aut-num:        AS3" +
			"as-name:        Example-Speaker2\n" +
			"descr:          Example speaker for client2\n" +
			"import:         from AS1 accept ANY\n" +
			"export:         to AS5 announce AS4\n" +
			"admin-c:        IIX-ADMIN\n" +
			"tech-c:         IIX-TECH\n" +
			"mnt-by:         IIX-MAINT-AS3\n" +
			"changed:        noc@iix.net 20150420\n" +
			"source:         TEST\n"
			);
    	sampleWriter.close();
    	
    	sampleArgsXML = "-o " + outputPath.toString() + " -i " + inputPath.toString() + " -e " + "XMLEmitter";
    	
    	runTime = Runtime.getRuntime();

	}
    

    /**
     * Checks that the application exits successfully when given valid arguments 
     * @throws IOException 
     */
	@Test
    public void argParseTest() throws IOException {
		setup();
		
    	Process p = runTime.exec(EXEC_COMMAND + sampleArgsXML);
    	System.out.println("INFO: running argParseTest()");
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
		System.out.println("INFO: running badArgParseTest()");
		int eCode = processWait(p);
		if(eCode==0) { //if success return code given (for invalid input), fail
			//System.out.println("INFO: exit code: " + eCode);
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
	
	//broken out, only used for debug purposes.
	private void printWorkingDir() throws IOException
	{
    	Process pTest = runTime.exec("pwd"); //get us some clarity here.. nope.. how does one redirect stdout here..
    	BufferedReader childReader = new BufferedReader(new InputStreamReader(pTest.getInputStream()));
    	String line1;
        while((line1 = childReader.readLine()) != null)
        	System.out.println("Working directory: " + line1);
	}
}

/* Docs used
/http://www.avajava.com/tutorials/general-java/how-do-i-run-another-application-from-java/RuntimeExecTest1.java
/http://stackoverflow.com/questions/4741878/redirect-runtime-getruntime-exec-output-with-system-setout
*/
