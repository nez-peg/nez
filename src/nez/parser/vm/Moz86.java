package nez.parser.vm;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;

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
import nez.parser.TerminationException;
import nez.parser.vm.MozMachine.MozStackData;
import nez.util.StringUtils;

public class Moz86 {
	public final static String[][] Specification = { //
	//
			{ "Nop", "name" }, // name is for debug symbol
			{ "Exit", "state" }, //
			{ "Cov", "uid", "state" }, //

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

			// DFA instructions
			{ "Dispatch", "jumpIndex", "jumpTable" }, //
			{ "DDispatch", "jumpIndex", "jumpTable" }, //

			// AST Construction
			{ "TPush" }, //
			{ "TPop" }, //
			{ "TBegin", "shift" }, //
			{ "TEnd", "shift", "tag", "value" }, //
			{ "TTag", "tag" }, //
			{ "TReplace", "value" }, //
			{ "TLink", "label" }, //
			{ "TFold", "shift", "label" }, //
			{ "TEmit", "label" }, //

			// Symbol instructions
			{ "SOpen" }, //
			{ "SClose" }, //
			{ "SMask", "table" }, //
			{ "SDef", "table" }, //
			{ "SExists", "table" }, //
			{ "SIsDef", "table", "utf8" }, //
			{ "SMatch", "table" }, //
			{ "SIs", "table" }, //
			{ "SIsa", "table" }, //

			// Read N, Repeat N
			{ "NScan", "mask", "shift" }, //
			{ "NDec", "jump" }, //

			// Memoization
			{ "Lookup", "jump", "uid" }, //
			{ "Memo", "uid" }, //
			{ "FailMemo", "uid" }, //
			{ "TLookup", "jump", "uid" }, //
			{ "TMemo", "uid" }, //
	};

	static HashMap<String, String[]> specMap = new HashMap<>();
	static HashMap<String, java.lang.Byte> opcodeMap = new HashMap<>();
	static {
		for (String[] insts : Specification) {
			opcodeMap.put(insts[0], (byte) opcodeMap.size());
			specMap.put(insts[0], insts);

		}
	}

	static byte opCode(String name) {
		return opcodeMap.get(name);
	}

	static int opSize(String name) {
		String[] insts = specMap.get(name);
		return insts.length - 1;
	}

	static Object opValue(MozInst inst, int p) {
		String[] insts = specMap.get(inst.getName());
		try {
			Field f = inst.getClass().getField(insts[p + 1]);
			return f.get(inst);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	static void stringfy(MozInst inst, StringBuilder sb) {
		sb.append(inst.getName());
		for (int i = 0; i < opSize(inst.getName()); i++) {
			sb.append(" ");
			sb.append(opValue(inst, i));
		}
	}

	protected static MozInst joinPoint(MozInst inst) {
		if (inst != null) {
			inst.joinPoint = true;
		}
		return inst;
	}

	public final static class Nop extends MozInst {
		public final String name;

		public Nop(String name, MozInst next) {
			super(MozSet.Label, null, next);
			this.name = name;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitNop(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return this.next;
		}

	}

	public final static class Exit extends MozInst {
		public final boolean status;

		public Exit(boolean status) {
			super(MozSet.Exit, null, null);
			this.status = status;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitExit(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			throw new TerminationException(status);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			throw new TerminationException(status);
		}
	}

	public final static class Cov extends MozInst {
		final CoverageProfiler prof;
		public final int id;
		public final boolean start;

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
		public void visit(InstructionVisitor v) {
			v.visitCov(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			// Coverage.enter(this.id);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			prof.countCoverage(this.id, start);
			return this.next;
		}
	}

	public final static class Pos extends MozInst {
		public Pos(Expression e, MozInst next) {
			super(MozSet.Pos, e, next);
		}

		public Pos(MozInst next) {
			super(MozSet.Pos, null, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitPos(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.newUnusedStack();
			s.value = sc.getPosition();
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xPos();
			return this.next;
		}

	}

	public final static class Back extends MozInst {
		public Back(Expression e, MozInst next) {
			super(MozSet.Back, e, next);
		}

		public Back(MozInst next) {
			super(MozSet.Back, null, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitBack(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.popStack();
			sc.setPosition(s.value);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xBack();
			return this.next;
		}

	}

	public final static class Move extends MozInst {
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
		public void visit(InstructionVisitor v) {
			v.visitMove(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			sc.consume(this.shift);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.move(this.shift);
			return this.next;
		}

	}

	public final static class Jump extends MozInst {
		public MozInst jump = null;

		public Jump(MozInst jump) {
			super(MozSet.Call, null, null);
			this.jump = jump;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitJump(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			return this.jump;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return this.jump;
		}

	}

	public final static class Call extends MozInst {
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
				this.jump = joinPoint(this.next);
				this.next = joinPoint(f.getCompiled());
			}
			this.f = null;
		}

		public final String getNonTerminalName() {
			return this.name;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitCall(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.newUnusedStack();
			s.ref = this.jump;
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xCall(name, jump);
			return this.next;
		}

	}

	public final static class Ret extends MozInst {
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
		public void visit(InstructionVisitor v) {
			v.visitRet(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.popStack();
			return (MozInst) s.ref;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.xRet();
		}

	}

	public final static class Alt extends MozInst {
		public final MozInst jump;

		public Alt(Expression e, MozInst failjump, MozInst next) {
			super(MozSet.Alt, e, next);
			this.jump = joinPoint(failjump);
		}

		public Alt(MozInst failjump, MozInst next) {
			super(MozSet.Alt, null, next);
			this.jump = joinPoint(failjump);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitAlt(this);
		}

		@Override
		MozInst branch() {
			return this.jump;
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			sc.pushAlt(this.jump);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xAlt(jump);
			return this.next;
		}
	}

	public final static class Succ extends MozInst {
		public Succ(Expression e, MozInst next) {
			super(MozSet.Succ, e, next);
		}

		public Succ(MozInst next) {
			super(MozSet.Succ, null, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSucc(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			sc.popAlt();
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xSucc();
			return this.next;
		}
	}

	public final static class Fail extends MozInst {
		public Fail(Expression e) {
			super(MozSet.Fail, e, null);
		}

		public Fail() {
			super(MozSet.Fail, null, null);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitFail(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.xFail();
		}

	}

	public final static class Guard extends MozInst {
		public Guard(Expression e) {
			super(MozSet.Skip, e, null);
		}

		public Guard() {
			super(MozSet.Skip, null, null);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitGuard(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			return sc.skip(this.next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.xStep(this.next);
		}
	}

	public final static class Step extends MozInst {
		public Step(Expression e) {
			super(MozSet.Skip, e, null);
		}

		public Step() {
			super(MozSet.Skip, null, null);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitStep(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			return sc.skip(this.next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.xStep(this.next);
		}
	}

	/**
	 * Byte
	 * 
	 * @author kiki
	 *
	 */

	static abstract class AbstByte extends MozInst {
		public final int byteChar;

		AbstByte(byte bytecode, Nez.Byte e, MozInst next) {
			super(bytecode, e, next);
			this.byteChar = e.byteChar;
		}

		AbstByte(byte bytecode, int byteChar, MozInst next) {
			super(bytecode, null, next);
			this.byteChar = byteChar;
		}

		@Override
		protected String getOperand() {
			return StringUtils.stringfyCharacter(byteChar);
		}

	}

	public static class Byte extends AbstByte {
		public Byte(int byteChar, MozInst next) {
			super(MozSet.Byte, byteChar, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitByte(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.prefetch() == this.byteChar) {
				sc.consume(1);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			/* EOF must be checked at the next instruction */
			if (sc.read() == this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}
	}

	public final static class BinaryByte extends Byte {
		public BinaryByte(MozInst next) {
			super(0, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (sc.prefetch() == 0 && !sc.eof()) {
				sc.move(1);
				return this.next;
			}
			return sc.xFail();
		}
	}

	public static class NByte extends AbstByte {
		public NByte(int byteChar, MozInst next) {
			super(MozSet.NByte, byteChar, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitNByte(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.prefetch() != this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (sc.prefetch() != this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class BinaryNByte extends NByte {
		public BinaryNByte(int byteChar, MozInst next) {
			super(byteChar, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (sc.prefetch() != this.byteChar && !sc.eof()) {
				return this.next;
			}
			return sc.xFail();
		}
	}

	public static class OByte extends AbstByte {
		public OByte(int byteChar, MozInst next) {
			super(MozSet.OByte, byteChar, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitOByte(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.prefetch() == this.byteChar) {
				sc.consume(1);
			}
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (sc.prefetch() == this.byteChar) {
				if (this.byteChar == 0) {
					return this.next;
				}
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class BinaryOByte extends OByte {
		public BinaryOByte(MozInst next) {
			super(0, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (sc.prefetch() == 0 && !sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class RByte extends AbstByte {
		public RByte(int byteChar, MozInst next) {
			super(MozSet.RByte, byteChar, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitRByte(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			while (sc.prefetch() == this.byteChar) {
				sc.consume(1);
			}
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			while (sc.prefetch() == this.byteChar) {
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class BinaryRByte extends RByte {
		public BinaryRByte(MozInst next) {
			super(0, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			while (sc.prefetch() == 0 && !sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}
	}

	static abstract class AbstAny extends MozInst {
		AbstAny(byte opcode, Expression e, MozInst next) {
			super(opcode, e, next);
		}

	}

	public final static class Any extends AbstAny {
		public Any(Expression e, MozInst next) {
			super(MozSet.Any, e, next);
		}

		public Any(MozInst next) {
			super(MozSet.Any, null, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitAny(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.hasUnconsumed()) {
				sc.consume(1);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (!sc.eof()) {
				sc.move(1);
				return this.next;
			}
			return sc.xFail();
		}
	}

	public final static class NAny extends AbstAny {
		public NAny(Expression e, boolean isBinary, MozInst next) {
			super(MozSet.NAny, e, next);
		}

		public NAny(Expression e, MozInst next) {
			super(MozSet.NAny, null, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitNAny(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.hasUnconsumed()) {
				return sc.xFail();
			}
			return next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (sc.eof()) {
				return next;
			}
			return sc.xFail();
		}
	}

	static abstract class AbstSet extends MozInst {
		public final boolean[] byteSet;

		AbstSet(byte opcode, Nez.ByteSet e, MozInst next) {
			super(opcode, e, next);
			this.byteSet = e.byteMap;
			// if (this.byteMap[0]) {
			// this.byteMap[0] = false; // for safety
			// }
		}

		AbstSet(byte opcode, boolean[] byteMap, MozInst next) {
			super(opcode, null, next);
			this.byteSet = byteMap;
			// if (this.byteMap[0]) {
			// this.byteMap[0] = false; // for safety
			// }
		}

		@Override
		protected String getOperand() {
			return StringUtils.stringfyCharacterClass(byteSet);
		}

	}

	public static class Set extends AbstSet {
		public Set(boolean[] byteMap, MozInst next) {
			super(MozSet.Set, byteMap, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSet(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar]) {
				sc.consume(1);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.read();
			if (byteSet[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class BinarySet extends Set {
		public BinarySet(boolean[] byteMap, MozInst next) {
			super(byteMap, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar] && !sc.eof()) {
				sc.move(1);
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class OSet extends AbstSet {
		public OSet(boolean[] byteMap, MozInst next) {
			super(MozSet.OSet, byteMap, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitOSet(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar]) {
				sc.consume(1);
			}
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar]) {
				sc.move(1);
			}
			return this.next;
		}

	}

	public static class BinaryOSet extends OSet {
		public BinaryOSet(boolean[] byteMap, MozInst next) {
			super(byteMap, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (byteSet[byteChar] && sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}
	}

	public static class NSet extends AbstSet {
		public NSet(boolean[] byteMap, MozInst next) {
			super(MozSet.NSet, byteMap, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitNSet(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (!byteSet[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (!byteSet[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class BinaryNSet extends NSet {
		public BinaryNSet(boolean[] byteMap, MozInst next) {
			super(byteMap, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (!byteSet[byteChar] && !sc.eof()) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public static class RSet extends AbstSet {
		public RSet(boolean[] byteMap, MozInst next) {
			super(MozSet.RSet, byteMap, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitRSet(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			while (byteSet[byteChar]) {
				sc.consume(1);
				byteChar = sc.prefetch();
			}
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			while (byteSet[sc.prefetch()]) {
				sc.move(1);
			}
			return this.next;
		}

	}

	public static class BinaryRSet extends RSet {
		public BinaryRSet(boolean[] byteMap, MozInst next) {
			super(byteMap, next);
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			while (byteSet[sc.prefetch()] && !sc.eof()) {
				sc.move(1);
			}
			return this.next;
		}

	}

	static abstract class AbstStr extends MozInst {
		final byte[] utf8;

		public AbstStr(byte opcode, Nez.MultiByte e, byte[] utf8, MozInst next) {
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

	}

	public final static class Str extends AbstStr {
		public Str(Nez.MultiByte e, MozInst next) {
			super(MozSet.Str, e, e.byteSeq, next);
		}

		public Str(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitStr(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.match(this.utf8)) {
				sc.consume(utf8.length);
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class NStr extends AbstStr {
		public NStr(Nez.MultiByte e, MozInst next) {
			super(MozSet.NStr, e, e.byteSeq, next);
		}

		public NStr(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitNStr(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (!sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			if (!sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

	}

	public final static class OStr extends AbstStr {
		public OStr(Nez.MultiByte e, MozInst next) {
			super(MozSet.OStr, e, e.byteSeq, next);
		}

		public OStr(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitOStr(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.match(this.utf8)) {
				sc.consume(utf8.length);
			}
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.match(this.utf8);
			return this.next;
		}

	}

	public final static class RStr extends AbstStr {
		public RStr(Nez.MultiByte e, MozInst next) {
			super(MozSet.RStr, e, e.byteSeq, next);
		}

		public RStr(byte[] byteSeq, MozInst next) {
			super(MozSet.Str, null, byteSeq, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitRStr(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			while (sc.match(this.utf8)) {
				sc.consume(utf8.length);
			}
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			while (sc.match(this.utf8)) {
			}
			return this.next;
		}

	}

	public static class Dispatch extends MozInst {
		public byte[] jumpIndex;
		public MozInst[] jumpTable;

		public Dispatch(byte opcode, Nez.Choice e, MozInst next) {
			super(opcode, e, next);
			jumpTable = new MozInst[257];
			Arrays.fill(jumpTable, next);
		}

		public Dispatch(Nez.Choice e, MozInst next) {
			this(MozSet.First, e, next);
		}

		void setJumpTable(int ch, MozInst inst) {
			jumpTable[ch] = joinPoint(inst);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitDispatch(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			int ch = sc.prefetch();
			return jumpTable[ch];
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int ch = sc.prefetch();
			return jumpTable[ch];
		}

	}

	public final static class DDispatch extends Dispatch {
		public DDispatch(Nez.Choice e, MozInst next) {
			super(MozSet.DFirst, e, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitDDispatch(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			int ch = sc.prefetch();
			sc.consume(1);
			return jumpTable[ch];
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return jumpTable[sc.read()];
		}

	}

	// Tree Construction

	public final static class TPush extends MozInst {
		public TPush(Expression e, MozInst next) {
			super(MozSet.TPush, e, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTPush(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logPush();
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xTPush();
			return this.next;
		}

	}

	public final static class TPop extends MozInst {

		public TPop(Expression e, MozInst next) {
			super(MozSet.TPop, e, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTPop(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			// ASTMachine astMachine = sc.getAstMachine();
			// astMachine.a(label);
			System.out.println("unsupported");
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xTPop();
			return this.next;
		}

	}

	public final static class TBegin extends MozInst {
		public final int shift;

		public TBegin(Nez.BeginTree e, MozInst next) {
			super(MozSet.TNew, e, next);
			this.shift = e.shift;
		}

		public TBegin(int shift, MozInst next) {
			super(MozSet.TNew, null, next);
			this.shift = shift;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTBegin(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logNew(sc.getPosition() + shift, this.id);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.beginTree(shift);
			return this.next;
		}
	}

	public final static class TEnd extends MozInst {
		public final int shift;
		public final Symbol tag;
		public final String value;

		public TEnd(Nez.EndTree e, MozInst next) {
			super(MozSet.TCapture, e, next);
			this.shift = e.shift;
			this.tag = e.tag;
			this.value = e.value;
		}

		public TEnd(Symbol tag, String value, int shift, MozInst next) {
			super(MozSet.TCapture, null, next);
			this.tag = tag;
			this.value = value;
			this.shift = shift;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTEnd(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logCapture(sc.getPosition() + shift);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.endTree(tag, value, shift);
			return this.next;
		}
	}

	public final static class TTag extends MozInst {
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
		public void visit(InstructionVisitor v) {
			v.visitTTag(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logTag(tag);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.tagTree(tag);
			return this.next;
		}

	}

	public final static class TReplace extends MozInst {
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
		public void visit(InstructionVisitor v) {
			v.visitTReplace(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logReplace(this.value);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.valueTree(value);
			return this.next;
		}

	}

	public final static class TLink extends MozInst {
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
		public void visit(InstructionVisitor v) {
			v.visitTLink(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logPop(label);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xTLink(label);
			return this.next;
		}

	}

	public final static class TFold extends MozInst {
		public final int shift;
		public final Symbol label;

		public TFold(Nez.FoldTree e, MozInst next) {
			super(MozSet.TLeftFold, e, next);
			this.shift = e.shift;
			this.label = e.label;
		}

		public TFold(Symbol label, int shift, MozInst next) {
			super(MozSet.TLeftFold, null, next);
			this.label = label;
			this.shift = shift;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTFold(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.logLeftFold(sc.getPosition() + shift, this.label);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.foldTree(shift, label);
			return this.next;
		}
	}

	public final static class TEmit extends MozInst {
		public final Symbol label;

		public TEmit(Nez.LinkTree e, MozInst next) {
			super(MozSet.TCommit, e, next);
			this.label = e.label;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTEmit(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.popStack();
			ASTMachine astMachine = sc.getAstMachine();
			astMachine.commitTransactionPoint(label, s.ref);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return this.next;
		}

	}

	public final static class TStart extends MozInst {
		public TStart(Nez.LinkTree e, MozInst next) {
			super(MozSet.TStart, e, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTStart(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.newUnusedStack();
			ASTMachine astMachine = sc.getAstMachine();
			s.ref = astMachine.saveTransactionPoint();
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return this.next;
		}

	}

	/* Symbol */

	static abstract class AbstractTableInstruction extends MozInst {
		public final Symbol table;

		AbstractTableInstruction(byte opcode, Expression e, Symbol tableName, MozInst next) {
			super(opcode, e, next);
			this.table = tableName;
		}

		@Override
		protected String getOperand() {
			return table.getSymbol();
		}

	}

	public final static class SOpen extends MozInst {
		public SOpen(Nez.BlockScope e, MozInst next) {
			super(MozSet.SOpen, e, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSOpen(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.newUnusedStack();
			s.value = sc.getSymbolTable().saveSymbolPoint();
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xSOpen();
			return this.next;
		}

	}

	public final static class SClose extends MozInst {
		public SClose(Expression e, MozInst next) {
			super(MozSet.SClose, e, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSClose(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.popStack();
			sc.getSymbolTable().backSymbolPoint((int) s.value);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xSClose();
			return this.next;
		}
	}

	public final static class SMask extends AbstractTableInstruction {
		public SMask(Nez.LocalScope e, MozInst next) {
			super(MozSet.SMask, e, e.tableName, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSMask(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.newUnusedStack();
			SymbolTable st = sc.getSymbolTable();
			s.value = st.saveSymbolPoint();
			st.addSymbolMask(table);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.xSOpen();
			sc.addSymbolMask(table);
			return this.next;
		}

	}

	public final static class SDef extends AbstractTableInstruction {
		public SDef(Nez.SymbolAction e, MozInst next) {
			super(MozSet.SDef, e, e.tableName, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSDef(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData top = sc.popStack();
			byte[] captured = sc.subbyte(top.value, sc.getPosition());
			// System.out.println("symbol captured: " + new String(captured) +
			// ", @"
			// + this.tableName);
			sc.getSymbolTable().addSymbol(this.table, captured);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			sc.addSymbol(table, ppos);
			return this.next;
		}
	}

	public final static class SExists extends AbstractTableInstruction {
		public SExists(Nez.SymbolExists e, MozInst next) {
			super(MozSet.SExists, e, e.tableName, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSExists(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			byte[] t = sc.getSymbolTable().getSymbol(table);
			return t != null ? this.next : sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.exists(table) ? this.next : sc.xFail();
		}

	}

	public final static class SIsDef extends AbstractTableInstruction {
		byte[] utf8;

		public SIsDef(SymbolExists e, MozInst next) {
			super(MozSet.SIsDef, e, e.tableName, next);
			utf8 = StringUtils.toUtf8(e.symbol);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSIsDef(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			if (sc.getSymbolTable().contains(this.table, utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.existsSymbol(table, utf8) ? this.next : sc.xFail();
		}
	}

	public final static class SMatch extends AbstractTableInstruction {
		public SMatch(Nez.SymbolMatch e, MozInst next) {
			super(MozSet.SMatch, e, e.tableName, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSMatch(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			byte[] t = sc.getSymbolTable().getSymbol(table);
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
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.matchSymbol(table) ? this.next : sc.xFail();
		}

	}

	public final static class SIs extends AbstractTableInstruction {
		public SIs(Nez.SymbolPredicate e, MozInst next) {
			super(MozSet.SIs, e, e.tableName, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSIs(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			byte[] symbol = sc.getSymbolTable().getSymbol(table);
			if (symbol != null) {
				MozStackData s = sc.popStack();
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
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			return sc.equals(table, ppos) ? this.next : sc.xFail();
		}

	}

	public final static class SIsa extends AbstractTableInstruction {
		public SIsa(Nez.SymbolPredicate e, MozInst next) {
			super(MozSet.SIsa, e, e.tableName, next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitSIsa(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MozStackData s = sc.popStack();
			byte[] captured = sc.subbyte(s.value, sc.getPosition());
			if (sc.getSymbolTable().contains(this.table, captured)) {
				// sc.consume(captured.length);
				return this.next;

			}
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			return sc.contains(table, ppos) ? this.next : sc.xFail();
		}

	}

	/* Number */

	public final static class NScan extends MozInst {
		public final long mask;
		public final int shift;

		public NScan(long mask, int shift, MozInst next) {
			super(MozSet.SDefNum, null, next);
			this.mask = mask;
			this.shift = shift;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitNScan(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xPPos();
			sc.scanCount(ppos, mask, shift);
			return next;
		}
	}

	public final static class NDec extends MozInst {
		public final MozInst jump;

		public NDec(MozInst jump, MozInst next) {
			super(MozSet.SCount, null, next);
			this.jump = jump;
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitNDec(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			return this.jump;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			return sc.decCount() ? this.next : this.jump;
		}
	}

	/* Memoization */

	static abstract class AbstractMemoizationInstruction extends MozInst {
		final MemoPoint memoPoint;
		final int uid;
		final boolean state;
		final MozInst jump;

		AbstractMemoizationInstruction(byte opcode, Expression e, MemoPoint m, boolean state, MozInst next, MozInst skip) {
			super(opcode, e, next);
			this.memoPoint = m;
			this.uid = m.id;
			this.jump = joinPoint(skip);
			this.state = state;
		}

		AbstractMemoizationInstruction(byte opcode, Expression e, MemoPoint m, boolean state, MozInst next) {
			super(opcode, e, next);
			this.memoPoint = m;
			this.uid = m.id;
			this.state = state;
			this.jump = null;
		}

		@Override
		protected String getOperand() {
			return String.valueOf(this.memoPoint);
		}

	}

	public final static class Lookup extends AbstractMemoizationInstruction {
		public Lookup(Expression e, MemoPoint m, MozInst next, MozInst skip) {
			super(MozSet.Lookup, e, m, m.isStateful(), next, skip);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitLookup(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MemoEntry entry = sc.getMemo(uid, state);
			if (entry != null) {
				if (entry.failed) {
					memoPoint.failHit();
					return sc.xFail();
				}
				memoPoint.memoHit(entry.consumed);
				sc.consume(entry.consumed);
				return this.jump;
			}
			memoPoint.miss();
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			switch (sc.lookupMemo(uid)) {
			case ParserContext.NotFound:
				return this.next;
			case ParserContext.SuccFound:
				return this.jump;
			default:
				return sc.xFail();
			}
		}
	}

	public final static class Memo extends AbstractMemoizationInstruction {
		public Memo(Expression e, MemoPoint m, MozInst next) {
			super(MozSet.Memo, e, m, m.isStateful(), next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitMemo(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			long ppos = sc.popAlt();
			int length = (int) (sc.getPosition() - ppos);
			sc.setMemo(ppos, uid, false, null, length, this.state);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xSuccPos();
			sc.memoSucc(uid, ppos);
			return this.next;
		}
	}

	public final static class MemoFail extends AbstractMemoizationInstruction {
		public MemoFail(Expression e, MemoPoint m) {
			super(MozSet.MemoFail, e, m, m.isStateful(), null);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitMemoFail(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			sc.setMemo(sc.getPosition(), uid, true, null, 0, state);
			return sc.xFail();
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			sc.memoFail(uid);
			return sc.xFail();
		}

	}

	public final static class TLookup extends AbstractMemoizationInstruction {
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
		public void visit(InstructionVisitor v) {
			v.visitTLookup(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			MemoEntry entry = sc.getMemo(uid, state);
			if (entry != null) {
				if (entry.failed) {
					memoPoint.failHit();
					return sc.xFail();
				}
				memoPoint.memoHit(entry.consumed);
				sc.consume(entry.consumed);
				ASTMachine astMachine = sc.getAstMachine();
				astMachine.logLink(label, entry.result);
				return this.jump;
			}
			memoPoint.miss();
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			switch (sc.lookupTreeMemo(uid)) {
			case ParserContext.NotFound:
				return this.next;
			case ParserContext.SuccFound:
				return this.jump;
			default:
				return sc.xFail();
			}
		}

	}

	public final static class TMemo extends AbstractMemoizationInstruction {
		public TMemo(Expression e, MemoPoint m, MozInst next) {
			super(MozSet.TMemo, e, m, m.isStateful(), next);
		}

		@Override
		public void visit(InstructionVisitor v) {
			v.visitTMemo(this);
		}

		@Override
		public MozInst execMoz(MozMachine sc) throws TerminationException {
			ASTMachine astMachine = sc.getAstMachine();
			long ppos = sc.popAlt();
			int length = (int) (sc.getPosition() - ppos);
			sc.setMemo(ppos, uid, false, astMachine.getLatestLinkedNode(), length, this.state);
			return this.next;
		}

		@Override
		public MozInst exec(ParserMachineContext sc) throws TerminationException {
			int ppos = sc.xSuccPos();
			sc.memoTreeSucc(uid, ppos);
			return this.next;
		}

	}

}
