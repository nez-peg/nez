package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

class Xon extends Nez.On implements Expression.Conditional {
	boolean predicate;

	String flagName;

	public final String getFlagName() {
		return this.flagName;
	}

	Xon(SourceLocation s, boolean predicate, String flagName, Expression inner) {
		super(predicate, flagName, inner);
		this.setSourceLocation(s);
	}

}