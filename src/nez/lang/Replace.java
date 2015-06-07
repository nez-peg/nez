package nez.lang;

import nez.ast.SourcePosition;
import nez.util.StringUtils;
import nez.vm.Instruction;
import nez.vm.NezEncoder;

public class Replace extends ASTOperation {
	public String value;
	Replace(SourcePosition s, String value) {
		super(s);
		this.value = value;
	}
	@Override
	public final boolean equalsExpression(Expression o) {
		if(o instanceof Replace) {
			return this.value.equals(((Replace)o).value);
		}
		return false;
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
	protected final void format(StringBuilder sb) {
		sb.append(StringUtils.quoteString('`', this.value, '`'));
	}

	@Override
	public boolean isConsumed() {
		return false;
	}
	@Override
	public Expression reshape(GrammarReshaper m) {
		return m.reshapeReplace(this);
	}
	@Override
	public Instruction encode(NezEncoder bc, Instruction next, Instruction failjump) {
		return bc.encodeReplace(this, next);
	}
}