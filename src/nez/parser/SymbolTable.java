package nez.parser;

import nez.ast.Symbol;

public class SymbolTable {
	public final static byte[] NullSymbol = { 0, 0, 0, 0 }; // to distinguish
															// others
	SymbolTableEntry2[] tables;
	int tableSize = 0;
	int maxTableSize = 0;

	public int stateValue = 0;
	int stateCount = 0;

	final class SymbolTableEntry2 {
		int stateValue;
		Symbol table;
		long code;
		byte[] symbol; // if uft8 is null, hidden

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			sb.append(stateValue);
			sb.append(", ");
			sb.append(table);
			sb.append(", ");
			sb.append((symbol == null) ? "<masked>" : new String(symbol));
			sb.append("]");
			return sb.toString();
		}
	}

	final static long hash(byte[] utf8) {
		long hashCode = 1;
		for (int i = 0; i < utf8.length; i++) {
			hashCode = hashCode * 31 + (utf8[i] & 0xff);
		}
		return hashCode;
	}

	public static final boolean equals(byte[] utf8, byte[] b) {
		if (utf8.length == b.length) {
			for (int i = 0; i < utf8.length; i++) {
				if (utf8[i] != b[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private void initEntry(int s, int e) {
		for (int i = s; i < e; i++) {
			this.tables[i] = new SymbolTableEntry2();
		}
	}

	private void push(Symbol table, long code, byte[] utf8) {
		if (!(tableSize < maxTableSize)) {
			if (maxTableSize == 0) {
				maxTableSize = 128;
				this.tables = new SymbolTableEntry2[128];
				initEntry(0, maxTableSize);
			} else {
				maxTableSize *= 2;
				SymbolTableEntry2[] newtable = new SymbolTableEntry2[maxTableSize];
				System.arraycopy(this.tables, 0, newtable, 0, tables.length);
				this.tables = newtable;
				initEntry(tables.length / 2, maxTableSize);
			}
		}
		SymbolTableEntry2 entry = tables[tableSize];
		tableSize++;
		if (entry.table == table && equals(entry.symbol, utf8)) {
			// reuse state value
			entry.code = code;
			this.stateValue = entry.stateValue;
		} else {
			entry.table = table;
			entry.code = code;
			entry.symbol = utf8;
			this.stateCount += 1;
			this.stateValue = stateCount;
			entry.stateValue = stateCount;
		}
	}

	public final int savePoint() {
		return this.tableSize;
	}

	public final void rollBack(int savePoint) {
		if (this.tableSize != savePoint) {
			this.tableSize = savePoint;
			if (this.tableSize == 0) {
				this.stateValue = 0;
			} else {
				this.stateValue = tables[savePoint - 1].stateValue;
			}
		}
	}

	public final int getState() {
		return this.stateValue;
	}

	public final void addSymbol(Symbol table, byte[] utf8) {
		push(table, hash(utf8), utf8);
	}

	public final void addSymbolMask(Symbol table) {
		push(table, 0, NullSymbol);
	}

	public final boolean exists(Symbol table) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];

			if (entry.table == table) {
				return entry.symbol != null;
			}
		}
		return false;
	}

	public final boolean exists(Symbol table, byte[] symbol) {
		long code = hash(symbol);
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == null)
					return false; // masked
				if (entry.code == code && equals(entry.symbol, symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	public final boolean exists(Symbol table, Symbol xtable, byte[] symbol) {
		long code = hash(symbol);
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if (entry.table == xtable) {
				if (entry.symbol == null) {
					xtable = null;
					continue;
				}
				if (entry.code == code && equals(entry.symbol, symbol)) {
					return false;
				}
				continue;
			}
			if (entry.table == table) {
				if (entry.symbol == null)
					return false; // masked
				if (entry.code == code && equals(entry.symbol, symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	public final byte[] getSymbol(Symbol table) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if (entry.table == table) {
				return entry.symbol;
			}
		}
		return null;
	}

	public final boolean contains(Symbol table, byte[] symbol) {
		long code = hash(symbol);
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == NullSymbol) {
					return false; // masked
				}
				if (entry.code == code && equals(entry.symbol, symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	public final void setCount(Symbol table, int number) {
		push(table, number, NullSymbol);
	}

	public final boolean count(Symbol table) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry2 entry = tables[i];
			if (entry.table == table) {
				if (entry.code == 0)
					return false;
				entry.code--;
				return true;
			}
		}
		return false;
	}
}
