package nez.lang;

import java.util.HashMap;
import java.util.TreeMap;

import nez.ast.Reporter;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Psequence;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Uand;
import nez.lang.expr.Umatch;
import nez.lang.expr.Unary;
import nez.lang.expr.Unot;
import nez.lang.expr.Uone;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Xdef;
import nez.lang.expr.Xif;
import nez.lang.expr.Xis;
import nez.lang.expr.Xon;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.vm.ParseFunc;
import nez.vm.ParserGrammar;

public class GrammarChecker extends GrammarTransducer {
	ParserGrammar g;
	// boolean offAST = true;
	private int requiredTypeState = Typestate.BooleanType;
	final TreeMap<String, Boolean> boolMap;
	UList<Expression> stacked;
	int stacktop = 0;
	Reporter repo = new Reporter();

	public GrammarChecker(ParserGrammar g, boolean offAST, TreeMap<String, Boolean> ctx, Production start) {
		this.g = g;
		this.boolMap = (ctx == null) ? new TreeMap<String, Boolean>() : ctx;

		this.stacked = new UList<Expression>(new Expression[128]);
		this.stacktop = 0;
		if (offAST) {
			this.enterNonASTContext();
		}
		String uname = uniqueName(start.getUniqueName(), start);
		this.checkFirstVisitedProduction(uname, start); // start
	}

	@Override
	protected void push(Expression e) {
		this.stacked.add(e);
	}

	@Override
	protected void pop(Expression e) {
		this.stacked.pop();
		// Expression e2 = this.stacked.pop();
		// if (e != e2) {
		// Verbose.debug("FIXME push/pop \n\t" + e2 + "\n\t" + e);
		// }
	}

	private ParseFunc checkFirstVisitedProduction(String uname, Production p) {
		Production lp/* local production */= g.newProduction(uname, null);
		ParseFunc f = new ParseFunc(uname, p);
		g.setParserFunc(f);
		if (UFlag.is(p.flag, Production.ResetFlag)) {
			p.initFlag();
			if (p.isRecursive()) {
				checkLeftRecursion(p.getExpression(), new ProductionStacker(p, null));
			}
			p.isNoNTreeConstruction();
		}
		int stackedTypeState = this.requiredTypeState;
		this.requiredTypeState = this.isNonASTContext() ? Typestate.BooleanType : p.inferTypestate(null);
		Expression e = this.reshapeInner(p.getExpression());
		f.setExpression(e);
		lp.setExpression(e);
		this.requiredTypeState = stackedTypeState;
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
			if (e instanceof Uone) {
				return r;
			}
			if (e instanceof Unot || e instanceof Uzero || e instanceof Uoption || e instanceof Uand) {
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
			return reshapeInner(p.getExpression());
		}

		int innerTypeState = this.isNonASTContext() ? Typestate.BooleanType : p.inferTypestate(null);
		String uname = this.uniqueName(n.getUniqueName(), p);
		ParseFunc f = g.getParserFunc(uname);
		if (f == null) {
			f = checkFirstVisitedProduction(uname, p);
		}
		f.count();

		NonTerminal renamed = g.newNonTerminal(uname);
		if (this.requiredTypeState == Typestate.ObjectType) {
			if (innerTypeState == Typestate.OperationType) {
				// FIXME removed tree construction
			}
		}
		if (this.requiredTypeState == Typestate.OperationType) {
			if (innerTypeState == Typestate.ObjectType) {
				reportInserted(n, "$");
				return ExpressionCommons.newTlink(n.getSourcePosition(), null, renamed);
			}
		}
		return renamed;
	}

	@Override
	public Expression reshapeXon(Xon p) {
		String flagName = p.getFlagName();
		System.out.println("on " + flagName);
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
	public Expression reshapeUmatch(Umatch p) {
		boolean stacked = this.enterNonASTContext();
		Expression inner = this.reshapeInner(p.get(0));
		this.exitNonASTContext(stacked);
		return inner;
	}

	@Override
	public Expression reshapeTnew(Tnew p) {
		if (this.isNonASTContext()) {
			return this.empty(p);
		}
		if (p.leftFold) {
			if (this.requiredTypeState != Typestate.OperationType) {
				this.reportRemoved(p, "{$");
				return empty(p);
			}
		} else {
			if (this.requiredTypeState != Typestate.ObjectType) {
				this.reportRemoved(p, "{");
				return empty(p);
			}
		}
		this.requiredTypeState = Typestate.OperationType;
		return super.reshapeTnew(p);
	}

	@Override
	public Expression reshapeTcapture(Tcapture p) {
		if (this.isNonASTContext()) {
			return this.empty(p);
		}
		if (this.requiredTypeState != Typestate.OperationType) {
			this.reportRemoved(p, "}");
			return empty(p);
		}
		this.requiredTypeState = Typestate.OperationType;
		return super.reshapeTcapture(p);
	}

	@Override
	public Expression reshapeTtag(Ttag p) {
		if (this.isNonASTContext()) {
			return this.empty(p);
		}
		if (this.requiredTypeState != Typestate.OperationType) {
			reportRemoved(p, "#" + p.tag.getSymbol());
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapeTreplace(Treplace p) {
		if (this.isNonASTContext()) {
			return this.empty(p);
		}
		if (this.requiredTypeState != Typestate.OperationType) {
			reportRemoved(p, "`" + p.value + "`");
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapeTlink(Tlink p) {
		Expression inner = p.get(0);
		if (this.isNonASTContext()) {
			return this.reshapeInner(inner);
		}
		if (this.requiredTypeState != Typestate.OperationType) {
			reportRemoved(p, "$");
			return this.reshapeInner(inner);
		}

		int innerTypeState = this.isNonASTContext() ? Typestate.BooleanType : inner.inferTypestate(null);
		if (innerTypeState != Typestate.ObjectType) {
			reportInserted(p, "{");
			inner = ExpressionCommons.newNewCapture(inner.getSourcePosition(), this.reshapeInner(inner));
		} else {
			this.requiredTypeState = Typestate.ObjectType;
			inner = this.reshapeInner(p.get(0));
		}
		this.requiredTypeState = Typestate.OperationType;
		return ExpressionCommons.newTlink(p.getSourcePosition(), p.getLabel(), inner);
	}

	@Override
	public Expression reshapePchoice(Pchoice p) {
		int required = this.requiredTypeState;
		int next = this.requiredTypeState;
		UList<Expression> l = ExpressionCommons.newList(p.size());
		for (Expression inner : p) {
			this.requiredTypeState = required;
			ExpressionCommons.addChoice(l, this.reshapeInner(inner));
			if (this.requiredTypeState != required && this.requiredTypeState != next) {
				next = this.requiredTypeState;
			}
		}
		this.requiredTypeState = next;
		return ExpressionCommons.newPchoice(p.getSourcePosition(), l);
	}

	@Override
	public Expression reshapeUzero(Uzero p) {
		return ExpressionCommons.newUzero(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	@Override
	public Expression reshapeUone(Uone p) {
		return ExpressionCommons.newUone(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	@Override
	public Expression reshapeUoption(Uoption p) {
		return ExpressionCommons.newUoption(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	private Expression reshapeOptionalInner(Unary p) {
		int innerTypeState = this.isNonASTContext() ? Typestate.BooleanType : p.get(0).inferTypestate(null);
		if (innerTypeState == Typestate.ObjectType) {
			if (this.requiredTypeState == Typestate.OperationType) {
				this.reportInserted(p.get(0), "$");
				Expression inner = reshapeInner(p.get(0));
				return ExpressionCommons.newTlink(p.getSourcePosition(), inner);
			} else {
				reportWarning(p, "disallowed tree construction in e?, e*, e+, or &e  " + innerTypeState);
				boolean stacked = this.enterNonASTContext();
				Expression inner = reshapeInner(p.get(0));
				this.exitNonASTContext(stacked);
				return inner;
			}
		}
		return reshapeInner(p.get(0));
	}

	@Override
	public Expression reshapeUand(Uand p) {
		return ExpressionCommons.newUand(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	@Override
	public Expression reshapeUnot(Unot p) {
		Expression inner = p.get(0);
		int innerTypeState = this.isNonASTContext() ? Typestate.BooleanType : inner.inferTypestate(null);
		if (innerTypeState != Typestate.BooleanType) {
			reportWarning(p, "disallowed tree construction in !e");
			boolean stacked = this.enterNonASTContext();
			inner = this.reshapeInner(inner);
			this.exitNonASTContext(stacked);
		} else {
			inner = this.reshapeInner(inner);
		}
		return ExpressionCommons.newUnot(p.getSourcePosition(), inner);
	}

	@Override
	public Expression reshapeXdef(Xdef p) {
		Expression inner = p.get(0);
		int t = inner.inferTypestate(null);
		if (t != Typestate.BooleanType) {
			boolean stacked = this.enterNonASTContext();
			inner = this.reshapeInner(inner);
			this.exitNonASTContext(stacked);
		} else {
			inner = this.reshapeInner(inner);
		}
		g.setSymbolExpresion(p.getTableName(), inner);
		return ExpressionCommons.newXdef(p.getSourcePosition(), g, p.getTable(), inner);
	}

	@Override
	public Expression reshapeXis(Xis p) {
		Expression e = p.getSymbolExpression();
		if (e == null) {
			reportError(p, "undefined table: " + p.getTableName());
			return ExpressionCommons.newFailure(p.getSourcePosition());
		}
		return ExpressionCommons.newXis(p.getSourcePosition(), g, p.getTable(), p.is);
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
		System.out.println("unique: " + uname + ", " + this.boolMap.keySet() + "=>" + sb.toString());
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
		this.repo.reportError(p.getSourcePosition(), message);
	}

	public final void reportWarning(Expression p, String message) {
		this.repo.reportWarning(p.getSourcePosition(), message);
	}

	public final void reportNotice(Expression p, String message) {
		this.repo.reportNotice(p.getSourcePosition(), message);
	}

}