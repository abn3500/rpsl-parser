/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang3.tuple.Pair;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import comp3500.abn.rpsl.AttributeLexerWrapper;
import difflib.StringUtills;
import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
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
	 * @param parent Node the object is to be the child of
	 * @param object The object to be converted to XML and added
	 */
	private void emitObject(Document rootDocument, Node parent, RpslObject object) {
		//TODO Determine if reference values are handled
		
		//Type attributes are the "RPSL Class", always mandatory && single-valued
		//The element of each object will be of the form <objectType value="objectValue">...</>
		Element objectElement = rootDocument.createElement(object.getType().getName());
		objectElement.setAttribute("value", object.getTypeAttribute().getCleanValue().toString());
		
		//Append each attribute as a child of objectElement
		Boolean firstAttr = true;
		for(RpslAttribute attr : object.getAttributes()) {
			//Skip the type attribute
			if(firstAttr) {
				firstAttr = false;
				continue;
			}
			
			//If the attribute contains a comment, prepend it
			if(attr.getCleanComment() != null) {
				Comment attrComment = rootDocument.createComment(attr.getCleanComment());
				objectElement.appendChild(attrComment);
			}
			

			//Append the attribute elements to a tag of its type
			Element attrElement = rootDocument.createElement(attr.getType().getName());
			try {
				//Try to use a Lexer if it exists. This results in more readable XML
				
				AttributeLexerWrapper lexer = new AttributeLexerWrapper(attr.getType().getName());
				StringReader attrReader = new StringReader(attr.getCleanValue().toString());
				
				for(Pair<String, List<String>> pair : lexer.parse(attrReader)) {
					//Our pairs look like ("keyword", "list of values"), add them ass <keyword>values...</>
					Element valueElement = rootDocument.createElement(pair.getLeft());
					valueElement.setTextContent(StringUtills.join(pair.getRight(), " "));
					attrElement.appendChild(valueElement);
				}
			} catch(ClassNotFoundException e) {
				//No lexer, add this attirbute as <type>value</type>. 
				System.err.println("No lexer for " + attr.getType().getName() + ", attribute will be added to object unparsed");
				Set<CIString> attrValues = attr.getCleanValues();
				
				//Add a tag for each attribute
				for(CIString attrValue : attrValues) {
					attrElement = rootDocument.createElement(attr.getType().getName());
					attrElement.setTextContent(attrValue.toString());
				}
			} catch (IOException e) {
				System.err.println("Error parsing" + attr.getType().getName() + ", attribute will not be added");
				e.printStackTrace();
				continue;
			}
			
			//Append the attribute element, regardless of whether it was lexed or manually input
			objectElement.appendChild(attrElement);
		}
		
		//Append the object any any comment on the type attribute to the parent node
		if(object.getTypeAttribute().getCleanComment() != null) {
			Comment objectComment = rootDocument.createComment(object.getTypeAttribute().getCleanComment());
			parent.appendChild(objectComment);
		}
		parent.appendChild(objectElement);
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
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
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
