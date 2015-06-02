package nez.vm;

import nez.ast.SourcePosition;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.Empty;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarFactory;
import nez.lang.GrammarReshaper;
import nez.lang.Link;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Acceptance;
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

//	@Override
//	public Expression reshapeSequence(Sequence p) {
//		Expression first = p.getFirst().reshape(this);
//		Expression last  = p.getLast().reshape(this);
//		if(UFlag.is(option, Grammar.CommonPrefix)) {
//			
//		}
////		if(UFlag.is(option, Grammar.Optimization)) {
////			if(isNotChar(first) || isNotChar(last)) {
////				boolean[] b = bitMap(last, first);
//////				GrammarFactory.newByteMap(s, byteMap);
////			}
////		}
//		return p.newSequence(first, last);
//	}
	
	private boolean isNotChar(Expression p) {
		if(p instanceof Not) {
			return (p.get(0) instanceof ByteMap || p.get(0) instanceof ByteChar);
		}
		return false;
	}

	private boolean isAnyChar(Expression p) {
		return (p instanceof AnyChar || p instanceof ByteChar);
	}

//	private boolean[] bitMap(Expression any, Expression not) {
//		boolean[] b = null;;
//		if(any instanceof AnyChar) {
//			b = ByteMap.newMap(false);
//		}
//		if(any instanceof ByteMap) {
//			b = any.byteMap.clone;
//		}
//	}
	
//	@Override
//	public Expression reshapeSequence(Sequence parentExpression) {
//		UList<Expression> l = new UList<Expression>(new Expression[parentExpression.size()]);
//		for(Expression subExpression: parentExpression) {
//			GrammarFactory.addSequence(l, subExpression.reshape(this));
//		}
////		reorderSequence(l);  // FIXME
//		if(UFlag.is(option, Grammar.Optimization)) {
//			
//			int loc = findNotAny(0, l);
//			if(loc != -1) {
//				UList<Expression> nl = new UList<Expression>(new Expression[l.size()]);
//				joinNotAny(0, loc, l, nl);
//				l = nl;
//			}
//		}
//		return GrammarFactory.newSequence(parentExpression.getSourcePosition(), l);
//	}
//	
//	/**
//	 * Sequence otimization
//	 * // #t 'a' 'b' => 'a' #t 'b'
//	 */
//
//	private void reorderSequence(UList<Expression> l) {
//		for(int i = 1; i < l.size(); i++) {
//			Expression p = l.ArrayValues[i-1];
//			Expression e = l.ArrayValues[i];
//			if(Expression.isByteConsumed(e)) {   // #t 'a' 'b' => 'a' #t 'b'
//				if(Expression.isPositionIndependentOperation(p)) {
//					l.ArrayValues[i-1] = e;
//					l.ArrayValues[i]   = p;
//					continue;
//				}
//				if(p instanceof New) {
//					New n = (New)p;
//					l.ArrayValues[i-1] = e;
//					if(n.isInterned()) {
//						l.ArrayValues[i] =  GrammarFactory.newNew(n.getSourcePosition(), n.lefted, n.shift - 1);
//					}
//					else {
//						n.shift -= 1;
//						l.ArrayValues[i]   = n;
//					}
//					continue;
//				}
//				if(p instanceof Capture) {
//					Capture n = (Capture)p;
//					l.ArrayValues[i-1] = e;
//					if(n.isInterned()) {
//						l.ArrayValues[i] =  GrammarFactory.newCapture(n.getSourcePosition(), n.shift - 1);
//					}
//					else {
//						n.shift -= 1;
//						l.ArrayValues[i]   = n;
//					}
//					continue;
//				}
//			}
//		}
//	}
//
//	private int findNotAny(int s, UList<Expression> l) {
//		for(int i = s; i < l.size(); i++) {
//			Expression p = l.ArrayValues[i];
//			if(p instanceof Not) {
//				if(findAny(i, l) != -1) {
//					return i;
//				}
//			}
//		}
//		return -1;
//	}
//
//	private int findAny(int s, UList<Expression> l) {
//		for(int i = s; i < l.size(); i++) {
//			Expression p = l.ArrayValues[i];
//			if(p instanceof Not) {
//				continue;
//			}
//			if(p instanceof AnyChar) {
//				return i;
//			}
//			break;
//		}
//		return -1;
//	}
//
//	private void joinNotAny(int s, int loc, UList<Expression> l, UList<Expression> nl) {
//		for(int i = s; i < loc; i++) {
//			nl.add(l.ArrayValues[i]);
//		}
//		int e = findAny(loc, l);
//		assert(e != -1);
//		Not not = (Not)l.ArrayValues[loc];
//		AnyChar any = (AnyChar)l.ArrayValues[e];
//		if(loc + 1 < e) {
//			UList<Expression> sl = new UList<Expression>(new Expression[4]);
//			for(int i = loc; i < e; i++) {
//				GrammarFactory.addChoice(sl, l.ArrayValues[i]);
//			}
//			not = GrammarFactory.newNot(not.getSourcePosition(), GrammarFactory.newChoice(not.getSourcePosition(), sl).reshape(this));
//		}
//		if(not.get(0) instanceof ByteChar) {
//			boolean[] byteMap = ByteMap.newMap(true);
//			byteMap[((ByteChar) not.get(0)).byteChar] = false;
//			if(!UFlag.is(option, Grammar.Binary)) {
//				byteMap[0] = false;
//			}
//			nl.add(GrammarFactory.newByteMap(not.getSourcePosition(), byteMap));
//		}
//		else if(not.get(0) instanceof ByteMap) {
//			boolean[] byteMap = ByteMap.newMap(false);
//			ByteMap.appendBitMap(byteMap, ((ByteMap) not.get(0)).byteMap);
//			ByteMap.reverse(byteMap, option);
//			nl.add(GrammarFactory.newByteMap(not.getSourcePosition(), byteMap));
//		}
//		else {
//			nl.add(not);
//			nl.add(any);
//		}
//		loc = findNotAny(e+1, l);
//		if(loc != -1) {
//			joinNotAny(e+1, loc, l, nl);
//			return;
//		}
//		for(int i = e+1; i < l.size(); i++) {
//			nl.add(l.ArrayValues[i]);
//		}
//	}
	
	public Expression reshapeLink(Link p) {
		if(p.get(0) instanceof Choice) {
			Expression inner = p.get(0);
			UList<Expression> l = new UList<Expression>(new Expression[inner.size()]);
			for(Expression subChoice: inner) {
				subChoice = subChoice.reshape(this);
				l.add(GrammarFactory.newLink(p.getSourcePosition(), subChoice, p.index));
			}			
			return inner.newChoice(l);
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
//			if(UFlag.is(option, Grammar.Prediction)) {
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
//			}
		}
		return p;
	}
	
	private void flattenChoiceList(Choice parentExpression, UList<Expression> l) {
		for(Expression subExpression: parentExpression) {
			subExpression = resolveNonTerminal(subExpression);
			if(subExpression instanceof Choice) {
				flattenChoiceList((Choice)subExpression, l);
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
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference();
		}
		return e;
	}

	// OptimizerLibrary
	
	public final static Expression newOptimizedByteMap(SourcePosition s, UList<Expression> choiceList) {
		boolean byteMap[] = ByteMap.newMap(false);
		boolean binary = false;
		for(Expression e : choiceList) {
			if(e instanceof ByteChar) {
				byteMap[((ByteChar) e).byteChar] = true;
				if(((ByteChar) e).isBinary()) {
					binary = true;
				}
				continue;
			}
			if(e instanceof ByteMap) {
				ByteMap.appendBitMap(byteMap, ((ByteMap)e).byteMap);
				continue;
			}
			if(e instanceof AnyChar) {
				return e;
			}
			return null;
		}
		return GrammarFactory.newByteMap(s, binary, byteMap);
	}
				
	private Expression selectChoice(Choice choice, UList<Expression> choiceList, int ch) {
		Expression first = null;
		UList<Expression> newChoiceList = null;
		boolean commonPrifixed = false;
		for(Expression p: choiceList) {
			short r = p.acceptByte(ch, this.option);
			if(r == Acceptance.Reject) {
				continue;
			}
			if(first == null) {
				first = p;
				continue;
			}
			if(newChoiceList == null) {
				Expression common = tryCommonFactoring(choice, first, p, true);
				if(common != null) {
					first = common;
					commonPrifixed = true;
					continue;
				}
				newChoiceList = new UList<Expression>(new Expression[2]);
				newChoiceList.add(first);				
				newChoiceList.add(p);
			}
			else {
				Expression last = newChoiceList.ArrayValues[newChoiceList.size()-1];
				Expression common = tryCommonFactoring(choice, last, p, true);
				if(common != null) {
					newChoiceList.ArrayValues[newChoiceList.size()-1] = common;
					continue;
				}
				newChoiceList.add(p);
			}
		}
		if(newChoiceList != null) {
			return GrammarFactory.newChoice(choice.getSourcePosition(), newChoiceList);
		}
		return commonPrifixed == true ? first.reshape(this) : first;
	}
		
	public final static Expression tryCommonFactoring(Choice base, Expression e, Expression e2, boolean ignoredFirstChar) {
		UList<Expression> l = null;
		while(e != null && e2 != null) {
			Expression f = e.getFirst();
			Expression f2 = e2.getFirst();
			if(ignoredFirstChar) {
				ignoredFirstChar = false;
				if(Expression.isByteConsumed(f) && Expression.isByteConsumed(f2)) {
					l = GrammarFactory.newList(4);
					l.add(f);
					e = e.getLast();
					e2 = e2.getLast();
					continue;
				}
				return null;
			}
			if(!f.equalsExpression(f2)) {
				break;
			}
			if(l == null) {
				l = GrammarFactory.newList(4);
			}
			l.add(f);
			e = e.getLast();
			e2 = e2.getLast();
			//System.out.println("l="+l.size()+",e="+e);
		}
		if(l == null) {
			return null;
		}
		if(e == null) {
			e = base.newEmpty();
		}
		if(e2 == null) {
			e2 = base.newEmpty();
		}		
		Expression alt = base.newChoice(e, e2);
		l.add(alt);
		return base.newSequence(l);
	}

	

	
}