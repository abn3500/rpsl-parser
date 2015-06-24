package comp3500.abn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import net.ripe.db.whois.common.io.RpslObjectFileReader;

import org.junit.Test;

import com.beust.jcommander.ParameterException;

import comp3500.abn.emitters.NullEmitter;
import comp3500.abn.emitters.ODLConfigEmitter;

public class AppTest {

	final String arg_helpShort[] = {"-h"};
	final String arg_helpLong[] = {"--help"};
	
	final String arg_inputShort_noval[] = {"-i"};
	final String arg_inputShort[] = {"-i", "testPath"};
	final String arg_inputLong[] = {"--input", "testPath"};
	
	final String arg_outputShort_noval[] = {"-o"};
	final String arg_outputShort[] = {"-o", "testPath"};
	final String arg_outputLong[] = {"--output", "testPath"};
	
	final String arg_emitterShort_noval[] = {"-e"};
	final String arg_emitterShort[] = {"-e", "ODL_Config"};
	final String arg_emitterLong[] = {"--emitter"};
	
	Path tempDirPath, inputPath, outputPath, expectedODLConfig;
	
	public void setup() throws IOException {
		tempDirPath = Files.createTempDirectory(null);
    	inputPath = Files.createTempFile(tempDirPath, "rpslSample", ".txt");
    	outputPath = Files.createTempFile(tempDirPath, "parseOutput", ".xml");
    	
    	expectedODLConfig = Files.createTempFile(tempDirPath, "expectedODLConfig", ".xml");

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
			"aut-num:        AS3\n" +
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
    	
    	BufferedWriter expectedODLWriter = new BufferedWriter(new FileWriter(expectedODLConfig.toFile()));
    	expectedODLWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    			"<!-- vi: set et smarttab sw2 tabstop=2: -->\n" + 
    			"<snapshot>\n" + 
    			"  <!-- This template is based on the example BGP speaker/peer configuration found at\n" + 
    			"  bgp/controller-config/src/main/resources/initial/41-bgp-example.xml of the BGPPCEP ODL module\n" + 
    			"\n" + 
    			"  It assumes the inclusion of the defaults defined in 31-bgp.xml of the same module -->\n" + 
    			"\n" + 
    			"  <!-- Merged capability list from 31-bgp.xml and 41-bgp-example.xml -->\n" + 
    			"  <required-capabilities>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:bgp:rib:cfg?module=odl-bgp-rib-cfg&amp;revision=2013-07-01</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:bgp:rib:spi?module=odl-bgp-rib-spi-cfg&amp;revision=2013-11-15</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:bgp:rib:impl?module=odl-bgp-rib-impl-cfg&amp;revision=2013-04-09</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:bgp:topology:provider?module=odl-bgp-topology-provider-cfg&amp;revision=2013-11-15</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:bgp:reachability:ipv6?module=odl-bgp-treachability-ipv6-cfg&amp;revision=2013-11-15</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:bgp:reachability:ipv4?module=odl-bgp-treachability-ipv4-cfg&amp;revision=2013-11-15</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:md:sal:binding?module=opendaylight-md-sal-binding&amp;revision=2013-10-28</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:netty?module=netty&amp;revision=2013-11-19</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:protocol:framework?module=protocol-framework&amp;revision=2014-03-13</capability>\n" + 
    			"    <capability>urn:opendaylight:params:xml:ns:yang:controller:topology?module=odl-topology-api-cfg&amp;revision=2013-11-15</capability>\n" + 
    			"  </required-capabilities>\n" + 
    			"\n" + 
    			"  <configuration>\n" + 
    			"    <data xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n" + 
    			"      <modules xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:config\">\n" + 
    			"\n" + 
    			"        <!-- Reconnect strategy configuration. -->\n" + 
    			"        <module>\n" + 
    			"          <type xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:protocol:framework\">prefix:timed-reconnect-strategy-factory</type>\n" + 
    			"          <name>reconnect-strategy-factory</name>\n" + 
    			"          <min-sleep>1000</min-sleep>\n" + 
    			"          <max-sleep>180000</max-sleep>\n" + 
    			"          <sleep-factor>2.0</sleep-factor>\n" + 
    			"          <connect-time>5000</connect-time>\n" + 
    			"          <timed-reconnect-executor>\n" + 
    			"            <type xmlns:netty=\"urn:opendaylight:params:xml:ns:yang:controller:netty\">netty:netty-event-executor</type>\n" + 
    			"            <name>global-event-executor</name> <!-- Defined in 31-bgp.xml -->\n" + 
    			"          </timed-reconnect-executor>\n" + 
    			"        </module>\n" + 
    			"\n" + 
    			"        <!--******************START OF SPEAKERS******************-->\n" + 
    			"        <!--*******************END OF SPEAKERS*******************-->\n" + 
    			"\n" + 
    			"      </modules>\n" + 
    			"      <services xmlns=\"urn:opendaylight:params:xml:ns:yang:controller:config\">\n" + 
    			"        <!-- Reconnect strategy configuration. -->\n" + 
    			"        <service>\n" + 
    			"          <type xmlns:prefix=\"urn:opendaylight:params:xml:ns:yang:controller:protocol:framework\">prefix:reconnect-strategy-factory</type>\n" + 
    			"          <instance>\n" + 
    			"            <name>reconnect-strategy-factory</name>\n" + 
    			"            <provider>/config/modules/module[name='timed-reconnect-strategy-factory']/instance[name='reconnect-strategy-factory']</provider>\n" + 
    			"          </instance>\n" + 
    			"        </service>\n" + 
    			"\n" + 
    			"        <service>\n" + 
    			"          <type xmlns:bgpspi=\"urn:opendaylight:params:xml:ns:yang:controller:bgp:rib:impl\">bgpspi:bgp-peer-registry</type>\n" + 
    			"          <!--******************START OF SPEAKERS******************-->\n" + 
    			"          <!--*******************END OF SPEAKERS*******************-->\n" + 
    			"        </service>\n" + 
    			"      </services>\n" + 
    			"    </data>\n" + 
    			"  </configuration>\n" + 
    			"</snapshot>");
    	expectedODLWriter.close();
	}
	
	
	@Test
	public void testHelpParams() {
		App app;
		boolean setupResult;
		
		app = new App();
		setupResult = app.setup(arg_helpShort);
		assertTrue("setup() should return false if not ready to run - ie, failed or help mode", setupResult==false);
		assertTrue("Help mode should be enabled after parsing the -h flag", app.helpMode);
		assertTrue("Help mode shouldn't initialise other parameters", app.emitter==null && app.reader==null && app.writer==null);
		
		app = new App();;
		setupResult = app.setup(arg_helpLong);
		assertTrue("setup() should return false if not ready to run - ie, failed or help mode", setupResult==false);
		assertTrue("Help mode should be enabled after parsing the --help flag", app.helpMode);
		assertTrue("Help mode shouldn't initialise other parameters", app.emitter==null && app.reader==null && app.writer==null);
	}
	
	@Test (expected = ParameterException.class)
	public void testInputNoPathFail() {
		App app = new App();
		app.setup(arg_inputShort_noval);
		fail("Setup should fail if input flag is used with no value");
	}
	
	@Test
	public void testInitialisation() throws IOException {
		
		//prints help text to stdout.. unfortunately :/ redirecting it into the equivalent of /dev/null.. not sure in java.. ok,
		//making a null output stream is easy enough, but replacing sysout after the fact.. hmm. I don't want to change the code
		//any more in the name of testability
		
		setup();
		assertTrue(inputPath!=null);
		assertTrue(outputPath!=null);
		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "ODLCONFIG", "-m", "a=1", "-m", "b=2"};
		
		App app = new App();
		app.setup(argSample);
		
		assertTrue(app.inputPath.equals(inputPath.toString()));
		assertTrue(app.outputPath.equals(outputPath.toString()));
		assertTrue(app.emitterName.equals("ODLCONFIG"));
		//System.out.println(app.emitter.getClass().getName());
		assertTrue(app.emitter instanceof ODLConfigEmitter);
		
		//check emitter parameters were correctly passed
		HashMap<String, String> emitterParams = new HashMap<String, String>();
		emitterParams.put("a", "1");
		emitterParams.put("b", "2");
		assertTrue(app.emitterArguments.equals(emitterParams));
		
		//we shouldn't be in a help mode
		assertFalse(app.helpMode);
		assertFalse(app.help_displayEmitters);
		
		//check that a file - rather than stdin - reader was set up
		assertTrue(app.reader instanceof RpslObjectFileReader);
		assertTrue(app.writer.outputEmitter instanceof ODLConfigEmitter);
		
		//System.out.println(app.writer.toString());
		
	}
	
//	@Test
//	public void testXMLOutput() throws IOException {
//		setup();
//		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "XML"};
//		
//		JCommanderApp app = new JCommanderApp();
//		app.setup(argSample);
//		app.run();
//		
//		BufferedReader reader = new BufferedReader(new FileReader(outputPath.toFile()));
//		String line;
//		while((line=reader.readLine()) !=null) {
//			System.out.println(line);
//		}
//		//TODO: Check output..
//	}
	
	@Test
	public void testODLConfigOutput() throws IOException {
		setup();
		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "ODLCONFIG"};
		
		App app = new App();
		app.setup(argSample);
		app.run();
		
		BufferedReader generatedConfigReader = new BufferedReader(new FileReader(outputPath.toFile()));
		BufferedReader expectedConfigReader = new BufferedReader(new FileReader(outputPath.toFile()));
		String generatedLine, expectedLine;
		while((generatedLine=generatedConfigReader.readLine()) !=null) {
			if((expectedLine = expectedConfigReader.readLine()) == null) //if we've exhausted the sample output before the generated output, we generated too many lines..
				fail("Too many lines in output ODL config file");
			assertTrue("Lines should match between sample output and generated output", expectedLine.equals(generatedLine));
			//TODO: Ensure sample output here is valid!!!!
			fail("Don't forget to make sure sample output is valid before signing off this code!!");
		}
	}

	@Test
	public void testNullOutputToFile() throws IOException {
		setup();
		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "NULL"};
		
		App app = new App();
		app.setup(argSample);
		app.run();
		
		BufferedReader reader = new BufferedReader(new FileReader(outputPath.toFile()));
		assertTrue(reader.readLine() == null); //file should be empty
	}
	
	@Test
	public void testSetupNullOutputToStdOut() throws IOException {
		setup();
		final String argSample[] = {"-i", inputPath.toString()};
		
		App app = new App();
		app.setup(argSample);
		
		assertTrue(app.emitterName == null); //no emitter param passed in
		assertTrue(app.emitter instanceof NullEmitter); //should default to NullEmitter
		assertTrue(app.outputPath == null); // no output path was specified
	}
}
