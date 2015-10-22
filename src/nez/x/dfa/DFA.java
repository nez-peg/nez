package nez.x.dfa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeSet;

public class DFA {
	private HashSet<State> S = null;
	private TreeSet<Transition> tau = null;
	private State f = null;
	private HashSet<State> F = null;

	public DFA() {
		S = new HashSet<State>();
		tau = new TreeSet<Transition>();
		f = new State();
		F = new HashSet<State>();
	}

	public DFA(HashSet<State> S, TreeSet<Transition> tau, State f, HashSet<State> F) {
		this();
		for (State state : S) {
			this.S.add(new State(state.getID()));
		}
		for (Transition transition : tau) {
			this.tau.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
		}
		this.f = new State(f.getID());
		for (State state : F) {
			this.F.add(new State(state.getID()));
		}
	}

	public HashSet<State> getS() {
		return this.S;
	}

	public TreeSet<Transition> getTau() {
		return this.tau;
	}

	public State getf() {
		return this.f;
	}

	public HashSet<State> getF() {
		return this.F;
	}

	public ArrayList<ArrayList<Transition>> toAdjacencyList() {
		ArrayList<ArrayList<Transition>> adjacencyList = new ArrayList<ArrayList<Transition>>();
		for (int i = 0; i < S.size(); i++) {
			adjacencyList.add(new ArrayList<Transition>());
		}
		for (Transition transition : this.tau) {
			int src = transition.getSrc();
			int dst = transition.getDst();
			adjacencyList.get(src).add(new Transition(src, dst, transition.getLabel(), -1));
		}
		return adjacencyList;
	}

}
