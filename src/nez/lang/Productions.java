package nez.lang;

import java.util.HashMap;
import java.util.HashSet;

public class Productions {

	/**
	 * Test whether the given production p is recursive.
	 * 
	 * @param p
	 *            production
	 * @return true if the production is recursive
	 */

	public final static boolean isRecursive(Production p) {
		return isRecursive(p.getExpression(), p.getUniqueName(), new HashSet<>());
	}

	private final static boolean isRecursive(Expression e, String uname, HashSet<String> visited) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (p == null) {
				return false;
			}
			String u = ((NonTerminal) e).getUniqueName();
			if (uname.equals(u)) {
				return true;
			}
			if (!visited.contains(u)) {
				visited.add(u);
				return isRecursive(p.getExpression(), uname, visited);
			}
			return false;
		}
		for (Expression sub : e) {
			boolean b = isRecursive(sub, uname, visited);
			if (b == true) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Counts the number of nonterminal references in the given grammar
	 * 
	 * @param grammar
	 * @return
	 */

	public final static NonterminalReference countNonterminalReference(Grammar grammar) {
		NonterminalReference refc = new NonterminalReference();
		refc.put(grammar.getStartProduction().getUniqueName(), 1);
		for (Production p : grammar) {
			count(p.getExpression(), refc);
		}
		if (hasUnusedProduction(grammar, refc)) {
			NonterminalReference refc2 = new NonterminalReference();
			refc2.put(grammar.getStartProduction().getUniqueName(), 1);
			for (Production p : grammar) {
				String uname = p.getUniqueName();
				if (refc.count(uname) > 0) {
					count(p.getExpression(), refc2);
				}
			}
			return refc2;
		}
		return refc;
	}

	private final static void count(Expression e, HashMap<String, Integer> counts) {
		if (e instanceof NonTerminal) {
			String uname = ((NonTerminal) e).getUniqueName();
			Integer n = counts.get(uname);
			if (n == null) {
				counts.put(uname, 1);
			} else {
				counts.put(uname, n + 1);
			}
			return;
		}
		for (Expression sub : e) {
			count(sub, counts);
		}
	}

	private final static boolean hasUnusedProduction(Grammar grammar, NonterminalReference refc) {
		for (Production p : grammar) {
			String uname = p.getUniqueName();
			if (refc.count(uname) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * NonterminalReference holds the results of countNonterminalReference.
	 * 
	 * @author kiki
	 *
	 */
	@SuppressWarnings("serial")
	public static class NonterminalReference extends HashMap<String, Integer> {
		public final int count(String key) {
			Integer n = this.get(key);
			return n == null ? 0 : (int) n;
		}
	}

	// public static class ProductionProperty extends UList<String> {
	// HashMap<String, Boolean> boolMap;
	//
	// public ProductionProperty() {
	// super(new String[64]);
	// this.boolMap = new HashMap<>();
	// }
	//
	// public void push(String uname) {
	// this.add(uname);
	// }
	//
	// public boolean isVisited(String uname) {
	// for (String u : this) {
	// if (uname.equals(u)) {
	// return true;
	// }
	// }
	// return false;
	// }
	//
	// public boolean hasProperty(String uname) {
	// return this.boolMap.containsKey(uname);
	// }
	//
	// public Boolean getProperty(String uname) {
	// return this.boolMap.get(uname);
	// }
	//
	// public void setProperty(String uname, Boolean result) {
	// this.boolMap.put(uname, result);
	// }
	//
	// void setAll(String uname, Boolean result) {
	// for (int i = this.size() - 1; i >= 0; i--) {
	// String u = this.get(i);
	// this.boolMap.put(u, result);
	// if (uname.equals(u)) {
	// break;
	// }
	// }
	// }
	// }

	/*
	 * public final static boolean isContextual(Production p, ProductionProperty
	 * stack) { String uname = p.getUniqueName(); if (stack.hasProperty(uname))
	 * { return stack.getProperty(uname); } if(!stack.isVisited(uname)) {
	 * stack.push(uname); checkContextual(p.getExpression(), stack);
	 * stack.pop(); } if (stack.hasProperty(uname)) { return
	 * stack.getProperty(uname); } stack.setProperty(uname, false); return
	 * false; }
	 * 
	 * private void checkContextual(Expression e, ProductionProperty stack) { if
	 * (e instanceof NonTerminal) { Production p = ((NonTerminal)
	 * e).getProduction(); if (p == null) { stack.setProperty(((NonTerminal)
	 * e).getUniqueName(), false); return; } isContextual(p, stack); } if (e
	 * instanceof Nez.Contextual) { stack.setAll(uname, result); return; } for
	 * (Expression sub : e) { checkContextual(sub, v); } }
	 */

}
