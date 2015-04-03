package nez.util;

import java.lang.reflect.Array;
import java.util.AbstractList;

public class UList<T> extends AbstractList<T> {
	private int    currentSize;
	public T[] ArrayValues;

	public UList(T[] Values) {
		this.ArrayValues = Values;
		this.currentSize = 0;
	}

	@Override public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		sBuilder.append("[");
		for(int i = 0; i < this.size(); i++) {
			if(i > 0) {
				sBuilder.append(", ");
			}
			sBuilder.append(this.Stringify(this.ArrayValues[i]));
		}
		sBuilder.append("]");
		return sBuilder.toString();
	}

	protected String Stringify(Object Value) {
		if(Value instanceof String) {
			return StringUtils.quoteString('"', (String) Value, '"');
		}
		return Value.toString();
	}

	@Override
	public final int size() {
		return this.currentSize;
	}

	private T[] newArray(int orgsize, int newsize) {
		@SuppressWarnings("unchecked")
		T[] newarrays = (T[])Array.newInstance(this.ArrayValues.getClass().getComponentType(), newsize);
		System.arraycopy(this.ArrayValues, 0, newarrays, 0, orgsize);
		return newarrays;
	}

	private void reserve(int newsize) {
		int currentCapacity = this.ArrayValues.length;
		if(newsize < currentCapacity) {
			return;
		}
		int newCapacity = currentCapacity * 2;
		if(newCapacity < newsize) {
			newCapacity = newsize;
		}
		this.ArrayValues = this.newArray(this.currentSize, newCapacity);
	}

	@Override
	public final void add(int index, T Value) {
		this.reserve(this.currentSize + 1);
		//System.arraycopy(this.ArrayValues, index, this.ArrayValues, index+1, this.currentSize - index);
		this.ArrayValues[index] = Value;
		this.currentSize = this.currentSize + 1;
	}

	public final void clear(int index) {
		assert(index <= this.currentSize);
		this.currentSize = index;
	}

	public void pop() {
		this.currentSize -= 1;
	}

	public final T[] compactArray() {
		if(this.currentSize == this.ArrayValues.length) {
			return this.ArrayValues;
		}
		else {
			@SuppressWarnings("unchecked")
			T[] newValues = (T[])Array.newInstance(this.ArrayValues.getClass().getComponentType(), this.currentSize);
			System.arraycopy(this.ArrayValues, 0, newValues, 0, this.currentSize);
			return newValues;
		}
	}
	//
	//	public static void ThrowOutOfArrayIndex(int Size, long Index) {
	//		throw new SoftwareFault("out of array index " + Index + " < " + Size);
	//	}

	@Override
	public boolean add(T e) {
		//System.out.println("size: " + this.currentSize + ", " + this.ArrayValues.length);
		this.reserve(this.currentSize + 1);
		this.ArrayValues[this.currentSize] = e;
		this.currentSize = this.currentSize + 1;
		return true;
	}

	@Override
	public T remove(int index) {
		T e = this.get(index);
		if(this.currentSize > 1) {
			System.arraycopy(this.ArrayValues, index+1, this.ArrayValues, index, this.currentSize - 1);
		}
		this.currentSize = this.currentSize - 1;
		return e;
	}

	@Override
	public T get(int index) {
		return this.ArrayValues[index];
	}


}
