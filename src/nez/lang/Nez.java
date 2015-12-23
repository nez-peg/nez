package nez.lang;

import nez.ast.Symbol;
import nez.lang.expr.ExpressionCommons;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Psequence;
import nez.lang.expr.Tlink;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xexists;
import nez.util.StringUtils;
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

	public abstract static class Empty extends Terminal {

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Pempty);
		}

		@Override
		public final void format(StringBuilder sb) {
			sb.append("''");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitEmpty(this, a);
		}
	}

	public abstract static class Fail extends Terminal {
		@Override
		public final boolean equals(Object o) {
			return (o instanceof Pfail);
		}

		@Override
		public final void format(StringBuilder sb) {
			sb.append("!''");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitFail(this, a);
		}
	}

	public static interface Character {
	}

	public abstract static class Byte extends Terminal implements Character {
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
		public final void format(StringBuilder sb) {
			sb.append(StringUtils.stringfyCharacter(this.byteChar));
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitByte(this, a);
		}
	}

	public static abstract class Any extends Terminal implements Character {

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Any);
		}

		@Override
		public final void format(StringBuilder sb) {
			sb.append(".");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitAny(this, a);
		}
	}

	public static abstract class ByteSet extends Terminal implements Character {
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
		public final void format(StringBuilder sb) {
			sb.append(StringUtils.stringfyCharacterClass(this.byteMap));
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitByteSet(this, a);
		}
	}

	public static abstract class String extends Terminal implements Character {
		public byte[] byteSeq;

		public String(byte[] byteSeq) {
			this.byteSeq = byteSeq;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.String) {
				Nez.String mb = (Nez.String) o;
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
		public final void format(StringBuilder sb) {
			sb.append("'");
			for (int i = 0; i < this.byteSeq.length; i++) {
				StringUtils.appendByteChar(sb, byteSeq[i] & 0xff, "\'");
			}
			sb.append("'");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitString(this, a);
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

		protected final void formatUnary(StringBuilder sb, java.lang.String prefix, Expression inner, java.lang.String suffix) {
			if (prefix != null) {
				sb.append(prefix);
			}
			if (inner instanceof NonTerminal || inner instanceof Nez.Terminal) {
				inner.format(sb);
			} else {
				sb.append("(");
				inner.format(sb);
				sb.append(")");
			}
			if (suffix != null) {
				sb.append(suffix);
			}
		}

	}

	public abstract static class Option extends Nez.Unary {
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
		public final void format(StringBuilder sb) {
			this.formatUnary(sb, null, this.inner, "?");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitOption(this, a);
		}

	}

	public static interface Repetition {
	}

	public abstract static class ZeroMore extends Unary implements Repetition {
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
		public void format(StringBuilder sb) {
			this.formatUnary(sb, null, this.inner, "*");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitZeroMore(this, a);
		}

	}

	public abstract static class OneMore extends Unary implements Repetition {
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
		public final void format(StringBuilder sb) {
			this.formatUnary(sb, null, this.inner, "+");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitOneMore(this, a);
		}

	}

	public abstract static class And extends Unary {
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
		public final void format(StringBuilder sb) {
			this.formatUnary(sb, "&", this.inner, null);
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitAnd(this, a);
		}

	}

	public abstract static class Not extends Unary {
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
		public final void format(StringBuilder sb) {
			this.formatUnary(sb, "!", this.inner, null);
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitNot(this, a);
		}
	}

	public abstract static class Pair extends Expression {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitPair(this, a);
		}

		public final UList<Expression> toList() {
			UList<Expression> l = ExpressionCommons.newList(4);
			Nez.Pair p = this;
			while (true) {
				l.add(p.getFirst());
				Expression e = p.getNext();
				if (!(e instanceof Psequence)) {
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

		@Override
		public final void format(StringBuilder sb) {
			for (int i = 0; i < this.size(); i++) {
				if (i > 0) {
					sb.append(delim());
				}
				this.get(i).format(sb);
			}
		}

		protected abstract java.lang.String delim();

	}

	public abstract static class Sequence extends Nez.List {

		@Override
		protected java.lang.String delim() {
			return " ";
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Sequence) {
				return this.equalsList((Nez.List) o);
			}
			return false;
		}

		// @Override
		// public Object visit(Expression.Visitor v, Object a) {
		// return v.visitSequence(this, a);
		// }

	}

	public abstract static class Choice extends List {

		@Override
		protected java.lang.String delim() {
			return " / ";
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Nez.Choice) {
				return this.equalsList((Nez.List) o);
			}
			return false;
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
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

	public abstract static class PreNew extends Terminal implements AST {
		public int shift = 0;

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.PreNew && this.shift == ((Nez.PreNew) o).shift);
		}

		@Override
		public final void format(StringBuilder sb) {
			sb.append("{");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitPreNew(this, a);
		}

	}

	public abstract static class New extends Terminal implements AST {
		public int shift = 0;

		@Override
		public final boolean equals(Object o) {
			return (o instanceof Nez.New && this.shift == ((Nez.New) o).shift);
		}

		@Override
		public final void format(StringBuilder sb) {
			sb.append("}");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitNew(this, a);
		}

	}

	public abstract static class LeftFold extends Terminal implements AST {
		public int shift = 0;
		public Symbol label;

		public LeftFold(int shift, Symbol label) {
			this.label = label;
			this.shift = shift;
		}

		public final Symbol getLabel() {
			return this.label;
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
		public final void format(StringBuilder sb) {
			sb.append("{$");
			if (label != null) {
				sb.append(label);
			}
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitLeftFold(this, a);
		}

	}

	public abstract static class Tag extends Terminal implements AST {
		public Symbol tag;

		public Tag(Symbol tag) {
			this.tag = tag;
		}

		public final java.lang.String getTagName() {
			return tag.getSymbol();
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Ttag) {
				return this.tag == ((Ttag) o).tag;
			}
			return false;
		}

		@Override
		public final void format(StringBuilder sb) {
			sb.append("#" + tag.getSymbol());
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitTag(this, a);
		}
	}

	public abstract static class Replace extends Terminal implements AST {
		public java.lang.String value;

		public Replace(java.lang.String value) {
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
		public final void format(StringBuilder sb) {
			sb.append(StringUtils.quoteString('`', this.value, '`'));
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitReplace(this, a);
		}
	}

	public abstract static class Action extends Terminal implements AST {
		Object value;
	}

	public abstract static class Link extends Unary implements AST {
		public Symbol label;

		public Link(Symbol label, Expression e) {
			super(e);
			this.label = label;
		}

		public final Symbol getLabel() {
			return this.label;
		}

		@Override
		public final boolean equals(Object o) {
			if (o instanceof Tlink && this.label == ((Tlink) o).label) {
				return this.get(0).equals(((Expression) o).get(0));
			}
			return false;
		}

		@Override
		public final void format(StringBuilder sb) {
			formatUnary(sb, (label != null) ? "$" + label + "(" : "$(", this.get(0), ")");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitLink(this, a);
		}
	}

	public abstract static class Detree extends Unary implements AST {
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
		public final void format(StringBuilder sb) {
			this.formatUnary(sb, "~", inner, null);
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitDetree(this, a);
		}

	}

	/* Symbol */
	private static Expression empty = ExpressionCommons.newEmpty(null);

	public static abstract class Operand extends Unary {
		public final Predicate op;

		public Operand(Predicate op, Expression e) {
			super(e);
			this.op = op;
		}

		@Override
		public void format(StringBuilder sb) {
			sb.append("<");
			sb.append(this.op);
			if (this.inner != empty) {
				sb.append(" ");
				sb.append(this.inner);
			}
			sb.append(">");
		}

		public void formatOperand(StringBuilder sb) {
		}
	}

	public abstract static class SymbolAction extends Operand {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolAction(this, a);
		}

	}

	public abstract static class SymbolPredicate extends Operand {
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
		public void format(StringBuilder sb) {
			sb.append("<");
			sb.append(this.op);
			sb.append(" ");
			sb.append(tableName);
			sb.append(">");
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolPredicate(this, a);
		}

	}

	public abstract static class SymbolMatch extends Operand {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolMatch(this, a);
		}

	}

	public abstract static class SymbolExists extends Operand implements AST {
		public final Symbol tableName;
		public final java.lang.String symbol;

		public SymbolExists(Symbol table, java.lang.String symbol) {
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

		private boolean equals(java.lang.String s, java.lang.String s2) {
			if (s != null && s2 != null) {
				return s.equals(s2);
			}
			return s == s2;
		}

		@Override
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitSymbolExists(this, a);
		}

	}

	public abstract static class BlockScope extends Operand implements AST {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitBlockScope(this, a);
		}

	}

	public abstract static class LocalScope extends Operand implements AST {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitLocalScope(this, a);
		}

		@Override
		public void formatOperand(StringBuilder sb) {
			sb.append(" ");
			sb.append(this.tableName);
		}

	}

	public static interface Conditional {

	}

	public abstract static class On extends Operand {
		public boolean predicate;
		public final java.lang.String flagName;

		public On(boolean predicate, java.lang.String c, Expression e) {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitOn(this, a);
		}
	}

	public abstract static class If extends Operand implements Conditional {
		public final boolean predicate;
		public final java.lang.String flagName;

		public If(boolean predicate, java.lang.String c) {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitIf(this, a);
		}
	}

	public abstract static class SetCount extends Operand {
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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitExtended(this, a);
		}
	}

	public abstract static class Count extends Operand {

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
		public Object visit(Expression.Visitor v, Object a) {
			return v.visitExtended(this, a);
		}
	}

}
