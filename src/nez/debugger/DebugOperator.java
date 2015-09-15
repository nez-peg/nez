package nez.debugger;

public abstract class DebugOperator {
	DebugOperation type;
	String code;

	public DebugOperator(DebugOperation type) {
		this.type = type;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public abstract boolean exec(NezDebugger d) throws MachineExitException;

	@Override
	public String toString() {
		return this.type.toString();
	}
}

class Print extends DebugOperator {
	static int printProduction = 0;
	static int printPos = 1;
	static int printNode = 2;
	static int printCallers = 3;
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
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class BackTrace extends DebugOperator {
	static int callStackTrace = 0;
	static int longestTrace = 1;
	int type = 0;

	public BackTrace() {
		super(DebugOperation.BackTrace);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}

}

class Break extends DebugOperator {

	public Break() {
		super(DebugOperation.Break);
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class StepOver extends DebugOperator {

	public StepOver() {
		super(DebugOperation.StepOver);
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class StepIn extends DebugOperator {

	public StepIn() {
		super(DebugOperation.StepIn);
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class StepOut extends DebugOperator {

	public StepOut() {
		super(DebugOperation.StepOut);
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class Continue extends DebugOperator {

	public Continue() {
		super(DebugOperation.Continue);
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class Run extends DebugOperator {

	public Run() {
		super(DebugOperation.Run);
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class Exit extends DebugOperator {

	public Exit() {
		super(DebugOperation.Exit);
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}
}

class StartRule extends DebugOperator {
	String name;

	public StartRule(String name) {
		super(DebugOperation.StartRule);
		this.name = name;
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return false;
	}

}

class StartPosition extends DebugOperator {
	long pos;

	public StartPosition(long pos) {
		super(DebugOperation.StartPosition);
		this.pos = pos;
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}

}

class Consume extends DebugOperator {
	long pos;

	public Consume(long pos) {
		super(DebugOperation.Consume);
		this.pos = pos;
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}

}

class Goto extends DebugOperator {
	String name;

	public Goto(String name) {
		super(DebugOperation.Goto);
		this.name = name;
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}

}

class Reachable extends DebugOperator {
	String name;
	String path;

	public Reachable(String name, String path) {
		super(DebugOperation.Reachable);
		this.name = name;
		this.path = path;
	}

	@Override
	public boolean exec(NezDebugger d) throws MachineExitException {
		return d.exec(this);
	}

}

enum DebugOperation {
	Print, Break, BackTrace, StepOver, StepIn, StepOut, Continue, Run, Exit, StartRule, StartPosition, Consume, Goto, Reachable
}