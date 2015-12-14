package nez.parser.moz;

import nez.lang.Expression;
import nez.parser.ByteCoder;
import nez.parser.NezInst;
import nez.parser.TerminationException;

public abstract class MozInst implements NezInst {
	public final byte opcode;
	protected Expression e;
	public MozInst next;
	public int id;
	public boolean label = false;

	public MozInst(byte opcode, Expression e, MozInst next) {
		this.opcode = opcode;
		this.e = e;
		this.id = -1;
		this.next = next;
	}

	public final boolean isIncrementedNext() {
		if (this.next != null) {
			return this.next.id == this.id + 1;
		}
		return true; // RET or instructions that are unnecessary to go next
	}

	MozInst branch() {
		return null;
	}

	public final void encode(ByteCoder c) {
		if (isIncrementedNext()) {
			c.encodeOpcode(this.opcode);
			this.encodeImpl(c);
		} else {
			c.encodeOpcode((byte) (this.opcode | 128)); // opcode | 10000000
			this.encodeImpl(c);
			c.encodeJump(this.next);
		}
	}

	protected abstract void encodeImpl(ByteCoder c);

	public abstract MozInst exec(MozMachine sc) throws TerminationException;

	protected static MozInst labeling(MozInst inst) {
		if (inst != null) {
			inst.label = true;
		}
		return inst;
	}

	protected static String label(MozInst inst) {
		return "L" + inst.id;
	}

	public final String getName() {
		return MozSet.stringfy(this.opcode);
	}

	protected String getOperand() {
		return null;
	}

	public Expression getExpression() {
		return this.e;
	}

	protected void stringfy(StringBuilder sb) {
		sb.append(this.getName());
		String op = getOperand();
		if (op != null) {
			sb.append(" ");
			sb.append(op);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		stringfy(sb);
		return sb.toString();
	}

	public abstract void visit(MozVisitor v);
}
