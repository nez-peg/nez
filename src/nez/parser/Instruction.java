package nez.parser;

import java.util.Arrays;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlfold;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Ttag;
import nez.lang.expr.Xblock;
import nez.lang.expr.Xdefindent;
import nez.lang.expr.Xexists;
import nez.lang.expr.Xindent;
import nez.lang.expr.Xis;
import nez.lang.expr.Xlocal;
import nez.lang.expr.Xmatch;
import nez.lang.expr.Xsymbol;
import nez.util.StringUtils;

public abstract class Instruction {
	public final byte opcode;
	protected Expression e;
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
		if (this.next != null) {
			return this.next.id == this.id + 1;
		}
		return true; // RET or instructions that are unnecessary to go next
	}

	Instruction branch() {
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

	public abstract Instruction exec(RuntimeContext sc) throws TerminationException;

	protected static Instruction labeling(Instruction inst) {
		if (inst != null) {
			inst.label = true;
		}
		return inst;
	}

	protected static String label(Instruction inst) {
		return "L" + inst.id;
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
}

class IFail extends Instruction {
	IFail(Expression e) {
		super(InstructionSet.Fail, e, null);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
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
	protected void encodeImpl(ByteCoder c) {
		c.encodeJump(this.failjump);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.pushAlt(this.failjump);
		return this.next;
	}
}

//
// class INotFailPush1 extends IAlt implements StackOperation {
// INotFailPush1(Expression e, Instruction failjump, Instruction next) {
// super(e, failjump, next);
// }
// }

class ISucc extends Instruction {
	ISucc(Expression e, Instruction next) {
		super(InstructionSet.Succ, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.popAlt();
		return this.next;
	}
}

// class IFailSkip extends Instruction {
// IFailSkip(Expression e) {
// super(e, null);
// }
// @Override
// Instruction exec(Context sc) throws TerminationException {
// return sc.failSkip(this);
// }
// }

/*
 * IFailCheckSkip Check unconsumed repetition
 */

class ISkip extends Instruction {
	ISkip(Expression e) {
		super(InstructionSet.Skip, e, null);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.skip(this.next);
	}
}

class ILabel extends Instruction {
	Production rule;

	ILabel(Production rule, Instruction next) {
		super(InstructionSet.Label, rule.getExpression(), next);
		this.rule = rule;
	}

	@Override
	protected String getOperand() {
		return rule.getLocalName();
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeNonTerminal(rule.getLocalName());
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		return this.next;
	}
}

class ICall extends Instruction {
	Production prod;
	NonTerminal ne;
	public Instruction jump = null;

	ICall(Production rule, Instruction next) {
		super(InstructionSet.Call, rule.getExpression(), next);
		this.prod = rule;
	}

	void setResolvedJump(Instruction jump) {
		assert (this.jump == null);
		this.jump = labeling(this.next);
		this.next = labeling(jump);
	}

	@Override
	protected String getOperand() {
		return label(jump);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeJump(this.jump);
		c.encodeNonTerminal(prod.getLocalName()); // debug information
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.ref = this.jump;
		return this.next;
	}
}

class IRet extends Instruction {
	IRet(Production e) {
		super(InstructionSet.Ret, e.getExpression(), null);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		return (Instruction) s.ref;
	}
}

class IPos extends Instruction {
	IPos(Expression e, Instruction next) {
		super(InstructionSet.Pos, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
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
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
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
	protected void encodeImpl(ByteCoder c) {
		c.write_b(status);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		throw new TerminationException(status);
	}
}

abstract class AbstractByteInstruction extends Instruction {
	public final int byteChar;

	AbstractByteInstruction(byte bytecode, Cbyte e, Instruction next) {
		super(bytecode, e, next);
		this.byteChar = e.byteChar;
	}

	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacter(byteChar);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeByte(byteChar);
	}
}

class IByte extends AbstractByteInstruction {
	IByte(Cbyte e, Instruction next) {
		super(InstructionSet.Byte, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

class INByte extends AbstractByteInstruction {
	INByte(Cbyte e, Instruction next) {
		super(InstructionSet.NByte, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.byteAt(sc.getPosition()) != this.byteChar) {
			return this.next;
		}
		return sc.fail();
	}
}

class IOByte extends AbstractByteInstruction {
	IOByte(Cbyte e, Instruction next) {
		super(InstructionSet.OByte, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
		}
		return this.next;
	}
}

class IRByte extends AbstractByteInstruction {
	IRByte(Cbyte e, Instruction next) {
		super(InstructionSet.RByte, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		while (sc.byteAt(sc.getPosition()) == this.byteChar) {
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
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}
}

class IAny extends AbstractAnyInstruction {
	IAny(Expression e, Instruction next) {
		super(InstructionSet.Any, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.hasUnconsumed()) {
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
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.hasUnconsumed()) {
			return sc.fail();
		}
		return next;
	}
}

abstract class AbstractSetInstruction extends Instruction {
	public final boolean[] byteMap;

	AbstractSetInstruction(byte opcode, Cset e, Instruction next) {
		super(opcode, e, next);
		this.byteMap = e.byteMap;
		if (this.byteMap[0]) {
			this.byteMap[0] = false; // for safety
		}
	}

	@Override
	protected String getOperand() {
		return StringUtils.stringfyCharacterClass(byteMap);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeBset(byteMap);
	}
}

class ISet extends AbstractSetInstruction {
	ISet(Cset e, Instruction next) {
		super(InstructionSet.Set, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		if (byteMap[byteChar]) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}
}

class IOSet extends AbstractSetInstruction {
	IOSet(Cset e, Instruction next) {
		super(InstructionSet.OSet, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		if (byteMap[byteChar]) {
			sc.consume(1);
		}
		return this.next;
	}
}

class INSet extends AbstractSetInstruction {
	INSet(Cset e, Instruction next) {
		super(InstructionSet.NSet, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		if (!byteMap[byteChar]) {
			return this.next;
		}
		return sc.fail();
	}
}

class IRSet extends AbstractSetInstruction {
	IRSet(Cset e, Instruction next) {
		super(InstructionSet.RSet, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int byteChar = sc.byteAt(sc.getPosition());
		while (byteMap[byteChar]) {
			sc.consume(1);
			byteChar = sc.byteAt(sc.getPosition());
		}
		return this.next;
	}
}

abstract class AbstractStrInstruction extends Instruction {
	final byte[] utf8;

	public AbstractStrInstruction(byte opcode, Cmulti e, byte[] utf8, Instruction next) {
		super(opcode, e, next);
		this.utf8 = utf8;
	}

	@Override
	protected String getOperand() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < utf8.length; i++) {
			if (i > 0) {
				sb.append(" ");
			}
			sb.append(StringUtils.stringfyCharacter(utf8[i] & 0xff));
		}
		return sb.toString();
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeBstr(utf8);
	}
}

class IStr extends AbstractStrInstruction {
	public IStr(Cmulti e, Instruction next) {
		super(InstructionSet.Str, e, e.byteSeq, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
			return this.next;
		}
		return sc.fail();
	}
}

class INStr extends AbstractStrInstruction {
	public INStr(Cmulti e, Instruction next) {
		super(InstructionSet.NStr, e, e.byteSeq, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (!sc.match(sc.getPosition(), this.utf8)) {
			return this.next;
		}
		return sc.fail();
	}
}

class IOStr extends AbstractStrInstruction {
	public IOStr(Cmulti e, Instruction next) {
		super(InstructionSet.OStr, e, e.byteSeq, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
		}
		return this.next;
	}
}

class IRStr extends AbstractStrInstruction {
	public IRStr(Cmulti e, Instruction next) {
		super(InstructionSet.RStr, e, e.byteSeq, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		while (sc.match(sc.getPosition(), this.utf8)) {
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
	protected void encodeImpl(ByteCoder c) {
		c.encodeShift(shift);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.consume(this.shift);
		return this.next;
	}
}

// class IBacktrack extends Instruction {
// final int prefetched;
// IBacktrack(Expression e, int prefetched, Instruction next) {
// super(e, next);
// this.prefetched = prefetched;
// }
// @Override
// Instruction exec(Context sc) throws TerminationException {
// sc.consume(-1);
// return this.next;
// }
// }

class IFirst extends Instruction {
	Instruction[] jumpTable;

	IFirst(byte opcode, Pchoice e, Instruction next) {
		super(opcode, e, next);
		jumpTable = new Instruction[257];
		Arrays.fill(jumpTable, next);
	}

	IFirst(Pchoice e, Instruction next) {
		this(InstructionSet.First, e, next);
	}

	void setJumpTable(int ch, Instruction inst) {
		jumpTable[ch] = Instruction.labeling(inst);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeJumpTable();
		for (int i = 0; i < jumpTable.length; i++) {
			c.encodeJump(jumpTable[i]);
		}
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int ch = sc.byteAt(sc.getPosition());
		return jumpTable[ch].exec(sc);
	}
}

class IDFirst extends IFirst {
	IDFirst(Pchoice e, Instruction next) {
		super(InstructionSet.DFirst, e, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int ch = sc.byteAt(sc.getPosition());
		sc.consume(1);
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
	protected void encodeImpl(ByteCoder c) {
		c.write_b(this.state);
		c.write_u32(memoId);
		if (skip != null) {
			c.encodeJump(skip);
		}
	}
}

class ILookup extends AbstractMemoizationInstruction {
	ILookup(Expression e, MemoPoint m, boolean state, Instruction next, Instruction skip) {
		super(InstructionSet.Lookup, e, m, state, next, skip);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		MemoEntry entry = sc.getMemo(memoId, state);
		if (entry != null) {
			if (entry.failed) {
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
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		long ppos = sc.popAlt();
		int length = (int) (sc.getPosition() - ppos);
		sc.setMemo(ppos, memoId, false, null, length, this.state);
		return this.next;
	}
}

class IMemoFail extends AbstractMemoizationInstruction {
	IMemoFail(Expression e, boolean state, MemoPoint m) {
		super(InstructionSet.MemoFail, e, m, state, null);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.setMemo(sc.getPosition(), memoId, true, null, 0, state);
		return sc.fail();
	}
}

// AST Construction

class INew extends Instruction {
	int shift;

	INew(Tnew e, Instruction next) {
		super(InstructionSet.TNew, e, next);
		this.shift = e.shift;
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeShift(shift);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logNew(sc.getPosition() + shift, this.id);
		return this.next;
	}
}

class ITLeftFold extends Instruction {
	int shift;
	Symbol label;

	ITLeftFold(Tlfold e, Instruction next) {
		super(InstructionSet.TLeftFold, e, next);
		this.shift = e.shift;
		this.label = e.getLabel();
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeShift(shift);
		c.encodeLabel(label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logLeftFold(sc.getPosition() + shift, this.label);
		return this.next;
	}
}

class ICapture extends Instruction {
	int shift;

	ICapture(Tcapture e, Instruction next) {
		super(InstructionSet.TCapture, e, next);
		this.shift = e.shift;
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeShift(shift);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logCapture(sc.getPosition() + shift);
		return this.next;
	}
}

class IReplace extends Instruction {
	public final String value;

	IReplace(Treplace e, Instruction next) {
		super(InstructionSet.TReplace, e, next);
		this.value = e.value;
	}

	@Override
	protected String getOperand() {
		return StringUtils.quoteString('"', value, '"');
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeBstr(value.getBytes());
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logReplace(this.value);
		return this.next;
	}
}

class ITag extends Instruction {
	public final Symbol tag;

	ITag(Ttag e, Instruction next) {
		super(InstructionSet.TTag, e, next);
		this.tag = e.tag;
	}

	@Override
	protected String getOperand() {
		return StringUtils.quoteString('"', tag.getSymbol(), '"');
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeTag(tag);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logTag(tag);
		return this.next;
	}
}

class ITPush extends Instruction {
	ITPush(Tlink e, Instruction next) {
		super(InstructionSet.TPush, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPush();
		return this.next;
	}
}

class ITPop extends Instruction {
	public final Symbol label;

	ITPop(Tlink e, Instruction next) {
		super(InstructionSet.TPop, e, next);
		this.label = e.getLabel();
	}

	@Override
	protected String getOperand() {
		return label.getSymbol();
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeLabel(label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPop(label);
		return this.next;
	}
}

class ITStart extends Instruction {
	ITStart(Tlink e, Instruction next) {
		super(InstructionSet.TStart, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		ASTMachine astMachine = sc.getAstMachine();
		s.ref = astMachine.saveTransactionPoint();
		return this.next;
	}
}

class ICommit extends Instruction {
	public final Symbol label;

	ICommit(Tlink e, Instruction next) {
		super(InstructionSet.TCommit, e, next);
		this.label = e.getLabel();
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeLabel(label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.commitTransactionPoint(label, s.ref);
		return this.next;
	}
}

class ITLookup extends AbstractMemoizationInstruction {
	public final Symbol label;

	ITLookup(Tlink e, MemoPoint m, boolean state, Instruction next, Instruction skip) {
		super(InstructionSet.TLookup, e, m, state, next, skip);
		this.label = e.getLabel();
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		super.encodeImpl(c);
		c.encodeLabel(label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		MemoEntry entry = sc.getMemo(memoId, state);
		if (entry != null) {
			if (entry.failed) {
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
		super(InstructionSet.TMemo, e, m, state, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		long ppos = sc.popAlt();
		int length = (int) (sc.getPosition() - ppos);
		sc.setMemo(ppos, memoId, false, astMachine.getLatestLinkedNode(), length, this.state);
		return this.next;
	}
}

/* Symbol */

abstract class AbstractTableInstruction extends Instruction {
	final Symbol tableName;

	AbstractTableInstruction(byte opcode, Expression e, Symbol tableName, Instruction next) {
		super(opcode, e, next);
		this.tableName = tableName;
	}

	@Override
	protected String getOperand() {
		return tableName.getSymbol();
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		c.encodeTable(tableName);
	}
}

class IBeginSymbolScope extends Instruction {
	IBeginSymbolScope(Xblock e, Instruction next) {
		super(InstructionSet.SOpen, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No Arguments
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getSymbolTable().savePoint();
		return this.next;
	}
}

class IBeginLocalScope extends AbstractTableInstruction {
	IBeginLocalScope(Xlocal e, Instruction next) {
		super(InstructionSet.SMask, e, e.getTable(), next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
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
	protected void encodeImpl(ByteCoder c) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.getSymbolTable().rollBack((int) s.value);
		return this.next;
	}
}

class IDefSymbol extends AbstractTableInstruction {
	IDefSymbol(Xsymbol e, Instruction next) {
		super(InstructionSet.SDef, e, e.tableName, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData top = sc.popStack();
		byte[] captured = sc.subbyte(top.value, sc.getPosition());
		// System.out.println("symbol captured: " + new String(captured) + ", @"
		// + this.tableName);
		sc.getSymbolTable().addSymbol(this.tableName, captured);
		return this.next;
	}
}

class IExists extends AbstractTableInstruction {
	IExists(Xexists e, Instruction next) {
		super(InstructionSet.SExists, e, e.tableName, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(tableName);
		return t != null ? this.next : sc.fail();
	}
}

class IExistsSymbol extends AbstractTableInstruction {
	byte[] symbol;

	IExistsSymbol(Xexists e, Instruction next) {
		super(InstructionSet.SIsDef, e, e.tableName, next);
		symbol = StringUtils.toUtf8(e.getSymbol());
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		super.encodeImpl(c);
		c.encodeBstr(symbol);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.getSymbolTable().contains(this.tableName, symbol)) {
			return this.next;
		}
		return sc.fail();
	}
}

class IMatch extends AbstractTableInstruction {
	IMatch(Xmatch e, Instruction next) {
		super(InstructionSet.SMatch, e, e.getTable(), next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(tableName);
		if (t != null && sc.match(sc.getPosition(), t)) {
			sc.consume(t.length);
			return this.next;
		}
		return sc.fail();
	}
}

class IIsSymbol extends AbstractTableInstruction {
	IIsSymbol(Xis e, Instruction next) {
		super(InstructionSet.SIs, e, e.tableName, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] symbol = sc.getSymbolTable().getSymbol(tableName);
		if (symbol != null) {
			StackData s = sc.popStack();
			byte[] captured = sc.subbyte(s.value, sc.getPosition());
			// System.out.println("captured:" + new String(captured));
			if (symbol.length == captured.length && SymbolTable.equals(symbol, captured)) {
				// sc.consume(symbol.length);
				return this.next;
			}
		}
		return sc.fail();
	}
}

class IIsaSymbol extends AbstractTableInstruction {
	IIsaSymbol(Xis e, Instruction next) {
		super(InstructionSet.SIsa, e, e.tableName, next);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		byte[] captured = sc.subbyte(s.value, sc.getPosition());
		if (sc.getSymbolTable().contains(this.tableName, captured)) {
			// sc.consume(captured.length);
			return this.next;

		}
		return sc.fail();
	}
}

class IDefIndent extends Instruction {
	public final static Symbol _Indent = Symbol.tag("Indent");

	IDefIndent(Xdefindent e, Instruction next) {
		super(InstructionSet.Nop, e, next);
	}

	final long getLineStartPosition(RuntimeContext sc, long fromPostion) {
		long startIndex = fromPostion;
		if (!(startIndex < sc.length())) {
			startIndex = sc.length() - 1;
		}
		if (startIndex < 0) {
			startIndex = 0;
		}
		while (startIndex > 0) {
			int ch = sc.byteAt(startIndex);
			if (ch == '\n') {
				startIndex = startIndex + 1;
				break;
			}
			startIndex = startIndex - 1;
		}
		return startIndex;
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		long pos = sc.getPosition();
		long spos = getLineStartPosition(sc, pos);
		byte[] b = sc.subbyte(spos, pos);
		for (int i = 0; i < b.length; i++) {
			if (b[i] != '\t') {
				b[i] = ' ';
			}
		}
		sc.getSymbolTable().addSymbol(_Indent, b);
		return this.next;
	}
}

class IIsIndent extends Instruction {
	public final static Symbol _Indent = Symbol.tag("Indent");

	IIsIndent(Xindent e, Instruction next) {
		super(InstructionSet.Nop, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// No argument
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		long pos = sc.getPosition();
		if (pos > 0) {
			if (sc.byteAt(pos - 1) != '\n') {
				return sc.fail();
			}
		}
		byte[] b = sc.getSymbolTable().getSymbol(_Indent);
		if (b != null) {
			if (sc.match(pos, b)) {
				sc.consume(b.length);
				return this.next;
			}
			return sc.fail();
		}
		return this.next; // empty entry is allowable
	}
}
