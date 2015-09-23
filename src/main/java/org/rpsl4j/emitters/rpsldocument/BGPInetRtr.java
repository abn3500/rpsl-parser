/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */

package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

/**
 * Representation of an RPSL inet-rtr object and an ODL BGP Speaker.
 * Construct the peers of the inet-rtr and provide them the routes of the speakers AutNum
 * @author Benjamin George Roberts
 */
public class BGPInetRtr {
	private RpslObject inetRtr;
	protected BGPAutNum autNumObject;
	protected String speakerAddress, speakerName, peerRegistry;
	protected long speakerAutNum;
	protected Set<BGPPeer> peers = new HashSet<BGPPeer>();
	
	/**
	 * Instantiate a new BGPInetRtr (BGP Speaker) using the provided inet-rtr rpsl object.
	 * Also instantiates any peers of the instance and uses them the {@link BGPAutNum} to
	 * construct their route tables.
	 * @param object RPSL inet-rtr object of the speaker
	 * @param speakerAddress address of the speaker (should be ifaddr attribtue of the object)
	 * @param autNumObject AutNum the speaker is a member of
	 */
	public BGPInetRtr(RpslObject object, String speakerAddress, BGPAutNum autNumObject) {
		if(object.getType() != ObjectType.INET_RTR) throw new IllegalArgumentException("Requires INET_RTR object, got " + object.getType());

		//Populate fields
		this.inetRtr = object;
		this.speakerName = inetRtr.getTypeAttribute().getCleanValue().toString(); //DNS name
		this.speakerAutNum = autNumObject.autNum;
		this.speakerAddress = speakerAddress;
		this.autNumObject = autNumObject;
		this.peerRegistry = this.toString() + "-registry";
		
		//Add peers of rtr
		for(RpslAttribute peerAttribute : inetRtr.findAttributes(AttributeType.PEER))
			addPeer(peerAttribute);
		
	}
	
	/**
	 * Create and add a peer to the BGPInetRtr instance.
	 * Doesn't support peer options currently
	 * @param peerAttribute attribute of the inet-rtr declaring the peer to be added
	 */
	private void addPeer(RpslAttribute peerAttribute) {
		//TODO should handle peer attributes such as asno and port. Update: asno now handled. Port isn't specified in rpsl, so isn't applicable. Flap_damp() should perhaps be considered
		//Parse the attribute
		List<Pair<String, List<String>>> peerAttrAst = peerAttribute.getTokenList();
		
		long peerAS = -1;
		String  peerAddress = null;
		//Find the peer IP address, should be in the form ("dns", "BGP4", "1.2.3.4")
		for(Pair<String, List<String>> entry : peerAttrAst) {
			if(entry.getLeft().equals("dns") && entry.getRight().size() > 1) {
				peerAddress		 = entry.getRight().get(1);
				peerAS = autNumObject.getASOfPeer(peerAddress);
				break;
			}
		}
		
		//if an asno is provided explicitly in the peer attribute, extract it and use that
		for(Pair<String, List<String>> entry : peerAttrAst) {
			if(entry.getLeft().equals("asno") && entry.getRight().size() > 0) {
				try {
					AutNum asNo = AutNum.parse(entry.getRight().get(0));
					peerAS = asNo.getValue();
					break;
				} catch(AttributeParseException e) {
					//TODO: Log issue or throw exception
				}
			}
		}
		
		//Add new peer
		if(peerAddress != null && peerAS != -1) 
			peers.add(new BGPPeer(peerAS, peerAddress, this));
		//TODO: else log issue
	}

	/**
	 * Instantiate a set of BGPInetRtr speakers declared in an RPSL inet-rtr object.
	 * One instance will be created for each if-addr attribtue of unique address.
	 * @param object inet-rtr instance of {@link RpslObject}
	 * @param autNumObject the aut-num the speakers are members of
	 * @return Set of BGPInetRtr speakers
	 */
	public static Set<BGPInetRtr> getSpeakerInstances(RpslObject object, BGPAutNum autNumObject) {		
		//Check object type
		if(object.getType() != ObjectType.INET_RTR) throw new IllegalArgumentException("Requires INET_RTR object, got " + object.getType());
		
		Set<BGPInetRtr> speakerSet = new HashSet<BGPInetRtr>();
		
		for(RpslAttribute ifAddrAttr : object.findAttributes(AttributeType.IFADDR)) {
			//Parse the ifaddr attribute
			List<Pair<String, List<String>>> attrAst = ifAddrAttr.getTokenList();
			
			//Find the IP of the ifaddr
			for(Pair<String, List<String>> entry : attrAst) {
				//TODO ignores masklen, shouldn't matter though
				if(entry.getLeft().equals("dns") && entry.getRight().size() > 0) {
					speakerSet.add(new BGPInetRtr(object, entry.getRight().get(0), autNumObject));
					break; //Should only be one address per attr.
				}
			}
		}
		return speakerSet;
	}
	
	/**
	 * Get a copy of the objects peer set
	 * @return Set of objects {@link BGPPeer}s
	 */
	public Set<BGPPeer> getPeers() {
		return new HashSet<BGPPeer>(peers);
	}

	@Override
	public String toString() {
		return String.format("%s(%s)", autNumObject.name, speakerAddress);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof BGPInetRtr))
			return false;
		
		BGPInetRtr other = (BGPInetRtr) o;
		return this.speakerAddress.equals(other.speakerAddress) && this.speakerAutNum == other.speakerAutNum;
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

	/**
	 * Get the AutNum the inet-rtr is a member of
	 * @return Speaker's AutNum
	 */
	public BGPAutNum getAutNumObject() {
		return autNumObject;
	}

	/**
	 * Get the listening address of the inet-rtr/speaker
	 * @return address
	 */
	public String getSpeakerAddress() {
		return speakerAddress;
	}

	/**
	 * Get the configured name of the inet-rtr/speaker
	 * @return name
	 */
	public String getSpeakerName() {
		return speakerName;
	}

	/**
	 * Get the peer registry the inet-rtr/speaker will accept (OpenDaylight specific)
	 * @return peer-registry name
	 */
	public String getPeerRegistry() {
		return peerRegistry;
	}

	/**
	 * Get the Autnonomous System number of the inet-rtr
	 * @return aut-system number
	 */
	public long getSpeakerAutNum() {
		return speakerAutNum;
	}
	
	
}
