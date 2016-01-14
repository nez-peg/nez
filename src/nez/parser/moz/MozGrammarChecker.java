package nez.parser.moz;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import nez.lang.Conditions;
import nez.lang.Expression;
import nez.lang.ExpressionDuplicationVisitor;
import nez.lang.ExpressionTransformer;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Productions;
import nez.lang.Typestate;
import nez.parser.ParserStrategy;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.Verbose;

class MozGrammarChecker extends ExpressionDuplicationVisitor {
	private final ParserStrategy strategy;
	ParserGrammar parserGrammar;
	final TreeMap<String, Boolean> boolMap0;
	final Conditions conds;
	UList<Expression> stacked;

	private Typestate requiredTypestate;

	public MozGrammarChecker(ParserGrammar gg, TreeMap<String, Boolean> boolMap, Production start, ParserStrategy strategy) {
		this.parserGrammar = gg;
		this.strategy = strategy;
		this.boolMap0 = (boolMap == null) ? new TreeMap<String, Boolean>() : boolMap;
		this.conds = Conditions.newConditions(start, boolMap);

		this.stacked = new UList<Expression>(new Expression[128]);
		if (!strategy.TreeConstruction) {
			this.enterNonASTContext();
		}
		String uname = uniqueName(start.getUniqueName(), start);
		this.checkFirstVisitedProduction(uname, start); // start
		if (strategy.Optimization) {
			Verbose.println("optimizing %s ..", strategy);
			new MozGrammarOptimizer(gg, strategy);
		}
		new Normalizer().perform(gg);
	}

	private Expression visitInner(Expression e) {
		return (Expression) e.visit(this, null);
	}

	private void checkFirstVisitedProduction(String uname, Production p) {
		Production parserProduction/* local production */= parserGrammar.addProduction(uname, null);
		this.visited(uname);
		Productions.checkLeftRecursion(p);
		Typestate stackedTypestate = this.requiredTypestate;
		this.requiredTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(p);
		Expression e = this.visitInner(p.getExpression());
		parserProduction.setExpression(e);
		this.requiredTypestate = stackedTypestate;
	}

	@Override
	public Expression visitNonTerminal(NonTerminal n, Object a) {
		Production p = n.getProduction();
		if (p == null) {
			if (n.isTerminal()) {
				reportNotice(n, "undefined terminal: " + n.getLocalName());
				return Expressions.newExpression(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
			}
			reportWarning(n, "undefined production: " + n.getLocalName());
			return n.newEmpty();
		}
		if (n.isTerminal()) { /* Inlining Terminal */
			try {
				return visitInner(p.getExpression());
			} catch (StackOverflowError e) {
				/* Handling a bad grammar */
				reportError(n, "terminal is recursive: " + n.getLocalName());
				return Expressions.newExpression(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
			}
		}
		// System.out.print("NonTerminal: " + n.getLocalName() + " -> ");
		// this.dumpStack();

		Typestate innerTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(p);
		String uname = this.uniqueName(n.getUniqueName(), p);
		if (!this.isVisited(uname)) {
			checkFirstVisitedProduction(uname, p);
		}
		NonTerminal pn = Expressions.newNonTerminal(n.getSourceLocation(), parserGrammar, uname);
		if (innerTypestate == Typestate.Unit) {
			return pn;
		}
		Typestate required = this.requiredTypestate;
		if (required == Typestate.Tree) {
			if (innerTypestate == Typestate.TreeMutation) {
				reportInserted(n, "{");
				this.requiredTypestate = Typestate.TreeMutation;
				return Expressions.newTree(n.getSourceLocation(), pn);
			}
			this.requiredTypestate = Typestate.TreeMutation;
			return pn;
		}
		if (required == Typestate.TreeMutation) {
			if (innerTypestate == Typestate.Tree) {
				reportInserted(n, "$");
				this.requiredTypestate = Typestate.Tree;
				return Expressions.newLinkTree(n.getSourceLocation(), null, pn);
			}
		}
		return pn;
	}

	@Override
	public Expression visitOn(Nez.OnCondition p, Object a) {
		Boolean stackedFlag = isFlag(p.flagName);
		if (p.isPositive()) {
			onFlag(p.flagName);
		} else {
			offFlag(p.flagName);
		}
		Expression newe = visitInner(p.get(0));
		if (stackedFlag) {
			onFlag(p.flagName);
		} else {
			offFlag(p.flagName);
		}
		return newe;
	}

	@Override
	public Expression visitIf(Nez.IfCondition p, Object a) {
		if (isFlag(p.flagName)) { /* true */
			return p.predicate ? p.newEmpty() : p.newFailure();
		}
		return p.predicate ? p.newFailure() : p.newEmpty();
	}

	void reportInserted(Expression e, String operator) {
		reportWarning(e, "expected " + operator + " .. => inserted!!");
	}

	void reportRemoved(Expression e, String operator) {
		reportWarning(e, "unexpected " + operator + " .. => removed!!");
	}

	@Override
	public Expression visitDetree(Nez.Detree p, Object a) {
		boolean stacked = this.enterNonASTContext();
		Expression inner = this.visitInner(p.get(0));
		this.exitNonASTContext(stacked);
		return inner;
	}

	@Override
	public Expression visitBeginTree(Nez.BeginTree p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.Tree) {
			this.reportRemoved(p, "{");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitBeginTree(p, a);
	}

	@Override
	public Expression visitFoldTree(Nez.FoldTree p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			this.reportRemoved(p, "{$");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitFoldTree(p, a);
	}

	@Override
	public Expression visitEndTree(Nez.EndTree p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			this.reportRemoved(p, "}");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitEndTree(p, a);
	}

	@Override
	public Expression visitTag(Nez.Tag p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			reportRemoved(p, "#" + p.tag.getSymbol());
			return p.newEmpty();
		}
		return p;
	}

	@Override
	public Expression visitReplace(Nez.Replace p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			reportRemoved(p, "`" + p.value + "`");
			return p.newEmpty();
		}
		return p;
	}

	@Override
	public Expression visitLinkTree(Nez.LinkTree p, Object a) {
		Expression inner = p.get(0);
		if (this.isNonASTContext()) {
			return this.visitInner(inner);
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			reportRemoved(p, "$");
			return this.visitInner(inner);
		}
		Typestate innerTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(inner);
		if (innerTypestate != Typestate.Tree) {
			reportInserted(p, "{");
			inner = Expressions.newTree(inner.getSourceLocation(), this.visitInner(inner));
		} else {
			this.requiredTypestate = Typestate.Tree;
			inner = this.visitInner(p.get(0));
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return Expressions.newLinkTree(p.getSourceLocation(), p.label, inner);
	}

	@Override
	public Expression visitChoice(Nez.Choice p, Object a) {
		Typestate required = this.requiredTypestate;
		Typestate next = this.requiredTypestate;
		UList<Expression> l = Expressions.newUList(p.size());
		for (Expression inner : p) {
			this.requiredTypestate = required;
			Expressions.addChoice(l, this.visitInner(inner));
			if (this.requiredTypestate != required && this.requiredTypestate != next) {
				next = this.requiredTypestate;
			}
		}
		this.requiredTypestate = next;
		return Expressions.newChoice(l);
	}

	@Override
	public Expression visitZeroMore(Nez.ZeroMore p, Object a) {
		return Expressions.newZeroMore(p.getSourceLocation(), visitOptionalInner(p));
	}

	@Override
	public Expression visitOneMore(Nez.OneMore p, Object a) {
		return Expressions.newOneMore(p.getSourceLocation(), visitOptionalInner(p));
	}

	@Override
	public Expression visitOption(Nez.Option p, Object a) {
		return Expressions.newOption(p.getSourceLocation(), visitOptionalInner(p));
	}

	private Expression visitOptionalInner(Nez.Unary p) {
		Typestate innerTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(p.get(0));
		if (innerTypestate == Typestate.Tree) {
			if (this.requiredTypestate == Typestate.TreeMutation) {
				this.reportInserted(p.get(0), "$");
				this.requiredTypestate = Typestate.Tree;
				Expression inner = visitInner(p.get(0));
				inner = Expressions.newLinkTree(p.getSourceLocation(), inner);
				this.requiredTypestate = Typestate.TreeMutation;
				return inner;
			} else {
				reportWarning(p, "disallowed tree construction in e?, e*, e+, or &e  " + innerTypestate);
				boolean stacked = this.enterNonASTContext();
				Expression inner = visitInner(p.get(0));
				this.exitNonASTContext(stacked);
				return inner;
			}
		}
		return visitInner(p.get(0));
	}

	@Override
	public Expression visitAnd(Nez.And p, Object a) {
		return Expressions.newAnd(p.getSourceLocation(), visitOptionalInner(p));
	}

	@Override
	public Expression visitNot(Nez.Not p, Object a) {
		Expression inner = p.get(0);
		Typestate innerTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(inner);
		if (innerTypestate != Typestate.Unit) {
			// reportWarning(p, "disallowed tree construction in !e");
			boolean stacked = this.enterNonASTContext();
			inner = this.visitInner(inner);
			this.exitNonASTContext(stacked);
		} else {
			inner = this.visitInner(inner);
		}
		return Expressions.newNot(p.getSourceLocation(), inner);
	}

	/* static context */

	boolean enterNonASTContext() {
		boolean b = this.boolMap0.get("-") != null;
		this.boolMap0.put("-", true);
		return b;
	}

	void exitNonASTContext(boolean backed) {
		if (backed) {
			this.boolMap0.put("-", true);
		} else {
			this.boolMap0.remove("-");
		}
	}

	boolean isNonASTContext() {
		return this.boolMap0.get("-") != null;
	}

	void onFlag(String flag) {
		this.conds.put(flag, true);
		this.boolMap0.remove(flag);
	}

	void offFlag(String flag) {
		this.conds.put(flag, false);
		this.boolMap0.put(flag, false);
	}

	boolean isFlag(String flag) {
		return this.boolMap0.get(flag) == null;
	}

	/* uniquename */

	String uniqueName(String uname, Production p) {
		boolean isNonTree = parserGrammar.typeState(p) == Typestate.Unit;
		StringBuilder sb = new StringBuilder();
		sb.append(uname);
		for (String flagName : this.boolMap0.keySet()) {
			if (flagName.equals("-")) {
				if (!isNonTree) {
					sb.append(flagName);
				}
				continue;
			}
			if (hasFlag(p, flagName) != False) {
				sb.append("!");
				sb.append(flagName);
			}
		}
		String cname = sb.toString();
		// System.out.println("unique: " + uname + ", " + this.boolMap.keySet()
		// + "=>" + sb.toString());
		return cname;
	}

	final static Short True = 1;
	final static Short False = -1;
	final static Short Unknown = 0;

	HashMap<String, Short> flagMap = new HashMap<String, Short>();

	private Short hasFlag(Production p, String flagName) {
		if (flagMap == null) {
			this.flagMap = new HashMap<String, Short>();
		}
		String key = p.getUniqueName() + "+" + flagName;
		Short res = flagMap.get(key);
		if (res == null) {
			flagMap.put(key, Unknown);
			res = hasFlag(p.getExpression(), flagName);
			flagMap.put(key, res);
		}
		return res;
	}

	private Short hasFlag(Expression e, String flagName) {
		if (e instanceof Nez.IfCondition) {
			return flagName.equals(((Nez.IfCondition) e).flagName) ? True : False;
		}
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (p == null) {
				return False;
			}
			return hasFlag(p, flagName);
		}
		for (Expression se : e) {
			if (hasFlag(se, flagName) == True) {
				return True;
			}
		}
		return False;
	}

	// Report

	public final void reportError(Expression p, String message) {
		this.strategy.reportError(p.getSourceLocation(), message);
	}

	public final void reportWarning(Expression p, String message) {
		this.strategy.reportWarning(p.getSourceLocation(), message);
	}

	public final void reportNotice(Expression p, String message) {
		this.strategy.reportNotice(p.getSourceLocation(), message);
	}

	static class Normalizer extends ExpressionTransformer {

		void perform(Grammar g) {
			final Map<String, Integer> refCounts = Productions.countNonterminalReference(g);
			UList<Production> prodList = new UList<Production>(new Production[g.size()]);
			for (Production p : g) {
				Integer refcnt = refCounts.get(p.getUniqueName());
				if (refcnt != null && refcnt > 0) {
					Expression e = visitInner(p.getExpression(), null);
					p.setExpression(e);
					prodList.add(p);
				}
			}
			g.update(prodList);
		}

		@Override
		public Expression visitChoice(Nez.Choice p, Object a) {
			if (p.predicted != null) {
				Expression[] l = p.predicted.unique0;
				for (int i = 1; i < l.length; i++) {
					l[i] = (Expression) l[i].visit(this, a);
				}
				return p;
			} else {
				return super.visitChoice(p, a);
			}
		}

		@Override
		public Expression visitPair(Nez.Pair p, Object a) {
			return Expressions.tryConvertingMultiCharSequence(p);
		}

	}

}