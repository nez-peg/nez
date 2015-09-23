package nez.x.dfa;

import java.util.Comparator;

public class Tau implements Comparable<Tau> {
	State state;
	char sigma;

	public Tau(State state, char sigma) {
		this.state = state;
		this.sigma = sigma;
	}

	public State getState() {
		return this.state;
	}

	public char getSigma() {
		return sigma;
	}

	@Override
	public String toString() {
		return "(" + state + " X " + sigma + ")";
	}

	@Override
	public int compareTo(Tau o) {
		int result = new Character(this.sigma).compareTo(new Character(o.getSigma()));
		if (result == 0) {
			result = state.compareTo(o.getState());
		}
		return result;
	}
}

class TauComparator implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		Tau e1 = (Tau) o1;
		Tau e2 = (Tau) o2;
		if (e1.getSigma() != e2.getSigma()) {
			return Character.compare(e1.getSigma(), e2.getSigma());
		}
		return State.compare(e1.getState(), e2.getState());
	}

}
