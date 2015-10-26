package nez.dfa;

import java.util.HashSet;
import java.util.TreeSet;

public class NFA {
	private HashSet<State> allStates = null;
	private TreeSet<Transition> stateTransitionFunction = null;
	private HashSet<State> initialStates = null;
	private HashSet<State> acceptingStates = null;

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

	// 部分集合構成法から DFA を生成する
	// Brozozowski's algorithm 用なのでε遷移を展開する処理がない　そのため一般に使用することはできないので注意すること
	public DFA det() {
		return null;
	}

}
