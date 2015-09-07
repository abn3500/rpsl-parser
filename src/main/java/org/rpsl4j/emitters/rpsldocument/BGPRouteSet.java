package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import clover.com.google.common.collect.Sets;
import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AddressPrefixRange;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;

public class BGPRouteSet extends BGPRpslSet {
	
	//TODO add slf4j logger
	
	public BGPRouteSet(RpslObject obj) {
		super(obj);
	}	

	//members, mbrsByRef.. etc. //TODO:
	@Override
	protected Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes) {
		HashSet<BGPRoute> flattenedRoutes = new HashSet<BGPRoute>();

		flattenedRoutes.addAll(super.resolve(parentRpslDocument, visitedNodes));
		
		if (visitedNodes.contains(this)) //ensure we're not retracing our footsteps
			return flattenedRoutes;
		visitedNodes.add(this); //add this set to the index
		
		
		for(CIString member : members) {
			
			//Try it as a prefix
			try {
				AddressPrefixRange prefix = AddressPrefixRange.parse(member);
				flattenedRoutes.add(new BGPRoute(prefix, null));
				continue;
			} catch(AttributeParseException e) {}
			
			//Try member as a route set
			if(member.startsWith("rs-")) {
				BGPRpslSet memberSetObject = parentRpslDocument.routeSets.get(member);
				Set<BGPRoute> resolvedRoutes = memberSetObject.resolve(parentRpslDocument, visitedNodes);
				//TODO Apply prefix operation to these routes without clone
				flattenedRoutes.addAll(resolvedRoutes);
			}
			
		}				
		
		//Resolve mbrs-by-ref routes
		if(mbrsByRef.size() == 1 && mbrsByRef.contains("ANY")) {
			//Take all routes that are member-of this set
			flattenedRoutes.addAll(parentRpslDocument.getSetMemberRoutes(name));
		} else if(mbrsByRef.size() > 0) {
			//Take intersection of routes that are member-of this set, and routes mnt-by a mbr-by-ref maintainer
			Set<BGPRoute> 	setMembers 		= parentRpslDocument.getSetMemberRoutes(name),
							byRefMembers 	= new HashSet<>();
			
			for(CIString maintainer : mbrsByRef)
				byRefMembers.addAll(parentRpslDocument.getMntByRoutes(maintainer));
			
			flattenedRoutes.addAll(Sets.intersection(setMembers, byRefMembers));
		}
		
		return flattenedRoutes;

//			if(m.type==BGPSetMember.ROUTE) {
//				flattenedRoutes.add(m.getValue_Route());
//				continue;
//			}
//			String postfix;
//			if(m.type==BGPSetMember.SET) {
//				//flatten set, then decide if it's contents need their postfixes updating
//				//that set might be an as-set..!!
//				if(m.referencedSet.name.startsWith("as-"))
//					System.err.println("Pulling routes via as-sets not yet supported");
//				Set<BGPRoute> nestedSetRoutes = m.referencedSet.resolve(parentRpslDocument, visitedNodes);
//				
//				if(m.getReferencedSetPostFix() == null) { //no postfix to apply
//					flattenedRoutes.addAll(nestedSetRoutes);
//					continue;
//				}
//				
//				//there is a postfix we need to apply
//				postfix = m.getReferencedSetPostFix().toString();
//				HashSet<BGPRoute> postfixedMembers = new HashSet<BGPRoute>();
//				for(BGPRoute r : nestedSetRoutes) {
//					BGPRoute newroute = r.clone();
//					newroute.appendPostfix(postfix);
//					postfixedMembers.add(newroute);
//				}
//				flattenedRoutes.addAll(postfixedMembers);
//				continue;
//			}
//			if(m.type==BGPSetMember.AS) {
//				//get the routes that the given AS originates, apply postfixes to them if necessary, and add them to the collection of flattened routes. //TODO: make sure this method of deriving AS routes is appropriate; is 
//				Set<BGPRoute> asRoutes = parentRpslDocument.getASRoutes(m.getValue_AS().getValue());
//				if(m.getReferencedSetPostFix()==null) { //can we add straight away?
//					flattenedRoutes.addAll(asRoutes);
//					continue;
//				}
//				
//				//apply postfix
//				HashSet<BGPRoute> postfixedASRoutes = new HashSet<BGPRoute>();
//				postfix = m.getReferencedSetPostFix().toString(); 
//				for(BGPRoute r : asRoutes) {
//					BGPRoute newRoute = r.clone();
//					r.appendPostfix(postfix);
//					postfixedASRoutes.add(newRoute);
//				}
//				flattenedRoutes.addAll(postfixedASRoutes);
//				continue;
//			}
//		}
	}
}
