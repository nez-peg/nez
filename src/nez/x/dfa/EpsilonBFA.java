package nez.x.dfa;

import java.util.Set;

/*
 class Context {
 StringContext sc;
 int top;

 Context(String context) {
 sc = new StringContext(context);
 top = 0;
 }

 boolean isEmpty() {
 return top == sc.length();
 }

 char charAt(int pos) {
 return (char) sc.byteAt(pos);
 }

 void incTop() {
 if (top < sc.length()) {
 ++top;
 }
 }

 }

 class Tau {
 State state;
 char ch;

 }

 class BFA2 {
 Set<State> S;
 Map<Tau, State> tau;
 State f;
 Set<State> F;
 Set<State> L;

 State getNextState(State state, char ch) {

 }
 }

 class State {
 BFA bfa;
 int id;

 boolean accept(Context context) {
 if (context.isEmpty()) {
 return bfa.isAcceptingState(this.id);
 }
 State next = bfa.getNextState(this, context.charAt(0)); // 失敗した場合は -1
 // とする

 if (next.id == -1) { // no such transition
 return false;
 }
 context.incTop();
 return next.accept(context);
 }
 }

 class And extends State {
 State left, right;

 @Override
 boolean accept(int ch) {
 return this.left.accept(ch) && this.right.accept(ch);
 }

 }

 class Or extends State {
 State left, right;

 @Override
 boolean accept(int ch) {
 return this.left.accept(ch) || this.right.accept(ch);
 }
 }

 class Not extends State {
 State inner;

 @Override
 boolean accept(int ch) {
 return !this.inner.accept(ch);
 }
 }
 */
public class EpsilonBFA {
	private Set<Integer> S; // HashSet
	private Set<Integer> initialState; // HashSet
	private Set<Integer> acceptingState; // HashSet
	private Set<Integer> acceptingStateLA;// HashSet
	private Set<Edge> edges; // TreeSet

	public EpsilonBFA(Set<Integer> S, Set<Integer> initialState, Set<Integer> acceptingState, Set<Integer> acceptingStateLA, Set<Edge> edges) {
		this.S = S;
		this.initialState = initialState;
		this.acceptingState = acceptingState;
		this.acceptingStateLA = acceptingStateLA;
		this.edges = edges;
	}

	public Set<Integer> getS() {
		return S;
	}

	public Set<Integer> getInitialState() {
		return initialState;
	}

	public Set<Integer> getAcceptingState() {
		return acceptingState;
	}

	public Set<Integer> getAcceptingStateLA() {
		return acceptingStateLA;
	}

	public Set<Edge> getEdges() {
		return edges;
	}

	public void setS(Set<Integer> S) {
		this.S = S;
	}

	public void setInitialState(Set<Integer> initialState) {
		this.initialState = initialState;
	}

	public void setAcceptingState(Set<Integer> acceptingState) {
		this.acceptingState = acceptingState;
	}

	public void setAcceptingStateLA(Set<Integer> acceptingStateLA) {
		this.acceptingStateLA = acceptingStateLA;
	}

	public void setEdges(Set<Edge> edges) {
		this.edges = edges;
	}

	@Override
	public String toString() {
		String buf = new String();
		buf += "S = {";
		for (Integer i : S) {
			buf += (i + " ");
		}
		buf += "}\n";
		buf += "initialState = {";
		for (Integer i : initialState) {
			buf += (i + " ");
		}
		buf += "}\n";
		buf += "acceptingState = {";
		for (Integer i : acceptingState) {
			buf += (i + " ");
		}
		buf += "}\n";
		buf += "acceptingStateLA = {";
		for (Integer i : acceptingStateLA) {
			buf += (i + " ");
		}
		buf += "}\n";
		buf += "edges = {";
		for (Edge e : edges) {
			buf += (e + " ");
		}
		buf += "}\n";
		return buf;
	}
}
