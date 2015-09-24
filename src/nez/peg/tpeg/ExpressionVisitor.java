package nez.peg.tpeg;

import nez.peg.tpeg.TypedPEG.AnyExpr;
import nez.peg.tpeg.TypedPEG.CharClassExpr;
import nez.peg.tpeg.TypedPEG.ChoiceExpr;
import nez.peg.tpeg.TypedPEG.LabeledExpr;
import nez.peg.tpeg.TypedPEG.NonTerminalExpr;
import nez.peg.tpeg.TypedPEG.OptionalExpr;
import nez.peg.tpeg.TypedPEG.PredicateExpr;
import nez.peg.tpeg.TypedPEG.RepeatExpr;
import nez.peg.tpeg.TypedPEG.RootExpr;
import nez.peg.tpeg.TypedPEG.RuleExpr;
import nez.peg.tpeg.TypedPEG.SequenceExpr;
import nez.peg.tpeg.TypedPEG.StringExpr;
import nez.peg.tpeg.TypedPEG.TypedRuleExpr;

/**
 * Created by skgchxngsxyz-osx on 15/08/28.
 */
public abstract class ExpressionVisitor<T, P> {
	public T visit(TypedPEG expr) {
		return this.visit(expr, null);
	}

	public T visit(TypedPEG expr, P param) {
		return expr.accept(this, param);
	}

	public abstract T visitAnyExpr(AnyExpr expr, P param);

	public abstract T visitStringExpr(StringExpr expr, P param);

	public abstract T visitCharClassExpr(CharClassExpr expr, P param);

	public abstract T visitRepeatExpr(RepeatExpr expr, P param);

	public abstract T visitOptionalExpr(OptionalExpr expr, P param);

	public abstract T visitPredicateExpr(PredicateExpr expr, P param);

	public abstract T visitSequenceExpr(SequenceExpr expr, P param);

	public abstract T visitChoiceExpr(ChoiceExpr expr, P param);

	public abstract T visitNonTerminalExpr(NonTerminalExpr expr, P param);

	public abstract T visitLabeledExpr(LabeledExpr expr, P param);

	public abstract T visitRuleExpr(RuleExpr expr, P param);

	public abstract T visitTypedRuleExpr(TypedRuleExpr expr, P param);

	public abstract T visitRootExpr(RootExpr expr, P param);
}
