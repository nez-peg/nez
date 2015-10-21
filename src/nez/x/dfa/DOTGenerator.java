package nez.x.dfa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class DOTGenerator {
	public DOTGenerator() {

	}

	private static final String fColor = "#4169E1"; // royalblue
	private static final String FColor = "#7fffd4"; // aquamarine
	private static final String LColor = "#ff6347"; // tomato

	public static void generate(AFA afa) {
		if (afa == null) {
			System.out.println("WARNING : afa is null");
			return;
		}

		System.out.println("\ndigraph g {");
		System.out.println("\"" + afa.getf().getID() + "\"[style=filled,fillcolor=\"" + fColor + "\"];");
		for (State state : afa.getF()) {
			System.out.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
		}
		for (State state : afa.getL()) {
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
			for (State state : afa.getF()) {
				pw.println("\"" + state.getID() + "\"[style=filled,fillcolor=\"" + FColor + "\"];");
			}
			for (State state : afa.getL()) {
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

}
