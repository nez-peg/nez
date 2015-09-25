package nez.x.dfa;

import java.util.Comparator;

public class State implements Comparable<State> {
	BFA bfa = null;
	public int id = -1;

	public State() {
	}

	public State(int stateID) {
		this.id = stateID;
	}

	public State(BFA bfa, int id) {
		this.bfa = bfa;
		this.id = id;
	}

	public void setBFA(BFA bfa) {
		this.bfa = bfa;
	}

	public BFA getBFA() {
		return bfa;
	}

	public int getID() {
		return id;
	}

	// boolean accept(Context context) {
	// boolean accept(Context context, Map<ExecMemoState, Boolean> execMemo) {
	boolean accept(Context context, byte[][] execMemo) {
		// System.out.println(this);
		// System.out.println("-----curretID = " + this.id);
		// System.out.println(context.getTop() + " | current char = " +
		// context.getChar() + " id = " + this.id);
		if (this.id == -1) {
			return false;
		}

		// ----- MEMO -----
		/*
		 * ExecMemoState ems = new ExecMemoState(this.id, context.getTop()); if
		 * (execMemo.containsKey(ems)) { return execMemo.get(ems); }
		 */
		if (execMemo[this.id][context.getTop()] != -1) {
			return execMemo[this.id][context.getTop()] == 1;
		}

		// ----- MEMO -----

		if (context.isEmpty()) {
			// System.out.println("bfa = " + bfa);
			return bfa.isAcceptingState(this);
		}
		// System.out.println(context.getTop() + " | current char = " +
		// context.getChar() + " id = " + this.id);
		// System.out.println("bfa is null? " + (this.bfa == null));
		State next = bfa.getNextState(this, context.getChar());
		// System.out.println("next = " + next);
		// System.out.println("");
		if (next == null) { // no such transition
			return false;
		}
		// System.out.println("set BFA -> " + next + " : null?" + (next.getBFA()
		// == null));
		Context nextContext = context.getContext();
		nextContext.incTop();

		// ----- MEMO -----
		boolean result = next.accept(nextContext, execMemo);
		// execMemo.put(ems, result);
		execMemo[this.id][context.getTop()] = (result ? (byte) 1 : (byte) 0);
		return result;
		// ----- MEMO -----
		// return next.accept(nextContext);
	}

	public static int compare(State state, State state2) {
		return Integer.compare(state.getID(), state2.getID());
	}

	@Override
	public String toString() {
		return Integer.toString(id);
	}

	@Override
	public int compareTo(State o) {
		return new Integer(this.id).compareTo(o.getID());
	}

}

class StateComparator implements Comparator {
	@Override
	public int compare(Object o1, Object o2) {
		State e1 = (State) o1;
		State e2 = (State) o2;
		return Integer.compare(e1.getID(), e2.getID());
	}
}
