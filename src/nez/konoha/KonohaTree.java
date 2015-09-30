package nez.konoha;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.string.StringTransducer;

public class KonohaTree extends Tree<KonohaTree> {
	KonohaType typed = null;
	KonohaTypeRule matched = null;

	public KonohaTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new KonohaTree[size] : null, value);
	}

	@Override
	public KonohaTree newInstance(Symbol tag, int size, Object value) {
		return new KonohaTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
	}

	@Override
	protected KonohaTree dupImpl() {
		return new KonohaTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", sb);
		return sb.toString();
	}

	public void stringfy(String indent, StringBuilder sb) {
		super.stringfy(indent, null, sb);
		if (typed != null) {
			sb.append(" :");
			sb.append(typed.toString());
		}
	}

	public final static String keyTag(String name) {
		return "#" + name;
	}

	public final static String keyTag(Symbol t) {
		return keyTag(t.getSymbol());
	}

	public final String getRuleName() {
		if (Konoha.Expression == this.getTag()) {
		}
		return keyTag(this.getTag());
	}

	public StringTransducer getStringTransducer() {
		return matched != null ? matched.st : null;
	}

	@Override
	protected KonohaTree newInstance(Symbol tag, Source source, long pos, int len, int objectsize, Object value) {
		return new KonohaTree(tag, source, pos, len, objectsize, value);
	}

	@Override
	protected void link(int n, Symbol label, Object child) {
		this.link(n, label, child);
	}

}
