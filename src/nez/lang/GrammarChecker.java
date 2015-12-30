package nez.lang;

import nez.lang.expr.Expressions;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.Verbose;

public class GrammarChecker extends GrammarTransformer {

	// ParserGrammar parserGrammar;
	private GrammarContext context;
	private Conditions conds;

	Grammar g;
	private boolean ConstructingTree = true;
	private Typestate requiredTypestate;

	public GrammarChecker(GrammarContext context) {
		this.context = context;
	}

	public final Grammar transform() {
		this.g = context.newGrammar();
		this.conds = this.context.newConditions();
		if (context.getParserStrategy().TreeConstruction) {
			this.ConstructingTree = true;
			this.requiredTypestate = Typestate.Tree;
		} else {
			this.ConstructingTree = false;
			this.requiredTypestate = Typestate.Unit;
			this.enterNonASTContext();
		}
		String cname = conds.conditionalName(context.getStartProduction(), isNonTreeConstruction());
		checkFirstVisitedProduction(cname, context.getStartProduction());
		return this.g;
	}

	/* ASTContext */

	private boolean enterNonASTContext() {
		boolean b = ConstructingTree;
		ConstructingTree = false;
		return b;
	}

	private void exitNonASTContext(boolean backed) {
		ConstructingTree = backed;
	}

	private boolean isNonTreeConstruction() {
		return !this.ConstructingTree;
	}

	/* Conditional */

	private void onFlag(String flag) {
		this.conds.put(flag, true);
	}

	private void offFlag(String flag) {
		this.conds.put(flag, false);
	}

	private boolean isFlag(String flag) {
		return this.conds.get(flag);
	}

	private void reportInserted(Expression e, String operator) {
		context.reportWarning(e, "expected " + operator + " .. => inserted!!");
	}

	private void reportRemoved(Expression e, String operator) {
		context.reportWarning(e, "unexpected " + operator + " .. => removed!!");
	}

	private Expression check(Expression e) {
		return (Expression) e.visit(this, null);
	}

	private Production checkFirstVisitedProduction(String cname, Production p) {
		this.visited(cname);
		Production gp = this.g.newProduction(cname, null);
		// Production parserProduction/* local production */=
		// parserGrammar.newProduction(uname, null);
		// ParseFunc f = parserGrammar.setParseFunc(uname, p, parserProduction,
		// init);
		// if (UFlag.is(p.flag, Production.ResetFlag)) {
		// p.initFlag();
		// if (p.isRecursive()) {
		// checkLeftRecursion(p.getExpression(), new ProductionStacker(p,
		// null));
		// }
		// // p.isNoNTreeConstruction();
		// }
		Typestate stackedTypestate = this.requiredTypestate;
		this.requiredTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(p);
		gp.setExpression(check(p.getExpression()));
		this.requiredTypestate = stackedTypestate;
		return gp;
	}

	// boolean checkLeftRecursion(Expression e, ProductionStacker s) {
	// if (e instanceof NonTerminal) {
	// Production p = ((NonTerminal) e).getProduction();
	// if (s.isVisited(p)) {
	// context.reportError(e, "left recursion: " + p.getLocalName());
	// return true; // stop as consumed
	// }
	// return checkLeftRecursion(p.getExpression(), new ProductionStacker(p,
	// s));
	// }
	// if (e.size() > 0) {
	// if (e instanceof Psequence) {
	// if (!checkLeftRecursion(e.get(0), s)) {
	// return checkLeftRecursion(e.get(1), s);
	// }
	// }
	// if (e instanceof Pchoice) {
	// boolean consumed = true;
	// for (Expression se : e) {
	// if (!checkLeftRecursion(e.get(1), s)) {
	// consumed = false;
	// }
	// }
	// return consumed;
	// }
	// boolean r = checkLeftRecursion(e.get(0), s);
	// if (e instanceof Pone) {
	// return r;
	// }
	// if (e instanceof Pnot || e instanceof Pzero || e instanceof Poption || e
	// instanceof Pand) {
	// return false;
	// }
	// return r;
	// }
	// return this.parserGrammar.isConsumed(e);
	// }

	@Override
	public Expression visitNonTerminal(NonTerminal n, Object a) {
		Production p = n.getProduction();
		if (p == null) {
			if (n.isTerminal()) {
				context.reportNotice(n, "undefined terminal: " + n.getLocalName());
				return Expressions.newMultiByte(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
			}
			context.reportWarning(n, "undefined production: " + n.getLocalName());
			return n.newEmpty();
		}
		if (n.isTerminal()) { /* Inlining Terminal */
			try {
				return check(p.getExpression());
			} catch (StackOverflowError e) {
				/* Handling a bad grammar */
				context.reportError(n, "terminal is recursive: " + n.getLocalName());
				return Expressions.newMultiByte(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
			}
		}

		Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(p);
		if (this.requiredTypestate == Typestate.TreeMutation) {
			if (innerTypestate == Typestate.TreeMutation) {
				Verbose.println("inlining mutation nonterminal" + p.getLocalName());
				return check(p.getExpression());
			}
		}

		String cname = conds.conditionalName(p, innerTypestate == Typestate.Unit);
		if (this.isVisited(cname)) {
			this.checkFirstVisitedProduction(cname, p);
		}

		NonTerminal pn = g.newNonTerminal(n.getSourceLocation(), cname);
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
		if (!this.conds.containsKey(p.flagName)) {
			this.context.reportWarning(p, "unused condition: " + p.flagName);
			return check(p.get(0));
		}
		if (!conds.isConditional(p.get(0), p.flagName)) {
			this.context.reportWarning(p, "unreached condition: " + p.flagName);
			return check(p.get(0));
		}
		Boolean stackedFlag = isFlag(p.flagName);
		if (p.isPositive()) {
			onFlag(p.flagName);
		} else {
			offFlag(p.flagName);
		}
		Expression newe = check(p.get(0));
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

	@Override
	public Expression visitDetree(Nez.Detree p, Object a) {
		boolean stacked = this.enterNonASTContext();
		Expression inner = check(p.get(0));
		this.exitNonASTContext(stacked);
		return inner;
	}

	@Override
	public Expression visitPreNew(Nez.PreNew p, Object a) {
		if (this.isNonTreeConstruction()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.Tree) {
			this.reportRemoved(p, "{");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitPreNew(p, a);
	}

	@Override
	public Expression visitLeftFold(Nez.LeftFold p, Object a) {
		if (this.isNonTreeConstruction()) {
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
	public Expression visitNew(Nez.New p, Object a) {
		if (this.isNonTreeConstruction()) {
			return p.newEmpty();
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			this.reportRemoved(p, "}");
			return p.newEmpty();
		}
		this.requiredTypestate = Typestate.TreeMutation;
		return super.visitNew(p, a);
	}

	@Override
	public Expression visitTag(Nez.Tag p, Object a) {
		if (this.isNonTreeConstruction()) {
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
		if (this.isNonTreeConstruction()) {
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
		if (this.isNonTreeConstruction()) {
			return this.check(inner);
		}
		if (this.requiredTypestate != Typestate.TreeMutation) {
			reportRemoved(p, "$");
			return this.check(inner);
		}
		Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(inner);
		if (innerTypestate != Typestate.Tree) {
			reportInserted(p, "{");
			inner = Expressions.newTree(inner.getSourceLocation(), this.check(inner));
		} else {
			this.requiredTypestate = Typestate.Tree;
			inner = this.check(p.get(0));
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
			Expressions.addChoice(l, this.check(inner));
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
		Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(p.get(0));
		if (innerTypestate == Typestate.Tree) {
			if (this.requiredTypestate == Typestate.TreeMutation) {
				reportInserted(p.get(0), "$");
				this.requiredTypestate = Typestate.Tree;
				Expression inner = check(p.get(0));
				inner = Expressions.newLinkTree(p.getSourceLocation(), inner);
				this.requiredTypestate = Typestate.TreeMutation;
				return inner;
			} else {
				context.reportWarning(p, "disallowed tree construction in e?, e*, e+, or &e  " + innerTypestate);
				boolean stacked = this.enterNonASTContext();
				Expression inner = check(p.get(0));
				this.exitNonASTContext(stacked);
				return inner;
			}
		}
		return check(p.get(0));
	}

	@Override
	public Expression visitAnd(Nez.And p, Object a) {
		return Expressions.newAnd(p.getSourceLocation(), visitOptionalInner(p));
	}

	@Override
	public Expression visitNot(Nez.Not p, Object a) {
		Expression inner = p.get(0);
		Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(inner);
		if (innerTypestate != Typestate.Unit) {
			// context.reportWarning(p, "disallowed tree construction in !e");
			boolean stacked = this.enterNonASTContext();
			inner = this.check(inner);
			this.exitNonASTContext(stacked);
		} else {
			inner = this.check(inner);
		}
		return Expressions.newNot(p.getSourceLocation(), inner);
	}

}
