package nez.lang;

import java.util.Map;
import java.util.TreeMap;

import nez.util.UMap;

class ConditionalAnalysis extends Manipulator {
	Map<String, Boolean> undefedFlags;
	ConditionalAnalysis(Grammar g) {
		undefedFlags = new TreeMap<String, Boolean> ();
	}
	
	public Expression reshapeProduction(Production p) {
		p.setExpression(p.getExpression().reshape(this));
		return p;
	}
	
	public Expression reshapeOnFlag(OnFlag p) {
		String flagName = p.getFlagName();
		if(p.predicate) {
			if(undefedFlags.containsKey(flagName)) {
				undefedFlags.remove(flagName);
				Expression newe = p.get(0).reshape(this);
				undefedFlags.put(flagName, false);
				return newe;
			}
		}
		else {
			if(!undefedFlags.containsKey(flagName)) {
				undefedFlags.put(flagName, false);
				Expression newe = p.get(0).reshape(this);
				undefedFlags.remove(flagName);
				return newe;
			}
		}
		return p.get(0).reshape(this);
	}

	public Expression reshapeIfFlag(IfFlag p) {
		String flagName = p.getFlagName();
		if(p.predicate) {
			if(undefedFlags.containsKey(flagName)) {
				return fail(p);
			}
			return empty(p);
		}
		if(!undefedFlags.containsKey(flagName)) {
			return fail(p);
		}
		return empty(p);
	}

	public Expression reshapeNonTerminal(NonTerminal n) {
		Production r = elminateFlag(n.getProduction());
		if(r != n.getProduction()) {
			return Factory.newNonTerminal(n.s, r.getNameSpace(), r.getLocalName());
		}
		return n;
	}
	
	private Production elminateFlag(Production p) {
		if(undefedFlags.size() > 0) {
			StringBuilder sb = new StringBuilder();
			String localName = p.getLocalName();
			int loc = localName.indexOf('!');
			if(loc > 0) {
				sb.append(localName.substring(0, loc));
			}
			else {
				sb.append(localName);
			}
			for(String flagName: undefedFlags.keySet()) {
				if(hasReachableFlag(p.getExpression(), flagName)) {
					sb.append("!");
					sb.append(flagName);
				}
			}
			localName = sb.toString();
			Production newp = p.getNameSpace().getProduction(localName);
			if(newp == null) {
				newp = p.getNameSpace().newReducedProduction(localName, p, this);
			}
			return newp;
		}
		return p;
	}
	
	private static boolean hasReachableFlag(Expression e, String flagName) {
		return hasReachableFlag(e, flagName, new UMap<String>());
	}

	private static boolean hasReachableFlag(Expression e, String flagName, UMap<String> visited) {
		if(e instanceof OnFlag) {
			if(flagName.equals(((OnFlag) e).flagName)) {
				return false;
			}
		}
		for(Expression se : e) {
			if(hasReachableFlag(se, flagName, visited)) {
				return true;
			}
		}
		if(e instanceof IfFlag) {
			return flagName.equals(((IfFlag) e).flagName);
		}
		if(e instanceof NonTerminal) {
			NonTerminal ne = (NonTerminal)e;
			String un = ne.getUniqueName();
			if(!visited.hasKey(un)) {
				visited.put(un, un);
				Production r = ne.getProduction();
				return hasReachableFlag(r.body, flagName, visited);
			}
		}
		return false;
	}

}