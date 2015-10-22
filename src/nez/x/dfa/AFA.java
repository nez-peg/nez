package nez.x.dfa;

import java.util.HashSet;
import java.util.TreeSet;

public class AFA {
	final public static char epsilon = ' ';
	private HashSet<State> S;
	// private TreeMap<TauKey, BooleanExpressionのようなもの> tau;
	private State f;
	private HashSet<State> F;
	private HashSet<State> L;

	private TreeSet<Transition> transitions; // tau構築用 visitor内ではこちらを利用

	public AFA() {
		S = new HashSet<State>();
		// tau = new TreeMap<TauKey, State>();
		f = new State();
		F = new HashSet<State>();
		L = new HashSet<State>();

		transitions = new TreeSet<Transition>();
	}

	public AFA(HashSet<State> S, TreeSet<Transition> transitions, State f, HashSet<State> F, HashSet<State> L) {
		this.S = S;
		this.transitions = transitions;
		this.f = f;
		this.F = F;
		this.L = L;
	}

	// public AFA(HashSet<State> S, TreeMap<TauKey, > tau, State f,
	// HashSet<State> F, HashSet<State> L) {
	// this.S = S;
	// this.tau = tau;
	// this.f = f;
	// this.F = F;
	// this.L = L;
	// }

	public void setS(HashSet<State> S) {
		this.S = S;
	}

	// public void setTau(TreeMap<TauKey, > tau) {
	// this.tau = tau;
	// }

	public void setf(State f) {
		this.f = f;
	}

	public void setF(HashSet<State> F) {
		this.F = F;
	}

	public void setL(HashSet<State> L) {
		this.L = L;
	}

	public void setTransitions(TreeSet<Transition> transitions) {
		this.transitions = transitions;
	}

	public HashSet<State> getS() {
		return this.S;
	}

	// public TreeMap<TauKey, > getTau() {
	// return this.tau;
	// }

	public State getf() {
		return this.f;
	}

	public HashSet<State> getF() {
		return this.F;
	}

	public HashSet<State> getL() {
		return this.L;
	}

	public TreeSet<Transition> getTransitions() {
		return this.transitions;
	}

	public DFA toDFA() {
		DFAConverter dfaConverter = new DFAConverter(new AFA(S, transitions, f, F, L));
		return dfaConverter.convert();
	}
}
