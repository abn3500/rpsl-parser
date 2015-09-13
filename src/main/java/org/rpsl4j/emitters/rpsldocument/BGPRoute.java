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
			
			// try parse route string as AS, then as a routeset/as-set, then finally as a route prefix
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
			} else if(routeString.startsWith("as-")) {
				if(doc.asSets.containsKey(routeString)) {
					for(BGPRoute r : doc.asSets.get(routeString).resolve(doc)) {
						r.nextHop = localRouter;
						routeObjectSet.add(r);
					}
				}
			} else if(routeString.startsWith("rs-")) {
				if(doc.routeSets.containsKey(routeString)) {
					for(BGPRoute r : doc.routeSets.get(routeString).resolve(doc)) {
						r.nextHop = localRouter;
						routeObjectSet.add(r);
					}
				}
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
	 * Applies the given postfix to this object, combining it with any existing prefix.
	 * As per: https://tools.ietf.org/html/rfc2622#page-6 //TODO: Note that this is an updated version of the spec. It was chosen because it had greater clarity
	 * @param newPrefix rpsl route prefix operator
	 */
	public void appendPostfix(String newPrefix) {
		String addrStr = routePrefixObject.toString().trim(); //extract current route string including prefix if present
		
		Pair<String, String> existingRouteAndPrefix = BGPRpslSet.splitPrefix(addrStr);
		String existingRoute = existingRouteAndPrefix.getLeft();
		String existingPrefix = existingRouteAndPrefix.getRight();
		
		String newPrefixedRoute;
		
		if(existingPrefix==null) //no prefix
			newPrefixedRoute = existingRoute + newPrefix; //apply new prefix directly
		else {
			int currentN = routePrefixObject.getRangeOperation().getN(); 

			//alternate logic for + and -
			//for +/-, we don't consider an upper bound; so everything becomes x-32, given we're in IPV4
			if(newPrefix.equals("^-")) { //exclusive more specifics operator
				if((currentN+1)>32) //if an exclusive more specifics version of this prefix would result in 33-32, we clearly have no addresses left, and an invalid prefix. Drop it. Not positive what the spec says about this, but it presumably falls under the m-n where m>n case; drop the address completely.
					throw new IllegalArgumentException("prefix ^" + currentN+1  +"-32 would be created by applying ^- to route: " + addrStr);
				
				//if we'd have {1.1.1.1/8^+}^- --> 1.1.1.1/8^9-32, change that for 1.1.1.1/8^-
				if(currentN==routePrefixObject.getIpInterval().getPrefixLength()) //avoid m-n if previous prefix was ^+
					newPrefixedRoute = existingRoute + "^-";
				else
					newPrefixedRoute = existingRoute + "^"+ (currentN+1) + "-32";  //we want currentN+1 - 32
			}
			else if (newPrefix.equals("^+")) { //inclusive more specifics operator
				
				//if existing is ^- or equivalent, don't do anything; adding a ^+ to a ^- results in a ^-
				if(currentN==routePrefixObject.getIpInterval().getPrefixLength()+1) //if currently ^(maskLength+1)-x, we effectively have ^-. Adding a ^+ won't change it. So stop now
					newPrefixedRoute = existingRoute + "^-"; //adding ^+ to a ^- equivalent has no effect. - But if we get 1.1.1.1/8^9-32, we can still neaten it up
				else if(currentN==routePrefixObject.getIpInterval().getPrefixLength()) //if n is the mask length (ie x.x.x.x/8^8-x), we want to say ^+ rather than ^maskLength-32, as they have equivalent meaning.
						newPrefixedRoute = existingRoute + "^+";
				else
					newPrefixedRoute = existingRoute + "^"+ currentN + "-32";
			}
			else {
				//if numeric
				//calculate new prefix - format is ^n-m (counter intuitively)
				
				
				RangeOperation newPrefix_RangeOperation = RangeOperation.parse(newPrefix, 0, 32); //TODO Maxing at 32bit for ipv4
				int newN = newPrefix_RangeOperation.getN();
				int newM = newPrefix_RangeOperation.getM();
				
				int maxN = Math.max(currentN, newN);
				
				if (!(maxN <= newM)) //New prefix is going backwards (eg 1.1.1.1/8^16-15). As per spec, strip prefix altogether. - Edit: actually by prefix, they meant the whole address. Delete the whole address. Now it makes sense.
					throw new IllegalArgumentException("Cannot set m of an address prefix to a value smaller than the larger of the two n values - see the spec."); //newPrefixedRoute = existingRoute;
				else {
					if(maxN==newM) // if range format of n-n, just use n
						newPrefixedRoute = existingRoute + "^"+maxN;
					else
						newPrefixedRoute = existingRoute + "^"+maxN+"-"+newM;
				}
			}
		}

		//System.out.println("DEBUG: " + newPrefixedRoute);
		//reconstruct route prefix object with new prefix
		routePrefixObject = AddressPrefixRange.parse(newPrefixedRoute);
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
