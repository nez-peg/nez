package nez.x.dfa;

import java.util.HashSet;
import java.util.TreeSet;

public class AFA {
	final public static char epsilon = ' ';
	private HashSet<State> S;
	private TreeSet<Transition> tau;
	private State f;
	private HashSet<State> F;
	private HashSet<State> L;

	public AFA() {
		S = new HashSet<State>();
		// tau = new TreeMap<TauKey, State>();
		f = new State();
		F = new HashSet<State>();
		L = new HashSet<State>();

		tau = new TreeSet<Transition>();
	}

	public AFA(HashSet<State> S, TreeSet<Transition> tau, State f, HashSet<State> F, HashSet<State> L) {
		this.S = S;
		this.tau = tau;
		this.f = f;
		this.F = F;
		this.L = L;
	}

	public void setS(HashSet<State> S) {
		this.S = S;
	}

	public void setf(State f) {
		this.f = f;
	}

	public void setF(HashSet<State> F) {
		this.F = F;
	}

	public void setL(HashSet<State> L) {
		this.L = L;
	}

	public void setTau(TreeSet<Transition> tau) {
		this.tau = tau;
	}

	public HashSet<State> getS() {
		return this.S;
	}

	public State getf() {
		return this.f;
	}

	public HashSet<State> getF() {
		return this.F;
	}

	public HashSet<State> getL() {
		return this.L;
	}

	public TreeSet<Transition> getTau() {
		return this.tau;
	}

	public DFA toDFA() {
		DFAConverter dfaConverter = new DFAConverter(new AFA(S, tau, f, F, L));
		return dfaConverter.convert();
	}
}
