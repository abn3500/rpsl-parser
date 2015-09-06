package org.rpsl4j.emitters.rpsldocument;

import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.rpsl.attrs.AutNum;

public class BGPSetMember {
	
	protected BGPRpslSet	referencedSet	= null;
	protected CIString		postfix			= null; //for referenced sets and optionally AS references in route-set contexts..? //TODO
	protected BGPRoute		value_Route		= null;
	protected AutNum		value_AS		= null;
	//TODO: make that less lousy..
	
	protected int type = -1;
	
	protected static final int ROUTE = 1;
	protected static final int AS = 2;
	protected static final int SET = 3;
	
	public BGPSetMember(BGPRpslSet referencedSet) {
		this.referencedSet = referencedSet;
		this.type = SET;
	}
	
	public BGPSetMember(BGPRpslSet referencedSet, CIString referencedSetPostfix) {
		this.referencedSet = referencedSet;
		this.postfix = referencedSetPostfix;
		this.type = SET;
	}
	
	public BGPSetMember(BGPRoute route) {
		this.value_Route = route;
		this.type = ROUTE;
	}
	
	public BGPSetMember(AutNum autNum) {
		this.value_AS = autNum;
		this.type = AS;
	}
	
	//for that weird case (is it even possible?) where an AS is referenced (so as to indirectly reference all the routes it originates), and a postfix is also used..
	public BGPSetMember(AutNum autNum, CIString postFix) {
		this.value_AS = autNum;
		this.postfix = postFix;
		this.type = AS;
	}
	
	public CIString getReferencedSetPostFix() {
		return postfix;
	}
	
	//TODO: Seriously need to make this bit less crap..
	public BGPRoute getValue_Route() {
		return value_Route;
	}
	
	public AutNum getValue_AS() {
		return value_AS;
	}
	
	public String toString() {
		String s = "";
		s+= (referencedSet!=null ? "referencedSet: " + referencedSet.name : "");
		s+= (value_Route!=null ? "route: " + value_Route : "");
		s+= (value_AS!=null ? "as: " + value_AS : "");
		s+= (postfix!=null ? " postfix: " + postfix : "");
		
		return s;
	}
	
}
