package nez.parser;

import nez.lang.Expression;

abstract class AbstractSLRInstraction extends Instruction {
	public AbstractSLRInstraction(byte opcode, Expression e, Instruction next) {
		super(opcode, e, next);
	}
}

class ILRCall extends AbstractSLRInstraction {
	ParseFunc f;
	String name;
	public Instruction jump = null;

	ILRCall(ParseFunc f, String name, Instruction next) {
		super(InstructionSet.LRCall, null, next);
		this.f = f;
		this.name = name;
	}

	void sync() {
		if (this.jump == null) {
			this.jump = labeling(this.next);
			this.next = labeling(f.compiled);
		}
		this.f = null;
	}

	public final String getNonTerminalName() {
		return this.name;
	}

	@Override
	protected String getOperand() {
		return label(jump);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// TODO encode argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		return null; // TODO implement
	}
}

class ILRGrow extends AbstractSLRInstraction {
	ParseFunc f;
	String name;
	public Instruction jump = null;

	ILRGrow(ParseFunc f, String name, Instruction next) {
		super(InstructionSet.LRGrow, null, next);
		this.f = f;
		this.name = name;
	}

	void sync() {
		if (this.jump == null) {
			this.jump = labeling(this.next);
			this.next = labeling(f.compiled);
		}
		this.f = null;
	}

	public final String getNonTerminalName() {
		return this.name;
	}

	@Override
	protected String getOperand() {
		return label(jump);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// TODO encode argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		return null; // TODO implementation
	}
}