package nez.lang;

import java.util.HashMap;
import java.util.HashSet;

import nez.Strategy;
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
import nez.lang.expr.Tlink;
import nez.main.Verbose;
import nez.parser.GenerativeGrammar;
import nez.parser.ParseFunc;
import nez.util.UList;

public class GrammarOptimizer2 extends GrammarRewriter {
	/* local optimizer option */
	boolean enabledCommonLeftFactoring = true; // true;
	boolean enabledCostBasedReduction = true;
	boolean enabledOutOfOrder = false; // bugs!!
	boolean enabledDuplicatedProduction = false;
	boolean enabledLexicalOptimization = false;
	boolean enabledInlining = false;
	boolean enabledSecondChoice = true;

	GenerativeGrammar gg;
	Strategy strategy;
	final HashMap<String, Integer> optimizedMap = new HashMap<String, Integer>();
	HashMap<String, Production> bodyMap = null;
	HashMap<String, String> aliasMap = null;

	public GrammarOptimizer2(GenerativeGrammar gg, Strategy strategy) {
		this.gg = gg;
		this.strategy = strategy;
		if (strategy.isEnabled("Ofirst", Strategy.Ofirst)) {
			// seems slow when the prediction option is enabled
			this.enabledCommonLeftFactoring = true;
			this.toOptimizeChoiceList = new UList<Pchoice>(new Pchoice[8]);
		}
		if (strategy.isEnabled("Oinline", Strategy.Oinline)) {
			this.enabledInlining = true;
			enabledDuplicatedProduction = true;
			this.bodyMap = new HashMap<String, Production>();
			this.aliasMap = new HashMap<String, String>();
		}
		if (strategy.isEnabled("Olex", Strategy.Olex)) {
			this.enabledLexicalOptimization = true;
		}
		optimize();
	}

	private void optimize() {
		Production start = gg.getStartProduction();
		optimizeProduction(start);
		this.optimizeFirstChoice();
		this.optimizedMap.clear();
		this.resetReferenceCount();
		gg.getParseFunc(start.getLocalName()).incCount();
		this.recheckReference(start);
		UList<Production> prodList = new UList<Production>(new Production[gg.size()]);
		for (Production p : gg) {
			String key = p.getLocalName();
			ParseFunc f = gg.getParseFunc(key);
			// Verbose.debug(key + ": ref=" + f.getRefCount());
			if (f.getRefCount() > 0) {
				prodList.add(p);
			} else {
				gg.removeParseFunc(key);
			}
		}
		gg.updateProductionList(prodList);
	}

	private void resetReferenceCount() {
		for (Production p : gg) {
			String key = p.getLocalName();
			ParseFunc f = gg.getParseFunc(key);
			f.resetCount();
		}
	}

	private void recheckReference(Production start) {
		String key = start.getLocalName();
		if (!this.optimizedMap.containsKey(key)) {
			this.optimizedMap.put(key, 0);
			recheckReference(start.getExpression());
		}
	}

	private void recheckReference(Expression e) {
		if (e instanceof NonTerminal) {
			ParseFunc f = gg.getParseFunc(((NonTerminal) e).getLocalName());
			f.incCount();
			recheckReference(((NonTerminal) e).getProduction());
			return;
		}
		if (e instanceof Pchoice && ((Pchoice) e).firstInners != null) {
			Pchoice choice = (Pchoice) e;
			for (Expression sub : choice.firstInners) {
				recheckReference(sub);
			}
			return;
		}
		for (Expression sub : e) {
			recheckReference(sub);
		}
	}

	private Expression optimizeProduction(Production p) {
		assert (p.getGrammar() == this.gg);
		String uname = p.getLocalName();
		if (!optimizedMap.containsKey(uname)) {
			optimizedMap.put(uname, 0);
			Expression optimized = this.reshapeInner(p.getExpression());
			p.setExpression(optimized);
			if (this.enabledDuplicatedProduction) {
				checkDuplicatedProduction(p);
			}
			return optimized;
		}
		return p.getExpression();
	}

	Expression inlineNonTerminal2(Expression e) {
		if (this.enabledInlining) {
			while (e instanceof NonTerminal) {
				NonTerminal n = (NonTerminal) e;
				e = optimizeProduction(n.getProduction());
			}
		}
		return e;
	}

	void checkDuplicatedProduction(Production p) {
		String key = p.getExpression().toString();
		Production p2 = bodyMap.get(key);
		if (p2 == null) {
			bodyMap.put(key, p);
			return;
		}
		aliasMap.put(p.getLocalName(), p2.getLocalName());
		// Verbose.debug("duplicated: " + p.getLocalName() + " " +
		// p2.getLocalName() + "\n\t" + key);
	}

	String alias(String nname) {
		if (aliasMap != null) {
			String alias = aliasMap.get(nname);
			if (alias != null) {
				return alias;
			}
		}
		return null;
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal n) {
		Production p = n.getProduction();
		Expression deref = optimizeProduction(p);
		ParseFunc f = gg.getParseFunc(n.getLocalName());
		if (this.enabledInlining) {
			if (f.getRefCount() == 1) {
				assert (!p.isRecursive());
				// reportInfo("inline(ref=1)", n, deref);
				return deref;
			}
			if (deref instanceof NonTerminal) {
				// reportInfo("deref", n, deref);
				return this.reshapeNonTerminal((NonTerminal) deref);
			}
			if (deref instanceof Pempty || deref instanceof Pfail) {
				// reportInfo("deref", n, deref);
				return deref;
			}
			if (isSingleCharacter(deref)) {
				// reportInfo("deref", n, deref);
				return deref;
			}
			if (isPairCharacter(deref)) {
				reportInfo("deref(pair)", n, deref);
				return deref;
			}
			if (this.enabledLexicalOptimization && isSingleInstruction(deref)) {
				// reportInfo("deref", n, deref);
				return deref;
			}
		}
		String alias = alias(n.getLocalName());
		if (alias != null) {
			NonTerminal nn = n.newNonTerminal(alias);
			reportInfo("alias", n, nn);
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
	public Expression reshapePsequence(Psequence p) {
		UList<Expression> l = p.toList();
		UList<Expression> l2 = ExpressionCommons.newList(l.size());
		for (int i = 0; i < l.size(); i++) {
			Expression inner = l.ArrayValues[i];
			push(inner);
			inner = inner.reshape(this);
			l2.add(inner);
		}
		// System.out.println("l1" + l);
		// System.out.println("l2" + l2);
		for (int i = l.size() - 1; i >= 0; i--) {
			pop(l.ArrayValues[i]);
		}
		if (this.enabledOutOfOrder) {
			// // if (next instanceof Psequence) {
			// // Psequence nextSequence = (Psequence) next;
			// // if (isSingleCharacter(nextSequence.first) &&
			// isOutOfOrdered(first)) {
			// // rewrite_outoforder(first, nextSequence.first);
			// // Expression temp = nextSequence.first;
			// // nextSequence.first = first;
			// // first = temp;
			// // }
			// // } else {
			// // if (isSingleCharacter(next) && isOutOfOrdered(first)) {
			// // rewrite_outoforder(first, next);
			// // Expression temp = first;
			// // first = next;
			// // next = temp;
			// // }
			// // }
		}
		for (int i = 1; i < l.size(); i++) {
			Expression first = l.get(i - 1);
			Expression next = l.get(i);
			if (isNotChar(first)) {
				if (next instanceof Cany) {
					l.ArrayValues[i] = convertBitMap(next, first.get(0));
					l.ArrayValues[i - 1] = p.newEmpty();
					// if (optimized != null) {
					// rewrite("not-merge", p, optimized);
					// return optimized;
					// }
				}
				if (next instanceof Cset && isNotChar(first)) {
					l.ArrayValues[i] = convertBitMap(next, first.get(0));
					l.ArrayValues[i - 1] = p.newEmpty();
				}
			}
		}
		return p.newSequence(l2);
	}

	// private boolean isOutOfOrdered(Expression e) {
	// if (e instanceof Ttag) {
	// return true;
	// }
	// if (e instanceof Treplace) {
	// return true;
	// }
	// if (e instanceof Tnew) {
	// ((Tnew) e).shift -= 1;
	// return true;
	// }
	// if (e instanceof Tcapture) {
	// ((Tcapture) e).shift -= 1;
	// return true;
	// }
	// return false;
	// }

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
			isBinary = any.isBinary();
			bany = Cset.newMap(true);
			if (!isBinary) {
				bany[0] = false;
			}
		}
		if (next instanceof Cset) {
			Cset bm = (Cset) next;
			isBinary = bm.isBinary();
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
		return not.newByteMap(isBinary, bany);
	}

	@Override
	public Expression reshapeTlink(Tlink p) {
		if (p.get(0) instanceof Pchoice) {
			Expression choice = p.get(0);
			UList<Expression> l = ExpressionCommons.newList(choice.size());
			for (Expression inner : choice) {
				inner = this.reshapeInner(inner);
				l.add(ExpressionCommons.newTlink(p.getSourcePosition(), p.getLabel(), inner));
			}
			return choice.newChoice(l);
		}
		return super.reshapeTlink(p);
	}

	@Override
	public Expression reshapePchoice(Pchoice p) {
		UList<Expression> l = ExpressionCommons.newList(p.size());
		for (Expression sub : p) {
			ExpressionCommons.addChoice(l, this.reshapeInner(sub));
		}
		return this.reshapePchoice(p, l);
	}

	public Expression reshapePchoice(Pchoice p, UList<Expression> l) {
		Expression optimized = canConvertToCset(p, l);
		if (optimized != null) {
			// reportInfo("choice-to-set", p, optimized);
			return optimized;
		}
		l = checkTrieTree(p, l);
		if (l.size() == 1) {
			reportInfo("single-choice", p, l.ArrayValues[0]);
			return l.ArrayValues[0];
		}
		Expression n = ExpressionCommons.newPchoice(p.getSourcePosition(), l);
		if (n instanceof Pchoice) {
			((Pchoice) n).isTrieTree = p.isTrieTree;
			addChoiceToOptimizeList((Pchoice) n);
		}
		return n;
	}

	private Expression canConvertToCset(Pchoice choice, UList<Expression> choiceList) {
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

	private boolean isTrieTreeHead(Expression inner) {
		Expression first = inner.getFirst();
		if (first instanceof Cbyte || first instanceof Cset) {
			return true;
		}
		// if (inner instanceof Cbyte) {
		// return true;
		// }
		// if (inner instanceof Psequence && inner.getFirst() instanceof Cbyte)
		// {
		// return true;
		// }
		return false;
	}

	private UList<Expression> checkTrieTree(Pchoice choice, UList<Expression> l) {
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
			return this.reshapePchoice((Pchoice) e, el);
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
			flattenChoiceList(p, choiceList, new HashSet<String>());
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

	private void flattenChoiceList(Pchoice choice, UList<Expression> l, HashSet<String> ucheck) {
		for (Expression inner : choice) {
			inner = reshapeInner(inner);
			// inner = inlineNonTerminal(inner); // FIXME
			if (inner instanceof Pchoice) {
				flattenChoiceList((Pchoice) inner, l, ucheck);
			} else {
				// inner = reshapeInner(inner);
				String key = inner.toString();
				if (ucheck.contains(key)) {
					strategy.reportNotice(inner.getSourcePosition(), "duplicated choice: " + key);
					continue;
				}
				ucheck.add(key);
				// if (l.size() > 0 && this.enabledCommonLeftFactoring) {
				// Expression lastExpression = l.ArrayValues[l.size() - 1];
				// Expression first = lastExpression.getFirst();
				// if (first.equalsExpression(inner.getFirst())) {
				// Expression next =
				// lastExpression.newChoice(lastExpression.getNext(),
				// inner.getNext());
				// Expression common = lastExpression.newSequence(first, next);
				// rewrite_common(lastExpression, inner, common);
				// l.ArrayValues[l.size() - 1] = common;
				// continue;
				// }
				// }
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
		Expression p = ExpressionCommons.newPchoice(choice.getSourcePosition(), newlist);
		newlist.clear(0);
		// if (commonFactored && !(p instanceof Pchoice)) {
		// tryFactoredSecondChoice(p);
		// }
		map.put(key, p);
		return p;
	}

	private void tryFactoredSecondChoice(Expression p) {
		if (p instanceof Pchoice) {
			if (((Pchoice) p).firstInners == null) {
				Verbose.debug("second choice: " + p);
			}
			return;
		}
		for (Expression sub : p) {
			tryFactoredSecondChoice(p);
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
			if (!f.equalsExpression(f2)) {
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

	// private void rewrite_outoforder(Expression e, Expression e2) {
	// // Verbose.debug("out-of-order " + e + " <==> " + e2);
	// }

	private void reportInfo(String msg, Expression e, Expression e2) {
		Verbose.debug(msg + " " + e + "\n\t=>" + e2);
	}

	//
	// private void rewrite_common(Expression e, Expression e2, Expression e3) {
	// // Verbose.debug("common (" + e + " / " + e2 + ")\n\t=>" + e3);
	// }

	public final void reportError(Expression e, String message) {
		strategy.reportError(e.getSourcePosition(), message);
	}

	public final void reportWarning(Expression e, String message) {
		strategy.reportError(e.getSourcePosition(), message);
	}

	public final void reportNotice(Expression e, String message) {
		strategy.reportError(e.getSourcePosition(), message);
	}
}
