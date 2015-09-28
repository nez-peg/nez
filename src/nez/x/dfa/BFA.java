package nez.x.dfa;

import java.util.Map;
import java.util.Set;

public class BFA {
	public Set<State> S;
	public Map<Tau, State> tau;
	public State f;
	public Set<State> F;
	public Set<State> L;

	public BFA() {
	}

	public BFA(Set<State> S, Map<Tau, State> tau, State f, Set<State> F, Set<State> L) {
		this.S = S;
		this.tau = tau;
		this.f = f;
		this.F = F;
		this.L = L;
	}

	public Set<State> getS() {
		return this.S;
	}

	public Map<Tau, State> getTau() {
		return this.tau;
	}

	public State getf() {
		// this.f.setBFA(this);
		return this.f;
	}

	public Set<State> getF() {
		return this.F;
	}

	public Set<State> getL() {
		return this.L;
	}

	public void setS(Set<State> S) {
		this.S = S;
	}

	public void setTau(Map<Tau, State> tau) {
		this.tau = tau;
	}

	public void setf(State f) {
		this.f = f;
	}

	public void setF(Set<State> F) {
		this.F = F;
	}

	public void setL(Set<State> L) {
		this.L = L;
	}

	public boolean isAcceptingState(State state) {
		return F.contains(state) || L.contains(state);
	}

	public State getNextState(State s, char sigma) {
		// System.out.println("sigma = " + sigma);
		Tau tmp = new Tau(s, sigma);
		// System.out.println("sigma? = " + tmp);
		// System.out.println("contain ?= " + tau.containsKey(tmp));
		if (tau.containsKey(tmp)) {
			return tau.get(new Tau(s, sigma));
		}
		return null;
	}

	// State next = bfa.getNextState(this, context.charAt(0)

}
