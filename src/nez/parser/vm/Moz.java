package nez.parser.vm;

import java.io.IOException;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.parser.ASTMachine;
import nez.parser.ByteCoder;
import nez.parser.Instruction;
import nez.parser.MemoEntry;
import nez.parser.RuntimeContext;
import nez.parser.StackData;
import nez.parser.SymbolTable;
import nez.parser.TerminationException;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class Moz {
	public final static byte Nop = 0;
	public final static byte Fail = 1;
	public final static byte Alt = 2;
	public final static byte Succ = 3;
	public final static byte Jump = 4;
	public final static byte Call = 5;
	public final static byte Ret = 6;
	public final static byte Pos = 7;
	public final static byte Back = 8;
	public final static byte Skip = 9;
	public final static byte Byte = 10;
	public final static byte Any = 11;
	public final static byte Str = 12;
	public final static byte Set = 13;
	public final static byte NByte = 14;
	public final static byte NAny = 15;
	public final static byte NStr = 16;
	public final static byte NSet = 17;
	public final static byte OByte = 18;
	public final static byte OAny = 19;
	public final static byte OStr = 20;
	public final static byte OSet = 21;
	public final static byte RByte = 22;
	public final static byte RAny = 23;
	public final static byte RStr = 24;
	public final static byte RSet = 25;
	public final static byte Consume = 26;
	public final static byte First = 27;
	public final static byte Lookup = 28;
	public final static byte Memo = 29;
	public final static byte MemoFail = 30;
	public final static byte TPush = 31;
	public final static byte TPop = 32;
	public final static byte TLeftFold = 33;
	public final static byte TNew = 34;
	public final static byte TCapture = 35;
	public final static byte TTag = 36;
	public final static byte TReplace = 37;
	public final static byte TStart = 38;
	public final static byte TCommit = 39;
	public final static byte TAbort = 40;
	public final static byte TLookup = 41;
	public final static byte TMemo = 42;
	public final static byte SOpen = 43;
	public final static byte SClose = 44;
	public final static byte SMask = 45;
	public final static byte SDef = 46;
	public final static byte SIsDef = 47;
	public final static byte SExists = 48;
	public final static byte SMatch = 49;
	public final static byte SIs = 50;
	public final static byte SIsa = 51;
	public final static byte SDefNum = 52;
	public final static byte SCount = 53;
	public final static byte Exit = 54;
	public final static byte DFirst = 55;
	public final static byte Label = 56;

	public final static void dump(byte[] code) {
		MozLoader l = new MozLoader();
		try {
			l.loadCode(code);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

abstract class MozInst extends Instruction {

	public MozInst(byte opcode, Expression e, Instruction next) {
		super(opcode, e, next);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// TODO Auto-generated method stub

	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.getClass().getSimpleName());
		formatImpl(sb);
		return sb.toString();
	}

	protected abstract void formatImpl(StringBuilder sb);

	protected void formatNonTerminal(StringBuilder sb, String a) {
		sb.append(' ');
		sb.append(a);
	}

	protected void formatJump(StringBuilder sb, Instruction a) {
		sb.append(' ');
		sb.append("L" + a.id);
	}

	protected void formatJumpTable(StringBuilder sb, Instruction[] a) {
		sb.append(' ');
		sb.append("...");
	}

	protected void formatByte(StringBuilder sb, int a) {
		sb.append(' ');
		sb.append(StringUtils.stringfyCharacter(a));
	}

	protected void formatBset(StringBuilder sb, boolean[] a) {
		sb.append(' ');
		sb.append(StringUtils.stringfyCharacterClass(a));
	}

	protected void formatBstr(StringBuilder sb, byte[] a) {
		for (int i = 0; i < a.length; i++) {
			sb.append(' ');
			sb.append(StringUtils.stringfyCharacter(a[i] & 0xff));
		}
	}

	protected void formatShift(StringBuilder sb, int a) {
		sb.append(' ');
		sb.append("shift=" + String.valueOf(a));
	}

	protected void formatMemoPoint(StringBuilder sb, int a) {
		sb.append(' ');
		sb.append("memo=" + String.valueOf(a));
	}

	protected void formatState(StringBuilder sb, boolean a) {
		sb.append(' ');
		sb.append(a ? "state" : "stateless");
	}

	protected void formatLabel(StringBuilder sb, Symbol a) {
		sb.append(' ');
		sb.append("$" + a);
	}

	protected void formatTag(StringBuilder sb, Symbol a) {
		sb.append(' ');
		sb.append("#" + a);
	}

	protected void formatTable(StringBuilder sb, Symbol a) {
		sb.append(' ');
		sb.append(a.getSymbol());
	}

}

abstract class Branch extends MozInst {
	protected Instruction jump;

	public Branch(byte opcode, Expression e, Instruction next) {
		super(opcode, e, next);
	}
}

abstract class BranchTable extends MozInst {
	protected Instruction[] jumpTable;

	public BranchTable(byte opcode, Expression e, Instruction next) {
		super(opcode, e, next);
	}
}

// Nop
class Nop extends MozInst {
	public Nop(Expression e, Instruction next) {
		super(Moz.Nop, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

}

// Fail
class Fail extends MozInst {
	public Fail(Expression e, Instruction next) {
		super(Moz.Fail, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.fail();
	}
}

// Alt
class Alt extends Branch {
	public Alt(Expression e, Instruction next, Instruction jump) {
		super(Moz.Alt, e, next);
		this.jump = jump;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeJump(this.jump);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatJump(sb, this.jump);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.pushAlt(this.jump);
		return this.next;
	}
}

// Succ
class Succ extends MozInst {
	public Succ(Expression e, Instruction next) {
		super(Moz.Succ, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.popAlt();
		return this.next;
	}

}

// Jump
class Jump extends Branch {
	public Jump(Expression e, Instruction next, Instruction jump) {
		super(Moz.Jump, e, next);
		this.jump = jump;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeJump(this.jump);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatJump(sb, this.jump);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		return this.jump;
	}

}

// Call
class Call extends Branch {
	private String nonTerminal;

	public Call(Expression e, Instruction next, Instruction jump, String nonTerminal) {
		super(Moz.Call, e, next);
		this.jump = jump;
		this.nonTerminal = nonTerminal;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeJump(this.jump);
		bc.encodeNonTerminal(this.nonTerminal);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatJump(sb, this.jump);

		this.formatNonTerminal(sb, this.nonTerminal);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.ref = this.jump;
		return this.next;
	}

}

// Ret
class Ret extends MozInst {
	public Ret(Expression e, Instruction next) {
		super(Moz.Ret, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		return (Instruction) s.ref;
	}
}

// Pos
class Pos extends MozInst {
	public Pos(Expression e, Instruction next) {
		super(Moz.Pos, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getPosition();
		return this.next;
	}

}

// Back
class Back extends MozInst {
	public Back(Expression e, Instruction next) {
		super(Moz.Back, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.setPosition(s.value);
		return this.next;
	}

}

// Skip
class Skip extends MozInst {
	public Skip(Expression e, Instruction next) {
		super(Moz.Skip, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		return sc.skip(this.next);
	}

}

// Byte
class Byte extends MozInst {
	private int byteChar;

	public Byte(Expression e, Instruction next, int byteChar) {
		super(Moz.Byte, e, next);
		this.byteChar = byteChar;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeByte(this.byteChar);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatByte(sb, this.byteChar);
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

// Any
class Any extends MozInst {
	public Any(Expression e, Instruction next) {
		super(Moz.Any, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
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

// Str
class Str extends MozInst {
	private byte[] utf8;

	public Str(Expression e, Instruction next, byte[] utf8) {
		super(Moz.Str, e, next);
		this.utf8 = utf8;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBstr(this.utf8);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBstr(sb, this.utf8);
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

// Set
class Set extends MozInst {
	private boolean[] byteMap;

	public Set(Expression e, Instruction next, boolean[] byteMap) {
		super(Moz.Set, e, next);
		this.byteMap = byteMap;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBset(this.byteMap);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBset(sb, this.byteMap);
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

// NByte
class NByte extends MozInst {
	private int byteChar;

	public NByte(Expression e, Instruction next, int byteChar) {
		super(Moz.NByte, e, next);
		this.byteChar = byteChar;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeByte(this.byteChar);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatByte(sb, this.byteChar);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.byteAt(sc.getPosition()) != this.byteChar) {
			return this.next;
		}
		return sc.fail();
	}

}

// NAny
class NAny extends MozInst {
	public NAny(Expression e, Instruction next) {
		super(Moz.NAny, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.hasUnconsumed()) {
			return sc.fail();
		}
		return next;
	}

}

// NStr
class NStr extends MozInst {
	private byte[] utf8;

	public NStr(Expression e, Instruction next, byte[] utf8) {
		super(Moz.NStr, e, next);
		this.utf8 = utf8;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBstr(this.utf8);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBstr(sb, this.utf8);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (!sc.match(sc.getPosition(), this.utf8)) {
			return this.next;
		}
		return sc.fail();
	}

}

// NSet
class NSet extends MozInst {
	private boolean[] byteMap;

	public NSet(Expression e, Instruction next, boolean[] byteMap) {
		super(Moz.NSet, e, next);
		this.byteMap = byteMap;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBset(this.byteMap);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBset(sb, this.byteMap);
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

// OByte
class OByte extends MozInst {
	private int byteChar;

	public OByte(Expression e, Instruction next, int byteChar) {
		super(Moz.OByte, e, next);
		this.byteChar = byteChar;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeByte(this.byteChar);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatByte(sb, this.byteChar);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
		}
		return this.next;
	}

}

// OAny
class OAny extends MozInst {
	public OAny(Expression e, Instruction next) {
		super(Moz.OAny, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}
}

// OStr
class OStr extends MozInst {
	private byte[] utf8;

	public OStr(Expression e, Instruction next, byte[] utf8) {
		super(Moz.OStr, e, next);
		this.utf8 = utf8;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBstr(this.utf8);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBstr(sb, this.utf8);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
		}
		return this.next;
	}

}

// OSet
class OSet extends MozInst {
	private boolean[] byteMap;

	public OSet(Expression e, Instruction next, boolean[] byteMap) {
		super(Moz.OSet, e, next);
		this.byteMap = byteMap;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBset(this.byteMap);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBset(sb, this.byteMap);
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

// RByte
class RByte extends MozInst {
	private int byteChar;

	public RByte(Expression e, Instruction next, int byteChar) {
		super(Moz.RByte, e, next);
		this.byteChar = byteChar;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeByte(this.byteChar);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatByte(sb, this.byteChar);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		while (sc.byteAt(sc.getPosition()) == this.byteChar) {
			sc.consume(1);
		}
		return this.next;
	}

}

// RAny
class RAny extends MozInst {
	public RAny(Expression e, Instruction next) {
		super(Moz.RAny, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}
}

// RStr
class RStr extends MozInst {
	private byte[] utf8;

	public RStr(Expression e, Instruction next, byte[] utf8) {
		super(Moz.RStr, e, next);
		this.utf8 = utf8;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBstr(this.utf8);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBstr(sb, this.utf8);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		while (sc.match(sc.getPosition(), this.utf8)) {
			sc.consume(utf8.length);
		}
		return this.next;
	}

}

// RSet
class RSet extends MozInst {
	private boolean[] byteMap;

	public RSet(Expression e, Instruction next, boolean[] byteMap) {
		super(Moz.RSet, e, next);
		this.byteMap = byteMap;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBset(this.byteMap);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBset(sb, this.byteMap);
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

// Consume
class Consume extends MozInst {
	private int shift;

	public Consume(Expression e, Instruction next, int shift) {
		super(Moz.Consume, e, next);
		this.shift = shift;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeShift(this.shift);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatShift(sb, this.shift);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.consume(this.shift);
		return this.next;
	}

}

// First
class First extends BranchTable {

	public First(Expression e, Instruction next, Instruction[] jumpTable) {
		super(Moz.First, e, next);
		this.jumpTable = jumpTable;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeJumpTable(this.jumpTable);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatJumpTable(sb, this.jumpTable);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int ch = sc.byteAt(sc.getPosition());
		return jumpTable[ch].exec(sc);
	}

}

// Lookup
class Lookup extends Branch {
	private int memoPoint;
	private boolean state;

	public Lookup(Expression e, Instruction next, boolean state, int memoPoint, Instruction jump) {
		super(Moz.Lookup, e, next);
		this.jump = jump;
		this.memoPoint = memoPoint;
		this.state = state;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeState(this.state);
		bc.encodeMemoPoint(this.memoPoint);
		bc.encodeJump(this.jump);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatState(sb, this.state);
		this.formatMemoPoint(sb, this.memoPoint);
		this.formatJump(sb, this.jump);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		MemoEntry entry = sc.getMemo(this.memoPoint, state);
		if (entry != null) {
			if (entry.failed) {
				// memoPoint.failHit();
				return sc.fail();
			}
			// memoPoint.memoHit(entry.consumed);
			sc.consume(entry.consumed);
			return this.jump;
		}
		// memoPoint.miss();
		return this.next;
	}

}

// Memo
class Memo extends MozInst {
	private int memoPoint;
	private boolean state;

	public Memo(Expression e, Instruction next, boolean state, int memoPoint) {
		super(Moz.Memo, e, next);
		this.memoPoint = memoPoint;
		this.state = state;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeState(this.state);
		bc.encodeMemoPoint(this.memoPoint);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatState(sb, this.state);
		this.formatMemoPoint(sb, this.memoPoint);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		long ppos = sc.popAlt();
		int length = (int) (sc.getPosition() - ppos);
		sc.setMemo(ppos, this.memoPoint, false, null, length, this.state);
		return this.next;
	}

}

// MemoFail
class MemoFail extends MozInst {
	private int memoPoint;
	private boolean state;

	public MemoFail(Expression e, Instruction next, boolean state, int memoPoint) {
		super(Moz.MemoFail, e, next);
		this.memoPoint = memoPoint;
		this.state = state;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeState(this.state);
		bc.encodeMemoPoint(this.memoPoint);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatState(sb, this.state);
		this.formatMemoPoint(sb, this.memoPoint);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		sc.setMemo(sc.getPosition(), memoPoint, true, null, 0, state);
		return sc.fail();
	}

}

// TPush
class TPush extends MozInst {
	public TPush(Expression e, Instruction next) {
		super(Moz.TPush, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPush();
		return this.next;
	}

}

// TPop
class TPop extends MozInst {
	private Symbol label;

	public TPop(Expression e, Instruction next, Symbol label) {
		super(Moz.TPop, e, next);
		this.label = label;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeLabel(this.label);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatLabel(sb, this.label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPop(label);
		return this.next;
	}

}

// TLeftFold
class TLeftFold extends MozInst {
	private int shift;
	private Symbol label;

	public TLeftFold(Expression e, Instruction next, int shift, Symbol label) {
		super(Moz.TLeftFold, e, next);
		this.shift = shift;
		this.label = label;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeShift(this.shift);
		bc.encodeLabel(this.label);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatShift(sb, this.shift);

		this.formatLabel(sb, this.label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logLeftFold(sc.getPosition() + shift, this.label);
		return this.next;
	}

}

// TNew
class TNew extends MozInst {
	private int shift;

	public TNew(Expression e, Instruction next, int shift) {
		super(Moz.TNew, e, next);
		this.shift = shift;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeShift(this.shift);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatShift(sb, this.shift);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logNew(sc.getPosition() + shift, this.id);
		return this.next;
	}

}

// TCapture
class TCapture extends MozInst {
	private int shift;

	public TCapture(Expression e, Instruction next, int shift) {
		super(Moz.TCapture, e, next);
		this.shift = shift;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeShift(this.shift);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatShift(sb, this.shift);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logCapture(sc.getPosition() + shift);
		return this.next;
	}

}

// TTag
class TTag extends MozInst {
	private Symbol tag;

	public TTag(Expression e, Instruction next, Symbol tag) {
		super(Moz.TTag, e, next);
		this.tag = tag;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTag(this.tag);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTag(sb, this.tag);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logTag(tag);
		return this.next;
	}

}

// TReplace
class TReplace extends MozInst {
	private byte[] utf8;
	String value;

	public TReplace(Expression e, Instruction next, byte[] utf8) {
		super(Moz.TReplace, e, next);
		this.utf8 = utf8;
		this.value = new String(utf8); //
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeBstr(this.utf8);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatBstr(sb, this.utf8);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logReplace(this.value);
		return this.next;
	}

}

// TStart
class TStart extends MozInst {
	public TStart(Expression e, Instruction next) {
		super(Moz.TStart, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		ASTMachine astMachine = sc.getAstMachine();
		s.ref = astMachine.saveTransactionPoint();
		return this.next;
	}

}

// TCommit
class TCommit extends MozInst {
	private Symbol label;

	public TCommit(Expression e, Instruction next, Symbol label) {
		super(Moz.TCommit, e, next);
		this.label = label;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeLabel(this.label);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatLabel(sb, this.label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.commitTransactionPoint(label, s.ref);
		return this.next;
	}

}

// TAbort
class TAbort extends MozInst {
	public TAbort(Expression e, Instruction next) {
		super(Moz.TAbort, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}
}

// TLookup
class TLookup extends Branch {
	private int memoPoint;
	private boolean state;
	private Symbol label;

	public TLookup(Expression e, Instruction next, boolean state, int memoPoint, Instruction jump, Symbol label) {
		super(Moz.TLookup, e, next);
		this.jump = jump;
		this.memoPoint = memoPoint;
		this.state = state;
		this.label = label;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeState(this.state);
		bc.encodeMemoPoint(this.memoPoint);
		bc.encodeJump(this.jump);
		bc.encodeLabel(this.label);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatState(sb, this.state);
		this.formatMemoPoint(sb, this.memoPoint);
		this.formatJump(sb, this.jump);
		this.formatLabel(sb, this.label);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		MemoEntry entry = sc.getMemo(memoPoint, state);
		if (entry != null) {
			if (entry.failed) {
				// memoPoint.failHit();
				return sc.fail();
			}
			// memoPoint.memoHit(entry.consumed);
			sc.consume(entry.consumed);
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logLink(label, entry.result);
			return this.jump;
		}
		// memoPoint.miss();
		return this.next;
	}

}

// TMemo
class TMemo extends MozInst {
	private int memoPoint;
	private boolean state;

	public TMemo(Expression e, Instruction next, boolean state, int memoPoint) {
		super(Moz.TMemo, e, next);
		this.memoPoint = memoPoint;
		this.state = state;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeState(this.state);
		bc.encodeMemoPoint(this.memoPoint);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatState(sb, this.state);
		this.formatMemoPoint(sb, this.memoPoint);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		long ppos = sc.popAlt();
		int length = (int) (sc.getPosition() - ppos);
		sc.setMemo(ppos, memoPoint, false, astMachine.getLatestLinkedNode(), length, this.state);
		return this.next;
	}

}

// SOpen
class SOpen extends MozInst {
	public SOpen(Expression e, Instruction next) {
		super(Moz.SOpen, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getSymbolTable().savePoint();
		return this.next;
	}

}

// SClose
class SClose extends MozInst {
	public SClose(Expression e, Instruction next) {
		super(Moz.SClose, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.getSymbolTable().rollBack((int) s.value);
		return this.next;
	}

}

// SMask
class SMask extends MozInst {
	private Symbol table;

	public SMask(Expression e, Instruction next, Symbol table) {
		super(Moz.SMask, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		SymbolTable st = sc.getSymbolTable();
		s.value = st.savePoint();
		st.addSymbolMask(table);
		return this.next;
	}

}

// SDef
class SDef extends MozInst {
	private Symbol table;

	public SDef(Expression e, Instruction next, Symbol table) {
		super(Moz.SDef, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData top = sc.popStack();
		byte[] captured = sc.subbyte(top.value, sc.getPosition());
		sc.getSymbolTable().addSymbol(this.table, captured);
		return this.next;
	}

}

// SIsDef
class SIsDef extends MozInst {
	private Symbol table;
	private byte[] utf8;

	public SIsDef(Expression e, Instruction next, Symbol table, byte[] utf8) {
		super(Moz.SIsDef, e, next);
		this.table = table;
		this.utf8 = utf8;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
		bc.encodeBstr(this.utf8);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);

		this.formatBstr(sb, this.utf8);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		if (sc.getSymbolTable().contains(this.table, utf8)) {
			return this.next;
		}
		return sc.fail();
	}

}

// SExists
class SExists extends MozInst {
	private Symbol table;

	public SExists(Expression e, Instruction next, Symbol table) {
		super(Moz.SExists, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(table);
		return t != null ? this.next : sc.fail();
	}

}

// SMatch
class SMatch extends MozInst {
	private Symbol table;

	public SMatch(Expression e, Instruction next, Symbol table) {
		super(Moz.SMatch, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(table);
		if (t != null && sc.match(sc.getPosition(), t)) {
			sc.consume(t.length);
			return this.next;
		}
		return sc.fail();
	}
}

// SIs
class SIs extends MozInst {
	private Symbol table;

	public SIs(Expression e, Instruction next, Symbol table) {
		super(Moz.SIs, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		byte[] symbol = sc.getSymbolTable().getSymbol(table);
		// System.out.println("symbol:" + new String(symbol));
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

// SIsa
class SIsa extends MozInst {
	private Symbol table;

	public SIsa(Expression e, Instruction next, Symbol table) {
		super(Moz.SIsa, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		StackData s = sc.popStack();
		byte[] captured = sc.subbyte(s.value, sc.getPosition());
		if (sc.getSymbolTable().contains(this.table, captured)) {
			// sc.consume(captured.length);
			return this.next;

		}
		return sc.fail();
	}

}

// SDefNum
class SDefNum extends MozInst {
	private Symbol table;

	public SDefNum(Expression e, Instruction next, Symbol table) {
		super(Moz.SDefNum, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}
}

// SCount
class SCount extends MozInst {
	private Symbol table;

	public SCount(Expression e, Instruction next, Symbol table) {
		super(Moz.SCount, e, next);
		this.table = table;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeTable(this.table);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatTable(sb, this.table);
	}
}

// Exit
class Exit extends MozInst {
	boolean state;

	public Exit(Expression e, Instruction next, boolean state) {
		super(Moz.Exit, e, next);
		this.state = state;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		throw new TerminationException(state);
	}

}

// DFirst
class DFirst extends BranchTable {
	private Instruction[] jumpTable;

	public DFirst(Expression e, Instruction next, Instruction[] jumpTable) {
		super(Moz.DFirst, e, next);
		this.jumpTable = jumpTable;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeJumpTable(this.jumpTable);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatJumpTable(sb, this.jumpTable);
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		int ch = sc.byteAt(sc.getPosition());
		sc.consume(1);
		return jumpTable[ch].exec(sc);
	}

}

// Label
class Label extends MozInst {
	String nonTerminal;

	public Label(Expression e, Instruction next, String nonTerminal) {
		super(Moz.Label, e, next);
		this.nonTerminal = nonTerminal;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
		bc.encodeNonTerminal(this.nonTerminal);
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
		this.formatNonTerminal(sb, this.nonTerminal);
	}
}

class Ref extends Instruction {
	public Ref(int id) {
		super((byte) 0, null, null);
		this.id = id;
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// TODO Auto-generated method stub
	}

	@Override
	public Instruction exec(RuntimeContext sc) throws TerminationException {
		// TODO Auto-generated method stub
		return null;
	}
}

class MozLoader {
	UList<Instruction> codeList;
	byte[] buf;
	int pos = 0;
	private int instSize;
	private int memoSize;
	private String[] poolNonTerminal;
	private boolean[][] poolBset;
	private byte[][] poolBstr;
	private Symbol[] poolTag;
	private Symbol[] poolTable;
	boolean debug;

	int read() {
		int ch = buf[pos];
		pos++;
		return ch;
	}

	int uread() {
		int ch = buf[pos] & 0xff;
		pos++;
		return ch;
	}

	public boolean read_b() {
		return read() == 0 ? false : true;
	}

	public int read_i8() {
		return read();
	}

	public int read_u16() {
		// stream.write(0xff & (num >> 8));
		// stream.write(0xff & (num >> 0));
		return uread() << 8 | uread();
	}

	public int read_u24() {
		// stream.write(0xff & (num >> 16));
		// stream.write(0xff & (num >> 8));
		// stream.write(0xff & (num >> 0));
		return uread() << 16 | uread() << 8 | uread();
	}

	public int read_u32() {
		// stream.write(0xff & (num >> 24));
		// stream.write(0xff & (num >> 16));
		// stream.write(0xff & (num >> 8));
		// stream.write(0xff & (num >> 0));
		return (uread() << 24) | (uread() << 16) | (uread() << 8) | uread();
	}

	public byte[] read_utf8() throws IOException {
		int len = read_u16();
		byte[] b = new byte[len];
		for (int i = 0; i < len; i++) {
			b[i] = (byte) read();
		}
		int check = read();
		if (check != 0) {
			throw new IOException("Moz format error");
		}
		return b;
	}

	public String readString() throws IOException {
		return StringUtils.newString(read_utf8());
	}

	public boolean[] read_byteMap() {
		boolean[] b = new boolean[257];
		for (int i = 0; i < 256; i += 32) {
			read_bytemap(b, i);
		}
		return b;
	}

	private void read_bytemap(boolean[] b, int offset) {
		int u = read_u32();
		for (int i = 0; i < 32; i++) {
			int flag = (1 << i);
			if ((u & flag) == flag) {
				b[offset + i] = true;
			}
		}
	}

	//

	private int readByte() {
		return uread();
	}

	private boolean[] readBset() {
		int id = read_u16();
		return poolBset[id];
	}

	private byte[] readBstr() {
		int id = read_u16();
		return poolBstr[id];
	}

	private Symbol readTable() {
		int id = read_u16();
		return poolTable[id];
	}

	private Symbol readTag() {
		int id = read_u16();
		return poolTag[id];
	}

	private Symbol readLabel() {
		int id = read_u16();
		Symbol l = poolTag[id];
		return (l == Symbol.NullSymbol) ? null : l;
	}

	private boolean readState() {
		return this.read_b();
	}

	private int readMemoPoint() {
		int point = read_u32();
		// System.out.println("mr: " + point);
		return point;
	}

	private Instruction[] readJumpTable() {
		Instruction[] table = new Instruction[257];
		for (int i = 0; i < table.length; i++) {
			table[i] = readJump();
		}
		return table;
	}

	private int readShift() {
		return read_i8();
	}

	private String readNonTerminal() {
		int id = read_u16();
		return poolNonTerminal[id];
	}

	private Instruction readJump() {
		int id = read_u24();
		if (!(id < this.instSize)) {
			System.out.println("r: " + id);
		}
		return new Ref(id);
	}

	private void p(String msg) {
		ConsoleUtils.println(msg);
	}

	private void p(String fmt, Object... args) {
		ConsoleUtils.println(String.format(fmt, args));
	}

	public void loadCode(byte[] buf) throws IOException {
		this.buf = buf;
		this.pos = 0;
		if (read() != 'N' || read() != 'E' || read() != 'Z') {
			throw new IOException("Non moz format");
		}
		char c = (char) read();
		p("Version: %c", c);
		this.instSize = read_u16();
		p("InstructionSize: %d", instSize);
		this.memoSize = read_u16();
		p("memoSize: %d", memoSize);
		int jumpTableSize = read_u16();
		p("jumpTableSize: %d", jumpTableSize);

		int pool = read_u16();
		poolNonTerminal = new String[pool];
		p("NonTerminal: %d", pool);
		for (int i = 0; i < pool; i++) {
			poolNonTerminal[i] = readString();
			// p("NonTerminal: %d %s", i, poolNonTerminal[i]);
		}
		pool = read_u16();
		// p("BitmapSetPool: %d", pool);
		poolBset = new boolean[pool][];
		for (int i = 0; i < pool; i++) {
			poolBset[i] = read_byteMap();
		}
		pool = read_u16();
		// p("StringPool: %d", pool);
		poolBstr = new byte[pool][];
		for (int i = 0; i < pool; i++) {
			poolBstr[i] = read_utf8();
		}
		pool = read_u16();
		// p("SymbolPool: %d", pool);
		poolTag = new Symbol[pool];
		for (int i = 0; i < pool; i++) {
			poolTag[i] = Symbol.tag(readString());
		}
		pool = read_u16();
		p("TablePool: %d", pool);
		poolTable = new Symbol[pool];
		for (int i = 0; i < pool; i++) {
			poolTable[i] = Symbol.tag(readString());
		}

		this.codeList = new UList<Instruction>(new Instruction[instSize]);
		for (int i = 0; i < instSize; i++) {
			loadInstruction();
		}
		if (this.pos != this.buf.length) {
			throw new IOException("Moz format error");
		}
		for (int i = 0; i < instSize; i++) {
			Instruction inst = codeList.ArrayValues[i];
			inst.next = rev(codeList.ArrayValues, (Ref) inst.next);
			if (inst instanceof Branch) {
				Branch binst = (Branch) inst;
				binst.jump = rev(codeList.ArrayValues, (Ref) binst.jump);
			}
			if (inst instanceof BranchTable) {
				BranchTable binst = (BranchTable) inst;
				for (int j = 0; j < binst.jumpTable.length; j++) {
					binst.jumpTable[j] = rev(codeList.ArrayValues, (Ref) binst.jumpTable[j]);
				}
			}
			if (inst instanceof Label) {
				p(((Label) inst).nonTerminal);
			} else {
				p(" L%d\t%s", inst.id, inst);
				if (!inst.isIncrementedNext()) {
					p(" \tjump L%d", inst.next.id);
				}
			}
		}

	}

	Instruction rev(Instruction[] code, Ref ref) {
		return code[ref.id];
	}

	private void loadInstruction() {
		int opcode = uread();
		boolean jumpNext = ((opcode & 128) == 128);
		// System.out.println("opcode=" + opcode + ",jump=" + jumpNext + "=> " +
		// (0b1111111 & opcode));
		opcode = 0b1111111 & opcode;

		Instruction inst = newInstruction((byte) opcode);
		inst.id = codeList.size();
		codeList.add(inst);
		if (jumpNext) {
			inst.next = readJump();
		} else {
			inst.next = new Ref(codeList.size());
		}
	}

	Instruction newInstruction(byte opcode) {
		switch (opcode) {
		case Moz.Nop: {
			return new Nop(null, null);
		}
		case Moz.Fail: {
			return new Fail(null, null);
		}
		case Moz.Alt: {
			Instruction jump = this.readJump();
			return new Alt(null, null, jump);
		}
		case Moz.Succ: {
			return new Succ(null, null);
		}
		case Moz.Jump: {
			Instruction jump = this.readJump();
			return new Jump(null, null, jump);
		}
		case Moz.Call: {
			Instruction jump = this.readJump();
			String nonTerminal = this.readNonTerminal();
			return new Call(null, null, jump, nonTerminal);
		}
		case Moz.Ret: {
			return new Ret(null, null);
		}
		case Moz.Pos: {
			return new Pos(null, null);
		}
		case Moz.Back: {
			return new Back(null, null);
		}
		case Moz.Skip: {
			return new Skip(null, null);
		}
		case Moz.Byte: {
			int byteChar = this.readByte();
			return new Byte(null, null, byteChar);
		}
		case Moz.Any: {
			return new Any(null, null);
		}
		case Moz.Str: {
			byte[] utf8 = this.readBstr();
			return new Str(null, null, utf8);
		}
		case Moz.Set: {
			boolean[] byteMap = this.readBset();
			return new Set(null, null, byteMap);
		}
		case Moz.NByte: {
			int byteChar = this.readByte();
			return new NByte(null, null, byteChar);
		}
		case Moz.NAny: {
			return new NAny(null, null);
		}
		case Moz.NStr: {
			byte[] utf8 = this.readBstr();
			return new NStr(null, null, utf8);
		}
		case Moz.NSet: {
			boolean[] byteMap = this.readBset();
			return new NSet(null, null, byteMap);
		}
		case Moz.OByte: {
			int byteChar = this.readByte();
			return new OByte(null, null, byteChar);
		}
		case Moz.OAny: {
			return new OAny(null, null);
		}
		case Moz.OStr: {
			byte[] utf8 = this.readBstr();
			return new OStr(null, null, utf8);
		}
		case Moz.OSet: {
			boolean[] byteMap = this.readBset();
			return new OSet(null, null, byteMap);
		}
		case Moz.RByte: {
			int byteChar = this.readByte();
			return new RByte(null, null, byteChar);
		}
		case Moz.RAny: {
			return new RAny(null, null);
		}
		case Moz.RStr: {
			byte[] utf8 = this.readBstr();
			return new RStr(null, null, utf8);
		}
		case Moz.RSet: {
			boolean[] byteMap = this.readBset();
			return new RSet(null, null, byteMap);
		}
		case Moz.Consume: {
			int shift = this.readShift();
			return new Consume(null, null, shift);
		}
		case Moz.First: {
			Instruction[] jumpTable = this.readJumpTable();
			return new First(null, null, jumpTable);
		}
		case Moz.Lookup: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			Instruction jump = this.readJump();
			return new Lookup(null, null, state, memoPoint, jump);
		}
		case Moz.Memo: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			return new Memo(null, null, state, memoPoint);
		}
		case Moz.MemoFail: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			return new MemoFail(null, null, state, memoPoint);
		}
		case Moz.TPush: {
			return new TPush(null, null);
		}
		case Moz.TPop: {
			Symbol label = this.readLabel();
			return new TPop(null, null, label);
		}
		case Moz.TLeftFold: {
			int shift = this.readShift();
			Symbol label = this.readLabel();
			return new TLeftFold(null, null, shift, label);
		}
		case Moz.TNew: {
			int shift = this.readShift();
			return new TNew(null, null, shift);
		}
		case Moz.TCapture: {
			int shift = this.readShift();
			return new TCapture(null, null, shift);
		}
		case Moz.TTag: {
			Symbol tag = this.readTag();
			return new TTag(null, null, tag);
		}
		case Moz.TReplace: {
			byte[] utf8 = this.readBstr();
			return new TReplace(null, null, utf8);
		}
		case Moz.TStart: {
			return new TStart(null, null);
		}
		case Moz.TCommit: {
			Symbol label = this.readLabel();
			return new TCommit(null, null, label);
		}
		case Moz.TAbort: {
			return new TAbort(null, null);
		}
		case Moz.TLookup: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			Instruction jump = this.readJump();
			Symbol label = this.readLabel();
			return new TLookup(null, null, state, memoPoint, jump, label);
		}
		case Moz.TMemo: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			return new TMemo(null, null, state, memoPoint);
		}
		case Moz.SOpen: {
			return new SOpen(null, null);
		}
		case Moz.SClose: {
			return new SClose(null, null);
		}
		case Moz.SMask: {
			Symbol table = this.readTable();
			return new SMask(null, null, table);
		}
		case Moz.SDef: {
			Symbol table = this.readTable();
			return new SDef(null, null, table);
		}
		case Moz.SIsDef: {
			Symbol table = this.readTable();
			byte[] utf8 = this.readBstr();
			return new SIsDef(null, null, table, utf8);
		}
		case Moz.SExists: {
			Symbol table = this.readTable();
			return new SExists(null, null, table);
		}
		case Moz.SMatch: {
			Symbol table = this.readTable();
			return new SMatch(null, null, table);
		}
		case Moz.SIs: {
			Symbol table = this.readTable();
			return new SIs(null, null, table);
		}
		case Moz.SIsa: {
			Symbol table = this.readTable();
			return new SIsa(null, null, table);
		}
		case Moz.SDefNum: {
			Symbol table = this.readTable();
			return new SDefNum(null, null, table);
		}
		case Moz.SCount: {
			Symbol table = this.readTable();
			return new SCount(null, null, table);
		}
		case Moz.Exit: {
			boolean state = this.readState();
			return new Exit(null, null, state);
		}
		case Moz.DFirst: {
			Instruction[] jumpTable = this.readJumpTable();
			return new DFirst(null, null, jumpTable);
		}
		case 127:
		case Moz.Label: {
			String nonTerminal = this.readNonTerminal();
			return new Label(null, null, nonTerminal);
		}
		}
		return null;
	}
}
