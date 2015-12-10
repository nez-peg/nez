package nez.lang;

import java.util.HashMap;
import java.util.TreeMap;

import nez.Verbose;
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

public class GrammarChecker extends GrammarTransducer {
	ParserGrammar gg;
	private int requiredTypestate;
	final TreeMap<String, Boolean> boolMap;
	UList<Expression> stacked;
	private final ParserStrategy strategy;

	public GrammarChecker(ParserGrammar gg, TreeMap<String, Boolean> boolMap, Production start, ParserStrategy strategy) {
		this.gg = gg;
		this.boolMap = (boolMap == null) ? new TreeMap<String, Boolean>() : boolMap;
		this.strategy = strategy;

		this.stacked = new UList<Expression>(new Expression[128]);
		if (!strategy.TreeConstruction) {
			this.enterNonASTContext();
		}
		String uname = uniqueName(start.getUniqueName(), start);
		this.checkFirstVisitedProduction(uname, start, 1); // start
		if (!strategy.Optimization) {
			if (ConsoleUtils.isDebug()) {
				Verbose.println("optimizing ..");
			}
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
			System.out.print(" ");
			if (e instanceof NonTerminal) {
				System.out.print(((NonTerminal) e).getLocalName());
			} else {
				System.out.print(e.getClass().getSimpleName());
			}
		}
		System.out.println();
	}

	private ParseFunc checkFirstVisitedProduction(String uname, Production p, int init) {
		Production parserProduction/* local production */= gg.newProduction(uname, null);
		ParseFunc f = gg.setParseFunc(uname, p, parserProduction, init);
		if (UFlag.is(p.flag, Production.ResetFlag)) {
			p.initFlag();
			if (p.isRecursive()) {
				checkLeftRecursion(p.getExpression(), new ProductionStacker(p, null));
			}
			p.isNoNTreeConstruction();
		}
		int stackedTypestate = this.requiredTypestate;
		this.requiredTypestate = this.isNonASTContext() ? Typestate.BooleanType : p.inferTypestate(null);
		Expression e = this.reshapeInner(p.getExpression());
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
	public Expression reshapeNonTerminal(NonTerminal n) {
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
				return reshapeInner(p.getExpression());
			} catch (StackOverflowError e) {
				/* Handling a bad grammar */
				reportError(n, "terminal is recursive: " + n.getLocalName());
				return ExpressionCommons.newString(n.getSourcePosition(), StringUtils.unquoteString(n.getLocalName()));
			}
		}
		// System.out.print("NonTerminal: " + n.getLocalName() + " -> ");
		// this.dumpStack();

		int innerTypestate = this.isNonASTContext() ? Typestate.BooleanType : p.inferTypestate(null);
		String uname = this.uniqueName(n.getUniqueName(), p);
		ParseFunc f = gg.getParseFunc(uname);
		if (f == null) {
			f = checkFirstVisitedProduction(uname, p, 1);
		} else {
			f.incCount();
		}
		NonTerminal pn = gg.newNonTerminal(n.getSourcePosition(), uname);
		if (innerTypestate == Typestate.BooleanType) {
			return pn;
		}
		int required = this.requiredTypestate;
		if (required == Typestate.ObjectType) {
			if (innerTypestate == Typestate.OperationType) {
				reportInserted(n, "{");
				this.requiredTypestate = Typestate.OperationType;
				return ExpressionCommons.newNewCapture(n.getSourcePosition(), pn);
			}
			this.requiredTypestate = Typestate.OperationType;
			return pn;
		}
		if (required == Typestate.OperationType) {
			if (innerTypestate == Typestate.ObjectType) {
				reportInserted(n, "$");
				this.requiredTypestate = Typestate.ObjectType;
				return ExpressionCommons.newTlink(n.getSourcePosition(), null, pn);
			}
		}
		return pn;
	}

	@Override
	public Expression reshapeXon(Xon p) {
		String flagName = p.getFlagName();
		// System.out.println("on " + flagName);
		Boolean stackedFlag = isFlag(flagName);
		if (p.isPositive()) {
			onFlag(flagName);
		} else {
			offFlag(flagName);
		}
		Expression newe = reshapeInner(p.get(0));
		if (stackedFlag) {
			onFlag(flagName);
		} else {
			offFlag(flagName);
		}
		return newe;
	}

	@Override
	public Expression reshapeXif(Xif p) {
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

	// @Override
	// public Expression reshapeProduction(Production p) {
	// int t = checkNamingConvention(p.name);
	// this.required = p.inferTypestate(null);
	// if (t != Typestate.Undefined && this.required != t) {
	// reportNotice(p, "invalid naming convention: " + p.name);
	// }
	// p.setExpression(p.getExpression().reshape(this));
	// return p;
	// }
	//
	// private static int checkNamingConvention(String ruleName) {
	// int start = 0;
	// if (ruleName.startsWith("~") || ruleName.startsWith("\"")) {
	// return Typestate.BooleanType;
	// }
	// for (; ruleName.charAt(start) == '_'; start++) {
	// if (start + 1 == ruleName.length()) {
	// return Typestate.BooleanType;
	// }
	// }
	// boolean firstUpperCase = Character.isUpperCase(ruleName.charAt(start));
	// for (int i = start + 1; i < ruleName.length(); i++) {
	// char ch = ruleName.charAt(i);
	// if (ch == '!')
	// break; // option
	// if (Character.isUpperCase(ch) && !firstUpperCase) {
	// return Typestate.OperationType;
	// }
	// if (Character.isLowerCase(ch) && firstUpperCase) {
	// return Typestate.ObjectType;
	// }
	// }
	// return firstUpperCase ? Typestate.BooleanType : Typestate.Undefined;
	// }

	@Override
	public Expression reshapeTdetree(Tdetree p) {
		boolean stacked = this.enterNonASTContext();
		Expression inner = this.reshapeInner(p.get(0));
		this.exitNonASTContext(stacked);
		return inner;
	}

	@Override
	public Expression reshapeTnew(Tnew p) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.ObjectType) {
			this.reportRemoved(p, "{");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.OperationType;
		return super.reshapeTnew(p);
	}

	@Override
	public Expression reshapeTlfold(Tlfold p) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.OperationType) {
			this.reportRemoved(p, "{$");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.OperationType;
		return super.reshapeTlfold(p);
	}

	@Override
	public Expression reshapeTcapture(Tcapture p) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.OperationType) {
			this.reportRemoved(p, "}");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.OperationType;
		return super.reshapeTcapture(p);
	}

	@Override
	public Expression reshapeTtag(Ttag p) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.OperationType) {
			reportRemoved(p, "#" + p.tag.getSymbol());
			return p.newEmpty();
		}
		return p;
	}

	@Override
	public Expression reshapeTreplace(Treplace p) {
		if (this.isNonASTContext()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.OperationType) {
			reportRemoved(p, "`" + p.value + "`");
			return p.newEmpty();
		}
		return p;
	}

	@Override
	public Expression reshapeTlink(Tlink p) {
		Expression inner = p.get(0);
		if (this.isNonASTContext()) {
			return this.reshapeInner(inner);
		}
		if (this.requiredTypestate != Typestate.OperationType) {
			reportRemoved(p, "$");
			return this.reshapeInner(inner);
		}
		int innerTypestate = this.isNonASTContext() ? Typestate.BooleanType : inner.inferTypestate(null);
		if (innerTypestate != Typestate.ObjectType) {
			reportInserted(p, "{");
			inner = ExpressionCommons.newNewCapture(inner.getSourcePosition(), this.reshapeInner(inner));
		} else {
			this.requiredTypestate = Typestate.ObjectType;
			inner = this.reshapeInner(p.get(0));
		}
		this.requiredTypestate = Typestate.OperationType;
		return ExpressionCommons.newTlink(p.getSourcePosition(), p.getLabel(), inner);
	}

	@Override
	public Expression reshapePchoice(Pchoice p) {
		int required = this.requiredTypestate;
		int next = this.requiredTypestate;
		UList<Expression> l = ExpressionCommons.newList(p.size());
		for (Expression inner : p) {
			this.requiredTypestate = required;
			ExpressionCommons.addChoice(l, this.reshapeInner(inner));
			if (this.requiredTypestate != required && this.requiredTypestate != next) {
				next = this.requiredTypestate;
			}
		}
		this.requiredTypestate = next;
		return ExpressionCommons.newPchoice(p.getSourcePosition(), l);
	}

	@Override
	public Expression reshapePzero(Pzero p) {
		return ExpressionCommons.newPzero(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	@Override
	public Expression reshapePone(Pone p) {
		return ExpressionCommons.newPone(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	@Override
	public Expression reshapePoption(Poption p) {
		return ExpressionCommons.newPoption(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	private Expression reshapeOptionalInner(Unary p) {
		int innerTypestate = this.isNonASTContext() ? Typestate.BooleanType : p.get(0).inferTypestate(null);
		if (innerTypestate == Typestate.ObjectType) {
			if (this.requiredTypestate == Typestate.OperationType) {
				this.reportInserted(p.get(0), "$");
				this.requiredTypestate = Typestate.ObjectType;
				Expression inner = reshapeInner(p.get(0));
				inner = ExpressionCommons.newTlink(p.getSourcePosition(), inner);
				this.requiredTypestate = Typestate.OperationType;
				return inner;
			} else {
				reportWarning(p, "disallowed tree construction in e?, e*, e+, or &e  " + innerTypestate);
				boolean stacked = this.enterNonASTContext();
				Expression inner = reshapeInner(p.get(0));
				this.exitNonASTContext(stacked);
				return inner;
			}
		}
		return reshapeInner(p.get(0));
	}

	@Override
	public Expression reshapePand(Pand p) {
		return ExpressionCommons.newPand(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	@Override
	public Expression reshapePnot(Pnot p) {
		Expression inner = p.get(0);
		int innerTypestate = this.isNonASTContext() ? Typestate.BooleanType : inner.inferTypestate(null);
		if (innerTypestate != Typestate.BooleanType) {
			// reportWarning(p, "disallowed tree construction in !e");
			boolean stacked = this.enterNonASTContext();
			inner = this.reshapeInner(inner);
			this.exitNonASTContext(stacked);
		} else {
			inner = this.reshapeInner(inner);
		}
		return ExpressionCommons.newPnot(p.getSourcePosition(), inner);
	}

	// @Override
	// public Expression reshapeXdef(Xdef p) {
	// Expression inner = p.get(0);
	// p.getGrammar().get
	// // int t = inner.inferTypestate(null);
	// // if (t != Typestate.BooleanType) {
	// // boolean stacked = this.enterNonASTContext();
	// // inner = this.reshapeInner(inner);
	// // this.exitNonASTContext(stacked);
	// // } else {
	// inner = this.reshapeInner(inner);
	// // }
	// String uname = p.getTableName();
	// Production pp = this.gg.newProduction(inner.getSourcePosition(),
	// Production.SymbolTableProduction, uname, inner);
	// this.gg.setParseFunc(uname, null, pp, 2/* to avoid removal */);
	// Expression ndef = ExpressionCommons.newXdef(p.getSourcePosition(), gg,
	// p.getTable(), gg.newNonTerminal(null, uname));
	// // System.out.println("@@@@" + p + "\n\t" + ndef + "   " +
	// // gg.hasProduction(uname));
	// return ndef;
	// }
	//
	// @Override
	// public Expression reshapeXis(Xis p) {
	// ParseFunc f = this.gg.getParseFunc(p.getTableName());
	// if (f == null) {
	// reportError(p, "undefined table: " + p.getTableName());
	// return ExpressionCommons.newFailure(p.getSourcePosition());
	// }
	// f.incCount();
	// return ExpressionCommons.newXis(p.getSourcePosition(), gg, p.getTable(),
	// p.is);
	// }

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
		boolean isNonTree = p.isNoNTreeConstruction();
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