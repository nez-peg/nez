package nez.lang;

import nez.util.UList;

public class Manipulator {
	public final static Manipulator RemoveASTandRename = new ASTConstructionEliminator(true);
	public final static Manipulator RemoveAST = new ASTConstructionEliminator(false);

	public Expression reshapeUndefined(Expression e) {
		return e;
	}
	
	public void updateProductionAttribute(Production origProduction, Production newProduction) {
		
	}
	
	public Expression reshapeProduction(Production p) {
		return p.getExpression().reshape(this);
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

	public Expression reshapeSequence(Sequence e) {
		UList<Expression> l = e.newList();
		boolean updated = false;
		for(Expression s: e) {
			Expression ns = s.reshape(this);
			if(s != ns) {
				updated = true;
			}
			Factory.addSequence(l, ns);
		}
		if(updated) {
			return Factory.newSequence(e.s, l);
		}
		return e;
	}

	public Expression reshapeChoice(Choice e) {
		UList<Expression> l = e.newList();
		boolean updated = false;
		for(Expression s: e) {
			Expression ns = s.reshape(this);
			if(s != ns) {
				updated = true;
			}
			Factory.addChoice(l, ns);
		}
		if(updated) {
			return Factory.newChoice(e.s, l);
		}
		return e;
	}

	public Expression reshapeOption(Option e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeRepetition(Repetition e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeRepetition1(Repetition1 e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeAnd(And e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeNot(Not e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeMatch(Match e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeNew(New e) {
		return e;
	}

	public Expression reshapeLink(Link e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
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
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeLocalTable(LocalTable e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	public Expression reshapeDefSymbol(DefSymbol e) {
		Expression inner = e.get(0).reshape(this);
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
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
		if(!e.isInterned()) {
			e.inner = inner;
			return e;
		}
		return update(e, inner);
	}

	protected final Expression empty(Expression e) {
		return Factory.newEmpty(null);
	}

	protected final Expression fail(Expression e) {
		return Factory.newFailure(null);
	}

	protected final Expression update(Option e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newOption(e.s, inner) : e;
	}
	protected final Expression update(Repetition e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newRepetition(e.s, inner) : e;
	}
	protected final Expression update(Repetition1 e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newRepetition1(e.s, inner) : e;
	}
	protected final Expression update(And e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newAnd(e.s, inner) : e;
	}
	protected final Expression update(Not e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newNot(e.s, inner) : e;
	}
	protected final Expression update(Match e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newMatch(e.s, inner) : e;
	}
	protected final Expression update(Link e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newLink(e.s, inner, e.index) : e;
	}
	protected final Expression update(Block e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newBlock(e.s, inner) : e;
	}
	protected final Expression update(LocalTable e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newLocal(e.s, e.getNameSpace(), e.getTable(), inner) : e;
	}
	protected final Expression update(DefSymbol e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newDefSymbol(e.s, e.getNameSpace(), e.getTable(), inner) : e;
	}
	protected final Expression update(OnFlag e, Expression inner) {
		return (e.get(0) != inner) ? Factory.newOnFlag(e.s, e.predicate, e.flagName, inner) : e;
	}
}

class ASTConstructionEliminator extends Manipulator {
	boolean renaming ;
	ASTConstructionEliminator(boolean renaming) {
		this.renaming = renaming;
	}
	public void updateProductionAttribute(Production origProduction, Production newProduction) {
		newProduction.transType = Typestate.BooleanType;
		newProduction.minlen = origProduction.minlen;
	}
	
	@Override
	public Expression reshapeNonTerminal(NonTerminal e) {
		if(renaming) {
			Production r = removeASTOperator(e.getProduction());
			if(!e.getLocalName().equals(r.getLocalName())) {
				return Factory.newNonTerminal(e.s, r.getNameSpace(), r.getLocalName());
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
