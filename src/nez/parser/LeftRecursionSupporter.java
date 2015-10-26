package nez.parser;

import java.util.HashMap;

import nez.ast.ASTMachine;
import nez.ast.Symbol;
import nez.lang.Expression;

abstract class AbstractSLRInstraction extends Instruction {
	protected static HashMap<String, HashMap<Long, MapEntry>> growing = new HashMap<String, HashMap<Long, MapEntry>>();

	public AbstractSLRInstraction(byte opcode, Expression e, Instruction next) {
		super(opcode, e, next);
	}

	protected final void log(int type, long pos, Symbol label, Object value, ASTMachine astMachine) {
		switch (type) {
		case 1:
			astMachine.logCapture(pos);
			break;
		case 2:
			astMachine.logTag((Symbol) value);
			break;
		case 3:
			astMachine.logReplace(value);
			break;
		case 4:
			astMachine.logLeftFold(pos, label);
			break;
		case 5:
			astMachine.logPop(label);
			break;
		case 6:
			astMachine.logPush();
			break;
		case 7:
			astMachine.logLink(label, value);
			break;
		case 8:
			astMachine.logNew(pos, value);
			break;
		}
	}
}

class ILRCall extends AbstractSLRInstraction {
	ParseFunc f;
	String name;
	public Instruction jump = null;

	private long pos;
	private Object astlog;

	ILRCall(ParseFunc f, String name, Instruction next) {
		super(InstructionSet.LRCall, null, next);
		this.f = f;
		this.name = name;

		if (!growing.containsKey(this.name)) {
			growing.put(this.name, new HashMap<Long, MapEntry>());
		}
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
		ASTMachine astMachine = sc.getAstMachine();
		this.pos = sc.getPosition();
		this.astlog = astMachine.saveTransactionPoint();
		if (growing.get(this.name).containsKey(this.pos)) {
			sc.setPosition(growing.get(this.name).get(this.pos).getPos());
			if (growing.get(this.name).get(this.pos).getAnsType()) {
				growing.get(this.name).get(this.pos).setLRDetected(true);
				return sc.fail();
			}
			growing.get(this.name).get(this.pos).revertLogData();
			while (true) {
				Object logData[] = growing.get(this.name).get(this.pos).pollLogData();
				if (logData == null) {
					break;
				}
				super.log((int) logData[0], (long) logData[3], (Symbol) logData[1], logData[2], astMachine);
			}
			return ((ILRGrow) this.jump).jump;
		}
		growing.get(this.name).put(this.pos, new MapEntry(false, this.pos));
		StackData s0 = sc.newUnusedStack();
		s0.value = this.pos;
		s0.ref = this.astlog;
		StackData s1 = sc.newUnusedStack();
		s1.ref = this.jump;
		return this.next;
	}
}

class ILRGrow extends AbstractSLRInstraction {
	ParseFunc f;
	String name;
	public Instruction jump = null;

	private Long pos;
	private Object astlog;
	private boolean isGrow = false;

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
		ASTMachine astMachine = sc.getAstMachine();
		StackData s = sc.popStack();
		this.pos = s.value;
		this.astlog = s.ref;
		if (this.isGrow) {
			if (sc.getPosition() <= growing.get(this.name).get(this.pos).getPos()) {
				sc.setPosition(growing.get(this.name).get(this.pos).getPos());
				astMachine.rollTransactionPoint(this.astlog);
				growing.get(this.name).get(this.pos).revertLogData();
				while (true) {
					Object logData[] = growing.get(this.name).get(this.pos).pollLogData();
					if (logData == null) {
						break;
					}
					super.log((int) logData[0], (long) logData[3], (Symbol) logData[1], logData[2], astMachine);
				}
				this.isGrow = false;
				return this.jump;
			}
			growing.get(this.name).get(this.pos).setPos(sc.getPosition());
			growing.get(this.name).get(this.pos).clearLogData();
			Object next[] = astMachine.getNextLogData(this.astlog);
			while (next != null) {
				growing.get(this.name).get(this.pos).addLogData(next);
				next = astMachine.getNextLogData(next[4]);
			}
			sc.setPosition(this.pos);
			astMachine.rollTransactionPoint(this.astlog);
			StackData s0 = sc.newUnusedStack();
			s0.value = this.pos;
			s0.ref = this.astlog;
			StackData s1 = sc.newUnusedStack();
			s1.ref = this;
			return this.next;
		}
		growing.get(this.name).get(this.pos).setPos(sc.getPosition());
		growing.get(this.name).get(this.pos).clearLogData();
		Object next[] = astMachine.getNextLogData(this.astlog);
		while (next != null) {
			growing.get(this.name).get(this.pos).addLogData(next);
			next = astMachine.getNextLogData(next[4]);
		}
		if (growing.get(this.name).get(this.pos).getLRDetected() && this.pos < sc.getPosition()) {
			sc.setPosition(this.pos);
			astMachine.rollTransactionPoint(this.astlog);
			StackData s0 = sc.newUnusedStack();
			s0.value = this.pos;
			s0.ref = this.astlog;
			StackData s1 = sc.newUnusedStack();
			s1.ref = this;
			this.isGrow = true;
			return this.next;
		}
		return this.jump;
	}
}

class MapEntry {
	private boolean ansType; // true -> LR, false -> ASTLogData
	private boolean lrDetected;
	private ASTLogData firstData;
	private ASTLogData currentData;
	private ASTLogData lastData;
	private long pos;

	public MapEntry(boolean lrDetected, long pos) {
		this.ansType = true;
		this.lrDetected = lrDetected;
		this.firstData = new ASTLogData();
		this.currentData = this.firstData;
		this.lastData = this.firstData;
		this.pos = pos;
	}

	public boolean getAnsType() {
		return this.ansType;
	}

	public boolean getLRDetected() {
		return this.lrDetected;
	}

	public void setLRDetected(boolean lrDetected) {
		this.ansType = true;
		this.lrDetected = lrDetected;
	}

	public void clearLogData() {
		this.ansType = false;
		this.currentData = this.firstData;
		this.lastData = this.firstData;
	}

	public void revertLogData() {
		this.currentData = this.firstData;
	}

	public Object[] pollLogData() {
		if (this.currentData != this.lastData) {
			Object astLogData[] = new Object[4];
			astLogData[0] = this.currentData.type;
			astLogData[1] = this.currentData.label;
			astLogData[2] = this.currentData.ref;
			astLogData[3] = this.currentData.value;
			this.currentData = this.currentData.next;
			return astLogData;
		}
		return null;
	}

	public void addLogData(Object[] logData) {
		this.lastData.type = (int) logData[0];
		this.lastData.label = (Symbol) logData[1];
		this.lastData.ref = logData[2];
		this.lastData.value = (long) logData[3];
		this.lastData.next = new ASTLogData();
		this.lastData = this.lastData.next;
	}

	public long getPos() {
		return this.pos;
	}

	public void setPos(long pos) {
		this.pos = pos;
	}

	class ASTLogData {
		int type;
		Symbol label;
		Object ref;
		long value;
		public ASTLogData next;
	}
}