package nez.vm;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import nez.ast.Tag;
import nez.main.Verbose;
import nez.util.StringUtils;

public class ByteCoder {

	class SetEntry {
		int id; boolean[] data; 
		SetEntry(int id, boolean[] data) { 
			this.id = id;
			this.data = data;
		}
	}
	class StrEntry {
		int id; byte[] data; 
		StrEntry(int id, byte[] data) { 
			this.id = id;
			this.data = data;
		}
	}
	class TagEntry {
		int id; Tag data; 
		TagEntry(int id, Tag data) { 
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
	HashMap<String, TagEntry> TablePoolMap;  // tableEntry
	ArrayList<StrEntry> NonTerminalPools;
	ArrayList<SetEntry> BSetPools;
	ArrayList<StrEntry> BStrPools;
	ArrayList<TagEntry> TagPools;
	ArrayList<TagEntry> TablePools;  // tableEntry
	
	public void setHeader(int instSize, int prodSize, int memoSize) {
		this.instSize = instSize;
		this.prodSize = prodSize;
		this.memoSize = memoSize;
		NonTerminalPoolMap = new HashMap<>();
		BSetPoolMap = new HashMap<>();
		BStrPoolMap = new HashMap<>();
		TagPoolMap = new HashMap<>();
		TablePoolMap = new HashMap<>();
//		SymbolTableMap = new HashMap<>();
		NonTerminalPools = new ArrayList<>();
		BSetPools = new ArrayList<>();
		BStrPools = new ArrayList<>();
		TagPools = new ArrayList<>();
		TablePools = new ArrayList<>();
//		SymbolTablePools = new ArrayList<>();
	}

	public void setInstructions(Instruction[] insts, int len) {
		stream = new ByteArrayOutputStream();
		for(int i = 0; i < len; i++) {
			if(insts[i] != null) {
				assert(insts[i].id == i);
				insts[i].encode(this);
			}
			else {
				encodeOpcode(InstructionSet.Nop);
			}
		}
	}
		
	public void encodeBoolean(boolean b) {
		stream.write(b ? 1: 0);
	}

	public void encodeByte(int num) {
		stream.write(num);
	}

	public void write_u16(int num) {
		int n1 = num % 256;
		int n0 = num / 256;
		stream.write(n0);
		stream.write(n1);
	}

	public void write_u32(int num) {
		int n3 = num % 256;
		num = num / 256;
		int n2 = num % 256;
		num = num / 256;
		int n1 = num % 256;
		int n0 = num / 256;
		stream.write(n0);
		stream.write(n1);
		stream.write(n2);
		stream.write(n3);
	}

	private void encodeData(boolean[] byteMap) {
		for(int i = 0; i < 256; i+=32) {
			encodeByteMap(byteMap, i);
		}
	}

	private void encodeByteMap(boolean[] b, int offset) {
		int n = 0;
		for(int i = 0; i < 32; i++) {
			if(b[offset+i]) {
				n |= (1 << (31-i));
			}
		}
		write_u32(n);
	}

	private void encodeData(byte[] utf8) {
		write_u16(utf8.length);
		try {
			stream.write(utf8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void encodeData(String str) {
		encodeData(StringUtils.toUtf8(str));
	}

	private void encodeData(Tag tag) {
		encodeData(StringUtils.toUtf8(tag.getName()));
	}


	//
	
	public void encodeOpcode(byte opcode) {
		stream.write(opcode);
	}

	public final void encodeJumpAddr(Instruction jump) {
		write_u32(jump.id);
	}

	public void encodeShift(int shift) {
		stream.write(shift);
	}
	
	public void encodeIndex(int index) {
		stream.write(index);
	}

	public void encodeByteChar(int byteChar) {
		stream.write(byteChar);
	}

	public void encodeByteMap(boolean[] byteMap) {
		String key = StringUtils.stringfyBitmap(byteMap);
		SetEntry entry = BSetPoolMap.get(key);
		if(entry == null) {
			entry = new SetEntry(BSetPoolMap.size(), byteMap);
			BSetPoolMap.put(key, entry);
		}
		write_u16(entry.id);
	}


	public void encodeMultiByte(byte[] utf8) {
		try {
			String key = new String(utf8, StringUtils.DefaultEncoding);
			StrEntry entry = BStrPoolMap.get(key);
			if(entry == null) {
				entry = new StrEntry(BStrPoolMap.size(), utf8);
				BStrPoolMap.put(key, entry);
			}
			write_u16(entry.id);
		}
		catch(IOException e) {
			Verbose.traceException(e);
		}
	}

	public void encodeLabel(String localName) {
		encodeData(localName);
	}
	
	public void encodeTag(Tag tag) {
		String key = tag.getName();
		TagEntry entry = TagPoolMap.get(key);
		if(entry == null) {
			entry = new TagEntry(TagPoolMap.size(), tag);
			TagPoolMap.put(key, entry);
		}
		write_u16(entry.id);
	}

	public void encodeSymbolTable(Tag tableName) {
		String key = tableName.getName();
		TagEntry entry = TablePoolMap.get(key);
		if(entry == null) {
			entry = new TagEntry(TablePoolMap.size(), tableName);
			TablePoolMap.put(key, entry);
		}
		write_u16(entry.id);
	}

	public void encodeSymbol(Tag tableName, String symbol) {
		String key = tableName.getName();
		TagEntry entry = TablePoolMap.get(key);
		if(entry == null) {
			entry = new TagEntry(TablePoolMap.size(), tableName);
			TablePoolMap.put(key, entry);
		}
		write_u16(entry.id);
		// TODO
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
		for(StrEntry e: NonTerminalPools) {
			encodeData(e.data);
		}
		write_u16(BSetPools.size());
		for(SetEntry e: BSetPools) {
			encodeData(e.data);
		}
		write_u16(BStrPools.size());
		for(StrEntry e: BStrPools) {
			encodeData(e.data);
		}
		write_u16(TagPools.size());
		for(TagEntry e: TagPools) {
			encodeData(e.data);
		}
		write_u16(TablePools.size());
		for(TagEntry e: TablePools) {
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
		}
		catch(IOException e) {
			Verbose.traceException(e);
		}
	}

}
