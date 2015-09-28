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
import java.util.TreeMap;
import java.util.TreeSet;

import nez.Grammar;
import nez.ast.AbstractTreeVisitor;
import nez.io.SourceContext;
import nez.io.StringContext;
import nez.lang.Expression;
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

/*
 * 高速化のためにできること memo
 * 1. 論理式の木のつなぎ方を逆にしたほうが良いかもしれない
 * exec の際に木を左->右の順に見ていくので、左にsigmaがあれば文字数を明らかに超えるのとかは即終了できる、入力次第だけどましな気はする
 * 2. execMemo をより活用するために、 And, Or, Not にもユニークな stateID を割り振る
 * 現在特別割り振っていない（確か全て-1にしてる）ので、これらにも番号つければメモ化できるよ 
 */

// don't forget to insert \n at the end of an input file
// it causes java.lang.ArrayIndexOutOfBoundsException
public class DFAConverter extends AbstractTreeVisitor {
	static char epsilon = ' ';
	final protected FileBuilder file;
	final protected Grammar grammar;
	private HashMap<String, Integer> initialStateOfNonTerminal;
	private HashMap<String, Integer> acceptingStateOfNonTerminal;
	private int V; // the number of vertices
	static int MAX = 10000; // maximum number of vertices
	private ArrayList<Edge>[] bfa;
	private EpsilonBFA BFA_graph;
	private BFA final_bfa;
	private boolean showBooleanExpression;

	// public DFAConverter(GrammarFile grammar, String name) {
	public DFAConverter(Grammar grammar, String name) {
		this.file = new FileBuilder(name);
		this.grammar = grammar;
		this.initialStateOfNonTerminal = new HashMap<String, Integer>();
		this.acceptingStateOfNonTerminal = new HashMap<String, Integer>();
		this.V = 0;
		this.BFA_graph = null;
		this.showBooleanExpression = false;
		bfa = new ArrayList[MAX];
		for (int i = 0; i < MAX; i++) {
			bfa[i] = new ArrayList<Edge>();
		}
		convertToBFA();
	}

	public void convertToBFA() {
		Production p = grammar.getProduction("Start");

		String NonTerminalName = p.getLocalName();
		int s = getTnewVertex();
		int t = getTnewVertex();
		initialStateOfNonTerminal.put(NonTerminalName, s);
		acceptingStateOfNonTerminal.put(NonTerminalName, t);

		EpsilonBFA content = visitProduction(p);

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

		BFA_graph = new EpsilonBFA(S, initialState, acceptingState, acceptingStateLA, edges);

		for (Edge e : edges) {
			int src = e.getSrc();
			bfa[src].add(e);
		}

		removeRedundantEdges1(); // arbitrary
		removeEpsilonCycle(); // must for exec
		removeRedundantEdges1(); // arbitrary
		eliminateEpsilonTransition();

		fixStateID();

		for (int i = 0; i < V; i++) {
			bfa[i].clear();
		}

		for (Edge e : BFA_graph.getEdges()) {
			int src = e.getSrc();
			bfa[src].add(e);
		}

		System.out.println("final state = " + BFA_graph); // for debug

		buildBFA();

		System.out.println("V = " + V);

	}

	// stateID が飛び飛びになっているので０から順番に付け直す
	public void fixStateID() {
		Map<Integer, Integer> stateIDTable = new HashMap<Integer, Integer>();
		V = 0;
		Set<Edge> newEdges = new TreeSet<Edge>(new EdgeComparator());
		for (Edge e : BFA_graph.getEdges()) {
			int src = e.getSrc();
			if (!stateIDTable.containsKey(src)) {
				stateIDTable.put(src, V++);
			}
			src = stateIDTable.get(src);

			int dst = e.getDst();
			if (!stateIDTable.containsKey(dst)) {
				stateIDTable.put(dst, V++);
			}
			dst = stateIDTable.get(dst);
			newEdges.add(new Edge(src, dst, e.getLabel(), e.getPredicate()));
		}
		BFA_graph.setEdges(newEdges);
		Set<Integer> newS = new HashSet<Integer>();
		for (int i = 0; i < V; i++) {
			newS.add(i);
		}
		BFA_graph.setS(newS);

		Set<Integer> newInitialState = new HashSet<Integer>();
		for (Integer i : BFA_graph.getInitialState()) {
			if (!stateIDTable.containsKey(i)) {
				System.out.println("INVALID... CANNOT FIND INITIAL STATE");
			}
			newInitialState.add(stateIDTable.get(i));
		}
		BFA_graph.setInitialState(newInitialState);

		Set<Integer> newAcceptingState = new HashSet<Integer>();
		for (Integer i : BFA_graph.getAcceptingState()) {
			if (!stateIDTable.containsKey(i)) {
				continue;
			}
			newAcceptingState.add(stateIDTable.get(i));
		}
		BFA_graph.setAcceptingState(newAcceptingState);

		Set<Integer> newAcceptingStateLA = new HashSet<Integer>();
		for (Integer i : BFA_graph.getAcceptingStateLA()) {
			if (!stateIDTable.containsKey(i)) {
				continue;
			}
			newAcceptingStateLA.add(stateIDTable.get(i));
		}
		BFA_graph.setAcceptingStateLA(newAcceptingStateLA);

	}

	void buildBFA() {
		for (int i = 0; i < V; i++) {
			bfa[i].clear();
		}

		Set<Character> allLabels = new HashSet<Character>();
		for (Edge e : BFA_graph.getEdges()) {
			int src = e.getSrc();
			bfa[src].add(e);
			if (e.getLabel() != epsilon && e.getLabel() != '.') {
				allLabels.add(e.getLabel());
			}
		}

		final_bfa = new BFA();
		// Set<State> S;
		Set<Integer> oldS = BFA_graph.getS();
		Set<State> newS = new TreeSet<State>(new StateComparator());
		for (Integer i : oldS) {
			// newS.add(new State(null, i));
			newS.add(new State(i));
		}
		final_bfa.setS(newS);

		// State f;
		Set<Integer> oldInitialState = BFA_graph.getInitialState();
		assert (oldInitialState.size() == 1);
		for (Integer i : oldInitialState) {
			// final_bfa.setf(new State(null, i));
			final_bfa.setf(new State(i));
			break;
		}

		// Set<State> F;
		Set<Integer> oldF = BFA_graph.getAcceptingState();
		Set<State> newF = new TreeSet<State>(new StateComparator());
		for (Integer i : oldF) {
			// newF.add(new State(null, i));
			newF.add(new State(i));
		}
		final_bfa.setF(newF);

		// Set<State> L;
		Set<Integer> oldL = BFA_graph.getAcceptingStateLA();
		Set<State> newL = new TreeSet<State>(new StateComparator());
		for (Integer i : oldL) {
			// newL.add(new State(null, i));
			newL.add(new State(i));
		}
		final_bfa.setL(newL);

		// Map<Tau, State> tau;
		Map<Tau, State> newTau = new TreeMap<Tau, State>(new TauComparator());
		int nV = V;
		for (int stateID = 0; stateID < V; stateID++) {
			// for (Character label : allLabels) {
			for (char label = 0; label < 256; label++) {
				// TauConstructor tc = new TauConstructor(BFA_graph,
				// final_bfa,stateID, label);

				TauConstructor tc = new TauConstructor(BFA_graph, final_bfa, stateID, label, nV);

				// if (Character.isAlphabetic(label)) {
				// System.out.println("stateID = " + stateID + ", label = "
				// + label);
				// System.out.println("state = " + tc.constructTau(stateID,
				// false));
				// System.out.println("");
				// }
				// newTau.put(new Tau(new State(null, stateID), label),
				// tc.constructTau(stateID, false));
				newTau.put(new Tau(new State(stateID), label), tc.constructTau(stateID, false));
				nV = tc.getV(); //
			}
		}
		V = nV;
		final_bfa.setTau(newTau);

	}

	Map<EpsilonMemoState, Set<EpsilonMemoState>> epsilonMemo = null;

	private Set<EpsilonMemoState> moveEpsilonTransition(EpsilonMemoState state) {
		if (epsilonMemo.containsKey(state)) {
			return epsilonMemo.get(state);
		}
		Set<EpsilonMemoState> nextStateIDs = new HashSet<EpsilonMemoState>();
		int currentStateID = state.getStateID();
		boolean allEpsilon = true;
		for (int i = 0; i < bfa[currentStateID].size(); i++) {
			Edge e = bfa[currentStateID].get(i);
			if (e.getLabel() != epsilon) {
				allEpsilon = false;
				continue;
			}

			EpsilonMemoState nextState = new EpsilonMemoState(e.getDst(), state.getPredicate());
			if (e.getPredicate() != -1) {
				// nextState.changePredicate(e.getPredicate());
				nextStateIDs.add(new EpsilonMemoState(e.getDst(), e.getPredicate()));
				continue;
			}
			Set<EpsilonMemoState> partOfStateIDs = moveEpsilonTransition(nextState);
			for (EpsilonMemoState ems : partOfStateIDs) {
				nextStateIDs.add(ems);
			}
		}
		if (!allEpsilon || (bfa[currentStateID].size() == 0)) {
			nextStateIDs.add(state);
		}
		epsilonMemo.put(state, nextStateIDs);
		return nextStateIDs;
	}

	private void eliminateEpsilonTransition() {
		if (epsilonMemo == null) {
			epsilonMemo = new TreeMap<EpsilonMemoState, Set<EpsilonMemoState>>(new EpsilonMemoStateComparator());
		}
		ArrayList<Edge> epsilonEdges = new ArrayList<Edge>();
		Set<Character> allLabels = new HashSet<Character>();
		for (Edge e : BFA_graph.getEdges()) {
			if (e.getLabel() != epsilon) {
				allLabels.add(e.getLabel());
			}
			if (e.getLabel() == epsilon) {
				epsilonEdges.add(e);
			}
		}

		boolean update = true;
		while (update) {

			update = false;
			for (int i = 0; i < V; i++) {
				bfa[i].clear();
			}
			Set<Edge> edges = BFA_graph.getEdges();
			for (Edge e : edges) {
				int src = e.getSrc();
				bfa[src].add(e);
			}

			for (int stateID = 0; stateID < V; stateID++) {
				Set<EpsilonMemoState> nextStateIDs = moveEpsilonTransition(new EpsilonMemoState(stateID, -1));
				if (nextStateIDs.size() == 0) {
					continue;
				}
				for (Character label : allLabels) {
					// System.out.println("label = " + label);
					for (EpsilonMemoState ems : nextStateIDs) {

						if (ems.getPredicate() != -1) {
							Edge new_e = new Edge(stateID, ems.getStateID(), epsilon, ems.getPredicate());
							if (!edges.contains(new_e)) {
								update = true;
								edges.add(new_e);
							}
							continue;
						}

						Set<EpsilonMemoState> tmp = new TreeSet<EpsilonMemoState>(new EpsilonMemoStateComparator());
						int nextStateID = ems.getStateID();
						// System.out.println("nextStateID = " + nextStateID);
						for (int i = 0; i < bfa[nextStateID].size(); i++) {
							Edge e = bfa[nextStateID].get(i);
							if (e.getLabel() == label || e.getLabel() == '.') {
								Set<EpsilonMemoState> tmp2 = moveEpsilonTransition(new EpsilonMemoState(e.getDst(), ems.getPredicate()));
								for (EpsilonMemoState ems2 : tmp2) {
									Edge new_e = new Edge(stateID, ems2.getStateID(), label, ems2.getPredicate());
									if (!edges.contains(new_e)) {
										update = true;
										edges.add(new_e);
									}
								}
							}
						}
					}
				}
			}
			BFA_graph.setEdges(edges);
		}

		Set<Edge> newEdges = BFA_graph.getEdges();
		for (int i = 0; i < epsilonEdges.size(); i++) {
			newEdges.remove(epsilonEdges.get(i));
		}

		// remove useless vertices
		Set<Integer> initialState = BFA_graph.getInitialState();
		Set<Integer> acceptingState = BFA_graph.getAcceptingState();
		Set<Integer> acceptingStateLA = BFA_graph.getAcceptingStateLA();
		update = true;
		while (update) {
			update = false;
			epsilonEdges.clear();
			int in_degree[] = new int[V];
			int out_degree[] = new int[V];
			for (int i = 0; i < V; i++) {
				in_degree[i] = out_degree[i] = 0;
			}
			for (Edge e : newEdges) {
				++in_degree[e.getDst()];
				++out_degree[e.getSrc()];
			}

			Set<Integer> uselessVertices = new HashSet<Integer>();
			for (int i = 0; i < V; i++) {
				if (out_degree[i] == 0 && !initialState.contains(i) && !acceptingState.contains(i) && !acceptingStateLA.contains(i)) {
					uselessVertices.add(i);
				}
			}

			for (Edge e : newEdges) {
				if ((in_degree[e.getSrc()] == 0 && !initialState.contains(e.getSrc())) || uselessVertices.contains(e.getDst())) {
					epsilonEdges.add(e);
					update = true;
				}

			}

			for (Edge e : epsilonEdges) {
				newEdges.remove(e);
			}
		}

		for (int i = 0; i < V; i++) {
			bfa[i].clear();
		}

		for (Edge e : newEdges) {
			int src = e.getSrc();
			bfa[src].add(e);
		}
		// remove useless edges
		for (int i = 0; i < V; i++) {
			Set<Integer> hasAny = new HashSet<Integer>();
			for (int j = 0; j < bfa[i].size(); j++) {
				if (bfa[i].get(j).getLabel() == '.') {
					hasAny.add(bfa[i].get(j).getDst());
				}
			}
			for (int j = 0; j < bfa[i].size(); j++) {
				Edge e = bfa[i].get(j);
				if (e.getLabel() == '.')
					continue;
				if (e.getPredicate() != -1)
					continue;
				if (hasAny.contains(e.getDst())) {
					newEdges.remove(e);
				}
			}
		}

		BFA_graph.setEdges(newEdges);
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
				for (int j = 0; j < bfa[i].size(); j++) {
					int dst = bfa[i].get(j).getDst();
					++in_degree[dst];
				}
			}

			for (int vertex1 = 0; vertex1 < V; vertex1++) {
				for (int j = 0; j < bfa[vertex1].size(); j++) {
					Edge e1 = bfa[vertex1].get(j);
					int vertex2 = e1.getDst();
					if (in_degree[vertex2] != 1)
						continue;
					if (initialState.contains(vertex2) || acceptingState.contains(vertex2) || acceptingStateLA.contains(vertex2))
						continue;
					if (bfa[vertex2].size() != 1)
						continue;
					for (int k = 0; k < bfa[vertex2].size(); k++) {
						Edge e2 = bfa[vertex2].get(k);
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
					bfa[i].clear();
				}
				for (Edge e : edges) {
					int src = e.getSrc();
					bfa[src].add(e);
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
			bfa[i].clear();
		}
		for (Edge e : edges) {
			int src = e.getSrc();
			bfa[src].add(e);
		}
	}

	private void removeEpsilonCycle() {
		StrongestConnectedComponent scc = new StrongestConnectedComponent(V, BFA_graph);
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

	/*
	 * private Map<Integer, String> memo; private String
	 * computeNextBooleanExpression(Integer booleanVariable) { if
	 * (memo.containsKey(booleanVariable)) { return memo.get(booleanVariable); }
	 * boolean fin = true; for (int i = 0; i < bfa[booleanVariable].size(); i++)
	 * { Edge e = bfa[booleanVariable].get(i); if (e.getLabel() == epsilon) {
	 * fin = false; break; } } if (fin) { memo.put(booleanVariable,
	 * booleanVariable.toString()); return booleanVariable.toString(); }
	 * ArrayList<Integer> normal = new ArrayList<Integer>(); ArrayList<Integer>
	 * andPredicate = new ArrayList<Integer>(); ArrayList<Integer> notPredicate
	 * = new ArrayList<Integer>(); boolean myself = false; for (int i = 0; i <
	 * bfa[booleanVariable].size(); i++) { Edge e = bfa[booleanVariable].get(i);
	 * if (e.getPredicate() == 0) { // and andPredicate.add(e.getDst()); } else
	 * if (e.getPredicate() == 1) { // not notPredicate.add(e.getDst()); } else
	 * { if (e.getLabel() == epsilon) { normal.add(e.getDst()); } else { myself
	 * = true; } } } // String booleanExpression = "("; StringBuilder
	 * booleanExpression = new StringBuilder("("); if (normal.size() > 0) { //
	 * booleanExpression += "("; booleanExpression.append("("); if (myself) { //
	 * booleanExpression += ( booleanVariable.toString() + "|" );
	 * booleanExpression.append(booleanVariable.toString()).append("|"); } for
	 * (int i = 0; i < normal.size(); i++) { // String tmp =
	 * computeNextBooleanExpression(normal.get(i)) + // "|"; //
	 * booleanExpression += tmp;
	 * booleanExpression.append(computeNextBooleanExpression
	 * (normal.get(i))).append("|"); } if
	 * (booleanExpression.charAt(booleanExpression.length() - 1) == '|' ||
	 * booleanExpression.charAt(booleanExpression.length() - 1) == '&') { //
	 * booleanExpression = //
	 * booleanExpression.substring(0,booleanExpression.length()-1);
	 * booleanExpression = new StringBuilder(booleanExpression.substring(0,
	 * booleanExpression.length() - 1)); } // booleanExpression += ")";
	 * booleanExpression.append(")"); } else { if (myself) {
	 * booleanExpression.append(booleanVariable.toString()).append("&"); } }
	 * 
	 * if (andPredicate.size() > 0) { if
	 * (booleanExpression.charAt(booleanExpression.length() - 1) == ')') { //
	 * booleanExpression += "&"; booleanExpression.append("&"); } for (int i =
	 * 0; i < andPredicate.size(); i++) { // String tmp = //
	 * computeNextBooleanExpression(andPredicate.get(i)) + "&"; //
	 * booleanExpression += tmp;
	 * booleanExpression.append(computeNextBooleanExpression
	 * (andPredicate.get(i))).append("&"); } } if (notPredicate.size() > 0) { if
	 * (booleanExpression.charAt(booleanExpression.length() - 1) == ')') { //
	 * booleanExpression += "&"; booleanExpression.append("&"); } for (int i =
	 * 0; i < notPredicate.size(); i++) { // String tmp = "!" + //
	 * computeNextBooleanExpression(notPredicate.get(i)) + "&"; //
	 * booleanExpression += tmp;
	 * booleanExpression.append("!").append(computeNextBooleanExpression
	 * (notPredicate.get(i))).append("&"); } }
	 * 
	 * if (booleanExpression.charAt(booleanExpression.length() - 1) == '|' ||
	 * booleanExpression.charAt(booleanExpression.length() - 1) == '&') { //
	 * booleanExpression = //
	 * booleanExpression.substring(0,booleanExpression.length()-1);
	 * booleanExpression = new StringBuilder(booleanExpression.substring(0,
	 * booleanExpression.length() - 1)); } // booleanExpression += ")";
	 * booleanExpression.append(")"); memo.put(booleanVariable,
	 * booleanExpression.toString()); return booleanExpression.toString(); }
	 * 
	 * private String removeRedundantParentheses(String s) { boolean[]
	 * redundancy = new boolean[s.length()]; for (int i = 0; i < s.length();
	 * i++) { redundancy[i] = false; } Stack<Integer> stk = new
	 * Stack<Integer>(); for (int i = 0; i < s.length(); i++) { if (s.charAt(i)
	 * == '(') { stk.push(i); } else if (s.charAt(i) == ')') { int L =
	 * stk.pop(); int R = i; --L; ++R; while (0 <= L && R < s.length() &&
	 * s.charAt(L) == '(' && s.charAt(R) == ')') { redundancy[L] = redundancy[R]
	 * = true; --L; ++R; } } } String simple_s = ""; for (int i = 0; i <
	 * s.length(); i++) { if (redundancy[i]) continue; simple_s += s.charAt(i);
	 * } return simple_s; }
	 * 
	 * private boolean evalBooleanExpression(String booleanExpression) {
	 * Set<Integer> AcceptingState = BFA_graph.getAcceptingState(); Set<Integer>
	 * AcceptingStateLA = BFA_graph.getAcceptingStateLA(); String
	 * tmpBooleanExpression = ""; for (int j = 0; j <
	 * booleanExpression.length(); j++) { if
	 * (Character.isDigit(booleanExpression.charAt(j))) { int v = 0; while (j <
	 * booleanExpression.length() &&
	 * Character.isDigit(booleanExpression.charAt(j))) { v *= 10; v +=
	 * (booleanExpression.charAt(j) - '0'); ++j; } --j; tmpBooleanExpression +=
	 * ((AcceptingState.contains(v) || AcceptingStateLA.contains(v)) ? "T" :
	 * "F"); } else { tmpBooleanExpression += booleanExpression.charAt(j); } }
	 * BooleanExpressionEvaluator bee = new
	 * BooleanExpressionEvaluator(tmpBooleanExpression); return bee.eval(); }
	 * 
	 * private String eliminateEpsilonFromBooleanExpression(String
	 * booleanExpression) { StringBuilder tmpBooleanExpression = new
	 * StringBuilder(""); for (int j = 0; j < booleanExpression.length(); j++) {
	 * if (Character.isDigit(booleanExpression.charAt(j))) { int v = 0; while (j
	 * < booleanExpression.length() &&
	 * Character.isDigit(booleanExpression.charAt(j))) { v *= 10; v +=
	 * (booleanExpression.charAt(j) - '0'); ++j; } --j; // tmpBooleanExpression
	 * += computeNextBooleanExpression(v);
	 * tmpBooleanExpression.append(computeNextBooleanExpression(v)); } else { //
	 * tmpBooleanExpression += booleanExpression.charAt(j);
	 * tmpBooleanExpression.append(booleanExpression.charAt(j)); } } return
	 * removeRedundantParentheses(tmpBooleanExpression.toString()); }
	 * 
	 * // !!!NOTICE!!! REMOVE ALL F! LET'S IMPLEMENT private boolean FAILED;
	 * 
	 * private String moveToTheNextState(char c, String booleanExpression) {
	 * StringBuilder tmpBooleanExpression = new StringBuilder(""); FAILED =
	 * true; for (int j = 0; j < booleanExpression.length(); j++) { if
	 * (Character.isDigit(booleanExpression.charAt(j))) { int v = 0; while (j <
	 * booleanExpression.length() &&
	 * Character.isDigit(booleanExpression.charAt(j))) { v *= 10; v +=
	 * (booleanExpression.charAt(j) - '0'); ++j; } --j; ArrayList<Integer> next
	 * = new ArrayList<Integer>(); for (int k = 0; k < bfa[v].size(); k++) {
	 * Edge e = bfa[v].get(k); if (e.getLabel() != c && e.getLabel() != '.')
	 * continue; next.add(e.getDst()); } if (next.size() > 0) { //
	 * tmpBooleanExpression += "("; tmpBooleanExpression.append("("); for (int k
	 * = 0; k < next.size(); k++) { if (k > 0) { // tmpBooleanExpression += "|";
	 * tmpBooleanExpression.append("|"); } // tmpBooleanExpression +=
	 * next.get(k).toString(); if (verifyPredicate(next.get(k).toString())) {
	 * tmpBooleanExpression.append("T"); } else {
	 * tmpBooleanExpression.append(next.get(k).toString()); } } //
	 * tmpBooleanExpression += ")"; tmpBooleanExpression.append(")"); FAILED =
	 * false; } else { // tmpBooleanExpression += "F";
	 * tmpBooleanExpression.append("F"); } } else { if (j + 1 <
	 * booleanExpression.length() && booleanExpression.charAt(j) == 'F' &&
	 * booleanExpression.charAt(j + 1) == '|') { ++j; continue; }
	 * 
	 * // tmpBooleanExpression += booleanExpression.charAt(j);
	 * tmpBooleanExpression.append(booleanExpression.charAt(j)); } } return
	 * removeRedundantParentheses(tmpBooleanExpression.toString()); }
	 * 
	 * Set<Integer> inVerifyPredicateAcceptingStateLA;
	 * 
	 * private boolean verifyPredicate(String booleanVariable) { String
	 * booleanExpression =
	 * eliminateEpsilonFromBooleanExpression(booleanVariable); for (int i = 0; i
	 * < booleanExpression.length(); i++) { if
	 * (Character.isDigit(booleanExpression.charAt(i))) { int v = 0; //while (i+
	 * 1< booleanExpression.length() &&
	 * Character.isDigit(booleanExpression.charAt(i))) { while (i <
	 * booleanExpression.length() &&
	 * Character.isDigit(booleanExpression.charAt(i))) { v *= 10; v +=
	 * (booleanExpression.charAt(i) - '0'); ++i; } --i; if
	 * (inVerifyPredicateAcceptingStateLA.contains(v)) { return true; } } }
	 * return false; }
	 */
	// public Map<ExecMemoState, Boolean> execMemo = null;
	final int H = 10000;
	final int W = 10000;
	public static byte[][] execMemo = null; // execMemo[stateID][top]
	public static Map<Tau, State> staticTau = null;
	public static Set<State> staticF = null;

	public static SourceContext staticSc = null;

	private void initExec() {
		if (execMemo == null) {
			execMemo = new byte[H][W];
			// staticTau = new TreeMap<Tau, State>(new TauComparator());
			// staticF = new TreeSet<State>(new StateComparator());
			staticTau = final_bfa.getTau();
			staticF = final_bfa.getF();
			for (State state : final_bfa.getL()) {
				staticF.add(state);
			}
		}
		for (int i = 0; i < H; i++) {
			for (int j = 0; j < W; j++) {
				execMemo[i][j] = -1;
			}
		}
	}

	// execute on DFA
	// On-the-Fly
	public boolean exec(String text) {
		/*
		 * if (execMemo == null) { execMemo = new TreeMap<ExecMemoState,
		 * Boolean>(new ExecMemoStateComparator()); } execMemo.clear();
		 */
		initExec();

		for (Map.Entry<Tau, State> e : staticTau.entrySet()) {
			if (e.getKey().getSigma() != 'a' && e.getKey().getSigma() != 'b') {
				continue;
			}
		}

		staticSc = new StringContext(text);
		// Context context = new Context(text);
		// System.out.println("f = " + final_bfa.getf());
		long st = System.currentTimeMillis();
		// boolean result = final_bfa.getf().accept(context);
		boolean result = final_bfa.getf().accept(0);
		long ed = System.currentTimeMillis();
		System.out.println((ed - st) + "ms");
		return result;
		// return final_bfa.getf().accept(context);
	}

	/*
	 * public boolean exec(String text) { inVerifyPredicateAcceptingStateLA =
	 * BFA_graph.getAcceptingStateLA(); FAILED = false; memo = new
	 * HashMap<Integer, String>(); String booleanExpression = ""; Set<Integer>
	 * initialState = BFA_graph.getInitialState(); for (Integer i :
	 * initialState) { // assert initialState.size() == 1 booleanExpression =
	 * i.toString(); }
	 * 
	 * for (int i = 0; i < text.length(); i++) { char c = text.charAt(i);
	 * booleanExpression =
	 * eliminateEpsilonFromBooleanExpression(booleanExpression); if
	 * (showBooleanExpression) { System.out.println("booleanExpression = " +
	 * booleanExpression); } booleanExpression = moveToTheNextState(c,
	 * booleanExpression); if (FAILED) { break; } } booleanExpression =
	 * eliminateEpsilonFromBooleanExpression(booleanExpression); if
	 * (showBooleanExpression) { System.out.println("final booleanExpression = "
	 * + booleanExpression); } return evalBooleanExpression(booleanExpression);
	 * }
	 */

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

	public EpsilonBFA visitProduction(Production rule) {
		// System.out.println("here is Production : " + rule);
		Expression e = rule.getExpression();
		if (e instanceof Pchoice) {
			return visitPchoice(e);
		} else {
			return visitExpression(e);
		}
	}

	public EpsilonBFA visitExpression(Expression e) {
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

	private EpsilonBFA visitXblock(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Xblock : " + e);
		return null;
	}

	private EpsilonBFA visitTreplace(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Treplace : " + e);
		return null;
	}

	private EpsilonBFA visitTcapture(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Tcapture : " + e);
		return visitExpression(e);
	}

	private EpsilonBFA visitTnew(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Tnew : " + e);
		return null;
	}

	private EpsilonBFA visitTlink(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Tlink : " + e);
		return null;
	}

	private EpsilonBFA visitCmulti(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Cmulti : " + e);
		return null;
	}

	private EpsilonBFA visitNonTerminal(Expression e) {
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
			return new EpsilonBFA(S, initialState, acceptingState, acceptingStateLA, edges);
		} else {

			int s = getTnewVertex();
			int t = getTnewVertex();
			initialStateOfNonTerminal.put(NonTerminalName, s);
			acceptingStateOfNonTerminal.put(NonTerminalName, t);

			EpsilonBFA content = visitProduction(((NonTerminal) e).getProduction());

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

			return new EpsilonBFA(S, initialState, acceptingState, acceptingStateLA, edges);
		}
		// return null;
	}

	private EpsilonBFA visitPchoice(Expression e) {
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
			EpsilonBFA g = visitExpression(e.get(i));
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

		return new EpsilonBFA(newS, newInitialState, newAcceptingState, newAcceptingStateLA, edges);
	}

	private EpsilonBFA visitPsequence(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Psequence : " + e);
		return getPsequence(e);
	}

	private EpsilonBFA visitPnot(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pnot : " + e);
		EpsilonBFA g1 = getPsequence(e);
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
		return new EpsilonBFA(newS, newInitialState, acceptingState, acceptingStateLA, edges);
		// return new
		// Graph(newS,newInitialState,acceptingState,g1.getAcceptingState(),edges);
	}

	private EpsilonBFA fixPredicate(EpsilonBFA g) {
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
		return new EpsilonBFA(g.getS(), initialState, g.getAcceptingState(), g.getAcceptingStateLA(), edges);
	}

	private EpsilonBFA getPsequence(Expression e) {
		Deque<EpsilonBFA> predicate = new ArrayDeque<EpsilonBFA>();
		Deque<Integer> p_types = new ArrayDeque<Integer>();
		EpsilonBFA graph = null;
		for (int i = 0; i < e.size(); i++) {
			Expression tmp = e.get(i);
			if (tmp instanceof Pand || tmp instanceof Pnot) {
				predicate.addLast(visitExpression(tmp));
				p_types.addLast((tmp instanceof Pand) ? 0 : 1);
			} else {
				EpsilonBFA graph2 = visitExpression(tmp);
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
						EpsilonBFA p_graph = fixPredicate(predicate.pollFirst());
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
					graph2 = new EpsilonBFA(newS, newInitialState, newAcceptingState, newAcceptingStateLA, edges);
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
				EpsilonBFA p_graph = fixPredicate(predicate.pollFirst());
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
			EpsilonBFA graph2 = new EpsilonBFA(newS, newInitialState, newAcceptingState, newAcceptingStateLA, edges);
			graph = concatenate(graph, graph2);
		}

		return graph;
	}

	private EpsilonBFA concatenate(EpsilonBFA g1, EpsilonBFA g2) {
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

		return new EpsilonBFA(newS, g1.getInitialState(), g2_acceptingState, newAcceptingStateLA, edges);
	}

	private EpsilonBFA visitPand(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pand : " + e);
		EpsilonBFA g1 = getPsequence(e);
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
		return new EpsilonBFA(newS, newInitialState, acceptingState, acceptingStateLA, edges);
		// return new
		// Graph(newS,newInitialState,acceptingState,g1.getAcceptingState(),edges);
	}

	private EpsilonBFA visitPone(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pone : " + e);
		EpsilonBFA graph = visitExpression(e.get(0));
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
		return new EpsilonBFA(newS, newInitialState, newAcceptingState, graph.getAcceptingStateLA(), edges);
	}

	private EpsilonBFA visitPzero(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pzero : " + e);
		EpsilonBFA graph = visitExpression(e.get(0));
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

		return new EpsilonBFA(newS, newInitialState, newAcceptingState, graph.getAcceptingStateLA(), edges);
	}

	private EpsilonBFA visitPoption(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Poption : " + e);
		EpsilonBFA graph = visitExpression(e.get(0));
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

		return new EpsilonBFA(newS, newInitialState, newAcceptingState, graph.getAcceptingStateLA(), edges);
	}

	private EpsilonBFA visitCset(Expression e) {
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
		return new EpsilonBFA(S, initialState, acceptingState, acceptingStateLA, edges);
	}

	private EpsilonBFA visitCbyte(Expression e) {
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
		return new EpsilonBFA(S, initialState, acceptingState, acceptingStateLA, edges);
	}

	private EpsilonBFA visitCany(Expression e) {
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
		return new EpsilonBFA(S, initialState, acceptingState, acceptingStateLA, edges);
	}

	private EpsilonBFA visitPfail(Expression e) {
		// TODO Auto-generated method stub
		// System.out.println("here is Pfail : " + e);
		return null;
	}

	private EpsilonBFA visitPempty(Expression e) {
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
		return new EpsilonBFA(S, initialState, acceptingState, acceptingStateLA, edges);
	}

}
