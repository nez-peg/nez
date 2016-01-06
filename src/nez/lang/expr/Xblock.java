package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

public class Xblock extends Nez.BlockScope {
	public Xblock(SourceLocation s, Expression e) {
		super(e);
		this.setSourceLocation(s);
	}

}