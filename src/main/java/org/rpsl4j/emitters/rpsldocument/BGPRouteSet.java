package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import net.ripe.db.whois.common.domain.CIString;

public class BGPRouteSet extends BGPRpslSet {

	//members, mbrsByRef.. etc. //TODO:
	
	protected Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes) {
		if (visitedNodes.contains(this)) //ensure we're not retracing our footsteps
			return new HashSet<BGPRoute>();
		visitedNodes.add(this); //add this set to the index
		
		HashSet<BGPRoute> flattenedRoutes = new HashSet<BGPRoute>();
		
		for(BGPSetMember m : members) {
			if(m.type==BGPSetMember.ROUTE) {
				flattenedRoutes.add(m.getValue_Route());
				continue;
			}
			String postfix;
			if(m.type==BGPSetMember.SET) {
				//flatten set, then decide if it's contents need their postfixes updating
				Set<BGPRoute> nestedSetRoutes = m.referencedSet.resolve(parentRpslDocument, visitedNodes);
				
				if(m.getReferencedSetPostFix() == null) { //no postfix to apply
					flattenedRoutes.addAll(nestedSetRoutes);
					continue;
				}
				
				//there is a postfix we need to apply
				postfix = m.getReferencedSetPostFix().toString();
				HashSet<BGPRoute> postfixedMembers = new HashSet<BGPRoute>();
				for(BGPRoute r : nestedSetRoutes) {
					BGPRoute newroute = r.clone();
					newroute.appendPostfix(postfix);
					postfixedMembers.add(newroute);
				}
				flattenedRoutes.addAll(postfixedMembers);
				continue;
			}
			if(m.type==BGPSetMember.AS) {
				//get the routes that the given AS originates, apply postfixes to them if necessary, and add them to the collection of flattened routes. //TODO: make sure this method of deriving AS routes is appropriate; is 
				Set<BGPRoute> asRoutes = parentRpslDocument.getASRoutes(m.getValue_AS().getValue());
				if(m.getReferencedSetPostFix()==null) { //can we add straight away?
					flattenedRoutes.addAll(asRoutes);
					continue;
				}
				
				//apply postfix
				HashSet<BGPRoute> postfixedASRoutes = new HashSet<BGPRoute>();
				postfix = m.getReferencedSetPostFix().toString(); 
				for(BGPRoute r : asRoutes) {
					BGPRoute newRoute = r.clone();
					r.appendPostfix(postfix);
					postfixedASRoutes.add(newRoute);
				}
				flattenedRoutes.addAll(postfixedASRoutes);
				continue;
			}
		}
		return flattenedRoutes;
	}
	
	protected BGPRouteSet(CIString name) {
		super(name);
	}

}
