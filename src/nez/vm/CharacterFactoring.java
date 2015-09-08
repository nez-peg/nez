package nez.vm;

import nez.lang.Expression;
import nez.lang.ExpressionTransducer;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.Choice;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.Sequence;
import nez.util.UList;

public class CharacterFactoring extends ExpressionTransducer {
	public final static CharacterFactoring s = new CharacterFactoring();

	/**
	 * try factoring character
	 * 
	 * @param e
	 * @return
	 */

	public Expression tryFactoringCharacter(Expression e) {
		Expression r = e.reshape(this);
		return r == e ? null : r;
	}

	public Expression reshapeByteChar(Cbyte e) {
		return empty(e);
	}

	public Expression reshapeByteMap(Cset e) {
		return empty(e);
	}

	public Expression reshapeAnyChar(Cany e) {
		return empty(e);
	}

	public Expression reshapeSequence(Sequence e) {
		Expression first = e.get(0).reshape(this);
		if (first == e.get(0)) {
			return e;
		}
		UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
		ExpressionCommons.addSequence(l, first);
		for (int i = 1; i < e.size(); i++) {
			l.add(e.get(i));
		}
		return ExpressionCommons.newSequence(e.getSourcePosition(), l);
	}

	public Expression reshapeChoice(Choice e) {
		UList<Expression> l = new UList<Expression>(new Expression[e.size()]);
		for (Expression sub : e) {
			Expression p = sub.reshape(this);
			if (p == sub) {
				return e;
			}
		}
		return ExpressionCommons.newChoice(e.getSourcePosition(), l);
	}

}
