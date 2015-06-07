package nez.lang;

import nez.util.UFlag;
import nez.util.UList;

public class GrammarReshaper {
	public final static GrammarReshaper RemoveASTandRename = new ASTConstructionEliminator(true);
	public final static GrammarReshaper RemoveAST = new ASTConstructionEliminator(false);

	public Expression reshapeProduction(Production p) {
		return p.getExpression().reshape(this);
	}

	public void updateProductionAttribute(Production origProduction, Production newProduction) {
	}

	public Expression reshapeEmpty(Empty e) {
		return e;
	}

	public Expression reshapeFailure(Failure e) {
		return e;
	}

	public Expression reshapeByteChar(ByteChar e) {
		return e;
	}

	public Expression reshapeByteMap(ByteMap e) {
		return e;
	}

	public Expression reshapeAnyChar(AnyChar e) {
		return e;
	}

	public Expression reshapeNonTerminal(NonTerminal e) {
		return e;
	}

//	public Expression reshapeSequence(Sequence e) {
//		int i = 0;
//		Expression updated = null;
//		for(i = 0; i < e.size(); i++) {
//			Expression s = e.get(i);
//			updated = s.reshape(this);
//			if(s == updated) {
//				updated = null;
//				continue;
//			}
//			break;
//		}
//		if(updated == null) {
//			return e;
//		}
//		UList<Expression> l = GrammarFactory.newList(2);
//		for(int j = 0; j < i; j++) {
//			l.add(e.get(j));
//		}
//		GrammarFactory.addSequence(l, updated);
//		for(int j = i + 1; j < e.size(); j++) {
//			GrammarFactory.addSequence(l, e.get(j).reshape(this));
//		}
//		return GrammarFactory.newSequence(e.s, l);
//	}

	public Expression reshapeSequence(Sequence e) {
		Expression first = e.getFirst().reshape(this);
		Expression last = e.getLast().reshape(this);
		if(first == e.getFirst() && last == e.getLast()) {
			return e;
		}
		return e.newSequence(first, last);
	}

	
	public Expression reshapeChoice(Choice e) {
		int i = 0;
		Expression updated = null;
		for(i = 0; i < e.size(); i++) {
			Expression s = e.get(i);
			updated = s.reshape(this);
			if(s == updated) {
				updated = null;
				continue;
			}
			break;
		}
		if(updated == null) {
			return e;
		}
		UList<Expression> l = GrammarFactory.newList(e.size());
		for(int j = 0; j < i; j++) {
			l.add(e.get(j));
		}
		GrammarFactory.addChoice(l, updated);
		for(int j = i+1; j < e.size(); j++) {
			GrammarFactory.addChoice(l, e.get(j).reshape(this));
		}
		return GrammarFactory.newChoice(e.s, l);
	}

	public Expression reshapeOption(Option e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeRepetition(Repetition e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeRepetition1(Repetition1 e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeAnd(And e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeNot(Not e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeMatch(Match e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeNew(New e) {
		return e;
	}

	public Expression reshapeLink(Link e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeTagging(Tagging e) {
		return e;
	}

	public Expression reshapeReplace(Replace e) {
		return e;
	}

	public Expression reshapeCapture(Capture e) {
		return e;
	}
	
	public Expression reshapeBlock(Block e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeLocalTable(LocalTable e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeDefSymbol(DefSymbol e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeIsSymbol(IsSymbol e) {
		return e;
	}

	public Expression reshapeExistsSymbol(ExistsSymbol e) {
		return e;
	}

	public Expression reshapeIsIndent(IsIndent e) {
		return e;
	}
	
	public Expression reshapeIfFlag(IfFlag e) {
		return e;
	}
	
	public Expression reshapeOnFlag(OnFlag e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}
	
	public Expression reshapeUndefined(Expression e) {
		return e;
	}


	protected final Expression empty(Expression e) {
		return GrammarFactory.newEmpty(null);
	}

	protected final Expression fail(Expression e) {
		return GrammarFactory.newFailure(null);
	}

	protected final Expression updateInner(Option e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newOption(e.s, inner) : e;
	}
	protected final Expression updateInner(Repetition e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newRepetition(e.s, inner) : e;
	}
	protected final Expression updateInner(Repetition1 e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newRepetition1(e.s, inner) : e;
	}
	protected final Expression updateInner(And e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newAnd(e.s, inner) : e;
	}
	protected final Expression updateInner(Not e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newNot(e.s, inner) : e;
	}
	protected final Expression updateInner(Match e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newMatch(e.s, inner) : e;
	}
	protected final Expression updateInner(Link e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newLink(e.s, inner, e.index) : e;
	}
	protected final Expression updateInner(Block e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newBlock(e.s, inner) : e;
	}
	protected final Expression updateInner(LocalTable e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newLocal(e.s, e.getNameSpace(), e.getTable(), inner) : e;
	}
	protected final Expression updateInner(DefSymbol e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newDefSymbol(e.s, e.getNameSpace(), e.getTable(), inner) : e;
	}
	protected final Expression updateInner(OnFlag e, Expression inner) {
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return (e.get(0) != inner) ? GrammarFactory.newOnFlag(e.s, e.predicate, e.flagName, inner) : e;
	}
}

class ASTConstructionEliminator extends GrammarReshaper {
	boolean renaming ;
	ASTConstructionEliminator(boolean renaming) {
		this.renaming = renaming;
	}
	public void updateProductionAttribute(Production origProduction, Production newProduction) {
		newProduction.flag = UFlag.unsetFlag(origProduction.flag, Production.ObjectProduction | Production.OperationalProduction);
	}
	
	@Override
	public Expression reshapeNonTerminal(NonTerminal e) {
		if(renaming) {
			Production r = removeASTOperator(e.getProduction());
			if(!e.getLocalName().equals(r.getLocalName())) {
				return GrammarFactory.newNonTerminal(e.s, r.getNameSpace(), r.getLocalName());
			}
		}
		return e;
	}
	
	private Production removeASTOperator(Production p) {
		if(p.inferTypestate(null) == Typestate.BooleanType) {
			return p;
		}
		String name = "~" + p.getLocalName();
		Production r = p.getNameSpace().getProduction(name);
		if(r == null) {
			r = p.getNameSpace().newReducedProduction(name, p, this);
		}
		return r;
	}
	
	public Expression reshapeMatch(Match e) {
		return e.get(0).reshape(this);
	}
	
	public Expression reshapeNew(New e) {
		return empty(e);
	}
	
	public Expression reshapeLink(Link e) {
		return e.get(0).reshape(this);
	}

	public Expression reshapeTagging(Tagging e) {
		return empty(e);
	}

	public Expression reshapeReplace(Replace e) {
		return empty(e);
	}

	public Expression reshapeCapture(Capture e) {
		return empty(e);
	}
		
}
