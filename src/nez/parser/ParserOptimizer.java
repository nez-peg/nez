package nez.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import nez.lang.ByteAcceptance;
import nez.lang.ByteConsumption;
import nez.lang.Bytes;
import nez.lang.Expression;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Productions;
import nez.lang.Productions.NonterminalReference;
import nez.lang.Typestate;
import nez.lang.Typestate.TypestateAnalyzer;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;
import nez.util.Verbose;

public class ParserOptimizer {

	Grammar grammar;
	ParserStrategy strategy;

	public Grammar optimize(Production start, ParserStrategy strategy, TreeMap<String, Boolean> boolMap) {
		this.strategy = strategy;
		this.grammar = new Grammar();

		long t1 = System.nanoTime();
		new CheckerVisitor().check(start, boolMap);
		long t2 = System.nanoTime();
		if (strategy.Optimization) {
			// Verbose.println("optimizing %s ..", strategy);
			new OptimizerVisitor().optimize();
		}
		new NormalizerVisitor().perform();
		long t3 = System.nanoTime();
		Verbose.printElapsedTime("Grammar checking time", t1, t2);
		Verbose.printElapsedTime("Optimization time", t2, t3);
		return grammar;
	}

	/* Consumed */

	private final ByteConsumption consumed = new ByteConsumption();

	public final boolean isConsumed(Production p) {
		return consumed.isConsumed(p);
	}

	public final boolean isConsumed(Expression e) {
		return consumed.isConsumed(e);
	}

	/* Typestate */

	private final TypestateAnalyzer typestate = Typestate.newAnalyzer();

	public final Typestate typeState(Production p) {
		return typestate.inferTypestate(p);
	}

	public final Typestate typeState(Expression e) {
		return typestate.inferTypestate(e);
	}

	// final static Short True = 1;
	// final static Short False = -1;
	// final static Short Unknown = 0;

	@SuppressWarnings("serial")
	static class Conditions extends TreeMap<String, Boolean> {

		public final static Conditions newConditions(Production start, Map<String, Boolean> inits, boolean defaultTrue) {
			Conditions conds = new Conditions();
			Set<String> s = retriveConditionSet(start);
			for (String c : s) {
				if (inits != null && inits.containsKey(c)) {
					conds.put(c, inits.get(c));
				} else {
					conds.put(c, defaultTrue); // FIXME: false
				}
			}
			// System.out.println("new conditions: " + conds);
			return conds;
		}

		public final static Set<String> retriveConditionSet(Production start) {
			TreeSet<String> ts = new TreeSet<>();
			HashMap<String, Boolean> visited = new HashMap<>();
			checkCondition(start.getExpression(), ts, visited);
			return ts;
		}

		private static void checkCondition(Expression e, TreeSet<String> ts, HashMap<String, Boolean> visited) {
			if (e instanceof Nez.IfCondition) {
				ts.add(((Nez.IfCondition) e).flagName);
			}
			if (e instanceof NonTerminal) {
				Production p = ((NonTerminal) e).getProduction();
				if (p != null && !visited.containsKey(p.getUniqueName())) {
					visited.put(p.getUniqueName(), true);
					checkCondition(p.getExpression(), ts, visited);
				}
			}
			for (Expression se : e) {
				checkCondition(se, ts, visited);
			}
		}

		enum Value {
			Reachable, Unreachable, Undecided;
		}

		HashMap<String, Value> reachMap = new HashMap<String, Value>();

		public final boolean isConditional(Production p, String condition) {
			return (hasIfCondition(p, condition) != Value.Unreachable);
		}

		public final boolean isConditional(Expression p, String condition) {
			return (hasIfCondition(p, condition) != Value.Unreachable);
		}

		private Value hasIfCondition(Production p, String condition) {
			if (reachMap == null) {
				this.reachMap = new HashMap<String, Value>();
			}
			String key = p.getUniqueName() + "+" + condition;
			Value res = reachMap.get(key);
			if (res == null) {
				reachMap.put(key, Value.Undecided);
				res = hasIfCondition(p.getExpression(), condition);
				if (res != Value.Undecided) {
					reachMap.put(key, res);
				}
			}
			return res;
		}

		private final Value hasIfCondition(Expression e, String condition) {
			if (e instanceof Nez.IfCondition) {
				return condition.equals(((Nez.IfCondition) e).flagName) ? Value.Reachable : Value.Unreachable;
			}
			if (e instanceof Nez.OnCondition && condition.equals(((Nez.OnCondition) e).flagName)) {
				return Value.Unreachable;
			}
			if (e instanceof NonTerminal) {
				Production p = ((NonTerminal) e).getProduction();
				if (p == null) {
					return Value.Unreachable;
				}
				return hasIfCondition(p, condition);
			}
			boolean hasUndecided = false;
			for (Expression se : e) {
				Value dep = hasIfCondition(se, condition);
				if (dep == Value.Reachable) {
					return Value.Reachable;
				}
				if (dep == Value.Undecided) {
					hasUndecided = true;
				}
			}
			return hasUndecided ? Value.Undecided : Value.Unreachable;
		}

		public String conditionalName(Production p, boolean nonTreeConstruction) {
			StringBuilder sb = new StringBuilder();
			if (nonTreeConstruction) {
				sb.append("~");
			}
			sb.append(p.getUniqueName());
			for (String c : this.keySet()) {
				if (isConditional(p, c)) {
					if (this.get(c)) {
						sb.append("&");
					} else {
						sb.append("!");
					}
					sb.append(c);
				}
			}
			String cname = sb.toString();
			// System.out.println("flags: " + this.keySet());
			// System.out.println(p.getUniqueName() + "=>" + cname);
			return cname;
		}
	}

	class CheckerVisitor extends Expression.DuplicateVisitor {
		CheckerVisitor() {
		}

		Conditions conds;
		boolean ConstructingTree = true;
		Typestate requiredTypestate;

		final boolean enterNoTreeConstruction() {
			boolean b = ConstructingTree;
			ConstructingTree = false;
			return b;
		}

		final void exitNoTreeConstruction(boolean backed) {
			ConstructingTree = backed;
		}

		final boolean isNoTreeConstruction() {
			return !this.ConstructingTree;
		}

		/* Conditional */

		final void onFlag(String flag) {
			this.conds.put(flag, true);
		}

		final void offFlag(String flag) {
			this.conds.put(flag, false);
		}

		final boolean isFlag(String flag) {
			return this.conds.get(flag);
		}

		final void check(Production start, TreeMap<String, Boolean> boolMap) {
			this.conds = Conditions.newConditions(start, boolMap, strategy.DefaultCondition);
			if (!strategy.TreeConstruction) {
				this.enterNoTreeConstruction();
			}
			String uname = conds.conditionalName(start, isNoTreeConstruction());
			// String uname = uniqueName(start.getUniqueName(), start);
			this.checkFirstVisitedProduction(uname, start); // start
		}

		private final Expression visitExpression(Expression e) {
			return (Expression) e.visit(this, null);
		}

		void checkFirstVisitedProduction(String uname, Production p) {
			Production parserProduction/* local production */= grammar.addProduction(uname, null);
			this.visited(uname);
			Productions.checkLeftRecursion(p);
			Typestate stackedTypestate = this.requiredTypestate;
			this.requiredTypestate = this.isNoTreeConstruction() ? Typestate.Unit : typeState(p);
			Expression e = this.visitExpression(p.getExpression());
			if (strategy.Coverage) {
				e = Expressions.newCoverage(p.getUniqueName(), e);
			}
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
					return visitExpression(p.getExpression());
				} catch (StackOverflowError e) {
					/* Handling a bad grammar */
					reportError(n, "terminal is recursive: " + n.getLocalName());
					return Expressions.newExpression(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
				}
			}

			Typestate innerState = this.isNoTreeConstruction() ? Typestate.Unit : typeState(p);

			// if (innerTypestate == Typestate.TreeMutation) {
			// if (this.requiredTypestate == Typestate.TreeMutation) {
			// reportNotice(n, "inlining mutation nonterminal" +
			// p.getLocalName());
			// return visitExpression(p.getExpression());
			// }
			// }

			String uname = conds.conditionalName(p, innerState == Typestate.Unit);
			if (!this.isVisited(uname)) {
				checkFirstVisitedProduction(uname, p);
			}
			Typestate required = this.requiredTypestate;
			NonTerminal pn = Expressions.newNonTerminal(n.getSourceLocation(), grammar, uname);
			if (innerState == Typestate.Unit) {
				return pn;
			}
			if (innerState == Typestate.TreeMutation) {
				if (required == Typestate.Tree || required == Typestate.Immutation) {
					reportWarning(n, "The left tree cannot be mutated");
					return Expressions.newDetree(pn);
				}
				this.requiredTypestate = Typestate.TreeMutation;
				return pn;
			}
			if (innerState == Typestate.Tree) {
				if (required == Typestate.Unit || required == Typestate.Immutation) {
					reportWarning(n, "The left tree cannot be mutated");
					return detree(pn);
				}
				if (required == Typestate.TreeMutation) {
					reportWarning(n, "unlabeled link");
					return Expressions.newLinkTree(n.getSourceLocation(), null, pn);
				}
				this.requiredTypestate = Typestate.Immutation;
			}
			return pn;
		}

		@Override
		public Expression visitDetree(Nez.Detree p, Object a) {
			return detree(p.get(0));
		}

		private Expression detree(Expression inner) {
			if (strategy.Moz || strategy.Detree) {
				boolean stacked = this.enterNoTreeConstruction();
				inner = this.visitExpression(inner);
				this.exitNoTreeConstruction(stacked);
				return inner;
			} else {
				return Expressions.newDetree(inner);
			}
		}

		@Override
		public Expression visitOn(Nez.OnCondition p, Object a) {
			if (!conds.isConditional(p.get(0), p.flagName)) {
				reportWarning(p, "unused condition: " + p.flagName);
				return visitExpression(p.get(0));
			}
			Boolean stackedFlag = isFlag(p.flagName);
			if (p.isPositive()) {
				onFlag(p.flagName);
			} else {
				offFlag(p.flagName);
			}
			Expression newe = visitExpression(p.get(0));
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

		//
		// void reportInserted(Expression e, String operator) {
		// reportWarning(e, "expected " + operator + " .. => inserted!!");
		// }
		//
		// void reportRemoved(Expression e, String operator) {
		// reportWarning(e, "unexpected " + operator + " .. => removed!!");
		// }

		@Override
		public Expression visitBeginTree(Nez.BeginTree p, Object a) {
			if (this.isNoTreeConstruction()) {
				return Expressions.newEmpty();
			}
			if (this.requiredTypestate == Typestate.TreeMutation) {
				if (this.requiredTypestate != null) {
					reportError(p, "The labeled link ${ is required");
					this.requiredTypestate = null;
				}
				return Expressions.newEmpty();
			}
			if (this.requiredTypestate != Typestate.Tree) {
				if (this.requiredTypestate != null) {
					reportError(p, "The tree constrution is unexpected");
					this.requiredTypestate = null;
				}
				return Expressions.newEmpty();
			}
			this.requiredTypestate = Typestate.TreeMutation;
			return super.visitBeginTree(p, a);
		}

		@Override
		public Expression visitEndTree(Nez.EndTree p, Object a) {
			if (this.isNoTreeConstruction()) {
				return Expressions.newEmpty();
			}
			if (this.requiredTypestate != Typestate.TreeMutation) {
				if (this.requiredTypestate != null) {
					reportWarning(p, "The left tree cannot be mutated");
					this.requiredTypestate = null;
				}
				return Expressions.newEmpty();
			}
			this.requiredTypestate = Typestate.Immutation;
			return super.visitEndTree(p, a);
		}

		@Override
		public Expression visitFoldTree(Nez.FoldTree p, Object a) {
			if (this.isNoTreeConstruction()) {
				return Expressions.newEmpty();
			}
			if (this.requiredTypestate != Typestate.Immutation) {
				if (this.requiredTypestate != null) {
					reportWarning(p, "A tree to fold is expected on the left hand");
					this.requiredTypestate = null;
				}
				return Expressions.newEmpty();
			}
			this.requiredTypestate = Typestate.TreeMutation;
			return super.visitFoldTree(p, a);
		}

		@Override
		public Expression visitTag(Nez.Tag p, Object a) {
			if (this.isNoTreeConstruction()) {
				return Expressions.newEmpty();
			}
			if (this.requiredTypestate != Typestate.TreeMutation) {
				if (requiredTypestate != null) {
					reportWarning(p, "The left tree cannot be mutated");
					this.requiredTypestate = null;
				}
				return Expressions.newEmpty();
			}
			return p;
		}

		@Override
		public Expression visitReplace(Nez.Replace p, Object a) {
			if (this.isNoTreeConstruction()) {
				return Expressions.newEmpty();
			}
			if (this.requiredTypestate != Typestate.TreeMutation) {
				if (requiredTypestate != null) {
					reportWarning(p, "The left tree cannot be mutated");
					this.requiredTypestate = null;
				}
				return Expressions.newEmpty();
			}
			return p;
		}

		@Override
		public Expression visitLinkTree(Nez.LinkTree p, Object a) {
			Expression inner = p.get(0);
			if (this.isNoTreeConstruction()) {
				return this.visitExpression(inner);
			}
			if (this.requiredTypestate != Typestate.TreeMutation) {
				if (requiredTypestate != null) {
					reportWarning(p, "The left tree cannot be mutated");
					this.requiredTypestate = null;
				}
				boolean backed = this.enterNoTreeConstruction();
				inner = this.visitExpression(inner);
				this.exitNoTreeConstruction(backed);
				return inner;
			}

			Typestate innerState = this.isNoTreeConstruction() ? Typestate.Unit : typeState(inner);
			if (innerState != Typestate.Tree) {
				reportWarning(p, "Implicit tree construction");
				inner = Expressions.newTree(inner.getSourceLocation(), this.visitExpression(inner));
			} else {
				this.requiredTypestate = Typestate.Tree;
				inner = this.visitExpression(p.get(0));
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
				Expressions.addChoice(l, this.visitExpression(inner));
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
			Typestate innerState = this.isNoTreeConstruction() ? Typestate.Unit : typeState(p.get(0));
			if (innerState == Typestate.Tree) {
				if (this.requiredTypestate == Typestate.TreeMutation) {
					reportWarning(p.get(0), "Implicit subtree construction");
					this.requiredTypestate = Typestate.Tree;
					Expression inner = visitExpression(p.get(0));
					inner = Expressions.newLinkTree(p.getSourceLocation(), inner);
					this.requiredTypestate = Typestate.TreeMutation;
					return checkUnconsumed(p, inner);
				} else {
					reportWarning(p, "disallowed tree construction in e?, e*, e+, or &e  " + innerState);
					boolean stacked = this.enterNoTreeConstruction();
					Expression inner = visitExpression(p.get(0));
					this.exitNoTreeConstruction(stacked);
					return checkUnconsumed(p, inner);
				}
			}
			return checkUnconsumed(p, visitExpression(p.get(0)));
		}

		private Expression checkUnconsumed(Nez.Unary p, Expression inner) {
			if (strategy.PEGCompatible && (p instanceof Nez.OneMore || p instanceof Nez.ZeroMore)) {
				if (consumed.isConsumed(inner)) {
					reportError(p, "unconsumed repetition");
				}
			}
			return inner;
		}

		@Override
		public Expression visitAnd(Nez.And p, Object a) {
			return Expressions.newAnd(p.getSourceLocation(), visitOptionalInner(p));
		}

		@Override
		public Expression visitNot(Nez.Not p, Object a) {
			Expression inner = p.get(0);
			Typestate innerTypestate = this.isNoTreeConstruction() ? Typestate.Unit : typeState(inner);
			if (innerTypestate != Typestate.Unit) {
				// reportWarning(p, "disallowed tree construction in !e");
				boolean stacked = this.enterNoTreeConstruction();
				inner = this.visitExpression(inner);
				this.exitNoTreeConstruction(stacked);
			} else {
				inner = this.visitExpression(inner);
			}
			return Expressions.newNot(p.getSourceLocation(), inner);
		}

	}

	// used to test inlining
	public final static boolean isMultiChar(Expression e) {
		if (e instanceof Nez.Byte || e instanceof Nez.MultiByte || e instanceof Nez.Empty) {
			return true;
		}
		if (e instanceof Nez.Pair) {
			return isMultiChar(e.get(0)) && isMultiChar(e.get(1));
		}
		if (e instanceof Nez.Sequence) {
			for (Expression sub : e) {
				if (!isMultiChar(sub)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	// used to test inlining
	public final static boolean isSingleInstruction(Expression e) {
		if (e instanceof Nez.Not || e instanceof Nez.ZeroMore || e instanceof Nez.Option || e instanceof Nez.OneMore) {
			return isSingleCharacter(e.get(0)) || isMultiChar(e.get(0));
		}
		return false;
	}

	// used to test inlining
	public final static boolean isSingleCharacter(Expression e) {
		if (e instanceof Nez.ByteSet || e instanceof Nez.Byte || e instanceof Nez.Any) {
			return true;
		}
		return false;
	}

	class OptimizerVisitor extends Expression.TransformVisitor {

		/**
		 * Local Optimization Option
		 */

		boolean verboseGrammar = false;
		boolean InliningSubchoice = true;
		boolean enabledSecondChoice = false;

		HashMap<String, Production> bodyMap = null;
		HashMap<String, String> aliasMap = null;

		OptimizerVisitor() {
			initOption();
		}

		private void initOption() {
			if (strategy.Oalias) {
				this.bodyMap = new HashMap<String, Production>();
				this.aliasMap = new HashMap<String, String>();
			}
		}

		private NonterminalReference refc = null;

		void optimize() {
			long t1 = System.nanoTime();
			refc = Productions.countNonterminalReference(grammar);
			Production start = grammar.getStartProduction();
			optimizeProduction(start);
			long t2 = System.nanoTime();
			Verbose.printElapsedTime("Lexical Optimization", t1, t2);

			if (strategy.ChoicePrediction > 1) {
				t1 = t2;
				optimizeChoicePrediction();
				t2 = System.nanoTime();
				Verbose.printElapsedTime("Choice Prediction", t2, t2);
			}
			NonterminalReference refc2 = Productions.countNonterminalReference(grammar);
			UList<Production> prodList = new UList<Production>(new Production[grammar.size()]);
			for (Production p : grammar) {
				String uname = p.getUniqueName();
				// System.out.printf("%s refc %d -> %d rec=%s\n", uname,
				// refc.count(uname), refc2.count(uname),
				if (refc2.count(uname) > 0) {
					prodList.add(p);
				}
			}
			grammar.update(prodList);
			long t3 = System.nanoTime();
			Verbose.printElapsedTime("Inlining", t2, t3);
		}

		private Expression optimizeProduction(Production p) {
			String uname = p.getUniqueName();
			if (!this.isVisited(uname)) {
				this.visited(uname);
				Expression optimized = this.visitInner(p.getExpression(), null);
				p.setExpression(optimized);
				if (strategy.Oalias) {
					performAliasAnalysis(p);
				}
				return optimized;
			}
			return p.getExpression(); // already optimized
		}

		private void performAliasAnalysis(Production p) {
			String key = p.getExpression().toString();
			Production p2 = bodyMap.get(key);
			if (p2 == null) {
				bodyMap.put(key, p);
				return;
			}
			aliasMap.put(p.getLocalName(), p2.getLocalName());
			verboseFoundAlias(p.getLocalName(), p2.getLocalName());
		}

		private String findAliasName(String nname) {
			if (aliasMap != null) {
				String alias = aliasMap.get(nname);
				if (alias != null) {
					return alias;
				}
			}
			return null;
		}

		@Override
		public Expression visitNonTerminal(NonTerminal n, Object a) {
			Production p = n.getProduction();
			Expression deref = optimizeProduction(p);
			if (strategy.Oinline) {
				if (deref instanceof NonTerminal) {
					// verboseInline("inline(deref)", n, deref);
					return this.visitNonTerminal((NonTerminal) deref, a);
				}
				if (deref.size() == 0) {
					// verboseInline("inline(single)", n, deref);
					return deref;
				}
				if (strategy.Olex && isSingleInstruction(deref)) {
					// verboseInline("inline(single instruction)", n, deref);
					return deref;
				}
			}
			if (strategy.Oinline) {
				int c = refc.count(n.getUniqueName());
				if (c == 1 && !Productions.isRecursive(p)) {
					// verboseInline("inline(ref=1)", n, deref);
					return deref;
				}
			}
			String alias = findAliasName(n.getLocalName());
			if (alias != null) {
				NonTerminal nn = n.newNonTerminal(alias);
				// verboseInline("inline(alias)", n, nn);
				return nn;
			}
			// verboseInline("*inline(ref=" + refc.count(n.getUniqueName()), n,
			// n);
			return n;
		}

		@Override
		public Expression visitPair(Nez.Pair p, Object a) {
			List<Expression> l = Expressions.flatten(p);
			List<Expression> l2 = Expressions.newList(l.size());
			for (int i = 0; i < l.size(); i++) {
				Expression inner = l.get(i);
				inner = (Expression) inner.visit(this, a);
				Expressions.addSequence(l2, inner);
			}
			this.optimizeNotCharacterSet(l2);
			if (strategy.Oorder) {
				while (this.optimizeTreeConstruction(l2))
					;
			}
			return Expressions.newPair(l2);
		}

		private boolean optimizeTreeConstruction(List<Expression> l) {
			boolean res = false;
			for (int i = 1; i < l.size(); i++) {
				Expression first = l.get(i - 1);
				Expression second = l.get(i);
				if (isSingleCharacter(second)) {
					if (first instanceof Nez.BeginTree) {
						((Nez.BeginTree) first).shift -= 1;
						Expressions.swap(l, i - 1, i);
						// this.verboseSequence("reorder", second, first);
						res = true;
						continue;
					}
					if (first instanceof Nez.FoldTree) {
						((Nez.FoldTree) first).shift -= 1;
						Expressions.swap(l, i - 1, i);
						// this.verboseSequence("reorder", second, first);
						res = true;
						continue;
					}
					if (first instanceof Nez.EndTree) {
						((Nez.EndTree) first).shift -= 1;
						Expressions.swap(l, i - 1, i);
						// this.verboseSequence("reorder", second, first);
						res = true;
						continue;
					}
					if (first instanceof Nez.Tag || first instanceof Nez.Replace) {
						Expressions.swap(l, i - 1, i);
						// this.verboseSequence("reorder", second, first);
						res = true;
						continue;
					}
				}
				if (!strategy.Moz) {
					if (second instanceof Nez.EndTree) {
						if (first instanceof Nez.Tag) {
							((Nez.EndTree) second).tag = ((Nez.Tag) first).tag;
							Expressions.swap(l, i - 1, i);
							l.set(i, Expressions.newEmpty());
							// this.verboseSequence("merge", second, first);
							res = true;
							continue;
						}
						if (first instanceof Nez.Replace) {
							((Nez.EndTree) second).value = ((Nez.Replace) first).value;
							Expressions.swap(l, i - 1, i);
							l.set(i, Expressions.newEmpty());
							// this.verboseSequence("merge", second, first);
							res = true;
							continue;
						}
					}
				}
			}
			return res;
		}

		private void optimizeNotCharacterSet(List<Expression> l) {
			for (int i = l.size() - 1; i > 0; i--) {
				Expression first = l.get(i - 1);
				Expression second = l.get(i);
				if (isNotChar(first)) {
					if (second instanceof Nez.Any) {
						l.set(i, convertBitMap(second, first.get(0)));
						l.set(i - 1, Expressions.newEmpty());
					}
					if (second instanceof Nez.ByteSet /* && isNotChar(first) */) {
						l.set(i, convertBitMap(second, first.get(0)));
						l.set(i - 1, Expressions.newEmpty());
					}
				}
			}
		}

		private boolean isNotChar(Expression p) {
			if (p instanceof Nez.Not) {
				return (p.get(0) instanceof Nez.ByteSet || p.get(0) instanceof Nez.Byte);
			}
			return false;
		}

		private Expression convertBitMap(Expression next, Expression not) {
			boolean[] bany = null;
			// boolean isBinary = false;
			Expression nextNext = Expressions.next(next);
			if (nextNext != null) {
				next = Expressions.first(next);
			}
			if (next instanceof Nez.Any) {
				bany = Bytes.newMap(true);
				if (!strategy.BinaryGrammar) {
					bany[0] = false;
				}
			}
			if (next instanceof Nez.ByteSet) {
				Nez.ByteSet bm = (Nez.ByteSet) next;
				bany = bm.byteMap.clone();
			}

			if (not instanceof Nez.ByteSet) {
				Nez.ByteSet bm = (Nez.ByteSet) not;
				for (int c = 0; c < bany.length - 1; c++) {
					if (bm.byteMap[c] && bany[c] == true) {
						bany[c] = false;
					}
				}
			}
			if (not instanceof Nez.Byte) {
				Nez.Byte bc = (Nez.Byte) not;
				if (bany[bc.byteChar] == true) {
					bany[bc.byteChar] = false;
				}
			}
			return Expressions.newByteSet(next.getSourceLocation(), bany);
		}

		@Override
		public Expression visitLinkTree(Nez.LinkTree p, Object a) {
			if (p.get(0) instanceof Nez.Choice) {
				Expression choice = p.get(0);
				UList<Expression> l = Expressions.newUList(choice.size());
				for (Expression inner : choice) {
					inner = this.visitInner(inner, a);
					l.add(Expressions.newLinkTree(p.getSourceLocation(), p.label, inner));
				}
				return choice.newChoice(l);
			}
			return super.visitLinkTree(p, a);
		}

		@Override
		public Expression visitChoice(Nez.Choice p, Object a) {
			assert (p.visited == false);
			p.visited = true;
			UList<Expression> l = Expressions.newUList(p.size());
			flattenAndOptimizeSubExpressions(p, l, a);
			// for (Expression inner : p) {
			// Expressions.addChoice(l, this.visitInner(inner, a));
			// }
			p.inners = l.compactArray();
			if (strategy.ChoicePrediction != 0) {
				Expression optimized = Expressions.tryConvertingByteSet(p);
				if (optimized != p) {
					// this.verboseOptimized("choice-to-set", p, optimized);
					return optimized;
				}
			}
			return p;
		}

		private void flattenAndOptimizeSubExpressions(Nez.Choice choice, List<Expression> l, Object a) {
			for (Expression inner : choice) {
				inner = optimizeSubExpression(inner, a);
				Expressions.addChoice(l, inner);
			}
		}

		private Expression optimizeSubExpression(Expression e, Object a) {
			if (e instanceof NonTerminal) {
				if (InliningSubchoice && strategy.Oinline) {
					while (e instanceof NonTerminal) {
						NonTerminal n = (NonTerminal) e;
						Production p = n.getProduction();
						e = optimizeProduction(p);
					}
					return e;
				}
			}
			return this.visitInner(e, a);
		}

		/* Choice Prediction */

		private void optimizeChoicePrediction() {
			NonterminalReference refc = Productions.countNonterminalReference(grammar);
			for (Production p : grammar) {
				if (refc.count(p.getUniqueName()) > 1) {
					optimizeChoicePrediction(p.getExpression());
				}
			}
		}

		private void optimizeChoicePrediction(Expression e) {
			if (e instanceof Nez.Choice) {
				if (((Nez.Choice) e).predicted == null) {
					optimizeChoicePrediction((Nez.Choice) e);
					for (Expression sub : e) {
						if (!(sub instanceof Nez.Choice)) {
							optimizeChoicePrediction(sub);
						}
					}
				}
				return;
			}
			for (Expression sub : e) {
				optimizeChoicePrediction(sub);
			}
		}

		UList<Expression> bufferList = Expressions.newUList(256);
		HashMap<String, Expression> bufferMap = new HashMap<>();
		ArrayList<Expression> uniqueList = new ArrayList<>();
		HashMap<String, Byte> bufferIndex = new HashMap<>();

		private void optimizeChoicePrediction(Nez.Choice choice) {
			Nez.ChoicePrediction p = new Nez.ChoicePrediction();
			choice.predicted = p;

			bufferList.clear(0);
			bufferMap.clear();
			uniqueList.clear();
			bufferIndex.clear();

			byte[] indexMap = new byte[256];
			int count = 0;
			int selected = 0;
			for (int ch = 0; ch < 255; ch++) {
				Expression predicted = selectPredictedChoice(choice, ch, indexMap);
				if (predicted != null) {
					count++;
					if (predicted instanceof Nez.Choice) {
						selected += predicted.size();
					} else {
						selected += 1;
					}
				}
			}
			p.reduced = (float) selected / count;
			p.striped = new boolean[bufferMap.size()];
			int c = 0;
			Expression[] newlist2 = new Expression[bufferMap.size()];
			for (Expression e : uniqueList) {
				newlist2[c] = e;
				c++;
			}
			choice.inners = newlist2;
			p.indexMap = indexMap;
		}

		private Expression selectPredictedChoice(Nez.Choice choice, int ch, byte[] indexMap) {
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < choice.size(); i++) {
				Expression p = choice.get(i);
				ByteAcceptance acc = ByteAcceptance.acc(p, ch);
				if (acc == ByteAcceptance.Reject) {
					continue;
				}
				sb.append(':');
				sb.append(i);
			}
			String key = sb.toString();
			if (key.length() == 0) {
				indexMap[ch] = 0;
				return null; // empty
			}
			if (bufferMap.containsKey(key)) {
				indexMap[ch] = bufferIndex.get(key);
				assert (uniqueList.get(indexMap[ch] - 1) == bufferMap.get(key));
				return bufferMap.get(key);
			}
			// boolean commonFactored = false;
			for (Expression sub : choice) {
				ByteAcceptance acc = ByteAcceptance.acc(sub, ch);
				if (acc == ByteAcceptance.Reject) {
					continue;
				}
				if (bufferList.size() > 0) {
					int prev = bufferList.size() - 1;
					Expression last = bufferList.ArrayValues[prev];
					Expression common = tryFactoringCommonLeft(choice, last, sub, true);
					if (common != null) {
						bufferList.ArrayValues[prev] = common;
						// commonFactored = true;
						continue;
					}
				}
				bufferList.add(sub);
			}
			Expression p = Expressions.newChoice(bufferList);
			bufferList.clear(0);
			bufferMap.put(key, p);
			uniqueList.add(p);
			byte b = (byte) uniqueList.size();
			indexMap[ch] = b;
			bufferIndex.put(key, b);
			assert (uniqueList.get(indexMap[ch] - 1) == bufferMap.get(key));
			return p;
		}

		// private void verboseReference(String name, int ref) {
		// if (this.verboseGrammar) {
		// ConsoleUtils.println(name + ": ref=" + ref);
		// }
		// }

		private void verboseFoundAlias(String name, String alias) {
			if (this.verboseGrammar) {
				ConsoleUtils.println("found alias production: " + name + ", " + alias);
			}
		}

		// private String shorten(Expression e) {
		// String s = e.toString();
		// if (s.length() > 40) {
		// return s.substring(0, 40) + " ... ";
		// }
		// return s;
		// }
		//
		// private void verboseInline(String name, NonTerminal n, Expression e)
		// {
		// Verbose.println(name + ": " + n.getLocalName() + " => " + e);
		// if (this.verboseGrammar) {
		// ConsoleUtils.println(name + ": " + n.getLocalName() + " => " +
		// shorten(e));
		// }
		// }
		//
		// private void verboseOptimized(String msg, Expression e, Expression
		// e2) {
		// Verbose.println(msg + ":=> " + e + " => " + e2);
		// }
		//
		// private void verboseSequence(String msg, Expression e, Expression e2)
		// {
		// Verbose.println(msg + ":=> " + e + " " + e2);
		// // if (this.verboseGrammar) {
		// // }
		// }
	}

	public final static Expression tryFactoringCommonLeft(Nez.Choice base, Expression e, Expression e2, boolean ignoredFirstChar) {
		UList<Expression> l = null;
		while (e != null && e2 != null) {
			Expression f = Expressions.first(e);
			Expression f2 = Expressions.first(e2);
			if (ignoredFirstChar) {
				ignoredFirstChar = false;
				if (Expression.isByteConsumed(f) && Expression.isByteConsumed(f2)) {
					l = Expressions.newUList(4);
					l.add(f);
					e = Expressions.next(e);
					e2 = Expressions.next(e2);
					continue;
				}
				return null;
			}
			if (!f.equals(f2)) {
				break;
			}
			if (l == null) {
				l = Expressions.newUList(4);
			}
			l.add(f);
			e = Expressions.next(e);
			e2 = Expressions.next(e2);
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
		return base.newPair(l);
	}

	class NormalizerVisitor extends Expression.TransformVisitor {

		void perform() {
			NonterminalReference refCounts = Productions.countNonterminalReference(grammar);
			UList<Production> prodList = new UList<Production>(new Production[grammar.size()]);
			for (Production p : grammar) {
				if (refCounts.count(p.getUniqueName()) > 0) {
					if (strategy.Ostring) {
						Expression e = visitInner(p.getExpression(), null);
						p.setExpression(e);
					}
					prodList.add(p);
				}
			}
			grammar.update(prodList);
		}

		@Override
		public Expression visitPair(Nez.Pair p, Object a) {
			if (!fastCheck(p)) {
				p.set(0, this.visitInner(p.get(0), a));
				p.set(1, this.visitInner(p.get(1), a));
				return p;
			}
			Expression e = Expressions.tryConvertingMultiCharSequence(p);
			if (e instanceof Nez.Pair) {
				e.set(1, this.visitInner(e.get(1), a));
			}
			// if (e != p) {
			// Verbose.println("" + p + " => " + e);
			// }
			return e;
		}

		private boolean fastCheck(Nez.Pair e) {
			if (e.get(0) instanceof Nez.Byte) {
				Expression second = e.get(1);
				if (second instanceof Nez.Byte) {
					return true;
				}
				if (second instanceof Nez.Pair) {
					return second.get(0) instanceof Nez.Byte;
				}
			}
			return false;
		}

	}

	// Report

	private final void reportError(Expression p, String message) {
		if (p.getSourceLocation() != null) {
			ConsoleUtils.perror(grammar, ConsoleUtils.ErrorColor, p.formatSourceMessage("error", message));
		}
	}

	private final void reportWarning(Expression p, String message) {
		if (p.getSourceLocation() != null) {
			ConsoleUtils.perror(grammar, ConsoleUtils.WarningColor, p.formatSourceMessage("warning", message));
		}
	}

	private final void reportNotice(Expression p, String message) {
		if (p.getSourceLocation() != null) {
			ConsoleUtils.perror(grammar, ConsoleUtils.WarningColor, p.formatSourceMessage("notice", message));
		}
	}
}
