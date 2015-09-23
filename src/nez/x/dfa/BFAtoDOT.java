package nez.x.dfa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import nez.util.ConsoleUtils;

public class BFAtoDOT {
	static char epsilon = ' ';
	private EpsilonBFA BFA_graph;

	public BFAtoDOT(EpsilonBFA BFA_graph) {
		this.BFA_graph = BFA_graph;
	}

	public void setBFA(EpsilonBFA BFA_graph) {
		this.BFA_graph = BFA_graph;
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

}
