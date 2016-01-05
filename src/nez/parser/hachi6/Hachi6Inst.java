package nez.parser.hachi6;

import nez.parser.NezInst;
import nez.parser.TerminationException;

public abstract class Hachi6Inst implements NezInst {
	public int id;
	public Hachi6Inst next;

	public Hachi6Inst(Hachi6Inst next) {
		this.id = -1;
		this.next = next;
	}

	public abstract void visit(Hachi6Visitor v);

	public abstract Hachi6Inst exec(Hachi6Machine sc) throws TerminationException;

	public final boolean isIncrementedNext() {
		if (this.next != null) {
			return this.next.id == this.id + 1;
		}
		return true; // RET or instructions that are unnecessary to go next
	}

	Hachi6Inst branch() {
		return null;
	}

	public final String getName() {
		return this.getClass().getSimpleName();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getName());
		int size = Hachi6.opSize(this.getName());
		for (int i = 0; i < size; i++) {
			sb.append(" ");
			Object v = Hachi6.opValue(this, i);
			if (v instanceof Hachi6Inst) {
				sb.append("L" + ((Hachi6Inst) v).id);
			} else {
				sb.append(v);
			}
		}
		return sb.toString();
	}
}

abstract class Hachi6Branch extends Hachi6Inst {
	public Hachi6Inst jump;

	public Hachi6Branch(Hachi6Inst jump, Hachi6Inst next) {
		super(next);
		this.jump = jump;
	}
}

abstract class Hachi6BranchTable extends Hachi6Inst {
	public final Hachi6Inst[] jumpTable;

	public Hachi6BranchTable(Hachi6Inst[] jumpTable, Hachi6Inst next) {
		super(next);
		this.jumpTable = jumpTable;
	}
}
