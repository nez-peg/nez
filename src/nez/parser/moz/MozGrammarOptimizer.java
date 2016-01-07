package nez.parser.moz;

import java.util.HashMap;
import java.util.List;

import nez.lang.ByteAcceptance;
import nez.lang.Bytes;
import nez.lang.Expression;
import nez.lang.ExpressionTransformer;
import nez.lang.Expressions;
import nez.lang.Grammar;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Productions;
import nez.lang.Productions.NonterminalReference;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.UList;

class MozGrammarOptimizer extends ExpressionTransformer {

	/**
	 * 
	 */
	boolean InliningSubchoice = false;

	boolean verboseGrammar = false;
	boolean enabledSecondChoice = false;

	final Grammar grammar;
	final ParserStrategy strategy;
	HashMap<String, Production> bodyMap = null;
	HashMap<String, String> aliasMap = null;

	MozGrammarOptimizer(ParserGrammar grammar, ParserStrategy strategy) {
		this.grammar = grammar;
		this.strategy = strategy;
		initOption();
		optimize();
	}

	private void initOption() {
		if (strategy.Oalias) {
			this.bodyMap = new HashMap<String, Production>();
			this.aliasMap = new HashMap<String, String>();
		}
	}

	private NonterminalReference refc = null;

	private void optimize() {
		refc = Productions.countNonterminalReference(grammar);
		Production start = grammar.getStartProduction();
		optimizeProduction(start);
		optimizeChoicePrediction();

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
				verboseInline("inline(deref)", n, deref);
				return this.visitNonTerminal((NonTerminal) deref, a);
			}
			if (deref.size() == 0) {
				verboseInline("inline(deref)", n, deref);
				return deref;
			}
			if (isMultiChar(deref)) {
				verboseInline("multi-char", n, deref);
				return deref;
			}
			if (strategy.Olex && isSingleInstruction(deref)) {
				verboseInline("inline(instruction)", n, deref);
				return deref;
			}
			if (strategy.Olex && isSingleInstruction(deref)) {
				verboseInline("inline(instruction)", n, deref);
				return deref;
			}
		}
		if (strategy.Oinline) {
			int c = refc.count(n.getUniqueName());
			if (c == 1 && !Productions.isRecursive(p)) {
				verboseInline("inline(ref=1)", n, deref);
				return deref;
			}
		}
		String alias = findAliasName(n.getLocalName());
		if (alias != null) {
			NonTerminal nn = n.newNonTerminal(alias);
			verboseInline("inline(alias)", n, nn);
			return nn;
		}
		// verboseInline("*inline(ref=" + refc.count(n.getUniqueName()), n, n);
		return n;
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

	@Override
	public Expression visitPair(Nez.Pair p, Object a) {
		List<Expression> l = Expressions.flatten(p);
		UList<Expression> l2 = Expressions.newList(l.size());
		for (int i = 0; i < l.size(); i++) {
			Expression inner = l.get(i);
			inner = (Expression) inner.visit(this, a);
			l2.add(inner);
		}
		if (strategy.Oorder) {
			while (this.performOutOfOrder(l2))
				;
		}
		this.mergeNotCharacter(l2);
		return p.newPair(l2);
	}

	private boolean performOutOfOrder(UList<Expression> l) {
		boolean res = false;
		for (int i = 1; i < l.size(); i++) {
			Expression first = l.get(i - 1);
			Expression next = l.get(i);
			if (isSingleCharacter(next)) {
				if (first instanceof Nez.BeginTree) {
					((Nez.BeginTree) first).shift -= 1;
					Expressions.swap(l, i - 1, i);
					this.verboseOutofOrdered("out-of-order", next, first);
					res = true;
					continue;
				}
				if (first instanceof Nez.FoldTree) {
					((Nez.FoldTree) first).shift -= 1;
					Expressions.swap(l, i - 1, i);
					this.verboseOutofOrdered("out-of-order", next, first);
					res = true;
					continue;
				}
				if (first instanceof Nez.EndTree) {
					((Nez.EndTree) first).shift -= 1;
					Expressions.swap(l, i - 1, i);
					this.verboseOutofOrdered("out-of-order", next, first);
					res = true;
					continue;
				}
				if (first instanceof Nez.Tag || first instanceof Nez.Replace) {
					Expressions.swap(l, i - 1, i);
					this.verboseOutofOrdered("out-of-order", next, first);
					res = true;
					continue;
				}
			}
		}
		return res;
	}

	private void mergeNotCharacter(UList<Expression> l) {
		for (int i = 1; i < l.size(); i++) {
			Expression first = l.get(i - 1);
			Expression next = l.get(i);
			if (isNotChar(first)) {
				if (next instanceof Nez.Any) {
					l.ArrayValues[i] = convertBitMap(next, first.get(0));
					l.ArrayValues[i - 1] = next.newEmpty();
					this.verboseOptimized("not-any", first, l.ArrayValues[i]);
				}
				if (next instanceof Nez.ByteSet && isNotChar(first)) {
					l.ArrayValues[i] = convertBitMap(next, first.get(0));
					l.ArrayValues[i - 1] = next.newEmpty();
					this.verboseOptimized("not-set", first, l.ArrayValues[i]);
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
		boolean isBinary = false;
		Expression nextNext = Expressions.next(next);
		if (nextNext != null) {
			next = Expressions.first(next);
		}
		if (next instanceof Nez.Any) {
			Nez.Any any = (Nez.Any) next;
			bany = Bytes.newMap(true);
			if (!isBinary) {
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
		return not.newByteSet(isBinary, bany);
	}

	@Override
	public Expression visitLink(Nez.LinkTree p, Object a) {
		if (p.get(0) instanceof Nez.Choice) {
			Expression choice = p.get(0);
			UList<Expression> l = Expressions.newList(choice.size());
			for (Expression inner : choice) {
				inner = this.visitInner(inner, a);
				l.add(Expressions.newLinkTree(p.getSourceLocation(), p.label, inner));
			}
			return choice.newChoice(l);
		}
		return super.visitLink(p, a);
	}

	// private UList<Nez.Choice> choiceList = new UList<Nez.Choice>(new
	// Nez.Choice[10]);

	@Override
	public Expression visitChoice(Nez.Choice p, Object a) {
		// if (p.isOptimized()) {
		UList<Expression> l = Expressions.newList(p.size());
		flattenChoiceList(p, l);
		for (int i = 0; i < l.size(); i++) {
			l.set(i, this.visitInner(l.get(i), a));
		}
		p.inners = l.compactArray();
		//

		Expression optimized = Expressions.tryConvertingByteSet(p);
		if (optimized != p) {
			this.verboseOptimized("choice-to-set", p, optimized);
			return optimized;
		}
		// choiceList.add(p);
		// p.setOptimized();
		// }
		return p;
	}

	/* Choice Prediction */

	private void optimizeChoicePrediction() {
		NonterminalReference refc = Productions.countNonterminalReference(grammar);
		for (Production p : this.grammar) {
			if (refc.count(p.getUniqueName()) > 0) {
				optimizeChoicePrediction(p.getExpression());
			}
		}
	}

	private void optimizeChoicePrediction(Expression e) {
		if (e instanceof Nez.Choice) {
			if (((Nez.Choice) e).predictedCase == null) {
				optimizeChoicePrediction((Nez.Choice) e);
			}
			return;
		}
		for (Expression sub : e) {
			optimizeChoicePrediction(sub);
		}
	}

	private void optimizeChoicePrediction(Nez.Choice choice) {
		int count = 0;
		int selected = 0;
		UList<Expression> newlist = Expressions.newList(choice.size());
		HashMap<String, Expression> map = new HashMap<String, Expression>();
		choice.predictedCase = new Expression[257];
		boolean isTrieTree = true;
		for (int ch = 0; ch <= 255; ch++) {
			Expression predicted = selectChoice(choice, ch, newlist, map);
			choice.predictedCase[ch] = predicted;
			if (predicted != null) {
				count++;
				if (predicted instanceof Nez.Choice) {
					selected += predicted.size();
				} else {
					selected += 1;
				}
				if (!isSingleCharacter(Expressions.next(predicted))) {
					isTrieTree = false;
				}
			}
		}
		choice.isTrieTree = isTrieTree;
		choice.reduced = (float) selected / count;
		choice.firstInners = new Expression[map.size()];
		// Verbose.debug("reduced: " + choiceList.size() + " => " +
		// p.reduced);
		// Verbose.debug("map: " + map);
		int c = 0;
		for (String k : map.keySet()) {
			choice.firstInners[c] = map.get(k);
			c++;
		}
	}

	private void flattenChoiceList(Nez.Choice choice, UList<Expression> l) {
		for (Expression inner : choice) {
			inner = firstChoiceInlining(inner);
			if (inner instanceof Nez.Choice) {
				flattenChoiceList((Nez.Choice) inner, l);
			} else {
				l.add(inner);
			}
		}
	}

	private Expression firstChoiceInlining(Expression e) {
		if (InliningSubchoice && strategy.Oinline) {
			while (e instanceof NonTerminal) {
				NonTerminal n = (NonTerminal) e;
				e = n.getProduction().getExpression();
			}
		}
		return e;
	}

	private Expression selectChoice(Nez.Choice choiceList, int ch, UList<Expression> newlist, HashMap<String, Expression> map) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < choiceList.size(); i++) {
			Expression p = choiceList.get(i);
			ByteAcceptance acc = ByteAcceptance.acc(p, ch);
			if (acc == ByteAcceptance.Reject) {
				continue;
			}
			sb.append(':');
			sb.append(i);
		}
		String key = sb.toString();
		if (key.length() == 0) {
			return null; // empty
		}
		if (map.containsKey(key)) {
			return map.get(key);
		}
		boolean commonFactored = false;
		for (Expression sub : choiceList) {
			ByteAcceptance acc = ByteAcceptance.acc(sub, ch);
			if (acc == ByteAcceptance.Reject) {
				continue;
			}
			if (newlist.size() > 0) {
				int prev = newlist.size() - 1;
				Expression last = newlist.ArrayValues[prev];
				Expression common = tryLeftCommonFactoring(choiceList, last, sub, true);
				if (common != null) {
					newlist.ArrayValues[prev] = common;
					commonFactored = true;
					continue;
				}
			}
			newlist.add(sub);
		}
		Expression p = Expressions.newChoice(choiceList.getSourceLocation(), newlist);
		newlist.clear(0);
		if (commonFactored && !(p instanceof Nez.Choice)) {
			tryFactoredSecondChoice(p);
		}
		map.put(key, p);
		return p;
	}

	private void tryFactoredSecondChoice(Expression p) {
		if (p instanceof Nez.Choice) {
			if (((Nez.Choice) p).firstInners == null) {
				// Verbose.debug("Second choice: " + p);
			}
			return;
		}
		for (Expression sub : p) {
			tryFactoredSecondChoice(sub);
		}
	}

	public final static Expression tryLeftCommonFactoring(Nez.Choice base, Expression e, Expression e2, boolean ignoredFirstChar) {
		UList<Expression> l = null;
		while (e != null && e2 != null) {
			Expression f = Expressions.first(e);
			Expression f2 = Expressions.first(e2);
			if (ignoredFirstChar) {
				ignoredFirstChar = false;
				if (Expression.isByteConsumed(f) && Expression.isByteConsumed(f2)) {
					l = Expressions.newList(4);
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
				l = Expressions.newList(4);
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

	private void verboseReference(String name, int ref) {
		if (this.verboseGrammar) {
			ConsoleUtils.println(name + ": ref=" + ref);
		}
	}

	private void verboseFoundAlias(String name, String alias) {
		if (this.verboseGrammar) {
			ConsoleUtils.println("found alias production: " + name + ", " + alias);
		}
	}

	private String shorten(Expression e) {
		String s = e.toString();
		if (s.length() > 40) {
			return s.substring(0, 40) + " ... ";
		}
		return s;
	}

	private void verboseInline(String name, NonTerminal n, Expression e) {
		// Verbose.println(name + ": " + n.getLocalName() + " => " +
		// shorten(e));
		if (this.verboseGrammar) {
			ConsoleUtils.println(name + ": " + n.getLocalName() + " => " + shorten(e));
		}
	}

	private void verboseOptimized(String msg, Expression e, Expression e2) {
		if (this.verboseGrammar) {
			// Verbose.debug(msg + " => " + e + "\n\t=>" + e2);
			ConsoleUtils.println(msg + ":=> " + shorten(e2));
		}
	}

	private void verboseOutofOrdered(String msg, Expression e, Expression e2) {
		if (this.verboseGrammar) {
			// Verbose.debug(msg + " => " + e + "\n\t=>" + e2);
			ConsoleUtils.println(msg + ":=> " + e + " " + e2);
		}
	}

}
