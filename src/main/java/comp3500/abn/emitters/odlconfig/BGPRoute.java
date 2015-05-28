/*
 * Copyright (c) 2015 Benjamin Roberts, Nathan Kelly, Andrew Maxwell
 * All rights reserved.
 */


package comp3500.abn.emitters.odlconfig;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslAttribute;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;

/**
 * BGPRoute represents a route exported by an aut-num to a potential peer.
 * The class also provides methods for parsing the routes found in RPSL export attributes,
 * however it does not support filter expressions as of yet (AND/OR/NOT etc).
 * @author Benjamin George Roberts
 */
public class BGPRoute {
	private static final List<String> OPERATORS = Arrays.asList("AND", "OR", "NOT");
	protected static final String ANY_ADDRESS = "0.0.0.0";
	
	AddressPrefixRange routePrefixObject;
	String 	nextHop,
			routeNetwork;
	int 	routePrefix;
	
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
	 * @return Set of routes which are to be exported
	 */
	static Set<BGPRoute> resolveRoutes(RpslAttribute exportAttr, String localRouter) {
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
				System.err.println("Filter expressions not currently supported");
				return new HashSet<BGPRoute>();
			}
			
			//Try and parse/add the route
			try {
				routeObjectSet.add(new BGPRoute(AddressPrefixRange.parse(routeString), localRouter));
			} catch (AttributeParseException e) {}
		}
		
		return routeObjectSet;
		
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
}
