package nez.peg.tpeg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import nez.peg.tpeg.type.LType;

/**
 * Created by skgchxngsxyz-osx on 15/08/28.
 */
public abstract class TypedPEG {
	protected final LongRange range;

	/**
	 * may be null.
	 */
	protected LType type;

	public TypedPEG(LongRange range) {
		this.range = range;
	}

	public LongRange getRange() {
		return range;
	}

	public LType setType(LType type) {
		return this.type = Objects.requireNonNull(type);
	}

	/**
	 *
	 * @return may be null
	 */
	public LType getType() {
		return type;
	}

	public abstract <T, P> T accept(ExpressionVisitor<T, P> visitor, P param);

	public static class AnyExpr extends TypedPEG {
		public AnyExpr(LongRange range) {
			super(range);
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitAnyExpr(this, param);
		}
	}

	public static class StringExpr extends TypedPEG {
		private final String text;

		/**
		 *
		 * @param text
		 *            must be raw string(not unquoted)
		 */
		public StringExpr(LongRange range, String text) {
			super(range);
			this.text = text;
		}

		public String getText() {
			return text;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitStringExpr(this, param);
		}
	}

	public static class CharClassExpr extends TypedPEG {
		private final String text;

		/**
		 *
		 * @param text
		 *            must be raw string
		 */
		public CharClassExpr(LongRange range, String text) {
			super(range);
			this.text = text;
		}

		public String getText() {
			return text;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitCharClassExpr(this, param);
		}
	}

	/**
	 * for Zero More and One More
	 */
	public static class RepeatExpr extends TypedPEG {
		private final TypedPEG expr;

		/**
		 * if true, represents zero more. if false, represents one more.
		 */
		private final boolean zereMore;

		private RepeatExpr(LongRange range, TypedPEG expr, boolean zeroMore) {
			super(range);
			this.expr = Objects.requireNonNull(expr);
			this.zereMore = zeroMore;
		}

		public static RepeatExpr oneMore(LongRange range, TypedPEG expr) {
			return new RepeatExpr(range, expr, false);
		}

		public static RepeatExpr zeroMore(LongRange range, TypedPEG expr) {
			return new RepeatExpr(range, expr, true);
		}

		public TypedPEG getExpr() {
			return expr;
		}

		public boolean isZereMore() {
			return zereMore;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitRepeatExpr(this, param);
		}
	}

	public static class OptionalExpr extends TypedPEG {
		private final TypedPEG expr;

		public OptionalExpr(LongRange range, TypedPEG expr) {
			super(range);
			this.expr = Objects.requireNonNull(expr);
		}

		public TypedPEG getExpr() {
			return expr;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitOptionalExpr(this, param);
		}
	}

	/**
	 * for And predicate or Not predicate
	 */
	public static class PredicateExpr extends TypedPEG {
		private final TypedPEG expr;

		/**
		 * if true, represents and predicate if fase, represents not predicate
		 */
		private final boolean andPredicate;

		private PredicateExpr(LongRange range, TypedPEG expr, boolean andPredicate) {
			super(range);
			this.expr = Objects.requireNonNull(expr);
			this.andPredicate = andPredicate;
		}

		public static PredicateExpr andPredicate(LongRange range, TypedPEG expr) {
			return new PredicateExpr(range, expr, true);
		}

		public static PredicateExpr notPredicate(LongRange range, TypedPEG expr) {
			return new PredicateExpr(range, expr, false);
		}

		public TypedPEG getExpr() {
			return expr;
		}

		public boolean isAndPredicate() {
			return andPredicate;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitPredicateExpr(this, param);
		}
	}

	public static class SequenceExpr extends TypedPEG {
		private List<TypedPEG> exprs = new ArrayList<>();

		/**
		 * if leftExpr or rightExpr is SequenceExpr, merge to exprs.
		 * 
		 * @param leftExpr
		 * @param rightExpr
		 */
		public SequenceExpr(TypedPEG leftExpr, TypedPEG rightExpr) {
			super(new LongRange(leftExpr.getRange().pos, rightExpr.getRange().len));

			if (leftExpr instanceof SequenceExpr) {
				exprs.addAll(((SequenceExpr) leftExpr).getExprs());
			} else {
				exprs.add(Objects.requireNonNull(leftExpr));
			}

			if (rightExpr instanceof SequenceExpr) {
				exprs.addAll(((SequenceExpr) rightExpr).getExprs());
			} else {
				exprs.add(Objects.requireNonNull(rightExpr));
			}

			// freeze
			this.exprs = Collections.unmodifiableList(this.exprs);
		}

		/**
		 *
		 * @return read only.
		 */
		public List<TypedPEG> getExprs() {
			return exprs;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitSequenceExpr(this, param);
		}
	}

	public static class ChoiceExpr extends TypedPEG {
		private List<TypedPEG> exprs = new ArrayList<>();

		public ChoiceExpr(TypedPEG leftExpr, TypedPEG rightExpr) {
			super(new LongRange(leftExpr.getRange().pos, rightExpr.getRange().len));

			if (leftExpr instanceof ChoiceExpr) {
				this.exprs.addAll(((ChoiceExpr) leftExpr).getExprs());
			} else {
				this.exprs.add(Objects.requireNonNull(leftExpr));
			}

			if (rightExpr instanceof ChoiceExpr) {
				this.exprs.addAll(((ChoiceExpr) rightExpr).getExprs());
			} else {
				this.exprs.add(Objects.requireNonNull(rightExpr));
			}

			// freeze
			this.exprs = Collections.unmodifiableList(this.exprs);
		}

		/**
		 *
		 * @return read only
		 */
		public List<TypedPEG> getExprs() {
			return exprs;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitChoiceExpr(this, param);
		}
	}

	public static class NonTerminalExpr extends TypedPEG {
		private final String name;

		public NonTerminalExpr(LongRange range, String name) {
			super(range);
			this.name = Objects.requireNonNull(name);
		}

		public String getName() {
			return name;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitNonTerminalExpr(this, param);
		}
	}

	public static class LabeledExpr extends TypedPEG {
		private final String labelName;
		private final TypedPEG expr;

		public LabeledExpr(LongRange range, String labelName, TypedPEG expr) {
			super(range);
			this.labelName = Objects.requireNonNull(labelName);
			this.expr = Objects.requireNonNull(expr);
		}

		public String getLabelName() {
			return labelName;
		}

		public TypedPEG getExpr() {
			return expr;
		}

		public LType getExprType() {
			return this.getExpr().getType();
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitLabeledExpr(this, param);
		}
	}

	public static class RuleExpr extends TypedPEG {
		protected final String ruleName;
		protected final TypedPEG expr;

		public RuleExpr(LongRange range, String ruleName, TypedPEG expr) {
			super(range);
			this.ruleName = ruleName;
			this.expr = expr;
		}

		public String getRuleName() {
			return ruleName;
		}

		public TypedPEG getExpr() {
			return expr;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitRuleExpr(this, param);
		}
	}

	public static class TypedRuleExpr extends RuleExpr {
		private final String typeName;

		public TypedRuleExpr(LongRange range, String ruleName, String typeName, TypedPEG expr) {
			super(range, ruleName, expr);
			this.typeName = typeName;
		}

		public String getTypeName() {
			return typeName;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitTypedRuleExpr(this, param);
		}
	}

	public static class RootExpr extends TypedPEG {
		private final List<RuleExpr> exprs;

		public RootExpr(LongRange range, RuleExpr[] exprs) {
			super(range);
			this.exprs = Collections.unmodifiableList(Arrays.asList(exprs));
		}

		/**
		 *
		 * @return read-only list
		 */
		public List<RuleExpr> getExprs() {
			return exprs;
		}

		@Override
		public <T, P> T accept(ExpressionVisitor<T, P> visitor, P param) {
			return visitor.visitRootExpr(this, param);
		}
	}
}
