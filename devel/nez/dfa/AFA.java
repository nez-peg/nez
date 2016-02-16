package nez.dfa;

import java.util.HashSet;
import java.util.TreeSet;

public class AFA {
	// final public static char epsilon = ' ';
	final public static int theOthers = -3;
	final public static int anyCharacter = -2;
	final public static int epsilon = -1;
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

	// conversion of this AFA into a NFA
	// this method will return null when this AFA has a predicate
	public NFA toNFA() {
		for (Transition e : tau) {
			int p = e.getPredicate();
			if (p == 0 || p == 1) {
				System.out.println("In AFA.java : ERROR : this AFA cannot convert into a NFA :: this AFA has some predicates");
				return null;
			}
		}
		HashSet<State> allStates = new HashSet<State>();
		TreeSet<Transition> stateTransitionFunction = new TreeSet<Transition>();
		HashSet<State> initialStates = new HashSet<State>();
		HashSet<State> acceptingStates = new HashSet<State>();

		for (State state : S) {
			allStates.add(new State(state.getID()));
		}
		for (Transition t : tau) {
			stateTransitionFunction.add(new Transition(t.getSrc(), t.getDst(), t.getLabel(), t.getPredicate()));
		}
		initialStates.add(new State(f.getID()));
		for (State state : F) {
			acceptingStates.add(new State(state.getID()));
		}
		return new NFA(allStates, stateTransitionFunction, initialStates, acceptingStates);
	}
}
