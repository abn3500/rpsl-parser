/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */


package org.rpsl4j.emitters.rpsldocument;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;
import net.ripe.db.whois.common.rpsl.attrs.RangeOperation;

/**
 * BGPRoute represents a route exported by an aut-num to a potential peer.
 * The class also provides methods for parsing the routes found in RPSL export attributes,
 * however it does not support filter expressions as of yet (AND/OR/NOT etc).
 * @author Benjamin George Roberts
 */
public class BGPRoute implements Cloneable {
	private static final List<String> OPERATORS = Arrays.asList("AND", "OR", "NOT");
	protected static final String ANY_ADDRESS = "0.0.0.0";
	final static Logger log = LoggerFactory.getLogger(BGPRoute.class);
	
	AddressPrefixRange routePrefixObject;
	String 	nextHop,
			routeNetwork;
	int 	routePrefix;
	private Map<String, String> actions = new HashMap<String, String>();

	public BGPRoute(AddressPrefixRange routePrefixObject, String nextHop) {
		this.routePrefixObject = routePrefixObject;
		this.nextHop = nextHop;
		this.routeNetwork = routePrefixObject.getIpInterval().beginAsInetAddress().getHostAddress();
		this.routePrefix = routePrefixObject.getIpInterval().getPrefixLength(); //TODO: should this consider a detached range - ie 1.1.1.1/8^16-24 ? Also, the implementation in Ipv4Resource looks unlikely to reliably return a usable result
	}

	/**
	 * Return the set of routes the provided attribute declares as exportable.
	 * The method currently does not handle filter expressions
	 * @param exportAttr export attribute of an aut-num RPSL object
	 * @param localRouter the address of the router the route is available at
	 * @param doc the {@link BGPRpslDocument} to lookup route/as sets from. Null if not used.
	 * @return Set of routes which are to be exported
	 */
	static Set<BGPRoute> resolveRoutes(RpslAttribute exportAttr, String localRouter, BGPRpslDocument doc) {
		//Sanity check on parameter
		if(exportAttr.getType() != AttributeType.EXPORT) throw new IllegalArgumentException("Requires EXPORT attribute, got " + exportAttr.getType());
		
		//Create a set to store the created routes in, parse the attribute
		Set<BGPRoute> routeObjectSet = new HashSet<BGPRoute>();
		List<Pair<String, List<String>>> attrAST = exportAttr.getTokenList();
		
		//Find the list of route strings
		List<String> routeList = null;
		for(Pair<String, List<String>> pair : attrAST) {
			if(pair.getLeft().equals("announce")) {
				routeList = pair.getRight();
				break;
			}
		}
		
		//TODO We naively assume that only IP address prefixes are permitted. This section would need to be rewritten to add support for them
		//Parse routes and build routeset
		for(String routeString : routeList) {
			//If it is an operator, return the empty set as we can't handle it.
			if(OPERATORS.contains(routeString)) {
				log.error("Filter expressions not currently supported");
				return new HashSet<BGPRoute>();
			}
			
			// try parse route string as AS, then as route prefix
			if (doc != null && routeString.matches("AS\\d{1,5}")) {
				
				// route string is an AS, parse it and add to dock
				try {
					AutNum autNum = AutNum.parse(routeString);
					for(BGPRoute r : doc.getASRoutes(autNum.getValue())) {
						// r is a copy, okay to mutate
						r.nextHop = localRouter;
						routeObjectSet.add(r);
					}	
				} catch (AttributeParseException e) {}
			} else {	
				//Try and parse/add the routeprefix
				try {
					routeObjectSet.add(new BGPRoute(AddressPrefixRange.parse(routeString), localRouter));
				} catch (AttributeParseException e) {}
			}
		}
		
		return routeObjectSet;
		
	}
	
	static Set<BGPRoute> resolveRoutes(RpslAttribute exportAttr, String localRouter, BGPRpslDocument doc, Map<String, String> actionMap) {
		Set<BGPRoute> routes = resolveRoutes(exportAttr, localRouter, doc);
		for(BGPRoute r : routes)
			r.setActions(actionMap);
		return routes;
	}
	
	/**
	 * @see{BGPRoute#resolveRoutes}
	 */
	static Set<BGPRoute> resolveRoutes(RpslAttribute exportAttr, String localRouter) {
		return resolveRoutes(exportAttr, localRouter, null);
	}
	
	@Override
	public BGPRoute clone() {
		BGPRoute clone = null;
		try { clone = (BGPRoute) super.clone(); } catch (CloneNotSupportedException e) {/*UNREACHABLE*/}
		clone.setActions(actions);
		return clone;
	}
	
	
	/**
	 * Clones this object and applies the given postfix
	 * If a postfix already exists, it is combined with the new one as per the spec: https://tools.ietf.org/html/rfc2622#page-6
	 * @param prefixOperator rpsl route prefix operator
	 */
	public void appendPostfix(String prefixOperator) {
		String addrStr = routePrefixObject.toString().trim();
		
		if(!addrStr.contains("^")) //no existing prefix
			addrStr+=prefixOperator;
		else {
			//a range applied to a set with existing ranges in it has the effect of bounding the address by the new range, with the exception that the lower bound can only move up.
			//if we had {innerAddress^i1-i2}^o1-o2, we would get innerAddress^max(i1, 01)-o2 - if 02 isn't greater or equal to max(i1, o1), the postfix is removed completely.. erm, that latter bit seems more iffy - but then, the address mask is still there, we're just removing the ability to specify additional addresses at all.. so I guess the result of closing it off too much win contradictory prefixes is that it has no viable prefix left.. right

			//extract existing
			//String origPostfix = addrStr.substring(addrStr.lastIndexOf('^'));
			
			RangeOperation ro = routePrefixObject.getRangeOperation();
			int previousN = ro.getN(); //format is ^n-m (counter intuitively)
			int previousM = ro.getM();
			
			RangeOperation ro2 = RangeOperation.parse(prefixOperator, 0, 32); //TODO: ensure getPrefixLength() is reliable enough to use here. Maxing at 32bit for ipv4
			int newN = ro2.getN();
			int newM = ro2.getM();
			
			int maxN = Math.max(previousN, newN);
			
			addrStr = addrStr.substring(0, addrStr.lastIndexOf('^')); //strip existing prefix
			if (!(maxN <= newM)) { //prefix is invalidated by addition. Return clean address
				log.debug("stripped prefix. New address string: " + addrStr);
			}
			else {
				//prefix is now ^maxN-newM
				addrStr+= "^"+maxN+"-"+newM;
			}
		}
		
		routePrefixObject = AddressPrefixRange.parse(addrStr);
		routeNetwork = routePrefixObject.getIpInterval().beginAsInetAddress().getHostAddress();
		routePrefix = routePrefixObject.getIpInterval().getPrefixLength();
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof BGPRoute)) {
			return false;
		} else {
			BGPRoute otherRoute = (BGPRoute) other;
			
			//handle case where nextHop is null 
			if((nextHop == null || otherRoute.nextHop == null) && nextHop != otherRoute.nextHop)
				return false;
			else if ((nextHop != null && otherRoute.nextHop != null) && !nextHop.equals(otherRoute.nextHop))
				return false;
			
			return routePrefix == otherRoute.routePrefix && routeNetwork.equals(otherRoute.routeNetwork);
		}
	}
	
	@Override
	public String toString() {
		return String.format("%s/%d via %s", routeNetwork, routePrefix, nextHop);
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}
	
	/**
	 * Set the actions of the BGP route
	 * @param actionMap map of actions to add
	 */
	public void setActions(Map<String, String> actionMap) {
		actions.clear();
		actions.putAll(actionMap);
	}
	
	/**
	 * Returns a map of the BGP routes actions
	 * @return map of action name to value
	 */
	public Map<String, String> getActions() {
		Map<String, String> ret = new HashMap<String, String>();
		ret.putAll(actions);
		return ret;
	}
	
	/**
	 * Get the next hop of the route
	 * @return next hop address for route
	 */
	public String getNextHopString() {
		return nextHop;
	}
	
	/**
	 * Get the network address of the route
	 * @return network of route
	 */
	public String getRouteNetworkString() {
		return routeNetwork;
	}
	
	/**
	 * Get the prefix length of the route
	 * @return length of route prefix
	 */
	public int getRoutePrefix() {
		return routePrefix;
	}
	
	/**
	 * Get the prefix range object of the route
	 * @return prefix range object
	 */
	public AddressPrefixRange getPrefixRange() {
		return routePrefixObject;
	}
}
