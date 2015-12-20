package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.PossibleAcceptance;
import nez.util.UList;

public class Pchoice extends Nez.Choice {
	Pchoice(SourceLocation s, UList<Expression> l, int size) {
		this.set(s);
		this.inners = new Expression[size];
		for (int i = 0; i < size; i++) {
			this.inners[i] = l.get(i);
		}
		this.reduced = size;
	}

	@Override
	public boolean isConsumed() {
		boolean afterAll = true;
		for (Expression e : this) {
			if (!e.isConsumed()) {
				afterAll = false;
			}
		}
		return afterAll;
	}

	@Override
	public short acceptByte(int ch) {
		boolean hasUnconsumed = false;
		for (int i = 0; i < this.size(); i++) {
			short r = this.get(i).acceptByte(ch);
			if (r == PossibleAcceptance.Accept) {
				return r;
			}
			if (r == PossibleAcceptance.Unconsumed) {
				hasUnconsumed = true;
			}
		}
		return hasUnconsumed ? PossibleAcceptance.Unconsumed : PossibleAcceptance.Reject;
	}

}
