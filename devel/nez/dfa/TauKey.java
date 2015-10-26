package nez.dfa;

public class TauKey implements Comparable<TauKey> {
	final private State state;
	final private int sigma;

	public TauKey(State state, int sigma) {
		this.state = new State(state.getID());
		this.sigma = sigma;
	}

	public State getState() {
		return state;
	}

	public int getSigma() {
		return sigma;
	}

	@Override
	public int compareTo(TauKey o) {
		if (state.getID() != o.getState().getID()) {
			return new Integer(state.getID()).compareTo(new Integer(o.getState().getID()));
		}
		return new Integer(sigma).compareTo(new Integer(o.getSigma()));
	}

}
