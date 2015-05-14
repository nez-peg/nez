package nez.lang;

import nez.ast.SourcePosition;
import nez.runtime.Instruction;
import nez.runtime.RuntimeCompiler;
import nez.util.StringUtils;

public class Replace extends ASTOperation {
	public String value;
	Replace(SourcePosition s, String value) {
		super(s);
		this.value = value;
	}
	@Override
	public String getPredicate() {
		return "replace " + StringUtils.quoteString('"', value, '"');
	}
	@Override
	public String key() {
		return "`" + this.value;
	}
	@Override
	public boolean isConsumed(Stacker stacker) {
		return false;
	}
	@Override
	public Expression reshape(Manipulator m) {
		return m.reshapeReplace(this);
	}
	@Override
	public Instruction encode(RuntimeCompiler bc, Instruction next) {
		return bc.encodeReplace(this, next);
	}
}