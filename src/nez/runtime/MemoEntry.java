package nez.runtime;


class MemoEntry {
	boolean failed;
	int  consumed;
	Object result;
//	int  memoPoint;
	int  stateValue = 0;
}

class MemoEntryKey extends MemoEntry {
	long key = -1;
}

class MemoEntryList extends MemoEntry {
	int memoPoint;
	MemoEntryList next;
}