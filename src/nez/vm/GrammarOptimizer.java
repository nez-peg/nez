package nez.vm;

import nez.ast.SourcePosition;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarFactory;
import nez.lang.GrammarReshaper;
import nez.lang.Link;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Prediction;
import nez.lang.Production;
import nez.lang.Sequence;
import nez.util.UFlag;
import nez.util.UList;

public class GrammarOptimizer extends GrammarReshaper {

	int option;

//	Grammar grammar = null;

	public GrammarOptimizer(int option) {
		this.option = option;
	}

	public final Expression optimize(Production p) {
		return resolveNonTerminal(p.getExpression()).reshape(this);
	}

	// used to test inlining
	public final static boolean isCharacterTerminal(Expression e) {
		if(e instanceof ByteMap || e instanceof ByteChar || e instanceof AnyChar) {
			return true;
		}
		return false;
	}

	@Override
	public Expression reshapeSequence(Sequence parentExpression) {
		UList<Expression> l = new UList<Expression>(new Expression[parentExpression.size()]);
		for(Expression subExpression : parentExpression) {
			GrammarFactory.addSequence(l, subExpression.reshape(this));
		}
//		reorderSequence(l);  // FIXME
		if(UFlag.is(option, Grammar.Optimization)) {
			int loc = findNotAny(0, l);
			if(loc != -1) {
				UList<Expression> nl = new UList<Expression>(new Expression[l.size()]);
				joinNotAny(0, loc, l, nl);
				l = nl;
			}
		}
		return GrammarFactory.newSequence(parentExpression.getSourcePosition(), l);
	}

	/**
	 * Sequence otimization
	 * // #t 'a' 'b' => 'a' #t 'b'
	 */

	private void reorderSequence(UList<Expression> l) {
		for(int i = 1; i < l.size(); i++) {
			Expression p = l.ArrayValues[i - 1];
			Expression e = l.ArrayValues[i];
			if(Expression.isByteConsumed(e)) { // #t 'a' 'b' => 'a' #t 'b'
				if(Expression.isPositionIndependentOperation(p)) {
					l.ArrayValues[i - 1] = e;
					l.ArrayValues[i] = p;
					continue;
				}
				if(p instanceof New) {
					New n = (New) p;
					l.ArrayValues[i - 1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] = GrammarFactory.newNew(n.getSourcePosition(), n.lefted, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i] = n;
					}
					continue;
				}
				if(p instanceof Capture) {
					Capture n = (Capture) p;
					l.ArrayValues[i - 1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] = GrammarFactory.newCapture(n.getSourcePosition(), n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i] = n;
					}
					continue;
				}
			}
		}
	}

	private int findNotAny(int s, UList<Expression> l) {
		for(int i = s; i < l.size(); i++) {
			Expression p = l.ArrayValues[i];
			if(p instanceof Not) {
				if(findAny(i, l) != -1) {
					return i;
				}
			}
		}
		return -1;
	}

	private int findAny(int s, UList<Expression> l) {
		for(int i = s; i < l.size(); i++) {
			Expression p = l.ArrayValues[i];
			if(p instanceof Not) {
				continue;
			}
			if(p instanceof AnyChar) {
				return i;
			}
			break;
		}
		return -1;
	}

	private void joinNotAny(int s, int loc, UList<Expression> l, UList<Expression> nl) {
		for(int i = s; i < loc; i++) {
			nl.add(l.ArrayValues[i]);
		}
		int e = findAny(loc, l);
		assert (e != -1);
		Not not = (Not) l.ArrayValues[loc];
		AnyChar any = (AnyChar) l.ArrayValues[e];
		if(loc + 1 < e) {
			UList<Expression> sl = new UList<Expression>(new Expression[4]);
			for(int i = loc; i < e; i++) {
				GrammarFactory.addChoice(sl, l.ArrayValues[i]);
			}
			not = GrammarFactory.newNot(not.getSourcePosition(), GrammarFactory.newChoice(not.getSourcePosition(), sl).reshape(this));
		}
		if(not.get(0) instanceof ByteChar) {
			boolean[] byteMap = ByteMap.newMap(true);
			byteMap[((ByteChar) not.get(0)).byteChar] = false;
			if(!UFlag.is(option, Grammar.Binary)) {
				byteMap[0] = false;
			}
			nl.add(GrammarFactory.newByteMap(not.getSourcePosition(), byteMap));
		}
		else if(not.get(0) instanceof ByteMap) {
			boolean[] byteMap = ByteMap.newMap(false);
			ByteMap.appendBitMap(byteMap, ((ByteMap) not.get(0)).byteMap);
			ByteMap.reverse(byteMap, option);
			nl.add(GrammarFactory.newByteMap(not.getSourcePosition(), byteMap));
		}
		else {
			nl.add(not);
			nl.add(any);
		}
		loc = findNotAny(e + 1, l);
		if(loc != -1) {
			joinNotAny(e + 1, loc, l, nl);
			return;
		}
		for(int i = e + 1; i < l.size(); i++) {
			nl.add(l.ArrayValues[i]);
		}
	}

	public Expression reshapeLink(Link p) {
		if(p.get(0) instanceof Choice) {
			Expression inner = p.get(0);
			UList<Expression> l = new UList<Expression>(new Expression[inner.size()]);
			for(Expression subChoice : inner) {
				subChoice = subChoice.reshape(this);
				l.add(GrammarFactory.newLink(p.getSourcePosition(), subChoice, p.index));
			}
			return GrammarFactory.newChoice(inner.getSourcePosition(), l);
		}
		return super.reshapeLink(p);
	}

	@Override
	public Expression reshapeChoice(Choice p) {
		if(p.predictedCase == null) {
			UList<Expression> choiceList = new UList<Expression>(new Expression[p.size()]);
			flattenChoiceList(p, choiceList);
			if(UFlag.is(option, Grammar.Optimization)) {
				Expression o = newOptimizedByteMap(p.getSourcePosition(), choiceList);
				if(o != null) {
					return o;
				}
			}
			if(UFlag.is(option, Grammar.Prediction)) {
				p.predictedCase = new Expression[257];
				for(int ch = 0; ch <= 256; ch++) {
					p.predictedCase[ch] = selectChoice(p, choiceList, ch);
				}
				Expression singleChoice = null;
				for(int ch = 0; ch <= 256; ch++) {
					if(p.predictedCase[ch] != null) {
						if(singleChoice != null) {
							singleChoice = null;
							break;
						}
						singleChoice = p.predictedCase[ch];
					}
				}
				if(singleChoice != null) {
					return singleChoice;
				}
				//System.out.println("PREDICTED: " + p);
			}
		}
		return p;
	}

	private void flattenChoiceList(Choice parentExpression, UList<Expression> l) {
		for(Expression subExpression : parentExpression) {
			subExpression = resolveNonTerminal(subExpression);
			if(subExpression instanceof Choice) {
				flattenChoiceList((Choice) subExpression, l);
			}
			else {
				subExpression = subExpression.reshape(this);
				if(subExpression instanceof Sequence) {

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

//	public final static Expression resolveSequenceNonTerminal(Expression e) {
//		while(e instanceof Sequence) {
//			Sequence s = (Sequence) e;
//			Expression p = s.get(0);
//			if(p instanceof NonTerminal) {
//				p = resolveNonTerminal(p);
//			}
//			e = nterm.deReference();
//		}
//		return e;
//	}

	// OptimizerLibrary

	public final static Expression newOptimizedByteMap(SourcePosition s, UList<Expression> choiceList) {
		boolean byteMap[] = ByteMap.newMap(false);
		for(Expression e : choiceList) {
			if(e instanceof ByteChar) {
				byteMap[((ByteChar) e).byteChar] = true;
				continue;
			}
			if(e instanceof ByteMap) {
				ByteMap.appendBitMap(byteMap, ((ByteMap) e).byteMap);
				continue;
			}
			if(e instanceof AnyChar) {
				return e;
			}
			return null;
		}
		return GrammarFactory.newByteMap(s, byteMap);
	}

	public int commonCount = 0;

	private Expression selectChoice(Choice choice, UList<Expression> choiceList, int ch) {
		Expression first = null;
		UList<Expression> newChoiceList = null;
		boolean commonPrifixed = false;
		boolean otherConversion = false;
		for(Expression p : choiceList) {
			short r = p.acceptByte(ch, this.option);
			if(r == Prediction.Reject) {
				continue;
			}
			if(first == null) {
				first = p;
				continue;
			}
			if(newChoiceList == null) {
				if(UFlag.is(this.option, Grammar.CommonPrefix)) {
					Expression common = tryCommonFactoring(first, p, true);
					if(common != null) {
						if(common instanceof Sequence) {
							first = common;
							commonPrifixed = true;
							continue;
						}
						else {
							first = common;
							otherConversion = true;
							continue;
						}
					}
				}
				newChoiceList = new UList<Expression>(new Expression[2]);
				newChoiceList.add(first);
				newChoiceList.add(p);
			}
			else {
				Expression last = newChoiceList.ArrayValues[newChoiceList.size() - 1];
				if(UFlag.is(this.option, Grammar.CommonPrefix)) {
					Expression common = tryCommonFactoring(last, p, true);
					if(common != null) {
						newChoiceList.ArrayValues[newChoiceList.size() - 1] = common;
						continue;
					}
				}
				newChoiceList.add(p);
			}

		}
		if(commonPrifixed) {
			commonCount++;
		}
		if(newChoiceList != null) {
			return GrammarFactory.newChoice(choice.getSourcePosition(), newChoiceList).reshape(this);
		}
		return (commonPrifixed == true || otherConversion == true) ? first.reshape(this) : first;
	}

	public final static Expression tryCommonFactoring(Expression e, Expression e2, boolean ignoredFirstChar) {
		int min = sequenceSize(e) < sequenceSize(e2) ? sequenceSize(e) : sequenceSize(e2);
		int commonIndex = -1;
		for(int i = 0; i < min; i++) {
			Expression p = sequenceGetAt(e, i);
			Expression p2 = sequenceGetAt(e2, i);
			if(ignoredFirstChar && i == 0) {
				if(Expression.isByteConsumed(p) && Expression.isByteConsumed(p2)) {
					commonIndex = i + 1;
					continue;
				}
				break;
			}
			if(!eaualsExpression(p, p2)) {
				break;
			}
			commonIndex = i + 1;
		}
		if(commonIndex == -1) {
			return null;
		}
		UList<Expression> common = new UList<Expression>(new Expression[commonIndex]);
		for(int i = 0; i < commonIndex; i++) {
			common.add(sequenceGetAt(e, i));
		}
		UList<Expression> l1 = new UList<Expression>(new Expression[sequenceSize(e)]);
		for(int i = commonIndex; i < sequenceSize(e); i++) {
			l1.add(sequenceGetAt(e, i));
		}
		UList<Expression> l2 = new UList<Expression>(new Expression[sequenceSize(e2)]);
		for(int i = commonIndex; i < sequenceSize(e2); i++) {
			l2.add(sequenceGetAt(e2, i));
		}
		UList<Expression> l3 = new UList<Expression>(new Expression[2]);
		GrammarFactory.addChoice(l3, GrammarFactory.newSequence(null, l1));
		GrammarFactory.addChoice(l3, GrammarFactory.newSequence(null, l2));
		GrammarFactory.addSequence(common, GrammarFactory.newChoice(null, l3));
		return GrammarFactory.newSequence(null, common);
	}

	private static final int sequenceSize(Expression e) {
		if(e instanceof Sequence) {
			return e.size();
		}
		return 1;
	}

	private static final Expression sequenceGetAt(Expression e, int index) {
		if(e instanceof Sequence) {
			return e.get(index);
		}
		return e;
	}

	private static final boolean eaualsExpression(Expression e1, Expression e2) {
		if(e1.isInterned() && e2.isInterned()) {
			return e1.getId() == e2.getId();
		}
		return e1.key().equals(e2.key());
	}

//	
//	private final Expression makeCommonPrefix(Choice p) {
//		if(!UFlag.is(this.option, Grammar.CommonPrefix)) {
//			return null;
//		}
//		int start = 0;
//		Expression common = null;
//		for(int i = 0; i < p.size() - 1; i++) {
//			Expression e = p.get(i);
//			Expression e2 = p.get(i+1);
//			if(retrieveAsList(e,0).getId() == retrieveAsList(e2,0).getId()) {
//				common = trimCommonPrefix(e, e2);
//				start = i;
//				break;
//			}
//		}
//		if(common == null) {
//			return null;
//		}
//		UList<Expression> l = new UList<Expression>(new Expression[p.size()]);
//		for(int i = 0; i < start; i++) {
//			Expression e = p.get(i);
//			l.add(e);
//		}
//		for(int i = start + 2; i < p.size(); i++) {
//			Expression e = p.get(i);
//			if(retrieveAsList(common, 0).getId() == retrieveAsList(e,0).getId()) {
//				e = trimCommonPrefix(common, e);
//				if(e != null) {
//					common = e;
//					continue;
//				}
//			}
//			l.add(common);
//			common = e;
//		}
//		l.add(common);
//		return GrammarFactory.newChoice(null, l);
//	}
//
//
//	private final Expression trimCommonPrefix(Expression e, Expression e2) {
//		int min = sizeAsSequence(e) < sizeAsSequence(e2) ? sizeAsSequence(e) : sizeAsSequence(e2);
//		int commonIndex = -1;
//		for(int i = 0; i < min; i++) {
//			Expression p = retrieveAsList(e, i);
//			Expression p2 = retrieveAsList(e2, i);
//			if(p.getId() != p2.getId()) {
//				break;
//			}
//			commonIndex = i + 1;
//		}
//		if(commonIndex == -1) {
//			return null;
//		}
//		UList<Expression> common = new UList<Expression>(new Expression[commonIndex]);
//		for(int i = 0; i < commonIndex; i++) {
//			common.add(retrieveAsList(e, i));
//		}
//		UList<Expression> l1 = new UList<Expression>(new Expression[sizeAsSequence(e)]);
//		for(int i = commonIndex; i < sizeAsSequence(e); i++) {
//			l1.add(retrieveAsList(e, i));
//		}
//		UList<Expression> l2 = new UList<Expression>(new Expression[sizeAsSequence(e2)]);
//		for(int i = commonIndex; i < sizeAsSequence(e2); i++) {
//			l2.add(retrieveAsList(e2, i));
//		}
//		UList<Expression> l3 = new UList<Expression>(new Expression[2]);
//		GrammarFactory.addChoice(l3, GrammarFactory.newSequence(null, l1));
//		GrammarFactory.addChoice(l3, GrammarFactory.newSequence(null, l2));
//		GrammarFactory.addSequence(common, GrammarFactory.newChoice(null, l3));
//		return GrammarFactory.newSequence(null, common);
//	}

	public final static Expression mergeChoice(Expression p, Expression p2) {
		if(p == null) {
			return p2;
		}
//		if(p instanceof Choice) {
//			Expression last = p.get(p.size() - 1);
//			Expression common = makeCommonChoice(last, p2);
//			if(common == null) {
//				return Factory.newChoice(null, p, p2);
//			}
//		}
		Expression common = tryCommonFactoring(p, p2, true);
		if(common == null) {
			return GrammarFactory.newChoice(null, p, p2);
		}
		return common;
	}

}