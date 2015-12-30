package nez.lang;

import nez.lang.Nez.Byte;
import nez.lang.expr.Expressions;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.util.UList;
import nez.util.Verbose;

public class OldGrammarTransducer extends Expression.Visitor {

	public Expression visitInner(Expression inner) {
		push(inner);
		Expression inner2 = (Expression) inner.visit(this, null);
		pop(inner);
		return inner2;
	}

	protected void push(Expression inner) {
	}

	protected void pop(Expression inner) {
	}

	public Production visitProduction(Production p) {
		p.setExpression(this.visitInner(p.getExpression()));
		return p;
	}

	@Override
	public Expression visitEmpty(Nez.Empty e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitFail(Nez.Fail e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitByte(Byte e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitByteSet(Nez.ByteSet e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitAny(Nez.Any e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitMultiByte(Nez.MultiByte e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitNonTerminal(NonTerminal e, Object a) {
		e.newNonTerminal(e.getLocalName());
		return e;
	}

	@Override
	public Expression visitPair(Nez.Pair e, Object a) {
		Expression first = e.getFirst();
		push(first);
		first = (Expression) first.visit(this, null);
		Expression next = e.getNext();
		push(next);
		next = (Expression) next.visit(this, null);
		pop(e.getNext());
		pop(e.getFirst());
		if (first instanceof Pempty) {
			return next;
		}
		if (first instanceof Pfail) {
			return first;
		}
		return e.newSequence(first, next);
	}

	@Override
	public Expression visitSequence(Nez.Sequence e, Object a) {
		UList<Expression> l = Expressions.newList(e.size());
		for (Expression sub : e) {
			Expressions.addSequence(l, this.visitInner(sub));
		}
		return e.newSequence(l);
	}

	@Override
	public Expression visitChoice(Nez.Choice e, Object a) {
		UList<Expression> l = Expressions.newList(e.size());
		for (Expression sub : e) {
			Expressions.addChoice(l, this.visitInner(sub));
		}
		return e.newChoice(l);
	}

	@Override
	public Expression visitOption(Nez.Option e, Object a) {
		return Expressions.newOption(e.getSourceLocation(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitZeroMore(Nez.ZeroMore e, Object a) {
		return Expressions.newZeroMore(e.getSourceLocation(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitOneMore(Nez.OneMore e, Object a) {
		return Expressions.newOneMore(e.getSourceLocation(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitAnd(Nez.And e, Object a) {
		return Expressions.newAnd(e.getSourceLocation(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitNot(Nez.Not e, Object a) {
		return Expressions.newNot(e.getSourceLocation(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitPreNew(Nez.PreNew e, Object a) {
		return Expressions.newBeginTree(e.getSourceLocation(), e.shift);
	}

	@Override
	public Expression visitLeftFold(Nez.LeftFold e, Object a) {
		return Expressions.newLeftFold(e.getSourceLocation(), e.label, e.shift);
	}

	@Override
	public Expression visitLink(Nez.Link e, Object a) {
		return Expressions.newLinkTree(e.getSourceLocation(), e.label, visitInner(e.get(0)));
	}

	@Override
	public Expression visitTag(Nez.Tag e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitReplace(Nez.Replace e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitNew(Nez.New e, Object a) {
		return Expressions.newEndTree(e.getSourceLocation(), e.shift);
	}

	@Override
	public Expression visitDetree(Nez.Detree e, Object a) {
		return Expressions.newDetree(e.getSourceLocation(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitBlockScope(Nez.BlockScope e, Object a) {
		return Expressions.newBlockScope(e.getSourceLocation(), visitInner(e.get(0)));
	}

	@Override
	public Expression visitLocalScope(Nez.LocalScope e, Object a) {
		return Expressions.newLocalScope(e.getSourceLocation(), e.tableName, visitInner(e.get(0)));
	}

	@Override
	public Expression visitSymbolAction(Nez.SymbolAction e, Object a) {
		return Expressions.newSymbolAction(e.getSourceLocation(), (NonTerminal) visitInner(e.get(0)));
	}

	@Override
	public Expression visitSymbolMatch(Nez.SymbolMatch e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
		return Expressions.newSymbolPredicate(e.getSourceLocation(), e.tableName, visitInner(e.get(0)), e.op == Predicate.is);
	}

	@Override
	public Expression visitSymbolExists(Nez.SymbolExists e, Object a) {
		return Expressions.newSymbolExists(e.getSourceLocation(), e.tableName, e.symbol);
	}

	@Override
	public Expression visitIf(Nez.If e, Object a) {
		return e; // immutable
	}

	@Override
	public Expression visitOn(Nez.On e, Object a) {
		return Expressions.newOn(e.getSourceLocation(), e.isPositive(), e.flagName, visitInner(e.get(0)));
	}

	@Override
	public Expression visitExtended(Expression e, Object a) {
		Verbose.println("TODO: implement visit in " + this.getClass());
		return e;
	}

}
