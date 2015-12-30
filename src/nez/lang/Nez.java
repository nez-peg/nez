package nez.lang;

import nez.ast.Symbol;
import nez.lang.expr.Expressions;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Psequence;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xexists;
import nez.util.UList;

public class Nez {

	public static abstract class Terminal extends Expression {
		@Override
		public final int size() {
			return 0;
		}

		@Override
		public final Expression get(int index) {
			return null;
		}
	}

	public static class Empty extends Terminal {

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.Empty);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitEmpty(this, a);
		}
	}

	public static class Fail extends Terminal {
		@Override
		public final boolean equals(Object o) {
			return (o instanceof Pfail);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitFail(this, a);
		}
	}

	public static interface Character {
	}

	public static class Byte extends Terminal implements Character {
		public final int byteChar;

		public Byte(int byteChar) {
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

	public static class Any extends Terminal implements Character {

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Any);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitAny(this, a);
		}
	}

	public static class ByteSet extends Terminal implements Character {
		public boolean[] byteMap; // Immutable

		public ByteSet() {
			super();
			this.byteMap = new boolean[257];
		}

		public ByteSet(boolean[] b) {
			super();
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

	public static class MultiByte extends Terminal implements Character {
		public byte[] byteSeq;

		public MultiByte(byte[] byteSeq) {
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

	public static abstract class Unary extends Expression {
		public Expression inner;

		public Unary(Expression e) {
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

	public static class Option extends Nez.Unary {
		public Option(Expression e) {
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

	public static interface Repetition {
	}

	public static class ZeroMore extends Unary implements Repetition {
		public ZeroMore(Expression e) {
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

	public static class OneMore extends Unary implements Repetition {
		public OneMore(Expression e) {
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

	public static class And extends Unary {
		public And(Expression e) {
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

	public static class Not extends Unary {
		public Not(Expression e) {
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

	public static class Pair extends Expression {
		public Expression first;
		public Expression next;

		public Pair(Expression first, Expression next) {
			this.first = first;
			this.next = next;
		}

		@Override
		public final int size() {
			return 2;
		}

		@Override
		public final Expression get(int index) {
			return index == 0 ? this.first : this.next;
		}

		@Override
		public final Expression set(int index, Expression e) {
			Expression p = this.first;
			if (index == 0) {
				this.first = e;
			} else if (index == 1) {
				p = this.next;
				this.next = e;
			}
			return p;
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

		public final UList<Expression> toList() {
			UList<Expression> l = Expressions.newList(4);
			Nez.Pair p = this;
			while (true) {
				l.add(p.getFirst());
				Expression e = p.getNext();
				if (!(e instanceof Nez.Sequence)) {
					break;
				}
				p = (Psequence) e;
			}
			l.add(p.getNext());
			return l;
		}

	}

	public abstract static class List extends Expression {
		public Expression[] inners;

		public List(Expression[] inners) {
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

	public static class Sequence extends Nez.List {

		public Sequence(Expression[] inners) {
			super(inners);
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

	public static class Choice extends List {

		public Choice(Expression[] inners) {
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

	public abstract static interface AST {
	}

	public static class PreNew extends Terminal implements AST {
		public int shift = 0;

		public PreNew(int shift) {
			this.shift = shift;
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.PreNew && this.shift == ((Nez.PreNew) o).shift);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitPreNew(this, a);
		}

	}

	public static class New extends Terminal implements AST {
		public int shift = 0;

		public New(int shift) {
			this.shift = shift;
		}

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.New && this.shift == ((Nez.New) o).shift);
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitNew(this, a);
		}

	}

	public static class LeftFold extends Terminal implements AST {
		public int shift;
		public final Symbol label;

		public LeftFold(int shift, Symbol label) {
			this.label = label;
			this.shift = shift;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.LeftFold) {
				Nez.LeftFold s = (Nez.LeftFold) o;
				return (this.label == s.label && this.shift == s.shift);
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitLeftFold(this, a);
		}

	}

	public static class Tag extends Terminal implements AST {
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
				return this.tag == ((Ttag) o).tag;
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitTag(this, a);
		}
	}

	public static class Replace extends Terminal implements AST {
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

	public static abstract class Action extends Terminal implements AST {
		Object value;
	}

	public static class Link extends Unary implements AST {
		public Symbol label;

		public Link(Symbol label, Expression e) {
			super(e);
			this.label = label;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Link && this.label == ((Nez.Link) o).label) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final Object visit(Expression.Visitor v, Object a) {
			return v.visitLink(this, a);
		}
	}

	public static class Detree extends Unary implements AST {
		public Detree(Expression e) {
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
		public final Predicate op;

		public Function(Predicate op, Expression e) {
			super(e);
			this.op = op;
		}

		public boolean hasInnerExpression() {
			return this.get(0) != empty;
		}

	}

	public static class SymbolAction extends Function {
		public final Symbol tableName;

		public SymbolAction(Predicate op, NonTerminal e) {
			super(op, e);
			tableName = Symbol.tag(e.getLocalName());
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

	public static class SymbolPredicate extends Function {
		public final Symbol tableName;

		public SymbolPredicate(Predicate op, Symbol table, Expression e) {
			super(op, e);
			this.tableName = table;
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

	public static class SymbolMatch extends Function {
		public final Symbol tableName;

		public SymbolMatch(Predicate op, Symbol table) {
			super(op, empty);
			this.tableName = table;
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

	public static class SymbolExists extends Function implements AST {
		public final Symbol tableName;
		public final String symbol;

		public SymbolExists(Symbol table, String symbol) {
			super(Predicate.exists, empty);
			this.tableName = table;
			this.symbol = symbol;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Xexists) {
				Xexists s = (Xexists) o;
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

	public static class BlockScope extends Function implements AST {
		public BlockScope(Expression e) {
			super(Predicate.block, e);
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

	public static class LocalScope extends Function implements AST {
		public final Symbol tableName;

		public LocalScope(Symbol table, Expression e) {
			super(Predicate.local, e);
			this.tableName = table;
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

	public static class On extends Function {
		public final boolean predicate;
		public final String flagName;

		public On(boolean predicate, String c, Expression e) {
			super(Predicate.on, e);
			if (c.startsWith("!")) {
				predicate = false;
				c = c.substring(1);
			}
			this.predicate = predicate;
			this.flagName = c;
		}

		public final boolean isPositive() {
			return predicate;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.On) {
				Nez.On e = (Nez.On) o;
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

	public static class If extends Function implements Conditional {
		public final boolean predicate;
		public final String flagName;

		public If(boolean predicate, String c) {
			super(Predicate._if, empty);
			if (c.startsWith("!")) {
				predicate = false;
				c = c.substring(1);
			}
			this.predicate = predicate;
			this.flagName = c;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.If) {
				Nez.If e = (Nez.If) o;
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

		public SetCount(long mask, Expression e) {
			super(Predicate.setcount, e);
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

		public Count(Expression e) {
			super(Predicate.count, e);
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
