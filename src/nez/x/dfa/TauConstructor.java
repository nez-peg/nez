package nez.x.dfa;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/*
 * this class constructs a tau function
 * 
 */
public class TauConstructor {
	static int MAX = 1000;
	static int AND = 0;
	static int OR = 1;
	EpsilonBFA BFA_graph;
	BFA final_bfa;
	ArrayList<Edge>[] bfa;
	int s;
	char sigma;
	int V;

	public TauConstructor(EpsilonBFA BFA_graph, BFA final_bfa, int s, char sigma, int V) {
		this.BFA_graph = BFA_graph;
		this.final_bfa = final_bfa;
		this.s = s;
		this.sigma = sigma;
		this.V = V;
		initBFA();
	}

	public TauConstructor(EpsilonBFA BFA_graph, BFA final_bfa, int s, char sigma) {
		this.BFA_graph = BFA_graph;
		this.final_bfa = final_bfa;
		this.s = s;
		this.sigma = sigma;
		initBFA();
	}

	// type 0 : and, type 1 : or
	private State mergeState(State left, State right, int type) {
		if (type == 0) {
			And and = new And(V++); // <---------------
			and.setLeft(left);
			and.setRight(right);
			return and;
		} else {
			Or or = new Or(V++);// <---------------
			or.setLeft(left);
			or.setRight(right);
			return or;
		}
	}

	private State constructAllSigma(Set<Integer> transitions) {
		State state = null;
		for (Integer i : transitions) {
			// state = new State(final_bfa, i);
			state = new State(i);
			break;
		}
		assert (state != null);
		transitions.remove(state.getID());
		if (transitions.isEmpty()) {
			return state;
		}
		for (Integer stateID : transitions) {
			// state = mergeState(state, new State(final_bfa, stateID), OR);
			state = mergeState(state, new State(stateID), OR);
		}
		return state;
	}

	private Not addNot(State state) {
		Not nextState = new Not(V++); // <---------------
		nextState.setInner(state);
		return nextState;
	}

	private State constructAllPredicate(Set<Edge> transitions) {
		State state = null;
		Edge removeEdge = null;
		for (Edge e : transitions) {
			state = constructTau(e.getDst(), false);
			if (e.getPredicate() == 1) {
				state = addNot(state);
			}
			removeEdge = e;
			break;
		}
		assert (state != null);
		transitions.remove(removeEdge);
		if (transitions.isEmpty()) {
			return state;
		}

		for (Edge e : transitions) {
			State state2 = constructTau(e.getDst(), false);
			if (e.getPredicate() == 1) {
				state2 = addNot(state2);
			}
			state = mergeState(state, state2, AND);
		}
		return state;
	}

	public State constructTau(int stateID, boolean alreadyMoved) {
		Set<Edge> predicateTransition = new TreeSet<Edge>(new EdgeComparator());
		Set<Integer> sigmaTransition = new HashSet<Integer>();
		for (int i = 0; i < bfa[stateID].size(); i++) {
			Edge e = bfa[stateID].get(i);
			if (e.getPredicate() != -1) {
				predicateTransition.add(e);
				continue;
			}
			if (alreadyMoved) {
				continue;
			}
			if (e.getLabel() != sigma && e.getLabel() != '.') {
				continue;
			}
			sigmaTransition.add(e.getDst());
		}
		if (predicateTransition.isEmpty() && sigmaTransition.isEmpty()) {
			if (alreadyMoved) {
				// return new State(final_bfa, stateID);
				return new State(stateID);
			} else {
				// return new State(final_bfa, -1); // ////
				return new State(-1); // ////
			}
		}
		if (predicateTransition.size() > 0) {
			if (sigmaTransition.isEmpty()) {
				return constructAllPredicate(predicateTransition);
			} else {
				return mergeState(constructAllPredicate(predicateTransition), constructAllSigma(sigmaTransition), AND);
			}
		} else {
			return constructAllSigma(sigmaTransition);
		}
	}

	public void initBFA() {
		bfa = new ArrayList[MAX];
		for (int i = 0; i < MAX; i++) {
			bfa[i] = new ArrayList<Edge>();
		}
		Set<Edge> edges = BFA_graph.getEdges();
		for (Edge e : edges) {
			bfa[e.getSrc()].add(e);
		}
	}

	public void setS(int s) {
		this.s = s;
	}

	public void setSigma(char sigma) {
		this.sigma = sigma;
	}

	public int getS() {
		return s;
	}

	public void setV(int V) {
		this.V = V;
	}

	public char getSigma() {
		return sigma;
	}

	public int getV() {
		return this.V;
	}

	@Override
	public String toString() {
		return "tau(" + s + "," + sigma + ")";
	}

}
