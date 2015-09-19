package nez.peg.tpeg;

import nez.Grammar;
import nez.lang.Expression;
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
import nez.peg.tpeg.type.LType;
import nez.peg.tpeg.type.LType.TupleType;
import nez.peg.tpeg.type.LType.UnionType;

public class NezGrammarGenerator extends ExpressionVisitor<Expression, Grammar> {

	private static String unquote(String text) {
		return text.substring(1, text.length() - 1);
	}

	private static Expression newTag(Grammar g, LType type) {
		return g.newTagging(type.getUniqueName());
	}

	@Override
	public Expression visitAnyExpr(AnyExpr expr, Grammar param) {
		return param.newAnyChar();
	}

	@Override
	public Expression visitStringExpr(StringExpr expr, Grammar param) {
		return param.newString(unquote(expr.getText()));
	}

	@Override
	public Expression visitCharClassExpr(CharClassExpr expr, Grammar param) {
		return param.newCharSet(unquote(expr.getText()));
	}

	@Override
	public Expression visitRepeatExpr(RepeatExpr expr, Grammar param) {
		Expression repeat = null;
		if (expr.isZereMore()) {
			repeat = param.newRepetition(new Expression[] { this.visit(expr.getExpr(), param) });
		} else {
			repeat = param.newRepetition1(new Expression[] { this.visit(expr.getExpr(), param) });
		}
		if (expr.getType().isVoid()) {
			return repeat;
		} else {
			return param.newNew(repeat, newTag(param, expr.getType()));
		}
	}

	@Override
	public Expression visitOptionalExpr(OptionalExpr expr, Grammar param) {
		Expression optional = param.newOption(this.visit(expr.getExpr(), param));
		if (expr.getType().isVoid()) {
			return optional;
		} else {
			return param.newNew(optional, newTag(param, expr.getType()));
		}
	}

	@Override
	public Expression visitPredicateExpr(PredicateExpr expr, Grammar param) {
		if (expr.isAndPredicate()) {
			return param.newAnd(this.visit(expr.getExpr(), param));
		} else {
			return param.newNot(this.visit(expr.getExpr(), param));
		}
	}

	@Override
	public Expression visitSequenceExpr(SequenceExpr expr, Grammar param) {
		final int size = expr.getExprs().size();
		Expression seq[] = new Expression[size];
		for (int i = 0; i < size; i++) {
			seq[i] = this.visit(expr.getExprs().get(i), param);
		}
		Expression sequence = param.newSequence(seq);

		if (!(expr.getType() instanceof TupleType)) {
			return sequence;
		} else {
			return param.newNew(sequence, newTag(param, expr.getType()));
		}
	}

	@Override
	public Expression visitChoiceExpr(ChoiceExpr expr, Grammar param) {
		final int size = expr.getExprs().size();
		Expression seq[] = new Expression[size];
		for (int i = 0; i < size; i++) {
			seq[i] = this.visit(expr.getExprs().get(i), param);
		}
		Expression choice = param.newChoice(seq);

		if (!(expr.getType() instanceof UnionType)) {
			return choice;
		} else {
			return param.newNew(choice, newTag(param, expr.getType()));
		}
	}

	@Override
	public Expression visitNonTerminalExpr(NonTerminalExpr expr, Grammar param) {
		return param.newNonTerminal(expr.getName());
	}

	@Override
	public Expression visitLabeledExpr(LabeledExpr expr, Grammar param) {
		return param.newLink(this.visit(expr.getExpr(), param));
	}

	@Override
	public Expression visitRuleExpr(RuleExpr expr, Grammar param) {
		param.newProduction(expr.getRuleName(), this.visit(expr.getExpr(), param));
		return null;
	}

	@Override
	public Expression visitTypedRuleExpr(TypedRuleExpr expr, Grammar param) {
		param.newProduction(expr.getRuleName(), param.newNew(this.visit(expr.getExpr(), param), newTag(param, expr.getType())));
		return null;
	}

	@Override
	public Expression visitRootExpr(RootExpr expr, Grammar param) {
		for (RuleExpr r : expr.getExprs()) {
			this.visit(r, param);
		}
		return null;
	}
}
