package nez.lang;

import java.util.HashMap;
import java.util.HashSet;

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
import nez.lang.expr.Pnot;
import nez.lang.expr.Psequence;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.parser.GenerativeGrammar;
import nez.parser.ParseFunc;
import nez.util.UList;

public class GrammarOptimizer extends GrammarRewriter {
	/* local optimizer option */
	boolean enabledCommonLeftFactoring = true; // true;
	boolean enabledCostBasedReduction = true;
	boolean enabledOutOfOrder = false; // bugs!!
	boolean enabledDuplicatedProduction = false;

	GenerativeGrammar gg;
	NezOption option;
	Reporter repo;
	HashMap<String, Integer> optimizedMap = new HashMap<String, Integer>();
	HashMap<String, Production> bodyMap = null;
	HashMap<String, String> aliasMap = null;

	public GrammarOptimizer(GenerativeGrammar gg, NezOption option, Reporter repo) {
		this.gg = gg;
		this.option = option;
		this.repo = repo;
		if (option.enabledPrediction) {
			// seems slow when the prediction option is enabled
			this.enabledCommonLeftFactoring = true;
		}
		if (option.enabledInlining) {
			enabledDuplicatedProduction = true;
			this.bodyMap = new HashMap<String, Production>();
			this.aliasMap = new HashMap<String, String>();
		}
		optimize();
	}

	private void optimize() {
		Production start = gg.getStartProduction();
		optimizeProduction(start);
		countNonTerminal(start.getLocalName());

		UList<Production> prodList = new UList<Production>(new Production[gg.size()]);
		for (Production p : gg) {
			String key = p.getLocalName();
			int refc = optimizedMap.get(key);
			// System.out.println(key + ": ref=" + refc);
			if (refc > 0) {
				ParseFunc f = gg.getParseFunc(key);
				f.update(p.getExpression(), refc);
				prodList.add(p);
			} else {
				gg.removeParseFunc(key);
			}
		}
		gg.updateProductionList(prodList);
	}

	Expression optimizeProduction(Production p) {
		String uname = p.getLocalName();
		if (!optimizedMap.containsKey(uname)) {
			optimizedMap.put(uname, 0);
			Expression inner = inlineNonTerminal(p.getExpression());
			Expression optimized = this.reshapeInner(inner);
			p.setExpression(optimized);
			if (this.enabledDuplicatedProduction) {
				checkDuplicatedProduction(p);
			}
			return optimized;
		}
		return p.getExpression();
	}

	Expression inlineNonTerminal(Expression e) {
		if (this.option.enabledInlining) {
			while (e instanceof NonTerminal) {
				NonTerminal n = (NonTerminal) e;
				e = optimizeProduction(n.getProduction());
			}
		}
		return e;
	}

	void countNonTerminal(String uname) {
		Integer n = optimizedMap.get(uname);
		optimizedMap.put(uname, n + 1);
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
		// if (p.isRecursive()) {
		// return n;
		// }
		// if (option.enabledInlining && p.isInline()) {
		// rewrite("inline", n, optimized);
		// return optimized;
		// }
		if (option.enabledInlining) {
			ParseFunc f = gg.getParseFunc(n.getLocalName());
			if (f.getRefCount() == 1) {
				rewrite("inline(ref=1)", n, deref);
				return deref;
			}
			if (isSingleCharacter(deref)) {
				rewrite("deref", n, deref);
				return deref;
			}
			if (deref instanceof Pempty || deref instanceof Pfail) {
				rewrite("deref", n, deref);
				return deref;
			}
		}
		String alias = alias(n.getLocalName());
		if (alias != null) {
			countNonTerminal(alias);
			return n.newNonTerminal(alias);
		}
		countNonTerminal(n.getLocalName());
		return n;
	}

	// used to test inlining
	public final static boolean isSingleCharacter(Expression e) {
		if (e instanceof Cset || e instanceof Cbyte || e instanceof Cany) {
			return true;
		}
		return false;
	}

	// @Override
	// public Expression reshapePsequence(Psequence p) {
	// Expression e = super.reshapePsequence(p);
	// if (e instanceof Psequence) {
	// Expression first = e.getFirst();
	// Expression next = e.getNext();
	// if (this.enabledOutOfOrder) {
	// if (next instanceof Psequence) {
	// Psequence nextSequence = (Psequence) next;
	// if (isSingleCharacter(nextSequence.first) && isOutOfOrdered(first)) {
	// rewrite_outoforder(first, nextSequence.first);
	// Expression temp = nextSequence.first;
	// nextSequence.first = first;
	// first = temp;
	// }
	// } else {
	// if (isSingleCharacter(next) && isOutOfOrdered(first)) {
	// rewrite_outoforder(first, next);
	// Expression temp = first;
	// first = next;
	// next = temp;
	// }
	// }
	// }
	// if (isNotChar(first)) {
	// Expression optimized = convertBitMap(next, first.get(0));
	// if (optimized != null) {
	// rewrite("not-merge", p, optimized);
	// return optimized;
	// }
	// }
	// }
	// return e;
	// }

	private boolean isOutOfOrdered(Expression e) {
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
		if (!p.isFlatten) {
			p.isFlatten = true;
			UList<Expression> choiceList = ExpressionCommons.newList(p.size());

			flattenChoiceList(p, choiceList, new HashSet<String>());
			Expression optimized = convertByteMap(p, choiceList);
			if (optimized != null) {
				rewrite("choice-map", p, optimized);
				return optimized;
			}
			choiceList = checkTrieTree(choiceList);
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
			Expression c = p.newChoice(choiceList);
			if (c instanceof Pchoice) {
				((Pchoice) c).isFlatten = true;
				((Pchoice) c).predictedCase = p.predictedCase;
			}
			return c;
		}
		return p;
	}

	private void flattenChoiceList(Pchoice choice, UList<Expression> l, HashSet<String> ucheck) {
		for (Expression inner : choice) {
			inner = inlineNonTerminal(inner);
			if (inner instanceof Pchoice) {
				flattenChoiceList((Pchoice) inner, l, ucheck);
			} else {
				inner = reshapeInner(inner);
				String key = inner.toString();
				if (ucheck.contains(key)) {
					repo.reportNotice(inner.getSourcePosition(), "duplicated choice: " + key);
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

	private UList<Expression> checkTrieTree(UList<Expression> l) {
		for (Expression inner : l) {
			if (inner instanceof Cbyte) {
				continue;
			}
			if (inner instanceof Psequence && inner.getFirst() instanceof Cbyte) {
				continue;
			}
			return l;
		}
		Object[] buffers = new Object[257];
		for (Expression inner : l) {
			Cbyte be = (Cbyte) inner.getFirst();
			buffers[be.byteChar] = mergeChoice(buffers[be.byteChar], inner.getNext());
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
				l.add(ExpressionCommons.newPsequence(null, be, ExpressionCommons.newPchoice(null, el)));
			}
		}
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

	private void rewrite_outoforder(Expression e, Expression e2) {
		// Verbose.debug("out-of-order " + e + " <==> " + e2);
	}

	private void rewrite(String msg, Expression e, Expression e2) {
		// Verbose.debug(msg + " " + e + "\n\t=>" + e2);
	}

	private void rewrite_common(Expression e, Expression e2, Expression e3) {
		// Verbose.debug("common (" + e + " / " + e2 + ")\n\t=>" + e3);
	}

	public final void reportError(Expression e, String message) {
		if (repo != null) {
			repo.reportError(e.getSourcePosition(), message);
		}
	}

	public final void reportWarning(Expression e, String message) {
		if (repo != null) {
			repo.reportError(e.getSourcePosition(), message);
		}
	}

	public final void reportNotice(Expression e, String message) {
		if (repo != null) {
			repo.reportError(e.getSourcePosition(), message);
		}
	}
}