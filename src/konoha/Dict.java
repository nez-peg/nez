package konoha;

import java.util.HashMap;

public class Dict<T> extends HashMap<String, T> {

	private Class<?> type;

	public Dict(Class<?> type) {
		super(17);
		this.type = type;
	}

	@SuppressWarnings("unchecked")
	public Dict(Class<?> type, Object[] values) {
		this.type = type;
		for (int i = 0; i < values.length; i += 2) {
			String key = (String) values[i];
			this.put(key, (T) values[i + 1]);
		}
	}

	public final Class<?> getElementType() {
		return this.type;
	}

	public final T set(String key, T value) {
		this.put(key, value);
		return value;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
}
