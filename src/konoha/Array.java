package konoha;

import java.util.AbstractList;

import nez.util.StringUtils;

public class Array<T> extends AbstractList<T> implements KonohaArray {
	private int currentSize;
	public T[] ArrayValues;

	public Array(T[] values) {
		this.ArrayValues = values;
		this.currentSize = 0;
	}

	public Array(T[] values, int size) {
		this.ArrayValues = values;
		this.currentSize = size;
	}

	@Override
	public final Class<?> getElementType() {
		return this.ArrayValues.getClass().getComponentType();
	}

	@Override
	public final int size() {
		return this.currentSize;
	}

	@Override
	public T get(int index) {
		return this.ArrayValues[index];
	}

	@Override
	public T set(int index, T value) {
		this.ArrayValues[index] = value;
		return value;
	}

	@Override
	public boolean add(T e) {
		this.reserve(this.currentSize + 1);
		this.ArrayValues[this.currentSize] = e;
		this.currentSize = this.currentSize + 1;
		return true;
	}

	@Override
	public final void add(int index, T Value) {
		this.reserve(this.currentSize + 1);
		// System.arraycopy(this.ArrayValues, index, this.ArrayValues, index+1,
		// this.currentSize - index);
		this.ArrayValues[index] = Value;
		this.currentSize = this.currentSize + 1;
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("[");
		for (int i = 0; i < this.size(); i++) {
			if (i > 0) {
				sBuilder.append(", ");
			}
			sBuilder.append(this.Stringify(this.ArrayValues[i]));
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}

	protected String Stringify(Object Value) {
		if (Value instanceof String) {
			return StringUtils.quoteString('"', (String) Value, '"');
		}
		return Value.toString();
	}

	private T[] newArray(int orgsize, int newsize) {
		@SuppressWarnings("unchecked")
		T[] newarrays = (T[]) java.lang.reflect.Array.newInstance(this.getElementType(), newsize);
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

	public final void clear(int index) {
		assert (index <= this.currentSize);
		this.currentSize = index;
	}

	public final T pop() {
		this.currentSize -= 1;
		return this.ArrayValues[this.currentSize];
	}

	public final T[] compactArray() {
		if (this.currentSize == this.ArrayValues.length) {
			return this.ArrayValues;
		} else {
			@SuppressWarnings("unchecked")
			T[] newValues = (T[]) java.lang.reflect.Array.newInstance(this.getElementType(), this.currentSize);
			System.arraycopy(this.ArrayValues, 0, newValues, 0, this.currentSize);
			return newValues;
		}
	}

	//
	// public static void ThrowOutOfArrayIndex(int Size, long Index) {
	// throw new SoftwareFault("out of array index " + Index + " < " + Size);
	// }

	@Override
	public T remove(int index) {
		T e = this.get(index);
		if (this.currentSize > 1) {
			System.arraycopy(this.ArrayValues, index + 1, this.ArrayValues, index, this.currentSize - 1);
		}
		this.currentSize = this.currentSize - 1;
		return e;
	}

}
