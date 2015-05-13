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
		for(Expression s: e) {
			Factory.addSequence(l, s.reshape(this));
		}
		return Factory.newSequence(e.s, l);
	}

	public Expression reshapeChoice(Choice e) {
		UList<Expression> l = e.newList();
		for(Expression s: e) {
			Factory.addChoice(l, s.reshape(this));
		}
		return Factory.newChoice(e.s, l);
	}

	public Expression reshapeOption(Option e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newOption(e.s, inner) : e;
	}

	public Expression reshapeRepetition(Repetition e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newRepetition(e.s, inner) : e;
	}

	public Expression reshapeRepetition1(Repetition1 e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newRepetition1(e.s, inner) : e;
	}

	public Expression reshapeAnd(And e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newAnd(e.s, inner) : e;
	}

	public Expression reshapeNot(Not e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newNot(e.s, inner) : e;
	}

	public Expression reshapeMatch(Match e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newMatch(e.s, inner) : e;
	}

	public Expression reshapeNew(New e) {
		return e;
	}

	public Expression reshapeLink(Link e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newLink(e.s, inner, e.index) : e;
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
		return (e.get(0) != inner) ? Factory.newBlock(e.s, inner) : e;
	}

	public Expression reshapeDefSymbol(DefSymbol e) {
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newDefSymbol(e.s, e.table, inner) : e;
	}

	public Expression reshapeIsSymbol(IsSymbol e) {
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
		return (e.get(0) != inner) ? Factory.newOnFlag(e.s, e.predicate, e.flagName, inner) : e;
	}

	protected final Expression empty(Expression e) {
		return Factory.newEmpty(null);
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
		Expression inner = e.get(0).reshape(this);
		return (e.get(0) != inner) ? Factory.newMatch(e.s, inner) : e;
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

	
//	@Override
//	public Expression removeASTOperator(boolean newNonTerminal) {
//		return Factory.newEmpty(s);
//	}


	

}
