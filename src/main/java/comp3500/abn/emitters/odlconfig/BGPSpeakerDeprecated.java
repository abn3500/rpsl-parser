/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package comp3500.abn.emitters.odlconfig;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.ripe.db.whois.common.rpsl.AttributeLexerWrapper;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

import org.apache.commons.lang3.tuple.Pair;


/**
 * Represents the speakers of the OpenDaylight configuration file.
 * @author Benjamin George Roberts
 */
public class BGPSpeakerDeprecated {
	//List of route attribtue types parsed
	private static final AttributeType[] ROUTE_TYPES = {AttributeType.IMPORT, AttributeType.EXPORT, AttributeType.DEFAULT};
	private static final String DEFAULT_SPEAKER_ADDRESS = "0.0.0.0";
	/*
	 * Template values
	 */
	String 	name,
			speakerAddress = DEFAULT_SPEAKER_ADDRESS, //By default, bi
			peerRegistry,
			asn;
	AutNum	serverASN = null;
	
	
	private RpslObject autnumObject = null;
	private Map<AutNum, BGPPeerDeprecated> bgpPeers = null;
	
	/**
	 * Create a new {@link BGPSpeakerDeprecated} object. Initialises the template fields.
	 * @param object an AUT_NUM {@link RpslObject}
	 */
	public BGPSpeakerDeprecated(RpslObject object) {
		//Sanity check the object type
		if(object.getType() != ObjectType.AUT_NUM)
			throw new IllegalArgumentException("Tried to instantiate BGPServer with " + object.getType().getName().toString());
		else
			autnumObject = object;
		
		//This parse method can throw an error. We can't really recover so we won't catch it
		asn = "AS" + AutNum.parse(autnumObject.getTypeAttribute().getCleanValue()).getValue();
		name = autnumObject.getValueForAttribute(AttributeType.AS_NAME).toString();
		peerRegistry = name + "-registry";
	}
	
	/**
	 * Using the {@link RpslObject} the speaker was initialised with, this method will create the required
	 * {@link BGPPeerDeprecated} instances. This is done by extracting the ASN's from each Import, Export or Default 
	 * attribute, creating or retrieving the {@link BGPPeerDeprecated} and passing the attribute (with routing information)
	 * to the {@link BGPPeerDeprecated}. If the table of {@link BGPPeerDeprecated}s has already been built, the method will return it.
	 * @return The BGPPeers associated with this speaker
	 */
	public Collection<BGPPeerDeprecated> getPeers() {	
		//TODO Doesn't take IP addresses into account
		//TODO Doesn't pass actions to the BGPPeers
		
		//short circuit if already run
		if(bgpPeers != null)
			return bgpPeers.values();
		else
			bgpPeers = new HashMap<AutNum, BGPPeerDeprecated>();
		
		//Run for each of the attribute types
		for(AttributeType attrType : ROUTE_TYPES) {
			AttributeLexerWrapper lexer = null;

			//Try get the lexer for this attribute type
			try { 
				lexer = new AttributeLexerWrapper(attrType.toString());
			} catch (ClassNotFoundException e) {
				System.err.println("Class not found for LexerWrapper while adding peers: " + attrType.toString());
				continue;
			}
			
			//Find attributes of current route type
			for(RpslAttribute attr : autnumObject.findAttributes(attrType)) {
				List<BGPPeerDeprecated> currentRoutePeers = new LinkedList<BGPPeerDeprecated>();
				List<Pair<String, List<String>>> parsedAttribute;
				
				//Parse the current attribute
				try {
					parsedAttribute = lexer.parse(new StringReader(attr.toString()));
				} catch (IOException e) {
					System.err.println("IO error parsing attributes: " + attr.toString());
					continue;
				}
				
				//Look for peer entries, these follow from (import) or to
				for(Pair<String, List<String>> tokenPair : parsedAttribute) {
					
					//Check for tokens containing the speaker address
					if(tokenPair.getLeft().equals("at")) {
						addSpeakerAddress(tokenPair.getRight());
						continue;
					}
					
					//Skip tokens which don't have peers in them
					if(!tokenPair.getLeft().equals((attrType == AttributeType.IMPORT) ? "from" : "to")
						|| tokenPair.getRight().size() < 1) 
						continue;
					
					//Add BGPPeer to the list we will add the route to. Create if non existant
					try {
						AutNum asn = AutNum.parse(tokenPair.getRight().get(0));
						
						if(!bgpPeers.containsKey(asn))
							bgpPeers.put(asn, new BGPPeerDeprecated(this, asn));
						currentRoutePeers.add(bgpPeers.get(asn));
						
					} catch (AttributeParseException e) {
						System.err.println("Failed to parse ASN: " + tokenPair.getRight().get(0));
						continue;
					}
				}
				
				//Add the route attribute to the peers
				for(BGPPeerDeprecated peer : currentRoutePeers)
					peer.addRoutes(parsedAttribute, attrType);
			}
		}
		
		//Return the list of peers
		return bgpPeers.values();
	}

	/**
	 * Attempts to set the address of the {@link BGPSpeaker}. Currently, the address can only 
	 * be changed once from DEFAULT_SPEAKER_ADDRESS; other changes are ignored and report warnings.
	 * @param addressList
	 */
	private void addSpeakerAddress(List<String> addressList) {
		//TODO doesnt handle multiple addresses for the same speaker
		if(addressList.size() < 1) {
			return;
		} else {
			if(addressList.size() > 1)
				System.err.println("Warning: Multiple speaker addresses in same attribute are not supported.");
			
			//Check if we are setting a 2nd, non default address
			if(!(addressList.get(0).equals(speakerAddress) || speakerAddress.equals(DEFAULT_SPEAKER_ADDRESS)))
				System.err.println("Warning: Speakers with multiple addresses are not supported");
			else
				speakerAddress = addressList.get(0);
			
		}
		
	}
}