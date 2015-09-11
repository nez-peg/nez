package nez.lang;

import java.util.TreeMap;

import nez.lang.expr.NonTerminal;
import nez.lang.expr.Xif;
import nez.lang.expr.Xon;

public class ConditionalAnalysis extends GrammarTransducer {
	TreeMap<String, Boolean> condMap;

	public ConditionalAnalysis(TreeMap<String, Boolean> condMap) {
		this.condMap = condMap;
	}

	public final Production newStart(Production start) {
		return eliminateConditionalFlag(start);
	}

	@Override
	public Expression reshapeXon(Xon p) {
		String flagName = p.getFlagName();
		Boolean bool = condMap.get(flagName);
		if (bool != null) {
			if (p.isPositive()) {
				if (!bool) {
					condMap.put(flagName, true);
					Expression newe = p.get(0).reshape(this);
					condMap.put(flagName, false);
					return newe;
				}
			} else {
				if (bool) {
					condMap.put(flagName, false);
					Expression newe = p.get(0).reshape(this);
					condMap.put(flagName, true);
					return newe;
				}
			}
		}
		return p.get(0).reshape(this);
	}

	@Override
	public Expression reshapeXif(Xif p) {
		String flagName = p.getFlagName();
		if (condMap.get(flagName)) {
			return p.isPredicate() ? p.newEmpty() : p.newFailure();
		}
		return p.isPredicate() ? p.newFailure() : p.newEmpty();
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal n) {
		Production r = eliminateConditionalFlag(n.getProduction());
		if (r != n.getProduction()) {
			return n.newNonTerminal(r.getLocalName());
		}
		return n;
	}

	private Production eliminateConditionalFlag(Production p) {
		if (p.isConditional()) {
			String flagedName = nameFlagedProduction(p);
			Production newp = p.getGrammar().getProduction(flagedName);
			if (newp == null) {
				p = this.getBaseProduction(p);
				newp = ((GrammarFile) p.getGrammar()).newReducedProduction(flagedName, p, this);
				// Verbose.debug("creating .. " + flagedName);
			}
			return newp;
		}
		return p;
	}

	private Production getBaseProduction(Production p) {
		String localName = p.getLocalName();
		int loc = findCondition(localName);
		if (loc > 0) {
			localName = localName.substring(0, loc);
		}
		return p.getGrammar().getProduction(localName);
	}

	private String nameFlagedProduction(Production p) {
		StringBuilder sb = new StringBuilder();
		String localName = p.getLocalName();
		int loc = findCondition(localName);
		if (loc > 0) {
			sb.append(localName.substring(0, loc));
		} else {
			sb.append(localName);
		}

		for (String flagName : condMap.keySet()) {
			if (condMap.get(flagName)) {
				sb.append("&");
			} else {
				sb.append("!");
			}
			sb.append(flagName);
		}
		return sb.toString();
	}

	private int findCondition(String s) {
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '&' || c == '!') {
				return i;
			}
		}
		return -1;
	}

	// private static boolean hasReachableFlag(Expression e, String flagName) {
	// return hasReachableFlag(e, flagName, new UMap<String>());
	// }

	// private static boolean hasReachableFlag(Expression e, String flagName,
	// UMap<String> visited) {
	// if(e instanceof Xon) {
	// Xon f = (Xon)e;
	//
	// if(flagName.equals(((Xon) e).flagName)) {
	// return false;
	// }
	// }
	// for(Expression se : e) {
	// if(hasReachableFlag(se, flagName, visited)) {
	// return true;
	// }
	// }
	// if(e instanceof IfFlag) {
	// return flagName.equals(((IfFlag) e).flagName);
	// }
	// if(e instanceof NonTerminal) {
	// NonTerminal ne = (NonTerminal)e;
	// String un = ne.getUniqueName();
	// if(!visited.hasKey(un)) {
	// visited.put(un, un);
	// Production r = ne.getProduction();
	// return hasReachableFlag(r.body, flagName, visited);
	// }
	// }
	// return false;
	// }

}