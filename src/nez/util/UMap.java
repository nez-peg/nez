package nez.util;

import java.util.HashMap;

public final class UMap <T> {
	final HashMap<String, T>	m;
	public UMap() {
		this.m = new HashMap<String, T>();
	}
	@Override public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		int i = 0;
		for(String Key : this.m.keySet()) {
			if(i > 0) {
				sb.append(", ");
			}
			sb.append(this.stringify(Key));
			sb.append(" : ");
			sb.append(this.stringify(this.m.get(Key)));
			i++;
		}
		sb.append("}");
		return sb.toString();
	}
	protected String stringify(Object Value) {
		if(Value instanceof String) {
			return StringUtils.quoteString('"', (String) Value, '"');
		}
		return Value.toString();
	}
	public final void put(String key, T value) {
		this.m.put(key, value);
	}
	public final T get(String key) {
		return this.m.get(key);
	}
	public final T get(String key, T defaultValue) {
		T Value = this.m.get(key);
		if(Value == null) {
			return defaultValue;
		}
		return Value;
	}
	public final void remove(String Key) {
		this.m.remove(Key);
	}
	public final boolean hasKey(String Key) {
		return this.m.containsKey(Key);
	}
	public final UList<String> keys() {
		UList<String> a = new UList<String>(new String[this.m.size()]);
		for(String k : this.m.keySet()) {
			a.add(k);
		}
		return a;
	}
	public final UList<T> values(T[] aa) {
		UList<T> a = new UList<T>(aa);
		for(T v : this.m.values()) {
			a.add(v);
		}
		return a;
	}
	public final int size() {
		return this.m.size();
	}
	
	public final void clear() {
		this.m.clear();
	}
}
