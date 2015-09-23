package nez.x.dfa;

import java.util.Comparator;

public class EpsilonMemoState {
	int stateID, predicate;

	public EpsilonMemoState(int stateID, int predicate) {
		this.stateID = stateID;
		this.predicate = predicate;
	}

	public int getStateID() {
		return this.stateID;
	}

	public int getPredicate() {
		return this.predicate;
	}

	public void setStateID(int stateID) {
		this.stateID = stateID;
	}

	public void setPredicate(int predicate) {
		this.predicate = predicate;
	}

}

class EpsilonMemoStateComparator implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		EpsilonMemoState e1 = (EpsilonMemoState) o1;
		EpsilonMemoState e2 = (EpsilonMemoState) o2;
		if (e1.getStateID() != e2.getStateID()) {
			return Integer.compare(e1.getStateID(), e2.getStateID());
		}
		return Integer.compare(e1.getPredicate(), e2.getPredicate());
	}
}