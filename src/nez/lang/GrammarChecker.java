package nez.lang;

import java.util.HashMap;
import java.util.TreeMap;

import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tdetree;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Unary;
import nez.lang.expr.Xif;
import nez.lang.expr.Xon;
import nez.parser.ParseFunc;
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.Verbose;

public class GrammarChecker extends GrammarTransducer {
	ParserGrammar parserGrammar;
	private Typestate requiredTypestate;
	final TreeMap<String, Boolean> boolMap;
	UList<Expression> stacked;
	private final ParserStrategy strategy;

	public GrammarChecker(ParserGrammar gg, TreeMap<String, Boolean> boolMap, Production start, ParserStrategy strategy) {
		this.parserGrammar = gg;
		this.boolMap = (boolMap == null) ? new TreeMap<String, Boolean>() : boolMap;
		this.strategy = strategy;

		this.stacked = new UList<Expression>(new Expression[128]);
		if (!strategy.TreeConstruction) {
			this.enterNonASTContext();
		}
		String uname = uniqueName(start.getUniqueName(), start);
		this.checkFirstVisitedProduction(uname, start, 1); // start
		if (strategy.Optimization) {
			Verbose.println("optimizing %s ..", strategy);
			new GrammarOptimizer(gg, strategy);
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
		if (UFlag.is(p.flag, Production.ResetFlag)) {
			p.initFlag();
			if (p.isRecursive()) {
				checkLeftRecursion(p.getExpression(), new ProductionStacker(p, null));
			}
			// p.isNoNTreeConstruction();
		}
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
			if (e instanceof Psequence) {
				if (!checkLeftRecursion(e.get(0), s)) {
					return checkLeftRecursion(e.get(1), s);
				}
			}
			if (e instanceof Pchoice) {
				boolean consumed = true;
				for (Expression se : e) {
					if (!checkLeftRecursion(e.get(1), s)) {
						consumed = false;
					}
				}
				return consumed;
			}
			boolean r = checkLeftRecursion(e.get(0), s);
			if (e instanceof Pone) {
				return r;
			}
			if (e instanceof Pnot || e instanceof Pzero || e instanceof Poption || e instanceof Pand) {
				return false;
			}
			return r;
		}
		return e.isConsumed();
	}

	@Override
	public Expression visitNonTerminal(NonTerminal n, Object a) {
		Production p = n.getProduction();
		if (p == null) {
			if (n.isTerminal()) {
				reportNotice(n, "undefined terminal: " + n.getLocalName());
				return ExpressionCommons.newString(n.getSourcePosition(), StringUtils.unquoteString(n.getLocalName()));
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
				return ExpressionCommons.newString(n.getSourcePosition(), StringUtils.unquoteString(n.getLocalName()));
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
		NonTerminal pn = parserGrammar.newNonTerminal(n.getSourcePosition(), uname);
		if (innerTypestate == Typestate.Unit) {
			return pn;
		}
		Typestate required = this.requiredTypestate;
		if (required == Typestate.Tree) {
			if (innerTypestate == Typestate.TreeMutation) {
				reportInserted(n, "{");
				this.requiredTypestate = Typestate.TreeMutation;
				return ExpressionCommons.newNewCapture(n.getSourcePosition(), pn);
			}
			this.requiredTypestate = Typestate.TreeMutation;
			return pn;
		}
		if (required == Typestate.TreeMutation) {
			if (innerTypestate == Typestate.Tree) {
				reportInserted(n, "$");
				this.requiredTypestate = Typestate.Tree;
				return ExpressionCommons.newTlink(n.getSourcePosition(), null, pn);
			}
		}
		return pn;
	}

	@Override
	public Expression visitXon(Xon p, Object a) {
		String flagName = p.getFlagName();
		// System.out.println("on " + flagName);
		Boolean stackedFlag = isFlag(flagName);
		if (p.isPositive()) {
			onFlag(flagName);
		} else {
			offFlag(flagName);
		}
		Expression newe = visitInner(p.get(0));
		if (stackedFlag) {
			onFlag(flagName);
		} else {
			offFlag(flagName);
		}
		return newe;
	}

	@Override
	public Expression visitXif(Xif p, Object a) {
		String flagName = p.getFlagName();
		if (isFlag(flagName)) { /* true */
			return p.isPredicate() ? p.newEmpty() : p.newFailure();
		}
		return p.isPredicate() ? p.newFailure() : p.newEmpty();
	}

	void reportInserted(Expression e, String operator) {
		reportWarning(e, "expected " + operator + " .. => inserted!!");
	}

	void reportRemoved(Expression e, String operator) {
		reportWarning(e, "unexpected " + operator + " .. => removed!!");
	}

	@Override
	public Expression visitTdetree(Tdetree p, Object a) {
		boolean stacked = this.enterNonASTContext();
		Expression inner = this.visitInner(p.get(0));
		this.exitNonASTContext(stacked);
		return inner;
	}

	@Override
	public Expression visitTnew(Tnew p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.Tree) {
			this.reportRemoved(p, "{");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitTnew(p, a);
	}

	@Override
	public Expression visitTlfold(Tlfold p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			this.reportRemoved(p, "{$");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitTlfold(p, a);
	}

	@Override
	public Expression visitTcapture(Tcapture p, Object a) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			this.reportRemoved(p, "}");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitTcapture(p, a);
	}

	@Override
	public Expression visitTtag(Ttag p, Object a) {
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
	public Expression visitTreplace(Treplace p, Object a) {
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
	public Expression visitTlink(Tlink p, Object a) {
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
			inner = ExpressionCommons.newNewCapture(inner.getSourcePosition(), this.visitInner(inner));
		} else {
			this.requiredTypestate = Typestate.Tree;
			inner = this.visitInner(p.get(0));
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return ExpressionCommons.newTlink(p.getSourcePosition(), p.getLabel(), inner);
	}

	@Override
	public Expression visitPchoice(Pchoice p, Object a) {
		Typestate required = this.requiredTypestate;
		Typestate next = this.requiredTypestate;
		UList<Expression> l = ExpressionCommons.newList(p.size());
		for (Expression inner : p) {
			this.requiredTypestate = required;
			ExpressionCommons.addChoice(l, this.visitInner(inner));
			if (this.requiredTypestate != required && this.requiredTypestate != next) {
				next = this.requiredTypestate;
			}
		}
		this.requiredTypestate = next;
		return ExpressionCommons.newPchoice(p.getSourcePosition(), l);
	}

	@Override
	public Expression visitPzero(Pzero p, Object a) {
		return ExpressionCommons.newPzero(p.getSourcePosition(), visitOptionalInner(p));
	}

	@Override
	public Expression visitPone(Pone p, Object a) {
		return ExpressionCommons.newPone(p.getSourcePosition(), visitOptionalInner(p));
	}

	@Override
	public Expression visitPoption(Poption p, Object a) {
		return ExpressionCommons.newPoption(p.getSourcePosition(), visitOptionalInner(p));
	}

	private Expression visitOptionalInner(Unary p) {
		Typestate innerTypestate = this.isNonASTContext() ? Typestate.Unit : parserGrammar.typeState(p.get(0));
		if (innerTypestate == Typestate.Tree) {
			if (this.requiredTypestate == Typestate.TreeMutation) {
				this.reportInserted(p.get(0), "$");
				this.requiredTypestate = Typestate.Tree;
				Expression inner = visitInner(p.get(0));
				inner = ExpressionCommons.newTlink(p.getSourcePosition(), inner);
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
	public Expression visitPand(Pand p, Object a) {
		return ExpressionCommons.newPand(p.getSourcePosition(), visitOptionalInner(p));
	}

	@Override
	public Expression visitPnot(Pnot p, Object a) {
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
		return ExpressionCommons.newPnot(p.getSourcePosition(), inner);
	}

	/* static context */

	boolean enterNonASTContext() {
		boolean b = this.boolMap.get("-") != null;
		this.boolMap.put("-", true);
		return b;
	}

	void exitNonASTContext(boolean backed) {
		if (backed) {
			this.boolMap.put("-", true);
		} else {
			this.boolMap.remove("-");
		}
	}

	boolean isNonASTContext() {
		return this.boolMap.get("-") != null;
	}

	void onFlag(String flag) {
		this.boolMap.remove(flag);
	}

	void offFlag(String flag) {
		this.boolMap.put(flag, false);
	}

	boolean isFlag(String flag) {
		return this.boolMap.get(flag) == null;
	}

	/* uniquename */

	String uniqueName(String uname, Production p) {
		boolean isNonTree = parserGrammar.typeState(p) == Typestate.Unit;
		StringBuilder sb = new StringBuilder();
		sb.append(uname);
		for (String flagName : this.boolMap.keySet()) {
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
		// System.out.println("unique: " + uname + ", " + this.boolMap.keySet()
		// + "=>" + sb.toString());
		return sb.toString();
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
		this.strategy.reportError(p.getSourcePosition(), message);
	}

	public final void reportWarning(Expression p, String message) {
		this.strategy.reportWarning(p.getSourcePosition(), message);
	}

	public final void reportNotice(Expression p, String message) {
		this.strategy.reportNotice(p.getSourcePosition(), message);
	}

}