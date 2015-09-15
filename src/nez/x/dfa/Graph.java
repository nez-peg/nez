package nez.dfa;

import java.util.Set;
import java.util.Comparator;

public class Graph {
	private Set<Integer> S; // HashSet
	private Set<Integer> initialState; // HashSet
	private Set<Integer> acceptingState; // HashSet
	private Set<Integer> acceptingStateLA;// HashSet
	private Set<Edge>    edges; // TreeSet
	public Graph(Set<Integer> S,Set<Integer> initialState,Set<Integer> acceptingState,Set<Integer> acceptingStateLA,Set<Edge> edges) {
		this.S                = S;
		this.initialState     = initialState;
		this.acceptingState   = acceptingState;
		this.acceptingStateLA = acceptingStateLA;
		this.edges            = edges;
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
		for(Integer i : S) {
			buf += ( i + " ");
		}
		buf += "}\n";
		buf += "initialState = {";
		for(Integer i : initialState) {
			buf += ( i + " ");
		}
		buf += "}\n";
		buf += "acceptingState = {";
		for(Integer i : acceptingState) {
			buf += ( i + " ");
		}
		buf += "}\n";
		buf += "acceptingStateLA = {";
		for(Integer i : acceptingStateLA) {
			buf += ( i + " ");
		}
		buf += "}\n";
		buf += "edges = {";
		for(Edge e : edges) {
			buf += ( e + " " );
		}
		buf += "}\n";
		return buf;
	}
}
