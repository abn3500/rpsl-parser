/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import static org.junit.Assert.*;

import java.text.AttributedCharacterIterator.Attribute;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class XMLEmitterTest {
	private static String XML_HEADER_STRING = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>";
	
	@Test
	public void generatesXMLString() {
		Document root = newDocument();
		Element element = root.createElement("test");
		element.setTextContent("test");
		element.setAttribute("test", "test");
		root.appendChild(element);
		System.out.println(XMLEmitter.generateString(root));
		assertEquals("", 
				XML_HEADER_STRING + "<test test=\"test\">test</test>", 
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
}
