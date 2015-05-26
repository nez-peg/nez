package nez.lang;

import nez.ast.SourcePosition;
import nez.util.UFlag;
import nez.util.UList;

class GrammarOptimizer extends GrammarReshaper {

	int option;
	Grammar grammar = null;
	
	public GrammarOptimizer(int option) {
		this.option = option;
	}

	public void optimize(Grammar grammar) {
		this.grammar = grammar;
		grammar.reshapeAll(this);
		this.grammar = null;
	}

	@Override
	public Expression reshapeChoice(Choice p) {
		UList<Expression> l = new UList<Expression>(new Expression[p.size()]);
		flatten(p, l);
		if(UFlag.is(option, Grammar.Optimization)) {
			Expression o = newOptimizedByteMap(p.s, l);
			if(o != null) {
				return o;
			}
		}
		return GrammarFactory.newChoice(p.getSourcePosition(), l);
//		if(UFlag.is(option, Grammar.Prediction) && !UFlag.is(option, Grammar.DFA)) {
//			Expression fails = Factory.newFailure(s);
//			this.matchCase = new Expression[257];
//			for(int ch = 0; ch <= 256; ch++) {
//				Expression selected = selectChoice(ch, fails, option);
//				matchCase[ch] = selected;
//			}
//		}
	}
	
	private void flatten(Choice parentExpression, UList<Expression> l) {
		for(Expression subExpression: parentExpression) {
			subExpression = subExpression.reshape(this);
//			e = resolveNonTerminal(e);
			if(subExpression instanceof Choice) {
				flatten((Choice)subExpression, l);
			}
			else {
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
	
	@Override
	public Expression reshapeNonTerminal(NonTerminal p) {
		Expression e = p;
		while(e instanceof NonTerminal) {
			NonTerminal nterm = (NonTerminal) e;
			e = nterm.deReference().optimize(option);
		}
		return e;
	}

	@Override
	public Expression reshapeSequence(Sequence parentExpression) {
		UList<Expression> l = new UList<Expression>(new Expression[parentExpression.size()]);
		for(Expression subExpression: parentExpression) {
			GrammarFactory.addSequence(l, subExpression.reshape(this));
		}
		reorderSequence(l);
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
			Expression p = l.ArrayValues[i-1];
			Expression e = l.ArrayValues[i];
			if(Expression.isByteConsumed(e)) {   // #t 'a' 'b' => 'a' #t 'b'
				if(Expression.isPositionIndependentOperation(p)) {
					l.ArrayValues[i-1] = e;
					l.ArrayValues[i]   = p;
					continue;
				}
				if(p instanceof New) {
					New n = (New)p;
					l.ArrayValues[i-1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] =  GrammarFactory.newNew(n.s, n.lefted, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i]   = n;
					}
					continue;
				}
				if(p instanceof Capture) {
					Capture n = (Capture)p;
					l.ArrayValues[i-1] = e;
					if(n.isInterned()) {
						l.ArrayValues[i] =  GrammarFactory.newCapture(n.s, n.shift - 1);
					}
					else {
						n.shift -= 1;
						l.ArrayValues[i]   = n;
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
		assert(e != -1);
		Not not = (Not)l.ArrayValues[loc];
		AnyChar any = (AnyChar)l.ArrayValues[e];
		if(loc + 1 < e) {
			UList<Expression> sl = new UList<Expression>(new Expression[4]);
			for(int i = loc; i < e; i++) {
				GrammarFactory.addChoice(sl, l.ArrayValues[i]);
			}
			not = GrammarFactory.newNot(not.s, GrammarFactory.newChoice(not.s, sl).reshape(this));
		}
		if(not.inner instanceof ByteChar) {
			boolean[] byteMap = ByteMap.newMap(true);
			byteMap[((ByteChar) not.inner).byteChar] = false;
			if(!UFlag.is(option, Grammar.Binary)) {
				byteMap[0] = false;
			}
			nl.add(GrammarFactory.newByteMap(not.s, byteMap));
		}
		else if(not.inner instanceof ByteMap) {
			boolean[] byteMap = ByteMap.newMap(false);
			ByteMap.appendBitMap(byteMap, ((ByteMap) not.inner).byteMap);
			ByteMap.reverse(byteMap, option);
			nl.add(GrammarFactory.newByteMap(not.s, byteMap));
		}
		else {
			nl.add(not);
			nl.add(any);
		}
		loc = findNotAny(e+1, l);
		if(loc != -1) {
			joinNotAny(e+1, loc, l, nl);
			return;
		}
		for(int i = e+1; i < l.size(); i++) {
			nl.add(l.ArrayValues[i]);
		}
	}
	
	public Expression reshapeLink(Link p) {
		if(p.get(0) instanceof Choice) {
			Expression inner = p.get(0);
			UList<Expression> l = new UList<Expression>(new Expression[inner.size()]);
			for(Expression subChoice: inner) {
				subChoice = subChoice.reshape(this);
				l.add(GrammarFactory.newLink(p.getSourcePosition(), subChoice, p.index));
			}			
			return GrammarFactory.newChoice(inner.getSourcePosition(), l);
		}
		return super.reshapeLink(p);
	}
	
	public static Expression newOptimizedByteMap(SourcePosition s, UList<Expression> l) {
		boolean byteMap[] = ByteMap.newMap(false);
		for(Expression e : l) {
			if(e instanceof ByteChar) {
				byteMap[((ByteChar) e).byteChar] = true;
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
		return GrammarFactory.newByteMap(s, byteMap);
	}
	
}