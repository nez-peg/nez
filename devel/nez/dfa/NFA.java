package nez.dfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

public class NFA {
	private HashSet<State> allStates = null;
	private TreeSet<Transition> stateTransitionFunction = null;
	private HashSet<State> initialStates = null;
	private HashSet<State> acceptingStates = null;

	private TreeMap<TauKey, HashSet<State>> tau = null;

	public NFA() {
		allStates = new HashSet<State>();
		stateTransitionFunction = new TreeSet<Transition>();
		initialStates = new HashSet<State>();
		acceptingStates = new HashSet<State>();
	}

	public NFA(HashSet<State> allStates, TreeSet<Transition> stateTransitionFunction, HashSet<State> initialStates, HashSet<State> acceptingStates) {
		this();
		this.allStates = allStates;
		this.stateTransitionFunction = stateTransitionFunction;
		this.initialStates = initialStates;
		this.acceptingStates = acceptingStates;
	}

	public TreeMap<TauKey, HashSet<State>> getTau() {
		return tau;
	}

	public HashSet<State> getAllStates() {
		return allStates;
	}

	public TreeSet<Transition> getStateTransitionFunction() {
		return stateTransitionFunction;
	}

	public HashSet<State> getInitialStates() {
		return initialStates;
	}

	public HashSet<State> getAcceptingStates() {
		return acceptingStates;
	}

	public void setAllStates(HashSet<State> allStates) {
		this.allStates = allStates;
	}

	public void setStateTransitionFunction(TreeSet<Transition> stateTransitionFunction) {
		this.stateTransitionFunction = stateTransitionFunction;
	}

	public void setInitialStates(HashSet<State> initialStates) {
		this.initialStates = initialStates;
	}

	public void setAcceptingStates(HashSet<State> acceptingStates) {
		this.acceptingStates = acceptingStates;
	}

	public String encode(HashSet<State> states) {
		StringBuilder sb = new StringBuilder();
		ArrayList<Integer> stateList = new ArrayList<Integer>();
		for (State state : states) {
			stateList.add(state.getID());
		}
		Collections.sort(stateList);
		for (int i = 0; i < stateList.size(); i++) {
			sb.append(String.valueOf(stateList.get(i)) + ":");
		}
		return sb.toString();
	}

	// 部分集合構成法から DFA を生成する
	// Brozozowski's algorithm 用なのでε遷移を展開する処理がない　そのため一般に使用することはできないので注意すること
	public DFA det() {
		if (getInitialStates() == null) {
			System.out.println("ERROR : det : NFA is null");
			return null;
		}
		HashSet<State> allStates = new HashSet<State>();
		TreeSet<Transition> stateTransitionFunction = new TreeSet<Transition>();
		State initialState = null;
		HashSet<State> acceptingStates = new HashSet<State>();

		ArrayList<ArrayList<Transition>> adjacencyList = new ArrayList<ArrayList<Transition>>();
		for (int i = 0; i < getAllStates().size(); i++) {
			adjacencyList.add(new ArrayList<Transition>());
		}
		for (Transition transition : getStateTransitionFunction()) {
			int src = transition.getSrc();
			int dst = transition.getDst();
			adjacencyList.get(src).add(new Transition(src, dst, transition.getLabel(), transition.getPredicate(), transition.getTheOthers()));
		}

		int stateID = 0;
		Deque<HashSet<State>> deq = new ArrayDeque<HashSet<State>>();
		TreeMap<String, State> dfaStateTable = new TreeMap<String, State>();

		deq.addLast(getInitialStates());
		// System.out.println("inital = " + getInitialStates());
		dfaStateTable.put(encode(getInitialStates()), new State(stateID));
		initialState = new State(stateID);
		{
			boolean isAcceptingState = false;
			for (State state : getInitialStates()) {
				if (getAcceptingStates().contains(state)) {
					isAcceptingState = true;
					break;
				}
			}
			if (isAcceptingState) {
				acceptingStates.add(new State(stateID));
			}
		}
		allStates.add(new State(stateID++));

		while (!deq.isEmpty()) {
			HashSet<State> states = deq.poll();
			for (int sigma = 0; sigma < 256; sigma++) {
				HashSet<State> newStates = new HashSet<State>();
				for (State state : states) {
					for (int i = 0; i < adjacencyList.get(state.getID()).size(); i++) {
						if (adjacencyList.get(state.getID()).get(i).getLabel() != sigma) {
							continue;
						}
						newStates.add(new State(adjacencyList.get(state.getID()).get(i).getDst()));
					}
				}
				if (dfaStateTable.containsKey(encode(newStates))) {
					stateTransitionFunction.add(new Transition(dfaStateTable.get(encode(states)).getID(), dfaStateTable.get(encode(newStates)).getID(), sigma, -1));
					continue;
				}
				deq.addLast(newStates);
				dfaStateTable.put(encode(newStates), new State(stateID));
				stateTransitionFunction.add(new Transition(dfaStateTable.get(encode(states)).getID(), stateID, sigma, -1));
				boolean isAcceptingState = false;
				for (State state : newStates) {
					if (getAcceptingStates().contains(state)) {
						isAcceptingState = true;
						break;
					}
				}
				if (isAcceptingState) {
					acceptingStates.add(new State(stateID));
				}
				allStates.add(new State(stateID++));

			}
		}

		return new DFA(allStates, stateTransitionFunction, initialState, acceptingStates);

	}

	public BitSet epsilonTransit(BitSet bset) {
		boolean update = true;
		BitSet nextSet = bset.copy();
		while (update) {
			update = false;
			ArrayList<Integer> iset = nextSet.toArrayList();
			for (int i = 0; i < iset.size(); i++) {
				int v = iset.get(i);
				for (TauKey key : graph.get(v)) {
					if (key.getSigma() != AFA.epsilon) {
						continue;
					}
					int next = key.getState().getID();
					if (!nextSet.get(next)) {
						nextSet.add(next);
						update = true;
					}
				}
			}
		}
		return nextSet;
	}

	public boolean isAcceptingState(BitSet bset) {
		for (Integer state : bset.toArrayList()) {
			if (acceptingStates.contains(new State(state))) {
				return true;
			}
		}
		return false;
	}

	public BitSet move(BitSet bset, char c) {
		BitSet nextSet = bset.copy();
		ArrayList<Integer> iset = nextSet.toArrayList();
		for (int i = 0; i < iset.size(); i++) {
			nextSet.remove(iset.get(i));
		}
		for (int i = 0; i < iset.size(); i++) {
			int v = iset.get(i);
			for (TauKey key : graph.get(v)) {
				if (key.getSigma() != AFA.anyCharacter && key.getSigma() != c) {
					continue;
				}
				int next = key.getState().getID();
				nextSet.add(next);
			}
		}
		return nextSet;
	}

	private ArrayList<ArrayList<TauKey>> graph = null;

	private void initGraph() {
		graph = new ArrayList<ArrayList<TauKey>>();
		for (int i = 0; i < allStates.size(); i++) {
			graph.add(new ArrayList<TauKey>());
		}
		for (Transition transit : stateTransitionFunction) {
			graph.get(transit.getSrc()).add(new TauKey(new State(transit.getDst()), transit.getLabel()));
		}
	}

	public DFA subsetConstruction() {
		initGraph();
		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> tau = new TreeSet<Transition>();
		State f = null;
		HashSet<State> F = new HashSet<State>();
		int newID = 0;
		Map<String, Integer> table = new HashMap<String, Integer>();
		Deque<BitSet> deq = new ArrayDeque<BitSet>();
		{
			BitSet set = new BitSet();
			for (State state : initialStates) {
				set.add(state.getID());
			}
			set = epsilonTransit(set);
			f = new State(newID);
			if (isAcceptingState(set)) {
				F.add(new State(newID));
			}
			S.add(new State(newID));
			table.put(set.toString(), newID++);
			deq.addLast(set);
		}
		while (!deq.isEmpty()) {
			BitSet bset = deq.pollFirst();
			ArrayList<Integer> iset = bset.toArrayList();

			System.out.println();
			int currID = table.get(bset.toString());
			for (char c = 0; c < 256; c++) {
				// for (char c = 'a'; c <= 'd'; c++) {
				BitSet nextSet = move(bset, c);
				nextSet = epsilonTransit(nextSet);
				int nextID = -1;
				boolean firstTime = false;
				if (table.containsKey(nextSet.toString())) {
					nextID = table.get(nextSet.toString());
				} else {
					nextID = newID++;
					S.add(new State(nextID));
					table.put(nextSet.toString(), nextID);
					if (isAcceptingState(nextSet)) {
						F.add(new State(nextID));
					}
					firstTime = true;
				}
				tau.add(new Transition(currID, nextID, c, -1));
				if (firstTime) {
					deq.addLast(nextSet);
				}
			}
		}
		return new DFA(S, tau, f, F);
	}

	public DFA toDFA() {
		return subsetConstruction();
	}
}
