package nez.vm;

import nez.util.ConsoleUtils;

public abstract class NezDebugOperator {
	DebugOperation type;
	String code;

	public NezDebugOperator(DebugOperation type) {
		this.type = type;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public abstract boolean exec(NezDebugger d) throws TerminationException;

	public String toString() {
		return this.type.toString();
	}
}

class Print extends NezDebugOperator {
	static int printProduction = 0;
	static int printContext = 1;
	int type = 0;

	public Print() {
		super(DebugOperation.Print);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public boolean exec(NezDebugger d) {
		return d.exec(this);
	}
}

class Break extends NezDebugOperator {

	public Break() {
		super(DebugOperation.Break);
	}

	@Override
	public boolean exec(NezDebugger d) {
		return d.exec(this);
	}
}

class StepOver extends NezDebugOperator {

	public StepOver() {
		super(DebugOperation.StepOver);
	}

	@Override
	public boolean exec(NezDebugger d) throws TerminationException {
		return d.exec(this);
	}
}

class StepIn extends NezDebugOperator {

	public StepIn() {
		super(DebugOperation.StepIn);
	}

	@Override
	public boolean exec(NezDebugger d) throws TerminationException {
		return d.exec(this);
	}
}

class StepOut extends NezDebugOperator {

	public StepOut() {
		super(DebugOperation.StepOut);
	}

	@Override
	public boolean exec(NezDebugger d) throws TerminationException {
		return d.exec(this);
	}
}

class Continue extends NezDebugOperator {

	public Continue() {
		super(DebugOperation.Continue);
	}

	@Override
	public boolean exec(NezDebugger d) throws TerminationException {
		return d.exec(this);
	}
}

class Run extends NezDebugOperator {

	public Run() {
		super(DebugOperation.Run);
	}

	@Override
	public boolean exec(NezDebugger d) throws TerminationException {
		return d.exec(this);
	}
}

class Exit extends NezDebugOperator {

	public Exit() {
		super(DebugOperation.Exit);
	}

	@Override
	public boolean exec(NezDebugger d) {
		return d.exec(this);
	}
}

enum DebugOperation {
	Print,
	Break,
	StepOver,
	StepIn,
	StepOut,
	Continue,
	Run,
	Exit
}