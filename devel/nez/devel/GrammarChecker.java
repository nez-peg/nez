//package nez.devel;
//
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//
//import nez.lang.ByteAcceptance;
//import nez.lang.Bytes;
//import nez.lang.Conditions;
//import nez.lang.Expression;
//import nez.lang.ExpressionDuplicationVisitor;
//import nez.lang.ExpressionTransformer;
//import nez.lang.Expressions;
//import nez.lang.Grammar;
//import nez.lang.Nez;
//import nez.lang.NonTerminal;
//import nez.lang.Production;
//import nez.lang.Productions;
//import nez.lang.Typestate;
//import nez.parser.ParserStrategy;
//import nez.util.ConsoleUtils;
//import nez.util.StringUtils;
//import nez.util.UList;
//import nez.util.Verbose;
//
//public class GrammarChecker {
//
//	public final static Grammar check(Production start, ParserStrategy strategy) {
//		GrammarContext context = new GrammarContext(start, null, strategy);
//		ConditionEliminator eliminator = new ConditionEliminator(context);
//		Grammar g = eliminator.transform();
//		new GrammarOptimizer(g, strategy);
//		// new Normalizer().perform(g);
//		return g;
//	}
//
//	private static class ConditionEliminator extends ExpressionDuplicationVisitor {
//
//		private GrammarContext context;
//
//		private Grammar grammar;
//		private Conditions conds;
//		private boolean ConstructingTree = true;
//		private Typestate requiredTypestate;
//
//		private ConditionEliminator(GrammarContext context) {
//			this.context = context;
//		}
//
//		final Grammar transform() {
//			this.grammar = context.newGrammar();
//			this.conds = this.context.newConditions();
//			if (context.getParserStrategy().TreeConstruction) {
//				this.ConstructingTree = true;
//				this.requiredTypestate = Typestate.Tree;
//			} else {
//				this.ConstructingTree = false;
//				this.requiredTypestate = Typestate.Unit;
//				this.enterNonASTContext();
//			}
//			String cname = conds.conditionalName(context.getStartProduction(), isNonTreeConstruction());
//			checkFirstVisitedProduction(cname, context.getStartProduction());
//			return this.grammar;
//		}
//
//		/* ASTContext */
//
//		private boolean enterNonASTContext() {
//			boolean b = ConstructingTree;
//			ConstructingTree = false;
//			return b;
//		}
//
//		private void exitNonASTContext(boolean backed) {
//			ConstructingTree = backed;
//		}
//
//		private boolean isNonTreeConstruction() {
//			return !this.ConstructingTree;
//		}
//
//		/* Conditional */
//
//		private void onFlag(String flag) {
//			this.conds.put(flag, true);
//		}
//
//		private void offFlag(String flag) {
//			this.conds.put(flag, false);
//		}
//
//		private boolean isFlag(String flag) {
//			return this.conds.get(flag);
//		}
//
//		private void reportInserted(Expression e, String operator) {
//			context.reportWarning(e, "expected " + operator + " .. => inserted!!");
//		}
//
//		private void reportRemoved(Expression e, String operator) {
//			context.reportWarning(e, "unexpected " + operator + " .. => removed!!");
//		}
//
//		private Expression check(Expression e) {
//			return (Expression) e.visit(this, null);
//		}
//
//		private Production checkFirstVisitedProduction(String cname, Production p) {
//			this.visited(cname);
//			Production gp = this.grammar.addProduction(cname, null);
//			Typestate stackedTypestate = this.requiredTypestate;
//			this.requiredTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(p);
//			gp.setExpression(check(p.getExpression()));
//			this.requiredTypestate = stackedTypestate;
//			return gp;
//		}
//
//		@Override
//		public Expression visitNonTerminal(NonTerminal n, Object a) {
//			Production p = n.getProduction();
//			if (p == null) {
//				if (n.isTerminal()) {
//					context.reportNotice(n, "undefined terminal: " + n.getLocalName());
//					return Expressions.newExpression(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
//				}
//				context.reportWarning(n, "undefined production: " + n.getLocalName());
//				return n.newEmpty();
//			}
//			if (n.isTerminal()) { /* Inlining Terminal */
//				try {
//					return check(p.getExpression());
//				} catch (StackOverflowError e) {
//					/* Handling a bad grammar */
//					context.reportError(n, "terminal is recursive: " + n.getLocalName());
//					return Expressions.newExpression(n.getSourceLocation(), StringUtils.unquoteString(n.getLocalName()));
//				}
//			}
//
//			Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(p);
//
//			if (this.requiredTypestate == Typestate.TreeMutation) {
//				if (innerTypestate == Typestate.TreeMutation) {
//					Verbose.println("inlining mutation nonterminal" + p.getLocalName());
//					return check(p.getExpression());
//				}
//			}
//
//			String cname = conds.conditionalName(p, innerTypestate == Typestate.Unit);
//			if (!this.isVisited(cname)) {
//				this.checkFirstVisitedProduction(cname, p);
//			}
//
//			NonTerminal pn = Expressions.newNonTerminal(n.getSourceLocation(), grammar, cname);
//			if (innerTypestate == Typestate.Unit) {
//				return pn;
//			}
//
//			Typestate required = this.requiredTypestate;
//			if (required == Typestate.Tree) {
//				if (innerTypestate == Typestate.TreeMutation) {
//					reportInserted(n, "{");
//					this.requiredTypestate = Typestate.TreeMutation;
//					return Expressions.newTree(n.getSourceLocation(), pn);
//				}
//				this.requiredTypestate = Typestate.TreeMutation;
//				return pn;
//			}
//			if (required == Typestate.TreeMutation) {
//				if (innerTypestate == Typestate.Tree) {
//					reportInserted(n, "$");
//					this.requiredTypestate = Typestate.Tree;
//					return Expressions.newLinkTree(n.getSourceLocation(), null, pn);
//				}
//			}
//			return pn;
//		}
//
//		@Override
//		public Expression visitOn(Nez.OnCondition p, Object a) {
//			if (!this.conds.containsKey(p.flagName)) {
//				this.context.reportWarning(p, "unused condition: " + p.flagName);
//				return check(p.get(0));
//			}
//			if (!conds.isConditional(p.get(0), p.flagName)) {
//				this.context.reportWarning(p, "unreached condition: " + p.flagName);
//				return check(p.get(0));
//			}
//			Boolean stackedFlag = isFlag(p.flagName);
//			if (p.isPositive()) {
//				onFlag(p.flagName);
//			} else {
//				offFlag(p.flagName);
//			}
//			Expression newe = check(p.get(0));
//			if (stackedFlag) {
//				onFlag(p.flagName);
//			} else {
//				offFlag(p.flagName);
//			}
//			return newe;
//		}
//
//		@Override
//		public Expression visitIf(Nez.IfCondition p, Object a) {
//			if (isFlag(p.flagName)) { /* true */
//				return p.predicate ? p.newEmpty() : p.newFailure();
//			}
//			return p.predicate ? p.newFailure() : p.newEmpty();
//		}
//
//		@Override
//		public Expression visitDetree(Nez.Detree p, Object a) {
//			boolean stacked = this.enterNonASTContext();
//			Expression inner = check(p.get(0));
//			this.exitNonASTContext(stacked);
//			return inner;
//		}
//
//		@Override
//		public Expression visitBeginTree(Nez.BeginTree p, Object a) {
//			if (this.isNonTreeConstruction()) {
//				return p.newEmpty();
//			}
//			if (this.requiredTypestate != Typestate.Tree) {
//				this.reportRemoved(p, "{");
//				return p.newEmpty();
//			}
//			this.requiredTypestate = Typestate.TreeMutation;
//			return super.visitBeginTree(p, a);
//		}
//
//		@Override
//		public Expression visitFoldTree(Nez.FoldTree p, Object a) {
//			if (this.isNonTreeConstruction()) {
//				return p.newEmpty();
//			}
//			if (this.requiredTypestate != Typestate.TreeMutation) {
//				this.reportRemoved(p, "{$");
//				return p.newEmpty();
//			}
//			this.requiredTypestate = Typestate.TreeMutation;
//			return super.visitFoldTree(p, a);
//		}
//
//		@Override
//		public Expression visitEndTree(Nez.EndTree p, Object a) {
//			if (this.isNonTreeConstruction()) {
//				return p.newEmpty();
//			}
//			if (this.requiredTypestate != Typestate.TreeMutation) {
//				this.reportRemoved(p, "}");
//				return p.newEmpty();
//			}
//			this.requiredTypestate = Typestate.TreeMutation;
//			return super.visitEndTree(p, a);
//		}
//
//		@Override
//		public Expression visitTag(Nez.Tag p, Object a) {
//			if (this.isNonTreeConstruction()) {
//				return p.newEmpty();
//			}
//			if (this.requiredTypestate != Typestate.TreeMutation) {
//				reportRemoved(p, "#" + p.tag.getSymbol());
//				return p.newEmpty();
//			}
//			return p;
//		}
//
//		@Override
//		public Expression visitReplace(Nez.Replace p, Object a) {
//			if (this.isNonTreeConstruction()) {
//				return p.newEmpty();
//			}
//			if (this.requiredTypestate != Typestate.TreeMutation) {
//				reportRemoved(p, "`" + p.value + "`");
//				return p.newEmpty();
//			}
//			return p;
//		}
//
//		@Override
//		public Expression visitLinkTree(Nez.LinkTree p, Object a) {
//			Expression inner = p.get(0);
//			if (this.isNonTreeConstruction()) {
//				return this.check(inner);
//			}
//			if (this.requiredTypestate != Typestate.TreeMutation) {
//				reportRemoved(p, "$");
//				return this.check(inner);
//			}
//			Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(inner);
//			if (innerTypestate != Typestate.Tree) {
//				reportInserted(p, "{");
//				inner = Expressions.newTree(inner.getSourceLocation(), this.check(inner));
//			} else {
//				this.requiredTypestate = Typestate.Tree;
//				inner = this.check(p.get(0));
//			}
//			this.requiredTypestate = Typestate.TreeMutation;
//			return Expressions.newLinkTree(p.getSourceLocation(), p.label, inner);
//		}
//
//		@Override
//		public Expression visitChoice(Nez.Choice p, Object a) {
//			Typestate required = this.requiredTypestate;
//			Typestate next = this.requiredTypestate;
//			UList<Expression> l = Expressions.newUList(p.size());
//			for (Expression inner : p) {
//				this.requiredTypestate = required;
//				Expressions.addChoice(l, this.check(inner));
//				if (this.requiredTypestate != required && this.requiredTypestate != next) {
//					next = this.requiredTypestate;
//				}
//			}
//			this.requiredTypestate = next;
//			return Expressions.newChoice(l);
//		}
//
//		@Override
//		public Expression visitZeroMore(Nez.ZeroMore p, Object a) {
//			return Expressions.newZeroMore(p.getSourceLocation(), visitOptionalInner(p));
//		}
//
//		@Override
//		public Expression visitOneMore(Nez.OneMore p, Object a) {
//			return Expressions.newOneMore(p.getSourceLocation(), visitOptionalInner(p));
//		}
//
//		@Override
//		public Expression visitOption(Nez.Option p, Object a) {
//			return Expressions.newOption(p.getSourceLocation(), visitOptionalInner(p));
//		}
//
//		private Expression visitOptionalInner(Nez.Unary p) {
//			Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(p.get(0));
//			if (innerTypestate == Typestate.Tree) {
//				if (this.requiredTypestate == Typestate.TreeMutation) {
//					reportInserted(p.get(0), "$");
//					this.requiredTypestate = Typestate.Tree;
//					Expression inner = check(p.get(0));
//					inner = Expressions.newLinkTree(p.getSourceLocation(), inner);
//					this.requiredTypestate = Typestate.TreeMutation;
//					return inner;
//				} else {
//					context.reportWarning(p, "disallowed tree construction in e?, e*, e+, or &e  " + innerTypestate);
//					boolean stacked = this.enterNonASTContext();
//					Expression inner = check(p.get(0));
//					this.exitNonASTContext(stacked);
//					return inner;
//				}
//			}
//			return check(p.get(0));
//		}
//
//		@Override
//		public Expression visitAnd(Nez.And p, Object a) {
//			return Expressions.newAnd(p.getSourceLocation(), visitOptionalInner(p));
//		}
//
//		@Override
//		public Expression visitNot(Nez.Not p, Object a) {
//			Expression inner = p.get(0);
//			Typestate innerTypestate = this.isNonTreeConstruction() ? Typestate.Unit : context.typeState(inner);
//			if (innerTypestate != Typestate.Unit) {
//				// context.reportWarning(p,
//				// "disallowed tree construction in !e");
//				boolean stacked = this.enterNonASTContext();
//				inner = this.check(inner);
//				this.exitNonASTContext(stacked);
//			} else {
//				inner = this.check(inner);
//			}
//			return Expressions.newNot(p.getSourceLocation(), inner);
//		}
//	}
//
//	static class GrammarOptimizer extends ExpressionTransformer {
//		boolean verboseGrammar = false;
//		boolean enabledSecondChoice = false;
//
//		final Grammar grammar;
//		final ParserStrategy strategy;
//		final Map<String, Integer> refCounts;
//		final HashSet<String> optimizedMap = new HashSet<String>();
//		HashMap<String, Production> bodyMap = null;
//		HashMap<String, String> aliasMap = null;
//
//		GrammarOptimizer(Grammar gg, ParserStrategy strategy) {
//			this.grammar = gg;
//			this.strategy = strategy;
//			this.refCounts = Productions.countNonterminalReference(grammar);
//			if (strategy.Oalias) {
//				this.bodyMap = new HashMap<String, Production>();
//				this.aliasMap = new HashMap<String, String>();
//			}
//			if (strategy.ChoicePrediction) {
//				this.deterministicChoiceList = new UList<Nez.Choice>(new Nez.Choice[8]);
//			}
//			Production start = grammar.getStartProduction();
//			optimizeProduction(start);
//			this.optimizeDeterministicChoice();
//		}
//
//		private Expression optimizeProduction(Production p) {
//			assert (p.getGrammar() == this.grammar);
//			String uname = p.getLocalName();
//			if (!optimizedMap.contains(uname)) {
//				optimizedMap.add(uname);
//				Expression optimized = optimize(p.getExpression());
//				p.setExpression(optimized);
//				// if (strategy.Oalias) {
//				// performAliasAnalysis(p);
//				// }
//				return optimized;
//			}
//			return p.getExpression(); // already optimized
//		}
//
//		private Expression optimize(Expression e) {
//			return this.visitInner(e, null);
//		}
//
//		private void verboseReference(String name, int ref) {
//			if (this.verboseGrammar) {
//				ConsoleUtils.println(name + ": ref=" + ref);
//			}
//		}
//
//		private void verboseFoundAlias(String name, String alias) {
//			if (this.verboseGrammar) {
//				ConsoleUtils.println("found alias production: " + name + ", " + alias);
//			}
//		}
//
//		private String shorten(Expression e) {
//			String s = e.toString();
//			if (s.length() > 40) {
//				return s.substring(0, 40) + " ... ";
//			}
//			return s;
//		}
//
//		private void verboseInline(String name, NonTerminal n, Expression e) {
//			if (this.verboseGrammar) {
//				ConsoleUtils.println(name + ": " + n.getLocalName() + " => " + shorten(e));
//			}
//		}
//
//		private void verboseOptimized(String msg, Expression e, Expression e2) {
//			if (this.verboseGrammar) {
//				// Verbose.debug(msg + " => " + e + "\n\t=>" + e2);
//				ConsoleUtils.println(msg + ":=> " + shorten(e2));
//			}
//		}
//
//		private void verboseOutofOrdered(String msg, Expression e, Expression e2) {
//			if (this.verboseGrammar) {
//				// Verbose.debug(msg + " => " + e + "\n\t=>" + e2);
//				ConsoleUtils.println(msg + ":=> " + e + " " + e2);
//			}
//		}
//
//		// private void performAliasAnalysis(Production p) {
//		// String key = p.getExpression().toString();
//		// Production p2 = bodyMap.get(key);
//		// if (p2 == null) {
//		// bodyMap.put(key, p);
//		// return;
//		// }
//		// aliasMap.put(p.getLocalName(), p2.getLocalName());
//		// verboseFoundAlias(p.getLocalName(), p2.getLocalName());
//		// }
//		//
//		// private String findAliasName(String nname) {
//		// if (aliasMap != null) {
//		// String alias = aliasMap.get(nname);
//		// if (alias != null) {
//		// return alias;
//		// }
//		// }
//		// return null;
//		// }
//
//		@Override
//		public Expression visitNonTerminal(NonTerminal n, Object a) {
//			Production p = n.getProduction();
//			Expression deref = optimizeProduction(p);
//			if (strategy.Oinline) {
//				if (deref instanceof NonTerminal) {
//					verboseInline("inline(deref)", n, deref);
//					return this.visitNonTerminal((NonTerminal) deref, a);
//				}
//				if (deref.size() == 0) {
//					verboseInline("inline(deref)", n, deref);
//					return deref;
//				}
//				if (isMultiChar(deref)) {
//					verboseInline("multi-char", n, deref);
//					return deref;
//				}
//				if (strategy.Olex && isSingleInstruction(deref)) {
//					verboseInline("inline(instruction)", n, deref);
//					return deref;
//				}
//				if (this.refCounts.get(p.getUniqueName()) == 1) {
//					verboseInline("inline(ref=1)", n, deref);
//					return deref;
//				}
//			}
//			// String alias = findAliasName(n.getLocalName());
//			// if (alias != null) {
//			// NonTerminal nn = n.newNonTerminal(alias);
//			// verboseInline("inline(alias)", n, nn);
//			// return nn;
//			// }
//			return n;
//		}
//
//		// used to test inlining
//		public final static boolean isMultiChar(Expression e) {
//			if (e instanceof Nez.Byte || e instanceof Nez.MultiByte || e instanceof Nez.Empty) {
//				return true;
//			}
//			if (e instanceof Nez.Pair) {
//				return isMultiChar(e.get(0)) && isMultiChar(e.get(1));
//			}
//			if (e instanceof Nez.Sequence) {
//				for (Expression sub : e) {
//					if (!isMultiChar(sub)) {
//						return false;
//					}
//				}
//				return true;
//			}
//			return false;
//		}
//
//		// used to test inlining
//		public final static boolean isSingleInstruction(Expression e) {
//			if (e instanceof Nez.Not || e instanceof Nez.ZeroMore || e instanceof Nez.Option || e instanceof Nez.OneMore) {
//				return isSingleCharacter(e.get(0)) || isMultiChar(e.get(0));
//			}
//			return false;
//		}
//
//		// used to test inlining
//		public final static boolean isSingleCharacter(Expression e) {
//			if (e instanceof Nez.ByteSet || e instanceof Nez.Byte || e instanceof Nez.Any) {
//				return true;
//			}
//			return false;
//		}
//
//		@Override
//		public Expression visitSequence(Nez.Sequence p, Object a) {
//			return p.newPair(optimize(Expressions.flatten(p)));
//		}
//
//		@Override
//		public Expression visitPair(Nez.Pair p, Object a) {
//			return p.newPair(optimize(Expressions.flatten(p)));
//		}
//
//		private List<Expression> optimize(List<Expression> l) {
//			UList<Expression> l2 = Expressions.newUList(l.size());
//			for (int i = 0; i < l.size(); i++) {
//				l2.add(optimize(l.get(i)));
//			}
//			if (strategy.Oorder) {
//				while (this.performOutOfOrder(l2))
//					;
//			}
//			this.mergeNotCharacter(l2);
//			return l2;
//		}
//
//		private boolean performOutOfOrder(UList<Expression> l) {
//			boolean res = false;
//			for (int i = 1; i < l.size(); i++) {
//				Expression first = l.get(i - 1);
//				Expression next = l.get(i);
//				if (isSingleCharacter(next)) {
//					if (first instanceof Nez.BeginTree) {
//						((Nez.BeginTree) first).shift -= 1;
//						Expressions.swap(l, i - 1, i);
//						this.verboseOutofOrdered("out-of-order", next, first);
//						res = true;
//						continue;
//					}
//					if (first instanceof Nez.FoldTree) {
//						((Nez.FoldTree) first).shift -= 1;
//						Expressions.swap(l, i - 1, i);
//						this.verboseOutofOrdered("out-of-order", next, first);
//						res = true;
//						continue;
//					}
//					if (first instanceof Nez.EndTree) {
//						((Nez.EndTree) first).shift -= 1;
//						Expressions.swap(l, i - 1, i);
//						this.verboseOutofOrdered("out-of-order", next, first);
//						res = true;
//						continue;
//					}
//					if (first instanceof Nez.Tag || first instanceof Nez.Replace) {
//						Expressions.swap(l, i - 1, i);
//						this.verboseOutofOrdered("out-of-order", next, first);
//						res = true;
//						continue;
//					}
//				}
//			}
//			return res;
//		}
//
//		private void mergeNotCharacter(UList<Expression> l) {
//			for (int i = 1; i < l.size(); i++) {
//				Expression first = l.get(i - 1);
//				Expression next = l.get(i);
//				if (isNotChar(first)) {
//					if (next instanceof Nez.Any) {
//						l.ArrayValues[i] = convertByteSet(next, first.get(0));
//						l.ArrayValues[i - 1] = next.newEmpty();
//						this.verboseOptimized("not-any", first, l.ArrayValues[i]);
//					}
//					if (next instanceof Nez.ByteSet && isNotChar(first)) {
//						l.ArrayValues[i] = convertByteSet(next, first.get(0));
//						l.ArrayValues[i - 1] = next.newEmpty();
//						this.verboseOptimized("not-set", first, l.ArrayValues[i]);
//					}
//				}
//			}
//		}
//
//		private boolean isNotChar(Expression p) {
//			if (p instanceof Nez.Not) {
//				return (p.get(0) instanceof Nez.ByteSet || p.get(0) instanceof Nez.Byte);
//			}
//			return false;
//		}
//
//		private Expression convertByteSet(Expression next, Expression not) {
//			boolean[] bany = null;
//			boolean isBinary = false;
//			Expression nextNext = Expressions.next(next);
//			if (nextNext != null) {
//				next = Expressions.first(next);
//			}
//			if (next instanceof Nez.Any) {
//				Nez.Any any = (Nez.Any) next;
//				bany = Bytes.newMap(true);
//				if (!isBinary) {
//					bany[0] = false;
//				}
//			}
//			if (next instanceof Nez.ByteSet) {
//				Nez.ByteSet bm = (Nez.ByteSet) next;
//				bany = bm.byteMap.clone();
//			}
//
//			if (not instanceof Nez.ByteSet) {
//				Nez.ByteSet bm = (Nez.ByteSet) not;
//				for (int c = 0; c < bany.length - 1; c++) {
//					if (bm.byteMap[c] && bany[c] == true) {
//						bany[c] = false;
//					}
//				}
//			}
//			if (not instanceof Nez.Byte) {
//				Nez.Byte bc = (Nez.Byte) not;
//				if (bany[bc.byteChar] == true) {
//					bany[bc.byteChar] = false;
//				}
//			}
//			return not.newByteSet(isBinary, bany);
//		}
//
//		@Override
//		public Expression visitLinkTree(Nez.LinkTree p, Object a) {
//			if (p.get(0) instanceof Nez.Choice) {
//				Expression choice = p.get(0);
//				UList<Expression> l = Expressions.newUList(choice.size());
//				for (Expression inner : choice) {
//					inner = optimize(inner);
//					l.add(Expressions.newLinkTree(p.getSourceLocation(), p.label, inner));
//				}
//				return choice.newChoice(l);
//			}
//			return super.visitLinkTree(p, a);
//		}
//
//		// @Override
//		// public Expression visitChoice(Nez.Choice p, Object a) {
//		// if (!p.isOptimized()) {
//		// p.setOptimized();
//		// List<Expression> l = Expressions.newList(p.size());
//		// flattenChoiceList(p, l);
//		// return this.optimizeChoice(p, l);
//		// }
//		// return p;
//		// }
//		//
//		// private void flattenChoiceList(Nez.Choice choice, List<Expression> l)
//		// {
//		// for (Expression inner : choice) {
//		// inner = firstChoiceInlining(inner);
//		// if (inner instanceof Nez.Choice) {
//		// flattenChoiceList((Nez.Choice) inner, l);
//		// } else {
//		// l.add(inner);
//		// }
//		// }
//		// }
//		//
//		// private Expression firstChoiceInlining(Expression e) {
//		// while (e instanceof NonTerminal) {
//		// NonTerminal n = (NonTerminal) e;
//		// e = n.getProduction().getExpression();
//		// }
//		// return e;
//		// }
//		//
//		// private Expression optimizeChoice(Nez.Choice p, List<Expression> l) {
//		// Expression optimized = tryByteSet(p, l);
//		// if (optimized != null) {
//		// this.verboseOptimized("choice-to-set", p, optimized);
//		// return optimized;
//		// }
//		// l = tryTrieTree(p, l);
//		// if (l.size() == 1) {
//		// verboseOptimized("single-choice", p, l.get(0));
//		// return l.get(0);
//		// }
//		// Expression n = Expressions.newChoice(l);
//		// if (n instanceof Nez.Choice) {
//		// ((Nez.Choice) n).isTrieTree = p.isTrieTree;
//		// addDeterministicChoice((Nez.Choice) n);
//		// }
//		// return n;
//		// }
//		//
//		// private Expression tryByteSet(Nez.Choice choice, List<Expression>
//		// choiceList) {
//		// boolean byteMap[] = Bytes.newMap(false);
//		// for (Expression e : choiceList) {
//		// if (e instanceof Nez.Byte) {
//		// byteMap[((Nez.Byte) e).byteChar] = true;
//		// continue;
//		// }
//		// if (e instanceof Nez.ByteSet) {
//		// Bytes.appendBitMap(byteMap, ((Nez.ByteSet) e).byteMap);
//		// continue;
//		// }
//		// if (e instanceof Nez.Any) {
//		// return e;
//		// }
//		// return null;
//		// }
//		// return choice.newByteSet(false, byteMap);
//		// }
//		//
//		// private List<Expression> tryTrieTree(Nez.Choice choice,
//		// List<Expression> l) {
//		// for (Expression inner : l) {
//		// if (isTrieTreeHead(inner)) {
//		// continue;
//		// }
//		// return l;
//		// }
//		// Object[] buffers = new Object[257];
//		// for (Expression inner : l) {
//		// Expression first = Expressions.first(inner);
//		// if (first instanceof Nez.Byte) {
//		// Nez.Byte be = (Nez.Byte) first;
//		// buffers[be.byteChar] = mergeChoice(buffers[be.byteChar],
//		// Expressions.next(inner));
//		// } else {
//		// Nez.ByteSet bs = (Nez.ByteSet) first;
//		// for (int ch = 0; ch < buffers.length; ch++) {
//		// if (bs.byteMap[ch]) {
//		// buffers[ch] = mergeChoice(buffers[ch], Expressions.next(inner));
//		// }
//		// }
//		// }
//		// }
//		// l = new UList<Expression>(new Expression[8]);
//		// for (int ch = 0; ch < buffers.length; ch++) {
//		// if (buffers[ch] == null)
//		// continue;
//		// @SuppressWarnings("unchecked")
//		// UList<Expression> el = (UList<Expression>) buffers[ch];
//		// Expression be = Expressions.newByte(null, ch);
//		// if (el.size() == 1) {
//		// l.add(Expressions.newPair(null, be, el.get(0)));
//		// } else {
//		// Expression next = trySecondChoice(Expressions.newChoice(el), el);
//		// l.add(Expressions.newPair(null, be, next));
//		// }
//		// }
//		// choice.isTrieTree = true;
//		// return l;
//		// }
//		//
//		// private boolean isTrieTreeHead(Expression inner) {
//		// Expression first = Expressions.first(inner);
//		// if (first instanceof Nez.Byte || first instanceof Nez.ByteSet) {
//		// return true;
//		// }
//		// return false;
//		// }
//		//
//		// private UList<Expression> mergeChoice(Object e1, Expression e2) {
//		// if (e2 == null) {
//		// e2 = Expressions.newEmpty(null);
//		// }
//		// @SuppressWarnings("unchecked")
//		// UList<Expression> l = (UList<Expression>) e1;
//		// if (l == null) {
//		// l = new UList<Expression>(new Expression[2]);
//		// }
//		// Expressions.addChoice(l, e2);
//		// return l;
//		// }
//		//
//		// private Expression trySecondChoice(Expression e, UList<Expression>
//		// el) {
//		// if (this.enabledSecondChoice && e instanceof Nez.Choice) {
//		// return this.optimizeChoice((Nez.Choice) e, el);
//		// }
//		// return e;
//		// }
//
//		private UList<Nez.Choice> deterministicChoiceList = null;
//
//		private void addDeterministicChoice(Nez.Choice n) {
//			if (deterministicChoiceList != null) {
//				deterministicChoiceList.add(n);
//			}
//		}
//
//		private void optimizeDeterministicChoice() {
//			if (deterministicChoiceList != null) {
//				for (Nez.Choice p : this.deterministicChoiceList) {
//					optimizeDeterministicChoice(p);
//				}
//			}
//		}
//
//		private void optimizeDeterministicChoice(Nez.Choice p) {
//			// if (p.isTrieTree) {
//			// p.predictedCase = new Expression[257];
//			// p.firstInners = new Expression[p.size()];
//			// int c = 0;
//			// for (Expression sub : p) {
//			// Nez.Byte be = (Nez.Byte) Expressions.first(sub);
//			// p.predictedCase[be.byteChar] = sub;
//			// p.firstInners[c] = sub;
//			// c++;
//			// }
//			// p.reduced = 1.0f;
//			// } else {
//			// UList<Expression> choiceList = Expressions.newUList(p.size());
//			// flattenChoiceList(p, choiceList);
//			// int count = 0;
//			// int selected = 0;
//			// UList<Expression> newlist = Expressions.newUList(p.size());
//			// HashMap<String, Expression> map = new HashMap<String,
//			// Expression>();
//			// p.predictedCase = new Expression[257];
//			// boolean isTrieTree = true;
//			// for (int ch = 0; ch <= 255; ch++) {
//			// Expression predicted = selectChoice(p, choiceList, ch, newlist,
//			// map);
//			// p.predictedCase[ch] = predicted;
//			// if (predicted != null) {
//			// count++;
//			// if (predicted instanceof Nez.Choice) {
//			// selected += predicted.size();
//			// } else {
//			// selected += 1;
//			// }
//			// if (!isSingleCharacter(Expressions.next(predicted))) {
//			// isTrieTree = false;
//			// }
//			// }
//			// }
//			// p.isTrieTree = isTrieTree;
//			// p.reduced = (float) selected / count;
//			// p.firstInners = new Expression[map.size()];
//			// // Verbose.debug("reduced: " + choiceList.size() + " => " +
//			// // p.reduced);
//			// // Verbose.debug("map: " + map);
//			// int c = 0;
//			// for (String k : map.keySet()) {
//			// p.firstInners[c] = map.get(k);
//			// c++;
//			// }
//			// }
//		}
//
//		// OptimizerLibrary
//
//		private Expression selectChoice(Nez.Choice choice, UList<Expression> choiceList, int ch, UList<Expression> newlist, HashMap<String, Expression> map) {
//			StringBuilder sb = new StringBuilder();
//			for (int i = 0; i < choiceList.size(); i++) {
//				Expression p = choiceList.ArrayValues[i];
//				ByteAcceptance acc = ByteAcceptance.acc(p, ch);
//				if (acc == ByteAcceptance.Reject) {
//					continue;
//				}
//				sb.append(':');
//				sb.append(i);
//			}
//			String key = sb.toString();
//			if (key.length() == 0) {
//				return null; // empty
//			}
//			if (map.containsKey(key)) {
//				return map.get(key);
//			}
//			boolean commonFactored = false;
//			for (Expression sub : choiceList) {
//				ByteAcceptance acc = ByteAcceptance.acc(sub, ch);
//				if (acc == ByteAcceptance.Reject) {
//					continue;
//				}
//				if (newlist.size() > 0) {
//					int prev = newlist.size() - 1;
//					Expression last = newlist.ArrayValues[prev];
//					Expression common = tryLeftCommonFactoring(choice, last, sub, true);
//					if (common != null) {
//						newlist.ArrayValues[prev] = common;
//						commonFactored = true;
//						continue;
//					}
//				}
//				newlist.add(sub);
//			}
//			Expression p = Expressions.newChoice(newlist);
//			newlist.clear(0);
//			if (commonFactored && !(p instanceof Nez.Choice)) {
//				tryFactoredSecondChoice(p);
//			}
//			map.put(key, p);
//			return p;
//		}
//
//		private void tryFactoredSecondChoice(Expression p) {
//			if (p instanceof Nez.Choice) {
//				if (((Nez.Choice) p).predicted == null) {
//					// Verbose.debug("Second choice: " + p);
//				}
//				return;
//			}
//			for (Expression sub : p) {
//				tryFactoredSecondChoice(sub);
//			}
//		}
//
//		public final static Expression tryLeftCommonFactoring(Nez.Choice base, Expression e, Expression e2, boolean ignoredFirstChar) {
//			UList<Expression> l = null;
//			while (e != null && e2 != null) {
//				Expression f = Expressions.first(e);
//				Expression f2 = Expressions.first(e2);
//				if (ignoredFirstChar) {
//					ignoredFirstChar = false;
//					if (Expression.isByteConsumed(f) && Expression.isByteConsumed(f2)) {
//						l = Expressions.newUList(4);
//						l.add(f);
//						e = Expressions.next(e);
//						e2 = Expressions.next(e2);
//						continue;
//					}
//					return null;
//				}
//				if (!f.equals(f2)) {
//					break;
//				}
//				if (l == null) {
//					l = Expressions.newUList(4);
//				}
//				l.add(f);
//				e = Expressions.next(e);
//				e2 = Expressions.next(e2);
//			}
//			if (l == null) {
//				return null;
//			}
//			if (e == null) {
//				e = base.newEmpty();
//			}
//			if (e2 == null) {
//				e2 = base.newEmpty();
//			}
//			Expression alt = base.newChoice(e, e2);
//			l.add(alt);
//			return base.newPair(l);
//		}
//
//	}
//
// }
