package nez.lang.expr;

import java.util.List;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Pchoice extends Nez.Choice {
	Pchoice(SourceLocation s, List<Expression> l, int size) {
		super(new Expression[size]);
		this.setSourceLocation(s);
		for (int i = 0; i < size; i++) {
			this.inners[i] = l.get(i);
		}
		this.reduced = size;
	}

}
