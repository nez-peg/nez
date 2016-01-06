package nez.lang;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@SuppressWarnings("serial")
public class Conditions extends TreeMap<String, Boolean> {

	public final static Conditions newConditions(Production start, Map<String, Boolean> inits) {
		Conditions conds = new Conditions();
		Set<String> s = retriveConditionSet(start);
		for (String c : s) {
			if (inits != null && inits.containsKey(c)) {
				conds.put(c, inits.get(c));
			} else {
				conds.put(c, true); // FIXME: false
			}
		}
		// System.out.println("new conditions: " + conds);
		return conds;
	}

	public final static Set<String> retriveConditionSet(Production start) {
		TreeSet<String> ts = new TreeSet<>();
		HashMap<String, Boolean> visited = new HashMap<>();
		checkCondition(start.getExpression(), ts, visited);
		return ts;
	}

	private static void checkCondition(Expression e, TreeSet<String> ts, HashMap<String, Boolean> visited) {
		if (e instanceof Nez.IfCondition) {
			ts.add(((Nez.IfCondition) e).flagName);
		}
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (p != null && !visited.containsKey(p.getUniqueName())) {
				visited.put(p.getUniqueName(), true);
				checkCondition(p.getExpression(), ts, visited);
			}
		}
		for (Expression se : e) {
			checkCondition(se, ts, visited);
		}
	}

	enum Value {
		Reachable, Unreachable, Undecided;
	}

	HashMap<String, Value> reachMap = new HashMap<String, Value>();

	public final boolean isConditional(Production p, String condition) {
		return (hasIfCondition(p, condition) != Value.Unreachable);
	}

	public final boolean isConditional(Expression p, String condition) {
		return (hasIfCondition(p, condition) != Value.Unreachable);
	}

	private Value hasIfCondition(Production p, String condition) {
		if (reachMap == null) {
			this.reachMap = new HashMap<String, Value>();
		}
		String key = p.getUniqueName() + "+" + condition;
		Value res = reachMap.get(key);
		if (res == null) {
			reachMap.put(key, Value.Undecided);
			res = hasIfCondition(p.getExpression(), condition);
			if (res != Value.Undecided) {
				reachMap.put(key, res);
			}
		}
		return res;
	}

	private final Value hasIfCondition(Expression e, String condition) {
		if (e instanceof Nez.IfCondition) {
			return condition.equals(((Nez.IfCondition) e).flagName) ? Value.Reachable : Value.Unreachable;
		}
		if (e instanceof Nez.OnCondition && condition.equals(((Nez.OnCondition) e).flagName)) {
			return Value.Unreachable;
		}
		if (e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			if (p == null) {
				return Value.Unreachable;
			}
			return hasIfCondition(p, condition);
		}
		boolean hasUndecided = false;
		for (Expression se : e) {
			Value dep = hasIfCondition(se, condition);
			if (dep == Value.Reachable) {
				return Value.Reachable;
			}
			if (dep == Value.Undecided) {
				hasUndecided = true;
			}
		}
		return hasUndecided ? Value.Undecided : Value.Unreachable;
	}

	public String conditionalName(Production p, boolean nonTreeConstruction) {
		StringBuilder sb = new StringBuilder();
		if (nonTreeConstruction) {
			sb.append("~");
		}
		sb.append(p.getUniqueName());
		for (String c : this.keySet()) {
			if (isConditional(p, c)) {
				if (this.get(c)) {
					sb.append("&");
				} else {
					sb.append("!");
				}
				sb.append(c);
			}
		}
		String cname = sb.toString();
		// System.out.println("unique: " + uname + ", " + this.boolMap.keySet()
		// + "=>" + sb.toString());
		return cname;
	}
}
