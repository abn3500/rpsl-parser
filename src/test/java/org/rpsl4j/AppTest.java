/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.File;
import java.util.HashMap;

import net.ripe.db.whois.common.io.RpslObjectFileReader;

import org.junit.Before;
import org.junit.Test;
import org.rpsl4j.App;
import org.rpsl4j.emitters.NullEmitter;
import org.rpsl4j.emitters.odlconfig.ODLConfigEmitter;

import com.beust.jcommander.ParameterException;

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
	
	File inputPath, outputPath;
	App app;
	
	@Before
	public void setup() throws IOException {
    	inputPath = File.createTempFile("rpslSample", ".txt");
    	outputPath = File.createTempFile("parseOutput", ".xml");
    	
    	app = new App();

    	BufferedWriter sampleWriter = new BufferedWriter(new FileWriter(inputPath));
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
	}
	
	
	@Test
	public void testHelpParams() {
		boolean setupResult;
		
		setupResult = app.setup(arg_helpShort);
		assertTrue("setup() should return false if not ready to run - ie. failed or in this case, help mode", setupResult==false);
		assertTrue("Help mode should be enabled after parsing the -h flag", app.helpMode);
		assertTrue("Help mode shouldn't initialise other parameters", app.emitter==null && app.reader==null && app.writer==null);
		
		app = new App(); //reinitialise App again
		setupResult = app.setup(arg_helpLong);
		assertTrue("setup() should return false if not ready to run - ie. failed or in this case, help mode", setupResult==false);
		assertTrue("Help mode should be enabled after parsing the --help flag", app.helpMode);
		assertTrue("Help mode shouldn't initialise other parameters", app.emitter==null && app.reader==null && app.writer==null);
	}
	
	@Test (expected = ParameterException.class)
	public void testInputNoPathFail() {
		app.setup(arg_inputShort_noval);
		fail("Setup should fail if input flag is used with no value");
	}
	
	@Test
	public void testInitialisation() throws IOException {
		//prints help text to stdout unfortunately :/ But redirecting that is more trouble than it's worth in this context I think
		
		assertTrue(inputPath!=null);
		assertTrue(outputPath!=null);
		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "ODLCONFIG", "-m", "a=1", "-m", "b=2"};
		
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
	}

	//TODO: re-enable when xml output is working better
//	@Test
//	public void testXMLOutput() throws IOException {
//		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "XML"};
//		
//		app.setup(argSample);
//		
//		assertTrue(app.emitterName.equals("XML")); //check emitter name was extracted correctly from input
//		assertTrue(app.emitter instanceof XMLEmitter); //XML emitter should have been instantiated
//		
//		app.run();
//		
//		BufferedReader reader = new BufferedReader(new FileReader(outputPath));
//		assertTrue("XML emitting mode should create a non-empty file", reader.readLine() != null);
//	}
	
	@Test
	public void testODLConfigOutput() throws IOException {
		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "ODLCONFIG"};
		
		app.setup(argSample);
		app.run();
		
		BufferedReader generatedConfigReader = new BufferedReader(new FileReader(outputPath));
		//BufferedReader expectedConfigReader = new BufferedReader(new FileReader(outputPath.toFile()));
		//String generatedLine, expectedLine;
		
		assertTrue("At the very least, there should be data in the output file", generatedConfigReader.readLine() != null);
//		while((generatedLine=generatedConfigReader.readLine()) !=null) {
//			if((expectedLine = expectedConfigReader.readLine()) == null) //if we've exhausted the sample output before the generated output, we generated too many lines..
//				fail("Too many lines in output ODL config file");
//			assertTrue("Lines should match between sample output and generated output", expectedLine.equals(generatedLine));
//			//TODO: Ensure sample output here is valid!!!!
//			fail("Don't forget to make sure sample output is valid before signing off this code!!");
//		}
		generatedConfigReader.close();
	}

	@Test
	public void testNullOutputToFile() throws IOException {
		final String argSample[] = {"-i", inputPath.toString(), "-o", outputPath.toString(), "-e", "NULL"};
		
		app.setup(argSample);
		
		assertTrue(app.emitter instanceof NullEmitter);
		
		app.run();
		
		BufferedReader reader = new BufferedReader(new FileReader(outputPath));
		assertTrue(reader.readLine() == null); //file should be empty
		
		reader.close();
	}
	
	@Test
	public void testSetupNullOutputToStdOut() throws IOException { //tests that no output file is created when not asked for. DOESN'T check that no data is output to stdout.
		final String argSample[] = {"-i", inputPath.toString()};
		
		app.setup(argSample);
		
		assertTrue(app.emitterName == null); //no emitter param passed in
		assertTrue(app.emitter instanceof NullEmitter); //should default to NullEmitter
		assertTrue(app.outputPath == null); // no output path was specified
		assertTrue(app.inputPath.equals(inputPath.toString()));
	}
}
