package nez.lang;

import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NezCompiler;

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
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeReplace(this);
	}
	@Override
	public Instruction encode(NezCompiler bc, Instruction next, Instruction failjump) {
		return bc.encodeReplace(this, next);
	}
}