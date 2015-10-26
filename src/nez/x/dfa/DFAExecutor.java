package nez.x.dfa;

import java.util.ArrayList;

import nez.io.StringContext;

public class DFAExecutor {
	public static boolean exec(StringContext context, DFA dfa) {
		ArrayList<ArrayList<Transition>> adjacencyList = dfa.toAdjacencyList();
		int stateID = dfa.getf().getID();
		for (int i = 0; i < context.length(); i++) {
			boolean found = false;
			for (Transition transition : adjacencyList.get(stateID)) {
				if (transition.getLabel() == context.charAt(i)) {
					stateID = transition.getDst();
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}

		return dfa.getF().contains(new State(stateID));
	}
}
