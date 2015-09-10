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

public class GrammarChecker extends ExpressionTransducer {
	ParserGrammar g;
	boolean offAST = true;
	private int required = Typestate.BooleanType;
	TreeMap<String, Boolean> ctx;
	Reporter repo = new Reporter();

	public GrammarChecker(ParserGrammar g, boolean offAST, TreeMap<String, Boolean> ctx, Production start) {
		this.g = g;
		this.offAST = offAST;
		this.ctx = ctx == null ? new TreeMap<String, Boolean>() : ctx;
		String uname = uniqueName(start.getUniqueName(), start);
		this.checkFirstVisitedProduction(uname, start);
	}

	private ParseFunc checkFirstVisitedProduction(String uname, Production p) {
		Production lp/* local production */= g.newProduction(uname, null);
		g.addProduction(lp);
		ParseFunc f = new ParseFunc(uname, p);
		g.setParserFunc(f);
		if (UFlag.is(p.flag, Production.ResetFlag)) {
			p.initFlag();
			if (p.isRecursive()) {
				checkLeftRecursion(p.getExpression(), new ProductionStacker(p, null));
			}
			p.isNoNTreeConstruction();
		}
		Expression e = p.getExpression();
		if (offAST) {
			e = p.reshape(ExpressionTransducer.RemoveAST);
		} else {
			this.required = p.inferTypestate();
		}
		e = e.reshape(this);
		f.setExpression(e);
		lp.setExpression(e);
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
			Expression e = p.getExpression();
			if (offAST) {
				e = p.reshape(ExpressionTransducer.RemoveAST);
			}
			return e.reshape(this);
		}
		String uname = this.uniqueName(n.getUniqueName(), p);
		ParseFunc f = g.getParserFunc(uname);
		if (f == null) {
			f = checkFirstVisitedProduction(uname, p);
		}
		f.count();

		int t = p.inferTypestate();
		if (t == Typestate.BooleanType) {
			return p;
		}
		if (this.required == Typestate.ObjectType) {
			if (t == Typestate.OperationType) {
				reportRemoved(p, "AST operations");
				return p.reshape(ExpressionTransducer.RemoveASTandRename);
			}
			this.required = Typestate.OperationType;
			return p;
		}
		if (this.required == Typestate.OperationType) {
			if (t == Typestate.ObjectType) {
				reportInserted(p, ":");
				return ExpressionCommons.newTlink(p.getSourcePosition(), null, p);
			}
		}
		return g.newNonTerminal(uname);
	}

	@Override
	public Expression reshapeXon(Xon p) {
		String flagName = p.getFlagName();
		Boolean bool = ctx.get(flagName);
		if (bool != null) {
			if (p.isPositive()) {
				if (!bool) {
					ctx.put(flagName, true);
					Expression newe = p.get(0).reshape(this);
					ctx.put(flagName, false);
					return newe;
				}
			} else {
				if (bool) {
					ctx.put(flagName, false);
					Expression newe = p.get(0).reshape(this);
					ctx.put(flagName, true);
					return newe;
				}
			}
		}
		// unnecessary on
		return p.get(0).reshape(this);
	}

	@Override
	public Expression reshapeXif(Xif p) {
		String flagName = p.getFlagName();
		if (ctx.get(flagName)) {
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
	public Expression reshapeTnew(Tnew p) {
		if (p.leftFold) {
			if (this.required != Typestate.OperationType) {
				this.reportRemoved(p, "{@");
				return p.reshape(ExpressionTransducer.RemoveASTandRename);
			}
		} else {
			if (this.required != Typestate.ObjectType) {
				this.reportRemoved(p, "{");
				return empty(p);
			}
		}
		this.required = Typestate.OperationType;
		return p;
	}

	@Override
	public Expression reshapeTlink(Tlink p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "@");
			p.inner = p.inner.reshape(ExpressionTransducer.RemoveASTandRename);
		}
		this.required = Typestate.ObjectType;
		Expression inn = p.inner.reshape(this);
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "@");
			this.required = Typestate.OperationType;
			return updateInner(p, inn);
		}
		this.required = Typestate.OperationType;
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeUmatch(Umatch p) {
		return p.inner.reshape(ExpressionTransducer.RemoveASTandRename);
	}

	@Override
	public Expression reshapeTtag(Ttag p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "#" + p.tag.getSymbol());
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapeTreplace(Treplace p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "`" + p.value + "`");
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapeCapture(Tcapture p) {
		if (this.required != Typestate.OperationType) {
			reportRemoved(p, "}");
			return empty(p);
		}
		return p;
	}

	@Override
	public Expression reshapePchoice(Pchoice p) {
		int required = this.required;
		int next = this.required;
		UList<Expression> l = ExpressionCommons.newList(p.size());
		for (Expression e : p) {
			this.required = required;
			ExpressionCommons.addChoice(l, e.reshape(this));
			if (this.required != required && this.required != next) {
				next = this.required;
			}
		}
		this.required = next;
		return ExpressionCommons.newPchoice(p.getSourcePosition(), l);
	}

	@Override
	public Expression reshapeUzero(Uzero p) {
		int required = this.required;
		Expression inn = p.inner.reshape(this);
		if (required != Typestate.OperationType && this.required == Typestate.OperationType) {
			reportWarning(p, "unable to create objects in repetition => removed!!");
			inn = inn.reshape(ExpressionTransducer.RemoveASTandRename);
			this.required = required;
		}
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeUone(Uone p) {
		int required = this.required;
		Expression inn = p.inner.reshape(this);
		if (required != Typestate.OperationType && this.required == Typestate.OperationType) {
			reportWarning(p, "unable to create objects in repetition => removed!!");
			inn = inn.reshape(ExpressionTransducer.RemoveASTandRename);
			this.required = required;
		}
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeUoption(Uoption p) {
		int required = this.required;
		Expression inn = p.inner.reshape(this);
		if (required != Typestate.OperationType && this.required == Typestate.OperationType) {
			reportWarning(p, "unable to create objects in repetition => removed!!");
			inn = inn.reshape(ExpressionTransducer.RemoveASTandRename);
			this.required = required;
		}
		return updateInner(p, inn);
	}

	@Override
	public Expression reshapeUand(Uand p) {
		if (this.required == Typestate.ObjectType) {
			this.required = Typestate.BooleanType;
			Expression inn = p.inner.reshape(this);
			this.required = Typestate.ObjectType;
			return updateInner(p, inn);
		}
		return updateInner(p, p.inner.reshape(this));
	}

	@Override
	public Expression reshapeUnot(Unot p) {
		int t = p.inner.inferTypestate(null);
		if (t == Typestate.ObjectType || t == Typestate.OperationType) {
			updateInner(p, p.inner.reshape(ExpressionTransducer.RemoveASTandRename));
		}
		return p;
	}

	@Override
	public Expression reshapeXdef(Xdef p) {
		int t = p.inner.inferTypestate(null);
		if (t != Typestate.BooleanType) {
			updateInner(p, p.inner.reshape(ExpressionTransducer.RemoveASTandRename));
		}
		return p;
	}

	@Override
	public Expression reshapeXis(Xis p) {
		Expression e = p.getSymbolExpression();
		if (e == null) {
			reportError(p, "undefined table: " + p.getTableName());
			return ExpressionCommons.newFailure(p.getSourcePosition());
		}
		return p;
	}

	/* uniquename */

	String uniqueName(String uname, Production p) {
		StringBuilder sb = new StringBuilder();
		sb.append(uname);
		for (String flagName : this.ctx.keySet()) {
			if (hasProductionFlag(p, flagName)) {
				sb.append("!");
				sb.append(flagName);
			}
		}
		return sb.toString();
	}

	final static Short True = 1;
	final static Short False = -1;
	final static Short Unknown = 0;
	HashMap<String, Short> flagMap = new HashMap<String, Short>();

	private boolean hasProductionFlag(Production p, String flagName) {
		if (flagMap == null) {
			this.flagMap = new HashMap<String, Short>();
		}
		return (checkProduction(p, flagName) != False);
	}

	private Short checkProduction(Production p, String flagName) {
		String key = p.getUniqueName() + "+" + flagName;
		Short res = flagMap.get(key);
		if (res == null) {
			flagMap.put(key, Unknown);
			res = hasFlag(p.getExpression(), flagName);
			flagMap.put(key, res == Unknown ? False : res);
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
			return checkProduction(p, flagName);
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