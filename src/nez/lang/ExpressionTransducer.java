package nez.lang;

import nez.lang.expr.Uand;
import nez.lang.expr.Cany;
import nez.lang.expr.Xblock;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Choice;
import nez.lang.expr.Xdef;
import nez.lang.expr.Empty;
import nez.lang.expr.Xexists;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.Failure;
import nez.lang.expr.Xif;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Tlink;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Umatch;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Tnew;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Unot;
import nez.lang.expr.Xon;
import nez.lang.expr.Uoption;
import nez.lang.expr.Uzero;
import nez.lang.expr.Uone;
import nez.lang.expr.Treplace;
import nez.lang.expr.Sequence;
import nez.lang.expr.Ttag;
import nez.util.UFlag;
import nez.util.UList;

public class ExpressionTransducer {
	public final static ExpressionTransducer RemoveASTandRename = new ASTConstructionEliminator(true);
	public final static ExpressionTransducer RemoveAST = new ASTConstructionEliminator(false);

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

	public Expression reshapeByteChar(Cbyte e) {
		return e;
	}

	public Expression reshapeByteMap(Cset e) {
		return e;
	}

	public Expression reshapeAnyChar(Cany e) {
		return e;
	}

	public Expression reshapeCharMultiByte(Cmulti e) {
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

	public Expression reshapeOption(Uoption e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeRepetition(Uzero e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeRepetition1(Uone e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeAnd(Uand e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeNot(Unot e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeMatch(Umatch e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeNew(Tnew e) {
		return e;
	}

	public Expression reshapeLink(Tlink e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeTagging(Ttag e) {
		return e;
	}

	public Expression reshapeReplace(Treplace e) {
		return e;
	}

	public Expression reshapeCapture(Tcapture e) {
		return e;
	}

	public Expression reshapeBlock(Xblock e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeLocalTable(Xlocal e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeDefSymbol(Xdef e) {
		Expression inner = e.get(0).reshape(this);
		return updateInner(e, inner);
	}

	public Expression reshapeMatchSymbol(Xmatch e) {
		return e;
	}

	public Expression reshapeIsSymbol(Xis e) {
		return e;
	}

	public Expression reshapeExistsSymbol(Xexists e) {
		return e;
	}

	public Expression reshapeIsIndent(Xindent e) {
		return e;
	}

	public Expression reshapeIfFlag(Xif e) {
		return e;
	}

	public Expression reshapeXon(Xon e) {
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

	protected final Expression updateInner(Uoption e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Uzero e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Uone e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Uand e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Unot e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Umatch e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Tlink e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Xblock e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Xlocal e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Xdef e, Expression inner) {
		e.inner = inner;
		return e;
	}

	protected final Expression updateInner(Xon e, Expression inner) {
		e.inner = inner;
		return e;
	}

}

class ASTConstructionEliminator extends ExpressionTransducer {
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
	public Expression reshapeMatch(Umatch e) {
		return e.get(0).reshape(this);
	}

	@Override
	public Expression reshapeNew(Tnew e) {
		return empty(e);
	}

	@Override
	public Expression reshapeLink(Tlink e) {
		return e.get(0).reshape(this);
	}

	@Override
	public Expression reshapeTagging(Ttag e) {
		return empty(e);
	}

	@Override
	public Expression reshapeReplace(Treplace e) {
		return empty(e);
	}

	@Override
	public Expression reshapeCapture(Tcapture e) {
		return empty(e);
	}

}
