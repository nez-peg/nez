package nez.lang.expr;

import nez.ast.SourceLocation;
import nez.lang.Expression;
import nez.lang.Nez;

public class Xif extends Nez.IfCondition implements Expression.Conditional {

	Xif(SourceLocation s, boolean predicate, String flagName) {
		super(predicate, flagName);
		this.setSourceLocation(s);
	}

	public final String getFlagName() {
		return this.flagName;
	}

	public boolean isPredicate() {
		return predicate;
	}

}