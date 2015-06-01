package nez.lang;

import java.util.TreeMap;

import nez.ast.SourcePosition;

public abstract class Unary extends Expression {
	Expression inner;
	Unary(SourcePosition s, Expression inner) {
		super(s);
		this.inner = inner;
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
	protected final void formatUnary(StringBuilder sb, String prefix, Expression inner, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(inner instanceof NonTerminal || inner instanceof ByteChar || inner instanceof ByteMap) {
			inner.format(sb);
		}
		else {
			sb.append("(");
			inner.format(sb);
			sb.append(")");
		}
		if(suffix != null) {
			sb.append(suffix);
		}
	}
	protected final void formatUnary(StringBuilder sb, String prefix, Expression inner) {
		this.formatUnary(sb, prefix, inner, null);
	}
	protected final void formatUnary(StringBuilder sb, Expression inner, String suffix) {
		this.formatUnary(sb, null, inner, suffix);
	}

}
