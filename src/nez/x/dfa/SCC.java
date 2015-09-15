package nez.dfa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class SCC { // Strongly Connected Components
	static char epsilon = ' ';
	int V,nV;
	Graph graph;
	ArrayList<ArrayList<Integer>> G,rG;
	ArrayList<Boolean> used;
	ArrayList<Integer> cmp,vs;
	
	public SCC(int V,Graph graph) {
		this.V     = V;
		this.graph = graph;
		G  = new ArrayList<ArrayList<Integer>>();
		rG = new ArrayList<ArrayList<Integer>>();
		used = new ArrayList<Boolean>();
		cmp  = new ArrayList<Integer>();

		for(int i=0;i<V;i++) {
			 G.add(new ArrayList<Integer>());
			rG.add(new ArrayList<Integer>());
			used.add(false);
			cmp.add(-1);
		}
		vs   = new ArrayList<Integer>();
	}
	
	void add_edge(int s,int t){
		 G.get(s).add(t);
		rG.get(t).add(s);
	}
	
	void dfs(int v){
		used.set(v,true);
		int len = G.get(v).size();
		for(int i=0;i<len;i++) {
			int next = G.get(v).get(i);
			if( !used.get(next) ) {
				dfs(next);
			}
		}
		vs.add(v);
	}
	
	void rdfs(int v,int k){
		used.set(v,true);
		cmp.set(v,k);
		int len = rG.get(v).size();
		for(int i=0;i<len;i++){
			int next = rG.get(v).get(i);
			if( !used.get(next) ) {
				rdfs(next,k);
			}
		}
	}
	
	int scc(){
		for(int i=0;i<V;i++) {
			used.set(i,false);
		}
		vs.clear();
		for(int i=0;i<V;i++) {
			if( !used.get(i) ) {
				dfs(i);
			}
		}
		for(int i=0;i<V;i++){
			used.set(i, false);
		}
		int k = 0;
		int len = vs.size();
		for(int i=len-1;i>=0;i--) {
			int next = vs.get(i);
			if( !used.get(next) ) {
				rdfs(next,k++);
			}
		}
		return k;
	}
	
	public Graph removeEpsilonCycle() {
		Set<Integer> S                = graph.getS();
		Set<Integer> acceptingState   = graph.getAcceptingState();
		Set<Integer> acceptingStateLA = graph.getAcceptingStateLA(); 
		Set<Edge> edges = graph.getEdges();
		for(Edge e : edges) {
			if( e.getLabel() == epsilon && e.getPredicate() == -1 ) {
				add_edge(e.getSrc(),e.getDst());
			}
		}

		nV = scc();
		ArrayList<Integer> groupSize = new ArrayList<Integer>();
		for(int i=0;i<V;i++) {
			groupSize.add(0);
		}
		for(int i=0;i<V;i++){
			int groupID = cmp.get(i);
			int current_size = groupSize.get(groupID);
			groupSize.set(groupID,current_size+1);
		}
		
		
		ArrayList<Set<Integer>> additionalAcceptingState   = new ArrayList<Set<Integer>>(V);
		ArrayList<Set<Integer>> additionalAcceptingStateLA = new ArrayList<Set<Integer>>(V);
		Map<Integer,Integer>    fromGroupIDtoVertexID      = new HashMap<Integer,Integer>();

		int tmpV = V;
		for(int i=0;i<V;i++) {
			additionalAcceptingState.add(new HashSet<Integer>());
			additionalAcceptingStateLA.add(new HashSet<Integer>());
			if( groupSize.get(i) > 1 ) {
				S.add(tmpV);
				fromGroupIDtoVertexID.put(i,tmpV++);
			}
		}
		Set<Edge> newEdges = new TreeSet<Edge>(new EdgeComparator());
		for(Edge e : edges) {
			int src = e.getSrc();
			int dst = e.getDst();
			if( cmp.get(src) != cmp.get(dst) ) {
				int srcGroupID = cmp.get(src);
				int dstGroupID = cmp.get(dst);
				int srcVertexID = src;
				int dstVertexID = dst;
				if( groupSize.get(srcGroupID) > 1 ) {
					srcVertexID = fromGroupIDtoVertexID.get(srcGroupID);
				}
				if( groupSize.get(dstGroupID) > 1 ) {
					dstVertexID = fromGroupIDtoVertexID.get(dstGroupID);
				}
				newEdges.add(new Edge(srcVertexID,dstVertexID,e.getLabel(),e.getPredicate()));
			} else {
				int groupID  = cmp.get(src);
				int vertexID = (fromGroupIDtoVertexID.containsKey(groupID))?fromGroupIDtoVertexID.get(groupID):groupID;
				

				if( acceptingState.contains(src) ) {
					additionalAcceptingState.get(groupID).add(src);
				}
				if( acceptingState.contains(dst) ) {
					additionalAcceptingState.get(groupID).add(dst);
				}
				if( acceptingStateLA.contains(src) ) {
					additionalAcceptingStateLA.get(groupID).add(src);
				}
				if( acceptingStateLA.contains(dst) ) {
					additionalAcceptingStateLA.get(groupID).add(dst);
				}
				if( e.getLabel() == epsilon && e.getPredicate() == -1 ) continue;
				newEdges.add(new Edge(vertexID,vertexID,e.getLabel(),e.getPredicate()));
			}
		}

		for(int groupID=0;groupID<V;groupID++) {
			if( !fromGroupIDtoVertexID.containsKey(groupID) ) continue;
			int vertexID = fromGroupIDtoVertexID.get(groupID);
			if( additionalAcceptingState.get(groupID).size() > 0 ) {
				Set<Integer> tmpAcceptingState = additionalAcceptingState.get(groupID);
				for(Integer ac_vertexID : tmpAcceptingState) {
					newEdges.add(new Edge(vertexID,ac_vertexID,epsilon,-1));
				}
			}
			if( additionalAcceptingStateLA.get(groupID).size() > 0 ) {
				Set<Integer> tmpAcceptingStateLA = additionalAcceptingStateLA.get(groupID);
				for(Integer ac_vertexID : tmpAcceptingStateLA) {
					newEdges.add(new Edge(vertexID,ac_vertexID,epsilon,-1));
				}
			}
		}
		
		return new Graph(S,graph.getInitialState(),graph.getAcceptingState(),graph.getAcceptingStateLA(),newEdges);
	}
	
}
