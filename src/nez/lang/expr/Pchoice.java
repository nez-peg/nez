package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.util.UList;

public class Pchoice extends Nez.Choice {
	Pchoice(SourceLocation s, UList<Expression> l, int size) {
		super(new Expression[size]);
		this.setSourceLocation(s);
		for (int i = 0; i < size; i++) {
			this.inners[i] = l.get(i);
		}
		this.reduced = size;
	}

}
