package nez.lang;

import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;

import nez.ast.Source;
import nez.ast.SourceLocation;
import nez.ast.Symbol;
import nez.lang.Nez.Function;
import nez.util.StringUtils;
import nez.util.UList;

public abstract class Expression extends AbstractList<Expression> implements SourceLocation {

	public abstract Object visit(Expression.Visitor v, Object a);

	// protected Expression() {
	// this(null);
	// }
	//
	// protected Expression(SourceLocation s) {
	// this.s = s;
	// }

	private SourceLocation s = null;

	public final void setSourceLocation(SourceLocation s) {
		if (s instanceof Expression) {
			s = ((Expression) s).getSourceLocation();
		}
		this.s = s;
	}

	public final SourceLocation getSourceLocation() {
		return this.s;
	}

	@Override
	public Source getSource() {
		if (this.s != null) {
			return this.s.getSource();
		}
		return null;
	}

	@Override
	public long getSourcePosition() {
		if (this.s != null) {
			this.s.getSourcePosition();
		}
		return 0L;
	}

	@Override
	public int getLineNum() {
		if (this.s != null) {
			this.s.getLineNum();
		}
		return 0;
	}

	@Override
	public int getColumn() {
		if (this.s != null) {
			this.s.getColumn();
		}
		return 0;
	}

	@Override
	public String formatSourceMessage(String type, String msg) {
		if (this.s != null) {
			return this.s.formatSourceMessage(type, msg);
		}
		return msg;
	}

	// test

	public static final boolean isByteConsumed(Expression e) {
		return (e instanceof Nez.Byte || e instanceof Nez.ByteSet || e instanceof Nez.Any);
	}

	public static final boolean isPositionIndependentOperation(Expression e) {
		return (e instanceof Nez.Tag || e instanceof Nez.Replace);
	}

	// convinient interface

	public final Expression newEmpty() {
		return Expressions.newEmpty(this.getSourceLocation());
	}

	public final Expression newFailure() {
		return Expressions.newFailure(this.getSourceLocation());
	}

	public final Expression newByteSet(boolean isBinary, boolean[] byteMap) {
		return Expressions.newByteSet(this.getSourceLocation(), byteMap);
	}

	public final Expression newPair(Expression e, Expression e2) {
		return Expressions.newPair(this.getSourceLocation(), e, e2);
	}

	public final Expression newPair(List<Expression> l) {
		return Expressions.newPair(this.getSourceLocation(), l);
	}

	public final Expression newChoice(Expression e, Expression e2) {
		return Expressions.newChoice(this.getSourceLocation(), e, e2);
	}

	public final Expression newChoice(UList<Expression> l) {
		return Expressions.newChoice(this.getSourceLocation(), l);
	}

	/* static class */

	public static interface Conditional {

	}

	public static interface Contextual {

	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		defaultFormatter.format(this, sb);
		return sb.toString();
	}

	public static abstract class Visitor {

		protected HashMap<String, Object> visited = null;

		public final Object lookup(String uname) {
			if (visited != null) {
				return visited.get(uname);
			}
			return null;
		}

		public final void memo(String uname, Object o) {
			if (visited == null) {
				visited = new HashMap<>();
			}
			visited.put(uname, o);
		}

		public final boolean isVisited(String uname) {
			if (visited != null) {
				return visited.containsKey(uname);
			}
			return false;
		}

		public final void visited(String uname) {
			memo(uname, uname);
		}

		public final void clear() {
			if (visited != null) {
				visited.clear();
			}
		}

		public abstract Object visitNonTerminal(NonTerminal e, Object a);

		public abstract Object visitEmpty(Nez.Empty e, Object a);

		public abstract Object visitFail(Nez.Fail e, Object a);

		public abstract Object visitByte(Nez.Byte e, Object a);

		public abstract Object visitByteSet(Nez.ByteSet e, Object a);

		public abstract Object visitAny(Nez.Any e, Object a);

		public abstract Object visitMultiByte(Nez.MultiByte e, Object a);

		public abstract Object visitPair(Nez.Pair e, Object a);

		public abstract Object visitSequence(Nez.Sequence e, Object a);

		public abstract Object visitChoice(Nez.Choice e, Object a);

		public abstract Object visitOption(Nez.Option e, Object a);

		public abstract Object visitZeroMore(Nez.ZeroMore e, Object a);

		public abstract Object visitOneMore(Nez.OneMore e, Object a);

		public abstract Object visitAnd(Nez.And e, Object a);

		public abstract Object visitNot(Nez.Not e, Object a);

		public abstract Object visitBeginTree(Nez.BeginTree e, Object a);

		public abstract Object visitFoldTree(Nez.FoldTree e, Object a);

		public abstract Object visitLink(Nez.LinkTree e, Object a);

		public abstract Object visitTag(Nez.Tag e, Object a);

		public abstract Object visitReplace(Nez.Replace e, Object a);

		public abstract Object visitEndTree(Nez.EndTree e, Object a);

		public abstract Object visitDetree(Nez.Detree e, Object a);

		public abstract Object visitBlockScope(Nez.BlockScope e, Object a);

		public abstract Object visitLocalScope(Nez.LocalScope e, Object a);

		public abstract Object visitSymbolAction(Nez.SymbolAction e, Object a);

		public abstract Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a);

		public abstract Object visitSymbolMatch(Nez.SymbolMatch e, Object a);

		public abstract Object visitSymbolExists(Nez.SymbolExists e, Object a);

		public abstract Object visitIf(Nez.IfCondition e, Object a);

		public abstract Object visitOn(Nez.OnCondition e, Object a);

		// public abstract Object visitSetCount(Nez.SetCount e, Object a);
		//
		// public abstract Object visitCount(Nez.Count e, Object a);

		public Object visitExtended(Expression e, Object a) {
			return a;
		}

	}

	private final static ExpressionFormatter defaultFormatter = new ExpressionFormatter();

	static class ExpressionFormatter extends Expression.Visitor {

		public void format(Expression e, StringBuilder sb) {
			e.visit(this, sb);
		}

		@Override
		public Object visitNonTerminal(NonTerminal e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append(e.getLocalName());
			return null;
		}

		@Override
		public Object visitEmpty(Nez.Empty e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append("''");
			return null;
		}

		@Override
		public Object visitFail(Nez.Fail e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append("!''");
			return null;
		}

		@Override
		public Object visitByte(Nez.Byte e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append(StringUtils.stringfyCharacter(e.byteChar));
			return null;
		}

		@Override
		public Object visitByteSet(Nez.ByteSet e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append(StringUtils.stringfyCharacterClass(e.byteMap));
			return null;
		}

		@Override
		public Object visitAny(Nez.Any e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append(".");
			return null;
		}

		@Override
		public Object visitMultiByte(Nez.MultiByte e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append("'");
			for (int i = 0; i < e.byteSeq.length; i++) {
				StringUtils.appendByteChar(sb, e.byteSeq[i] & 0xff, "\'");
			}
			sb.append("'");
			return null;
		}

		@Override
		public Object visitPair(Nez.Pair e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatSequence(e, sb, " ");
			return null;
		}

		@Override
		public Object visitSequence(Nez.Sequence e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatSequence(e, sb, " ");
			return null;
		}

		@Override
		public Object visitChoice(Nez.Choice e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatSequence(e, sb, " / ");
			return null;
		}

		@Override
		public Object visitOption(Nez.Option e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatUnary(sb, null, e.get(0), "?");
			return null;
		}

		@Override
		public Object visitZeroMore(Nez.ZeroMore e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatUnary(sb, null, e.get(0), "*");
			return null;
		}

		@Override
		public Object visitOneMore(Nez.OneMore e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatUnary(sb, null, e.get(0), "+");
			return null;
		}

		@Override
		public Object visitAnd(Nez.And e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatUnary(sb, "&", e.get(0), null);
			return null;
		}

		@Override
		public Object visitNot(Nez.Not e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatUnary(sb, "!", e.get(0), null);
			return null;
		}

		@Override
		public Object visitBeginTree(Nez.BeginTree e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append("{");
			return null;
		}

		@Override
		public Object visitFoldTree(Nez.FoldTree e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append("{$");
			if (e.label != null) {
				sb.append(e.label);
			}
			return null;
		}

		@Override
		public Object visitLink(Nez.LinkTree e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			formatUnary(sb, (e.label != null) ? "$" + e.label + "(" : "$(", e.get(0), ")");
			return null;
		}

		@Override
		public Object visitTag(Nez.Tag e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append("#" + e.tag);
			return null;
		}

		@Override
		public Object visitReplace(Nez.Replace e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append(StringUtils.quoteString('`', e.value, '`'));
			return null;
		}

		@Override
		public Object visitEndTree(Nez.EndTree e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			sb.append("}");
			return null;
		}

		@Override
		public Object visitDetree(Nez.Detree e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatUnary(sb, "~", e.get(0), null);
			return null;
		}

		@Override
		public Object visitBlockScope(Nez.BlockScope e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, null, sb);
			return null;
		}

		@Override
		public Object visitLocalScope(Nez.LocalScope e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, e.tableName, sb);
			return null;
		}

		@Override
		public Object visitSymbolAction(Nez.SymbolAction e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, null, sb);
			return null;
		}

		@Override
		public Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, e.tableName, sb);
			return null;
		}

		@Override
		public Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, e.tableName, sb);
			return null;
		}

		@Override
		public Object visitSymbolExists(Nez.SymbolExists e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, symbol(e.tableName, e.symbol), sb); // FIXME
			return null;
		}

		@Override
		public Object visitIf(Nez.IfCondition e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, condition(e.predicate, e.flagName), sb);
			return null;
		}

		@Override
		public Object visitOn(Nez.OnCondition e, Object a) {
			StringBuilder sb = (StringBuilder) a;
			this.formatFunction(e, condition(e.predicate, e.flagName), sb);
			return null;
		}

		private void formatSequence(Expression e, StringBuilder sb, String delim) {
			for (int i = 0; i < e.size(); i++) {
				if (i > 0) {
					sb.append(delim);
				}
				format(e.get(i), sb);
			}
		}

		private void formatUnary(StringBuilder sb, String prefix, Expression inner, String suffix) {
			if (prefix != null) {
				sb.append(prefix);
			}
			if (inner instanceof NonTerminal || inner instanceof Nez.Terminal) {
				format(inner, sb);
			} else {
				sb.append("(");
				format(inner, sb);
				sb.append(")");
			}
			if (suffix != null) {
				sb.append(suffix);
			}
		}

		private void formatFunction(Function e, Object argument, StringBuilder sb) {
			sb.append("<");
			sb.append(e.op);
			if (argument != null) {
				sb.append(" ");
				sb.append(argument);
			}
			if (e.hasInnerExpression()) {
				sb.append(" ");
				sb.append(e.get(0));
			}
			sb.append(">");
		}

		private String symbol(Symbol table, String name) {
			return name == null ? table.toString() : table + " " + name;
		}

		private String condition(boolean predicate, String name) {
			return predicate ? name : "!" + name;
		}

	}

}
