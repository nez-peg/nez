package nez.dfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class NFA {
	private HashSet<State> allStates = null;
	private TreeSet<Transition> stateTransitionFunction = null;
	private HashSet<State> initialStates = null;
	private HashSet<State> acceptingStates = null;

	public NFA() {
		allStates = new HashSet<>();
		stateTransitionFunction = new TreeSet<>();
		initialStates = new HashSet<>();
		acceptingStates = new HashSet<>();
	}

	public NFA(HashSet<State> allStates, TreeSet<Transition> stateTransitionFunction, HashSet<State> initialStates, HashSet<State> acceptingStates) {
		this();
		this.allStates = allStates;
		this.stateTransitionFunction = stateTransitionFunction;
		this.initialStates = initialStates;
		this.acceptingStates = acceptingStates;
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
		ArrayList<Integer> stateList = new ArrayList<>();
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
		HashSet<State> allStates = new HashSet<>();
		TreeSet<Transition> stateTransitionFunction = new TreeSet<>();
		State initialState = null;
		HashSet<State> acceptingStates = new HashSet<>();

		ArrayList<ArrayList<Transition>> adjacencyList = new ArrayList<>();
		for (int i = 0; i < getAllStates().size(); i++) {
			adjacencyList.add(new ArrayList<>());
		}
		for (Transition transition : getStateTransitionFunction()) {
			int src = transition.getSrc();
			int dst = transition.getDst();
			adjacencyList.get(src).add(new Transition(src, dst, transition.getLabel(), transition.getPredicate()));
		}

		int stateID = 0;
		Deque<HashSet<State>> deq = new ArrayDeque<>();
		TreeMap<String, State> dfaStateTable = new TreeMap<>();

		deq.addLast(getInitialStates());
		System.out.println("inital = " + getInitialStates());
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
				HashSet<State> newStates = new HashSet<>();
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

}
