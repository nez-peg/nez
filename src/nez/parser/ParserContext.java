package nez.parser;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.parser.io.StringSource;
import nez.util.StringUtils;
import nez.util.Verbose;

public class ParserContext<T extends Tree<T>> {
	public int pos = 0;
	public T left;

	public ParserContext(String s, T proto) {
		StringSource source = new StringSource(s);
		this.source = source;
		inputs = source.inputs;
		length = inputs.length - 1;
		this.pos = 0;
		this.left = proto;
	}

	protected ParserContext(Source s, T proto) {
		source = s;
		inputs = null;
		length = 0;
		this.pos = 0;
		this.left = proto;
	}

	protected Source source;
	private byte[] inputs;
	private int length;

	public boolean eof() {
		return !(pos < length);
	}

	public int read() {
		return inputs[pos++] & 0xff;
	}

	public int prefetch() {
		return inputs[pos] & 0xff;
	}

	public final void move(int shift) {
		pos += shift;
	}

	public void back(int pos) {
		this.pos = pos;
	}

	public boolean match(byte[] text) {
		int len = text.length;
		if (pos + len > this.length) {
			return false;
		}
		for (int i = 0; i < len; i++) {
			if (text[i] != this.inputs[pos + i]) {
				return false;
			}
		}
		pos += len;
		return true;
	}

	public byte[] subByte(int startIndex, int endIndex) {
		byte[] b = new byte[endIndex - startIndex];
		System.arraycopy(this.inputs, (startIndex), b, 0, b.length);
		return b;
	}

	protected byte byteAt(int n) {
		return inputs[n];
	}

	// AST

	private enum Operation {
		Link, Tag, Replace, New;
	}

	static class TreeLog {
		Operation op;
		int pos;
		Object value;
		Object tree;
	}

	private TreeLog[] logs = new TreeLog[0];
	private int unused_log = 0;

	private void log2(Operation op, int pos, Object value, T tree) {
		if (!(unused_log < logs.length)) {
			TreeLog[] newlogs = new TreeLog[logs.length + 1024];
			System.arraycopy(logs, 0, newlogs, 0, logs.length);
			for (int i = logs.length; i < newlogs.length; i++) {
				newlogs[i] = new TreeLog();
			}
			logs = newlogs;
		}
		TreeLog l = logs[unused_log];
		l.op = op;
		l.pos = pos;
		l.value = value;
		l.tree = tree;
		this.unused_log++;
	}

	public final void beginTree(int shift) {
		log2(Operation.New, pos + shift, null, null);
	}

	public final void linkTree(T parent, Symbol label) {
		log2(Operation.Link, 0, label, left);
	}

	public final void tagTree(Symbol tag) {
		log2(Operation.Tag, 0, tag, null);
	}

	public final void valueTree(String value) {
		log2(Operation.Replace, 0, value, null);
	}

	public final void foldTree(int shift, Symbol label) {
		log2(Operation.New, pos + shift, null, null);
		log2(Operation.Link, 0, label, left);
	}

	public final void endTree(int shift, Symbol tag, String value) {
		int objectSize = 0;
		TreeLog start = null;
		int start_index = 0;
		for (int i = unused_log - 1; i >= 0; i--) {
			TreeLog l = logs[i];
			if (l.op == Operation.Link) {
				objectSize++;
				continue;
			}
			if (l.op == Operation.New) {
				start = l;
				start_index = i;
				break;
			}
			if (l.op == Operation.Tag && tag == null) {
				tag = (Symbol) l.value;
			}
			if (l.op == Operation.Replace && value == null) {
				value = (String) l.value;
			}
		}
		left = newTree(tag, start.pos, (pos + shift), objectSize, value);
		if (objectSize > 0) {
			int n = 0;
			for (int j = start_index; j < unused_log; j++) {
				TreeLog l = logs[j];
				if (l.op == Operation.Link) {
					left.link(n++, (Symbol) l.value, l.tree);
					l.tree = null;
				}
			}
		}
		this.backLog(start_index);
	}

	public final T newTree(Symbol tag, int start, int end, int n, String value) {
		if (tag == null) {
			tag = Symbol.Null;
		}
		return left.newInstance(tag, source, start, (end - start), n, value);
	}

	public final int saveLog() {
		return unused_log;
	}

	public final void backLog(int log) {
		if (this.unused_log > log) {
			this.unused_log = log;
		}
	}

	public final T saveTree() {
		return this.left;
	}

	public final void backTree(T tree) {
		this.left = tree;
	}

	// Symbol Table ---------------------------------------------------------

	private final static byte[] NullSymbol = { 0, 0, 0, 0 }; // to distinguish

	// others
	private SymbolTableEntry[] tables = new SymbolTableEntry[0];
	private int tableSize = 0;

	private int stateValue = 0;
	private int stateCount = 0;

	static final class SymbolTableEntry {
		int stateValue;
		Symbol table;
		long code;
		byte[] symbol; // if uft8 is null, hidden

		// @Override
		// public String toString() {
		// StringBuilder sb = new StringBuilder();
		// sb.append('[');
		// sb.append(stateValue);
		// sb.append(", ");
		// sb.append(table);
		// sb.append(", ");
		// sb.append((symbol == null) ? "<masked>" : new String(symbol));
		// sb.append("]");
		// return sb.toString();
		// }
	}

	private final static long hash(byte[] utf8, int ppos, int pos) {
		long hashCode = 1;
		for (int i = ppos; i < pos; i++) {
			hashCode = hashCode * 31 + (utf8[i] & 0xff);
		}
		return hashCode;
	}

	private final static boolean equalsBytes(byte[] utf8, byte[] b) {
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

	private void push(Symbol table, long code, byte[] utf8) {
		if (!(tableSize < tables.length)) {
			SymbolTableEntry[] newtable = new SymbolTableEntry[tables.length + 256];
			System.arraycopy(this.tables, 0, newtable, 0, tables.length);
			for (int i = tables.length; i < newtable.length; i++) {
				newtable[i] = new SymbolTableEntry();
			}
			this.tables = newtable;
		}
		SymbolTableEntry entry = tables[tableSize];
		tableSize++;
		if (entry.table == table && equalsBytes(entry.symbol, utf8)) {
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

	public final int saveSymbolPoint() {
		return this.tableSize;
	}

	public final void backSymbolPoint(int savePoint) {
		if (this.tableSize != savePoint) {
			this.tableSize = savePoint;
			if (this.tableSize == 0) {
				this.stateValue = 0;
			} else {
				this.stateValue = tables[savePoint - 1].stateValue;
			}
		}
	}

	public final void addSymbol(Symbol table, int ppos) {
		byte[] b = this.subByte(ppos, pos);
		push(table, hash(b, 0, b.length), b);
	}

	public final void addSymbolMask(Symbol table) {
		push(table, 0, NullSymbol);
	}

	public final boolean exists(Symbol table) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				return entry.symbol != NullSymbol;
			}
		}
		return false;
	}

	public final boolean existsSymbol(Symbol table, byte[] symbol) {
		long code = hash(symbol, 0, symbol.length);
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == NullSymbol) {
					return false; // masked
				}
				if (entry.code == code && equalsBytes(entry.symbol, symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	public final boolean matchSymbol(Symbol table) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == NullSymbol) {
					return false; // masked
				}
				return this.match(entry.symbol);
			}
		}
		return false;
	}

	private final long hashInputs(int ppos, int pos) {
		long hashCode = 1;
		for (int i = ppos; i < pos; i++) {
			hashCode = hashCode * 31 + (byteAt(i) & 0xff);
		}
		return hashCode;
	}

	private final boolean equalsInputs(int ppos, int pos, byte[] b2) {
		if ((pos - ppos) == b2.length) {
			for (int i = 0; i < b2.length; i++) {
				if (byteAt(ppos + i) != b2[i]) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public final boolean equals(Symbol table, int ppos) {
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == NullSymbol) {
					return false; // masked
				}
				return equalsInputs(ppos, pos, entry.symbol);
			}
		}
		return false;
	}

	public boolean contains(Symbol table, int ppos) {
		long code = hashInputs(ppos, pos);
		for (int i = tableSize - 1; i >= 0; i--) {
			SymbolTableEntry entry = tables[i];
			if (entry.table == table) {
				if (entry.symbol == NullSymbol) {
					return false; // masked
				}
				if (code == entry.code && equalsInputs(ppos, pos, entry.symbol)) {
					return true;
				}
			}
		}
		return false;
	}

	// Counter ------------------------------------------------------------

	private int count = 0;

	public final void scanCount(int ppos, long mask, int shift) {
		if (mask == 0) {
			String num = StringUtils.newString(subByte(ppos, pos));
			count = (int) Long.parseLong(num);
		} else {
			long v = 0;
			for (int i = ppos; i < pos; i++) {
				int n = this.byteAt(i) & 0xff;
				v <<= 8;
				v |= n;
			}
			v = v & mask;
			count = (int) ((v & mask) >> shift);
		}
		Verbose.println("set count %d mask=%s, shift=%s", count, mask, shift);
	}

	public final boolean decCount() {
		return count-- > 0;
	}

	// Memotable ------------------------------------------------------------

	public final static int NotFound = 0;
	public final static int SuccFound = 1;
	public final static int FailFound = 2;

	private static class MemoEntry<E extends Tree<E>> {
		long key = -1;
		public int consumed;
		public E memoTree;
		public int result;
		public int stateValue = 0;
	}

	private MemoEntry<T>[] memoArray = null;
	private int shift = 0;

	@SuppressWarnings("unchecked")
	public void initMemoTable(int w, int n) {
		this.memoArray = new MemoEntry[w * n + 1];
		for (int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntry<T>();
			this.memoArray[i].key = -1;
			this.memoArray[i].result = NotFound;
		}
		this.shift = (int) (Math.log(n) / Math.log(2.0)) + 1;
		// this.initStat();
	}

	final long longkey(long pos, int memoPoint, int shift) {
		return ((pos << shift) | memoPoint) & Long.MAX_VALUE;
	}

	public final int lookupMemo(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		if (m.key == key) {
			this.pos += m.consumed;
			return m.result;
		}
		return NotFound;
	}

	public final int lookupTreeMemo(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		if (m.key == key) {
			this.pos += m.consumed;
			this.left = m.memoTree;
			return m.result;
		}
		return NotFound;
	}

	public void memoSucc(int memoPoint, int ppos) {
		long key = longkey(ppos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = left;
		m.consumed = pos - ppos;
		m.result = SuccFound;
		m.stateValue = -1;
		// this.CountStored += 1;
	}

	public void memoTreeSucc(int memoPoint, int ppos) {
		long key = longkey(ppos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = left;
		m.consumed = pos - ppos;
		m.result = SuccFound;
		m.stateValue = -1;
		// this.CountStored += 1;
	}

	public void memoFail(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = left;
		m.consumed = 0;
		m.result = FailFound;
		m.stateValue = -1;
	}

	/* State Version */

	public final int lookupStateMemo(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		if (m.key == key) {
			this.pos += m.consumed;
			return m.result;
		}
		return NotFound;
	}

	public final int lookupStateTreeMemo(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		if (m.key == key && m.stateValue == this.stateValue) {
			this.pos += m.consumed;
			this.left = m.memoTree;
			return m.result;
		}
		return NotFound;
	}

	public void memoStateSucc(int memoPoint, int ppos) {
		long key = longkey(ppos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = left;
		m.consumed = pos - ppos;
		m.result = SuccFound;
		m.stateValue = this.stateValue;
		// this.CountStored += 1;
	}

	public void memoStateTreeSucc(int memoPoint, int ppos) {
		long key = longkey(ppos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = left;
		m.consumed = pos - ppos;
		m.result = SuccFound;
		m.stateValue = this.stateValue;
		// this.CountStored += 1;
	}

	public void memoStateFail(int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntry<T> m = this.memoArray[hash];
		m.key = key;
		m.memoTree = left;
		m.consumed = 0;
		m.result = FailFound;
		m.stateValue = this.stateValue;
	}

}
