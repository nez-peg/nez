package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
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

}
