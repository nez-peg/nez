package nez.vm;

import nez.ast.Tag;
import nez.util.UList;

public class SymbolTable {
	public final static byte[] NullSymbol = {0,0,0,0}; // to distinguish others
	SymbolTableEntry2[] tables;
	int tableSize = 0;
	int maxTableSize = 0;

	public int stateValue = 0;
	int stateCount = 0;

	final class SymbolTableEntry2 {
		Tag     table;
		long    code;
		byte[]  utf8;
		boolean avail;   // if hidden, avail = false
	}
	
	final static long hashCode(byte[] utf8) {
		long hashCode = 0;
		for (int i = 0; i < utf8.length; i++) {
			hashCode = hashCode * 31 + (utf8[i] & 0xff);
		}
		return hashCode;
	}
	
	final boolean equals(byte[] utf8, byte[] b) {
		if(utf8.length == b.length) {
			for(int i = 0; i < utf8.length; i++) {
				if(utf8[i] != b[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	
	private void initEntry(int s, int e) {
		for(int i = s; i < e; i++) {
			this.tables[i] = new SymbolTableEntry2();
		}
	}
	
	private void push(Tag table, long code, byte[] utf8) {
		if(!(tableSize < maxTableSize)) {
			if(maxTableSize == 0) {
				maxTableSize = 128;
				this.tables = new SymbolTableEntry2[128];
				initEntry(0, maxTableSize);
			}
			else {
				maxTableSize *= 2;
				SymbolTableEntry2[] newtable = new SymbolTableEntry2[maxTableSize];
				System.arraycopy(this.tables, 0, newtable, 0, tables.length);
				this.tables = newtable;
				initEntry(tables.length, maxTableSize);
			}
		}
		SymbolTableEntry2 entry = tables[tableSize];
		tableSize++;
		entry.table = table;
		entry.code  = code;
		entry.utf8  = utf8;
		entry.avail = true;
		this.stateCount += 1;
		this.stateValue = stateCount;
	}

	public final int savePoint() {
		push(null, this.stateValue, NullSymbol);
		return this.tableSize - 1;
	}

	public final int saveHiddenPoint(Tag table) {
		push(table, this.stateValue, NullSymbol);
		this.tables[this.tableSize-1].avail = false;
		return this.tableSize - 1;
	}

	public final void rollBack(int savePoint) {
		this.stateValue = (int)tables[savePoint].code;
		this.tableSize = savePoint;
	}
		
	public final int getState() {
		return this.stateValue;
	}
	
	public final void addTable(Tag table, byte[] utf8) {
		push(table, hashCode(utf8), utf8);
	}

	public final boolean exists(Tag table) {
		for(int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if(entry.table == table && entry.avail ) {
				return true;
			}
		}
		return false;
	}

	public final byte[] getSymbol(Tag table) {
		for(int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if(entry.table == table && entry.avail ) {
				return entry.utf8;
			}
		}
		return null;
	}

	public final boolean contains(Tag table, byte[] s) {
		long code = hashCode(s);
		for(int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if(entry.table == table && entry.code == code && equals(entry.utf8, s)) {
				return entry.avail;
			}
		}
		return false;
	}

	public final boolean contains2(Tag table, byte[] s) {
		long code = hashCode(s);
		for(int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if(entry.code == code && equals(entry.utf8, s)) {
				if(entry.table == table && entry.avail) {
					return true;
				}
				if(entry.table != table && entry.avail) {
					return false;
				}
			}
		}
		return false;
	}
	
	public final void setCount(Tag table, int number) {
		push(table, number, NullSymbol);
	}

	public final boolean count(Tag table) {
		for(int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if(entry.table == table && entry.avail ) {
				if(entry.code == 0) return false;
				entry.code--;
				return true;
			}
		}
		return false;
	}
}
