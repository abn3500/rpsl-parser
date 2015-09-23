package org.rpsl4j.emitters.rpsldocument;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Sets;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.attrs.AttributeParseException;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

public class BGPAsSet extends BGPRpslSet {

	@Override
	Set<BGPRoute> resolve(BGPRpslDocument parentRpslDocument, Set<BGPRpslSet> visitedNodes) {
		HashSet<BGPRoute> flattenedRoutes = new HashSet<BGPRoute>();
		
		if (visitedNodes.contains(this)) //ensure we're not retracing our footsteps
			return flattenedRoutes;
		visitedNodes.add(this); //add this set to the index
		
		//Resolve AS and AS-Set objects in members list
		for(CIString member : members) {
			Pair<String, String> refMemberPair = splitPrefix(member.toString());
			String 	memberName = refMemberPair.getLeft(),
					prefix = refMemberPair.getRight();
			
			if(prefix != null) {
				log.warn("Encountered prefix operator'" + member + "' this is unsupported and the member will be skipped");
				continue;
			}
			
			//Test if member is a as-set
			if(memberName.startsWith("as-"))  {
				BGPRpslSet memberSetObject = parentRpslDocument.asSets.get(memberName);
				Set<BGPRoute> resolvedRoutes = memberSetObject.resolve(parentRpslDocument, visitedNodes);
				
				flattenedRoutes.addAll(resolvedRoutes);
			} else {
				//Try resolve it as an AS
				try {
					AutNum autNum = AutNum.parse(memberName);
					Set<BGPRoute> resolvedRoutes = parentRpslDocument.getASRoutes(autNum.getValue());

					flattenedRoutes.addAll(resolvedRoutes);
				} catch(AttributeParseException e) {}
			}
		}
		
		//Resolve mbrs-by-ref as's
		if(mbrsByRef.size() == 1 && mbrsByRef.contains(CIString.ciString("ANY"))) {
			//Take all as's that are member-of this set
			for(Long autNum : parentRpslDocument.getSetMemberAutNums(name))
				flattenedRoutes.addAll(parentRpslDocument.getASRoutes(autNum));
		} else if(mbrsByRef.size() > 0) {
			//Take intersection of as's that are member-of this set, and as's mnt-by a mbr-by-ref maintainer
			Set<Long> 	setMembers 		= parentRpslDocument.getSetMemberAutNums(name),
						byRefMembers 	= new HashSet<>();
			
			for(CIString maintainer : mbrsByRef)
				byRefMembers.addAll(parentRpslDocument.getMntByAutNums(maintainer));
			
			for(Long autNum : Sets.intersection(setMembers, byRefMembers))
				flattenedRoutes.addAll(parentRpslDocument.getASRoutes(autNum));
		}	
		
		return flattenedRoutes;
	}
	
	protected BGPAsSet(RpslObject rpslObject) {
		super(rpslObject);
	}

}
