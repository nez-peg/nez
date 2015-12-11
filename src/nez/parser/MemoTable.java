package nez.parser;

import java.util.HashMap;

public abstract class MemoTable {

	public abstract void setMemo(long pos, int memoPoint, boolean failed, Object result, int consumed, int stateValue);

	public abstract MemoEntry getMemo(long pos, int memoPoint);

	public abstract MemoEntry getMemo2(long pos, int memoPoint, int stateValue);

	int CountStored;
	int CountUsed;
	int CountInvalidated;

	void initStat() {
		this.CountStored = 0;
		this.CountUsed = 0;
		this.CountInvalidated = 0;
	}

	public static MemoTable newTable(int windowSize, int memoPointSize) {
		if (memoPointSize == 0 || windowSize == 0) {
			return new NullTable(windowSize, memoPointSize);
		}
		if (windowSize < -1) {
			return new PackratHashTable(windowSize, memoPointSize);
		}
		return new ElasticTable(windowSize, memoPointSize);
	}

	public void record(ParserProfier rec) {
		rec.setText("M.TableType", this.getClass().getSimpleName());
		rec.setCount("M.MemoStored", this.CountStored);
		rec.setRatio("M.MemoHit", this.CountUsed, this.CountStored);
		rec.setCount("M.Invalidated", this.CountInvalidated);
	}

}

class NullTable extends MemoTable {

	NullTable(int w, int n) {
		this.initStat();
	}

	@Override
	public void setMemo(long pos, int memoPoint, boolean failed, Object result, int consumed, int stateValue) {
		this.CountStored += 1;
	}

	@Override
	public MemoEntry getMemo(long pos, int id) {
		return null;
	}

	@Override
	public MemoEntry getMemo2(long pos, int id, int stateValue) {
		return null;
	}
}

class ElasticTable extends MemoTable {
	private MemoEntryKey[] memoArray;
	private final int shift;

	ElasticTable(int w, int n) {
		this.memoArray = new MemoEntryKey[w * n + 1];
		for (int i = 0; i < this.memoArray.length; i++) {
			this.memoArray[i] = new MemoEntryKey();
			this.memoArray[i].key = -1;
		}
		this.shift = (int) (Math.log(n) / Math.log(2.0)) + 1;
		this.initStat();
	}

	final long longkey(long pos, int memoPoint, int shift) {
		return ((pos << shift) | memoPoint) & Long.MAX_VALUE;
	}

	@Override
	public void setMemo(long pos, int memoPoint, boolean failed, Object result, int consumed, int stateValue) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntryKey m = this.memoArray[hash];
		m.key = key;
		m.failed = failed;
		m.result = result;
		m.consumed = consumed;
		m.stateValue = stateValue;
		this.CountStored += 1;
	}

	@Override
	public final MemoEntry getMemo(long pos, int memoPoint) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntryKey m = this.memoArray[hash];
		if (m.key == key) {
			this.CountUsed += 1;
			return m;
		}
		return null;
	}

	@Override
	public final MemoEntry getMemo2(long pos, int memoPoint, int stateValue) {
		long key = longkey(pos, memoPoint, shift);
		int hash = (int) (key % memoArray.length);
		MemoEntryKey m = this.memoArray[hash];
		if (m.key == key) {
			if (m.stateValue == stateValue) {
				this.CountUsed += 1;
				return m;
			}
			this.CountInvalidated += 1;
		}
		return null;
	}

}

class PackratHashTable extends MemoTable {
	HashMap<Long, MemoEntryList> memoMap;
	private MemoEntryList UnusedMemo = null;

	PackratHashTable(int w, int n) {
		this.memoMap = new HashMap<Long, MemoEntryList>(w * n);
	}

	private final MemoEntryList newMemo() {
		if (UnusedMemo != null) {
			MemoEntryList m = this.UnusedMemo;
			this.UnusedMemo = m.next;
			return m;
		} else {
			return new MemoEntryList();
		}
	}

	protected final void unusedMemo(MemoEntryList m) {
		MemoEntryList s = m;
		while (m.next != null) {
			m = m.next;
		}
		m.next = this.UnusedMemo;
		UnusedMemo = s;
	}

	@Override
	public MemoEntry getMemo(long pos, int memoPoint) {
		MemoEntryList m = this.memoMap.get(pos);
		while (m != null) {
			if (m.memoPoint == memoPoint) {
				this.CountUsed += 1;
				return m;
			}
			m = m.next;
		}
		return m;
	}

	@Override
	public MemoEntry getMemo2(long pos, int memoPoint, int stateValue) {
		MemoEntryList m = this.memoMap.get(pos);
		while (m != null) {
			if (m.memoPoint == memoPoint) {
				if (m.stateValue == stateValue) {
					this.CountUsed += 1;
					return m;
				}
				this.CountInvalidated += 1;
			}
			m = m.next;
		}
		return m;
	}

	@Override
	public void setMemo(long pos, int memoPoint, boolean failed, Object result, int consumed, int stateValue) {
		MemoEntryList m = newMemo();
		m.failed = failed;
		m.memoPoint = memoPoint;
		m.stateValue = stateValue;
		m.result = result;
		m.consumed = consumed;
		Long key = pos;
		m.next = this.memoMap.get(key);
		this.memoMap.put(key, m);
		this.CountStored += 1;
	}

}
