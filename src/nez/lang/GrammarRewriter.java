package nez.lang;

import nez.lang.Nez.Byte;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;

public class GrammarRewriter extends GrammarTransducer {

	@Override
	public Production visitProduction(Production p) {
		return p;
	}

	@Override
	public Expression visitEmpty(Nez.Empty e, Object a) {
		return e;
	}

	@Override
	public Expression visitFail(Nez.Fail e, Object a) {
		return e;
	}

	@Override
	public Expression visitByte(Byte e, Object a) {
		return e;
	}

	@Override
	public Expression visitByteSet(Nez.ByteSet e, Object a) {
		return e;
	}

	@Override
	public Expression visitAny(Nez.Any e, Object a) {
		return e;
	}

	@Override
	public Expression visitMultiByte(Nez.MultiByte e, Object a) {
		return e;
	}

	@Override
	public Expression visitNonTerminal(NonTerminal e, Object a) {
		return e;
	}

	@Override
	public Expression visitPair(Nez.Pair e, Object a) {
		Expression first = e.getFirst();
		push(first);
		first = (Expression) first.visit(this, a);
		Expression next = e.getNext();
		push(next);
		next = (Expression) next.visit(this, a);
		pop(e.getNext());
		pop(e.getFirst());
		if (first instanceof Pempty) {
			return next;
		}
		if (first instanceof Pfail || next instanceof Pempty) {
			return first;
		}
		e.first = first;
		e.next = next;
		return e;
	}

	@Override
	public Expression visitChoice(Nez.Choice e, Object a) {
		int i = 0;
		for (i = 0; i < e.size(); i++) {
			e.set(i, visitInner(e.get(i)));
		}
		return e;
	}

	@Override
	public Expression visitOption(Nez.Option e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitZeroMore(Nez.ZeroMore e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitOneMore(Nez.OneMore e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitAnd(Nez.And e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitNot(Nez.Not e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitDetree(Nez.Detree e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitPreNew(Nez.PreNew e, Object a) {
		return e;
	}

	@Override
	public Expression visitLink(Nez.Link e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitTag(Nez.Tag e, Object a) {
		return e;
	}

	@Override
	public Expression visitReplace(Nez.Replace e, Object a) {
		return e;
	}

	@Override
	public Expression visitNew(Nez.New e, Object a) {
		return e;
	}

	@Override
	public Expression visitBlockScope(Nez.BlockScope e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitLocalScope(Nez.LocalScope e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitSymbolAction(Nez.SymbolAction e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitSymbolMatch(Nez.SymbolMatch e, Object a) {
		return e;
	}

	@Override
	public Expression visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

	@Override
	public Expression visitSymbolExists(Nez.SymbolExists e, Object a) {
		return e;
	}

	@Override
	public Expression visitIf(Nez.If e, Object a) {
		return e;
	}

	@Override
	public Expression visitOn(Nez.On e, Object a) {
		e.set(0, this.visitInner(e.get(0)));
		return e;
	}

}