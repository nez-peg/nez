package nez.vm;

import java.util.Arrays;
import java.util.HashMap;

import nez.ast.Tag;
import nez.lang.Block;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.MultiChar;
import nez.lang.DefIndent;
import nez.lang.DefSymbol;
import nez.lang.ExistsSymbol;
import nez.lang.Expression;
import nez.lang.IsIndent;
import nez.lang.IsSymbol;
import nez.lang.Link;
import nez.lang.LocalTable;
import nez.lang.New;
import nez.lang.NezTag;
import nez.lang.PossibleAcceptance;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Tagging;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.vm.RuntimeContext.StackData;

public abstract class Instruction {
	public final byte opcode;
	protected Expression  e;
	public Instruction next;
	public int id;
	public boolean label = false;
	
	public Instruction(byte opcode, Expression e, Instruction next) {
		this.opcode = opcode;
		this.e = e;
		this.id = -1;
		this.next = next;
	}
	
	public final boolean isIncrementedNext() {
		if(this.next != null) {
			return this.next.id == this.id + 1;
		}
		return true;  // RET or instructions that are unnecessary to go next
	}

	Instruction branch() {
		return null;
	}

	public final void encode(ByteCoder c) {
		if(isIncrementedNext()) {
			c.encodeOpcode(this.opcode);
			this.encodeA(c);
		}
		else {
			c.encodeOpcode((byte)(this.opcode | 128));   // opcode | 10000000
			this.encodeA(c);
			c.encodeJumpAddr(this.next);
		}
	}
	
	abstract void encodeA(ByteCoder c);
	abstract Instruction exec(RuntimeContext sc) throws TerminationException;
		

	protected static Instruction labeling(Instruction inst) {
		if(inst != null) {
			inst.label = true;
		}
		return inst;
	}
	
	protected static String label(Instruction inst) {
		return "L"+inst.id;
	}

	public final String getName() {
		return InstructionSet.stringfy(this.opcode);
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
		if(op != null) {
			sb.append(" ");
			sb.append(op);
		}
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		stringfy(sb);
		return sb.toString();
	}
		
//	public void dump(HashMap<Integer, Boolean> visited) {
//		if(visited.containsKey(this.id)) {
//			visited.put(this.id, true);
//			if(this.next != null && this.next.id != this.id+1) {
//				ConsoleUtils.println(this.id + "\t" + this + "   ==> " + this.next.id);
//			}
//			else {
//				ConsoleUtils.println(this.id + "\t" + this);
//			}
//			if(this.next != null) {
//				next.dump(visited);
//			}
//			if(this.branch() != null) {
//				this.branch().dump(visited);
//			}
//		}
//		
//	}
}

class IFail extends Instruction {
	IFail(Expression e) {
		super(InstructionSet.Fail, e, null);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.fail();
	}
}

class IAlt extends Instruction {
	public final Instruction failjump;
	IAlt(Expression e, Instruction failjump, Instruction next) {
		super(InstructionSet.Alt, e, next);
		this.failjump = labeling(failjump);
	}
	@Override
	Instruction branch() {
		return this.failjump;
	}
	@Override
	protected String getOperand() {
		return label(this.failjump) + "  ## " + e;
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeJumpAddr(this.failjump);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.pushAlt(this.failjump);
		return this.next;
	}
}

//
//class INotFailPush1 extends IAlt implements StackOperation {
//	INotFailPush1(Expression e, Instruction failjump, Instruction next) {
//		super(e, failjump, next);
//	}
//}

class ISucc extends Instruction {
	ISucc(Expression e, Instruction next) {
		super(InstructionSet.Succ, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.popAlt();
		return this.next;
	}
}

//class IFailSkip extends Instruction {
//	IFailSkip(Expression e) {
//		super(e, null);
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		return sc.failSkip(this);
//	}
//}

/*
 * IFailCheckSkip
 * Check unconsumed repetition
 */

class ISkip extends Instruction {
	ISkip(Expression e) {
		super(InstructionSet.Skip, e, null);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.skip(this.next);
	}
}

class ILabel extends Instruction {
	Production rule;
	ILabel(Production rule, Instruction next) {
		super(InstructionSet.Label, rule, next);
		this.rule = rule;
	}
	@Override
	protected String getOperand() {
		return rule.getLocalName();
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeNonTerminal(rule.getLocalName());
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		return this.next;
	}
}

class ICall extends Instruction {
	Production rule;
	NonTerminal ne;
	public Instruction jump = null;
	ICall(Production rule, Instruction next) {
		super(InstructionSet.Call, rule, next);
		this.rule = rule;
	}
	void setResolvedJump(Instruction jump) {
		assert(this.jump == null);
		this.jump = labeling(this.next);
		this.next = labeling(jump);
	}
	@Override
	protected String getOperand() {
		return label(jump);
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeJumpAddr(this.jump);
		c.encodeNonTerminal(rule.getLocalName());  // debug information
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.ref = this.jump;
		return this.next;
	}
}

class IRet extends Instruction {
	IRet(Production e) {
		super(InstructionSet.Ret, e, null);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		return (Instruction)s.ref;
	}
}

//class IMemoCall extends Instruction implements StackOperation {
//	public Instruction returnPoint = null;
//	public Production  production; 
//	public MemoPoint   memoPoint = null;
//	public Instruction nonMemoCodePoint = null;
//	public Instruction memoCodePoint    = null;
//	CodePoint codePoint;
//	
//	IMemoCall(CodePoint codePoint, Instruction next) {
//		super(codePoint.production, next);
//		this.codePoint = codePoint;
//		this.production = codePoint.production;
//	}
//	@Override
//	protected String getOperand() {
//		return label(returnPoint) + "   ## " + production.getLocalName();
//	}
//	void resolveJumpAddress() {
//		assert(this.returnPoint == null);
//		assert(this.codePoint != null);
//		this.memoCodePoint = codePoint.memoStart;
//		this.nonMemoCodePoint = codePoint.nonmemoStart;
//		this.memoPoint = codePoint.memoPoint;
//		this.returnPoint = labeling(this.next);
//		this.next = labeling(memoCodePoint);
//		this.codePoint = null;
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		ContextStack top = sc.newUnusedLocalStack();
//		top.jump = this.returnPoint;
//		return this.next;
//	}
//	void deactivateMemo() {
//		this.next = nonMemoCodePoint;
//	}
//}
//
//class IMemoRet extends Instruction implements StackOperation {
//	public IMemoCall callPoint = null;
//	IMemoRet(Production p, IMemoCall callPoint) {
//		super(p, null);
//		this.callPoint = callPoint;
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		Instruction returnPoint = sc.popLocalStack().jump;
//		if(this.callPoint != null) {
//			if(callPoint.memoPoint.checkDeactivation()) {
//				callPoint.deactivateMemo();
//				callPoint = null;
//			}
//		}
//		return returnPoint;
//	}
//}

class IPos extends Instruction {
	IPos(Expression e, Instruction next) {
		super(InstructionSet.Pos, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getPosition();
		return this.next;
	}
}

class IBack extends Instruction {
	public IBack(Expression e, Instruction next) {
		super(InstructionSet.Back, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.setPosition(s.value);
		return this.next;
	}
}

class IExit extends Instruction {
	boolean status;
	IExit(boolean status) {
		super(InstructionSet.Exit, null, null);
		this.status = status;
	}
	@Override
	void encodeA(ByteCoder c) {
		c.write_b(status);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		throw new TerminationException(status);
	}
}

abstract class AbstractByteInstruction extends Instruction {
	public final int byteChar;
	AbstractByteInstruction(byte bytecode, ByteChar e, Instruction next) {
		super(bytecode, e, next);
		this.byteChar = e.byteChar;
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacter(byteChar);
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeByteChar(byteChar);
	}
}

class IByte extends AbstractByteInstruction {
	IByte(ByteChar e, Instruction next) {
		super(InstructionSet.Byte, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

class INByte extends AbstractByteInstruction {
	INByte(ByteChar e, Instruction next) {
		super(InstructionSet.NByte, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.byteAt(sc.getPosition()) != this.byteChar) {
			return this.next;
		}
		return sc.fail();
	}
}

class IOByte extends AbstractByteInstruction {
	IOByte(ByteChar e, Instruction next) {
		super(InstructionSet.OByte, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
		}
		return this.next;
	}
}

class IRByte extends AbstractByteInstruction {
	IRByte(ByteChar e, Instruction next) {
		super(InstructionSet.RByte, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		while(sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
		}
		return this.next;
	}
}

abstract class AbstractAnyInstruction extends Instruction {
	AbstractAnyInstruction(byte opcode, Expression e, Instruction next) {
		super(opcode, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
}

class IAny extends AbstractAnyInstruction {
	IAny(Expression e, Instruction next) {
		super(InstructionSet.Any, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.hasUnconsumed()) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

class INAny extends AbstractAnyInstruction {
	INAny(Expression e, boolean isBinary, Instruction next) {
		super(InstructionSet.NAny, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.hasUnconsumed()) {
			return sc.fail();
		}
		return next;
	}
}

abstract class AbstractSetInstruction extends Instruction {
	public final boolean[] byteMap;
	AbstractSetInstruction(byte opcode, ByteMap e, Instruction next) {
		super(opcode, e, next);
		this.byteMap = e.byteMap;
	}
	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacterClass(byteMap);
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeByteMap(byteMap);
	}
}

class ISet extends AbstractSetInstruction {
	ISet(ByteMap e, Instruction next) {
		super(InstructionSet.Set, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		if(byteMap[byteChar]) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

class IOSet extends AbstractSetInstruction {
	IOSet(ByteMap e, Instruction next) {
		super(InstructionSet.Set, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		if(byteMap[byteChar]) {
			sc.consume(1);
		}
		return this.next;
	}
}

class INSet extends AbstractSetInstruction {
	INSet(ByteMap e, Instruction next) {
		super(InstructionSet.NSet, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		if(!byteMap[byteChar]) {
			return this.next;
		}
		return sc.fail();
	}
}

class IRSet extends AbstractSetInstruction {
	IRSet(ByteMap e, Instruction next) {
		super(InstructionSet.RSet, e, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		while(byteMap[byteChar]) {
			sc.consume(1);
			byteChar = sc.byteAt(sc.getPosition());
		}
		return this.next;
	}
}

abstract class AbstractStrInstruction extends Instruction {
	final byte[] utf8;
	public AbstractStrInstruction(byte opcode, MultiChar e, byte[] utf8, Instruction next) {
		super(opcode, e, next);
		this.utf8 = utf8;
	}
	@Override
	protected String getOperand() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < utf8.length; i++) {
			if(i > 0) {
				sb.append(" ");
			}
			sb.append(StringUtils.stringfyCharacter(utf8[i] & 0xff));
		}
		return sb.toString();
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeMultiByte(utf8);
	}
}


class IStr extends AbstractStrInstruction {
	public IStr(MultiChar e, Instruction next) {
		super(InstructionSet.Str, e, e.byteSeq, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
			return this.next;
		}
		return sc.fail();
	}
}

class INStr extends AbstractStrInstruction {
	public INStr(MultiChar e, Instruction next) {
		super(InstructionSet.NStr, e, e.byteSeq, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(!sc.match(sc.getPosition(), this.utf8)) {
			return this.next;
		}
		return sc.fail();
	}
}

class IOStr extends AbstractStrInstruction {
	public IOStr(MultiChar e, Instruction next) {
		super(InstructionSet.OStr, e, e.byteSeq, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		if(sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
		}
		return this.next;
	}
}

class IRStr extends AbstractStrInstruction {
	public IRStr(MultiChar e, Instruction next) {
		super(InstructionSet.RStr, e, e.byteSeq, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		while(sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
		}
		return this.next;
	}
}

class IConsume extends Instruction {
	int shift;
	IConsume(Expression e, int shift, Instruction next) {
		super(InstructionSet.Consume, e, next);
		this.shift = shift;
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeShift(shift);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.consume(this.shift);
		return this.next;
	}
}

//class IBacktrack extends Instruction {
//	final int prefetched;
//	IBacktrack(Expression e, int prefetched, Instruction next) {
//		super(e, next);
//		this.prefetched = prefetched;
//	}
//	@Override
//	Instruction exec(Context sc) throws TerminationException {
//		sc.consume(-1);
//		return this.next;
//	}
//}


class IFirst extends Instruction {
	Instruction[] jumpTable;
	IFirst(Expression e, Instruction next) {
		super(InstructionSet.First, e, next);
		jumpTable = new Instruction[257];
		Arrays.fill(jumpTable, next);
	}
	void setJumpTable(int ch, Instruction inst) {
		if(inst instanceof IFirst) {
			jumpTable[ch] = ((IFirst) inst).jumpTable[ch];
		}
		else {
			jumpTable[ch] = Instruction.labeling(inst);
		}
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeJumpTable();
		for(int i = 0; i < jumpTable.length; i++) {
			c.encodeJumpAddr(jumpTable[i]);
		}
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		int ch = sc.byteAt(sc.getPosition());
		return jumpTable[ch].exec(sc);
	}
}

abstract class AbstractMemoizationInstruction extends Instruction {
	final MemoPoint memoPoint;
	final int memoId;
	final boolean state;
	final Instruction skip;
	AbstractMemoizationInstruction(byte opcode, Expression e, MemoPoint m, boolean state, Instruction next, Instruction skip) {
		super(opcode, e, next);
		this.memoPoint = m;
		this.memoId = m.id;
		this.skip = labeling(skip);
		this.state = state;
	}
	AbstractMemoizationInstruction(byte opcode, Expression e, MemoPoint m, boolean state, Instruction next) {
		super(opcode, e, next);
		this.memoPoint = m;
		this.memoId = m.id;
		this.state = state;
		this.skip = null;
	}
	@Override
	protected String getOperand() {
		return String.valueOf(this.memoPoint);
	}
	@Override
	void encodeA(ByteCoder c) {
		c.write_b(this.state);
		c.write_u32(memoId);
		if(skip != null) {
			c.encodeJumpAddr(skip);
		}
	}
}

class ILookup extends AbstractMemoizationInstruction {
	ILookup(Expression e, MemoPoint m, boolean state, Instruction next, Instruction skip) {
		super(InstructionSet.Lookup, e, m, state, next, skip);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		MemoEntry entry = sc.getMemo(memoId, state); 
		if(entry != null) {
			if(entry.failed) {
				memoPoint.failHit();
				return sc.fail();
			}
			memoPoint.memoHit(entry.consumed);
			sc.consume(entry.consumed);
			return this.skip;
		}
		memoPoint.miss();
		return this.next;
	}
}

class IMemo extends AbstractMemoizationInstruction {
	IMemo(Expression e, MemoPoint m, boolean state, Instruction next) {
		super(InstructionSet.Memo, e, m, state, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		long ppos = sc.popAlt();
		int length = (int)(sc.getPosition() - ppos);
		sc.setMemo(ppos, memoId, false, null, length, this.state);
		return this.next;
	}
}

class IMemoFail extends AbstractMemoizationInstruction {
	IMemoFail(Expression e, boolean state, MemoPoint m) {
		super(InstructionSet.MemoFail, e, m, state, null);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.setMemo(sc.getPosition(), memoId, true, null, 0, state);
		return sc.fail();
	}
}


// AST Construction 

class INew extends Instruction {
	int shift;
	INew(New e, Instruction next) {
		super(InstructionSet.TNew, e, next);
		this.shift = e.shift;
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeShift(shift);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logNew(sc.getPosition() + shift, this.id);
		return this.next;
	}
}

class ITLeftFold extends Instruction {
	int shift;
	Tag label;
	ITLeftFold(New e, Instruction next) {
		super(InstructionSet.TLeftFold, e, next);
		this.shift = e.shift;
		this.label = e.getLabel();
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeShift(shift);
		c.encodeLabel(label);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logLeftFold(sc.getPosition() + shift, this.label);
		return this.next;
	}
}

class ICapture extends Instruction {
	int shift;
	ICapture(Capture e, Instruction next) {
		super(InstructionSet.TCapture, e, next);
		this.shift = e.shift;
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeShift(shift);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logCapture(sc.getPosition() + shift);
		return this.next;
	}
}

class IReplace extends Instruction {
	public final String value;
	IReplace(Replace e, Instruction next) {
		super(InstructionSet.TReplace, e, next);
		this.value = e.value;
	}
	@Override
	protected String getOperand() {
		return StringUtils.quoteString('"', value, '"');
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeMultiByte(value.getBytes());
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logReplace(this.value);
		return this.next;
	}
}

class ITag extends Instruction {
	public final Tag tag;
	ITag(Tagging e, Instruction next) {
		super(InstructionSet.TTag, e, next);
		this.tag = e.tag;
	}
	@Override
	protected String getOperand() {
		return StringUtils.quoteString('"', tag.name, '"');
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeTag(tag);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logTag(tag);
		return this.next;
	}
}

class ITPush extends Instruction {
	ITPush(Link e, Instruction next) {
		super(InstructionSet.TPush, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPush();
		return this.next;
	}
}

class ITPop extends Instruction {
	public final Tag label;
	ITPop(Link e, Instruction next) {
		super(InstructionSet.TPop, e, next);
		this.label = e.getLabel();
	}
	@Override
	protected String getOperand() {
		return label.getName();
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeLabel(label);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPop(label);
		return this.next;
	}
}

class ITStart extends Instruction {
	ITStart(Link e, Instruction next) {
		super(InstructionSet.TStart, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		ASTMachine astMachine = sc.getAstMachine();
		s.ref = astMachine.saveTransactionPoint();
		return this.next;
	}
}

class ICommit extends Instruction {
	public final Tag label;
	ICommit(Link e, Instruction next) {
		super(InstructionSet.TCommit, e, next);
		this.label = e.getLabel();
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeLabel(label);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.commitTransactionPoint(label, s.ref);
		return this.next;
	}
}

class ITLookup extends AbstractMemoizationInstruction {
	public final Tag label;
	ITLookup(Link e, MemoPoint m, boolean state, Instruction next, Instruction skip) {
		super(InstructionSet.TLookup, e, m, state, next, skip);
		this.label = e.getLabel();
	}
	@Override
	void encodeA(ByteCoder c) {
		super.encodeA(c);
		c.encodeLabel(label);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		MemoEntry entry = sc.getMemo(memoId, state); 
		if(entry != null) {
			if(entry.failed) {
				memoPoint.failHit();
				return sc.fail();
			}
			memoPoint.memoHit(entry.consumed);
			sc.consume(entry.consumed);
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logLink(label, entry.result);
			return this.skip;
		}
		memoPoint.miss();
		return this.next;
	}
}

class ITMemo extends AbstractMemoizationInstruction {
	ITMemo(Expression e, MemoPoint m, boolean state, Instruction next) {
		super(InstructionSet.Memo, e, m, state, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		long ppos = sc.popAlt();
		int length = (int)(sc.getPosition() - ppos);
		sc.setMemo(ppos, memoId, false, astMachine.getLatestLinkedNode(), length, this.state);
		return this.next;
	}
}


/* Symbol */

abstract class AbstractTableInstruction extends Instruction {
	final Tag tableName;
	AbstractTableInstruction(byte opcode, Expression e, Tag tableName, Instruction next) {
		super(opcode, e, next);
		this.tableName = tableName;
	}
	@Override
	protected String getOperand() {
		return tableName.getName();
	}
	@Override
	void encodeA(ByteCoder c) {
		c.encodeSymbolTable(tableName);
	}
}

class IBeginSymbolScope extends Instruction {
	IBeginSymbolScope(Block e, Instruction next) {
		super(InstructionSet.SOpen, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No Arguments
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getSymbolTable().savePoint();
		return this.next;
	}
}

class IBeginLocalScope extends AbstractTableInstruction {
	IBeginLocalScope(LocalTable e, Instruction next) {
		super(InstructionSet.SMask, e, e.getTable(), next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		SymbolTable st = sc.getSymbolTable();
		s.value = st.savePoint();
		st.addSymbolMask(tableName);
		return this.next;
	}
}

class IEndSymbolScope extends Instruction {
	IEndSymbolScope(Expression e, Instruction next) {
		super(InstructionSet.SClose, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.getSymbolTable().rollBack((int)s.value);
		return this.next;
	}
}

class IDefSymbol extends AbstractTableInstruction {
	IDefSymbol(DefSymbol e, Instruction next) {
		super(InstructionSet.SDef, e, e.tableName, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData top = sc.popStack();
		byte[] captured = sc.subbyte(top.value, sc.getPosition());
		sc.getSymbolTable().addSymbol(this.tableName, captured);
		return this.next;
	}
}

class IExistsSymbol extends AbstractTableInstruction {
	IExistsSymbol(ExistsSymbol e, Instruction next) {
		super(InstructionSet.SExists, e, e.tableName, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(tableName);
		return t != null ? this.next : sc.fail();
	}
}

class IsMatch extends AbstractTableInstruction {
	IsMatch(IsSymbol e, Instruction next) {
		super(InstructionSet.SMatch, e, e.tableName, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(tableName);
		if(t != null && sc.match(sc.getPosition(), t)) {
			sc.consume(t.length);
			return this.next;
		}
		return sc.fail();
	}
}

class IIsSymbol extends AbstractTableInstruction {
	IIsSymbol(IsSymbol e, Instruction next) {
		super(InstructionSet.SIs, e, e.tableName, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] symbol = sc.getSymbolTable().getSymbol(tableName);
		if(symbol != null) {
			StackData s = sc.popStack();
			byte[] captured = sc.subbyte(s.value, sc.getPosition());
			if(symbol.length == captured.length && SymbolTable.equals(symbol, captured)) {
				sc.consume(symbol.length);
				return this.next;
			}
		}
		return sc.fail();
	}
}

class IIsaSymbol extends AbstractTableInstruction {
	IIsaSymbol(IsSymbol e, Instruction next) {
		super(InstructionSet.SIsa, e, e.tableName, next);
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		byte[] captured = sc.subbyte(s.value, sc.getPosition());
		if(sc.getSymbolTable().contains(this.tableName, captured)) {
			sc.consume(captured.length);
			return this.next;
			
		}
		return sc.fail();
	}
}

class IDefIndent extends Instruction {
	IDefIndent(DefIndent e, Instruction next) {
		super(InstructionSet.Nop, e, next);
	}
	final long getLineStartPosition(RuntimeContext sc, long fromPostion) {
		long startIndex = fromPostion;
		if(!(startIndex < sc.length())) {
			startIndex = sc.length() - 1;
		}
		if(startIndex < 0) {
			startIndex = 0;
		}
		while(startIndex > 0) {
			int ch = sc.byteAt(startIndex);
			if(ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		long pos = sc.getPosition();
		long spos = getLineStartPosition(sc, pos);
		byte[] b = sc.subbyte(spos, pos);
		for(int i = 0; i < b.length; i++) {
			if(b[i] != '\t') {
				b[i] = ' ';
			}
		}
		sc.getSymbolTable().addSymbol(NezTag.Indent, b);
		return this.next;
	}
}

class IIsIndent extends Instruction {
	IIsIndent(IsIndent e, Instruction next) {
		super(InstructionSet.Nop, e, next);
	}
	@Override
	void encodeA(ByteCoder c) {
		// No argument
	}
	@Override
	Instruction exec(RuntimeContext sc) throws TerminationException {
		long pos = sc.getPosition();
		if(pos > 0) {
			if(sc.byteAt(pos-1) != '\n') {
				return sc.fail();
			}
		}
		byte[] b = sc.getSymbolTable().getSymbol(NezTag.Indent);
		if(b != null) {
			if(sc.match(pos, b)) {
				sc.consume(b.length);
				return this.next;
			}
			return sc.fail();
		}
		return this.next;  // empty entry is allowable
	}
}
