/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import java.io.StringWriter;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import net.ripe.db.whois.common.rpsl.RpslObject;

/**
 *  Implementation of {@link OutputEmitter} which converts the entire RPSL Document to an XML representation.
 * @author Benjamin George Roberts
 */
public class XMLEmitter implements OutputEmitter {

	private DocumentBuilder docBuilder;
	
	public XMLEmitter() {
		//Get an xml parser instance
		try {
			docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			System.err.println( "Failed to initialise DocumentBuilder, " + 
								"Emitter will output nothing.");
			e.printStackTrace();
		}
	}
	
	@Override
	public String emit(Set<RpslObject> objects) {
		//Ensure we have a document builder
		if(docBuilder == null)
			return "";
		
		Document rootDocument = docBuilder.newDocument();
		
		for(RpslObject rpslObject : objects)
			emitObject(rootDocument, rootDocument, rpslObject);
		
		return generateString(rootDocument);
	}
	
	/**
	 * Adds an XML representation of the {@link RpslObject} to the parent {@link Node} 
	 * @param rootDocument Document the object is added to
	 * @param parent Node the object isto be the child of
	 * @param object The object to be converted to XML and added
	 */
	private void emitObject(Document rootDocument, Node parent, RpslObject object) {
		//TODO
	}

	/**
	 * Transforms the Document to an XML format String
	 * @param rootDocument Document to output
	 * @return Document as an XML format String
	 */
	public static String generateString(Document rootDocument) {
		//Try initialise a transformer
		Transformer transformer;
		 try {
			transformer = TransformerFactory.newInstance().newTransformer();
		 } catch (TransformerConfigurationException
				  | TransformerFactoryConfigurationError e) {
			System.err.println( "Failed to initialise Transformer, " + 
								"Emitter will output nothing.");
			e.printStackTrace();
			return "";	
		}
		
		
		//Intialise the transformation source/destinations
		DOMSource source = new DOMSource(rootDocument);
		StringWriter stringWriter = new StringWriter();
		StreamResult destination = new StreamResult(stringWriter);
		
		//Run the transformation and return the buffered string result
		try {
			transformer.transform(source, destination);
			return stringWriter.toString();
		} catch (TransformerException e) {
			System.err.println( "Failed to transform Document to String, " +
								"Emitter will output nothing");
			e.printStackTrace();
			return "";
		}

	}

}
