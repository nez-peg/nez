package nez.lang;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import nez.util.UList;

public class Analysis {
	/* this must be LRU Cache in the future */
	/* ProductionName : Production */
	/* "!" + flagName : Boolean */
	
	private static Map<String,Analysis> cache = new HashMap<String, Analysis>();

	public static Analysis get(Production p) {
		Analysis a = cache.get(p.getUniqueName());
		if(a == null) {
			a = new Analysis(p);
			cache.put(p.getUniqueName(), a);
		}
		return a;
	}

	private static Production ProductionFilter(Entry<String,Object> entry) {
		if(entry.getValue() instanceof Production) {
			return (Production)entry.getValue();
		}
		return null;
	}

	private static String consumedKey(Production p) {
		return "|" + p.getUniqueName()+"|";
	}

	private static String flagKey(String flagName) {
		return "!" + flagName;
	}


	private Production self;
	Map<String,Object> map = new HashMap<String, Object>();
	
	Analysis(Production p) {
		this.self = p;
		analyze(p.getExpression());
	}
	
	private static boolean isDefined(Production p, String key) {
		Analysis a = Analysis.get(p);
		return a.isDefined(key);
	}

	private boolean isDefined(String key) {
		return map.containsKey(key);
	}

	public final boolean IsNonTerminalReachable(String uname) {
		return map.containsKey(uname);
	}

	private void analyze(Expression e) {
		if(e instanceof NonTerminal) {
			Production p = ((NonTerminal)e).getProduction();
			if(!IsNonTerminalReachable(p.getUniqueName())) {
				map.put(p.getUniqueName(), p);
				if(this.self != p) {
					analyze(p.getExpression());
				}
			}
		}
		for(Expression sub: e) {
			analyze(sub);
		}
	}
	
//	private boolean quickCheckConsumed(Expression e) {
//		if(e instanceof NonTerminal ) {
//			Production p = ((NonTerminal) e).getProduction();
//			Analysis a = Analysis.get(p);
//			String key = consumedKey(p);
//			return (Boolean)a.map.get(key);  // NullPointerException
//		}
//		if(e instanceof ConsumedProperty) {
//			return true; /* next*/
//		}
//		if(e instanceof UnconsumedProperty) {
//			return false;
//		}
//		if(e instanceof InnerConsumedProperty) {
//			for(Expression sub: e) {
//				if(quickCheckConsumed(sub)) {
//					return true;
//				}
//			}
//			return false;
//		}
//		if(e instanceof Choice) {
//			for(Expression sub: e) {
//				if(!quickCheckConsumed(sub)) {
//					return false;
//				}
//			}
//			return true;
//		}
//		return false;
//	}
//
//	public final static boolean checkLeftRecursion(Production p) {
//		if(!hasRecursion(p)) {
//			return true;
//		}
//		Analysis a = Analysis.get(p);
//		return a.reachConditionalFlag(flagName);
//	}
//
//	public final static boolean hasRecursion(Production p) {
//		Analysis a = Analysis.get(p);
//		return a.map.containsKey(p.getUniqueName());
//	}
//
//	private boolean checkLeftRecursion(Expression e) {
//		if(e instanceof NonTerminal) {
//			
//		}
//		if(e instanceof Sequence) {
//			for(Expression sub: e) {
//				checkLeftRecursion(sub);
//				if(sub.isAlwaysConsumed()) {
//					return true;
//				}
//			}
//			return false;
//		}
//		if(e instanceof Choice) {
//			
//		}
//	}
//
//	class Determinism {
//		Determinism prev;
//		Production p;
//		Determinism(Production p, Determinism prev) {
//			this.prev = prev;
//			this.p = p;
//		}
//		boolean isVisited(Production p) {
//			Determinism d = this;
//			while(d != null) {
//				if(d.p == p) {
//					return true;
//				}
//				d = d.prev;
//			}
//			return false;
//		}
//	}
//	
//	public final static boolean isAlwaysConsumed(Production p, Determinism d) {
//		Analysis a = Analysis.get(p);
//		String key = consumedKey(p);
//		if(a.isDefined(key)) {
//			return (Boolean)a.map.get(key);
//		}
//		boolean res = a.isAlwaysConsumed(p.getExpression(), new Determinism(p, d));
//		a.map.put(key, res);
//	}
//	
//
//
//	boolean isAlwaysConsumed(Expression e, Determinism d) {
//		if(e instanceof NonTerminal ) {
//			Production p = ((NonTerminal) e).getProduction();
//			if(d == null) {
//				
//			}
//			return Analysis.isPossibleConsumed(p, d);
//		}
//		if(e instanceof ConsumedProperty) {
//			return true; /* next*/
//		}
//		if(e instanceof UnconsumedProperty) {
//			return false;
//		}
//		if(e instanceof InnerConsumedProperty) {
//			for(Expression sub: e) {
//				if(isAlwaysConsumed(sub, d, deepen)) {
//					return true;
//				}
//			}
//		}
//		if(e instanceof Choice) {
//			for(Expression sub: e) {
//				if(!isAlwaysConsumed(sub, d, deepen)) {
//					return false;
//				}
//			}
//			return true;
//		}
//	}


	
	
	
	/**
	 * isConditionalFlag tests the reachability of flagName
	 * @param p
	 * @param flagName
	 * @return
	 */
	
	public final static boolean isConditionalFlag(Production p, String flagName) {
		Analysis a = Analysis.get(p);
		return a.reachConditionalFlag(flagName);
	}

	private boolean reachConditionalFlag(String flagName) {
		String key = flagKey(flagName);
		if(map.containsKey(key)) {
			return (Boolean)map.containsKey(key);
		}
		Set<Production> reached = new HashSet<Production>();
		if(flagAnalysis(self.getExpression(), flagName, reached)) {
			return true;
		}
		for(Production p: reached) {
			if(isDefined(p, key)) {
				if(isConditionalFlag(p, flagName)) {
					this.map.put(key, true);
					return true;
				};
			}
			if(flagAnalysis(self.getExpression(), flagName, null)) {
				this.map.put(key, true);
				return true;
			}
		}
		this.map.put(key, false);
		return false;
	}

	private boolean flagAnalysis(Expression e, String flagName, Set<Production> set) {
		if(e instanceof IfFlag && flagName.equals(((IfFlag)e).getFlagName())) {
			String key = flagKey(flagName);
			map.put(key, true);
			return true;
		}
		if(e instanceof OnFlag && flagName.equals(((IfFlag)e).getFlagName())) {
			return false;
		}
		if(set != null && e instanceof NonTerminal) {
			Production p = ((NonTerminal) e).getProduction();
			updateProductionSet(p, set);
		}
		boolean res = false;
		for(Expression sub: e) {
			if(flagAnalysis(sub, flagName, set)) {
				res = true;
			}
		}
		return res;
	}

	private static void updateProductionSet(Production p, Set<Production> set) {
		Analysis a = Analysis.get(p);
		for(Entry<String,Object> entry: a.map.entrySet()) {
			set.add(ProductionFilter(entry));
		}
	}
	
}
