package nez.x.dfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Grammar;
import nez.lang.Expression;
import nez.lang.Production;

public class DFAValidator {

	private Grammar grammar;
	private int theNumberOfNonTerminal;
	private Map<String, Integer> nonTerminalIDTable;
	private Expression[] nonTerminalContext;
	private ArrayList[] nonTerminalRelationGraph;
	private ValidateContext[] validateContext;

	public DFAValidator(Grammar grammar) {
		this.grammar = grammar;
		this.theNumberOfNonTerminal = 0;
		this.nonTerminalIDTable = new HashMap<String, Integer>();
	}

	public boolean convertible() {
		List<Production> productions = this.grammar.getProductionList();
		theNumberOfNonTerminal = productions.size();
		nonTerminalRelationGraph = new ArrayList[theNumberOfNonTerminal];
		nonTerminalContext = new Expression[theNumberOfNonTerminal];
		validateContext = new ValidateContext[theNumberOfNonTerminal];
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			nonTerminalContext[i] = productions.get(i).getExpression();
			nonTerminalIDTable.put(productions.get(i).getLocalName(), i);
			System.out.println(productions.get(i).getLocalName() + " <- " + nonTerminalContext[i]);
		}
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			constructAPartOfGraph(i);
		}
		/*
		 * for (int i = 0; i < theNumberOfNonTerminal; i++) { for (int j = 0; j
		 * < nonTerminalRelationGraph[i].size(); j++) {
		 * System.out.print(nonTerminalRelationGraph[i].get(j) + " "); }
		 * System.out.println(""); } System.out.println("((((((((((((((");
		 */
		boolean result = removeUselessNonTerminal();
		/*
		 * for (int i = 0; i < theNumberOfNonTerminal; i++) {
		 * System.out.println(i + "-th is " + alreadyVerified[i]); }
		 * System.out.println("))))))))))))))"); for (int i = 0; i <
		 * theNumberOfNonTerminal; i++) {
		 * System.out.println(productions.get(i).getLocalName() + " | " +
		 * nonTerminalIDTable.get(productions.get(i).getLocalName()) +
		 * " ---------------"); System.out.println(validateContext[i]);
		 * System.out.println("---------------"); }
		 * 
		 * for (int i = 0; i < theNumberOfNonTerminal; i++) { for (int j = 0; j
		 * < nonTerminalRelationGraph[i].size(); j++) {
		 * System.out.print(nonTerminalRelationGraph[i].get(j) + " "); }
		 * System.out.println(""); }
		 */

		if (!result) {
			return false;
		}

		compressRedundantEdge();
		expandN(theNumberOfNonTerminal);
		/*
		 * for (Map.Entry<String, Integer> e : nonTerminalIDTable.entrySet()) {
		 * System.out.println(e.getKey() + " : " + e.getValue()); }
		 * 
		 * for (int i = 0; i < theNumberOfNonTerminal; i++) {
		 * System.out.println(i + "-th : " + alreadyVerified[i] + " | " +
		 * validateContext[i]); }
		 */
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			if (alreadyVerified[i]) {
				continue;
			}
			if (!validateContext[i].eval(i)) {
				return false;
			}
		}
		return true;
	}

	private void expandN(int loopCounter) {
		while (loopCounter > 0) {

			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				// stringContext[i] = removeRedundantWords(stringContext[i]);
				validateContext[i].removeRedundantWords();
				// if (!eval(i, validateContext[i].toString())) {
				if (!validateContext[i].eval(i)) {
					return;
				}
			}

			ValidateContext[] sb = new ValidateContext[theNumberOfNonTerminal];
			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				sb[i] = validateContext[i];
			}

			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				int pos = 0;
				ValidateContext tmp = new ValidateContext();
				while (pos < validateContext[i].length()) {
					char c = validateContext[i].charAt(pos);
					if (Character.isDigit(c)) {
						int ID = 0;
						while (pos < validateContext[i].length() && Character.isDigit(validateContext[i].charAt(pos))) {
							ID *= 10;
							ID += (validateContext[i].charAt(pos++) - '0');
						}
						// stringContext[i].charAt(pos) == $
						tmp.append(sb[ID]);
						++pos;
					} else {
						tmp.append(c);
						++pos;
					}
				}
				validateContext[i] = tmp;
			}
			/*
			 * System.out.println("^^^^^^^^^"); for (int i = 0; i <
			 * theNumberOfNonTerminal; i++) {
			 * System.out.println(alreadyVerified[i] + " | " +
			 * stringContext[i]); } System.out.println("^^^^^^^^^");
			 */
			--loopCounter;
		}
	}

	private boolean[] hasSelfLoop;

	private void updateHasSelfLoop() {
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			if (hasSelfLoop[i]) {
				continue;
			}
			/*
			 * for (int j = 0; j < stringContext[i].length(); j++) { if
			 * (Character.isDigit(stringContext[i].charAt(j))) { int ID = 0;
			 * while (j < stringContext[i].length() &&
			 * stringContext[i].charAt(j) != '$') { ID *= 10; ID +=
			 * (stringContext[i].charAt(j++) - '0'); } if (i == ID) {
			 * hasSelfLoop[i] = true; break; } } }
			 */
			for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
				ValidateEdge ve = (ValidateEdge) nonTerminalRelationGraph[i].get(j);
				if (ve.getSrc() == ve.getDst()) {
					hasSelfLoop[i] = true;
					break;
				}
			}
		}
	}

	private void compressRedundantEdge() {
		hasSelfLoop = new boolean[theNumberOfNonTerminal];
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			hasSelfLoop[i] = false;
		}

		boolean update = true;
		while (update) {
			update = false;
			updateHasSelfLoop();
			/*
			 * System.out.println("----------------"); for (int i = 0; i <
			 * theNumberOfNonTerminal; i++) { System.out.println(i + "-th : " +
			 * validateContext[i] + " | verify " + alreadyVerified[i] +
			 * " | loop " + hasSelfLoop[i]); for (int j = 0; j <
			 * nonTerminalRelationGraph[i].size(); j++) {
			 * System.out.println(nonTerminalRelationGraph[i].get(j) + " "); } }
			 * System.out.println("------"); System.out.println("");
			 */
			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				if (alreadyVerified[i]) {
					continue;
				}
				for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
					ValidateEdge ve = (ValidateEdge) nonTerminalRelationGraph[i].get(j);
					if (hasSelfLoop[ve.getSrc()] && hasSelfLoop[ve.getDst()]) {
						continue;
					}
					int base = ve.getSrc();
					int target = ve.getDst();
					if (hasSelfLoop[target]) {
						int tmp = base;
						base = target;
						target = tmp;
					}
					// System.out.println("in-compress " + base + " " + target);
					compress(base, target);
					// targetを潰してしまうため、その他の非終端記号内にあるtargetをbaseに置き換える
					for (int k = 0; k < theNumberOfNonTerminal; k++) {
						if (alreadyVerified[k] || k == base) {
							continue;
						}
						// stringContext[k] =
						// replaceAllNonTerminal(stringContext[k], new
						// Integer(target).toString() + "$", new
						// Integer(base).toString() + "$");
						validateContext[k] = ValidateContext.replaceAllNonTerminal(validateContext[k], new Integer(target).toString() + "$", new Integer(base).toString() + "$");
					}
					// System.out.println("out-compress");
					update = true;
					break;
				}
				if (update) {
					break;
				}
			}
		}
	}

	private void compress(int base, int target) {
		// stringContext[base] = replaceAllNonTerminal(stringContext[base], new
		// Integer(target).toString() + "$", stringContext[target].toString());
		validateContext[base] = ValidateContext.replaceAllNonTerminal(validateContext[base], new Integer(target).toString() + "$", validateContext[target].toString());
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			if (i == target) {
				for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
					ValidateEdge ve = (ValidateEdge) nonTerminalRelationGraph[i].get(j);
					nonTerminalRelationGraph[base].add(new ValidateEdge(base, ve.getDst()));
				}
				alreadyVerified[i] = true;
				nonTerminalRelationGraph[i].clear();
			} else {
				ArrayList<ValidateEdge> veArray = new ArrayList<ValidateEdge>();
				for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
					ValidateEdge ve = (ValidateEdge) nonTerminalRelationGraph[i].get(j);
					if (ve.getDst() == target) {
						veArray.add(ve);
						if (i != base) {
							nonTerminalRelationGraph[i].add(new ValidateEdge(i, base));
						}
					}
				}
				for (int j = 0; j < veArray.size(); j++) {
					nonTerminalRelationGraph[i].remove(veArray.get(j));
				}
			}
		}
	}

	/*
	 * private StringBuilder replaceAllNonTerminal(StringBuilder context, String
	 * before, String after) { // System.out.println("before " + context + " | "
	 * + before + " -> " + // after); StringBuilder newContext = new
	 * StringBuilder(); int before_len = before.length(); for (int i = 0; i <
	 * context.length(); i++) { char c = context.charAt(i); if (c ==
	 * before.charAt(0) && i + before_len - 1 < context.length()) {
	 * 
	 * String part = context.substring(i, i + before_len);
	 * 
	 * if (before.equals(part)) { if (i - 1 >= 0 &&
	 * Character.isDigit(context.charAt(i - 1))) { newContext.append(c);
	 * continue; } if (i + before_len < context.length() &&
	 * Character.isDigit(context.charAt(i + before_len))) {
	 * newContext.append(c); continue; } newContext.append(after); i +=
	 * (before_len - 1); } else { newContext.append(c); } } else {
	 * newContext.append(c); } } // System.out.println("after " + newContext);
	 * return newContext; }
	 */

	/*
	 * private boolean eval(int nonTerminalID, String context) { for (int i = 0;
	 * i < context.length(); i++) { if (Character.isDigit(context.charAt(i))) {
	 * int ID = 0; int L = i; while (i < context.length() && context.charAt(i)
	 * != '$') { ID *= 10; ID += (context.charAt(i++) - '0'); } int R = i; if
	 * (ID == nonTerminalID) { boolean hasLeft = hasChar(context, L - 1, -1);
	 * boolean hasRight = hasChar(context, R + 1, +1); if (!hasLeft &&
	 * !hasRight) { System.out.println("FOUND : A <- A");
	 * System.out.println(nonTerminalID + "-th : " + context); return false; }
	 * else if (hasLeft && !hasRight) { // VALID } else if (!hasLeft &&
	 * hasRight) { System.out.println("FOUND : A <- Aa");
	 * System.out.println(nonTerminalID + "-th : " + context); return false; }
	 * else if (hasLeft && hasRight) { System.out.println("FOUND : A <- aAa");
	 * System.out.println(nonTerminalID + "-th : " + context); return false; } }
	 * } } return true; }
	 */

	private int[] in_degree;
	private boolean[] alreadyVerified;

	private boolean removeUselessNonTerminal() {
		in_degree = new int[theNumberOfNonTerminal];
		alreadyVerified = new boolean[theNumberOfNonTerminal];
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			alreadyVerified[i] = false;
		}

		boolean update = true;
		while (update) {
			update = false;

			ArrayList[] in_vertexID = new ArrayList[theNumberOfNonTerminal];
			ArrayList[] in_edge = new ArrayList[theNumberOfNonTerminal];

			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				in_degree[i] = 0;
				in_vertexID[i] = new ArrayList<Integer>();
				in_edge[i] = new ArrayList<ValidateEdge>();
			}
			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
					ValidateEdge ve = (ValidateEdge) nonTerminalRelationGraph[i].get(j);
					++in_degree[ve.getDst()];
					in_vertexID[ve.getDst()].add(i);
					in_edge[ve.getDst()].add(ve);
				}
			}

			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				if (in_degree[i] == 0) {
					// boolean result = eval(i, validateContext[i].toString());
					boolean result = validateContext[i].eval(i);
					if (!result) {
						return false;
					}
					for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
						nonTerminalRelationGraph[i].remove(j);
						update = true;
					}
					if (update) {
						alreadyVerified[i] = true;
						break;
					}
				} else {
					if (nonTerminalRelationGraph[i].isEmpty() && in_degree[i] > 0) {
						for (int j = 0; j < in_degree[i]; j++) {
							int vertexID = (int) in_vertexID[i].get(j);
							validateContext[vertexID] = ValidateContext.replaceAllNonTerminal(validateContext[vertexID], (new Integer(i).toString()) + "$", validateContext[i].toString());
							nonTerminalRelationGraph[vertexID].remove(in_edge[i].get(j));
						}
						update = true;
						break;
					}
				}
				if (update) {
					break;
				}
			}
		}
		return true;
	}

	private void constructAPartOfGraph(int nonTerminalID) {
		nonTerminalRelationGraph[nonTerminalID] = new ArrayList<ValidateEdge>();
		Expression e = nonTerminalContext[nonTerminalID];
		String context = e.toString();
		context = eliminateRedundantCharacters(context);
		// stringContext[nonTerminalID] = new StringBuilder(context);
		validateContext[nonTerminalID] = new ValidateContext(context);
		System.out.println(nonTerminalID + " | " + nonTerminalContext[nonTerminalID] + " : context = " + validateContext[nonTerminalID]);
		for (int i = 0; i < context.length(); i++) {
			if (Character.isDigit(context.charAt(i))) {
				int ID = 0;
				int L = i;
				while (i < context.length() && context.charAt(i) != '$') {
					ID *= 10;
					ID += (context.charAt(i++) - '0');
				}
				int R = i;
				nonTerminalRelationGraph[nonTerminalID].add(new ValidateEdge(nonTerminalID, ID));
			}
		}
	}

	/*
	 * private boolean hasChar(String context, int start_pos, int dir) { int
	 * depth = 0; for (int i = start_pos; 0 <= i && i < context.length(); i +=
	 * dir) { char c = context.charAt(i); if (depth == 0) { if (dir == -1 && c
	 * == '{') { return false; } if (dir == +1 && c == '}') { return false; } if
	 * (c == '/') { return false; } } if (c == '}') { ++depth; } if (c == '{') {
	 * --depth; } if (depth == 0 && c == 'a') { return true; } } return false; }
	 */

	private String eliminateRedundantCharacters(String context) {
		StringBuilder newContext = new StringBuilder();
		for (int i = 0; i < context.length(); i++) {
			char c = context.charAt(i);
			if (Character.isWhitespace(c)) {
				continue;
			}
			if (c == '\'' || c == '\"') {
				boolean emptyChar = true;
				++i;
				while (i < context.length()) {
					if (i + 1 < context.length() && context.charAt(i) == '\\' && context.charAt(i + 1) == '\\') {
						i += 2;
						emptyChar = false;
						continue;
					}
					if (i + 1 < context.length() && context.charAt(i) == '\\' && context.charAt(i + 1) == c) {
						emptyChar = false;
						i += 2;
						continue;
					}
					if (context.charAt(i) == c) {
						break;
					}
					emptyChar = false;
					++i;
				}
				if (emptyChar) {
					continue;
				}
				if (newContext.length() - 1 >= 0 && newContext.charAt(newContext.length() - 1) != 'a') {
					newContext.append("a");
				} else if (newContext.length() == 0) {
					newContext.append("a");
				}
			} else if (c == '.') {
				if (newContext.length() - 1 >= 0 && newContext.charAt(newContext.length() - 1) != 'a') {
					newContext.append("a");
				} else if (newContext.length() == 0) {
					newContext.append("a");
				}
			} else if (c == '!' || c == '&') {
				if (i + 1 < context.length() && (context.charAt(i + 1) == '(' || context.charAt(i + 1) == '{')) {
					newContext.append('{');
					++i;
				} else {
					newContext.append(c);
				}
			} else if (c == '/' || c == '(' || c == ')') {
				newContext.append(c);
			} else if (c == '{') {
				newContext.append('(');
			} else if (c == '}') {
				newContext.append(')');
			} else if (c == '[') {
				boolean foundChar = false;
				while (i < context.length() && context.charAt(i) != ']') {
					if (context.charAt(i) != ' ') {
						foundChar = true;
					}
					++i;
				}
				if (foundChar) {
					newContext.append('a');
				}
			} else if (Character.isAlphabetic(c) || c == '_') {
				StringBuilder nonTerminalName = new StringBuilder();
				while (i < context.length()) {
					char c2 = context.charAt(i);
					if (!Character.isDigit(c2) && !Character.isAlphabetic(c2) && c2 != '_') {
						break;
					}
					nonTerminalName.append(c2);
					++i;
				}
				--i;
				assert nonTerminalIDTable.containsKey(nonTerminalName.toString()) == true : "no such non-terminal : " + nonTerminalName;
				if (nonTerminalIDTable.containsKey(nonTerminalName.toString()) == false) {
					System.out.println("no such non-terminal : " + nonTerminalName);
				}
				newContext.append(nonTerminalIDTable.get(nonTerminalName.toString()));
				newContext.append('$');

			}
		}

		Deque<Integer> deq = new ArrayDeque<Integer>();
		int depth = 0;
		for (int i = 0; i < newContext.length(); i++) {
			char c = newContext.charAt(i);
			if (c == '(' || c == '{')
				++depth;
			if (c == ')') {
				if (!deq.isEmpty() && deq.peekLast() == depth) {
					newContext.setCharAt(i, '}');
					deq.pollLast();
				}
				--depth;
			}
			if (newContext.charAt(i) == '{') {
				deq.addLast(depth);
			}
		}

		StringBuilder newContext2 = new StringBuilder();

		for (int i = 0; i < newContext.length(); i++) {

			char c = newContext.charAt(i);
			if (c == '!' || c == '&') {
				++i;
				if (i < newContext.length() && newContext.charAt(i) == 'a') {
					// useless
					/*
					 * newContext2.append('{'); newContext2.append('a');
					 * newContext2.append('}');
					 */
				} else {
					boolean debug = false;
					newContext2.append('{');
					while (i < newContext.length() && newContext.charAt(i) != '$') {
						debug = true;
						newContext2.append(newContext.charAt(i++));
					}
					newContext2.append('$');
					newContext2.append('}');
					assert debug == true;
				}
			} else {
				newContext2.append(c);
			}
		}

		return newContext2.toString();
	}
}
