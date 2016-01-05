package nez.parser.hachi6;

import java.lang.reflect.Field;
import java.util.HashMap;

import nez.ast.Symbol;
import nez.parser.TerminationException;

public class Hachi6 {
	public final static String[][] Hachi6InstSet = { //
	//
			{ "Nop" }, //
			{ "Label", "name" }, //
			{ "Cov", "uid" }, //
			{ "Exit", "state" }, //

			{ "Pos" }, //
			{ "Back" }, //
			{ "Move", "shift" }, //
			{ "Jump", "jump" }, //
			{ "Call", "jump", "name" }, // name is for debug
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

	static HashMap<String, String[]> instMap = new HashMap<String, String[]>();
	static {
		for (String[] insts : Hachi6InstSet) {
			instMap.put(insts[0], insts);
		}
	}

	static int opSize(String name) {
		String[] insts = instMap.get(name);
		return insts.length - 1;
	}

	static Object opValue(Hachi6Inst inst, int p) {
		String[] insts = instMap.get(inst.getName());
		try {
			Field f = inst.getClass().getField(insts[p + 1]);
			return f.get(inst);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// static HashMap<String, String> javaMap = new HashMap<String, String>();
	// static {
	// // type
	// javaMap.put("tNonTerminal", "String");
	// javaMap.put("tJump", "Hachi6Inst");
	// javaMap.put("tJumpTable", "Hachi6Inst[]");
	// javaMap.put("tByte", "int");
	// javaMap.put("tBset", "boolean[]");
	// javaMap.put("tBstr", "byte[]");
	// javaMap.put("tShift", "int");
	// javaMap.put("tMemoPoint", "int");
	// javaMap.put("tState", "boolean");
	// javaMap.put("tLabel", "Symbol");
	// javaMap.put("tTag", "Symbol");
	// javaMap.put("tTable", "Symbol");
	//
	// // name
	// javaMap.put("nNonTerminal", "nonTerminal");
	// javaMap.put("nJump", "jump");
	// javaMap.put("nJumpTable", "jumpTable");
	// javaMap.put("nByte", "byteChar");
	// javaMap.put("nBset", "byteMap");
	// javaMap.put("nBstr", "utf8");
	// javaMap.put("nShift", "shift");
	// javaMap.put("nMemoPoint", "memoPoint");
	// javaMap.put("nState", "state");
	// javaMap.put("nLabel", "label");
	// javaMap.put("nTag", "tag");
	// javaMap.put("nTable", "table");
	//
	// }

	public static class Nop extends Hachi6Inst {
		public Nop(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitNop(this);
		}
	}

	public static class Label extends Hachi6Inst {
		String name;

		public Label(String name, Hachi6Inst next) {
			super(next);
			this.name = name;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitLabel(this);
		}
	}

	public static class Cov extends Hachi6Inst {
		int uid;

		public Cov(int uid, Hachi6Inst next) {
			super(next);
			this.uid = uid;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitCov(this);
		}
	}

	public static class Exit extends Hachi6Inst {
		boolean state;

		public Exit(boolean state, Hachi6Inst next) {
			super(next);
			this.state = state;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			throw new TerminationException(state);
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitExit(this);
		}
	}

	public static class Pos extends Hachi6Inst {
		public Pos(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xPos();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitPos(this);
		}
	}

	public static class Back extends Hachi6Inst {
		public Back(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xBack();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitBack(this);
		}
	}

	public static class Move extends Hachi6Inst {
		public final int shift;

		public Move(int shift, Hachi6Inst next) {
			super(next);
			this.shift = shift;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.shift(shift);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitMove(this);
		}
	}

	public static class Jump extends Hachi6Branch {
		public Jump(Hachi6Inst jump, Hachi6Inst next) {
			super(jump, next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return this.jump;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitJump(this);
		}
	}

	// Call
	public static class Call extends Hachi6Branch {
		public String name;

		public Call(Hachi6Inst jump, String name, Hachi6Inst next) {
			super(jump, next);
			this.name = name;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.push(this.next);
			return this.jump;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitCall(this);
		}
	}

	// Ret
	public static class Ret extends Hachi6Inst {
		public Ret(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return (Hachi6Inst) sc.rpop();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitRet(this);
		}
	}

	// Alt
	public static class Alt extends Hachi6Branch {
		public Alt(Hachi6Inst jump, Hachi6Inst next) {
			super(jump, next);
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xAlt(this.jump);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitAlt(this);
		}
	}

	// Succ
	public static class Succ extends Hachi6Inst {
		public Succ(Hachi6Inst next) {
			super(next);
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xSucc();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSucc(this);
		}
	}

	// Fail
	public static class Fail extends Hachi6Inst {
		public Fail(Hachi6Inst next) {
			super(next);
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitFail(this);
		}
	}

	// Guard
	public static class Guard extends Hachi6Inst {
		public Guard(Hachi6Inst next) {
			super(next);
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return sc.xGuard(this.next);
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitGuard(this);
		}
	}

	// Step
	public static class Skip extends Hachi6Inst {
		public Skip(Hachi6Inst next) {
			super(next);
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return sc.xSkip(this.next);
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSkip(this);
		}
	}

	// Byte
	public static class Byte extends Hachi6Inst {
		public final int byteChar;

		public Byte(int byteChar, Hachi6Inst next) {
			super(next);
			this.byteChar = byteChar;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			if (sc.read() == this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitByte(this);
		}

	}

	// Any
	public static class Any extends Hachi6Inst {
		public Any(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			int u = sc.read();
			if (u == 0) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitAny(this);
		}
	}

	// Str
	public static class Str extends Hachi6Inst {
		public final byte[] utf8;

		public Str(byte[] utf8, Hachi6Inst next) {
			super(next);
			this.utf8 = utf8;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			if (sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitStr(this);
		}
	}

	// Set
	public static class Set extends Hachi6Inst {
		public final boolean[] byteSet;

		public Set(boolean[] set, Hachi6Inst next) {
			super(next);
			this.byteSet = set;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			int u = sc.read();
			if (byteSet[u]) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSet(this);
		}
	}

	// Lookup
	public static class Lookup extends Hachi6Branch {
		public final int uid;

		public Lookup(Hachi6Inst jump, int memo, Hachi6Inst next) {
			super(jump, next);
			this.uid = memo;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			return sc.xLookup(this.uid) ? this.jump : this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitLookup(this);
		}
	}

	// Memo
	public static class Memo extends Hachi6Inst {
		public final int uid;

		public Memo(int uid, Hachi6Inst next) {
			super(next);
			this.uid = uid;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xMemo(uid);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitMemo(this);
		}
	}

	// FailMemo
	public static class FailMemo extends Hachi6Inst {
		public final int uid;

		public FailMemo(int uid, Hachi6Inst next) {
			super(next);
			this.uid = uid;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xFailMemo(uid);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitFailMemo(this);
		}
	}

	// PushTree
	public static class PushTree extends Hachi6Inst {
		public PushTree(Hachi6Inst next) {
			super(next);
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xPushTree();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitPushTree(this);
		}
	}

	// PopTree
	public static class PopTree extends Hachi6Inst {
		public PopTree(Hachi6Inst next) {
			super(next);
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xPopTree();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitPopTree(this);
		}
	}

	// Sinit
	public static class Sinit extends Hachi6Inst {
		public final int shift;

		public Sinit(int shift, Hachi6Inst next) {
			super(next);
			this.shift = shift;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xSinit(shift);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSinit(this);
		}
	}

	// Snew
	public static class Snew extends Hachi6Inst {
		public final int shift;
		public final Symbol tag;
		public final String value;

		public Snew(int shift, Symbol tag, String value, Hachi6Inst next) {
			super(next);
			this.shift = shift;
			this.tag = tag;
			this.value = value;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xSnew(shift, tag, value);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSnew(this);
		}
	}

	// Init
	public static class Init extends Hachi6Inst {
		public final int shift;

		public Init(int shift, Hachi6Inst next) {
			super(next);
			this.shift = shift;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xInit(shift);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitInit(this);
		}
	}

	// New
	public static class New extends Hachi6Inst {
		public final int shift;

		public New(int shift, Hachi6Inst next) {
			super(next);
			this.shift = shift;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xNew(shift);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitNew(this);
		}
	}

	// Tag
	public static class Tag extends Hachi6Inst {
		public final Symbol tag;

		public Tag(Symbol tag, Hachi6Inst next) {
			super(next);
			this.tag = tag;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.opush(tag);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitTag(this);
		}
	}

	// Value
	public static class Value extends Hachi6Inst {
		public final String value;

		public Value(String value, Hachi6Inst next) {
			super(next);
			this.value = value;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.opush(value);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitValue(this);
		}
	}

	public static class Link extends Hachi6Inst {
		public final Symbol label;

		public Link(Symbol label, Hachi6Inst next) {
			super(next);
			this.label = label;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xLink(label);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitLink(this);
		}
	}

	// Emit
	public static class Emit extends Hachi6Inst {
		public final Symbol label;

		public Emit(Symbol label, Hachi6Inst next) {
			super(next);
			this.label = label;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xEmit(label);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitEmit(this);
		}
	}

	public static class LeftFold extends Hachi6Inst {
		public final int shift;
		public final Symbol label;

		public LeftFold(int shift, Symbol label, Hachi6Inst next) {
			super(next);
			this.shift = shift;
			this.label = label;
		}

		@Override
		public final Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.xLeftFold(shift, label);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitLeftFold(this);
		}
	}

	/* symbol table */

	// SOpen
	public static class SOpen extends Hachi6Inst {
		public SOpen(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// StackData s = sc.newUnusedStack();
			// s.value = sc.getSymbolTable().savePoint();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSOpen(this);
		}
	}

	// SClose
	public static class SClose extends Hachi6Inst {
		public SClose(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// StackData s = sc.popStack();
			// sc.getSymbolTable().rollBack((int) s.value);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSClose(this);
		}
	}

	// SMask
	public static class SMask extends Hachi6Inst {
		public final Symbol table;

		public SMask(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// StackData s = sc.newUnusedStack();
			// SymbolTable st = sc.getSymbolTable();
			// s.value = st.savePoint();
			// st.addSymbolMask(table);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSMask(this);
		}

	}

	public static class SDef extends Hachi6Inst {
		public final Symbol table;

		public SDef(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// StackData top = sc.popStack();
			// byte[] captured = sc.subbyte(top.value, sc.getPosition());
			// sc.getSymbolTable().addSymbol(this.table, captured);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSDef(this);
		}

	}

	public static class SIsDef extends Hachi6Inst {
		public final Symbol table;
		public final byte[] utf8;

		public SIsDef(Symbol table, byte[] utf8, Hachi6Inst next) {
			super(next);
			this.table = table;
			this.utf8 = utf8;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// if (sc.getSymbolTable().contains(this.table, utf8)) {
			// return this.next;
			// }
			// return sc.fail();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSIsDef(this);
		}
	}

	// SExists
	public static class SExists extends Hachi6Inst {
		public final Symbol table;

		public SExists(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// byte[] t = sc.getSymbolTable().getSymbol(table);
			// return t != null ? this.next : sc.fail();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSExists(this);
		}

	}

	// SMatch
	public static class SMatch extends Hachi6Inst {
		public final Symbol table;

		public SMatch(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// byte[] t = sc.getSymbolTable().getSymbol(table);
			// if (t == null) {
			// return this.next;
			// }
			// if (sc.match(sc.getPosition(), t)) {
			// sc.consume(t.length);
			// return this.next;
			// }
			// return sc.fail();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSMatch(this);
		}
	}

	// SIs
	public static class SIs extends Hachi6Inst {
		public final Symbol table;

		public SIs(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// byte[] symbol = sc.getSymbolTable().getSymbol(table);
			// // System.out.println("symbol:" + new String(symbol));
			// if (symbol != null) {
			// StackData s = sc.popStack();
			// byte[] captured = sc.subbyte(s.value, sc.getPosition());
			// // System.out.println("captured:" + new String(captured));
			// if (symbol.length == captured.length &&
			// SymbolTable.equals(symbol, captured)) {
			// // sc.consume(symbol.length);
			// return this.next;
			// }
			// }
			// return sc.fail();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSIs(this);
		}

	}

	// SIsa
	public static class SIsa extends Hachi6Inst {
		public final Symbol table;

		public SIsa(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// StackData s = sc.popStack();
			// byte[] captured = sc.subbyte(s.value, sc.getPosition());
			// if (sc.getSymbolTable().contains(this.table, captured)) {
			// // sc.consume(captured.length);
			// return this.next;
			//
			// }
			// return sc.fail();
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitSIsa(this);
		}
	}

	// SDefNum
	public static class SDefNum extends Hachi6Inst {
		public final Symbol table;

		public SDefNum(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			throw new RuntimeException("TODO");
		}
	}

	// SCount
	public static class SCount extends Hachi6Inst {
		public final Symbol table;

		public SCount(Symbol table, Hachi6Inst next) {
			super(next);
			this.table = table;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			throw new RuntimeException("TODO");
		}
	}

	/* DFA */

	public static class Dispatch extends Hachi6BranchTable {
		public Dispatch(Hachi6Inst[] jumpTable, Hachi6Inst next) {
			super(jumpTable, next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			int ch = sc.read();
			return jumpTable[ch];
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitDispatch(this);
		}

	}

	public static class EDispatch extends Hachi6BranchTable {
		public EDispatch(Hachi6Inst[] jumpTable, Hachi6Inst next) {
			super(jumpTable, next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			int ch = sc.prefetch();
			return jumpTable[ch];
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitEDispatch(this);
		}

	}

	/* lexical optimization */

	public static class NByte extends Hachi6Inst {
		public final int byteChar;

		public NByte(int byteChar, Hachi6Inst next) {
			super(next);
			this.byteChar = byteChar;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			if (sc.prefetch() != this.byteChar) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitNByte(this);
		}

	}

	// NAny
	public static class NAny extends Hachi6Inst {
		public NAny(Hachi6Inst next) {
			super(next);
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			if (sc.prefetch() != 0) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitNAny(this);
		}
	}

	public static class NStr extends Hachi6Inst {
		public final byte[] utf8;

		public NStr(byte[] utf8, Hachi6Inst next) {
			super(next);
			this.utf8 = utf8;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			if (!sc.match(this.utf8)) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitNStr(this);
		}
	}

	public static class NSet extends Hachi6Inst {
		public final boolean[] byteSet;

		public NSet(boolean[] set, Hachi6Inst next) {
			super(next);
			this.byteSet = set;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			if (!byteSet[byteChar]) {
				return this.next;
			}
			return sc.xFail();
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitNSet(this);
		}
	}

	// OByte
	public static class OByte extends Hachi6Inst {
		public final int byteChar;

		public OByte(int byteChar, Hachi6Inst next) {
			super(next);
			this.byteChar = byteChar;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			if (sc.prefetch() == this.byteChar) {
				sc.shift(1);
			}
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitOByte(this);
		}
	}

	// // OAny
	// public static class OAny extends Hachi6Inst {
	// public OAny(Expression e, Hachi6Inst next) {
	// super(Moz.OAny, e, next);
	// }
	// }

	public static class OStr extends Hachi6Inst {
		public final byte[] utf8;

		public OStr(byte[] utf8, Hachi6Inst next) {
			super(next);
			this.utf8 = utf8;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			sc.match(this.utf8);
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitOStr(this);
		}
	}

	// OSet
	public static class OSet extends Hachi6Inst {
		public final boolean[] byteSet;

		public OSet(boolean[] set, Hachi6Inst next) {
			super(next);
			this.byteSet = set;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			int bc = sc.prefetch();
			if (byteSet[bc]) {
				sc.shift(1);
			}
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitOSet(this);
		}
	}

	public static class RByte extends Hachi6Inst {
		public final int byteChar;

		public RByte(int byteChar, Hachi6Inst next) {
			super(next);
			this.byteChar = byteChar;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			while (sc.prefetch() == this.byteChar) {
				sc.shift(1);
			}
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitRByte(this);
		}
	}

	// public static class RAny extends Hachi6Inst {
	// public RAny(Hachi6Inst next) {
	// super(next);
	// }
	//
	// @Override
	// public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
	// while (sc.prefetch() != 0) {
	// sc.shift(1);
	// }
	// return this.next;
	// }
	//
	// @Override
	// public void visit(Hachi6Visitor v) {
	// v.visitRByte(this);
	// }
	//
	// }

	public static class RStr extends Hachi6Inst {
		public final byte[] utf8;

		public RStr(byte[] utf8, Hachi6Inst next) {
			super(next);
			this.utf8 = utf8;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			while (sc.match(this.utf8)) {
			}
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitRStr(this);
		}
	}

	public static class RSet extends Hachi6Inst {
		public final boolean[] set;

		public RSet(boolean[] set, Hachi6Inst next) {
			super(next);
			this.set = set;
		}

		@Override
		public Hachi6Inst exec(Hachi6Machine sc) throws TerminationException {
			int byteChar = sc.prefetch();
			while (set[byteChar]) {
				sc.shift(1);
				byteChar = sc.prefetch();
			}
			return this.next;
		}

		@Override
		public void visit(Hachi6Visitor v) {
			v.visitRSet(this);
		}
	}

}
