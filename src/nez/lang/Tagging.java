package nez.lang;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

public class Tagging extends ASTOperation {
	public Tag tag;
	Tagging(SourcePosition s, Tag tag) {
		super(s);
		this.tag = tag;
	}
	Tagging(SourcePosition s, String name) {
		this(s, Tag.tag(name));
	}
	public final String getTagName() {
		return tag.getName();
	}
	@Override
	public String getPredicate() {
		return "tag " + StringUtils.quoteString('"', tag.getName(), '"');
	}
	@Override
	public String key() {
		return "#" + this.tag.getName();
	}
	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeTagging(this);
	}
//	@Override
//	public boolean match(SourceContext context) {
//		context.left.setTag(this.tag);
//		return true;
//	}
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return bc.encodeTagging(this, next);
	}
}