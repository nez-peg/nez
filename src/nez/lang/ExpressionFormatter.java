package nez.lang;

import nez.ast.Symbol;
import nez.lang.Nez.And;
import nez.lang.Nez.Any;
import nez.lang.Nez.BlockScope;
import nez.lang.Nez.Byte;
import nez.lang.Nez.ByteSet;
import nez.lang.Nez.Choice;
import nez.lang.Nez.Detree;
import nez.lang.Nez.Empty;
import nez.lang.Nez.Fail;
import nez.lang.Nez.Function;
import nez.lang.Nez.If;
import nez.lang.Nez.LeftFold;
import nez.lang.Nez.Link;
import nez.lang.Nez.LocalScope;
import nez.lang.Nez.New;
import nez.lang.Nez.Not;
import nez.lang.Nez.On;
import nez.lang.Nez.OneMore;
import nez.lang.Nez.Option;
import nez.lang.Nez.Pair;
import nez.lang.Nez.PreNew;
import nez.lang.Nez.Replace;
import nez.lang.Nez.Sequence;
import nez.lang.Nez.SymbolAction;
import nez.lang.Nez.SymbolExists;
import nez.lang.Nez.SymbolMatch;
import nez.lang.Nez.SymbolPredicate;
import nez.lang.Nez.Tag;
import nez.lang.Nez.ZeroMore;
import nez.lang.expr.NonTerminal;
import nez.util.StringUtils;

class ExpressionFormatter extends Expression.Visitor {
	public final static ExpressionFormatter defaultFormatter = new ExpressionFormatter();

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
	public Object visitEmpty(Empty e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append("''");
		return null;
	}

	@Override
	public Object visitFail(Fail e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append("!''");
		return null;
	}

	@Override
	public Object visitByte(Byte e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append(StringUtils.stringfyCharacter(e.byteChar));
		return null;
	}

	@Override
	public Object visitByteSet(ByteSet e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append(StringUtils.stringfyCharacterClass(e.byteMap));
		return null;
	}

	@Override
	public Object visitAny(Any e, Object a) {
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
	public Object visitPair(Pair e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatSequence(e, sb, " ");
		return null;
	}

	@Override
	public Object visitSequence(Sequence e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatSequence(e, sb, " ");
		return null;
	}

	@Override
	public Object visitChoice(Choice e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatSequence(e, sb, " / ");
		return null;
	}

	@Override
	public Object visitOption(Option e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatUnary(sb, null, e.get(0), "?");
		return null;
	}

	@Override
	public Object visitZeroMore(ZeroMore e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatUnary(sb, null, e.get(0), "*");
		return null;
	}

	@Override
	public Object visitOneMore(OneMore e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatUnary(sb, null, e.get(0), "+");
		return null;
	}

	@Override
	public Object visitAnd(And e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatUnary(sb, "&", e.get(0), null);
		return null;
	}

	@Override
	public Object visitNot(Not e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatUnary(sb, "!", e.get(0), null);
		return null;
	}

	@Override
	public Object visitPreNew(PreNew e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append("{");
		return null;
	}

	@Override
	public Object visitLeftFold(LeftFold e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append("{$");
		if (e.label != null) {
			sb.append(e.label);
		}
		return null;
	}

	@Override
	public Object visitLink(Link e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		formatUnary(sb, (e.label != null) ? "$" + e.label + "(" : "$(", e.get(0), ")");
		return null;
	}

	@Override
	public Object visitTag(Tag e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append("#" + e.tag);
		return null;
	}

	@Override
	public Object visitReplace(Replace e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append(StringUtils.quoteString('`', e.value, '`'));
		return null;
	}

	@Override
	public Object visitNew(New e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		sb.append("}");
		return null;
	}

	@Override
	public Object visitDetree(Detree e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatUnary(sb, "~", e.get(0), null);
		return null;
	}

	@Override
	public Object visitBlockScope(BlockScope e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, null, sb);
		return null;
	}

	@Override
	public Object visitLocalScope(LocalScope e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, e.tableName, sb);
		return null;
	}

	@Override
	public Object visitSymbolAction(SymbolAction e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, null, sb);
		return null;
	}

	@Override
	public Object visitSymbolPredicate(SymbolPredicate e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, e.tableName, sb);
		return null;
	}

	@Override
	public Object visitSymbolMatch(SymbolMatch e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, e.tableName, sb);
		return null;
	}

	@Override
	public Object visitSymbolExists(SymbolExists e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, symbol(e.tableName, e.symbol), sb); // FIXME
		return null;
	}

	@Override
	public Object visitIf(If e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, condition(e.predicate, e.flagName), sb);
		return null;
	}

	@Override
	public Object visitOn(On e, Object a) {
		StringBuilder sb = (StringBuilder) a;
		this.formatFunction(e, condition(e.predicate, e.flagName), sb);
		return null;
	}

	private void formatSequence(Expression e, StringBuilder sb, java.lang.String delim) {
		for (int i = 0; i < e.size(); i++) {
			if (i > 0) {
				sb.append(delim);
			}
			format(e.get(i), sb);
		}
	}

	private void formatUnary(StringBuilder sb, java.lang.String prefix, Expression inner, java.lang.String suffix) {
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

	private java.lang.String symbol(Symbol table, java.lang.String name) {
		return name == null ? table.toString() : table + " " + name;
	}

	private java.lang.String condition(boolean predicate, java.lang.String name) {
		return predicate ? name : "!" + name;
	}

}
