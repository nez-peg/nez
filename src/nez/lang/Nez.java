package nez.lang;

import nez.ast.Symbol;

public class Nez {

	/**
	 * SingleCharacter represents the single character property.
	 * 
	 * @author kiki
	 *
	 */

	public static interface SingleCharacter {
	}

	public abstract static interface TreeConstruction {
	}

	static abstract class Terminal extends Expression {
		@Override
		public final int size() {
			return 0;
		}

		@Override
		public final Expression get(int index) {
			return null;
		}
	}

	public static abstract class Unary extends Expression {
		public Expression inner;

		protected Unary(Expression e) {
			this.inner = e;
		}

		@Override
		public final int size() {
			return 1;
		}

		@Override
		public final Expression get(int index) {
			return this.inner;
		}

		@Override
		public final Expression set(int index, Expression e) {
			Expression old = this.inner;
			this.inner = e;
			return old;
		}
	}

	/**
	 * The Nez.Empty represents an empty expression, denoted '' in Nez.
	 * 
	 * @author kiki
	 *
	 */

	public static class Empty extends Terminal {

		Empty() {
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.Empty);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitEmpty(this, a);
		}
	}

	/**
	 * The Nez.Fail represents a failure expression, denoted !'' in Nez.
	 * 
	 * @author kiki
	 *
	 */

	public static class Fail extends Terminal {
		Fail() {
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.Fail);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitFail(this, a);
		}
	}

	/**
	 * Nez.Byte represents a single-byte string literal, denoted as 'a' in Nez.
	 * 
	 * @author kiki
	 *
	 */

	public static class Byte extends Terminal implements SingleCharacter {
		/**
		 * byteChar
		 */
		public final int byteChar;

		Byte(int byteChar) {
			this.byteChar = byteChar;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Byte) {
				return this.byteChar == ((Nez.Byte) o).byteChar;
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitByte(this, a);
		}
	}

	/**
	 * Nez.Any represents an any character, denoted as . in Nez.
	 * 
	 * @author kiki
	 *
	 */

	public static class Any extends Terminal implements SingleCharacter {

		Any() {
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Any);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitAny(this, a);
		}
	}

	/**
	 * Nez.ByteSet is a bitmap-based representation of the character class [X-y]
	 * 
	 * @author kiki
	 *
	 */

	public static class ByteSet extends Terminal implements SingleCharacter {
		/**
		 * a 256-length bitmap array, represeting a character acceptance
		 */
		public final boolean[] byteMap; // Immutable

		ByteSet(boolean[] b) {
			this.byteMap = b;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof ByteSet) {
				ByteSet e = (ByteSet) o;
				for (int i = 0; i < this.byteMap.length; i++) {
					if (this.byteMap[i] != e.byteMap[i]) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitByteSet(this, a);
		}
	}

	/**
	 * Nez.MultiByte represents a byte-encoded string expression, such as 'abc'
	 * in Nez.
	 * 
	 * @author kiki
	 *
	 */

	public static class MultiByte extends Terminal implements SingleCharacter {
		public final byte[] byteSeq;

		MultiByte(byte[] byteSeq) {
			this.byteSeq = byteSeq;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.MultiByte) {
				Nez.MultiByte mb = (Nez.MultiByte) o;
				if (mb.byteSeq.length == this.byteSeq.length) {
					for (int i = 0; i < this.byteSeq.length; i++) {
						if (byteSeq[i] != mb.byteSeq[i]) {
							return false;
						}
					}
					return true;
				}
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitMultiByte(this, a);
		}
	}

	/* Unary */

	/**
	 * Nez.Option represents an optional expression e?
	 * 
	 * @author kiki
	 *
	 */

	public static class Option extends Nez.Unary {
		Option(Expression e) {
			super(e);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Option) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitOption(this, a);
		}

	}

	/**
	 * Nez.Repetition is used to identify a common property of ZeroMore and
	 * OneMore expressions.
	 * 
	 * @author kiki
	 *
	 */

	public static interface Repetition {
		Expression get(int index);
	}

	/**
	 * Nez.ZeroMore represents a zero or more repetition e*.
	 * 
	 * @author kiki
	 *
	 */

	public static class ZeroMore extends Unary implements Repetition {
		ZeroMore(Expression e) {
			super(e);
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof Nez.ZeroMore) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitZeroMore(this, a);
		}

	}

	/**
	 * Nez.OneMore represents a one or more repetition e+.
	 * 
	 * @author kiki
	 *
	 */

	public static class OneMore extends Unary implements Repetition {
		OneMore(Expression e) {
			super(e);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.OneMore) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitOneMore(this, a);
		}
	}

	/**
	 * Nez.And represents an and-predicate &e.
	 * 
	 * @author kiki
	 *
	 */

	public static class And extends Unary {
		And(Expression e) {
			super(e);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.And) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitAnd(this, a);
		}
	}

	/**
	 * Nez.Not represents a not-predicate !e.
	 * 
	 * @author kiki
	 *
	 */

	public static class Not extends Unary {
		Not(Expression e) {
			super(e);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Not) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitNot(this, a);
		}
	}

	/**
	 * Nez.Pair is a pair representation of Nez.Sequence.
	 * 
	 * @author kiki
	 *
	 */

	public static class Pair extends Expression {
		public Expression first;
		public Expression next;

		Pair(Expression first, Expression next) {
			this.first = first;
			this.next = next;
		}

		@Override
		public final int size() {
			return 2;
		}

		@Override
		public final Expression get(int index) {
			assert (index < 2);
			return index == 0 ? this.first : this.next;
		}

		@Override
		public final Expression set(int index, Expression e) {
			assert (index < 2);
			if (index == 0) {
				Expression p = this.first;
				this.first = e;
				return p;
			} else {
				Expression p = this.next;
				this.next = e;
				return p;
			}
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Pair) {
				return this.get(0).equals(((Expression) o).get(0)) && this.get(1).equals(((Expression) o).get(1));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitPair(this, a);
		}

	}

	abstract static class List extends Expression {
		public Expression[] inners;

		protected List(Expression[] inners) {
			this.inners = inners;
		}

		@Override
		public final int size() {
			return this.inners.length;
		}

		@Override
		public final Expression get(int index) {
			return this.inners[index];
		}

		@Override
		public Expression set(int index, Expression e) {
			Expression oldExpresion = this.inners[index];
			this.inners[index] = e;
			return oldExpresion;
		}

		protected final boolean equalsList(Nez.List l) {
			if (this.size() == l.size()) {
				for (int i = 0; i < this.size(); i++) {
					if (!this.get(i).equals(l.get(i))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
	}

	/**
	 * Nez.Sequence is a standard representation of the sequence e e e .
	 * 
	 * @author kiki
	 *
	 */

	public static class Sequence extends Nez.List {

		Sequence(Expression[] inners) {
			super(inners);
			this.setSourceLocation(inners[0].getSourceLocation());
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Sequence) {
				return this.equalsList((Nez.List) o);
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitSequence(this, a);
		}
	}

	/**
	 * Nez.Choice represents an ordered choice e / ... / e_n in Nez.
	 * 
	 * @author kiki
	 *
	 */

	public static class Choice extends List {

		Choice(Expression[] inners) {
			super(inners);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Choice) {
				return this.equalsList((Nez.List) o);
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitChoice(this, a);
		}

		/* optimized */

		public boolean isTrieTree = false;
		public Expression[] predictedCase = null;
		public float reduced;
		public Expression[] firstInners = null;
		private final static Expression[] optimized = new Expression[0];

		public boolean isOptimized() {
			return this.firstInners != null;
		}

		public void setOptimized() {
			this.firstInners = optimized;
		}
	}

	/* AST */

	public static class BeginTree extends Terminal implements TreeConstruction {
		public int shift = 0;

		public BeginTree(int shift) {
			this.shift = shift;
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.BeginTree && this.shift == ((Nez.BeginTree) o).shift);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitBeginTree(this, a);
		}

	}

	public static class EndTree extends Terminal implements TreeConstruction {
		public int shift = 0;

		public EndTree(int shift) {
			this.shift = shift;
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.EndTree && this.shift == ((Nez.EndTree) o).shift);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitEndTree(this, a);
		}

	}

	public static class FoldTree extends Terminal implements TreeConstruction {
		public int shift;
		public final Symbol label;

		public FoldTree(int shift, Symbol label) {
			this.label = label;
			this.shift = shift;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.FoldTree) {
				Nez.FoldTree s = (Nez.FoldTree) o;
				return (this.label == s.label && this.shift == s.shift);
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitFoldTree(this, a);
		}

	}

	public static class Tag extends Terminal implements TreeConstruction {
		public final Symbol tag;

		public Tag(Symbol tag) {
			this.tag = tag;
		}

		public final String getTagName() {
			return tag.getSymbol();
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Tag) {
				return this.tag == ((Nez.Tag) o).tag;
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitTag(this, a);
		}
	}

	public static class Replace extends Terminal implements TreeConstruction {
		public String value;

		public Replace(String value) {
			this.value = value;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Replace) {
				return this.value.equals(((Nez.Replace) o).value);
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitReplace(this, a);
		}
	}

	public static abstract class Action extends Terminal implements TreeConstruction {
		Object value;
	}

	public static class LinkTree extends Unary implements TreeConstruction {
		public Symbol label;

		public LinkTree(Symbol label, Expression e) {
			super(e);
			this.label = label;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.LinkTree && this.label == ((Nez.LinkTree) o).label) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitLinkTree(this, a);
		}
	}

	public static class Detree extends Unary implements TreeConstruction {
		Detree(Expression e) {
			super(e);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Detree) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitDetree(this, a);
		}

	}

	/* Symbol */
	private static Expression empty = Expressions.newEmpty(null);

	public static abstract class Function extends Unary {
		public final FunctionName op;

		protected Function(FunctionName op, Expression e) {
			super(e);
			this.op = op;
		}

		public boolean hasInnerExpression() {
			return this.get(0) != empty;
		}
	}

	public abstract static class SymbolFunction extends Function {
		public final Symbol tableName;

		SymbolFunction(FunctionName op, Expression e, Symbol symbol) {
			super(op, e);
			tableName = symbol;
		}
	}

	public static class SymbolAction extends SymbolFunction {
		SymbolAction(FunctionName op, NonTerminal e) {
			super(op, e, Symbol.unique(e.getLocalName()));
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof SymbolAction) {
				SymbolAction e = (SymbolAction) o;
				if (this.tableName == e.tableName) {
					return this.get(0).equals(e.get(0));
				}
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolAction(this, a);
		}

	}

	public static class SymbolPredicate extends SymbolFunction {
		SymbolPredicate(FunctionName op, NonTerminal pat, Symbol table) {
			super(op, pat, table == null ? Symbol.unique(pat.getLocalName()) : table);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof SymbolPredicate) {
				SymbolPredicate e = (SymbolPredicate) o;
				return e.op == this.op && this.tableName == e.tableName;
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolPredicate(this, a);
		}
	}

	public static class SymbolMatch extends SymbolFunction {

		SymbolMatch(FunctionName op, NonTerminal pat, Symbol table) {
			super(op, pat, table == null ? Symbol.unique(pat.getLocalName()) : table);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof SymbolMatch) {
				SymbolMatch e = (SymbolMatch) o;
				return e.op == this.op && this.tableName == e.tableName;
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolMatch(this, a);
		}

	}

	public static class SymbolExists extends SymbolFunction {
		public final String symbol;

		SymbolExists(Symbol table, String symbol) {
			super(FunctionName.exists, empty, table);
			this.symbol = symbol;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof SymbolExists) {
				SymbolExists s = (SymbolExists) o;
				return this.tableName == s.tableName && equals(this.symbol, s.symbol);
			}
			return false;
		}

		private boolean equals(String s, String s2) {
			if (s != null && s2 != null) {
				return s.equals(s2);
			}
			return s == s2;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolExists(this, a);
		}

	}

	public static class BlockScope extends Function {
		BlockScope(Expression e) {
			super(FunctionName.block, e);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof BlockScope) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitBlockScope(this, a);
		}

	}

	public static class LocalScope extends SymbolFunction {

		LocalScope(Symbol table, Expression e) {
			super(FunctionName.local, e, table);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof LocalScope) {
				LocalScope s = (LocalScope) o;
				if (this.tableName == s.tableName) {
					return this.get(0).equals(s.get(0));
				}
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitLocalScope(this, a);
		}

	}

	public static interface Conditional {

	}

	public static class OnCondition extends Function {
		public final boolean predicate;
		public final String flagName;

		OnCondition(boolean predicate, String c, Expression e) {
			super(FunctionName.on, e);
			this.predicate = predicate;
			this.flagName = c;
		}

		public final boolean isPositive() {
			return predicate;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.OnCondition) {
				Nez.OnCondition e = (Nez.OnCondition) o;
				if (this.predicate == e.predicate && this.flagName.equals(e.flagName)) {
					return this.get(0).equals(e.get(0));
				}
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitOn(this, a);
		}
	}

	public static class IfCondition extends Function implements Conditional {
		public final boolean predicate;
		public final String flagName;

		IfCondition(boolean predicate, String c) {
			super(FunctionName._if, empty);
			this.predicate = predicate;
			this.flagName = c;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.IfCondition) {
				Nez.IfCondition e = (Nez.IfCondition) o;
				return this.predicate == e.predicate && this.flagName.equals(e.flagName);
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitIf(this, a);
		}
	}

	public static class SetCount extends Function {
		public final long mask;

		SetCount(long mask, Expression e) {
			super(FunctionName.setcount, e);
			this.mask = mask;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.SetCount) {
				Nez.SetCount e = (Nez.SetCount) o;
				return this.mask == e.mask;
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitExtended(this, a);
		}
	}

	public static class Count extends Function {

		Count(Expression e) {
			super(FunctionName.count, e);
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Count) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitExtended(this, a);
		}
	}

}
