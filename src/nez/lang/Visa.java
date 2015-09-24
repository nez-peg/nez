package nez.lang;

import java.util.HashMap;

public class Visa {
	public final static boolean isVisited(Visa v, Production p) {
		return v == null ? false : v.isVisited(p);
	}

	public final static Visa visited(Visa v, Production p) {
		if (v == null) {
			v = new Visa();
		}
		v.visited(p);
		return v;
	}

	HashMap<String, Production> map = new HashMap<String, Production>();

	private boolean isVisited(Production p) {
		return map.containsKey(p.getUniqueName());
	}

	private void visited(Production p) {
		map.put(p.getUniqueName(), p);
	}
}