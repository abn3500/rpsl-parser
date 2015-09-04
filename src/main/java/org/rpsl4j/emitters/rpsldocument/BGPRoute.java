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

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

/**
 * BGPRoute represents a route exported by an aut-num to a potential peer.
 * The class also provides methods for parsing the routes found in RPSL export attributes,
 * however it does not support filter expressions as of yet (AND/OR/NOT etc).
 * @author Benjamin George Roberts
 */
public class BGPRoute {
	private static final List<String> OPERATORS = Arrays.asList("AND", "OR", "NOT");
	protected static final String ANY_ADDRESS = "0.0.0.0";
	final static Logger log = LoggerFactory.getLogger(BGPRoute.class);
	
	AddressPrefixRange routePrefixObject;
	String 	nextHop,
			routeNetwork;
	int 	routePrefix;
	private Map<String, String> actions = new HashMap<String, String>();
	Set<CIString> parentSets = new HashSet<CIString>(); //route-sets this route (says) it's a member of (no double checking and mbrsByRef validation yet) //TODO
	long asNumber = -1; //TODO: Should this be here..

	public BGPRoute(AddressPrefixRange routePrefixObject, String nextHop) {
		this.routePrefixObject = routePrefixObject;
		this.nextHop = nextHop;
		this.routeNetwork = routePrefixObject.getIpInterval().beginAsInetAddress().getHostAddress();
		this.routePrefix = routePrefixObject.getIpInterval().getPrefixLength();
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

	
	/**
	 * @param object
	 * throws IllegalArgumentException if the RpslObject passed in isn't a Route object, AttributeParseException if parsing object attributes fails. //TODO: neaten this comment up
	 */
	BGPRoute(RpslObject object) {
		if(object.getType() != ObjectType.ROUTE) throw new IllegalArgumentException("Requires ROUTE object, got " + object.getType());

		this.routePrefixObject = AddressPrefixRange.parse(object.getValueForAttribute(AttributeType.ROUTE));
		this.asNumber = AutNum.parse(object.getValueForAttribute(AttributeType.ORIGIN)).getValue();
		this.parentSets.addAll(object.getValuesForAttribute(AttributeType.MEMBER_OF));

		//TODO: deal with withdrawn dates. Complicating the problem, AttributeType lists no WITHDRAWN constant.
		//CIString withdrawnDate = object.getValueOrNullForAttribute(AttributeType.)
	}
	
	/**
	 * Copy constructor for BGPRoute
	 * @param r route to copy
	 */
	BGPRoute(BGPRoute r) {
		this(r.routePrefixObject, r.nextHop);
		setActions(r.actions);
		asNumber = r.asNumber;
		parentSets.addAll(r.parentSets);
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof BGPRoute)) {
			return false;
		} else {
			BGPRoute otherRoute = (BGPRoute) other;
			return nextHop.equals(otherRoute.nextHop) && routePrefix == otherRoute.routePrefix && routeNetwork.equals(otherRoute.routeNetwork);
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
