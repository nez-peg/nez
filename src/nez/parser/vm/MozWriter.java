package nez.parser.vm;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import nez.ast.Symbol;
import nez.parser.vm.Moz86.Alt;
import nez.parser.vm.Moz86.Any;
import nez.parser.vm.Moz86.Back;
import nez.parser.vm.Moz86.Byte;
import nez.parser.vm.Moz86.Call;
import nez.parser.vm.Moz86.Cov;
import nez.parser.vm.Moz86.DDispatch;
import nez.parser.vm.Moz86.Dispatch;
import nez.parser.vm.Moz86.Exit;
import nez.parser.vm.Moz86.Fail;
import nez.parser.vm.Moz86.Guard;
import nez.parser.vm.Moz86.Jump;
import nez.parser.vm.Moz86.Lookup;
import nez.parser.vm.Moz86.Memo;
import nez.parser.vm.Moz86.MemoFail;
import nez.parser.vm.Moz86.Move;
import nez.parser.vm.Moz86.NAny;
import nez.parser.vm.Moz86.NByte;
import nez.parser.vm.Moz86.NDec;
import nez.parser.vm.Moz86.NScan;
import nez.parser.vm.Moz86.NSet;
import nez.parser.vm.Moz86.NStr;
import nez.parser.vm.Moz86.Nop;
import nez.parser.vm.Moz86.OByte;
import nez.parser.vm.Moz86.OSet;
import nez.parser.vm.Moz86.OStr;
import nez.parser.vm.Moz86.Pos;
import nez.parser.vm.Moz86.RByte;
import nez.parser.vm.Moz86.RSet;
import nez.parser.vm.Moz86.RStr;
import nez.parser.vm.Moz86.Ret;
import nez.parser.vm.Moz86.SClose;
import nez.parser.vm.Moz86.SDef;
import nez.parser.vm.Moz86.SExists;
import nez.parser.vm.Moz86.SIs;
import nez.parser.vm.Moz86.SIsDef;
import nez.parser.vm.Moz86.SIsa;
import nez.parser.vm.Moz86.SMask;
import nez.parser.vm.Moz86.SMatch;
import nez.parser.vm.Moz86.SOpen;
import nez.parser.vm.Moz86.Set;
import nez.parser.vm.Moz86.Step;
import nez.parser.vm.Moz86.Str;
import nez.parser.vm.Moz86.Succ;
import nez.parser.vm.Moz86.TBegin;
import nez.parser.vm.Moz86.TEmit;
import nez.parser.vm.Moz86.TEnd;
import nez.parser.vm.Moz86.TFold;
import nez.parser.vm.Moz86.TLink;
import nez.parser.vm.Moz86.TLookup;
import nez.parser.vm.Moz86.TMemo;
import nez.parser.vm.Moz86.TPop;
import nez.parser.vm.Moz86.TPush;
import nez.parser.vm.Moz86.TReplace;
import nez.parser.vm.Moz86.TStart;
import nez.parser.vm.Moz86.TTag;
import nez.util.StringUtils;
import nez.util.Verbose;

public class MozWriter extends InstructionVisitor {

	class SetEntry {
		int id;
		boolean[] data;

		SetEntry(int id, boolean[] data) {
			this.id = id;
			this.data = data;
		}
	}

	class StrEntry {
		int id;
		byte[] data;

		StrEntry(int id, byte[] data) {
			this.id = id;
			this.data = data;
		}
	}

	class TagEntry {
		int id;
		Symbol data;

		TagEntry(int id, Symbol data) {
			this.id = id;
			this.data = data;
		}
	}

	class SymEntry {
		int id;
		int tabid;
		byte[] symbol;

		SymEntry(int id, int tabid, byte[] symbol) {
			this.id = id;
			this.tabid = tabid;
			this.symbol = symbol;
		}
	}

	int instSize;
	int prodSize;
	int jumpTableSize;
	int memoSize;
	ByteArrayOutputStream stream;
	HashMap<String, StrEntry> NonTerminalPoolMap;
	HashMap<String, SetEntry> BSetPoolMap;
	HashMap<String, StrEntry> BStrPoolMap;
	HashMap<String, TagEntry> TagPoolMap;
	HashMap<String, TagEntry> TablePoolMap; // tableEntry
	ArrayList<StrEntry> NonTerminalPools;
	ArrayList<SetEntry> BSetPools;
	ArrayList<StrEntry> BStrPools;
	ArrayList<TagEntry> TagPools;
	ArrayList<TagEntry> TablePools; // tableEntry

	public void setHeader(int instSize, int prodSize, int memoSize) {
		this.instSize = instSize;
		this.prodSize = prodSize;
		this.memoSize = memoSize;
		NonTerminalPoolMap = new HashMap<>();
		BSetPoolMap = new HashMap<>();
		BStrPoolMap = new HashMap<>();
		TagPoolMap = new HashMap<>();
		TablePoolMap = new HashMap<>();
		NonTerminalPools = new ArrayList<>();
		BSetPools = new ArrayList<>();
		BStrPools = new ArrayList<>();
		TagPools = new ArrayList<>();
		TablePools = new ArrayList<>();
	}

	public void setInstructions(MozInst[] insts, int len) {
		stream = new ByteArrayOutputStream();
		for (int i = 0; i < len; i++) {
			if (insts[i] != null) {
				assert (insts[i].id == i);
				encode(insts[i]);
			} else {
				encodeOpcode(MozSet.Nop);
			}
		}
	}

	public void write_b(boolean b) {
		stream.write(b ? 1 : 0);
	}

	public void write_i8(int num) {
		stream.write(num);
	}

	public void write_u16(int num) {
		stream.write(0xff & (num >> 8));
		stream.write(0xff & (num >> 0));
	}

	public void write_u24(int num) {
		stream.write(0xff & (num >> 16));
		stream.write(0xff & (num >> 8));
		stream.write(0xff & (num >> 0));
	}

	public void write_u32(int num) {
		stream.write(0xff & (num >> 24));
		stream.write(0xff & (num >> 16));
		stream.write(0xff & (num >> 8));
		stream.write(0xff & (num >> 0));
	}

	private void encodeData(boolean[] byteMap) {
		for (int i = 0; i < 256; i += 32) {
			encodeByteMap(byteMap, i);
		}
	}

	private void encodeByteMap(boolean[] b, int offset) {
		int n = 0;
		for (int i = 0; i < 32; i++) {
			if (b[offset + i]) {
				n |= (1 << i);
			}
		}
		write_u32(n);
	}

	private void write_utf8(byte[] utf8) {
		write_u16(utf8.length);
		try {
			stream.write(utf8);
			stream.write(0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void encodeData(Symbol tag) {
		write_utf8(StringUtils.toUtf8(tag.getSymbol()));
	}

	//

	public void encodeOpcode(byte opcode) {
		stream.write(opcode);
	}

	public final void encodeJumpTable() {
		this.jumpTableSize += 1;
	}

	public final void encodeJump(MozInst jump) {
		write_u24(jump.id);
	}

	public final void encodeJumpTable(MozInst[] table) {
		this.jumpTableSize += 1;
		for (MozInst j : table) {
			encodeJump(j);
		}
	}

	public void encodeState(boolean b) {
		stream.write(b ? 1 : 0);
	}

	public void encodeMemoPoint(int id) {
		this.write_u32(id);
	}

	public void encodeShift(int shift) {
		write_i8(shift);
	}

	public void encodeIndex(int index) {
		write_i8(index);
	}

	public void encodeByte(int byteChar) {
		stream.write(byteChar);
	}

	public void encodeBset(boolean[] byteMap) {
		String key = StringUtils.stringfyBitmap(byteMap);
		SetEntry entry = BSetPoolMap.get(key);
		if (entry == null) {
			entry = new SetEntry(BSetPoolMap.size(), byteMap);
			BSetPoolMap.put(key, entry);
			BSetPools.add(entry);
		}
		write_u16(entry.id);
	}

	public void encodeBstr(byte[] utf8) {
		try {
			String key = new String(utf8, StringUtils.DefaultEncoding);
			StrEntry entry = BStrPoolMap.get(key);
			if (entry == null) {
				entry = new StrEntry(BStrPoolMap.size(), utf8);
				BStrPoolMap.put(key, entry);
				BStrPools.add(entry);
			}
			write_u16(entry.id);
		} catch (IOException e) {
			Verbose.traceException(e);
		}
	}

	public void encodeNonTerminal(String key) {
		StrEntry entry = NonTerminalPoolMap.get(key);
		if (entry == null) {
			entry = new StrEntry(NonTerminalPoolMap.size(), StringUtils.toUtf8(key));
			NonTerminalPoolMap.put(key, entry);
			NonTerminalPools.add(entry);
		}
		write_u16(entry.id);
	}

	public void encodeTag(Symbol tag) {
		String key = tag.getSymbol();
		TagEntry entry = TagPoolMap.get(key);
		if (entry == null) {
			entry = new TagEntry(TagPoolMap.size(), tag);
			TagPoolMap.put(key, entry);
			TagPools.add(entry);
		}
		write_u16(entry.id);
	}

	public void encodeLabel(Symbol label) {
		if (label == null) {
			this.encodeTag(Symbol.NullSymbol);
		} else {
			this.encodeTag(label);
		}
	}

	public void encodeTable(Symbol tableName) {
		String key = tableName.getSymbol();
		TagEntry entry = TablePoolMap.get(key);
		if (entry == null) {
			entry = new TagEntry(TablePoolMap.size(), tableName);
			TablePoolMap.put(key, entry);
			TablePools.add(entry);
		}
		write_u16(entry.id);
	}

	public void writeTo(String fileName) {
		byte[] body = stream.toByteArray();
		stream = new ByteArrayOutputStream();
		stream.write('N');
		stream.write('E');
		stream.write('Z');
		stream.write('0');

		write_u16(instSize);
		write_u16(memoSize);
		write_u16(jumpTableSize);

		write_u16(NonTerminalPools.size());
		for (StrEntry e : NonTerminalPools) {
			write_utf8(e.data);
		}
		write_u16(BSetPools.size());
		for (SetEntry e : BSetPools) {
			encodeData(e.data);
		}
		write_u16(BStrPools.size());
		for (StrEntry e : BStrPools) {
			write_utf8(e.data);
		}
		write_u16(TagPools.size());
		for (TagEntry e : TagPools) {
			encodeData(e.data);
		}
		write_u16(TablePools.size());
		for (TagEntry e : TablePools) {
			encodeData(e.data);
		}
		try {
			stream.write(body);
		} catch (IOException e1) {
			Verbose.traceException(e1);
		}

		byte[] code = stream.toByteArray();
		try {
			OutputStream out = new FileOutputStream(fileName);
			out.write(code);
			out.close();
		} catch (IOException e) {
			Verbose.traceException(e);
		}
	}

	private void encode(MozInst inst) {
		if (inst.isIncrementedNext()) {
			this.encodeOpcode(inst.opcode);
			inst.visit(this);
		} else {
			this.encodeOpcode((byte) (inst.opcode | 128)); // opcode | 10000000
			inst.visit(this);
			this.encodeJump(inst.next);
		}
	}

	@Override
	public void visitNop(Nop inst) {
		this.encodeNonTerminal(inst.name);
	}

	@Override
	public void visitExit(Exit inst) {
		this.write_b(inst.status);
	}

	@Override
	public void visitCov(Cov inst) {
		this.write_u16(inst.id);
		this.write_b(inst.start);
	}

	@Override
	public void visitPos(Pos inst) {

	}

	@Override
	public void visitBack(Back inst) {

	}

	@Override
	public void visitMove(Move inst) {
		this.encodeShift(inst.shift);
	}

	@Override
	public void visitJump(Jump inst) {
		this.encodeJump(inst.jump);
	}

	@Override
	public void visitCall(Call inst) {
		this.encodeJump(inst.jump);
		this.encodeNonTerminal(inst.name); // debug information
	}

	@Override
	public void visitRet(Ret inst) {
	}

	@Override
	public void visitAlt(Alt inst) {
		this.encodeJump(inst.jump);
	}

	@Override
	public void visitSucc(Succ inst) {
	}

	@Override
	public void visitFail(Fail inst) {
	}

	@Override
	public void visitGuard(Guard inst) {
	}

	@Override
	public void visitStep(Step inst) {
	}

	@Override
	public void visitByte(Byte inst) {
		this.encodeByte(inst.byteChar);
	}

	@Override
	public void visitAny(Any inst) {
	}

	@Override
	public void visitStr(Str inst) {
		this.encodeBstr(inst.utf8);
	}

	@Override
	public void visitSet(Set inst) {
		this.encodeBset(inst.byteSet);
	}

	@Override
	public void visitNByte(NByte inst) {
		this.encodeByte(inst.byteChar);
	}

	@Override
	public void visitNAny(NAny inst) {
	}

	@Override
	public void visitNStr(NStr inst) {
		this.encodeBstr(inst.utf8);
	}

	@Override
	public void visitNSet(NSet inst) {
		this.encodeBset(inst.byteSet);
	}

	@Override
	public void visitOByte(OByte inst) {
		this.encodeByte(inst.byteChar);
	}

	@Override
	public void visitOStr(OStr inst) {
		this.encodeBstr(inst.utf8);
	}

	@Override
	public void visitOSet(OSet inst) {
		this.encodeBset(inst.byteSet);
	}

	@Override
	public void visitRByte(RByte inst) {
		this.encodeByte(inst.byteChar);
	}

	@Override
	public void visitRStr(RStr inst) {
		this.encodeBstr(inst.utf8);
	}

	@Override
	public void visitRSet(RSet inst) {
		this.encodeBset(inst.byteSet);
	}

	@Override
	public void visitDispatch(Dispatch inst) {
		this.encodeJumpTable();
		for (int i = 0; i < inst.jumpTable.length; i++) {
			this.encodeJump(inst.jumpTable[i]);
		}
	}

	@Override
	public void visitDDispatch(DDispatch inst) {
		this.encodeJumpTable();
		for (int i = 0; i < inst.jumpTable.length; i++) {
			this.encodeJump(inst.jumpTable[i]);
		}
	}

	@Override
	public void visitTPush(TPush inst) {
	}

	@Override
	public void visitTPop(TPop inst) {
	}

	@Override
	public void visitTBegin(TBegin inst) {
		this.encodeShift(inst.shift);
	}

	@Override
	public void visitTEnd(TEnd inst) {
		this.encodeShift(inst.shift);
	}

	@Override
	public void visitTTag(TTag inst) {
		this.encodeTag(inst.tag);
	}

	@Override
	public void visitTReplace(TReplace inst) {
		this.encodeBstr(inst.value.getBytes());
	}

	@Override
	public void visitTLink(TLink inst) {
		this.encodeLabel(inst.label);
	}

	@Override
	public void visitTFold(TFold inst) {
		this.encodeShift(inst.shift);
		this.encodeLabel(inst.label);
	}

	@Override
	public void visitTStart(TStart inst) {
	}

	@Override
	public void visitTEmit(TEmit inst) {
		this.encodeLabel(inst.label);
	}

	@Override
	public void visitSOpen(SOpen inst) {
	}

	@Override
	public void visitSClose(SClose inst) {
	}

	@Override
	public void visitSMask(SMask inst) {
		this.encodeTable(inst.table);
	}

	@Override
	public void visitSDef(SDef inst) {
		this.encodeTable(inst.table);
	}

	@Override
	public void visitSExists(SExists inst) {
		this.encodeTable(inst.table);
	}

	@Override
	public void visitSIsDef(SIsDef inst) {
		this.encodeTable(inst.table);
		this.encodeBstr(inst.utf8);
	}

	@Override
	public void visitSMatch(SMatch inst) {
		this.encodeTable(inst.table);
	}

	@Override
	public void visitSIs(SIs inst) {
		this.encodeTable(inst.table);
	}

	@Override
	public void visitSIsa(SIsa inst) {
		this.encodeTable(inst.table);
	}

	@Override
	public void visitNScan(NScan inst) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitNDec(NDec inst) {
		// TODO Auto-generated method stub

	}

	@Override
	public void visitLookup(Lookup inst) {
		this.write_b(inst.state);
		this.write_u32(inst.uid);
		this.encodeJump(inst.jump);
	}

	@Override
	public void visitMemo(Memo inst) {
		this.write_b(inst.state);
		this.write_u32(inst.uid);
	}

	@Override
	public void visitMemoFail(MemoFail inst) {
		this.write_b(inst.state);
		this.write_u32(inst.uid);
	}

	@Override
	public void visitTLookup(TLookup inst) {
		this.write_b(inst.state);
		this.write_u32(inst.uid);
		this.encodeJump(inst.jump);
		this.encodeLabel(inst.label);
	}

	@Override
	public void visitTMemo(TMemo inst) {
		this.write_b(inst.state);
		this.write_u32(inst.uid);
	}

}
