package nez.util;

public class IArray<T> {
	private int currentSize;
	public int[] ArrayValues;

	public IArray() {
		this.ArrayValues = new int[1];
		this.currentSize = 0;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sb.append(", ");
			}
			sb.append(this.ArrayValues[i]);
		}
		sb.append("]");
		return sb.toString();
	}

	public final int size() {
		return this.currentSize;
	}

	private int[] newArray(int orgsize, int newsize) {
		int[] newarrays = new int[newsize];
		System.arraycopy(this.ArrayValues, 0, newarrays, 0, orgsize);
		return newarrays;
	}

	private void reserve(int newsize) {
		int currentCapacity = this.ArrayValues.length;
		if (newsize < currentCapacity) {
			return;
		}
		int newCapacity = currentCapacity * 2;
		if (newCapacity < newsize) {
			newCapacity = newsize;
		}
		this.ArrayValues = this.newArray(this.currentSize, newCapacity);
	}

	public final void add(int index, int value) {
		this.reserve(this.currentSize + 1);
		this.ArrayValues[index] = value;
		this.currentSize = this.currentSize + 1;
	}

	public final void clear(int index) {
		assert (index <= this.currentSize);
		this.currentSize = index;
	}

	public final int pop() {
		this.currentSize -= 1;
		return this.ArrayValues[this.currentSize];
	}

	public final int[] compactArray() {
		if (this.currentSize == this.ArrayValues.length) {
			return this.ArrayValues;
		} else {
			int[] newValues = new int[this.currentSize];
			System.arraycopy(this.ArrayValues, 0, newValues, 0, this.currentSize);
			return newValues;
		}
	}

	public boolean add(int e) {
		this.reserve(this.currentSize + 1);
		this.ArrayValues[this.currentSize] = e;
		this.currentSize = this.currentSize + 1;
		return true;
	}

	public int remove(int index) {
		int e = this.get(index);
		if (this.currentSize > 1) {
			System.arraycopy(this.ArrayValues, index + 1, this.ArrayValues, index, this.currentSize - 1);
		}
		this.currentSize = this.currentSize - 1;
		return e;
	}

	public int get(int index) {
		return this.ArrayValues[index];
	}

}
