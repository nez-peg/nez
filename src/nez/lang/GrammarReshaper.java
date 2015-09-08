package nez.lang;

import nez.lang.expr.And;
import nez.lang.expr.AnyChar;
import nez.lang.expr.Block;
import nez.lang.expr.ByteChar;
import nez.lang.expr.ByteMap;
import nez.lang.expr.Capture;
import nez.lang.expr.Choice;
import nez.lang.expr.DefSymbol;
import nez.lang.expr.Empty;
import nez.lang.expr.ExistsSymbol;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.Failure;
import nez.lang.expr.IfFlag;
import nez.lang.expr.IsIndent;
import nez.lang.expr.IsSymbol;
import nez.lang.expr.Link;
import nez.lang.expr.LocalTable;
import nez.lang.expr.Match;
import nez.lang.expr.MatchSymbol;
import nez.lang.expr.MultiChar;
import nez.lang.expr.New;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Not;
import nez.lang.expr.OnFlag;
import nez.lang.expr.Option;
import nez.lang.expr.Repetition;
import nez.lang.expr.Repetition1;
import nez.lang.expr.Replace;
import nez.lang.expr.Sequence;
import nez.lang.expr.Tagging;
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

	public Expression reshapeCharMultiByte(MultiChar e) {
		return e;
	}

	public Expression reshapeNonTerminal(NonTerminal e) {
		return e;
	}

	// public Expression reshapeSequence(Sequence e) {
	// int i = 0;
	// Expression updated = null;
	// for(i = 0; i < e.size(); i++) {
	// Expression s = e.get(i);
	// updated = s.reshape(this);
	// if(s == updated) {
	// updated = null;
	// continue;
	// }
	// break;
	// }
	// if(updated == null) {
	// return e;
	// }
	// UList<Expression> l = GrammarFactory.newList(2);
	// for(int j = 0; j < i; j++) {
	// l.add(e.get(j));
	// }
	// GrammarFactory.addSequence(l, updated);
	// for(int j = i + 1; j < e.size(); j++) {
	// GrammarFactory.addSequence(l, e.get(j).reshape(this));
	// }
	// return GrammarFactory.newSequence(e.s, l);
	// }

	public Expression reshapeSequence(Sequence e) {
		Expression first = e.getFirst().reshape(this);
		Expression last = e.getNext().reshape(this);
		if (first == e.getFirst() && last == e.getNext()) {
			return e;
		}
		return e.newSequence(first, last);
	}

	public Expression reshapeChoice(Choice e) {
		int i = 0;
		Expression updated = null;
		for (i = 0; i < e.size(); i++) {
			Expression s = e.get(i);
			updated = s.reshape(this);
			if (s == updated) {
				updated = null;
				continue;
			}
			break;
		}
		if (updated == null) {
			return e;
		}
		UList<Expression> l = ExpressionCommons.newList(e.size());
		for (int j = 0; j < i; j++) {
			l.add(e.get(j));
		}
		ExpressionCommons.addChoice(l, updated);
		for (int j = i + 1; j < e.size(); j++) {
			ExpressionCommons.addChoice(l, e.get(j).reshape(this));
		}
		return ExpressionCommons.newChoice(e.getSourcePosition(), l);
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

	public Expression reshapeMatchSymbol(MatchSymbol e) {
		return e;
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
		return ExpressionCommons.newEmpty(null);
	}

	protected final Expression fail(Expression e) {
		return ExpressionCommons.newFailure(null);
	}

	protected final Expression updateInner(Option e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Repetition e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Repetition1 e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(And e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Not e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Match e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Link e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Block e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(LocalTable e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(DefSymbol e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(OnFlag e, Expression inner) {
		e.inner = inner;
		return e;
	}

}

class ASTConstructionEliminator extends GrammarReshaper {
	boolean renaming;

	ASTConstructionEliminator(boolean renaming) {
		this.renaming = renaming;
	}

	@Override
	public void updateProductionAttribute(Production origProduction, Production newProduction) {
		newProduction.flag = UFlag.unsetFlag(origProduction.flag, Production.ObjectProduction | Production.OperationalProduction);
	}

	@Override
	public Expression reshapeNonTerminal(NonTerminal e) {
		if (renaming) {
			Production r = removeASTOperator(e.getProduction());
			if (!e.getLocalName().equals(r.getLocalName())) {
				return ExpressionCommons.newNonTerminal(e.getSourcePosition(), r.getGrammarFile(), r.getLocalName());
			}
		}
		return e;
	}

	private Production removeASTOperator(Production p) {
		if (p.inferTypestate(null) == Typestate.BooleanType) {
			return p;
		}
		String name = "~" + p.getLocalName();
		Production r = p.getGrammarFile().getProduction(name);
		if (r == null) {
			r = p.getGrammarFile().newReducedProduction(name, p, this);
		}
		return r;
	}

	@Override
	public Expression reshapeMatch(Match e) {
		return e.get(0).reshape(this);
	}

	@Override
	public Expression reshapeNew(New e) {
		return empty(e);
	}

	@Override
	public Expression reshapeLink(Link e) {
		return e.get(0).reshape(this);
	}

	@Override
	public Expression reshapeTagging(Tagging e) {
		return empty(e);
	}

	@Override
	public Expression reshapeReplace(Replace e) {
		return empty(e);
	}

	@Override
	public Expression reshapeCapture(Capture e) {
		return empty(e);
	}

}
