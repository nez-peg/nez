package nez.dfa;

import java.util.ArrayList;

public class BitSet implements Comparable<BitSet> {
	private ArrayList<Integer> arr = null;
	private int size = 0;
	private final int LEN = 31;

	public BitSet() {
		arr = new ArrayList<Integer>();
	}

	public BitSet(ArrayList<Integer> arr, int size) {
		this.arr = arr;
		this.size = size;
	}

	public int arrSize() {
		return arr.size();
	}

	public int arrGet(int i) {
		return arr.get(i);
	}

	public int size() {
		return size;
	}

	public boolean get(int i) {
		if (i >= size) {
			return false;
		}
		int arrID = i / LEN;
		int ID = i % LEN;
		return ((arr.get(arrID) >> ID) & 1) >= 1;
	}

	public void remove(int i) {
		int arrID = i / LEN;
		int ID = i % LEN;
		int v = arr.get(arrID);
		v = v & (~(1 << ID));
		arr.set(arrID, v);
	}

	public void add(int i) {
		if (size <= i) {
			while (arr.size() < i / LEN + 1) {
				arr.add(0);
			}
			size = i + 1;
		}
		int arrID = i / LEN;
		int ID = i % LEN;
		int tmp = arr.get(arrID);
		tmp |= (1 << ID);

		arr.set(arrID, tmp);
	}

	public ArrayList<Integer> toArrayList() {
		ArrayList<Integer> set = new ArrayList<Integer>();
		for (int i = 0; i < size; i++) {
			int arrID = i / LEN;
			int ID = i % LEN;
			if (((arr.get(arrID) >> ID) & 1) >= 1) {
				set.add(i);
			}
		}
		return set;
	}

	public BitSet copy() {
		ArrayList<Integer> newArr = new ArrayList<Integer>();
		int newSize = size;
		for (int i = 0; i < arr.size(); i++) {
			newArr.add(new Integer(arr.get(i)));
		}
		return new BitSet(newArr, newSize);
	}

	@Override
	public int compareTo(BitSet o) {
		if (arr.size() != o.arrSize()) {
			return new Integer(arr.size()).compareTo(new Integer(o.arrSize()));
		}
		for (int i = 0; i < arr.size(); i++) {
			if (arr.get(i) != o.arrGet(i)) {
				return new Integer(arr.get(i)).compareTo(new Integer(o.arrGet(i)));
			}
		}
		return 0;
	}

	@Override
	public String toString() {
		int top = -1;
		for (int i = 0; i < size; i++) {
			int arrID = i / LEN;
			int ID = i % LEN;
			if (((arr.get(arrID) >> ID) & 1) >= 1) {
				top = i;
			}
		}
		if (top == -1) {
			return "0";
		}
		String v = "";
		for (int i = 0; i <= top; i++) {
			int arrID = i / LEN;
			int ID = i % LEN;
			if (((arr.get(arrID) >> ID) & 1) >= 1) {
				v += "1";
			} else {
				v += "0";
			}
		}
		return v;
	}
}
