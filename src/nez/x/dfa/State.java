package nez.x.dfa;

public class State {
	private int ID;

	public State() {

	}

	public State(int ID) {
		this.ID = ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public int getID() {
		return this.ID;
	}

	@Override
	public String toString() {
		return String.valueOf(ID);
	}

	@Override
	public int hashCode() {
		return new Integer(ID).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof State) {
			final State state = (State) obj;
			return this.getID() == state.getID();
		}
		return false;
	}

	public int compareTo(State state) {
		return new Integer(ID).compareTo(state.getID());
	}

}
