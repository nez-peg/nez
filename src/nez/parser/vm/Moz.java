package nez.parser.vm;

import java.util.Arrays;

import nez.ast.Symbol;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.Nez.SymbolExists;
import nez.lang.Production;
import nez.parser.CoverageProfiler;
import nez.parser.MemoEntry;
import nez.parser.MemoPoint;
import nez.parser.ParserCode.ProductionCode;
import nez.parser.ParserContext;
import nez.parser.SymbolTable;
import nez.parser.TerminationException;
import nez.util.StringUtils;

public class Moz {
	public final static String[][] FT86 = { //
	//
			{ "Nop" }, //
			{ "Label", "name" }, // name is for debug symbol
			{ "Cov", "uid" }, //
			{ "Exit", "state" }, //

			{ "Pos" }, //
			{ "Back" }, //
			{ "Move", "shift" }, //
			{ "Jump", "jump" }, //
			{ "Call", "jump", "name" }, // name is for debug symbol
			{ "Ret" }, //
			{ "Alt", "jump" }, //
			{ "Succ" }, //
			{ "Fail" }, //
			{ "Guard" }, //
			{ "Step" }, //

			// Matching
			{ "Byte", "byteChar" }, //
			{ "Set", "byteSet" }, //
			{ "Str", "utf8" }, //
			{ "Any" }, //

			{ "NByte", "byteChar" }, //
			{ "NSet", "byteSet" }, //
			{ "NStr", "utf8" }, //
			{ "NAny" }, //

			{ "OByte", "byteChar" }, //
			{ "OSet", "byteSet" }, //
			{ "OStr", "utf8" }, //

			{ "RByte", "byteChar" }, //
			{ "RSet", "byteSet" }, //
			{ "RStr", "utf8" }, //

			// Memoization
			{ "Lookup", "jump", "uid" }, //
			{ "Memo", "uid" }, //
			{ "FailMemo", "uid" }, //

			// AST Construction
			{ "PushTree" }, //
			{ "PopTree" }, //
			{ "Init", "shift" }, //
			{ "New", "shift" }, //
			{ "Tag", "tag" }, //
			{ "Value", "value" }, //
			{ "Link", "label" }, //
			{ "Emit", "label" }, //
			{ "LeftFold", "shift", "label" }, //

			// AST Construction (fast)
			{ "Sinit", "shift" }, //
			{ "Snew", "shift", "tag", "value" }, //

			// Symbol instructions
			{ "SOpen" }, //
			{ "SClose" }, //
			{ "SMask", "table" }, //
			{ "SDef", "table" }, //
			{ "SIsDef", "table", "utf8" }, //
			{ "SExists", "table" }, //
			{ "SMatch", "table" }, //
			{ "SIs", "table" }, //
			{ "SIsa", "table" }, //
			{ "SDefNum", "table" }, //
			{ "SCount", "table" }, //

			// DFA instructions
			{ "Dispatch", "jumpTable" }, //
			{ "EDispatch", "jumpTable" }, //

	};

	public static class Label extends MozInst {
		public final String name;

		public Label(String name, MozInst next) {
			super(MozSet.Label, null, next);
			this.name = name;
		}

		@Override
		protected String getOperand() {
			return name;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.encodeNonTerminal(name);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitLabel(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return this.next;
		}

	}

	public static class Exit extends MozInst {
		public final boolean status;

		public Exit(boolean status) {
			super(MozSet.Exit, null, null);
			this.status = status;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.write_b(status);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitExit(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			throw new TerminationException(status);
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			throw new TerminationException(status);
		}
	}

	public static class Pos extends MozInst {
		public Pos(Expression e, MozInst next) {
			super(MozSet.Pos, e, next);
		}

		public Pos(MozInst next) {
			super(MozSet.Pos, null, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitPos(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.newUnusedStack();
			s.value = sc.getPosition();
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xPos();
			return this.next;
		}

	}

	public static class Back extends MozInst {
		public Back(Expression e, MozInst next) {
			super(MozSet.Back, e, next);
		}

		public Back(MozInst next) {
			super(MozSet.Back, null, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitBack(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.popStack();
			sc.setPosition(s.value);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xBack();
			return this.next;
		}

	}

	public static class Move extends MozInst {
		public final int shift;

		public Move(Expression e, int shift, MozInst next) {
			super(MozSet.Consume, e, next);
			this.shift = shift;
		}

		public Move(int shift, MozInst next) {
			super(MozSet.Consume, null, next);
			this.shift = shift;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.encodeShift(shift);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitMove(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			sc.consume(this.shift);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.move(this.shift);
			return this.next;
		}

	}

	public static class Jump extends MozInst {
		public MozInst jump = null;

		public Jump(MozInst jump) {
			super(MozSet.Call, null, jump);
		}

		@Override
		public void visit(MozVisitor v) {
			// v.visitJump(this);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// TODO Auto-generated method stub

		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return this.next;
		}

	}

	public static class Call extends MozInst {
		ProductionCode<MozInst> f;
		String name;
		public MozInst jump = null;

		public Call(ProductionCode<MozInst> f, String name, MozInst next) {
			super(MozSet.Call, null, next);
			this.f = f;
			this.name = name;
		}

		public Call(String name, MozInst jump, MozInst next) {
			super(MozSet.Call, null, jump);
			this.name = name;
			this.jump = next;
		}

		public Call(MozInst jump, String name, MozInst next) {
			super(MozSet.Call, null, jump);
			this.name = name;
			this.jump = next;
		}

		void sync() {
			if (this.jump == null) {
				this.jump = labeling(this.next);
				this.next = labeling(f.getCompiled());
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
			c.encodeJump(this.jump);
			c.encodeNonTerminal(name); // debug information
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitCall(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.newUnusedStack();
			s.ref = this.jump;
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xCall(name, jump);
			return this.next;
		}

	}

	public static class Ret extends MozInst {
		public Ret(Production e) {
			super(MozSet.Ret, e.getExpression(), null);
		}

		public Ret(Expression e) {
			super(MozSet.Ret, e, null);
		}

		public Ret() {
			super(MozSet.Ret, null, null);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitRet(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.popStack();
			return (MozInst) s.ref;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return sc.xRet();
		}

	}

	public static class Fail extends MozInst {
		public Fail(Expression e) {
			super(MozSet.Fail, e, null);
		}

		public Fail() {
			super(MozSet.Fail, null, null);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitFail(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return sc.xFail();
		}

	}

	public static class Alt extends MozInst {
		public final MozInst failjump;

		public Alt(Expression e, MozInst failjump, MozInst next) {
			super(MozSet.Alt, e, next);
			this.failjump = labeling(failjump);
		}

		public Alt(MozInst failjump, MozInst next) {
			super(MozSet.Alt, null, next);
			this.failjump = labeling(failjump);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitAlt(this);
		}

		@Override
		MozInst branch() {
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
		public MozInst exec(MozMachine sc) throws TerminationException {
			sc.pushAlt(this.failjump);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xAlt(failjump);
			return this.next;
		}
	}

	public static class Succ extends MozInst {
		public Succ(Expression e, MozInst next) {
			super(MozSet.Succ, e, next);
		}

		public Succ(MozInst next) {
			super(MozSet.Succ, null, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSucc(this);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			sc.popAlt();
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xSucc();
			return this.next;
		}
	}

	public static class Skip extends MozInst {
		public Skip(Expression e) {
			super(MozSet.Skip, e, null);
		}

		public Skip() {
			super(MozSet.Skip, null, null);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSkip(this);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			return sc.skip(this.next);
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return sc.xSkip(this.next);
		}
	}

	public static class Guard extends MozInst {
		public Guard(Expression e) {
			super(MozSet.Skip, e, null);
		}

		public Guard() {
			super(MozSet.Skip, null, null);
		}

		@Override
		public void visit(MozVisitor v) {
			// v.visitGuard(this);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			return sc.skip(this.next);
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return sc.xSkip(this.next);
		}
	}

	public static class Scanf extends MozInst {
		public final long mask;
		public final int shift;

		public Scanf(long mask, int shift, MozInst next) {
			super(MozSet.Skip, null, next);
			this.mask = mask;
			this.shift = shift;
		}

		@Override
		public void visit(MozVisitor v) {
			// v.visitGuard(this);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			sc.setCount(ppos, mask, shift);
			return next;
		}
	}

	public static class DecCheck extends MozInst {
		public final MozInst jump;

		public DecCheck(MozInst jump, MozInst next) {
			super(MozSet.Skip, null, next);
			this.jump = jump;
		}

		@Override
		public void visit(MozVisitor v) {
			// v.visitGuard(this);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			return this.jump;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return sc.decCount() == 0 ? this.jump : this.next;
		}
	}

	/**
	 * Byte
	 * 
	 * @author kiki
	 *
	 */

	static abstract class AbstractByteInstruction extends MozInst {
		public final int byteChar;

		AbstractByteInstruction(byte bytecode, Nez.Byte e, MozInst next) {
			super(bytecode, e, next);
			this.byteChar = e.byteChar;
		}

		AbstractByteInstruction(byte bytecode, int byteChar, MozInst next) {
			super(bytecode, null, next);
			this.byteChar = byteChar;
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

	public static class Byte extends AbstractByteInstruction {
		public Byte(Nez.Byte e, MozInst next) {
			super(MozSet.Byte, e, next);
		}

		public Byte(int byteChar, MozInst next) {
			super(MozSet.Byte, byteChar, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitByte(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.prefetch() == this.byteChar) {
				sc.consume(1);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			if (sc.read() == this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class NByte extends AbstractByteInstruction {
		public NByte(Nez.Byte e, MozInst next) {
			super(MozSet.NByte, e, next);
		}

		public NByte(int byteChar, MozInst next) {
			super(MozSet.NByte, byteChar, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitNByte(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.prefetch() != this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			if (sc.prefetch() != this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class OByte extends AbstractByteInstruction {
		public OByte(Nez.Byte e, MozInst next) {
			super(MozSet.OByte, e, next);
		}

		public OByte(int byteChar, MozInst next) {
			super(MozSet.OByte, byteChar, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitOByte(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.prefetch() == this.byteChar) {
				sc.consume(1);
			}
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			if (sc.prefetch() == this.byteChar) {
				sc.move(1);
			}
			return this.next;
		}

	}

	public static class RByte extends AbstractByteInstruction {
		public RByte(Nez.Byte e, MozInst next) {
			super(MozSet.RByte, e, next);
		}

		public RByte(int byteChar, MozInst next) {
			super(MozSet.RByte, byteChar, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitRByte(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			while (sc.prefetch() == this.byteChar) {
				sc.consume(1);
			}
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			while (sc.prefetch() == this.byteChar) {
				sc.move(1);
			}
			return this.next;
		}
	}

	static abstract class AbstractAnyInstruction extends MozInst {
		AbstractAnyInstruction(byte opcode, Expression e, MozInst next) {
			super(opcode, e, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}
	}

	public static class Any extends AbstractAnyInstruction {
		public Any(Expression e, MozInst next) {
			super(MozSet.Any, e, next);
		}

		public Any(MozInst next) {
			super(MozSet.Any, null, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitAny(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.hasUnconsumed()) {
				sc.consume(1);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			if (!sc.eof()) {
				sc.move(1);
				return this.next;
			}
			return sc.xFail();
		}
	}

	public static class NAny extends AbstractAnyInstruction {
		public NAny(Expression e, boolean isBinary, MozInst next) {
			super(MozSet.NAny, e, next);
		}

		public NAny(Expression e, MozInst next) {
			super(MozSet.NAny, null, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitNAny(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.hasUnconsumed()) {
				return sc.xFail();
			}
			return next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			if (sc.eof()) {
				return next;
			}
			return sc.xFail();
		}

	}

	static abstract class AbstractSetInstruction extends MozInst {
		public final boolean[] byteMap;

		AbstractSetInstruction(byte opcode, Nez.ByteSet e, MozInst next) {
			super(opcode, e, next);
			this.byteMap = e.byteMap;
			// if (this.byteMap[0]) {
			// this.byteMap[0] = false; // for safety
			// }
		}

		AbstractSetInstruction(byte opcode, boolean[] byteMap, MozInst next) {
			super(opcode, null, next);
			this.byteMap = byteMap;
			// if (this.byteMap[0]) {
			// this.byteMap[0] = false; // for safety
			// }
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

	public static class Set extends AbstractSetInstruction {
		public Set(Nez.ByteSet e, MozInst next) {
			super(MozSet.Set, e, next);
		}

		public Set(boolean[] byteMap, MozInst next) {
			super(MozSet.Set, byteMap, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSet(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteMap[byteChar]) {
				sc.consume(1);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.read();
			if (byteMap[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class OSet extends AbstractSetInstruction {
		public OSet(Nez.ByteSet e, MozInst next) {
			super(MozSet.OSet, e, next);
		}

		public OSet(boolean[] byteMap, MozInst next) {
			super(MozSet.OSet, byteMap, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitOSet(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteMap[byteChar]) {
				sc.consume(1);
			}
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteMap[byteChar]) {
				sc.move(1);
			}
			return this.next;
		}

	}

	public static class NSet extends AbstractSetInstruction {
		public NSet(Nez.ByteSet e, MozInst next) {
			super(MozSet.NSet, e, next);
		}

		public NSet(boolean[] byteMap, MozInst next) {
			super(MozSet.OSet, byteMap, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitNSet(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (!byteMap[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (!byteMap[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class RSet extends AbstractSetInstruction {
		public RSet(Nez.ByteSet e, MozInst next) {
			super(MozSet.RSet, e, next);
		}

		public RSet(boolean[] byteMap, MozInst next) {
			super(MozSet.OSet, byteMap, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitRSet(this);
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

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			while (byteMap[sc.prefetch()]) {
				sc.move(1);
			}
			return this.next;
		}

	}

	static abstract class AbstractStrInstruction extends MozInst {
		final byte[] utf8;

		public AbstractStrInstruction(byte opcode, Nez.MultiByte e, byte[] utf8, MozInst next) {
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

	public static class Str extends AbstractStrInstruction {
		public Str(Nez.MultiByte e, MozInst next) {
			super(MozSet.Str, e, e.byteSeq, next);
		}

		public Str(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitStr(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.match(this.utf8)) {
				sc.consume(utf8.length);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			if (sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class NStr extends AbstractStrInstruction {
		public NStr(Nez.MultiByte e, MozInst next) {
			super(MozSet.NStr, e, e.byteSeq, next);
		}

		public NStr(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitNStr(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (!sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			if (!sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class OStr extends AbstractStrInstruction {
		public OStr(Nez.MultiByte e, MozInst next) {
			super(MozSet.OStr, e, e.byteSeq, next);
		}

		public OStr(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitOStr(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.match(this.utf8)) {
				sc.consume(utf8.length);
			}
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.match(this.utf8);
			return this.next;
		}

	}

	public static class RStr extends AbstractStrInstruction {
		public RStr(Nez.MultiByte e, MozInst next) {
			super(MozSet.RStr, e, e.byteSeq, next);
		}

		public RStr(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitRStr(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			while (sc.match(this.utf8)) {
				sc.consume(utf8.length);
			}
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			while (sc.match(this.utf8)) {
			}
			return this.next;
		}

	}

	public static class First extends MozInst {
		MozInst[] jumpTable;

		public First(byte opcode, Nez.Choice e, MozInst next) {
			super(opcode, e, next);
			jumpTable = new MozInst[257];
			Arrays.fill(jumpTable, next);
		}

		public First(Nez.Choice e, MozInst next) {
			this(MozSet.First, e, next);
		}

		void setJumpTable(int ch, MozInst inst) {
			jumpTable[ch] = MozInst.labeling(inst);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.encodeJumpTable();
			for (int i = 0; i < jumpTable.length; i++) {
				c.encodeJump(jumpTable[i]);
			}
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitFirst(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			int ch = sc.prefetch();
			return jumpTable[ch];
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int ch = sc.prefetch();
			return jumpTable[ch];
		}

	}

	public static class DFirst extends First {
		public DFirst(Nez.Choice e, MozInst next) {
			super(MozSet.DFirst, e, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitDFirst(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			int ch = sc.prefetch();
			sc.consume(1);
			return jumpTable[ch];
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return jumpTable[sc.read()];
		}

	}

	static abstract class AbstractMemoizationInstruction extends MozInst {
		final MemoPoint memoPoint;
		final int memoId;
		final boolean state;
		final MozInst skip;

		AbstractMemoizationInstruction(byte opcode, Expression e, MemoPoint m, boolean state, MozInst next, MozInst skip) {
			super(opcode, e, next);
			this.memoPoint = m;
			this.memoId = m.id;
			this.skip = labeling(skip);
			this.state = state;
		}

		AbstractMemoizationInstruction(byte opcode, Expression e, MemoPoint m, boolean state, MozInst next) {
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

	public static class Lookup extends AbstractMemoizationInstruction {
		public Lookup(Expression e, MemoPoint m, MozInst next, MozInst skip) {
			super(MozSet.Lookup, e, m, m.isStateful(), next, skip);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitLookup(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			MemoEntry entry = sc.getMemo(memoId, state);
			if (entry != null) {
				if (entry.failed) {
					memoPoint.failHit();
					return sc.xFail();
				}
				memoPoint.memoHit(entry.consumed);
				sc.consume(entry.consumed);
				return this.skip;
			}
			memoPoint.miss();
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			switch (sc.lookupMemo(memoId)) {
			case ParserContext.NotFound:
				return this.next;
			case ParserContext.SuccFound:
				return this.skip;
			default:
				return sc.xFail();
			}
		}
	}

	public static class Memo extends AbstractMemoizationInstruction {
		public Memo(Expression e, MemoPoint m, MozInst next) {
			super(MozSet.Memo, e, m, m.isStateful(), next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitMemo(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			long ppos = sc.popAlt();
			int length = (int) (sc.getPosition() - ppos);
			sc.setMemo(ppos, memoId, false, null, length, this.state);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xSuccPos();
			sc.memoSucc(memoId, ppos);
			return this.next;
		}
	}

	public static class MemoFail extends AbstractMemoizationInstruction {
		public MemoFail(Expression e, MemoPoint m) {
			super(MozSet.MemoFail, e, m, m.isStateful(), null);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitMemoFail(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			sc.setMemo(sc.getPosition(), memoId, true, null, 0, state);
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.memoFail(memoId);
			return sc.xFail();
		}

	}

	public static class TLookup extends AbstractMemoizationInstruction {
		public final Symbol label;

		public TLookup(Nez.LinkTree e, MemoPoint m, MozInst next, MozInst skip) {
			super(MozSet.TLookup, e, m, m.isStateful(), next, skip);
			this.label = e.label;
		}

		public TLookup(MemoPoint m, MozInst next, MozInst skip) {
			super(MozSet.TLookup, null, m, m.isStateful(), next, skip);
			this.label = null;
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTLookup(this);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			super.encodeImpl(c);
			c.encodeLabel(label);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			MemoEntry entry = sc.getMemo(memoId, state);
			if (entry != null) {
				if (entry.failed) {
					memoPoint.failHit();
					return sc.xFail();
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

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			switch (sc.lookupTreeMemo(memoId)) {
			case ParserContext.NotFound:
				return this.next;
			case ParserContext.SuccFound:
				return this.skip;
			default:
				return sc.xFail();
			}
		}

	}

	public static class TMemo extends AbstractMemoizationInstruction {
		public TMemo(Expression e, MemoPoint m, MozInst next) {
			super(MozSet.TMemo, e, m, m.isStateful(), next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTMemo(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			long ppos = sc.popAlt();
			int length = (int) (sc.getPosition() - ppos);
			sc.setMemo(ppos, memoId, false, astMachine.getLatestLinkedNode(), length, this.state);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xSuccPos();
			sc.memoTreeSucc(memoId, ppos);
			return this.next;
		}

	}

	// Tree Construction

	public static class TNew extends MozInst {
		public final int shift;

		public TNew(Nez.BeginTree e, MozInst next) {
			super(MozSet.TNew, e, next);
			this.shift = e.shift;
		}

		public TNew(int shift, MozInst next) {
			super(MozSet.TNew, null, next);
			this.shift = shift;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.encodeShift(shift);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTNew(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logNew(sc.getPosition() + shift, this.id);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.beginTree(shift);
			return this.next;
		}
	}

	public static class TLeftFold extends MozInst {
		public final int shift;
		public final Symbol label;

		public TLeftFold(Nez.FoldTree e, MozInst next) {
			super(MozSet.TLeftFold, e, next);
			this.shift = e.shift;
			this.label = e.label;
		}

		public TLeftFold(Symbol label, int shift, MozInst next) {
			super(MozSet.TLeftFold, null, next);
			this.label = label;
			this.shift = shift;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.encodeShift(shift);
			c.encodeLabel(label);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTLeftFold(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logLeftFold(sc.getPosition() + shift, this.label);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.foldTree(shift, label);
			return this.next;
		}
	}

	public static class TCapture extends MozInst {
		public final int shift;
		public final Symbol tag;
		public final String value;

		public TCapture(Nez.EndTree e, MozInst next) {
			super(MozSet.TCapture, e, next);
			this.shift = e.shift;
			this.tag = e.tag;
			this.value = e.value;
		}

		public TCapture(Symbol tag, String value, int shift, MozInst next) {
			super(MozSet.TCapture, null, next);
			this.tag = tag;
			this.value = value;
			this.shift = shift;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.encodeShift(shift);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTCapture(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logCapture(sc.getPosition() + shift);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.endTree(tag, value, shift);
			return this.next;
		}
	}

	public static class TReplace extends MozInst {
		public final String value;

		public TReplace(Nez.Replace e, MozInst next) {
			super(MozSet.TReplace, e, next);
			this.value = e.value;
		}

		public TReplace(String value, MozInst next) {
			super(MozSet.TReplace, null, next);
			this.value = value;
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
		public void visit(MozVisitor v) {
			v.visitTReplace(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logReplace(this.value);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.valueTree(value);
			return this.next;
		}

	}

	public static class TTag extends MozInst {
		public final Symbol tag;

		public TTag(Nez.Tag e, MozInst next) {
			super(MozSet.TTag, e, next);
			this.tag = e.tag;
		}

		public TTag(Symbol tag, MozInst next) {
			super(MozSet.TTag, null, next);
			this.tag = tag;
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
		public void visit(MozVisitor v) {
			v.visitTTag(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logTag(tag);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.tagTree(tag);
			return this.next;
		}

	}

	public static class TPush extends MozInst {
		public TPush(Expression e, MozInst next) {
			super(MozSet.TPush, e, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTPush(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logPush();
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xTPush();
			return this.next;
		}

	}

	public static class TLink extends MozInst {
		public final Symbol label;

		public TLink(Nez.LinkTree e, MozInst next) {
			super(MozSet.TPop, e, next);
			this.label = e.label;
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
		public void visit(MozVisitor v) {
			v.visitTLink(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logPop(label);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xTLink(label);
			return this.next;
		}

	}

	public static class TPop extends MozInst {

		public TPop(Expression e, MozInst next) {
			super(MozSet.TPop, e, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTPop(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			// ASTMachine astMachine = sc.getAstMachine();
			// astMachine.a(label);
			System.out.println("unsupported");
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xTPop();
			return this.next;
		}

	}

	public static class TStart extends MozInst {
		public TStart(Nez.LinkTree e, MozInst next) {
			super(MozSet.TStart, e, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No argument
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTStart(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.newUnusedStack();
			ASTMachine astMachine = sc.getAstMachine();
			s.ref = astMachine.saveTransactionPoint();
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return this.next;
		}

	}

	public static class TCommit extends MozInst {
		public final Symbol label;

		public TCommit(Nez.LinkTree e, MozInst next) {
			super(MozSet.TCommit, e, next);
			this.label = e.label;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			c.encodeLabel(label);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitTCommit(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.popStack();
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.commitTransactionPoint(label, s.ref);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return this.next;
		}

	}

	/* Symbol */

	static abstract class AbstractTableInstruction extends MozInst {
		final Symbol tableName;

		AbstractTableInstruction(byte opcode, Expression e, Symbol tableName, MozInst next) {
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

	public static class SOpen extends MozInst {
		public SOpen(Nez.BlockScope e, MozInst next) {
			super(MozSet.SOpen, e, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// No Arguments
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSOpen(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.newUnusedStack();
			s.value = sc.getSymbolTable().saveSymbolPoint();
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xSOpen();
			return this.next;
		}

	}

	public static class SMask extends AbstractTableInstruction {
		public SMask(Nez.LocalScope e, MozInst next) {
			super(MozSet.SMask, e, e.tableName, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSMask(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.newUnusedStack();
			SymbolTable st = sc.getSymbolTable();
			s.value = st.saveSymbolPoint();
			st.addSymbolMask(tableName);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xSOpen();
			return this.next;
		}

	}

	public static class SClose extends MozInst {
		public SClose(Expression e, MozInst next) {
			super(MozSet.SClose, e, next);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSClose(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.popStack();
			sc.getSymbolTable().backSymbolPoint((int) s.value);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			sc.xSClose();
			return this.next;
		}
	}

	public static class SDef extends AbstractTableInstruction {
		public SDef(Nez.SymbolAction e, MozInst next) {
			super(MozSet.SDef, e, e.tableName, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSDef(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData top = sc.popStack();
			byte[] captured = sc.subbyte(top.value, sc.getPosition());
			// System.out.println("symbol captured: " + new String(captured) +
			// ", @"
			// + this.tableName);
			sc.getSymbolTable().addSymbol(this.tableName, captured);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			sc.addSymbol(tableName, ppos);
			return this.next;
		}
	}

	public static class SExists extends AbstractTableInstruction {
		public SExists(Nez.SymbolExists e, MozInst next) {
			super(MozSet.SExists, e, e.tableName, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSExists(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			byte[] t = sc.getSymbolTable().getSymbol(tableName);
			return t != null ? this.next : sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return sc.exists(tableName) ? this.next : sc.xFail();
		}

	}

	public static class SIsDef extends AbstractTableInstruction {
		byte[] symbol;

		public SIsDef(SymbolExists e, MozInst next) {
			super(MozSet.SIsDef, e, e.tableName, next);
			symbol = StringUtils.toUtf8(e.symbol);
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			super.encodeImpl(c);
			c.encodeBstr(symbol);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSIsDef(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			if (sc.getSymbolTable().contains(this.tableName, symbol)) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			return sc.existsSymbol(tableName, symbol) ? this.next : sc.xFail();
		}
	}

	public static class SMatch extends AbstractTableInstruction {
		public SMatch(Nez.SymbolMatch e, MozInst next) {
			super(MozSet.SMatch, e, e.tableName, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSMatch(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			byte[] t = sc.getSymbolTable().getSymbol(tableName);
			if (t == null) {
				return this.next;
			}
			if (sc.match(t)) {
				sc.consume(t.length);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			byte[] sym = sc.getSymbol(tableName);
			return sc.match(sym) ? this.next : sc.xFail();
		}

	}

	public static class SIs extends AbstractTableInstruction {
		public SIs(Nez.SymbolPredicate e, MozInst next) {
			super(MozSet.SIs, e, e.tableName, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSIs(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			byte[] symbol = sc.getSymbolTable().getSymbol(tableName);
			if (symbol != null) {
				StackData s = sc.popStack();
				byte[] captured = sc.subbyte(s.value, sc.getPosition());
				// System.out.println("captured:" + new String(captured));
				if (symbol.length == captured.length && SymbolTable.equalsBytes(symbol, captured)) {
					// sc.consume(symbol.length);
					return this.next;
				}
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			return sc.equals(tableName, ppos) ? this.next : sc.xFail();
		}

	}

	public static class SIsa extends AbstractTableInstruction {
		public SIsa(Nez.SymbolPredicate e, MozInst next) {
			super(MozSet.SIsa, e, e.tableName, next);
		}

		@Override
		public void visit(MozVisitor v) {
			v.visitSIsa(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			StackData s = sc.popStack();
			byte[] captured = sc.subbyte(s.value, sc.getPosition());
			if (sc.getSymbolTable().contains(this.tableName, captured)) {
				// sc.consume(captured.length);
				return this.next;

			}
			return sc.xFail();
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			return sc.contains(tableName, ppos) ? this.next : sc.xFail();
		}

	}

	public static class Cov extends MozInst {
		final CoverageProfiler prof;
		final int id;
		final boolean start;

		public Cov(CoverageProfiler prof, int covPoint, boolean start, MozInst next) {
			super(MozSet.Cov, null, next);
			this.prof = prof;
			this.id = covPoint;
			this.start = start;
		}

		public Cov(int id, MozInst next) {
			super(MozSet.Cov, null, next);
			this.prof = null;
			this.id = id;
			this.start = true;
		}

		@Override
		protected void encodeImpl(ByteCoder c) {
			// TODO Auto-generated method stub

		}

		@Override
		public void visit(MozVisitor v) {
			v.visitCov(this);
		}

		@Override
		public MozInst exec(MozMachine sc) throws TerminationException {
			// Coverage.enter(this.id);
			return this.next;
		}

		@Override
		public MozInst exec2(ParserMachineContext sc) throws TerminationException {
			prof.countCoverage(this.id, start);
			return this.next;
		}
	}

}
