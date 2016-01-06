package nez.parser.moz;

import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;

class MozGrammarRewriter extends MozGrammarTransducer {

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
	public Expression visitByte(Nez.Byte e, Object a) {
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
		Expression first = e.get(0);
		push(first);
		first = (Expression) first.visit(this, a);
		Expression next = e.get(1);
		push(next);
		next = (Expression) next.visit(this, a);
		pop(e.get(1));
		pop(e.get(0));
		if (first instanceof Nez.Empty) {
			return next;
		}
		if (first instanceof Nez.Fail || next instanceof Nez.Empty) {
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
	public Expression visitBeginTree(Nez.BeginTree e, Object a) {
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
	public Expression visitEndTree(Nez.EndTree e, Object a) {
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