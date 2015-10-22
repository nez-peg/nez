package nez.x.dfa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

public class DOTGenerator {
	public DOTGenerator() {

	}

	private static final String fColor = "#4169E1"; // royalblue
	private static final String FColor = "#7fffd4"; // aquamarine
	private static final String LColor = "#ff6347"; // tomato
	private static final String FLColor = "#9400d3"; // darkviolet,
														// 先読み内で再帰すると１つの状態が通常の受理状態と先読みの受理状態の両方を持つことがある

	public static void generate(AFA afa) {
		if (afa == null) {
			System.out.println("WARNING : afa is null");
			return;
		}

		System.out.println("\ndigraph g {");
		System.out.println("\"" + afa.getf().getID() + "\"[style=filled,fillcolor=\"" + fColor + "\"];");

		HashSet<State> FandL = new HashSet<State>();
		for (State state : afa.getS()) {
			if (afa.getF().contains(state) && afa.getL().contains(state)) {
				FandL.add(new State(state.getID()));
				System.out.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FLColor + "\"];");
			}
		}

		for (State state : afa.getF()) {
			if (FandL.contains(state)) {
				continue;
			}
			System.out.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
		}
		for (State state : afa.getL()) {
			if (FandL.contains(state)) {
				continue;
			}
			System.out.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + LColor + "\"];");
		}
		for (Transition transition : afa.getTransitions()) {
			int label = transition.getLabel();
			int predicate = transition.getPredicate();
			System.out.print("	\"" + transition.getSrc() + "\"->\"" + transition.getDst() + "\"[label=\"");
			if (predicate == 0) {
				System.out.print("&predicate");
			} else if (predicate == 1) {
				System.out.print("!predicate");
			} else if (label != AFA.epsilon) {
				if (Character.isLetterOrDigit((char) label) || (char) label == '.') {
					System.out.print((char) label);
				} else {
					System.out.print(label);
				}
			} else {
				System.out.print("ε");
			}
			System.out.println("\"];");
		}
		System.out.println("}");
	}

	public static void generate(DFA dfa) {
		if (dfa == null) {
			System.out.println("WARNING : dfa is null");
			return;
		}

		System.out.println("\ndigraph g {");
		System.out.println("\"" + dfa.getf().getID() + "\"[style=filled,fillcolor=\"" + fColor + "\"];");

		for (State state : dfa.getF()) {
			System.out.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
		}

		for (Transition transition : dfa.getTau()) {
			int label = transition.getLabel();
			int predicate = transition.getPredicate();
			System.out.print("	\"" + transition.getSrc() + "\"->\"" + transition.getDst() + "\"[label=\"");
			if (predicate == 0) {
				System.out.print("&predicate");
			} else if (predicate == 1) {
				System.out.print("!predicate");
			} else if (label != AFA.epsilon) {
				if (Character.isLetterOrDigit((char) label) || (char) label == '.') {
					System.out.print((char) label);
				} else {
					System.out.print(label);
				}
			} else {
				System.out.print("ε");
			}
			System.out.println("\"];");
		}
		System.out.println("}");
	}

	private static void execCommandLine(String command) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);
			pr.waitFor();
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	private static final String dotFileName = "__bfa";

	public static void writeAFA(AFA afa) {
		if (afa == null) {
			System.out.println("WARNING : afa is null");
			return;
		}
		try {
			File file = new File(dotFileName + ".dot");
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
			for (Transition transition : afa.getTransitions()) {
				int label = transition.getLabel();
				int predicate = transition.getPredicate();
				pw.print("	\"" + transition.getSrc() + "\"->\"" + transition.getDst() + "\"[label=\"");
				if (predicate == 0) {
					pw.print("&predicate");
				} else if (predicate == 1) {
					pw.print("!predicate");
				} else if (label != AFA.epsilon) {
					if (Character.isLetterOrDigit((char) label) || (char) label == '.') {
						pw.print((char) label);
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

		execCommandLine("dot -Kdot -Tpng " + dotFileName + ".dot -o " + dotFileName + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

	}

	public static void writeDFA(DFA dfa) {
		if (dfa == null) {
			System.out.println("WARNING : dfa is null");
			return;
		}
		try {

			File file = new File(dotFileName + ".dot");
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

			for (Transition transition : dfa.getTau()) {
				int label = transition.getLabel();
				int predicate = transition.getPredicate();
				pw.print("	\"" + transition.getSrc() + "\"->\"" + transition.getDst() + "\"[label=\"");
				if (predicate == 0) {
					pw.print("&predicate");
				} else if (predicate == 1) {
					pw.print("!predicate");
				} else if (label != AFA.epsilon) {
					if (Character.isLetterOrDigit((char) label) || (char) label == '.') {
						pw.print((char) label);
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

		execCommandLine("dot -Kdot -Tpng " + dotFileName + ".dot -o " + dotFileName + ".png");
		// execCommandLine("open " + dotFileName + ".png &");
		// execCommandLine("rm " + dotFileName + ".dot");
		// execCommandLine("rm " + dotFileName + ".png");

	}

}
