package nez.parser.moz;

import java.io.IOException;

import nez.ast.ASTMachine;
import nez.ast.Symbol;
import nez.lang.Expression;
import nez.parser.ByteCoder;
import nez.parser.MemoEntry;
import nez.parser.StackData;
import nez.parser.SymbolTable;
import nez.parser.TerminationException;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class Moz2 {
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

abstract class MozInstruction extends MozInst {

	public MozInstruction(byte opcode, Expression e, MozInst next) {
		super(opcode, e, next);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// TODO Auto-generated method stub

	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
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

	protected void formatJump(StringBuilder sb, MozInst a) {
		sb.append(' ');
		sb.append("L" + a.id);
	}

	protected void formatJumpTable(StringBuilder sb, MozInst[] a) {
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

abstract class Branch extends MozInstruction {
	protected MozInst jump;

	public Branch(byte opcode, Expression e, MozInst next) {
		super(opcode, e, next);
	}
}

abstract class BranchTable extends MozInstruction {
	protected MozInst[] jumpTable;

	public BranchTable(byte opcode, Expression e, MozInst next) {
		super(opcode, e, next);
	}
}

// Nop
class Nop extends MozInstruction {
	public Nop(Expression e, MozInst next) {
		super(Moz2.Nop, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

}

// Fail
class Fail extends MozInstruction {
	public Fail(Expression e, MozInst next) {
		super(Moz2.Fail, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		return sc.fail();
	}
}

// Alt
class Alt extends Branch {
	public Alt(Expression e, MozInst next, MozInst jump) {
		super(Moz2.Alt, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		sc.pushAlt(this.jump);
		return this.next;
	}
}

// Succ
class Succ extends MozInstruction {
	public Succ(Expression e, MozInst next) {
		super(Moz2.Succ, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		sc.popAlt();
		return this.next;
	}

}

// Jump
class Jump extends Branch {
	public Jump(Expression e, MozInst next, MozInst jump) {
		super(Moz2.Jump, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		return this.jump;
	}

}

// Call
class Call extends Branch {
	private String nonTerminal;

	public Call(Expression e, MozInst next, MozInst jump, String nonTerminal) {
		super(Moz2.Call, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.ref = this.jump;
		return this.next;
	}

}

// Ret
class Ret extends MozInstruction {
	public Ret(Expression e, MozInst next) {
		super(Moz2.Ret, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.popStack();
		return (MozInst) s.ref;
	}
}

// Pos
class Pos extends MozInstruction {
	public Pos(Expression e, MozInst next) {
		super(Moz2.Pos, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getPosition();
		return this.next;
	}

}

// Back
class Back extends MozInstruction {
	public Back(Expression e, MozInst next) {
		super(Moz2.Back, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.setPosition(s.value);
		return this.next;
	}

}

// Skip
class Skip extends MozInstruction {
	public Skip(Expression e, MozInst next) {
		super(Moz2.Skip, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		return sc.skip(this.next);
	}

}

// Byte
class Byte extends MozInstruction {
	private int byteChar;

	public Byte(Expression e, MozInst next, int byteChar) {
		super(Moz2.Byte, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.prefetch() == this.byteChar) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}

}

// Any
class Any extends MozInstruction {
	public Any(Expression e, MozInst next) {
		super(Moz2.Any, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.hasUnconsumed()) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}

}

// Str
class Str extends MozInstruction {
	private byte[] utf8;

	public Str(Expression e, MozInst next, byte[] utf8) {
		super(Moz2.Str, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.match(this.utf8)) {
			sc.consume(utf8.length);
			return this.next;
		}
		return sc.fail();
	}

}

// Set
class Set extends MozInstruction {
	private boolean[] byteMap;

	public Set(Expression e, MozInst next, boolean[] byteMap) {
		super(Moz2.Set, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		int byteChar = sc.prefetch();
		if (byteMap[byteChar]) {
			sc.consume(1);
			return this.next;
		}
		return sc.fail();
	}

}

// NByte
class NByte extends MozInstruction {
	private int byteChar;

	public NByte(Expression e, MozInst next, int byteChar) {
		super(Moz2.NByte, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.prefetch() != this.byteChar) {
			return this.next;
		}
		return sc.fail();
	}

}

// NAny
class NAny extends MozInstruction {
	public NAny(Expression e, MozInst next) {
		super(Moz2.NAny, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.hasUnconsumed()) {
			return sc.fail();
		}
		return next;
	}

}

// NStr
class NStr extends MozInstruction {
	private byte[] utf8;

	public NStr(Expression e, MozInst next, byte[] utf8) {
		super(Moz2.NStr, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (!sc.match(this.utf8)) {
			return this.next;
		}
		return sc.fail();
	}

}

// NSet
class NSet extends MozInstruction {
	private boolean[] byteMap;

	public NSet(Expression e, MozInst next, boolean[] byteMap) {
		super(Moz2.NSet, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		int byteChar = sc.prefetch();
		if (!byteMap[byteChar]) {
			return this.next;
		}
		return sc.fail();
	}

}

// OByte
class OByte extends MozInstruction {
	private int byteChar;

	public OByte(Expression e, MozInst next, int byteChar) {
		super(Moz2.OByte, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.prefetch() == this.byteChar) {
			sc.consume(1);
		}
		return this.next;
	}

}

// OAny
class OAny extends MozInstruction {
	public OAny(Expression e, MozInst next) {
		super(Moz2.OAny, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}
}

// OStr
class OStr extends MozInstruction {
	private byte[] utf8;

	public OStr(Expression e, MozInst next, byte[] utf8) {
		super(Moz2.OStr, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.match(this.utf8)) {
			sc.consume(utf8.length);
		}
		return this.next;
	}

}

// OSet
class OSet extends MozInstruction {
	private boolean[] byteMap;

	public OSet(Expression e, MozInst next, boolean[] byteMap) {
		super(Moz2.OSet, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		int byteChar = sc.prefetch();
		if (byteMap[byteChar]) {
			sc.consume(1);
		}
		return this.next;
	}

}

// RByte
class RByte extends MozInstruction {
	private int byteChar;

	public RByte(Expression e, MozInst next, int byteChar) {
		super(Moz2.RByte, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		while (sc.prefetch() == this.byteChar) {
			sc.consume(1);
		}
		return this.next;
	}

}

// RAny
class RAny extends MozInstruction {
	public RAny(Expression e, MozInst next) {
		super(Moz2.RAny, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}
}

// RStr
class RStr extends MozInstruction {
	private byte[] utf8;

	public RStr(Expression e, MozInst next, byte[] utf8) {
		super(Moz2.RStr, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		while (sc.match(this.utf8)) {
			sc.consume(utf8.length);
		}
		return this.next;
	}

}

// RSet
class RSet extends MozInstruction {
	private boolean[] byteMap;

	public RSet(Expression e, MozInst next, boolean[] byteMap) {
		super(Moz2.RSet, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		int byteChar = sc.prefetch();
		while (byteMap[byteChar]) {
			sc.consume(1);
			byteChar = sc.prefetch();
		}
		return this.next;
	}

}

// Consume
class Consume extends MozInstruction {
	private int shift;

	public Consume(Expression e, MozInst next, int shift) {
		super(Moz2.Consume, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		sc.consume(this.shift);
		return this.next;
	}

}

// First
class First extends BranchTable {

	public First(Expression e, MozInst next, MozInst[] jumpTable) {
		super(Moz2.First, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		int ch = sc.prefetch();
		return jumpTable[ch].exec(sc);
	}

}

// Lookup
class Lookup extends Branch {
	private int memoPoint;
	private boolean state;

	public Lookup(Expression e, MozInst next, boolean state, int memoPoint, MozInst jump) {
		super(Moz2.Lookup, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
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
class Memo extends MozInstruction {
	private int memoPoint;
	private boolean state;

	public Memo(Expression e, MozInst next, boolean state, int memoPoint) {
		super(Moz2.Memo, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		long ppos = sc.popAlt();
		int length = (int) (sc.getPosition() - ppos);
		sc.setMemo(ppos, this.memoPoint, false, null, length, this.state);
		return this.next;
	}

}

// MemoFail
class MemoFail extends MozInstruction {
	private int memoPoint;
	private boolean state;

	public MemoFail(Expression e, MozInst next, boolean state, int memoPoint) {
		super(Moz2.MemoFail, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		sc.setMemo(sc.getPosition(), memoPoint, true, null, 0, state);
		return sc.fail();
	}

}

// TPush
class TPush extends MozInstruction {
	public TPush(Expression e, MozInst next) {
		super(Moz2.TPush, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPush();
		return this.next;
	}

}

// TPop
class TPop extends MozInstruction {
	private Symbol label;

	public TPop(Expression e, MozInst next, Symbol label) {
		super(Moz2.TPop, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logPop(label);
		return this.next;
	}

}

// TLeftFold
class TLeftFold extends MozInstruction {
	private int shift;
	private Symbol label;

	public TLeftFold(Expression e, MozInst next, int shift, Symbol label) {
		super(Moz2.TLeftFold, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logLeftFold(sc.getPosition() + shift, this.label);
		return this.next;
	}

}

// TNew
class TNew extends MozInstruction {
	private int shift;

	public TNew(Expression e, MozInst next, int shift) {
		super(Moz2.TNew, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logNew(sc.getPosition() + shift, this.id);
		return this.next;
	}

}

// TCapture
class TCapture extends MozInstruction {
	private int shift;

	public TCapture(Expression e, MozInst next, int shift) {
		super(Moz2.TCapture, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logCapture(sc.getPosition() + shift);
		return this.next;
	}

}

// TTag
class TTag extends MozInstruction {
	private Symbol tag;

	public TTag(Expression e, MozInst next, Symbol tag) {
		super(Moz2.TTag, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logTag(tag);
		return this.next;
	}

}

// TReplace
class TReplace extends MozInstruction {
	private byte[] utf8;
	String value;

	public TReplace(Expression e, MozInst next, byte[] utf8) {
		super(Moz2.TReplace, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.logReplace(this.value);
		return this.next;
	}

}

// TStart
class TStart extends MozInstruction {
	public TStart(Expression e, MozInst next) {
		super(Moz2.TStart, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		ASTMachine astMachine = sc.getAstMachine();
		s.ref = astMachine.saveTransactionPoint();
		return this.next;
	}

}

// TCommit
class TCommit extends MozInstruction {
	private Symbol label;

	public TCommit(Expression e, MozInst next, Symbol label) {
		super(Moz2.TCommit, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.popStack();
		ASTMachine astMachine = sc.getAstMachine();
		astMachine.commitTransactionPoint(label, s.ref);
		return this.next;
	}

}

// TAbort
class TAbort extends MozInstruction {
	public TAbort(Expression e, MozInst next) {
		super(Moz2.TAbort, e, next);
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

	public TLookup(Expression e, MozInst next, boolean state, int memoPoint, MozInst jump, Symbol label) {
		super(Moz2.TLookup, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
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
class TMemo extends MozInstruction {
	private int memoPoint;
	private boolean state;

	public TMemo(Expression e, MozInst next, boolean state, int memoPoint) {
		super(Moz2.TMemo, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		ASTMachine astMachine = sc.getAstMachine();
		long ppos = sc.popAlt();
		int length = (int) (sc.getPosition() - ppos);
		sc.setMemo(ppos, memoPoint, false, astMachine.getLatestLinkedNode(), length, this.state);
		return this.next;
	}

}

// SOpen
class SOpen extends MozInstruction {
	public SOpen(Expression e, MozInst next) {
		super(Moz2.SOpen, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		s.value = sc.getSymbolTable().savePoint();
		return this.next;
	}

}

// SClose
class SClose extends MozInstruction {
	public SClose(Expression e, MozInst next) {
		super(Moz2.SClose, e, next);
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.popStack();
		sc.getSymbolTable().rollBack((int) s.value);
		return this.next;
	}

}

// SMask
class SMask extends MozInstruction {
	private Symbol table;

	public SMask(Expression e, MozInst next, Symbol table) {
		super(Moz2.SMask, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData s = sc.newUnusedStack();
		SymbolTable st = sc.getSymbolTable();
		s.value = st.savePoint();
		st.addSymbolMask(table);
		return this.next;
	}

}

// SDef
class SDef extends MozInstruction {
	private Symbol table;

	public SDef(Expression e, MozInst next, Symbol table) {
		super(Moz2.SDef, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		StackData top = sc.popStack();
		byte[] captured = sc.subbyte(top.value, sc.getPosition());
		sc.getSymbolTable().addSymbol(this.table, captured);
		return this.next;
	}

}

// SIsDef
class SIsDef extends MozInstruction {
	private Symbol table;
	private byte[] utf8;

	public SIsDef(Expression e, MozInst next, Symbol table, byte[] utf8) {
		super(Moz2.SIsDef, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		if (sc.getSymbolTable().contains(this.table, utf8)) {
			return this.next;
		}
		return sc.fail();
	}

}

// SExists
class SExists extends MozInstruction {
	private Symbol table;

	public SExists(Expression e, MozInst next, Symbol table) {
		super(Moz2.SExists, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(table);
		return t != null ? this.next : sc.fail();
	}

}

// SMatch
class SMatch extends MozInstruction {
	private Symbol table;

	public SMatch(Expression e, MozInst next, Symbol table) {
		super(Moz2.SMatch, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		byte[] t = sc.getSymbolTable().getSymbol(table);
		if (t == null) {
			return this.next;
		}
		if (sc.match(t)) {
			sc.consume(t.length);
			return this.next;
		}
		return sc.fail();
	}
}

// SIs
class SIs extends MozInstruction {
	private Symbol table;

	public SIs(Expression e, MozInst next, Symbol table) {
		super(Moz2.SIs, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
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
class SIsa extends MozInstruction {
	private Symbol table;

	public SIsa(Expression e, MozInst next, Symbol table) {
		super(Moz2.SIsa, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
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
class SDefNum extends MozInstruction {
	private Symbol table;

	public SDefNum(Expression e, MozInst next, Symbol table) {
		super(Moz2.SDefNum, e, next);
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
class SCount extends MozInstruction {
	private Symbol table;

	public SCount(Expression e, MozInst next, Symbol table) {
		super(Moz2.SCount, e, next);
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
class Exit extends MozInstruction {
	boolean state;

	public Exit(Expression e, MozInst next, boolean state) {
		super(Moz2.Exit, e, next);
		this.state = state;
	}

	@Override
	protected void encodeImpl(ByteCoder bc) {
	}

	@Override
	protected void formatImpl(StringBuilder sb) {
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		throw new TerminationException(state);
	}

}

// DFirst
class DFirst extends BranchTable {
	private MozInst[] jumpTable;

	public DFirst(Expression e, MozInst next, MozInst[] jumpTable) {
		super(Moz2.DFirst, e, next);
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
	public MozInst exec(MozMachine sc) throws TerminationException {
		int ch = sc.prefetch();
		sc.consume(1);
		return jumpTable[ch].exec(sc);
	}

}

// Label
class Label extends MozInstruction {
	String nonTerminal;

	public Label(Expression e, MozInst next, String nonTerminal) {
		super(Moz2.Label, e, next);
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

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		return next;
	}
}

class Ref extends MozInst {
	public Ref(int id) {
		super((byte) 0, null, null);
		this.id = id;
	}

	@Override
	protected void encodeImpl(ByteCoder c) {
		// TODO Auto-generated method stub
	}

	@Override
	public MozInst exec(MozMachine sc) throws TerminationException {
		// TODO Auto-generated method stub
		return null;
	}
}

class MozLoader {
	UList<MozInst> codeList;
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

	private MozInst[] readJumpTable() {
		MozInst[] table = new MozInst[257];
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

	private MozInst readJump() {
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

		this.codeList = new UList<MozInst>(new MozInst[instSize]);
		for (int i = 0; i < instSize; i++) {
			loadInstruction();
		}
		if (this.pos != this.buf.length) {
			throw new IOException("Moz format error");
		}
		for (int i = 0; i < instSize; i++) {
			MozInst inst = codeList.ArrayValues[i];
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

	MozInst rev(MozInst[] code, Ref ref) {
		return code[ref.id];
	}

	private void loadInstruction() {
		int opcode = uread();
		boolean jumpNext = ((opcode & 128) == 128);
		// System.out.println("opcode=" + opcode + ",jump=" + jumpNext + "=> " +
		// (0b1111111 & opcode));
		opcode = 0b1111111 & opcode;

		MozInst inst = newInstruction((byte) opcode);
		inst.id = codeList.size();
		codeList.add(inst);
		if (jumpNext) {
			inst.next = readJump();
		} else {
			inst.next = new Ref(codeList.size());
		}
	}

	MozInst newInstruction(byte opcode) {
		switch (opcode) {
		case Moz2.Nop: {
			return new Nop(null, null);
		}
		case Moz2.Fail: {
			return new Fail(null, null);
		}
		case Moz2.Alt: {
			MozInst jump = this.readJump();
			return new Alt(null, null, jump);
		}
		case Moz2.Succ: {
			return new Succ(null, null);
		}
		case Moz2.Jump: {
			MozInst jump = this.readJump();
			return new Jump(null, null, jump);
		}
		case Moz2.Call: {
			MozInst jump = this.readJump();
			String nonTerminal = this.readNonTerminal();
			return new Call(null, null, jump, nonTerminal);
		}
		case Moz2.Ret: {
			return new Ret(null, null);
		}
		case Moz2.Pos: {
			return new Pos(null, null);
		}
		case Moz2.Back: {
			return new Back(null, null);
		}
		case Moz2.Skip: {
			return new Skip(null, null);
		}
		case Moz2.Byte: {
			int byteChar = this.readByte();
			return new Byte(null, null, byteChar);
		}
		case Moz2.Any: {
			return new Any(null, null);
		}
		case Moz2.Str: {
			byte[] utf8 = this.readBstr();
			return new Str(null, null, utf8);
		}
		case Moz2.Set: {
			boolean[] byteMap = this.readBset();
			return new Set(null, null, byteMap);
		}
		case Moz2.NByte: {
			int byteChar = this.readByte();
			return new NByte(null, null, byteChar);
		}
		case Moz2.NAny: {
			return new NAny(null, null);
		}
		case Moz2.NStr: {
			byte[] utf8 = this.readBstr();
			return new NStr(null, null, utf8);
		}
		case Moz2.NSet: {
			boolean[] byteMap = this.readBset();
			return new NSet(null, null, byteMap);
		}
		case Moz2.OByte: {
			int byteChar = this.readByte();
			return new OByte(null, null, byteChar);
		}
		case Moz2.OAny: {
			return new OAny(null, null);
		}
		case Moz2.OStr: {
			byte[] utf8 = this.readBstr();
			return new OStr(null, null, utf8);
		}
		case Moz2.OSet: {
			boolean[] byteMap = this.readBset();
			return new OSet(null, null, byteMap);
		}
		case Moz2.RByte: {
			int byteChar = this.readByte();
			return new RByte(null, null, byteChar);
		}
		case Moz2.RAny: {
			return new RAny(null, null);
		}
		case Moz2.RStr: {
			byte[] utf8 = this.readBstr();
			return new RStr(null, null, utf8);
		}
		case Moz2.RSet: {
			boolean[] byteMap = this.readBset();
			return new RSet(null, null, byteMap);
		}
		case Moz2.Consume: {
			int shift = this.readShift();
			return new Consume(null, null, shift);
		}
		case Moz2.First: {
			MozInst[] jumpTable = this.readJumpTable();
			return new First(null, null, jumpTable);
		}
		case Moz2.Lookup: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			MozInst jump = this.readJump();
			return new Lookup(null, null, state, memoPoint, jump);
		}
		case Moz2.Memo: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			return new Memo(null, null, state, memoPoint);
		}
		case Moz2.MemoFail: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			return new MemoFail(null, null, state, memoPoint);
		}
		case Moz2.TPush: {
			return new TPush(null, null);
		}
		case Moz2.TPop: {
			Symbol label = this.readLabel();
			return new TPop(null, null, label);
		}
		case Moz2.TLeftFold: {
			int shift = this.readShift();
			Symbol label = this.readLabel();
			return new TLeftFold(null, null, shift, label);
		}
		case Moz2.TNew: {
			int shift = this.readShift();
			return new TNew(null, null, shift);
		}
		case Moz2.TCapture: {
			int shift = this.readShift();
			return new TCapture(null, null, shift);
		}
		case Moz2.TTag: {
			Symbol tag = this.readTag();
			return new TTag(null, null, tag);
		}
		case Moz2.TReplace: {
			byte[] utf8 = this.readBstr();
			return new TReplace(null, null, utf8);
		}
		case Moz2.TStart: {
			return new TStart(null, null);
		}
		case Moz2.TCommit: {
			Symbol label = this.readLabel();
			return new TCommit(null, null, label);
		}
		case Moz2.TAbort: {
			return new TAbort(null, null);
		}
		case Moz2.TLookup: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			MozInst jump = this.readJump();
			Symbol label = this.readLabel();
			return new TLookup(null, null, state, memoPoint, jump, label);
		}
		case Moz2.TMemo: {
			boolean state = this.readState();
			int memoPoint = this.readMemoPoint();
			return new TMemo(null, null, state, memoPoint);
		}
		case Moz2.SOpen: {
			return new SOpen(null, null);
		}
		case Moz2.SClose: {
			return new SClose(null, null);
		}
		case Moz2.SMask: {
			Symbol table = this.readTable();
			return new SMask(null, null, table);
		}
		case Moz2.SDef: {
			Symbol table = this.readTable();
			return new SDef(null, null, table);
		}
		case Moz2.SIsDef: {
			Symbol table = this.readTable();
			byte[] utf8 = this.readBstr();
			return new SIsDef(null, null, table, utf8);
		}
		case Moz2.SExists: {
			Symbol table = this.readTable();
			return new SExists(null, null, table);
		}
		case Moz2.SMatch: {
			Symbol table = this.readTable();
			return new SMatch(null, null, table);
		}
		case Moz2.SIs: {
			Symbol table = this.readTable();
			return new SIs(null, null, table);
		}
		case Moz2.SIsa: {
			Symbol table = this.readTable();
			return new SIsa(null, null, table);
		}
		case Moz2.SDefNum: {
			Symbol table = this.readTable();
			return new SDefNum(null, null, table);
		}
		case Moz2.SCount: {
			Symbol table = this.readTable();
			return new SCount(null, null, table);
		}
		case Moz2.Exit: {
			boolean state = this.readState();
			return new Exit(null, null, state);
		}
		case Moz2.DFirst: {
			MozInst[] jumpTable = this.readJumpTable();
			return new DFirst(null, null, jumpTable);
		}
		case 127:
		case Moz2.Label: {
			String nonTerminal = this.readNonTerminal();
			return new Label(null, null, nonTerminal);
		}
		}
		return null;
	}
}
