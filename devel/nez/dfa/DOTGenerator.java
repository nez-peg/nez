package nez.dfa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DOTGenerator {
	public DOTGenerator() {

	}

	private static final String dotFileName = "__graph";
	private static int fileID = 0;
	private static final String fColor = "#4169E1"; // royalblue
	private static final String FColor = "#7fffd4"; // aquamarine
	private static final String LColor = "#ff6347"; // tomato
	private static final String FLColor = "#9400d3"; // darkviolet,
														// ひとつのノードが通常の受理状態と先読みの受理状態の両方を持つ場合

	private static void execCommandLine(String command) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);
			pr.waitFor();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public static int getFileID() {
		return fileID;
	}

	public static void writeAFA(AFA afa) {
		if (afa == null) {
			System.out.println("WARNING : afa is null");
			return;
		}
		try {
			File file = new File(dotFileName + fileID + ".dot");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw.println("\ndigraph g {");
			pw.println("\"" + afa.getf().getID() + "\"[style=filled,fillcolor=\"" + fColor + "\"];");

			HashSet<State> FandL = new HashSet<State>();
			for (State state : afa.getS()) {
				if (afa.getF().contains(state) && afa.getL().contains(state)) {
					FandL.add(new State(state.getID()));
					pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FLColor + "\"];");
				}
			}

			for (State state : afa.getF()) {
				if (FandL.contains(state)) {
					continue;
				}
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
			}
			for (State state : afa.getL()) {
				if (FandL.contains(state)) {
					continue;
				}
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + LColor + "\"];");
			}
			for (Transition transition : afa.getTau()) {
				int label = transition.getLabel();
				int predicate = transition.getPredicate();
				pw.print("	\"" + transition.getSrc() + "\"->\"" + transition.getDst() + "\"[label=\"");
				if (predicate == 0) {
					pw.print("&predicate");
				} else if (predicate == 1) {
					pw.print("!predicate");
				} else if (label != AFA.epsilon) {
					if (Character.isLetterOrDigit((char) label)) {
						pw.print((char) label);
					} else if (label == AFA.anyCharacter) {
						pw.print("any");
					} else {
						pw.print(label);
					}
				} else {
					pw.print("ε");
				}
				pw.println("\"];");
			}
			pw.println("}");
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		execCommandLine("dot -Kdot -Tpng " + dotFileName + fileID + ".dot -o " + dotFileName + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

		fileID++;
	}

	public static boolean isVisible(int v) {
		return (32 <= v && v <= 126);
	}

	public static void writeDFA(DFA dfa) {
		if (dfa == null) {
			System.out.println("WARNING : dfa is null");
			return;
		}
		try {

			ArrayList<ArrayList<ArrayList<Integer>>> tau = new ArrayList<ArrayList<ArrayList<Integer>>>();
			for (int i = 0; i < dfa.getS().size(); i++) {
				tau.add(new ArrayList<ArrayList<Integer>>());
			}
			for (int i = 0; i < dfa.getS().size(); i++) {
				for (int j = 0; j < dfa.getS().size(); j++) {
					tau.get(i).add(new ArrayList<Integer>());
				}
			}
			for (Transition transition : dfa.getTau()) {
				int src = transition.getSrc();
				int dst = transition.getDst();
				int label = transition.getLabel();
				tau.get(src).get(dst).add(new Integer(label));
			}

			File file = new File(dotFileName + fileID + ".dot");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw.println("\ndigraph g {");
			if (dfa.getF().contains(new State(dfa.getf().getID()))) {
				// 初期状態と受理状態が一緒の場合
				pw.println("\"" + dfa.getf().getID() + "\"[style=filled,fillcolor=\"" + FLColor + "\"];");
			} else {
				pw.println("\"" + dfa.getf().getID() + "\"[style=filled,fillcolor=\"" + fColor + "\"];");
			}

			for (State state : dfa.getF()) {
				if (state.getID() == dfa.getf().getID()) {
					continue;
				}
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
			}

			for (int src = 0; src < dfa.getS().size(); src++) {
				for (int dst = 0; dst < dfa.getS().size(); dst++) {

					int size = tau.get(src).get(dst).size();
					if (size == 0) {
						continue;
					}
					StringBuilder labels = new StringBuilder();
					Set<Integer> exists = new HashSet<Integer>();
					for (int i = 0; i < tau.get(src).get(dst).size(); i++) {
						if (i > 0) {

						}
						int v = tau.get(src).get(dst).get(i);
						exists.add(v);
						if (isVisible(v)) {
							if ((char) v == '"') {
								labels.append("\\" + new StringBuilder(String.valueOf((char) v)));
							} else {
								labels.append(new StringBuilder(String.valueOf((char) v)));
							}
						} else {
							labels.append("(" + new StringBuilder(String.valueOf(v)) + ")");
						}
					}

					if (size > 128) {
						StringBuilder tmp = new StringBuilder("");
						for (int j = 0; j < 256; j++) {
							if (exists.contains(new Integer(j))) {
								continue;
							}
							if (isVisible(j)) {
								if ((char) j == '"') {
									tmp.append("\\" + new StringBuilder(String.valueOf((char) j)));
								} else {
									tmp.append(new StringBuilder(String.valueOf((char) j)));
								}
							} else {
								tmp.append("(" + new StringBuilder(String.valueOf(j)) + ")");
							}
						}
						labels = tmp;
					}

					pw.print("	\"" + src + "\"->\"" + dst + "\"[label=\"");
					pw.print("[" + ((size < 128) ? labels : ("^" + labels)) + "]");
					pw.println("\"];");

				}
			}
			// for (Transition transition : dfa.getTau()) {
			// int label = transition.getLabel();
			// int predicate = transition.getPredicate();
			// pw.print(" \"" + transition.getSrc() + "\"->\"" +
			// transition.getDst() + "\"[label=\"");
			// if (predicate == 0) {
			// pw.print("&predicate");
			// } else if (predicate == 1) {
			// pw.print("!predicate");
			// } else if (label != AFA.epsilon) {
			// if (Character.isLetterOrDigit((char) label) || (char) label ==
			// '.') {
			// pw.print((char) label);
			// } else {
			// pw.print(label);
			// }
			// } else {
			// pw.print("ε");
			// }
			// pw.println("\"];");
			// }
			pw.println("}");
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		execCommandLine("dot -Kdot -Tpng " + dotFileName + fileID + ".dot -o " + dotFileName + fileID + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

		fileID++;
	}

	public static void writeAFA(AFA afa, String file_name) {
		if (afa == null) {
			System.out.println("WARNING : afa is null");
			return;
		}
		try {
			File file = new File("/tmp/" + file_name + ".dot");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw.println("\ndigraph g {");
			pw.println("\"" + afa.getf().getID() + "\"[style=filled,fillcolor=\"" + fColor + "\"];");

			HashSet<State> FandL = new HashSet<State>();
			for (State state : afa.getS()) {
				if (afa.getF().contains(state) && afa.getL().contains(state)) {
					FandL.add(new State(state.getID()));
					pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FLColor + "\"];");
				}
			}

			for (State state : afa.getF()) {
				if (FandL.contains(state)) {
					continue;
				}
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
			}
			for (State state : afa.getL()) {
				if (FandL.contains(state)) {
					continue;
				}
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + LColor + "\"];");
			}
			for (Transition transition : afa.getTau()) {
				int label = transition.getLabel();
				int predicate = transition.getPredicate();
				pw.print("	\"" + transition.getSrc() + "\"->\"" + transition.getDst() + "\"[label=\"");
				if (predicate == 0) {
					pw.print("&predicate");
				} else if (predicate == 1) {
					pw.print("!predicate");
				} else if (label != AFA.epsilon) {
					if (Character.isLetterOrDigit((char) label)) {
						pw.print((char) label);
					} else if (label == AFA.anyCharacter) {
						pw.print("any");
					} else {
						pw.print(label);
					}
				} else {
					pw.print("ε");
				}
				pw.println("\"];");
			}
			pw.println("}");
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		execCommandLine("dot -Kdot -Tpng /tmp/" + file_name + fileID + ".dot -o /tmp/" + file_name + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

		fileID++;
	}

	// for BFAConverterForREwLA
	// Note that the directory that the result is written is /tmp
	public static void writeDFA(DFA dfa, String file_name) {
		if (dfa == null) {
			System.out.println("WARNING : dfa is null");
			return;
		}
		try {

			ArrayList<ArrayList<ArrayList<Integer>>> tau = new ArrayList<ArrayList<ArrayList<Integer>>>();
			for (int i = 0; i < dfa.getS().size(); i++) {
				tau.add(new ArrayList<ArrayList<Integer>>());
			}
			for (int i = 0; i < dfa.getS().size(); i++) {
				for (int j = 0; j < dfa.getS().size(); j++) {
					tau.get(i).add(new ArrayList<Integer>());
				}
			}
			for (Transition transition : dfa.getTau()) {
				int src = transition.getSrc();
				int dst = transition.getDst();
				int label = transition.getLabel();
				tau.get(src).get(dst).add(new Integer(label));
			}

			File file = new File("/tmp/" + file_name + ".dot");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			pw.println("\ndigraph g {");
			// pw.println("graph[bgcolor=\"#00000000\"];");
			if (dfa.getF().contains(new State(dfa.getf().getID()))) {
				// 初期状態と受理状態が一緒の場合
				pw.println("\"" + dfa.getf().getID() + "\"[style=filled,fillcolor=\"" + FLColor + "\", shape=doublecircle];");
			} else {
				pw.println("\"" + dfa.getf().getID() + "\"[style=filled,fillcolor=\"" + fColor + "\"];");
			}

			for (State state : dfa.getF()) {
				if (state.getID() == dfa.getf().getID()) {
					continue;
				}
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\", shape=doublecircle];");
			}

			for (int src = 0; src < dfa.getS().size(); src++) {
				for (int dst = 0; dst < dfa.getS().size(); dst++) {

					int size = tau.get(src).get(dst).size();
					if (size == 0) {
						continue;
					}
					StringBuilder labels = new StringBuilder();
					Set<Integer> exists = new HashSet<Integer>();
					for (int i = 0; i < tau.get(src).get(dst).size(); i++) {
						if (i > 0) {

						}
						int v = tau.get(src).get(dst).get(i);
						boolean hasTheOthers = ((v >= 0) ? false : true);
						if (hasTheOthers) {
							v *= -1;
						}
						exists.add(v);
						if (isVisible(v)) {
							if ((char) v == '"') {
								labels.append("\\" + new StringBuilder(String.valueOf((char) v)));
							} else {
								labels.append(new StringBuilder(String.valueOf((char) v)));
							}
						} else {
							labels.append("(" + new StringBuilder(String.valueOf(v)) + ")");
						}

					}

					if (size > 128) {
						StringBuilder tmp = new StringBuilder("");
						for (int j = 0; j < 256; j++) {
							if (exists.contains(new Integer(j))) {
								continue;
							}
							if (isVisible(j)) {
								if ((char) j == '"') {
									tmp.append("\\" + new StringBuilder(String.valueOf((char) j)));
								} else {
									tmp.append(new StringBuilder(String.valueOf((char) j)));
								}
							} else {
								tmp.append("(" + new StringBuilder(String.valueOf(j)) + ")");
							}
						}
						labels = tmp;
					}

					pw.print("	\"" + src + "\"->\"" + dst + "\"[label=\"");
					pw.print("[" + ((size < 128) ? labels : ("^" + labels)) + "]");
					pw.println("\"];");

				}
			}

			pw.println("}");
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}

		execCommandLine("dot -Kdot -Tpng /tmp/" + file_name + ".dot -o /tmp/" + file_name + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

		fileID++;
	}

	public static void writeNFA(NFA nfa) {
		if (nfa == null) {
			System.out.println("WARNING : dfa is null");
			return;
		}
		try {

			ArrayList<ArrayList<ArrayList<Integer>>> tau = new ArrayList<ArrayList<ArrayList<Integer>>>();
			for (int i = 0; i < nfa.getAllStates().size(); i++) {
				tau.add(new ArrayList<ArrayList<Integer>>());
			}
			for (int i = 0; i < nfa.getAllStates().size(); i++) {
				for (int j = 0; j < nfa.getAllStates().size(); j++) {
					tau.get(i).add(new ArrayList<Integer>());
				}
			}
			for (Transition transition : nfa.getStateTransitionFunction()) {
				int src = transition.getSrc();
				int dst = transition.getDst();
				int label = transition.getLabel();
				tau.get(src).get(dst).add(new Integer(label));
			}

			File file = new File(dotFileName + fileID + ".dot");
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));

			int f = -1;
			for (State state : nfa.getInitialStates()) {
				f = state.getID();
			}
			pw.println("\ndigraph g {");
			if (nfa.getAcceptingStates().contains(new State(f))) {
				// 初期状態と受理状態が一緒の場合
				pw.println("\"" + f + "\"[style=filled,fillcolor=\"" + FLColor + "\"];");
			} else {
				pw.println("\"" + f + "\"[style=filled,fillcolor=\"" + fColor + "\"];");
			}

			for (State state : nfa.getAcceptingStates()) {
				if (state.getID() == f) {
					continue;
				}
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
			}

			for (int src = 0; src < nfa.getAllStates().size(); src++) {
				for (int dst = 0; dst < nfa.getAllStates().size(); dst++) {

					int size = tau.get(src).get(dst).size();
					if (size == 0) {
						continue;
					}
					StringBuilder labels = new StringBuilder();
					Set<Integer> exists = new HashSet<Integer>();
					for (int i = 0; i < tau.get(src).get(dst).size(); i++) {
						if (i > 0) {

						}
						int v = tau.get(src).get(dst).get(i);
						exists.add(v);
						if (isVisible(v)) {
							if ((char) v == '"') {
								labels.append("\\" + new StringBuilder(String.valueOf((char) v)));
							} else {
								labels.append(new StringBuilder(String.valueOf((char) v)));
							}
						} else {
							labels.append("(" + new StringBuilder(String.valueOf(v)) + ")");
						}
					}

					if (size > 128) {
						StringBuilder tmp = new StringBuilder("");
						for (int j = 0; j < 256; j++) {
							if (exists.contains(new Integer(j))) {
								continue;
							}
							if (isVisible(j)) {
								if ((char) j == '"') {
									tmp.append("\\" + new StringBuilder(String.valueOf((char) j)));
								} else {
									tmp.append(new StringBuilder(String.valueOf((char) j)));
								}
							} else {
								tmp.append("(" + new StringBuilder(String.valueOf(j)) + ")");
							}
						}
						labels = tmp;
					}

					pw.print("	\"" + src + "\"->\"" + dst + "\"[label=\"");
					pw.print("[" + ((size < 128) ? labels : ("^" + labels)) + "]");
					pw.println("\"];");

				}
			}
			// for (Transition transition : dfa.getTau()) {
			// int label = transition.getLabel();
			// int predicate = transition.getPredicate();
			// pw.print(" \"" + transition.getSrc() + "\"->\"" +
			// transition.getDst() + "\"[label=\"");
			// if (predicate == 0) {
			// pw.print("&predicate");
			// } else if (predicate == 1) {
			// pw.print("!predicate");
			// } else if (label != AFA.epsilon) {
			// if (Character.isLetterOrDigit((char) label) || (char) label ==
			// '.') {
			// pw.print((char) label);
			// } else {
			// pw.print(label);
			// }
			// } else {
			// pw.print("ε");
			// }
			// pw.println("\"];");
			// }
			pw.println("}");
			pw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
		execCommandLine("dot -Kdot -Tpng " + dotFileName + fileID + ".dot -o " + dotFileName + fileID + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

		fileID++;
	}

}
