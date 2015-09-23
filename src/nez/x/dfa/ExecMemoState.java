package nez.x.dfa;

import java.util.Comparator;

public class ExecMemoState implements Comparable<ExecMemoState> {
	public int stateID, top;

	public ExecMemoState(int stateID, int top) {
		this.stateID = stateID;
		this.top = top;
	}

	public void setStateID(int stateID) {
		this.stateID = stateID;
	}

	public void setTop(int top) {
		this.top = top;
	}

	@Override
	public int compareTo(ExecMemoState ems) {
		int result = new Integer(this.stateID).compareTo(ems.stateID);
		if (result == 0) {
			result = new Integer(this.top).compareTo(ems.top);
		}
		return result;
	}
}

class ExecMemoStateComparator implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		ExecMemoState e1 = (ExecMemoState) o1;
		ExecMemoState e2 = (ExecMemoState) o2;
		if (e1.stateID != e2.stateID) {
			return Integer.compare(e1.stateID, e2.stateID);
		}
		return Integer.compare(e1.top, e2.top);
	}
}