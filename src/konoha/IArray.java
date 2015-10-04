package konoha;

public class IArray<T> implements KonohaArray {
	private int currentSize;
	public int[] ArrayValues;

	public IArray(int[] v) {
		this.ArrayValues = v;
		this.currentSize = 0;
	}

	public IArray(Object[] v, int size) {
		this.ArrayValues = new int[v.length];
		this.currentSize = size;
		for (int i = 0; i < v.length; i++) {
			Object o = v[i];
			this.ArrayValues[i] = (o instanceof Number) ? ((Number) o).intValue() : 0;
		}
	}

	@Override
	public Class<?> getElementType() {
		return int.class;
	}

	public final int size() {
		return this.currentSize;
	}

	public final int get(int index) {
		return this.ArrayValues[index];
	}

	public final int get(int index, int value) {
		return this.ArrayValues[index];
	}

	public final boolean add(int e) {
		this.reserve(this.currentSize + 1);
		this.ArrayValues[this.currentSize] = e;
		this.currentSize = this.currentSize + 1;
		return true;
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

	public int remove(int index) {
		int e = this.get(index);
		if (this.currentSize > 1) {
			System.arraycopy(this.ArrayValues, index + 1, this.ArrayValues, index, this.currentSize - 1);
		}
		this.currentSize = this.currentSize - 1;
		return e;
	}

}
