package nez.lang;

import java.util.HashMap;
import java.util.HashSet;

import nez.util.ConsoleUtils;

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
			return n == null ? 0 : n;
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

	private static LeftRecursionChecker leftRecursionChecker = new LeftRecursionChecker();

	public final static boolean isLeftRecursive1(Production p) {
		try {
			return leftRecursionChecker.check(p.getExpression(), null);
		} catch (StackOverflowError e) {
		}
		return true;
	}

	public final static void checkLeftRecursion(Production p) {
		leftRecursionChecker.check(p.getExpression(), p.getUniqueName());
	}

	private static class LeftRecursionChecker extends Expression.Visitor {

		boolean check(Expression e, Object a) {
			return (Boolean) e.visit(this, a);
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			if (a != null) {
				if (e.getUniqueName().equals(a)) {
					ConsoleUtils.perror(e.getGrammar(), e.formatSourceMessage("error", "left recursion"));
					return true;
				}
			}
			return check(e.getProduction().getExpression(), a);
		}

		@Override
		public Object visitEmpty(Nez.Empty e, Object a) {
			return true;
		}

		@Override
		public Object visitFail(Nez.Fail e, Object a) {
			return true;
		}

		@Override
		public Object visitByte(Nez.Byte e, Object a) {
			return false;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			return false;
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			return false;
		}

		@Override
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
			return false;
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			if (check(e.get(0), a) == false) {
				return false;
			}
			return check(e.get(1), a);
		}

		@Override
		public Object visitSequence(Nez.Sequence e, Object a) {
			for (Expression sub : e) {
				boolean c = check(sub, a);
				if (c == false) {
					return false;
				}
			}
			return true;
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
			boolean unconsumed = false;
			for (Expression sub : e) {
				boolean c = check(sub, a);
				if (c == true) {
					unconsumed = true;
				}
			}
			return unconsumed;
		}

		@Override
		public Object visitOption(Nez.Option e, Object a) {
			return true;
		}

		@Override
		public Object visitZeroMore(Nez.ZeroMore e, Object a) {
			return true;
		}

		@Override
		public Object visitOneMore(Nez.OneMore e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitAnd(Nez.And e, Object a) {
			return true;
		}

		@Override
		public Object visitNot(Nez.Not e, Object a) {
			return true;
		}

		@Override
		public Object visitBeginTree(Nez.BeginTree e, Object a) {
			return true;
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			return true;
		}

		@Override
		public Object visitLinkTree(Nez.LinkTree e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			return true;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			return true;
		}

		@Override
		public Object visitEndTree(Nez.EndTree e, Object a) {
			return true;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitBlockScope(Nez.BlockScope e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitLocalScope(Nez.LocalScope e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitSymbolAction(Nez.SymbolAction e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
			return true;
		}

		@Override
		public Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitSymbolExists(Nez.SymbolExists e, Object a) {
			return check(e.get(0), a);
		}

		@Override
		public Object visitIf(Nez.IfCondition e, Object a) {
			return true;
		}

		@Override
		public Object visitOn(Nez.OnCondition e, Object a) {
			return check(e.get(0), a);
		}
	}

}
