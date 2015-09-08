package nez.vm;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import nez.ast.SymbolId;
import nez.main.Verbose;
import nez.util.StringUtils;

public class ByteCoder {

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
		SymbolId data;

		TagEntry(int id, SymbolId data) {
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

	public void setInstructions(Instruction[] insts, int len) {
		stream = new ByteArrayOutputStream();
		for (int i = 0; i < len; i++) {
			if (insts[i] != null) {
				assert (insts[i].id == i);
				insts[i].encode(this);
			} else {
				encodeOpcode(InstructionSet.Nop);
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

	private void encodeData(SymbolId tag) {
		write_utf8(StringUtils.toUtf8(tag.getSymbol()));
	}

	//

	public void encodeOpcode(byte opcode) {
		stream.write(opcode);
	}

	public final void encodeJumpTable() {
		this.jumpTableSize += 1;
	}

	public final void encodeJumpAddr(Instruction jump) {
		write_u24(jump.id);
	}

	public void encodeShift(int shift) {
		write_i8(shift);
	}

	public void encodeIndex(int index) {
		write_i8(index);
	}

	public void encodeByteChar(int byteChar) {
		stream.write(byteChar);
	}

	public void encodeByteMap(boolean[] byteMap) {
		String key = StringUtils.stringfyBitmap(byteMap);
		SetEntry entry = BSetPoolMap.get(key);
		if (entry == null) {
			entry = new SetEntry(BSetPoolMap.size(), byteMap);
			BSetPoolMap.put(key, entry);
			BSetPools.add(entry);
		}
		write_u16(entry.id);
	}

	public void encodeMultiByte(byte[] utf8) {
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

	public void encodeTag(SymbolId tag) {
		String key = tag.getSymbol();
		TagEntry entry = TagPoolMap.get(key);
		if (entry == null) {
			entry = new TagEntry(TagPoolMap.size(), tag);
			TagPoolMap.put(key, entry);
			TagPools.add(entry);
		}
		write_u16(entry.id);
	}

	public void encodeLabel(SymbolId label) {
		if (label == null) {
			this.encodeTag(SymbolId.NullTag);
		} else {
			this.encodeTag(label);
		}
	}

	public void encodeSymbolTable(SymbolId tableName) {
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

}
