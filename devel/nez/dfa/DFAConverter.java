package nez.dfa;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

//AFA を DFA に変換する
public class DFAConverter {

	private boolean showExecTimes = false;

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
		for (Transition transition : afa.getTau()) {
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

		if (be.hasValue()) {
			return new LogicVariable(be.getID(), be.getValue());
		}

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
				// if (epsilon.size() != 1) {
				// System.out.println("FATAL ERROR :
				// epsilonExpansionLogicVariable : predicate and epsilon.size()
				// = "
				// + epsilon.size() +
				// " : epsilon.size() must be 1 or My understanding is a little
				// bit wrong");
				// }
				if (epsilon.size() == 1) {
					BooleanExpression left = epsilonExpansion(predicate.get(0));
					BooleanExpression right = epsilonExpansion(epsilon.get(0));
					return new And(((predicateType.get(0) == 1) ? new Not(left) : left), right);
				} else {
					BooleanExpression left = epsilonExpansion(predicate.get(0));
					ArrayList<BooleanExpression> arrRight = new ArrayList<BooleanExpression>();
					for (LogicVariable lv : epsilon) {
						arrRight.add(epsilonExpansion(lv));
					}
					BooleanExpression right = new Or(arrRight.get(0), null);
					BooleanExpression top = right;
					for (int i = 1; i < arrRight.size(); i++) {
						if (i == (arrRight.size() - 1)) {
							((Or) right).right = arrRight.get(i);
						} else {
							((Or) right).right = new Or(arrRight.get(i), null);
							right = ((Or) right).right;
						}
					}
					return new And(((predicateType.get(0) == 1) ? new Not(left) : left), top);
				}
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

	public BooleanExpression transit(BooleanExpression be, char sigma) {
		if (be instanceof And) {
			return transitAnd(((And) be), sigma);
		} else if (be instanceof Or) {
			return transitOr(((Or) be), sigma);
		} else if (be instanceof Not) {
			return transitNot(((Not) be), sigma);
		} else {
			if (!(be instanceof LogicVariable)) {
				System.out.println("WHAT IS THIS");
			}
			return transitLogicVariable(((LogicVariable) be), sigma);
		}
	}

	private BooleanExpression transitAnd(And be, char sigma) {
		BooleanExpression tmpLeft = transit(be.left, sigma);
		BooleanExpression tmpRight = transit(be.right, sigma);

		// どちらか一方にでも false が存在する場合
		if (((tmpLeft instanceof LogicVariable) && ((LogicVariable) tmpLeft).isFalse()) || ((tmpRight instanceof LogicVariable) && ((LogicVariable) tmpRight).isFalse())) {
			return new LogicVariable(-1, false);
		}

		// どちらか一方にでも true が存在する場合
		if ((tmpLeft instanceof LogicVariable) && (tmpRight instanceof LogicVariable)) {
			if (((LogicVariable) tmpLeft).isTrue() && ((LogicVariable) tmpRight).isTrue()) {
				return new LogicVariable(-1, true);
			} else if (((LogicVariable) tmpLeft).isTrue()) {
				return tmpRight;
			} else if (((LogicVariable) tmpRight).isTrue()) {
				return tmpLeft;
			}
		} else if (tmpLeft instanceof LogicVariable) {
			if (((LogicVariable) tmpLeft).isTrue()) {
				return tmpRight;
			}
		} else if (tmpRight instanceof LogicVariable) {
			if (((LogicVariable) tmpRight).isTrue()) {
				return tmpLeft;
			}
		}

		return new And(tmpLeft, tmpRight);
	}

	private BooleanExpression transitOr(Or be, char sigma) {
		BooleanExpression tmpLeft = transit(be.left, sigma);
		BooleanExpression tmpRight = transit(be.right, sigma);

		// どちらか一方にでも false が存在する場合
		if ((tmpLeft instanceof LogicVariable) && (tmpRight instanceof LogicVariable)) {
			if (((LogicVariable) tmpLeft).isFalse() && ((LogicVariable) tmpRight).isFalse()) {
				return new LogicVariable(-1, false);
			} else if (((LogicVariable) tmpLeft).isFalse()) {
				return tmpRight;
			} else if (((LogicVariable) tmpRight).isFalse()) {
				return tmpLeft;
			}
		} else if (tmpLeft instanceof LogicVariable) {
			if (((LogicVariable) tmpLeft).isFalse()) {
				return tmpRight;
			}
		} else if (tmpRight instanceof LogicVariable) {
			if (((LogicVariable) tmpRight).isFalse()) {
				return tmpLeft;
			}
		}

		// どちらか一方にでも true が存在する場合
		if (((tmpLeft instanceof LogicVariable) && ((LogicVariable) tmpLeft).isTrue()) || ((tmpRight instanceof LogicVariable) && ((LogicVariable) tmpRight).isTrue())) {
			return new LogicVariable(-1, true);
		}

		return new Or(tmpLeft, tmpRight);
	}

	private BooleanExpression transitNot(Not be, char sigma) {
		BooleanExpression tmp = transit(be.inner, sigma);
		if (tmp instanceof LogicVariable && ((LogicVariable) tmp).hasValue()) {
			((LogicVariable) tmp).reverseValue();
			return tmp;
		}
		return new Not(tmp);
	}

	private BooleanExpression transitLogicVariable(LogicVariable be, char sigma) {
		ArrayList<LogicVariable> next = new ArrayList<LogicVariable>();
		for (Transition transition : adjacencyList.get(be.getID())) {
			if (transition.getPredicate() == -1 && (transition.getLabel() == sigma || transition.getLabel() == AFA.anyCharacter)) {
				next.add(new LogicVariable(transition.getDst()));
			}
		}
		if (next.isEmpty()) {
			return new LogicVariable(-1, false);
		}
		if (next.size() == 1) {
			return next.get(0);
		}
		BooleanExpression tmp = new Or(next.get(0), null);
		BooleanExpression top = tmp;
		for (int i = 1; i < next.size(); i++) {
			if (i == (next.size() - 1)) {
				((Or) tmp).right = next.get(i);
			} else {
				((Or) tmp).right = new Or(next.get(i), null);
				tmp = ((Or) tmp).right;
			}
		}
		return top;
	}

	public DFA convert() {

		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> tau = new TreeSet<Transition>();
		State f = null;
		HashSet<State> F = new HashSet<State>();

		BDD bdd = new BDD();
		Map<Integer, Integer> BDDIDtoVertexID = new HashMap<Integer, Integer>();
		int vertexID = 0;
		Deque<BooleanExpression> deq = new ArrayDeque<BooleanExpression>();
		{
			BooleanExpression lf = new LogicVariable(afa.getf().getID());
			// BooleanExpression ef = epsilonExpansionWithEpsilonCycle(lf);
			BooleanExpression ef = epsilonExpansion(lf);

			// System.out.println("lf = " + lf);
			// System.out.println("epsilon expansion lf = " + ef);

			int bddID = bdd.build(ef);
			ef.bddID = bddID;

			deq.addFirst(ef);

			// System.out.println(bddID + " => " + vertexID);
			f = new State(vertexID);
			S.add(new State(vertexID));
			if (ef.eval(afa.getF(), afa.getL())) {
				F.add(new State(vertexID));
			}
			BDDIDtoVertexID.put(bddID, vertexID++);
		}

		HashMap<String, Integer> cacheVertexID = new HashMap<String, Integer>();
		while (!deq.isEmpty()) {
			BooleanExpression be = deq.poll();
			// System.out.println("-----" + be);
			// System.out.println("BDD ID = " + be.bddID);
			if (be instanceof LogicVariable && (((LogicVariable) be).isFalse() || ((LogicVariable) be).isTrue())) {
				continue;
			}

			// for (char c = '!'; c <= '~'; c++) {
			// for (char c = 'a'; c <= 'd'; c++) {

			int src = BDDIDtoVertexID.get(be.bddID);
			for (int i = 0; i < 256; i++) {
				char c = (char) i;
				// System.out.println("---");
				// System.out.println("be = " + be + "," + c);
				// System.out.println("transit --START");
				long t = System.nanoTime();
				BooleanExpression transitBe = transit(be, c);
				long t2 = System.nanoTime();
				if (showExecTimes) {
					System.out.println("transit time           : " + (t2 - t));
				}

				// System.out.println("transit --END");
				// System.out.println("transitBe = " + transitBe);

				if (be instanceof LogicVariable && (((LogicVariable) be).isFalse() || ((LogicVariable) be).isTrue())) {
					continue;
				}

				// System.out.println("epsilonExpansion --START");

				t = System.nanoTime();
				BooleanExpression epsilonExpansionTransitBe = epsilonExpansion(transitBe);
				t2 = System.nanoTime();
				if (showExecTimes) {
					System.out.println("epsilon Expansion time : " + (t2 - t));
				}

				if (cacheVertexID.containsKey(epsilonExpansionTransitBe.toString())) {
					int dst = cacheVertexID.get(epsilonExpansionTransitBe.toString());
					tau.add(new Transition(src, dst, c, -1));
					continue;
				}

				// System.out.println("epsilonExpansion --END");
				// BooleanExpression epsilonExpansionTransitBe =
				// epsilonExpansionWithEpsilonCycle(transitBe);

				// System.out.println("eetb = " + epsilonExpansionTransitBe);
				// System.out.println("bdd.build --START");
				t = System.nanoTime();
				int bddID = bdd.build(epsilonExpansionTransitBe);
				t2 = System.nanoTime();
				if (showExecTimes) {
					System.out.println("bdd.build time         : " + (t2 - t));
				}
				epsilonExpansionTransitBe.bddID = bddID;

				// System.out.println("bdd.build --END");
				// System.out.println("bddID = " + bddID +
				// " already exists?? -> " + BDDIDtoVertexID.containsKey(new
				// Integer(bddID)));

				// System.out.println("bdd.build(2) --START");
				// int src = BDDIDtoVertexID.get(bdd.build(be));
				t = System.nanoTime();
				if (!BDDIDtoVertexID.containsKey(be.bddID)) {
					System.out.println("ERROR :: DFAConverter.convert :: BDDIDtoVertexID does not contain " + be.bddID);
				}

				t2 = System.nanoTime();
				if (showExecTimes) {
					System.out.println("get time               : " + (t2 - t));
				}

				// System.out.println("bdd.build(2) --END");
				int dst = -1;
				if (!BDDIDtoVertexID.containsKey(new Integer(bddID))) { // 初めて現れた状態ならば追加する
					// System.out.println("vertexID = " + vertexID);
					S.add(new State(vertexID));
					// System.out.println("epsilonExpansionTransitBe.eval --START");
					t = System.nanoTime();
					if (epsilonExpansionTransitBe.eval(afa.getF(), afa.getL())) {
						F.add(new State(vertexID));
					}
					t2 = System.nanoTime();
					if (showExecTimes) {
						System.out.println("eval time              : " + (t2 - t));
					}

					// System.out.println("epsilonExpansionTransitBe.eval --END");
					dst = vertexID;
					cacheVertexID.put(epsilonExpansionTransitBe.toString(), vertexID);
					BDDIDtoVertexID.put(bddID, vertexID++);
					deq.addLast(epsilonExpansionTransitBe);
				} else {
					dst = BDDIDtoVertexID.get(bddID);
				}
				tau.add(new Transition(src, dst, c, -1));
			}
		}
		return new DFA(S, tau, f, F);
	}

	// <--- new epsilon expansion : it accepts epsilon cycle

	public BooleanExpression epsilonExpansionWithEpsilonCycle(BooleanExpression arg) {
		BooleanExpression be = arg.deepCopy();
		BDD bdd = new BDD();
		HashSet<Integer> isAlreadyGenerated = new HashSet<Integer>();
		int bddID = bdd.build(be.recoverPredicate());
		isAlreadyGenerated.add(new Integer(bddID));

		while (true) {
			be = epsilonExpansionOnce(be);
			bddID = bdd.build(be.recoverPredicate());
			// System.out.println("once : bddID = " + bddID + " : " + be);

			if (isAlreadyGenerated.contains(new Integer(bddID))) {
				return be.recoverPredicate();
			}
			isAlreadyGenerated.add(new Integer(bddID));
		}
	}

	public BooleanExpression epsilonExpansionOnce(BooleanExpression be) {
		if (be instanceof And) {
			return epsilonExpansionAndOnce((And) be);
		} else if (be instanceof Or) {
			return epsilonExpansionOrOnce((Or) be);
		} else if (be instanceof Not) {
			return epsilonExpansionNotOnce((Not) be);
		} else {
			if (!(be instanceof LogicVariable)) {
				System.out.println("ERROR : epsilonExpansion : " + be + " is not instance of LogicVariable");
			}
			return epsilonExpansionLogicVariableOnce((LogicVariable) be);
		}
	}

	public BooleanExpression epsilonExpansionAndOnce(And be) {
		return new And(epsilonExpansionOnce(be.left), epsilonExpansionOnce(be.right));
	}

	public BooleanExpression epsilonExpansionOrOnce(Or be) {
		return new Or(epsilonExpansionOnce(be.left), epsilonExpansionOnce(be.right));
	}

	public BooleanExpression epsilonExpansionNotOnce(Not be) {
		return new Not(epsilonExpansionOnce(be.inner));
	}

	public BooleanExpression epsilonExpansionLogicVariableOnce(LogicVariable be) {

		if (be.hasValue()) {
			return new LogicVariable(be.getID(), be.getValue());
		}

		if (be.getID() < 0) { // predicate用
			return new LogicVariable(be.getID(), be.getValue());
		}

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
				// System.out.println(be + " -> FATAL SUPPORT : " +
				// predicate.get(0));
				// System.out.println("FATAL ERROR :
				// epsilonExpansionLogicVariable : predicate.size() == 1 &&
				// hasOtherwise");
				// System.out.println("WARNING : epsilonExpansionLogicVariable :
				// predicate.size() == 1 && hasOtherwise");
			}
			if (epsilon.isEmpty()) {
				// BooleanExpression tmp =
				// epsilonExpansionOnce(predicate.get(0));
				BooleanExpression tmp = predicate.get(0);
				// -be.getID()とすることで自分を再度展開しないようにする
				return new And(((predicateType.get(0) == 1) ? new Not(tmp) : tmp), new LogicVariable(-be.getID()));
			} else {
				// if (epsilon.size() != 1) {
				// System.out.println("FATAL ERROR :
				// epsilonExpansionLogicVariable : predicate and epsilon.size()
				// = "
				// + epsilon.size() +
				// " : epsilon.size() must be 1 or My understanding is a little
				// bit wrong");
				// }
				if (epsilon.size() == 1) {
					// BooleanExpression left =
					// epsilonExpansion(predicate.get(0));
					// BooleanExpression right =
					// epsilonExpansion(epsilon.get(0));
					BooleanExpression left = predicate.get(0);
					BooleanExpression right = epsilon.get(0);
					return new And(((predicateType.get(0) == 1) ? new Not(left) : left), right);
				} else {
					// BooleanExpression left =
					// epsilonExpansion(predicate.get(0));
					BooleanExpression left = predicate.get(0);
					ArrayList<BooleanExpression> arrRight = new ArrayList<BooleanExpression>();
					for (LogicVariable lv : epsilon) {
						// arrRight.add(epsilonExpansion(lv));
						arrRight.add(lv);
					}
					BooleanExpression right = new Or(arrRight.get(0), null);
					BooleanExpression top = right;
					for (int i = 1; i < arrRight.size(); i++) {
						if (i == (arrRight.size() - 1)) {
							((Or) right).right = arrRight.get(i);
						} else {
							((Or) right).right = new Or(arrRight.get(i), null);
							right = ((Or) right).right;
						}
					}
					return new And(((predicateType.get(0) == 1) ? new Not(left) : left), top);
				}
			}
		} else { // Or
			ArrayList<BooleanExpression> arr = new ArrayList<BooleanExpression>();
			if (hasOtherwise) {
				arr.add(new LogicVariable(be.getID()));
			}
			for (LogicVariable lv : epsilon) {
				// arr.add(epsilonExpansion(lv));
				arr.add(lv);
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

	// --->

}