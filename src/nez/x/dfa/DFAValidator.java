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
	private StringBuilder[] stringContext;

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
		stringContext = new StringBuilder[theNumberOfNonTerminal];
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			nonTerminalContext[i] = productions.get(i).getExpression();
			nonTerminalIDTable.put(productions.get(i).getLocalName(), i);
			System.out.println(productions.get(i).getLocalName() + " <- " + nonTerminalContext[i]);
		}
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			constructAPartOfGraph(i);
		}
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
				System.out.print(nonTerminalRelationGraph[i].get(j) + " ");
			}
			System.out.println("");
		}
		System.out.println("((((((((((((((");
		boolean result = removeUselessNonTerminal();
		if (!result) {
			return false;
		}
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			System.out.println(i + "-th is " + alreadyVerified[i]);
		}
		System.out.println("))))))))))))))");
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			System.out.println(stringContext[i]);
		}
		for (int i = 0; i < theNumberOfNonTerminal; i++) {
			for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
				System.out.print(nonTerminalRelationGraph[i].get(j) + " ");
			}
			System.out.println("");
		}
		return true;
	}

	private StringBuilder replaceAllNonTerminal(StringBuilder context, String before, String after) {
		StringBuilder newContext = new StringBuilder();
		int before_len = before.length();
		for (int i = 0; i < context.length() - before_len + 1; i++) {
			char c = context.charAt(i);
			if (c == before.charAt(0)) {

				String part = context.substring(i, i + before_len);

				if (before.equals(part)) {
					if (i - 1 >= 0 && Character.isDigit(context.charAt(i - 1))) {
						newContext.append(c);
						continue;
					}
					if (i + before_len < context.length() && Character.isDigit(context.charAt(i + before_len))) {
						newContext.append(c);
						continue;
					}
					newContext.append(after);
					i += (before_len - 1);
				} else {
					newContext.append(c);
				}
			} else {
				newContext.append(c);
			}
		}
		return newContext;
	}

	private boolean eval(int nonTerminalID, String context) {
		for (int i = 0; i < context.length(); i++) {
			if (Character.isDigit(context.charAt(i))) {
				int ID = 0;
				int L = i;
				while (i < context.length() && Character.isDigit(context.charAt(i))) {
					ID *= 10;
					ID += (context.charAt(i++) - '0');
				}
				--i;
				int R = i;
				if (ID == nonTerminalID) {
					boolean hasLeft = hasChar(context, L - 1, -1);
					boolean hasRight = hasChar(context, R + 1, +1);
					if (!hasLeft && !hasRight) {
						System.out.println("FOUND : A <- A");
					} else if (hasLeft && !hasRight) {
						// VALID
					} else if (!hasLeft && hasRight) {
						System.out.println("FOUND : A <- Aa");
					} else if (hasLeft && hasRight) {
						System.out.println("FOUND : A <- aAa");
					}
				}
			}
		}
		return true;
	}

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
			ArrayList[] in_edgeID = new ArrayList[theNumberOfNonTerminal];

			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				in_degree[i] = 0;
				in_vertexID[i] = new ArrayList<Integer>();
				in_edgeID[i] = new ArrayList<Integer>();
			}
			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				for (int j = 0; j < nonTerminalRelationGraph[i].size(); j++) {
					ValidateEdge ve = (ValidateEdge) nonTerminalRelationGraph[i].get(j);
					++in_degree[ve.getDst()];
					in_vertexID[ve.getDst()].add(i);
					in_edgeID[ve.getDst()].add(j);
				}
			}

			for (int i = 0; i < theNumberOfNonTerminal; i++) {
				if (in_degree[i] == 0) {
					boolean result = eval(i, stringContext[i].toString());
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
							int edgeID = (int) in_edgeID[i].get(j);
							stringContext[vertexID] = replaceAllNonTerminal(stringContext[vertexID], new Integer(i).toString(), stringContext[i].toString());
							nonTerminalRelationGraph[vertexID].remove(edgeID);

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
		stringContext[nonTerminalID] = new StringBuilder(context);
		System.out.println("context = " + stringContext[nonTerminalID]);
		for (int i = 0; i < context.length(); i++) {
			if (Character.isDigit(context.charAt(i))) {
				int ID = 0;
				int L = i;
				while (i < context.length() && Character.isDigit(context.charAt(i))) {
					ID *= 10;
					ID += (context.charAt(i++) - '0');
				}
				--i;
				int R = i;
				boolean hasLeft = hasChar(context, L - 1, -1);
				boolean hasRight = hasChar(context, R + 1, +1);
				nonTerminalRelationGraph[nonTerminalID].add(new ValidateEdge(nonTerminalID, ID, hasLeft, hasRight));
			}
		}
	}

	private boolean hasChar(String context, int start_pos, int dir) {
		int depth = 0;
		for (int i = start_pos; 0 <= i && i < context.length(); i += dir) {
			char c = context.charAt(i);
			if (depth == 0) {
				if (dir == -1 && c == '{') {
					return false;
				}
				if (dir == +1 && c == '}') {
					return false;
				}
				if (c == '/') {
					return false;
				}
			}
			if (c == '}') {
				++depth;
			}
			if (c == '{') {
				--depth;
			}
			if (depth == 0 && c == 'a') {
				return true;
			}
		}
		return false;
	}

	private String eliminateRedundantCharacters(String context) {
		StringBuilder newContext = new StringBuilder();
		for (int i = 0; i < context.length(); i++) {
			char c = context.charAt(i);
			if (c == '\'') {
				boolean emptyChar = true;
				++i;
				while (i < context.length()) {
					if (i - 1 >= 0 && context.charAt(i - 1) == '\\' && context.charAt(i) == '\'') {
						emptyChar = false;
						++i;
						continue;
					}
					if (context.charAt(i) == '\'') {
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
				if (i + 1 < context.length() && context.charAt(i + 1) == '(') {
					newContext.append('{');
					++i;
				} else {
					newContext.append(c);
				}
			} else if (c == '/' || c == '(' || c == ')') {
				newContext.append(c);
			} else if (Character.isAlphabetic(c)) {
				StringBuilder nonTerminalName = new StringBuilder();
				while (i < context.length()) {
					char c2 = context.charAt(i);
					if (!Character.isAlphabetic(c2) && !Character.isDigit(c2)) {
						break;
					}
					nonTerminalName.append(c2);
					++i;
				}
				--i;
				assert nonTerminalIDTable.containsKey(nonTerminalName.toString()) == true : "no such non-terminal : " + nonTerminalName;
				newContext.append(nonTerminalIDTable.get(nonTerminalName.toString()));
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
					while (i < newContext.length() && Character.isDigit(newContext.charAt(i))) {
						debug = true;
						newContext2.append(newContext.charAt(i++));
					}
					newContext2.append('}');
					assert debug == true;
					--i;
				}
			} else {
				newContext2.append(c);
			}
		}

		return newContext2.toString();
	}
}
