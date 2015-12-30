package nez.lang;

import java.util.HashMap;

import nez.util.UList;

public class Productions {

	public static class ProductionProperty extends UList<String> {
		HashMap<String, Boolean> boolMap;

		public ProductionProperty() {
			super(new String[64]);
		}

		public void push(String uname) {
			this.add(uname);
		}

		public boolean isVisited(String uname) {
			for (String u : this) {
				if (uname.equals(u)) {
					return true;
				}
			}
			return false;
		}

		public boolean hasProperty(String uname) {
			return this.boolMap.containsKey(uname);
		}

		public Boolean getProperty(String uname) {
			return this.boolMap.get(uname);
		}

		public void setProperty(String uname, Boolean result) {
			this.boolMap.put(uname, result);
		}

		void setAll(String uname, Boolean result) {
			for (int i = this.size() - 1; i >= 0; i--) {
				String u = this.get(i);
				this.boolMap.put(u, result);
				if (uname.equals(u)) {
					break;
				}
			}
		}
	}

	public final static boolean isRecursive(Production p, ProductionProperty stack) {
		String uname = p.getUniqueName();
		if (stack.hasProperty(uname)) {
			return stack.getProperty(uname);
		}
		if (stack.isVisited(uname)) {
			stack.setAll(uname, true);
			return true;
		}
		stack.push(uname);
		boolean b = isRecursive(p.getExpression(), stack);
		if (b == false) {
			stack.setProperty(uname, b);
		}
		stack.pop();
		return b;
	}

	private final static boolean isRecursive(Expression e, ProductionProperty stack) {
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (p == null) {
				return false;
			}
			return isRecursive(p, stack);
		}
		for (Expression sub : e) {
			boolean b = isRecursive(sub, stack);
			if (b == true) {
				return true;
			}
		}
		return false;
	}

	/*
	 * public final static boolean isContextual(Production p, ProductionProperty
	 * stack) { String uname = p.getUniqueName(); if (stack.hasProperty(uname))
	 * { return stack.getProperty(uname); } if(!stack.isVisited(uname)) {
	 * stack.push(uname); checkContextual(p.getExpression(), stack);
	 * stack.pop(); } if (stack.hasProperty(uname)) { return
	 * stack.getProperty(uname); } stack.setProperty(uname, false); return
	 * false; }
	 * 
	 * private void checkContextual(Expression e, ProductionProperty stack) { if
	 * (e instanceof NonTerminal) { Production p = ((NonTerminal)
	 * e).getProduction(); if (p == null) { stack.setProperty(((NonTerminal)
	 * e).getUniqueName(), false); return; } isContextual(p, stack); } if (e
	 * instanceof Nez.Contextual) { stack.setAll(uname, result); return; } for
	 * (Expression sub : e) { checkContextual(sub, v); } }
	 */

}
