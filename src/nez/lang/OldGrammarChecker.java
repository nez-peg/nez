package nez.lang;

import java.util.HashMap;
import java.util.TreeMap;

import nez.lang.expr.Expressions;
import nez.lang.expr.Pnot;
import nez.lang.expr.Xif;
import nez.parser.ParseFunc;
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.Verbose;

public class OldGrammarChecker extends OldGrammarTransducer {
	private final ParserStrategy strategy;
	ParserGrammar parserGrammar;
	final TreeMap<String, Boolean> boolMap0;
	final Conditions conds;
	UList<Expression> stacked;

	private Typestate requiredTypestate;

	public OldGrammarChecker(ParserGrammar gg, TreeMap<String, Boolean> boolMap, Production start, ParserStrategy strategy) {
		this.parserGrammar = gg;
		this.strategy = strategy;
		this.boolMap0 = (boolMap == null) ? new TreeMap<String, Boolean>() : boolMap;
		this.conds = Conditions.newConditions(start, boolMap);

		this.stacked = new UList<Expression>(new Expression[128]);
		if (!strategy.TreeConstruction) {
			this.enterNonASTContext();
		}
		String uname = uniqueName(start.getUniqueName(), start);
		this.checkFirstVisitedProduction(uname, start, 1); // start
		if (strategy.Optimization) {
			Verbose.println("optimizing %s ..", strategy);
			new OldGrammarOptimizer(gg, strategy);
		}
	}

	@Override
	protected void push(Expression e) {
		this.stacked.add(e);
	}

	@Override
	protected void pop(Expression e) {
		Expression e2 = this.stacked.pop();
		// Expression e2 = this.stacked.pop();
		// if (e != e2) {
		// Verbose.debug("FIXME push/pop \n\t" + e2 + "\n\t" + e);
		// }
	}

	protected void dumpStack() {
		for (Expression e : this.stacked) {
			ConsoleUtils.print(" ");
			if (e instanceof NonTerminal) {
				ConsoleUtils.print(((NonTerminal) e).getLocalName());
			} else {
				ConsoleUtils.print(e.getClass().getSimpleName());
			}
		}
		ConsoleUtils.println("");
	}

	private ParseFunc checkFirstVisitedProduction(String uname, Production p, int init) {
		Production parserProduction/* local production */= parserGrammar.newProduction(uname, null);
		ParseFunc f = parserGrammar.setParseFunc(uname, p, parserProduction, init);
		// if (UFlag.is(p.flag, Production.ResetFlag)) {
		// p.initFlag();
		// if (p.isRecursive()) {
		// checkLeftRecursion(p.getExpression(), new ProductionStacker(p,
		// null));
		// }
		// // p.isNoNTreeConstruction();
		// }
		Typestate stackedTypestate = this.requiredTypestate;
		this.requiredTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(p);
		Expression e = this.visitInner(p.getExpression());
		parserProduction.setExpression(e);
		this.requiredTypestate = stackedTypestate;
		return f;
	}

	boolean checkLeftRecursion(Expression e, ProductionStacker s) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (s.isVisited(p)) {
				reportError(e, "left recursion: " + p.getLocalName());
				return true; // stop as consumed
			}
			return checkLeftRecursion(p.getExpression(), new ProductionStacker(p, s));
		}
		if (e.size() > 0) {
			if (e instanceof Nez.Sequence) {
				if (!checkLeftRecursion(e.get(0), s)) {
					return checkLeftRecursion(e.get(1), s);
				}
			}
			if (e instanceof Nez.Choice) {
				boolean consumed = true;
				for (Expression se : e) {
					if (!checkLeftRecursion(e.get(1), s)) {
						consumed = false;
					}
				}
				return consumed;
			}
			boolean r = checkLeftRecursion(e.get(0), s);
			if (e instanceof Nez.OneMore) {
				return r;
			}
			if (e instanceof Nez.Not || e instanceof Nez.ZeroMore || e instanceof Nez.Option || e instanceof Nez.And) {
				return false;
			}
			return r;
		}
		return this.parserGrammar.isConsumed(e);
	}

	@Override
	public Expression visitNonTerminal(NonTerminal n, Object a) {
		Production p = n.getProduction();
		if (p == null) {
			if (n.isTerminal()) {
				reportNotice(n, "undefined terminal: " + n.getLocalName());
				return Expressions.newMultiByte(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
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
				return Expressions.newMultiByte(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
			}
		}
		// System.out.print("NonTerminal: " + n.getLocalName() + " -> ");
		// this.dumpStack();

		Typestate innerTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(p);
		String uname = this.uniqueName(n.getUniqueName(), p);
		ParseFunc f = parserGrammar.getParseFunc(uname);
		if (f == null) {
			f = checkFirstVisitedProduction(uname, p, 1);
		} else {
			f.incCount();
		}
		NonTerminal pn = parserGrammar.newNonTerminal(n.getSourceLocation(), uname);
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
	public Expression visitOn(Nez.On p, Object a) {
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
	public Expression visitIf(Nez.If p, Object a) {
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
	public Expression visitLeftFold(Nez.LeftFold p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			this.reportRemoved(p, "{$");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitLeftFold(p, a);
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
	public Expression visitLink(Nez.Link p, Object a) {
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
		UList<Expression> l = Expressions.newList(p.size());
		for (Expression inner : p) {
			this.requiredTypestate = required;
			Expressions.addChoice(l, this.visitInner(inner));
			if (this.requiredTypestate != required && this.requiredTypestate != next) {
				next = this.requiredTypestate;
			}
		}
		this.requiredTypestate = next;
		return Expressions.newChoice(p.getSourceLocation(), l);
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
		if (e instanceof Xif) {
			return flagName.equals(((Xif) e).getFlagName()) ? True : False;
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

}