package nez.lang;

import java.util.HashMap;
import java.util.TreeMap;

import nez.NezOption;
import nez.ast.Reporter;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
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
	private int requiredTypestate;
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
		int stackedTypestate = this.requiredTypestate;
		this.requiredTypestate = this.isNonASTContext() ? Typestate.BooleanType : p.inferTypestate(null);
		Expression e = this.reshapeInner(p.getExpression());
		f.setExpression(e);
		lp.setExpression(e);
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

		int innerTypestate = this.isNonASTContext() ? Typestate.BooleanType : p.inferTypestate(null);
		String uname = this.uniqueName(n.getUniqueName(), p);
		ParseFunc f = g.getParserFunc(uname);
		if (f == null) {
			f = checkFirstVisitedProduction(uname, p);
		}
		f.count();
		if (innerTypestate == Typestate.BooleanType) {
			return g.newNonTerminal(uname);
		}
		int required = this.requiredTypestate;
		if (required == Typestate.ObjectType) {
			if (innerTypestate == Typestate.OperationType) {
				reportInserted(n, "{");
				this.requiredTypestate = Typestate.OperationType;
				return ExpressionCommons.newNewCapture(n.getSourcePosition(), g.newNonTerminal(uname));
			}
			this.requiredTypestate = Typestate.OperationType;
			return g.newNonTerminal(uname);
		}
		if (required == Typestate.OperationType) {
			if (innerTypestate == Typestate.ObjectType) {
				reportInserted(n, "$");
				this.requiredTypestate = Typestate.ObjectType;
				return ExpressionCommons.newTlink(n.getSourcePosition(), null, g.newNonTerminal(uname));
			}
			// return g.newNonTerminal(uname);
		}
		return g.newNonTerminal(uname);
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
			if (this.requiredTypestate != Typestate.OperationType) {
				this.reportRemoved(p, "{$");
				return empty(p);
			}
		} else {
			if (this.requiredTypestate != Typestate.ObjectType) {
				this.reportRemoved(p, "{");
				return empty(p);
			}
		}
		this.requiredTypestate = Typestate.OperationType;
		return super.reshapeTnew(p);
	}

	@Override
	public Expression reshapeTcapture(Tcapture p) {
		if (this.isNonASTContext()) {
			return this.empty(p);
		}
		if (this.requiredTypestate != Typestate.OperationType) {
			this.reportRemoved(p, "}");
			return empty(p);
		}
		this.requiredTypestate = Typestate.OperationType;
		return super.reshapeTcapture(p);
	}

	@Override
	public Expression reshapeTtag(Ttag p) {
		if (this.isNonASTContext()) {
			return this.empty(p);
		}
		if (this.requiredTypestate != Typestate.OperationType) {
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
		if (this.requiredTypestate != Typestate.OperationType) {
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
	public Expression reshapeUand(Uand p) {
		return ExpressionCommons.newUand(p.getSourcePosition(), reshapeOptionalInner(p));
	}

	@Override
	public Expression reshapeUnot(Unot p) {
		Expression inner = p.get(0);
		int innerTypestate = this.isNonASTContext() ? Typestate.BooleanType : inner.inferTypestate(null);
		if (innerTypestate != Typestate.BooleanType) {
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

class GrammarOptimizer2 extends GrammarRewriter {
	/* local optimizer option */
	boolean enabledCommonLeftFactoring = true; // true;
	boolean enabledCostBasedReduction = true;
	boolean enabledOutOfOrder = false; // bugs!!

	NezOption option;
	HashMap<String, String> optimizedMap = new HashMap<String, String>();

	public GrammarOptimizer2(NezOption option) {
		this.option = option;
		if (option.enabledPrediction) {
			// seems slow when the prediction option is enabled
			this.enabledCommonLeftFactoring = false;
		}
	}

	public final Expression optimize(Production p) {
		String uname = p.getUniqueName();
		if (!optimizedMap.containsKey(uname)) {
			optimizedMap.put(uname, uname);
			Expression optimized = resolveNonTerminal(p.getExpression()).reshape(this);
			p.setExpression(optimized);
			return optimized;
		}
		return p.getExpression();
	}

	private void rewrite_outoforder(Expression e, Expression e2) {
		// Verbose.debug("out-of-order " + e + " <==> " + e2);
	}

	private void rewrite(String msg, Expression e, Expression e2) {
		// Verbose.debug(msg + " " + e + "\n\t=>" + e2);
	}

	private void rewrite_common(Expression e, Expression e2, Expression e3) {
		// Verbose.debug("common (" + e + " / " + e2 + ")\n\t=>" + e3);
	}

	// used to test inlining
	public final static boolean isSingleCharacter(Expression e) {
		if (e instanceof Cset || e instanceof Cbyte || e instanceof Cany) {
			return true;
		}
		return false;
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal n) {
		Production p = n.getProduction();
		if (p.isRecursive()) {
			return n;
		}
		Expression optimized = this.optimize(p);
		if (option.enabledInlining && p.isInline()) {
			rewrite("inline", n, optimized);
			return optimized;
		}
		Expression deref = resolveNonTerminal(optimized).reshape(this);
		if (isSingleCharacter(deref)) {
			rewrite("deref", n, deref);
			return deref;
		}
		if (deref instanceof Pempty || deref instanceof Pfail) {
			rewrite("deref", n, deref);
			return deref;
		}
		return n;
	}

	private boolean isOutOfOrderExpression(Expression e) {
		if (e instanceof Ttag) {
			return true;
		}
		if (e instanceof Treplace) {
			return true;
		}
		if (e instanceof Tnew) {
			((Tnew) e).shift -= 1;
			return true;
		}
		if (e instanceof Tcapture) {
			((Tcapture) e).shift -= 1;
			return true;
		}
		return false;
	}

	@Override
	public Expression reshapePsequence(Psequence p) {
		Expression first = p.getFirst().reshape(this);
		Expression next = p.getNext().reshape(this);
		if (this.enabledOutOfOrder) {
			if (next instanceof Psequence) {
				Psequence nextSequence = (Psequence) next;
				if (isSingleCharacter(nextSequence.first) && isOutOfOrderExpression(first)) {
					rewrite_outoforder(first, nextSequence.first);
					Expression temp = nextSequence.first;
					nextSequence.first = first;
					first = temp;
				}
			} else {
				if (isSingleCharacter(next) && isOutOfOrderExpression(first)) {
					rewrite_outoforder(first, next);
					Expression temp = first;
					first = next;
					next = temp;
				}
			}
		}
		if (isNotChar(first)) {
			Expression optimized = convertBitMap(next, first.get(0));
			if (optimized != null) {
				rewrite("not-merge", p, optimized);
				return optimized;
			}
		}
		return p.newSequence(first, next);
	}

	private boolean isNotChar(Expression p) {
		if (p instanceof Unot) {
			return (p.get(0) instanceof Cset || p.get(0) instanceof Cbyte);
		}
		return false;
	}

	private Expression convertBitMap(Expression next, Expression not) {
		boolean[] bany = null;
		boolean isBinary = false;
		Expression nextNext = next.getNext();
		if (nextNext != null) {
			next = next.getFirst();
		}
		if (next instanceof Cany) {
			Cany any = (Cany) next;
			isBinary = any.isBinary();
			bany = Cset.newMap(true);
			if (isBinary) {
				bany[0] = false;
			}
		}
		if (next instanceof Cset) {
			Cset bm = (Cset) next;
			isBinary = bm.isBinary();
			bany = bm.byteMap.clone();
		}
		if (next instanceof Cbyte) {
			Cbyte bc = (Cbyte) next;
			isBinary = bc.isBinary();
			bany = Cset.newMap(false);
			if (isBinary) {
				bany[0] = false;
			}
			bany[bc.byteChar] = true;
		}
		if (bany == null) {
			return null;
		}
		if (not instanceof Cset) {
			Cset bm = (Cset) not;
			for (int c = 0; c < bany.length - 1; c++) {
				if (bm.byteMap[c] && bany[c] == true) {
					bany[c] = false;
				}
			}
		}
		if (not instanceof Cbyte) {
			Cbyte bc = (Cbyte) not;
			if (bany[bc.byteChar] == true) {
				bany[bc.byteChar] = false;
			}
		}
		Expression e = not.newByteMap(isBinary, bany);
		if (nextNext != null) {
			return not.newSequence(e, nextNext);
		}
		return e;
	}

	@Override
	public Expression reshapeTlink(Tlink p) {
		if (p.get(0) instanceof Pchoice) {
			Expression inner = p.get(0);
			UList<Expression> l = new UList<Expression>(new Expression[inner.size()]);
			for (Expression subChoice : inner) {
				subChoice = subChoice.reshape(this);
				l.add(ExpressionCommons.newTlink(p.getSourcePosition(), p.getLabel(), subChoice));
			}
			return inner.newChoice(l);
		}
		return super.reshapeTlink(p);
	}

	@Override
	public Expression reshapePchoice(Pchoice p) {
		if (!p.isFlatten) {
			p.isFlatten = true;
			UList<Expression> choiceList = new UList<Expression>(new Expression[p.size()]);
			flattenChoiceList(p, choiceList);
			Expression optimized = convertByteMap(p, choiceList);
			if (optimized != null) {
				rewrite("choice-map", p, optimized);
				return optimized;
			}
			boolean isFlatten = p.size() != choiceList.size();
			for (int i = 0; i < choiceList.size(); i++) {
				Expression sub = choiceList.ArrayValues[i];
				if (!isFlatten) {
					if (sub.equalsExpression(p.get(i))) {
						continue;
					}
				}
				choiceList.ArrayValues[i] = sub.reshape(this);
			}
			if (choiceList.size() == 1) {
				rewrite("choice-single", p, choiceList.ArrayValues[0]);
				return choiceList.ArrayValues[0];
			}
			if (option.enabledPrediction) {
				int count = 0;
				int selected = 0;
				p.predictedCase = new Expression[257];
				Expression singleChoice = null;
				for (int ch = 0; ch <= 255; ch++) {
					Expression predicted = selectChoice(p, choiceList, ch);
					p.predictedCase[ch] = predicted;
					if (predicted != null) {
						singleChoice = predicted;
						count++;
						if (predicted instanceof Pchoice) {
							selected += predicted.size();
						} else {
							selected += 1;
						}
					}
				}
				double reduced = (double) selected / count;
				// Verbose.debug("reduced: " + choiceList.size() + " => " +
				// reduced);
				if (count == 1 && singleChoice != null) {
					rewrite("choice-single", p, singleChoice);
					return singleChoice;
				}
				if (this.enabledCostBasedReduction && reduced / choiceList.size() > 0.55) {
					p.predictedCase = null;
				}
			}
			if (!isFlatten) {
				return p;
			}
			Expression c = p.newChoice(choiceList);
			if (c instanceof Pchoice) {
				((Pchoice) c).isFlatten = true;
				((Pchoice) c).predictedCase = p.predictedCase;
			}
			// rewrite("flatten", p, c);
			return c;
		}
		return p;
	}

	private void flattenChoiceList(Pchoice parentExpression, UList<Expression> l) {
		for (Expression subExpression : parentExpression) {
			subExpression = resolveNonTerminal(subExpression);
			if (subExpression instanceof Pchoice) {
				flattenChoiceList((Pchoice) subExpression, l);
			} else {
				subExpression = subExpression.reshape(this);
				if (l.size() > 0 && this.enabledCommonLeftFactoring) {
					Expression lastExpression = l.ArrayValues[l.size() - 1];
					Expression first = lastExpression.getFirst();
					if (first.equalsExpression(subExpression.getFirst())) {
						Expression next = lastExpression.newChoice(lastExpression.getNext(), subExpression.getNext());
						Expression common = lastExpression.newSequence(first, next);
						rewrite_common(lastExpression, subExpression, common);
						l.ArrayValues[l.size() - 1] = common;
						continue;
					}
				}
				l.add(subExpression);
			}
		}
	}

	public final static Expression resolveNonTerminal(Expression e) {
		while (e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	// OptimizerLibrary

	private Expression convertByteMap(Pchoice choice, UList<Expression> choiceList) {
		boolean byteMap[] = Cset.newMap(false);
		boolean binary = false;
		for (Expression e : choiceList) {
			if (e instanceof Pfail) {
				continue;
			}
			if (e instanceof Cbyte) {
				byteMap[((Cbyte) e).byteChar] = true;
				if (((Cbyte) e).isBinary()) {
					binary = true;
				}
				continue;
			}
			if (e instanceof Cset) {
				Cset.appendBitMap(byteMap, ((Cset) e).byteMap);
				if (((Cset) e).isBinary()) {
					binary = true;
				}
				continue;
			}
			if (e instanceof Cany) {
				return e;
			}
			if (e instanceof Pempty) {
				break;
			}
			return null;
		}
		return choice.newByteMap(binary, byteMap);
	}

	private Expression selectChoice(Pchoice choice, UList<Expression> choiceList, int ch) {
		Expression first = null;
		UList<Expression> newChoiceList = null;
		boolean commonPrifixed = false;
		for (Expression p : choiceList) {
			short r = p.acceptByte(ch);
			if (r == PossibleAcceptance.Reject) {
				continue;
			}
			if (first == null) {
				first = p;
				continue;
			}
			if (newChoiceList == null) {
				Expression common = tryCommonFactoring(choice, first, p, true);
				if (common != null) {
					first = common;
					commonPrifixed = true;
					continue;
				}
				newChoiceList = new UList<Expression>(new Expression[2]);
				newChoiceList.add(first);
				newChoiceList.add(p);
			} else {
				Expression last = newChoiceList.ArrayValues[newChoiceList.size() - 1];
				Expression common = tryCommonFactoring(choice, last, p, true);
				if (common != null) {
					newChoiceList.ArrayValues[newChoiceList.size() - 1] = common;
					continue;
				}
				newChoiceList.add(p);
			}
		}
		if (newChoiceList != null) {
			return ExpressionCommons.newPchoice(choice.getSourcePosition(), newChoiceList);
		}
		return commonPrifixed == true ? first.reshape(this) : first;
	}

	public final static Expression tryCommonFactoring(Pchoice base, Expression e, Expression e2, boolean ignoredFirstChar) {
		UList<Expression> l = null;
		while (e != null && e2 != null) {
			Expression f = e.getFirst();
			Expression f2 = e2.getFirst();
			if (ignoredFirstChar) {
				ignoredFirstChar = false;
				if (Expression.isByteConsumed(f) && Expression.isByteConsumed(f2)) {
					l = ExpressionCommons.newList(4);
					l.add(f);
					e = e.getNext();
					e2 = e2.getNext();
					continue;
				}
				return null;
			}
			if (!f.equalsExpression(f2)) {
				break;
			}
			if (l == null) {
				l = ExpressionCommons.newList(4);
			}
			l.add(f);
			e = e.getNext();
			e2 = e2.getNext();
			// System.out.println("l="+l.size()+",e="+e);
		}
		if (l == null) {
			return null;
		}
		if (e == null) {
			e = base.newEmpty();
		}
		if (e2 == null) {
			e2 = base.newEmpty();
		}
		Expression alt = base.newChoice(e, e2);
		l.add(alt);
		return base.newSequence(l);
	}

}