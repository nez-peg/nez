package nez.konoha;


import nez.ast.AbstractTree;
import nez.ast.Source;
import nez.ast.Tag;
import nez.string.StringTransducer;

public class KonohaTree extends AbstractTree<KonohaTree> {
	KonohaType     typed = null;
	KonohaTypeRule matched = null;
	
	public KonohaTree(Tag tag, Source source, long pos, int len, int size, Object value) {
		super(tag, source, pos, len, size > 0 ? new KonohaTree[size] : null, value);
	}
	
	@Override
	protected KonohaTree dupImpl() {
		return new KonohaTree(this.getTag(), 
			this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		this.stringfy("", sb);
		return sb.toString();
	}

	public void stringfy(String indent, StringBuilder sb) {
		super.stringfy(indent, null, sb);
		if(typed != null) {
			sb.append(" :");
			sb.append(typed.toString());
		}
	}
	
	public final static String keyTag(String name) {
		return "#" + name;
	}

	public final static String keyTag(Tag t) {
		return keyTag(t.getName());
	}
	
	public final String getRuleName() {
		if(Konoha.Expression == this.getTag()) {
		}
		return keyTag(this.getTag());
	}

	public StringTransducer getStringTransducer() {
		return matched != null ? matched.st : null;
	}
	
}


