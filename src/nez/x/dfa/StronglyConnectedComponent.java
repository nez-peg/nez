package nez.x.dfa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class StronglyConnectedComponent {

	int V, nV;
	AFA afa;
	ArrayList<ArrayList<Integer>> G, rG;
	ArrayList<Boolean> used;
	ArrayList<Integer> cmp, vs;

	public StronglyConnectedComponent(int V, AFA afa) {
		this.V = V;
		this.afa = afa;
		G = new ArrayList<ArrayList<Integer>>();
		rG = new ArrayList<ArrayList<Integer>>();
		used = new ArrayList<Boolean>();
		cmp = new ArrayList<Integer>();

		for (int i = 0; i < V; i++) {
			G.add(new ArrayList<Integer>());
			rG.add(new ArrayList<Integer>());
			used.add(false);
			cmp.add(-1);
		}
		vs = new ArrayList<Integer>();
	}

	void add_edge(int s, int t) {
		G.get(s).add(t);
		rG.get(t).add(s);
	}

	void dfs(int v) {
		used.set(v, true);
		int len = G.get(v).size();
		for (int i = 0; i < len; i++) {
			int next = G.get(v).get(i);
			if (!used.get(next)) {
				dfs(next);
			}
		}
		vs.add(v);
	}

	void rdfs(int v, int k) {
		used.set(v, true);
		cmp.set(v, k);
		int len = rG.get(v).size();
		for (int i = 0; i < len; i++) {
			int next = rG.get(v).get(i);
			if (!used.get(next)) {
				rdfs(next, k);
			}
		}
	}

	int scc() {
		for (int i = 0; i < V; i++) {
			used.set(i, false);
		}
		vs.clear();
		for (int i = 0; i < V; i++) {
			if (!used.get(i)) {
				dfs(i);
			}
		}
		for (int i = 0; i < V; i++) {
			used.set(i, false);
		}
		int k = 0;
		int len = vs.size();
		for (int i = len - 1; i >= 0; i--) {
			int next = vs.get(i);
			if (!used.get(next)) {
				rdfs(next, k++);
			}
		}
		return k;
	}

	public AFA removeEpsilonCycle() {
		HashSet<State> S = afa.getS();
		HashSet<State> F = afa.getF();
		HashSet<State> L = afa.getL();
		TreeSet<Transition> tau = afa.getTau();
		for (Transition e : tau) {
			if (e.getLabel() == AFA.epsilon && e.getPredicate() == -1) {
				add_edge(e.getSrc(), e.getDst());
			}
		}

		nV = scc();
		ArrayList<Integer> groupSize = new ArrayList<Integer>();
		for (int i = 0; i < V; i++) {
			groupSize.add(0);
		}
		for (int i = 0; i < V; i++) {
			int groupID = cmp.get(i);
			int current_size = groupSize.get(groupID);
			groupSize.set(groupID, current_size + 1);
		}

		ArrayList<Set<State>> additionalAcceptingState = new ArrayList<Set<State>>(V);
		ArrayList<Set<State>> additionalAcceptingStateLA = new ArrayList<Set<State>>(V);
		Map<Integer, Integer> fromGroupIDtoVertexID = new HashMap<Integer, Integer>();

		int tmpV = V;
		for (int i = 0; i < V; i++) {
			additionalAcceptingState.add(new HashSet<State>());
			additionalAcceptingStateLA.add(new HashSet<State>());
			if (groupSize.get(i) > 1) {
				S.add(new State(tmpV));
				fromGroupIDtoVertexID.put(i, tmpV++);
			}
		}
		TreeSet<Transition> newEdges = new TreeSet<Transition>();
		for (Transition e : tau) {
			int src = e.getSrc();
			int dst = e.getDst();
			if (cmp.get(src) != cmp.get(dst)) {
				int srcGroupID = cmp.get(src);
				int dstGroupID = cmp.get(dst);
				int srcVertexID = src;
				int dstVertexID = dst;
				if (groupSize.get(srcGroupID) > 1) {
					srcVertexID = fromGroupIDtoVertexID.get(srcGroupID);
				}
				if (groupSize.get(dstGroupID) > 1) {
					dstVertexID = fromGroupIDtoVertexID.get(dstGroupID);
				}
				newEdges.add(new Transition(srcVertexID, dstVertexID, e.getLabel(), e.getPredicate()));
			} else {
				int groupID = cmp.get(src);
				int vertexID = (fromGroupIDtoVertexID.containsKey(groupID)) ? fromGroupIDtoVertexID.get(groupID) : groupID;

				if (F.contains(new State(src))) {
					additionalAcceptingState.get(groupID).add(new State(src));
				}
				if (F.contains(new State(dst))) {
					additionalAcceptingState.get(groupID).add(new State(dst));
				}
				if (L.contains(new State(src))) {
					additionalAcceptingStateLA.get(groupID).add(new State(src));
				}
				if (L.contains(new State(dst))) {
					additionalAcceptingStateLA.get(groupID).add(new State(dst));
				}
				if (e.getLabel() == AFA.epsilon && e.getPredicate() == -1)
					continue;
				newEdges.add(new Transition(vertexID, vertexID, e.getLabel(), e.getPredicate()));
			}
		}

		for (int groupID = 0; groupID < V; groupID++) {
			if (!fromGroupIDtoVertexID.containsKey(groupID))
				continue;
			int vertexID = fromGroupIDtoVertexID.get(groupID);
			if (additionalAcceptingState.get(groupID).size() > 0) {
				Set<State> tmpAcceptingState = additionalAcceptingState.get(groupID);
				for (State state : tmpAcceptingState) {
					int ac_vertexID = state.getID();
					newEdges.add(new Transition(vertexID, ac_vertexID, AFA.epsilon, -1));
				}
			}
			if (additionalAcceptingStateLA.get(groupID).size() > 0) {
				Set<State> tmpAcceptingStateLA = additionalAcceptingStateLA.get(groupID);
				for (State state : tmpAcceptingStateLA) {
					int ac_vertexID = state.getID();
					newEdges.add(new Transition(vertexID, ac_vertexID, AFA.epsilon, -1));
				}
			}
		}
		return new AFA(S, newEdges, new State(afa.getf().getID()), afa.getF(), afa.getL());
	}
}
