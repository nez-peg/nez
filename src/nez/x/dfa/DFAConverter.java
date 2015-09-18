package nez.x.dfa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import nez.ast.TreeVisitor;
import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cmulti;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;
import nez.lang.expr.Tcapture;
import nez.lang.expr.Tlink;
import nez.lang.expr.Tnew;
import nez.lang.expr.Treplace;
import nez.lang.expr.Xblock;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;

// don't forget to insert \n to the end of input file
// it causes java.lang.ArrayIndexOutOfBoundsException
public class DFAConverter extends TreeVisitor {
	static char epsilon = ' ';
	final protected FileBuilder file;
	final protected GrammarFile grammar;
	private HashMap<String, Integer> initialStateOfNonTerminal;
	private HashMap<String, Integer> acceptingStateOfNonTerminal;
	private int V; // the number of vertices
	static int MAX = 10000; // maximum number of vertices
	private ArrayList<Edge>[] BFA;
	private Graph BFA_graph;
	private boolean showBooleanExpression;

	public DFAConverter(GrammarFile grammar, String name) {
		this.file = new FileBuilder(name);
		this.grammar = grammar;
		this.initialStateOfNonTerminal = new HashMap<String, Integer>();
		this.acceptingStateOfNonTerminal = new HashMap<String, Integer>();
		this.V = 0;
		this.BFA_graph = null;
		this.showBooleanExpression = false;
		BFA = new ArrayList[MAX];
		for (int i = 0; i < MAX; i++) {
			BFA[i] = new ArrayList<Edge>();
		}
		convertToBFA();
	}

	public void convertToBFA() {
		Production p = grammar.getProduction("File");
		String NonTerminalName = p.getLocalName();
		int s = getTnewVertex();
		int t = getTnewVertex();
		initialStateOfNonTerminal.put(NonTerminalName, s);
		acceptingStateOfNonTerminal.put(NonTerminalName, t);

		Graph content = visitProduction(p);

		Set<Integer> S = content.getS();
		Set<Integer> initialState = new HashSet<Integer>();
		Set<Integer> acceptingState = new HashSet<Integer>();
		Set<Integer> acceptingStateLA = content.getAcceptingStateLA();
		Set<Edge> edges = content.getEdges();

		Set<Integer> oldInitialState = content.getInitialState();
		Set<Integer> oldAcceptingState = content.getAcceptingState();

		S.add(s);
		S.add(t);
		initialState.add(s);
		acceptingState.add(t);

		for (Integer i : oldInitialState) {
			edges.add(new Edge(s, i, epsilon, -1));
		}

		for (Integer i : oldAcceptingState) {
			edges.add(new Edge(i, t, epsilon, -1));
		}

		BFA_graph = new Graph(S, initialState, acceptingState, acceptingStateLA, edges);

		for (Edge e : edges) {
			int src = e.getSrc();
			BFA[src].add(e);
		}

		removeRedundantEdges1(); // arbitrary
		removeEpsilonCycle(); // must for exec
		removeRedundantEdges1(); // arbitrary
		// don't eliminate epsilon edges, that is totally

		System.out.println("final state = " + BFA_graph); // for debug

	}

	// change
	// ((vertex1,epsilon),vertex2"normal) -> ((vertex2,epsilon),vertex3"normal") => ((vertex1,epsilon),vertex3"normal)
	// vertex2 is neither initialState nor acceptingState
	// in degree and out degree of vertex2 is equal to 1 and 1, respectively
	private void removeRedundantEdges1() {
		Set<Integer> initialState = BFA_graph.getInitialState();
		Set<Integer> acceptingState = BFA_graph.getAcceptingState();
		Set<Integer> acceptingStateLA = BFA_graph.getAcceptingStateLA();
		Set<Edge> edges = BFA_graph.getEdges();
		boolean update = true;
		Integer[] in_degree = new Integer[V];

		while (update) {
			update = false;
			for (int i = 0; i < V; i++)
				in_degree[i] = 0;
			for (int i = 0; i < V; i++) {
				for (int j = 0; j < BFA[i].size(); j++) {
					int dst = BFA[i].get(j).getDst();
					++in_degree[dst];
				}
			}

			for (int vertex1 = 0; vertex1 < V; vertex1++) {
				for (int j = 0; j < BFA[vertex1].size(); j++) {
					Edge e1 = BFA[vertex1].get(j);
					int vertex2 = e1.getDst();
					if (in_degree[vertex2] != 1)
						continue;
					if (initialState.contains(vertex2) || acceptingState.contains(vertex2) || acceptingStateLA.contains(vertex2))
						continue;
					if (BFA[vertex2].size() != 1)
						continue;
					for (int k = 0; k < BFA[vertex2].size(); k++) {
						Edge e2 = BFA[vertex2].get(k);
						int vertex3 = e2.getDst();
						if (e1.getLabel() == epsilon && e1.getPredicate() == -1 && e2.getLabel() == epsilon && e2.getPredicate() == -1) {
							edges.remove(e1);
							edges.remove(e2);
							edges.add(new Edge(vertex1, vertex3, epsilon, -1));
							update = true;
							break;
						}
					}
					if (update)
						break;
				}
				if (update)
					break;
			}
			if (update) {
				for (int i = 0; i < V; i++) {
					BFA[i].clear();
				}
				for (Edge e : edges) {
					int src = e.getSrc();
					BFA[src].add(e);
				}
			}
		}
		BFA_graph.setEdges(edges);
	}

	private String initialStateColor = "#4169E1"; // royalblue
	private String acceptingStateColor = "#7fffd4"; // aquamarine
	private String acceptingStateLAColor = "#ff6347"; // tomato

	public void convertBFAtoDOT() {
		if (BFA_graph != null) {
			Set<Edge> edges = BFA_graph.getEdges();
			Set<Integer> initialState = BFA_graph.getInitialState();
			Set<Integer> acceptingState = BFA_graph.getAcceptingState();
			Set<Integer> acceptingStateLA = BFA_graph.getAcceptingStateLA();
			ConsoleUtils.println("\ndigraph g {");
			for (Integer i : initialState) {
				ConsoleUtils.println("\"" + i + "\"[style=filled,fillcolor=\"" + initialStateColor + "\"];");
			}
			for (Integer i : acceptingState) {
				ConsoleUtils.println("\"" + i + "\"[peripheries=2,style=filled,fillcolor=\"" + acceptingStateColor + "\"];");
			}
			for (Integer i : acceptingStateLA) {
				ConsoleUtils.println("\"" + i + "\"[peripheries=2,style=filled,fillcolor=\"" + acceptingStateLAColor + "\"];");
			}
			for (Edge e : edges) {
				char label = e.getLabel();
				int predicate = e.getPredicate();
				// ConsoleUtils.println("  \"" + e.getSrc() + "\"->\"" +
				// e.getDst() + "\"[label=\"" +
				// ((label==epsilon)?"epsilon":label) + "," +
				// ((predicate==-1)?"normal":((predicate==0)?"&predicate":"!predicate"))
				// + "\"];");
				ConsoleUtils.print("  \"" + e.getSrc() + "\"->\"" + e.getDst() + "\"[label=\"");
				if (predicate == 0) {
					ConsoleUtils.print("&predicate");
				} else if (predicate == 1) {
					ConsoleUtils.print("!predicate");
				} else if (label != epsilon) {
					ConsoleUtils.print(label);
				} else {
					ConsoleUtils.print("epsilon");
				}
				ConsoleUtils.println("\"];");
			}
			ConsoleUtils.println("}");
		}
	}

	private void execCommandLine(String command) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);
			pr.waitFor();
		} catch (Exception e) {
			ConsoleUtils.println(e);
		}
	}

	final private String dotFileName = "__bfa";

	public void printBFA() {
		try {
			File file = new File(dotFileName + ".dot");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
			if (BFA_graph != null) {
				Set<Edge> edges = BFA_graph.getEdges();
				Set<Integer> initialState = BFA_graph.getInitialState();
				Set<Integer> acceptingState = BFA_graph.getAcceptingState();
				Set<Integer> acceptingStateLA = BFA_graph.getAcceptingStateLA();
				pw.println("\ndigraph g {");
				for (Integer i : initialState) {
					pw.println("\"" + i + "\"[style=filled,fillcolor=\"" + initialStateColor + "\"];");
				}
				for (Integer i : acceptingState) {
					pw.println("\"" + i + "\"[peripheries=2,style=filled,fillcolor=\"" + acceptingStateColor + "\"];");
				}
				for (Integer i : acceptingStateLA) {
					pw.println("\"" + i + "\"[peripheries=2,style=filled,fillcolor=\"" + acceptingStateLAColor + "\"];");
				}
				for (Edge e : edges) {
					char label = e.getLabel();
					int predicate = e.getPredicate();
					// pw.println("  \"" + e.getSrc() + "\"->\"" + e.getDst() +
					// "\"[label=\"" + ((label==epsilon)?"epsilon":label) + ","
					// +
					// ((predicate==-1)?"normal":((predicate==0)?"&predicate":"!predicate"))
					// + "\"];");
					pw.print("  \"" + e.getSrc() + "\"->\"" + e.getDst() + "\"[label=\"");
					if (predicate == 0) {
						pw.print("&predicate");
					} else if (predicate == 1) {
						pw.print("!predicate");
					} else if (label != epsilon) {
						pw.print(label);
					} else {
						pw.print("epsilon");
					}
					pw.println("\"];");
				}
				pw.println("}");
				pw.close();
			}
		} catch (IOException e) {
			ConsoleUtils.println(e);
		}

		execCommandLine("dot -Kdot -Tpng " + dotFileName + ".dot -o " + dotFileName + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

	}

	private void updateBFA() {
		Set<Edge> edges = BFA_graph.getEdges();
		for (int i = 0; i < V; i++) {
			BFA[i].clear();
		}
		for (Edge e : edges) {
			int src = e.getSrc();
			BFA[src].add(e);
		}
	}

	private void removeEpsilonCycle() {
		SCC scc = new SCC(V, BFA_graph);
		BFA_graph = scc.removeEpsilonCycle();
		Set<Integer> S = BFA_graph.getS();
		--V;
		for (Integer i : S) {
			if (V < i) {
				V = i;
			}
		}
		++V;
		updateBFA();
	}

	private Map<Integer, String> memo;

	private String computeNextBooleanExpression(Integer booleanVariable) {
		if (memo.containsKey(booleanVariable)) {
			return memo.get(booleanVariable);
		}
		boolean fin = true;
		for (int i = 0; i < BFA[booleanVariable].size(); i++) {
			Edge e = BFA[booleanVariable].get(i);
			if (e.getLabel() == epsilon) {
				fin = false;
				break;
			}
		}
		if (fin) {
			memo.put(booleanVariable, booleanVariable.toString());
			return booleanVariable.toString();
		}
		ArrayList<Integer> normal = new ArrayList<Integer>();
		ArrayList<Integer> andPredicate = new ArrayList<Integer>();
		ArrayList<Integer> notPredicate = new ArrayList<Integer>();
		boolean myself = false;
		for (int i = 0; i < BFA[booleanVariable].size(); i++) {
			Edge e = BFA[booleanVariable].get(i);
			if (e.getPredicate() == 0) { // and
				andPredicate.add(e.getDst());
			} else if (e.getPredicate() == 1) { // not
				notPredicate.add(e.getDst());
			} else {
				if (e.getLabel() == epsilon) {
					normal.add(e.getDst());
				} else {
					myself = true;
				}
			}
		}
		// String booleanExpression = "(";
		StringBuilder booleanExpression = new StringBuilder("(");
		if (normal.size() > 0) {
			// booleanExpression += "(";
			booleanExpression.append("(");
			if (myself) {
				// booleanExpression += ( booleanVariable.toString() + "|" );
				booleanExpression.append(booleanVariable.toString()).append("|");
			}
			for (int i = 0; i < normal.size(); i++) {
				// String tmp = computeNextBooleanExpression(normal.get(i)) +
				// "|";
				// booleanExpression += tmp;
				booleanExpression.append(computeNextBooleanExpression(normal.get(i))).append("|");
			}
			if (booleanExpression.charAt(booleanExpression.length() - 1) == '|' || booleanExpression.charAt(booleanExpression.length() - 1) == '&') {
				// booleanExpression =
				// booleanExpression.substring(0,booleanExpression.length()-1);
				booleanExpression = new StringBuilder(booleanExpression.substring(0, booleanExpression.length() - 1));
			}
			// booleanExpression += ")";
			booleanExpression.append(")");
		}
		if (andPredicate.size() > 0) {
			if (booleanExpression.charAt(booleanExpression.length() - 1) == ')') {
				// booleanExpression += "&";
				booleanExpression.append("&");
			}
			for (int i = 0; i < andPredicate.size(); i++) {
				// String tmp =
				// computeNextBooleanExpression(andPredicate.get(i)) + "&";
				// booleanExpression += tmp;
				booleanExpression.append(computeNextBooleanExpression(andPredicate.get(i))).append("&");
			}
		}
		if (notPredicate.size() > 0) {
			if (booleanExpression.charAt(booleanExpression.length() - 1) == ')') {
				// booleanExpression += "&";
				booleanExpression.append("&");
			}
			for (int i = 0; i < notPredicate.size(); i++) {
				// String tmp = "!" +
				// computeNextBooleanExpression(notPredicate.get(i)) + "&";
				// booleanExpression += tmp;
				booleanExpression.append("!").append(computeNextBooleanExpression(notPredicate.get(i))).append("&");
			}
		}

		if (booleanExpression.charAt(booleanExpression.length() - 1) == '|' || booleanExpression.charAt(booleanExpression.length() - 1) == '&') {
			// booleanExpression =
			// booleanExpression.substring(0,booleanExpression.length()-1);
			booleanExpression = new StringBuilder(booleanExpression.substring(0, booleanExpression.length() - 1));
		}
		// booleanExpression += ")";
		booleanExpression.append(")");
		memo.put(booleanVariable, booleanExpression.toString());
		return booleanExpression.toString();
	}

	private String removeRedundantParentheses(String s) {
		boolean[] redundancy = new boolean[s.length()];
		for (int i = 0; i < s.length(); i++) {
			redundancy[i] = false;
		}
		Stack<Integer> stk = new Stack<Integer>();
		for (int i = 0; i < s.length(); i++) {
			if (s.charAt(i) == '(') {
				stk.push(i);
			} else if (s.charAt(i) == ')') {
				int L = stk.pop();
				int R = i;
				--L;
				++R;
				while (0 <= L && R < s.length() && s.charAt(L) == '(' && s.charAt(R) == ')') {
					redundancy[L] = redundancy[R] = true;
					--L;
					++R;
				}
			}
		}
		String simple_s = "";
		for (int i = 0; i < s.length(); i++) {
			if (redundancy[i])
				continue;
			simple_s += s.charAt(i);
		}
		return simple_s;
	}

	private boolean evalBooleanExpression(String booleanExpression) {
		Set<Integer> AcceptingState = BFA_graph.getAcceptingState();
		Set<Integer> AcceptingStateLA = BFA_graph.getAcceptingStateLA();
		String tmpBooleanExpression = "";
		for (int j = 0; j < booleanExpression.length(); j++) {
			if (Character.isDigit(booleanExpression.charAt(j))) {
				int v = 0;
				while (j < booleanExpression.length() && Character.isDigit(booleanExpression.charAt(j))) {
					v *= 10;
					v += (booleanExpression.charAt(j) - '0');
					++j;
				}
				--j;
				tmpBooleanExpression += ((AcceptingState.contains(v) || AcceptingStateLA.contains(v)) ? "T" : "F");
			} else {
				tmpBooleanExpression += booleanExpression.charAt(j);
			}
		}
		BooleanExpressionEvaluator bee = new BooleanExpressionEvaluator(tmpBooleanExpression);
		return bee.eval();
	}

	private String eliminateEpsilonFromBooleanExpression(String booleanExpression) {
		StringBuilder tmpBooleanExpression = new StringBuilder("");
		for (int j = 0; j < booleanExpression.length(); j++) {
			if (Character.isDigit(booleanExpression.charAt(j))) {
				int v = 0;
				while (j < booleanExpression.length() && Character.isDigit(booleanExpression.charAt(j))) {
					v *= 10;
					v += (booleanExpression.charAt(j) - '0');
					++j;
				}
				--j;
				// tmpBooleanExpression += computeNextBooleanExpression(v);
				tmpBooleanExpression.append(computeNextBooleanExpression(v));
			} else {
				// tmpBooleanExpression += booleanExpression.charAt(j);
				tmpBooleanExpression.append(booleanExpression.charAt(j));
			}
		}
		return removeRedundantParentheses(tmpBooleanExpression.toString());
	}

	// !!!NOTICE!!! REMOVE ALL F! LET'S IMPLEMENT
	private boolean FAILED;

	private String moveToTheNextState(char c, String booleanExpression) {
		StringBuilder tmpBooleanExpression = new StringBuilder("");
		FAILED = true;
		for (int j = 0; j < booleanExpression.length(); j++) {
			if (Character.isDigit(booleanExpression.charAt(j))) {
				int v = 0;
				while (j < booleanExpression.length() && Character.isDigit(booleanExpression.charAt(j))) {
					v *= 10;
					v += (booleanExpression.charAt(j) - '0');
					++j;
				}
				--j;
				ArrayList<Integer> next = new ArrayList<Integer>();
				for (int k = 0; k < BFA[v].size(); k++) {
					Edge e = BFA[v].get(k);
					if (e.getLabel() != c && e.getLabel() != '.')
						continue;
					next.add(e.getDst());
				}
				if (next.size() > 0) {
					// tmpBooleanExpression += "(";
					tmpBooleanExpression.append("(");
					for (int k = 0; k < next.size(); k++) {
						if (k > 0) {
							// tmpBooleanExpression += "|";
							tmpBooleanExpression.append("|");
						}
						// tmpBooleanExpression += next.get(k).toString();
						if (verifyPredicate(next.get(k).toString())) {
							tmpBooleanExpression.append("T");
						} else {
							tmpBooleanExpression.append(next.get(k).toString());
						}
					}
					// tmpBooleanExpression += ")";
					tmpBooleanExpression.append(")");
					FAILED = false;
				} else {
					// tmpBooleanExpression += "F";
					tmpBooleanExpression.append("F");
				}
			} else {

				if (j + 1 < booleanExpression.length() && booleanExpression.charAt(j) == 'F' && booleanExpression.charAt(j + 1) == '|') {
					++j;
					continue;
				}

				// tmpBooleanExpression += booleanExpression.charAt(j);
				tmpBooleanExpression.append(booleanExpression.charAt(j));
			}
		}
		return removeRedundantParentheses(tmpBooleanExpression.toString());
	}

	Set<Integer> inVerifyPredicateAcceptingStateLA;

	private boolean verifyPredicate(String booleanVariable) {
		String booleanExpression = eliminateEpsilonFromBooleanExpression(booleanVariable);
		for (int i = 0; i < booleanExpression.length(); i++) {
			if (Character.isDigit(booleanExpression.charAt(i))) {
				int v = 0;
				while (i + 1 < booleanExpression.length() && Character.isDigit(booleanExpression.charAt(i))) {
					v *= 10;
					v += (booleanExpression.charAt(i) - '0');
					++i;
				}
				--i;
				if (inVerifyPredicateAcceptingStateLA.contains(v)) {
					return true;
				}
			}
		}
		return false;
	}

	// execute on DFA
	// On-the-Fly
	public boolean exec(String text) {
		inVerifyPredicateAcceptingStateLA = BFA_graph.getAcceptingStateLA();
		FAILED = false;
		memo = new HashMap<Integer, String>();
		String booleanExpression = "";
		Set<Integer> initialState = BFA_graph.getInitialState();
		for (Integer i : initialState) { // assert initialState.size() == 1
			booleanExpression = i.toString();
		}

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			booleanExpression = eliminateEpsilonFromBooleanExpression(booleanExpression);
			if (showBooleanExpression) {
				System.out.println("booleanExpression = " + booleanExpression);
			}
			booleanExpression = moveToTheNextState(c, booleanExpression);
			if (FAILED) {
				break;
			}
		}
		booleanExpression = eliminateEpsilonFromBooleanExpression(booleanExpression);
		if (showBooleanExpression) {
			System.out.println("final booleanExpression = " + booleanExpression);
		}
		return evalBooleanExpression(booleanExpression);
	}

	private int getTnewVertex() {
		return V++;
	}

	public String getDesc() {
		return "dfa";
	}

	public void switchShowBooleanExpression() {
		showBooleanExpression = !showBooleanExpression;
	}

	// Visitor

	public Graph visitProduction(Production rule) {
		// System.out.println("here is Production : " + rule);
		Expression e = rule.getExpression();
		if (e instanceof Pchoice) {
			return visitPchoice(e);
		} else {
			return visitExpression(e);
		}
	}

	public Graph visitExpression(Expression e) {
		if (e instanceof Pempty) {
			return visitPempty(e);
		} else if (e instanceof Pfail) {
			return visitPfail(e);
		} else if (e instanceof Cany) {
			return visitCany(e);
		} else if (e instanceof Cbyte) {
			return visitCbyte(e);
		} else if (e instanceof Cset) {
			return visitCset(e);
		} else if (e instanceof Poption) {
			return visitPoption(e);
		} else if (e instanceof Pzero) {
			return visitPzero(e);
		} else if (e instanceof Pone) {
			return visitPone(e);
		} else if (e instanceof Pand) {
			return visitPand(e);
		} else if (e instanceof Pnot) {
			return visitPnot(e);
		} else if (e instanceof Psequence) {
			return visitPsequence(e);
		} else if (e instanceof Pchoice) {
			return visitPchoice(e);
		} else if (e instanceof nez.lang.expr.NonTerminal) {
			return visitNonTerminal(e);
		} else if (e instanceof Cmulti) {
			return visitCmulti(e);
		} else if (e instanceof Tlink) {
			return visitTlink(e);
		} else if (e instanceof Tnew) {
			return visitTnew(e);
		} else if (e instanceof Tcapture) {
			return visitTcapture(e);
		} else if (e instanceof Treplace) {
			return visitTreplace(e);
		} else if (e instanceof Xblock) {
			return visitXblock(e);
		}
		assert false : "invalid instance";
		return null;
	}

	private Graph visitXblock(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Xblock : " + e);
		return null;
	}

	private Graph visitTreplace(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Treplace : " + e);
		return null;
	}

	private Graph visitTcapture(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Tcapture : " + e);
		return visitExpression(e);
	}

	private Graph visitTnew(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Tnew : " + e);
		return null;
	}

	private Graph visitTlink(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Tlink : " + e);
		return null;
	}

	private Graph visitCmulti(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Cmulti : " + e);
		return null;
	}

	private Graph visitNonTerminal(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is NonTerminal : " + e);
		String NonTerminalName = ((NonTerminal) e).getLocalName();
		if (initialStateOfNonTerminal.containsKey(NonTerminalName)) {
			int s = getTnewVertex();
			int t = getTnewVertex();
			int nonTerminal_s = initialStateOfNonTerminal.get(NonTerminalName);
			int nonTerminal_t = acceptingStateOfNonTerminal.get(NonTerminalName);
			Set<Integer> S = new HashSet<Integer>();
			Set<Integer> initialState = new HashSet<Integer>();
			Set<Integer> acceptingState = new HashSet<Integer>();
			Set<Integer> acceptingStateLA = new HashSet<Integer>();
			Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());
			S.add(s);
			S.add(t);
			S.add(nonTerminal_s);
			S.add(nonTerminal_t);
			initialState.add(s);
			acceptingState.add(t);
			edges.add(new Edge(s, nonTerminal_s, epsilon, -1));
			edges.add(new Edge(nonTerminal_t, t, epsilon, -1));
			return new Graph(S, initialState, acceptingState, acceptingStateLA, edges);
		} else {

			int s = getTnewVertex();
			int t = getTnewVertex();
			initialStateOfNonTerminal.put(NonTerminalName, s);
			acceptingStateOfNonTerminal.put(NonTerminalName, t);

			Graph content = visitProduction(((NonTerminal) e).getProduction());

			Set<Integer> S = content.getS();
			Set<Integer> initialState = new HashSet<Integer>();
			Set<Integer> acceptingState = new HashSet<Integer>();
			Set<Integer> acceptingStateLA = content.getAcceptingStateLA();
			Set<Edge> edges = content.getEdges();

			Set<Integer> oldInitialState = content.getInitialState();
			Set<Integer> oldAcceptingState = content.getAcceptingState();

			S.add(s);
			S.add(t);
			initialState.add(s);
			acceptingState.add(t);

			for (Integer i : oldInitialState) {
				edges.add(new Edge(s, i, epsilon, -1));
			}

			for (Integer i : oldAcceptingState) {
				edges.add(new Edge(i, t, epsilon, -1));
			}

			return new Graph(S, initialState, acceptingState, acceptingStateLA, edges);
		}
		// return null;
	}

	private Graph visitPchoice(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pchoice : " + e);
		int new_s = getTnewVertex();
		int new_t = getTnewVertex();
		Set<Integer> newS = new HashSet<Integer>();
		Set<Integer> newInitialState = new HashSet<Integer>();
		Set<Integer> newAcceptingState = new HashSet<Integer>();
		Set<Integer> newAcceptingStateLA = new HashSet<Integer>();
		Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());
		newS.add(new_s);
		newS.add(new_t);
		newInitialState.add(new_s);
		newAcceptingState.add(new_t);
		for (int i = 0; i < e.size(); i++) {
			Graph g = visitExpression(e.get(i));
			Set<Integer> g_S = g.getS();
			Set<Integer> g_initialState = g.getInitialState();
			Set<Integer> g_acceptingState = g.getAcceptingState();
			Set<Integer> g_acceptingStateLA = g.getAcceptingStateLA();
			Set<Edge> g_edges = g.getEdges();

			for (Integer g_i : g_S) {
				newS.add(g_i);
			}

			for (Integer g_i : g_initialState) {
				edges.add(new Edge(new_s, g_i, epsilon, -1));
			}

			for (Integer g_i : g_acceptingState) {
				edges.add(new Edge(g_i, new_t, epsilon, -1));
			}

			for (Integer g_i : g_acceptingStateLA) {
				newAcceptingStateLA.add(g_i);
			}

			for (Edge g_i : g_edges) {
				edges.add(g_i);
			}

		}

		return new Graph(newS, newInitialState, newAcceptingState, newAcceptingStateLA, edges);
	}

	private Graph visitPsequence(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Psequence : " + e);
		return getPsequence(e);
	}

	private Graph visitPnot(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pnot : " + e);
		Graph g1 = getPsequence(e);
		int new_s = getTnewVertex();
		Set<Integer> oldInitialState = g1.getInitialState();
		Set<Edge> edges = g1.getEdges();
		for (Integer i : oldInitialState) {
			edges.add(new Edge(new_s, i, epsilon, 1));
		}
		Set<Integer> newS = g1.getS();
		newS.add(new_s);
		Set<Integer> newInitialState = new HashSet<Integer>();
		newInitialState.add(new_s);
		Set<Integer> acceptingState = new HashSet<Integer>();
		Set<Integer> acceptingStateLA = g1.getAcceptingState(); //
		Set<Integer> tmpAcceptingStateLA = g1.getAcceptingStateLA(); //
		for (Integer i : tmpAcceptingStateLA) { //
			acceptingStateLA.add(i); //
		} //
		return new Graph(newS, newInitialState, acceptingState, acceptingStateLA, edges);
		// return new
		// Graph(newS,newInitialState,acceptingState,g1.getAcceptingState(),edges);
	}

	private Graph fixPredicate(Graph g) {
		Set<Integer> initialState = g.getInitialState();
		if (initialState.size() != 1) { // how to use assert
			System.err.println("---___---initialState.size() = " + initialState.size() + "---___---");
		}
		int s = -1;
		for (Integer i : initialState) {
			s = i;
		}
		Set<Edge> oldEdges = g.getEdges();
		Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());
		for (Edge e : oldEdges) {
			if (e.getSrc() == s) {
				if (e.getPredicate() == -1) { // how to use assert
					System.err.println("---___---");
				}
				edges.add(new Edge(s, e.getDst(), e.getLabel(), -1));
			} else {
				edges.add(e);
			}
		}
		return new Graph(g.getS(), initialState, g.getAcceptingState(), g.getAcceptingStateLA(), edges);
	}

	private Graph getPsequence(Expression e) {
		Deque<Graph> predicate = new ArrayDeque<Graph>();
		Deque<Integer> p_types = new ArrayDeque<Integer>();
		Graph graph = null;
		for (int i = 0; i < e.size(); i++) {
			Expression tmp = e.get(i);
			if (tmp instanceof Pand || tmp instanceof Pnot) {
				predicate.addLast(visitExpression(tmp));
				p_types.addLast((tmp instanceof Pand) ? 0 : 1);
			} else {
				Graph graph2 = visitExpression(tmp);
				if (predicate.size() > 0) {
					int new_s = getTnewVertex();
					Set<Integer> newS = graph2.getS();
					Set<Integer> newInitialState = new HashSet<Integer>();
					Set<Integer> newAcceptingState = graph2.getAcceptingState();
					Set<Integer> newAcceptingStateLA = graph2.getAcceptingStateLA();
					Set<Edge> edges = graph2.getEdges();

					newS.add(new_s);
					newInitialState.add(new_s);

					Set<Integer> graph2_initialState = graph2.getInitialState();
					for (Integer j : graph2_initialState) {
						edges.add(new Edge(new_s, j, epsilon, -1));
					}

					while (predicate.size() > 0) {
						Graph p_graph = fixPredicate(predicate.pollFirst());
						Integer p_type = p_types.pollFirst();
						Set<Integer> oldS = p_graph.getS();
						Set<Integer> oldInitialState = p_graph.getInitialState();
						Set<Integer> oldAcceptingState = p_graph.getAcceptingState();
						Set<Integer> oldAcceptingStateLA = p_graph.getAcceptingStateLA();
						Set<Edge> oldEdges = p_graph.getEdges();

						for (Integer j : oldS) {
							newS.add(j);
						}

						for (Integer j : oldInitialState) { // assert
															// oldInitialState.size()
															// == 1
							edges.add(new Edge(new_s, j, epsilon, p_type));
						}

						if (oldAcceptingState.size() != 0) { // how to use
																// assert
							System.err.println("___---___empty___---___empty___---___");
						}

						for (Integer j : oldAcceptingStateLA) {
							newAcceptingStateLA.add(j);
						}

						for (Edge edge : oldEdges) {
							edges.add(edge);
						}
					}
					graph2 = new Graph(newS, newInitialState, newAcceptingState, newAcceptingStateLA, edges);
				}
				graph = concatenate(graph, graph2);
			}
		}

		if (predicate.size() > 0) {
			int new_s = getTnewVertex();
			Set<Integer> newS = new HashSet<Integer>();
			Set<Integer> newInitialState = new HashSet<Integer>();
			Set<Integer> newAcceptingState = new HashSet<Integer>();
			Set<Integer> newAcceptingStateLA = new HashSet<Integer>();
			Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());

			newS.add(new_s);
			newInitialState.add(new_s);

			while (predicate.size() > 0) {
				Graph p_graph = fixPredicate(predicate.pollFirst());
				Integer p_type = p_types.pollFirst();
				Set<Integer> oldS = p_graph.getS();
				Set<Integer> oldInitialState = p_graph.getInitialState();
				Set<Integer> oldAcceptingState = p_graph.getAcceptingState();
				Set<Integer> oldAcceptingStateLA = p_graph.getAcceptingStateLA();
				Set<Edge> oldEdges = p_graph.getEdges();

				for (Integer j : oldS) {
					newS.add(j);
				}

				for (Integer j : oldInitialState) { // assert
													// oldInitialState.size() ==
													// 1
					edges.add(new Edge(new_s, j, epsilon, p_type));
				}

				if (oldAcceptingState.size() != 0) { // how to use assert
					System.err.println("___---___empty___---___empty___---___");
				}

				for (Integer j : oldAcceptingStateLA) {
					newAcceptingStateLA.add(j);
				}

				for (Edge edge : oldEdges) {
					edges.add(edge);
				}
			}
			Graph graph2 = new Graph(newS, newInitialState, newAcceptingState, newAcceptingStateLA, edges);
			graph = concatenate(graph, graph2);
		}

		return graph;
	}

	private Graph concatenate(Graph g1, Graph g2) {
		if (g1 == null) {
			return g2;
		}
		if (g2 == null) {
			return g1;
		}
		int bridge_vertex = getTnewVertex();
		Set<Edge> edges = g1.getEdges();
		Set<Edge> g2_edges = g2.getEdges();
		for (Edge e : g2_edges) {
			edges.add(e);
		}
		Set<Integer> newS = g1.getS();
		Set<Integer> g1_acceptingState = g1.getAcceptingState();
		Set<Integer> newAcceptingStateLA = g1.getAcceptingStateLA();
		Set<Integer> g2_S = g2.getS();
		Set<Integer> g2_initialState = g2.getInitialState();
		Set<Integer> g2_acceptingState = g2.getAcceptingState();
		Set<Integer> g2_acceptingStateLA = g2.getAcceptingStateLA();

		newS.add(bridge_vertex);
		for (Integer i : g2_S) {
			newS.add(i);
		}

		for (Integer i : g1_acceptingState) {
			edges.add(new Edge(i, bridge_vertex, epsilon, -1));
		}
		for (Integer i : g2_initialState) {
			edges.add(new Edge(bridge_vertex, i, epsilon, -1));
		}

		for (Integer i : g2_acceptingStateLA) {
			newAcceptingStateLA.add(i);
		}

		return new Graph(newS, g1.getInitialState(), g2_acceptingState, newAcceptingStateLA, edges);
	}

	private Graph visitPand(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pand : " + e);
		Graph g1 = getPsequence(e);
		int new_s = getTnewVertex();
		Set<Integer> oldInitialState = g1.getInitialState();
		Set<Edge> edges = g1.getEdges();
		for (Integer i : oldInitialState) {
			edges.add(new Edge(new_s, i, epsilon, 0));
		}
		Set<Integer> newS = g1.getS();
		newS.add(new_s);
		Set<Integer> newInitialState = new HashSet<Integer>();
		newInitialState.add(new_s);
		Set<Integer> acceptingState = new HashSet<Integer>();
		Set<Integer> acceptingStateLA = g1.getAcceptingState(); //
		Set<Integer> tmpAcceptingStateLA = g1.getAcceptingStateLA(); //
		for (Integer i : tmpAcceptingStateLA) { //
			acceptingStateLA.add(i); //
		} //
		return new Graph(newS, newInitialState, acceptingState, acceptingStateLA, edges);
		// return new
		// Graph(newS,newInitialState,acceptingState,g1.getAcceptingState(),edges);
	}

	private Graph visitPone(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pone : " + e);
		Graph graph = visitExpression(e.get(0));
		int new_s = getTnewVertex();
		int new_t = getTnewVertex();
		Set<Integer> oldInitialState = graph.getInitialState();
		Set<Integer> oldAcceptingState = graph.getAcceptingState();
		Set<Edge> edges = graph.getEdges();

		// thompson
		for (Integer old_s : oldInitialState) {
			for (Integer old_t : oldAcceptingState) {
				edges.add(new Edge(old_t, old_s, epsilon, -1));
				edges.add(new Edge(new_s, old_s, epsilon, -1));
				edges.add(new Edge(old_t, new_t, epsilon, -1));
			}
		}

		Set<Integer> newInitialState = new HashSet<Integer>();
		Set<Integer> newAcceptingState = new HashSet<Integer>();
		newInitialState.add(new_s);
		newAcceptingState.add(new_t);

		Set<Integer> newS = graph.getS();
		newS.add(new_s);
		newS.add(new_t);
		return new Graph(newS, newInitialState, newAcceptingState, graph.getAcceptingStateLA(), edges);
	}

	private Graph visitPzero(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pzero : " + e);
		Graph graph = visitExpression(e.get(0));
		int new_s = getTnewVertex();
		int new_t = getTnewVertex();
		Set<Integer> oldInitialState = graph.getInitialState();
		Set<Integer> oldAcceptingState = graph.getAcceptingState();
		Set<Edge> edges = graph.getEdges();

		// thompson
		for (Integer old_s : oldInitialState) {
			for (Integer old_t : oldAcceptingState) {
				edges.add(new Edge(old_t, old_s, epsilon, -1));
				edges.add(new Edge(new_s, old_s, epsilon, -1));
				edges.add(new Edge(old_t, new_t, epsilon, -1));
			}
		}

		edges.add(new Edge(new_s, new_t, epsilon, -1));

		Set<Integer> newInitialState = new HashSet<Integer>();
		Set<Integer> newAcceptingState = new HashSet<Integer>();
		newInitialState.add(new_s);
		newAcceptingState.add(new_t);

		Set<Integer> newS = graph.getS();
		newS.add(new_s);
		newS.add(new_t);

		return new Graph(newS, newInitialState, newAcceptingState, graph.getAcceptingStateLA(), edges);
	}

	private Graph visitPoption(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Poption : " + e);
		Graph graph = visitExpression(e.get(0));
		int new_s = getTnewVertex();
		int new_t = getTnewVertex();
		Set<Integer> oldInitialState = graph.getInitialState();
		Set<Integer> oldAcceptingState = graph.getAcceptingState();
		Set<Edge> edges = graph.getEdges();

		for (Integer old_s : oldInitialState) {
			for (Integer old_t : oldAcceptingState) {
				edges.add(new Edge(new_s, old_s, epsilon, -1));
				edges.add(new Edge(old_t, new_t, epsilon, -1));
			}
		}

		edges.add(new Edge(new_s, new_t, epsilon, -1));

		Set<Integer> newInitialState = new HashSet<Integer>();
		Set<Integer> newAcceptingState = new HashSet<Integer>();
		newInitialState.add(new_s);
		newAcceptingState.add(new_t);

		Set<Integer> newS = graph.getS();
		newS.add(new_s);
		newS.add(new_t);

		return new Graph(newS, newInitialState, newAcceptingState, graph.getAcceptingStateLA(), edges);
	}

	private Graph visitCset(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Cset : " + e);
		Set<Integer> S = new HashSet<Integer>();
		Set<Integer> initialState = new HashSet<Integer>();
		Set<Integer> acceptingState = new HashSet<Integer>();
		Set<Integer> acceptingStateLA = new HashSet<Integer>();
		Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());
		int s = getTnewVertex();
		int t = getTnewVertex();
		initialState.add(s);
		acceptingState.add(t);
		S.add(s);
		S.add(t);
		for (int i = 0; i < 256; i++)
			if (((Cset) e).byteMap[i]) {
				edges.add(new Edge(s, t, (char) i, -1));
			}
		return new Graph(S, initialState, acceptingState, acceptingStateLA, edges);
	}

	private Graph visitCbyte(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Cbyte : " + e);
		Set<Integer> S = new HashSet<Integer>();
		Set<Integer> initialState = new HashSet<Integer>();
		Set<Integer> acceptingState = new HashSet<Integer>();
		Set<Integer> acceptingStateLA = new HashSet<Integer>();
		Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());
		int s = getTnewVertex();
		int t = getTnewVertex();
		initialState.add(s);
		acceptingState.add(t);
		S.add(s);
		S.add(t);
		edges.add(new Edge(s, t, (char) ((Cbyte) e).byteChar, -1));
		return new Graph(S, initialState, acceptingState, acceptingStateLA, edges);
	}

	private Graph visitCany(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Cany : " + e);
		Set<Integer> S = new HashSet<Integer>();
		Set<Integer> initialState = new HashSet<Integer>();
		Set<Integer> acceptingState = new HashSet<Integer>();
		Set<Integer> acceptingStateLA = new HashSet<Integer>();
		Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());
		int s = getTnewVertex();
		int t = getTnewVertex();
		initialState.add(s);
		acceptingState.add(t);
		S.add(s);
		S.add(t);
		edges.add(new Edge(s, t, '.', -1));
		return new Graph(S, initialState, acceptingState, acceptingStateLA, edges);
	}

	private Graph visitPfail(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pfail : " + e);
		return null;
	}

	private Graph visitPempty(Expression e) {
		// System.out.println("here is Pempty : " + e);
		Set<Integer> S = new HashSet<Integer>();
		Set<Integer> initialState = new HashSet<Integer>();
		Set<Integer> acceptingState = new HashSet<Integer>();
		Set<Integer> acceptingStateLA = new HashSet<Integer>();
		Set<Edge> edges = new TreeSet<Edge>(new EdgeComparator());
		int s = getTnewVertex();
		int t = getTnewVertex();
		initialState.add(s);
		acceptingState.add(t);
		S.add(s);
		S.add(t);
		edges.add(new Edge(s, t, epsilon, -1));
		return new Graph(S, initialState, acceptingState, acceptingStateLA, edges);
	}

}
