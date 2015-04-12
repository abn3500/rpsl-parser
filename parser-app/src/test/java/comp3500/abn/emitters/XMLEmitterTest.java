/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.ripe.db.whois.common.io.RpslObjectStringReader;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.RpslObjectBuilder;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XMLEmitterTest {
	private static String XML_HEADER_STRING = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n";
	private static String EXAMPLE_RPSL_STRING = "aut-num:  AS7574\n" + 
			"as-name:  AARNET-NT-RNO\n" + 
			"descr:    Australian Academic and Reasearch Network (AARNet)\n" + 
			"descr:    Northern Territory regional network organisation\n" + 
			"country:  AU\n" + 
			"import:   from AS7474\n" + 
			"          action pref=100;\n" + 
			"          accept ANY\n" + 
			"import:   from AS7569\n" + 
			"          action pref=100;\n" + 
			"          accept AS7569 AS4776 AS7570 AS7572 AS4738 AS7571 AS7574 AS7573 AS1221 AS7474\n" + 
			"import:   from AS4746\n" + 
			"          action pref=100;\n" + 
			"          accept AS7569 AS4776 AS7570 AS7572 AS4738 AS7571 AS7574 AS7573 AS1221 AS7474 AS4775\n" + 
			"import:   from AS7570\n" + 
			"          action pref=100;\n" + 
			"          accept AS7569 AS4776 AS7570 AS7572 AS4738 AS7571 AS7574 AS7573 AS1221 AS7474\n" + 
			"import:   from AS7572\n" + 
			"          action pref=100;\n" + 
			"          accept AS7569 AS4776 AS7570 AS7572 AS4738 AS7571 AS7574 AS7573 AS1221 AS7474\n" + 
			"import:   from AS4738\n" + 
			"          action pref=100;\n" + 
			"          accept AS7569 AS4776 AS7570 AS7572 AS4738 AS7571 AS7574 AS7573 AS1221 AS7474 AS4806 AS4739 AS4807\n" + 
			"import:   from AS7571\n" + 
			"          action pref=100;\n" + 
			"          accept AS7569 AS4776 AS7570 AS7572 AS4738 AS7571 AS7574 AS7573 AS1221 AS7474 AS4806 AS4739 AS4807\n" + 
			"import:   from AS7573\n" + 
			"          action pref=100;\n" + 
			"          accept AS7573\n" + 
			"export:   to AS7474\n" + 
			"          announce AS7574\n" + 
			"export:   to AS7569\n" + 
			"          announce AS7574\n" + 
			"export:   to AS4746\n" + 
			"          announce AS7574\n" + 
			"export:   to AS7570\n" + 
			"          announce AS7574\n" + 
			"export:   to AS7572\n" + 
			"          announce AS7574\n" + 
			"export:   to AS4738\n" + 
			"          announce AS7574\n" + 
			"export:   to AS7571\n" + 
			"          announce AS7574\n" + 
			"export:   to AS7573\n" + 
			"          announce AS7574\n" + 
			"default:  to AS7474\n" + 
			"          action pref=100;\n" + 
			"          networks ANY\n" + 
			"default:  to AS1221\n" + 
			"          action pref=50;\n" + 
			"          networks ANY\n" + 
			"admin-c:  GM2-AP\n" + 
			"tech-c:   AC10-AP\n" + 
			"mnt-by:   MAINT-AARNET-AP\n" + 
			"remarks:  hostmaster@apnic.net 970528\n" + 
			"source:   APNIC\n" + 
			"changed:  hm-changed@apnic.net 20111109\n";
	@Test
	public void generatesXMLFromRPSL() {
		//TODO Codify correct output
		OutputEmitter xmlEmitter = new XMLEmitter();
		Set<RpslObject> objects = parseRPSL(EXAMPLE_RPSL_STRING);
		String xmlString = xmlEmitter.emit(objects);
		System.out.println(xmlString);
		assertNotEquals("Should generate XML", "", xmlString);
		fail("correct oitput not implemented");
	}
	
	@Test
	public void generatesXMLString() {
		Document root = newDocument();
		Element element = root.createElement("test");
		element.setTextContent("test");
		element.setAttribute("test", "test");
		root.appendChild(element);
		assertEquals("", 
				XML_HEADER_STRING + "<test test=\"test\">test</test>\n", 
				XMLEmitter.generateString(root));
	}
	
	/**
	 * Helper method to generate clean document instances.
	 * @return New instance of Document
	 */
	private static Document newDocument() {
		try {
			return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		} catch (ParserConfigurationException e) {
			fail("Failed to create a new Document instance");
			return null; //UNREACHABLE
		}
	}
	
	/**
	 * Generates a set of {@link RpslObject}'s from a string
	 * @param rpslString String to parse into objects
	 * @return Set of {@link RpslObject}'s
	 */
	private static Set<RpslObject> parseRPSL(String rpslString) {
		Set<RpslObject> objectSet = new HashSet<RpslObject>();
		RpslObjectStringReader reader = new RpslObjectStringReader(rpslString);
		
		for(String objectString : reader)
			objectSet.add((new RpslObjectBuilder(objectString)).get());
		
		return objectSet;
	}
}
