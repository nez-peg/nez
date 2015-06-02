package nez.vm;

import java.util.HashMap;

import nez.lang.Choice;
import nez.lang.Empty;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.GrammarFactory;
import nez.lang.GrammarReshaper;
import nez.lang.NameSpace;
import nez.lang.NonTerminal;
import nez.lang.Option;
import nez.lang.Acceptance;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Sequence;
import nez.util.StringUtils;
import nez.util.UFlag;
import nez.util.UList;
import nez.util.UMap;

public class DfaOptimizer extends GrammarReshaper {

	public static final Grammar optimize(Grammar g) {
		NameSpace ns = NameSpace.newNameSpace();
		GrammarReshaper dup = new DuplicateGrammar(ns);
		GrammarReshaper inlining = new InliningChoice();
		for(Production p : g.getProductionList()) {
			dup.reshapeProduction(p);
		}
		for(Production p : ns.getDefinedRuleList()) {
			System.out.println(p.getLocalName() + "::\n\t"+inlining.reshapeProduction(p));
		}
		g = ns.newGrammar(g.getStartProduction().getLocalName());
		return g;
	}

}

class DuplicateGrammar extends GrammarReshaper {
	NameSpace ns;
	int c = 0;
	DuplicateGrammar(NameSpace ns) {
		this.ns = ns;
	}
	public Expression reshapeProduction(Production p) {
		Expression e = p.getExpression().reshape(GrammarReshaper.RemoveAST).reshape(this);
		this.ns.defineProduction(p.getSourcePosition(), p.getLocalName(), e);
		return e;
	}
	public Expression reshapeNonTerminal(NonTerminal p) {
		return GrammarFactory.newNonTerminal(p.getSourcePosition(), ns, p.getLocalName());
	}
	public Expression reshapeOption(Option e) {
		Expression inner = e.get(0).reshape(this);
		return GrammarFactory.newChoice(e.getSourcePosition(), inner, empty(e));
	}
	public Expression reshapeRepetition(Repetition e) {
		Expression inner = e.get(0).reshape(this);
		String name = "rr" + (c++);
		if(inner.isInterned()) {
			name = "r" + inner.getId();
			if(!this.ns.hasProduction(name)) {
				this.ns.defineProduction(e.getSourcePosition(), name, inner);
			}
		}
		else {
			this.ns.defineProduction(e.getSourcePosition(), name, inner);
		}
		Expression p = ns.newNonTerminal(name);
		Expression seq = GrammarFactory.newSequence(e.getSourcePosition(), inner, p);
		return GrammarFactory.newChoice(e.getSourcePosition(), seq, empty(e));
	}

	public Expression reshapeRepetition1(Repetition1 e) {
		Expression inner = e.get(0).reshape(this);
		return GrammarFactory.newSequence(e.getSourcePosition(), inner, reshapeRepetition(e));
	}

	public Expression reshapeSequence(Sequence e) {
		Expression first = e.get(0).reshape(this);
		Expression second = e.get(1).reshape(this);
		if(isEmptyChoice(first)) {
			return joinChoice((Choice)first, second);
		}
		return e.newSequence(first, second);
	}
	
	private boolean isEmptyChoice(Expression e) {
		if(e instanceof Choice) {
			Expression last = e.get(e.size()-1);
			if(last instanceof Empty) {
				return true;
			}
		}
		return false;
	}

	private Expression joinChoice(Choice e, Expression e2) {
		System.out.println("join** " + e + "\n\t" + e2);
		UList<Expression> l = GrammarFactory.newList(e.size());
		for(Expression se: e) {
			l.add(e.newSequence(se, e2));
		}
		return e.newChoice(l);
	}

}

class InliningChoice extends GrammarReshaper {
	
	boolean inlining = false;
	public Expression reshapeProduction(Production p) {
		this.inlining = false;
		Expression e = p.getExpression().reshape(this);
		p.setExpression(e);
		return e;
	}

	@Override
	public Expression reshapeChoice(Choice p) {
		UList<Expression> choiceList = new UList<Expression>(new Expression[p.size()]);
		boolean stacked = this.inlining;
		this.inlining = true;
		flattenChoiceList(p, choiceList);
		this.inlining = stacked;
		Expression newp = GrammarFactory.newChoice(p.getSourcePosition(), choiceList);
//		if(newp instanceof Choice) {
//			p = (Choice)newp;
//			if(p.predictedCase == null) {
////				System.out.println("choice: " + p);
//				p.predictedCase = new Expression[257];
//				for(int ch = 0; ch <= 256; ch++) {
//					p.predictedCase[ch] = selectChoice(p, choiceList, ch);
////					if(p.predictedCase[ch] != null && !(p.predictedCase[ch] instanceof Empty)) {
////						System.out.println(StringUtils.stringfyByte(ch)+ ":: " + p.predictedCase[ch]);
////					}
//				}
//			}
//		}
		return newp;
	}
	
	private void flattenChoiceList(Choice parentExpression, UList<Expression> l) {
		for(Expression subExpression: parentExpression) {
			subExpression = subExpression.reshape(this);
			if(subExpression instanceof Choice) {
				flattenChoiceList((Choice)subExpression, l);
			}
			else {
				l.add(subExpression);
			}
		}
	}
	
	public Expression reshapeNonTerminal(NonTerminal p) {
		if(this.inlining) {
			System.out.println(p.getLocalName());
			return p.deReference().reshape(this);
		}
//		Expression e = p.deReference().reshape(this);
//		if(isEmptyChoice(e)) {
////			System.out.println("empty: " + p + "," + e);
//			return e;
//		}
		return p;
	}
	
	private boolean isEmptyChoice(Expression e) {
		if(e instanceof Choice) {
			return e.get(e.size()-1) instanceof Empty;
		}
		if(e instanceof Sequence) {
			return isEmptyChoice(e.get(e.size()-1));
		}
		return false;
	}
	

	public Expression reshapeSequence(Sequence e) {
		if(this.inlining) {
			Expression first = e.getFirst().reshape(this);
			this.inlining = false;
			Expression last = e.getLast().reshape(this);
			this.inlining = true;
			if(first == e.getFirst() && last == e.getLast()) {
				return e;
			}
			return e.newSequence(first, last);
		}
		return super.reshapeSequence(e);
	}
	
	// prediction 
	
	private Expression selectChoice(Choice choice, UList<Expression> choiceList, int ch) {
		Expression first = null;
		UList<Expression> newChoiceList = null;
		boolean commonPrifixed = false;
		for(Expression p: choiceList) {
			short r = p.acceptByte(ch, 0);
			if(r == Acceptance.Reject) {
				continue;
			}
			if(first == null) {
				first = p;
				continue;
			}
			if(newChoiceList == null) {
				Expression common = tryCommonFactoring(first, p, true);
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
				Expression common = tryCommonFactoring(last, p, true);
				if(common != null) {
					newChoiceList.ArrayValues[newChoiceList.size()-1] = common;
					continue;
				}
				newChoiceList.add(p);
			}
		}
		if(newChoiceList != null) {
			return GrammarFactory.newChoice(choice.getSourcePosition(), newChoiceList).reshape(this);
		}
		return commonPrifixed == true ? first.reshape(this) : first;
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
}
