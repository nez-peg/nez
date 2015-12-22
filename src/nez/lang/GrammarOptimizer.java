package nez.lang;

import java.util.HashMap;
import java.util.HashSet;

import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.parser.ParseFunc;
import nez.parser.ParserGrammar;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class GrammarOptimizer extends GrammarRewriter {
	/* local optimizer option */

	// boolean enabledLexicalOptimization = false;
	// boolean enabledInlining = false;
	// boolean enabledAliasAnalysis = false;
	// boolean enabledOutOfOrderConstruction = true;
	//
	// boolean enabledTrieTreeChoice = false;
	// boolean enabledCommonLeftFactoring = false; // true;
	//
	// boolean enabledFirstChoice = false;
	// boolean enabledfirstChoiceInlining = false;
	// boolean enabledEmptyChoice = false;
	//
	// boolean verboseOption = false;
	boolean verboseGrammar = false;
	boolean enabledSecondChoice = false;
	// boolean verboseDebug = false;

	final ParserGrammar grammar;
	final ParserStrategy strategy;
	final HashSet<String> optimizedMap = new HashSet<String>();
	HashMap<String, Production> bodyMap = null;
	HashMap<String, String> aliasMap = null;

	public GrammarOptimizer(ParserGrammar gg, ParserStrategy strategy) {
		this.grammar = gg;
		this.strategy = strategy;
		initOption();
		optimize();
	}

	private void initOption() {
		// if (ConsoleUtils.isDebug()) {
		// this.verboseDebug = true;
		// }
		// if (strategy.isEnabled("Doption", ParserStrategy.Doption) ||
		// ConsoleUtils.isDebug()) {
		// this.verboseOption = true;
		// }
		// if (strategy.isEnabled("Dgrammar", ParserStrategy.Dgrammar)) {
		// this.verboseGrammar = true;
		// }
		if (strategy.Oalias) {
			this.bodyMap = new HashMap<String, Production>();
			this.aliasMap = new HashMap<String, String>();
		}
		if (strategy.Odchoice) {
			this.toOptimizeChoiceList = new UList<Pchoice>(new Pchoice[8]);
		}

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

	private void optimize() {
		Production start = grammar.getStartProduction();
		optimizeProduction(start);

		this.optimizeFirstChoice();
		this.optimizedMap.clear();
		this.resetReferenceCount();

		grammar.getParseFunc(start.getLocalName()).incCount();
		this.recheckReference(start);

		UList<Production> prodList = new UList<Production>(new Production[grammar.size()]);
		for (Production p : grammar) {
			String key = p.getLocalName();
			ParseFunc f = grammar.getParseFunc(key);
			verboseReference(key, f.getCount());
			if (f.getCount() > 0 || p.isSymbolTable()) {
				prodList.add(p);
			} else {
				grammar.removeParseFunc(key);
			}
		}
		grammar.updateProductionList(prodList);
	}

	private void resetReferenceCount() {
		for (Production p : grammar) {
			String key = p.getLocalName();
			ParseFunc f = grammar.getParseFunc(key);
			f.resetCount();
		}
	}

	private void recheckReference(Production start) {
		String key = start.getLocalName();
		if (!this.optimizedMap.contains(key)) {
			this.optimizedMap.add(key);
			recheckReference(start.getExpression());
		}
	}

	private void recheckReference(Expression e) {
		if (e instanceof NonTerminal) {
			ParseFunc f = grammar.getParseFunc(((NonTerminal) e).getLocalName());
			f.incCount();
			recheckReference(((NonTerminal) e).getProduction());
			return;
		}
		if (e instanceof Pchoice && ((Pchoice) e).firstInners != null) {
			Pchoice choice = (Pchoice) e;
			for (Expression sub : choice.firstInners) {
				recheckReference(sub);
			}
			// return; // FIXME: when enableFirstInline
		}
		for (Expression sub : e) {
			recheckReference(sub);
		}
	}

	private Expression optimizeProduction(Production p) {
		assert (p.getGrammar() == this.grammar);
		String uname = p.getLocalName();
		if (!optimizedMap.contains(uname)) {
			optimizedMap.add(uname);
			Expression optimized = this.visitInner(p.getExpression());
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
			ParseFunc f = grammar.getParseFunc(n.getLocalName());
			if (f.getCount() == 1) {
				assert (!p.isRecursive());
				verboseInline("inline(ref=1)", n, deref);
				return deref;
			}
			if (deref instanceof NonTerminal) {
				verboseInline("inline(deref)", n, deref);
				return this.visitNonTerminal((NonTerminal) deref, a);
			}
			if (deref instanceof Pempty || deref instanceof Pfail) {
				verboseInline("inline(deref)", n, deref);
				return deref;
			}
			if (isSingleCharacter(deref)) {
				verboseInline("inline(char)", n, deref);
				return deref;
			}
			if (isPairCharacter(deref)) {
				verboseInline("inline(char,char)", n, deref);
				return deref;
			}
			if (strategy.Olex && isSingleInstruction(deref)) {
				verboseInline("inline(instruction)", n, deref);
				return deref;
			}
		}
		String alias = findAliasName(n.getLocalName());
		if (alias != null) {
			NonTerminal nn = n.newNonTerminal(alias);
			verboseInline("inline(alias)", n, nn);
			return nn;
		}
		return n;
	}

	// used to test inlining
	public final static boolean isSingleCharacter(Expression e) {
		if (e instanceof Cset || e instanceof Cbyte || e instanceof Cany) {
			return true;
		}
		return false;
	}

	// used to test inlining
	public final static boolean isSingleInstruction(Expression e) {
		if (e instanceof Pnot || e instanceof Pzero || e instanceof Poption) {
			return isSingleCharacter(e.get(0));
		}
		return false;
	}

	// used to test inlining
	public final static boolean isPairCharacter(Expression e) {
		if (e instanceof Psequence && isSingleCharacter(e.getFirst()) && isSingleCharacter(e.getNext())) {
			return true;
		}
		return false;
	}

	@Override
	public Expression visitPair(Nez.Pair p, Object a) {
		UList<Expression> l = p.toList();
		UList<Expression> l2 = ExpressionCommons.newList(l.size());
		for (int i = 0; i < l.size(); i++) {
			Expression inner = l.ArrayValues[i];
			inner = (Expression) inner.visit(this, a);
			l2.add(inner);
		}
		if (strategy.Oorder) {
			while (this.performOutOfOrder(l2))
				;
		}
		this.mergeNotCharacter(l2);
		return p.newSequence(l2);
	}

	private boolean performOutOfOrder(UList<Expression> l) {
		boolean res = false;
		for (int i = 1; i < l.size(); i++) {
			Expression first = l.get(i - 1);
			Expression next = l.get(i);
			if (isSingleCharacter(next)) {
				if (first instanceof Tnew) {
					((Tnew) first).shift -= 1;
					ExpressionCommons.swap(l, i - 1, i);
					this.verboseOutofOrdered("out-of-order", next, first);
					res = true;
					continue;
				}
				if (first instanceof Tlfold) {
					((Tlfold) first).shift -= 1;
					ExpressionCommons.swap(l, i - 1, i);
					this.verboseOutofOrdered("out-of-order", next, first);
					res = true;
					continue;
				}
				if (first instanceof Tcapture) {
					((Tcapture) first).shift -= 1;
					ExpressionCommons.swap(l, i - 1, i);
					this.verboseOutofOrdered("out-of-order", next, first);
					res = true;
					continue;
				}
				if (first instanceof Ttag || first instanceof Treplace) {
					ExpressionCommons.swap(l, i - 1, i);
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
				if (next instanceof Cany) {
					l.ArrayValues[i] = convertBitMap(next, first.get(0));
					l.ArrayValues[i - 1] = next.newEmpty();
					this.verboseOptimized("not-any", first, l.ArrayValues[i]);
				}
				if (next instanceof Cset && isNotChar(first)) {
					l.ArrayValues[i] = convertBitMap(next, first.get(0));
					l.ArrayValues[i - 1] = next.newEmpty();
					this.verboseOptimized("not-set", first, l.ArrayValues[i]);
				}
			}
		}
	}

	private boolean isNotChar(Expression p) {
		if (p instanceof Pnot) {
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
			bany = Cset.newMap(true);
			if (!isBinary) {
				bany[0] = false;
			}
		}
		if (next instanceof Cset) {
			Cset bm = (Cset) next;
			bany = bm.byteMap.clone();
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
		return not.newCset(isBinary, bany);
	}

	@Override
	public Expression visitLink(Nez.Link p, Object a) {
		if (p.get(0) instanceof Pchoice) {
			Expression choice = p.get(0);
			UList<Expression> l = ExpressionCommons.newList(choice.size());
			for (Expression inner : choice) {
				inner = this.visitInner(inner);
				l.add(ExpressionCommons.newTlink(p.getSourceLocation(), p.getLabel(), inner));
			}
			return choice.newChoice(l);
		}
		return super.visitLink(p, a);
	}

	@Override
	public Expression visitChoice(Nez.Choice p, Object a) {
		if (!p.isOptimized()) {
			p.setOptimized();
			UList<Expression> l = ExpressionCommons.newList(p.size());
			for (Expression sub : p) {
				ExpressionCommons.addChoice(l, this.visitInner(sub));
			}
			return this.visitChoice(p, l);
		}
		return p;
	}

	public Expression visitChoice(Nez.Choice p, UList<Expression> l) {
		Expression optimized = canConvertToCset(p, l);
		if (optimized != null) {
			this.verboseOptimized("choice-to-set", p, optimized);
			return optimized;
		}
		l = checkTrieTree(p, l);
		if (l.size() == 1) {
			verboseOptimized("single-choice", p, l.ArrayValues[0]);
			return l.ArrayValues[0];
		}
		Expression n = ExpressionCommons.newPchoice(p.getSourceLocation(), l);
		if (n instanceof Pchoice) {
			((Pchoice) n).isTrieTree = p.isTrieTree;
			addChoiceToOptimizeList((Pchoice) n);
		}
		return n;
	}

	private Expression canConvertToCset(Nez.Choice choice, UList<Expression> choiceList) {
		boolean byteMap[] = Cset.newMap(false);
		boolean binary = false;
		for (Expression e : choiceList) {
			e = ExpressionCommons.resolveNonTerminal(e);
			if (e instanceof Cbyte) {
				byteMap[((Cbyte) e).byteChar] = true;
				// if (((Cbyte) e).isBinary()) {
				// binary = true;
				// }
				continue;
			}
			if (e instanceof Cset) {
				Cset.appendBitMap(byteMap, ((Cset) e).byteMap);
				continue;
			}
			if (e instanceof Cany) {
				return e;
			}
			return null;
		}
		return choice.newCset(binary, byteMap);
	}

	private boolean isTrieTreeHead(Expression inner) {
		Expression first = inner.getFirst();
		if (first instanceof Cbyte || first instanceof Cset) {
			return true;
		}
		return false;
	}

	private UList<Expression> checkTrieTree(Nez.Choice choice, UList<Expression> l) {
		for (Expression inner : l) {
			if (isTrieTreeHead(inner)) {
				continue;
			}
			return l;
		}
		Object[] buffers = new Object[257];
		for (Expression inner : l) {
			Expression first = inner.getFirst();
			if (first instanceof Cbyte) {
				Cbyte be = (Cbyte) first;
				buffers[be.byteChar] = mergeChoice(buffers[be.byteChar], inner.getNext());
			} else {
				Cset bs = (Cset) first;
				for (int ch = 0; ch < buffers.length; ch++) {
					if (bs.byteMap[ch]) {
						buffers[ch] = mergeChoice(buffers[ch], inner.getNext());
					}
				}
			}
		}
		l = new UList<Expression>(new Expression[8]);
		for (int ch = 0; ch < buffers.length; ch++) {
			if (buffers[ch] == null)
				continue;
			@SuppressWarnings("unchecked")
			UList<Expression> el = (UList<Expression>) buffers[ch];
			Expression be = ExpressionCommons.newCbyte(null, false, ch);
			if (el.size() == 1) {
				l.add(ExpressionCommons.newPsequence(null, be, el.get(0)));
			} else {
				Expression next = trySecondChoice(ExpressionCommons.newPchoice(null, el), el);
				l.add(ExpressionCommons.newPsequence(null, be, next));
			}
		}
		choice.isTrieTree = true;
		return l;
	}

	private UList<Expression> mergeChoice(Object e1, Expression e2) {
		if (e2 == null) {
			e2 = ExpressionCommons.newEmpty(null);
		}
		@SuppressWarnings("unchecked")
		UList<Expression> l = (UList<Expression>) e1;
		if (l == null) {
			l = new UList<Expression>(new Expression[2]);
		}
		ExpressionCommons.addChoice(l, e2);
		return l;
	}

	private Expression trySecondChoice(Expression e, UList<Expression> el) {
		if (this.enabledSecondChoice && e instanceof Pchoice) {
			return this.visitChoice((Pchoice) e, el);
		}
		return e;
	}

	private UList<Pchoice> toOptimizeChoiceList = null;

	private void addChoiceToOptimizeList(Pchoice n) {
		if (toOptimizeChoiceList != null) {
			toOptimizeChoiceList.add(n);
		}
	}

	private void optimizeFirstChoice() {
		if (toOptimizeChoiceList != null) {
			for (Pchoice p : this.toOptimizeChoiceList) {
				optimizeFirstChoice(p);
			}
		}
	}

	private void optimizeFirstChoice(Pchoice p) {
		if (p.isTrieTree) {
			p.predictedCase = new Expression[257];
			p.firstInners = new Expression[p.size()];
			int c = 0;
			for (Expression sub : p) {
				Cbyte be = (Cbyte) sub.getFirst();
				p.predictedCase[be.byteChar] = sub;
				p.firstInners[c] = sub;
				c++;
			}
			p.reduced = 1.0f;
		} else {
			UList<Expression> choiceList = ExpressionCommons.newList(p.size());
			flattenChoiceList(p, choiceList);
			int count = 0;
			int selected = 0;
			UList<Expression> newlist = ExpressionCommons.newList(p.size());
			HashMap<String, Expression> map = new HashMap<String, Expression>();
			p.predictedCase = new Expression[257];
			boolean isTrieTree = true;
			for (int ch = 0; ch <= 255; ch++) {
				Expression predicted = selectChoice(p, choiceList, ch, newlist, map);
				p.predictedCase[ch] = predicted;
				if (predicted != null) {
					count++;
					if (predicted instanceof Pchoice) {
						selected += predicted.size();
					} else {
						selected += 1;
					}
					if (!isSingleCharacter(predicted.getFirst())) {
						isTrieTree = false;
					}
				}
			}
			p.isTrieTree = isTrieTree;
			p.reduced = (float) selected / count;
			p.firstInners = new Expression[map.size()];
			// Verbose.debug("reduced: " + choiceList.size() + " => " +
			// p.reduced);
			// Verbose.debug("map: " + map);
			int c = 0;
			for (String k : map.keySet()) {
				p.firstInners[c] = map.get(k);
				c++;
			}
		}
	}

	private Expression firstChoiceInlining(Expression e) {
		// if (this.enabledfirstChoiceInlining) {
		while (e instanceof NonTerminal) {
			NonTerminal n = (NonTerminal) e;
			e = n.getProduction().getExpression();
		}
		// }
		return e;
	}

	private void flattenChoiceList(Pchoice choice, UList<Expression> l) {
		for (Expression inner : choice) {
			inner = firstChoiceInlining(inner);
			if (inner instanceof Pchoice) {
				flattenChoiceList((Pchoice) inner, l);
			} else {
				l.add(inner);
			}
		}
	}

	// OptimizerLibrary

	private Expression selectChoice(Pchoice choice, UList<Expression> choiceList, int ch, UList<Expression> newlist, HashMap<String, Expression> map) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < choiceList.size(); i++) {
			Expression p = choiceList.ArrayValues[i];
			short r = p.acceptByte(ch);
			if (r == PossibleAcceptance.Reject) {
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
			short r = sub.acceptByte(ch);
			if (r == PossibleAcceptance.Reject) {
				continue;
			}
			if (newlist.size() > 0) {
				int prev = newlist.size() - 1;
				Expression last = newlist.ArrayValues[prev];
				Expression common = tryLeftCommonFactoring(choice, last, sub, true);
				if (common != null) {
					newlist.ArrayValues[prev] = common;
					commonFactored = true;
					continue;
				}
			}
			newlist.add(sub);
		}
		Expression p = ExpressionCommons.newPchoice(choice.getSourceLocation(), newlist);
		newlist.clear(0);
		if (commonFactored && !(p instanceof Pchoice)) {
			tryFactoredSecondChoice(p);
		}
		map.put(key, p);
		return p;
	}

	private void tryFactoredSecondChoice(Expression p) {
		if (p instanceof Pchoice) {
			if (((Pchoice) p).firstInners == null) {
				// Verbose.debug("Second choice: " + p);
			}
			return;
		}
		for (Expression sub : p) {
			tryFactoredSecondChoice(sub);
		}
	}

	public final static Expression tryLeftCommonFactoring(Pchoice base, Expression e, Expression e2, boolean ignoredFirstChar) {
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
			if (!f.equals(f2)) {
				break;
			}
			if (l == null) {
				l = ExpressionCommons.newList(4);
			}
			l.add(f);
			e = e.getNext();
			e2 = e2.getNext();
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
