package nez.vm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
	int memoSize;
	ByteArrayOutputStream stream;
	HashMap<String, SetEntry> setMap;
	HashMap<String, StrEntry> strMap;
	HashMap<String, TagEntry> tagMap;
	HashMap<String, TagEntry> tabMap;  // tableEntry
	HashMap<String, SymEntry> symMap;
	ArrayList<SetEntry> setList;
	ArrayList<StrEntry> strList;
	ArrayList<TagEntry> tagList;
	ArrayList<TagEntry> tabList;  // tableEntry
	ArrayList<SymEntry> symList;
	
	public void setHeader(int instSize, int prodSize, int memoSize) {
		this.instSize = instSize;
		this.prodSize = prodSize;
		this.memoSize = memoSize;
		setMap = new HashMap<>();
		strMap = new HashMap<>();
		tagMap = new HashMap<>();
		tabMap = new HashMap<>();
		symMap = new HashMap<>();
		setList = new ArrayList<>();
		strList = new ArrayList<>();
		tagList = new ArrayList<>();
		tabList = new ArrayList<>();
		symList = new ArrayList<>();
	}

	public byte[] generate(Instruction[] insts) {
		stream = new ByteArrayOutputStream();
		for(int i = 0; i < insts.length; i++) {
			if(insts[i] != null) {
				assert(insts[i].id == i);
				insts[i].encode(this);
			}
			else {
				encodeOpcode(InstructionSet.Nop);
			}
		}

		byte[] body = stream.toByteArray();
		stream = new ByteArrayOutputStream();
		stream.write('N');
		stream.write('E');
		stream.write('Z');
		stream.write('0');
		encodeShort(instSize);
		encodeShort(prodSize);
		encodeShort(memoSize);
		encodeShort(setList.size());
		for(SetEntry e: setList) {
			encodeData(e.data);
		}
		encodeShort(strList.size());
		for(StrEntry e: strList) {
			encodeData(e.data);
		}
		encodeShort(tagList.size());
		for(TagEntry e: tagList) {
			encodeData(e.data);
		}
		encodeShort(tabList.size());
		for(TagEntry e: tabList) {
			encodeData(e.data);
		}
		encodeShort(symList.size());
		for(SymEntry e: symList) {
			encodeShort(e.tabid);
			encodeData(e.symbol);
		}
		try {
			stream.write(body);
		} catch (IOException e1) {
			Verbose.traceException(e1);
		}
		return stream.toByteArray();
	}
		
	public void encodeBoolean(boolean b) {
		stream.write(b ? 1: 0);
	}

	public void encodeByte(int num) {
		stream.write(num);
	}

	public void encodeShort(int num) {
		int n1 = num % 256;
		int n0 = num / 256;
		stream.write(n0);
		stream.write(n1);
	}

	public void encodeInt(int num) {
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
		encodeInt(n);
	}

	private void encodeData(byte[] utf8) {
		encodeShort(utf8.length);
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
		encodeInt(jump.id);
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
		SetEntry entry = setMap.get(key);
		if(entry == null) {
			entry = new SetEntry(setMap.size(), byteMap);
			setMap.put(key, entry);
		}
		encodeShort(entry.id);
	}


	public void encodeMultiByte(byte[] utf8) {
		try {
			String key = new String(utf8, StringUtils.DefaultEncoding);
			StrEntry entry = strMap.get(key);
			if(entry == null) {
				entry = new StrEntry(strMap.size(), utf8);
				strMap.put(key, entry);
			}
			encodeShort(entry.id);
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
		TagEntry entry = tagMap.get(key);
		if(entry == null) {
			entry = new TagEntry(tagMap.size(), tag);
			tagMap.put(key, entry);
		}
		encodeShort(entry.id);
	}

	public void encodeSymbolTable(Tag tableName) {
		String key = tableName.getName();
		TagEntry entry = tabMap.get(key);
		if(entry == null) {
			entry = new TagEntry(tabMap.size(), tableName);
			tabMap.put(key, entry);
		}
		encodeShort(entry.id);
	}

	public void encodeSymbol(Tag tableName, String symbol) {
		String key = tableName.getName();
		TagEntry entry = tabMap.get(key);
		if(entry == null) {
			entry = new TagEntry(tabMap.size(), tableName);
			tabMap.put(key, entry);
		}
		encodeShort(entry.id);
		// TODO
	}

}
