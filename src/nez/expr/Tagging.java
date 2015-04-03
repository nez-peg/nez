package nez.expr;

import nez.ast.SourcePosition;
import nez.ast.Tag;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.StringUtils;

public class Tagging extends ASTOperation {
	public Tag tag;
	Tagging(SourcePosition s, Tag tag) {
		super(s);
		this.tag = tag;
	}
	Tagging(SourcePosition s, String name) {
		this(s, Tag.tag(name));
	}
	@Override
	public String getPredicate() {
		return "tag " + StringUtils.quoteString('"', tag.getName(), '"');
	}
	@Override
	public String getInterningKey() {
		return "#" + this.tag.getName();
	}
	@Override
	public Expression checkTypestate(GrammarChecker checker, Typestate c) {
		return this.checkTypestate(checker, c, "#" + tag.getName());
	}
//	@Override
//	public boolean match(SourceContext context) {
//		context.left.setTag(this.tag);
//		return true;
//	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeTagging(this, next);
	}
}