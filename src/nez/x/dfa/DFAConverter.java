package nez.x.dfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

// AFA を DFA に変換する
public class DFAConverter {

	private int theNumberOfStates;
	private AFA afa = null;
	private ArrayList<ArrayList<Transition>> adjacencyList = null;

	public DFAConverter(AFA afa) {
		this.afa = afa;
		this.theNumberOfStates = afa.getS().size();
		adjacencyList = new ArrayList<ArrayList<Transition>>();
		for (int i = 0; i < this.theNumberOfStates; i++) {
			adjacencyList.add(new ArrayList<Transition>());
		}
		for (Transition transition : afa.getTransitions()) {
			adjacencyList.get(transition.getSrc()).add(transition);
		}
	}

	public BooleanExpression epsilonExpansion(BooleanExpression be) {
		if (be instanceof And) {
			return epsilonExpansionAnd((And) be);
		} else if (be instanceof Or) {
			return epsilonExpansionOr((Or) be);
		} else if (be instanceof Not) {
			return epsilonExpansionNot((Not) be);
		} else {
			if (!(be instanceof LogicVariable)) {
				System.out.println("ERROR : epsilonExpansion : " + be + " is not instance of LogicVariable");
			}
			return epsilonExpansionLogicVariable((LogicVariable) be);
		}
	}

	public BooleanExpression epsilonExpansionAnd(And be) {
		return new And(epsilonExpansion(be.left), epsilonExpansion(be.right));
	}

	public BooleanExpression epsilonExpansionOr(Or be) {
		return new Or(epsilonExpansion(be.left), epsilonExpansion(be.right));
	}

	public BooleanExpression epsilonExpansionNot(Not be) {
		return new Not(epsilonExpansion(be.inner));
	}

	public BooleanExpression epsilonExpansionLogicVariable(LogicVariable be) {
		ArrayList<LogicVariable> predicate = new ArrayList<LogicVariable>();
		ArrayList<Integer> predicateType = new ArrayList<Integer>();
		ArrayList<LogicVariable> epsilon = new ArrayList<LogicVariable>();
		boolean hasOtherwise = false;
		for (Transition transition : adjacencyList.get(be.getID())) {
			if (transition.getPredicate() != -1) {
				predicate.add(new LogicVariable(transition.getDst()));
				predicateType.add(transition.getPredicate());
				continue;
			}
			if (transition.getLabel() == AFA.epsilon) {
				epsilon.add(new LogicVariable(transition.getDst()));
				continue;
			}
			hasOtherwise = true;
		}

		if (predicate.isEmpty() && epsilon.isEmpty()) {
			return new LogicVariable(be.getID());
		}

		if (!predicate.isEmpty() && predicate.size() != 1) {
			System.out.println("FATAL ERROR : epsilonExpansionLogicVariable : predicate.size() = " + predicate.size() + " : predicate.size() must be 1 or My understanding is a little bit wrong");
		}

		if (!predicate.isEmpty()) { // And
			if (hasOtherwise) {
				System.out.println("FATAL ERROR : epsilonExpansionLogicVariable : predicate.size() == 1 && hasOtherwise");
			}
			if (epsilon.isEmpty()) {
				BooleanExpression tmp = epsilonExpansion(predicate.get(0));
				return new And(((predicateType.get(0) == 1) ? new Not(tmp) : tmp), new LogicVariable(be.getID()));
			} else {
				if (epsilon.size() != 1) {
					System.out.println("FATAL ERROR : epsilonExpansionLogicVariable : predicate and epsilon.size() = " + epsilon.size() + " : epsilon.size() must be 1 or My understanding is a little bit wrong");
				}
				BooleanExpression left = epsilonExpansion(predicate.get(0));
				BooleanExpression right = epsilonExpansion(epsilon.get(0));
				return new And(((predicateType.get(0) == 1) ? new Not(left) : left), right);
			}
		} else { // Or
			ArrayList<BooleanExpression> arr = new ArrayList<BooleanExpression>();
			if (hasOtherwise) {
				arr.add(new LogicVariable(be.getID()));
			}
			for (LogicVariable lv : epsilon) {
				arr.add(epsilonExpansion(lv));
			}

			if (arr.isEmpty()) {
				System.out.println("ERROR : epsilonExpansionLogicVariable : arr.isEmpty() : WHAT IS THIS");
			}
			if (arr.size() == 1) {
				return arr.get(0);
			}
			BooleanExpression tmp = new Or(arr.get(0), null);
			BooleanExpression top = tmp;
			for (int i = 1; i < arr.size(); i++) {
				if (i == (arr.size() - 1)) {
					((Or) tmp).right = arr.get(i);
				} else {
					((Or) tmp).right = new Or(arr.get(i), null);
					tmp = ((Or) tmp).right;
				}
			}
			return top;
		}
	}

	public DFA convert() {

		int vertexID = 0;
		Deque<BooleanExpression> deq = new ArrayDeque<BooleanExpression>();
		{
			BooleanExpression f = new LogicVariable(afa.getf().getID());
			System.out.println("f = " + f);
			System.out.println("epsilon expansion f = " + epsilonExpansion(f));
		}

		while (!deq.isEmpty()) {
			BooleanExpression be = deq.poll();

		}

		return null;
	}

}
