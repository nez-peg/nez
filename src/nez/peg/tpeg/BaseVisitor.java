package nez.peg.tpeg;

/**
 * Created by skgchxngsxyz-opensuse on 15/09/02.
 */
public abstract class BaseVisitor<R, P> extends ExpressionVisitor<R, P> {
	public abstract R visitDefault(TypedPEG expr, P param);

	@Override
	public R visitAnyExpr(TypedPEG.AnyExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitStringExpr(TypedPEG.StringExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitCharClassExpr(TypedPEG.CharClassExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitRepeatExpr(TypedPEG.RepeatExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitOptionalExpr(TypedPEG.OptionalExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitPredicateExpr(TypedPEG.PredicateExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitSequenceExpr(TypedPEG.SequenceExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitChoiceExpr(TypedPEG.ChoiceExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitNonTerminalExpr(TypedPEG.NonTerminalExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitLabeledExpr(TypedPEG.LabeledExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitRuleExpr(TypedPEG.RuleExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitTypedRuleExpr(TypedPEG.TypedRuleExpr expr, P param) {
		return this.visitDefault(expr, param);
	}

	@Override
	public R visitRootExpr(TypedPEG.RootExpr expr, P param) {
		return this.visitDefault(expr, param);
	}
}
