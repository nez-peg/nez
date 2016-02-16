package nez.dfa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;

import nez.ast.TreeVisitorMap;
import nez.dfa.AFAConverter.DefaultVisitor;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Production;

/*
 * 
 * 非終端記号ー制約：
 * 	非終端記号の初期状態と受理状態は１つ
 * 	複数存在する場合は新たな状態を作成し、それを初期（受理）状態とし、それにε遷移をはるようにする
 * 	
 */
public class AFAConverter extends TreeVisitorMap<DefaultVisitor> {

	private boolean showTrace = false;
	private Grammar grammar = null;
	private AFA afa = null;
	private HashMap<String, State> initialStateOfNonTerminal; // 非終端記号の初期状態は１つ
	private HashMap<String, State> acceptingStateOfNonTerminal; // 非終端記号の受理状態は１つ
	private int theNumberOfStates;
	final private String StartProduction = "Start";

	public AFAConverter() {
		this.initialStateOfNonTerminal = new HashMap<String, State>();
		this.acceptingStateOfNonTerminal = new HashMap<String, State>();
		this.theNumberOfStates = 0;
		init(AFAConverter.class, new DefaultVisitor());
	}

	public AFAConverter(Grammar grammar) {
		this.grammar = grammar;
		this.initialStateOfNonTerminal = new HashMap<String, State>();
		this.acceptingStateOfNonTerminal = new HashMap<String, State>();
		this.theNumberOfStates = 0;
		init(AFAConverter.class, new DefaultVisitor());
	}

	public void build() {
		System.out.println("This function may be out of date :: use build(Production)");
		Production p = grammar.getProduction(StartProduction);
		if (p == null) {
			System.out.println("ERROR : START PRODUCTION \"" + StartProduction + "\" ... NOT FOUND");
			System.out.println("BUILD FAILED");
			return;
		}

		HashSet<String> visitedNonTerminal = new HashSet<String>();
		HashMap<String, Integer> nonTerminalToVertexID = new HashMap<String, Integer>();
		HashSet<String> visitedNonTerminalInPredicate = new HashSet<String>();
		HashMap<String, Integer> nonTerminalToVertexIDInPredicate = new HashMap<String, Integer>();

		this.afa = visitProduction(p, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, false);
		TreeSet<Transition> tau = this.afa.getTau();
		for (State state : this.afa.getL()) {
			tau.add(new Transition(state.getID(), state.getID(), AFA.anyCharacter, -1));
		}
		this.afa.setTau(tau);

		this.afa = eliminateEpsilonCycle(this.afa);
		this.afa = relabel(this.afa);
	}

	public void buildForPartial(Production p) {
		if (p == null) {
			System.out.println("ERROR : START PRODUCTION \"" + StartProduction + "\" ... NOT FOUND");
			System.out.println("BUILD FAILED");
			return;
		}

		System.out.println(p.getLocalName() + " => " + p.getExpression());
		ProductionConverterOfPrioritizedChoice pconverter = new ProductionConverterOfPrioritizedChoice();
		p.setExpression(pconverter.convert(p));
		System.out.println(p.getLocalName() + " => " + p.getExpression());

		HashSet<String> visitedNonTerminal = new HashSet<String>();
		HashMap<String, Integer> nonTerminalToVertexID = new HashMap<String, Integer>();
		HashSet<String> visitedNonTerminalInPredicate = new HashSet<String>();
		HashMap<String, Integer> nonTerminalToVertexIDInPredicate = new HashMap<String, Integer>();
		System.out.println("AFA construction START : " + p.getExpression());
		this.afa = visitProduction(p, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, false);
		TreeSet<Transition> tau = this.afa.getTau();
		for (State state : this.afa.getL()) {
			tau.add(new Transition(state.getID(), state.getID(), AFA.anyCharacter, -1));
		}

		HashSet<State> tmpS = this.afa.getS();
		HashSet<State> tmpF = new HashSet<State>();
		for (State state : this.afa.getF()) {
			int stateID = getNewStateID();
			tmpS.add(new State(stateID));
			tmpF.add(new State(stateID));
			tau.add(new Transition(state.getID(), stateID, AFA.epsilon, -1));
			tau.add(new Transition(stateID, stateID, AFA.theOthers, -1));
		}
		this.afa.setTau(tau);
		this.afa.setS(tmpS);
		this.afa.setF(tmpF);
		System.out.println("AFA construction END");
		// DOTGenerator.writeAFA(afa);
		// this.afa = eliminateEpsilonCycle(this.afa); // should not call this
		// function

		this.afa = relabel(this.afa);
		// System.out.println("write->");
		// DOTGenerator.writeAFA(this.afa);
		// System.out.println("<-write");
		System.out.println("=== AFA BUILD FINISHED ===");
	}

	public void build(Production p) {
		if (p == null) {
			System.out.println("ERROR : START PRODUCTION \"" + StartProduction + "\" ... NOT FOUND");
			System.out.println("BUILD FAILED");
			return;
		}

		System.out.println(p.getLocalName() + " => " + p.getExpression());
		ProductionConverterOfPrioritizedChoice pconverter = new ProductionConverterOfPrioritizedChoice();
		p.setExpression(pconverter.convert(p));
		System.out.println(p.getLocalName() + " => " + p.getExpression());

		HashSet<String> visitedNonTerminal = new HashSet<String>();
		HashMap<String, Integer> nonTerminalToVertexID = new HashMap<String, Integer>();
		HashSet<String> visitedNonTerminalInPredicate = new HashSet<String>();
		HashMap<String, Integer> nonTerminalToVertexIDInPredicate = new HashMap<String, Integer>();
		System.out.println("AFA construction START : " + p.getExpression());
		this.afa = visitProduction(p, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, false);
		TreeSet<Transition> tau = this.afa.getTau();
		for (State state : this.afa.getL()) {
			tau.add(new Transition(state.getID(), state.getID(), AFA.anyCharacter, -1));
		}
		this.afa.setTau(tau);
		System.out.println("AFA construction END");
		// DOTGenerator.writeAFA(afa);
		// this.afa = eliminateEpsilonCycle(this.afa); // should not call this
		// function

		this.afa = relabel(this.afa);
		// System.out.println("write->");
		// DOTGenerator.writeAFA(this.afa);
		// System.out.println("<-write");
		System.out.println("=== AFA BUILD FINISHED ===");
	}

	// Relabel state IDs from 0 to n
	public AFA relabel(AFA tmpAfa) {
		if (tmpAfa == null) {
			System.out.println("WARNING :: AFAConverter :: relabel :: argument tmpAfa is null");
			return null;
		}

		// DOTGenerator.writeAFA(tmpAfa);

		Map<Integer, Integer> newStateIDs = new HashMap<Integer, Integer>();

		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> transitions = new TreeSet<Transition>();
		State f = null;
		HashSet<State> F = new HashSet<State>();
		HashSet<State> L = new HashSet<State>();

		int newID = 0;
		for (State state : tmpAfa.getS()) {
			newStateIDs.put(state.getID(), newID);
			S.add(new State(newID++));
		}

		f = new State(newStateIDs.get(new Integer(tmpAfa.getf().getID())));
		for (Transition t : tmpAfa.getTau()) {
			transitions.add(new Transition(newStateIDs.get(new Integer(t.getSrc())), newStateIDs.get(new Integer(t.getDst())), t.getLabel(), t.getPredicate()));
		}

		for (State state : tmpAfa.getF()) {
			F.add(new State(newStateIDs.get(new Integer(state.getID()))));
		}

		for (State state : tmpAfa.getL()) {
			L.add(new State(newStateIDs.get(new Integer(state.getID()))));
		}

		return new AFA(S, transitions, f, F, L);
	}

	/*
	 * Start = ('a'*)* のように繰り返しを繰り返すとε遷移の閉路ができる
	 */
	public AFA eliminateEpsilonCycle(AFA argAfa) {
		StronglyConnectedComponent scc = new StronglyConnectedComponent(theNumberOfStates, argAfa);
		return scc.removeEpsilonCycle();
	}

	public AFA getAFA() {
		return this.afa;
	}

	public int getNewStateID() {
		return theNumberOfStates++;
	}

	public DFA computeDFA() {
		DFAConverter dfaConverter = new DFAConverter(this.afa);
		return dfaConverter.convert();
	}

	// <----- Visitor ----->

	private AFA visitProduction(Production rule, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate,
			boolean inPredicate) {
		if (showTrace) {
			System.out.println("here is Production : " + rule.getLocalName() + " " + (inPredicate ? "(in predicate)" : ""));
		}
		// return visitExpression(rule.getExpression());
		return visit(rule.getExpression(), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate);
	}

	public AFA visit(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
		return find(e.getClass().getSimpleName()).accept(e, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate);
	}

	public class DefaultVisitor {
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			System.out.println("ERROR :: INVALID INSTANCE : WHAT IS " + e);
			return null;
		}
	}

	public class Empty extends DefaultVisitor {

		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Pempty : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			int s = getNewStateID();
			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));

			F.add(new State(s));

			return new AFA(S, transitions, f, F, L);
		}
	}

	public class Fail extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Pfail : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}
			System.out.println("ERROR : WHAT IS Pfail : UNIMPLEMENTED FUNCTION");
			return null;
		}
	}

	public class Any extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Cany : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			int s = getNewStateID();
			int t = getNewStateID();
			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			S.add(new State(t));

			// transitions.add(new Transition(s, t, '.', -1));
			transitions.add(new Transition(s, t, AFA.anyCharacter, -1));

			F.add(new State(t));

			return new AFA(S, transitions, f, F, L);
		}
	}

	public class Byte extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Cbyte : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			int s = getNewStateID();
			int t = getNewStateID();
			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			S.add(new State(t));

			// transitions.add(new Transition(s, t, ((nez.lang.expr.Cbyte)
			// e).byteChar, -1));
			transitions.add(new Transition(s, t, ((nez.lang.Nez.Byte) e).byteChar, -1));

			F.add(new State(t));

			return new AFA(S, transitions, f, F, L);
		}
	}

	public class ByteSet extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Cset : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			int s = getNewStateID();
			int t = getNewStateID();
			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			S.add(new State(t));

			for (int i = 0; i < 256; i++) {
				// if (((nez.lang.expr.Cset) e).byteMap[i]) {
				if (((nez.lang.Nez.ByteSet) e).byteMap[i]) {
					transitions.add(new Transition(s, t, i, -1));
				}
			}

			F.add(new State(t));

			return new AFA(S, transitions, f, F, L);
		}
	}

	// e? ... e / !e ''
	public class Option extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Poption : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}
			AFA tmpAFA1 = visit(e.get(0), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate); // e
			AFA tmpAFA2 = computePnotAFA(copyAFA(tmpAFA1, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate)); // !e

			int s = getNewStateID();

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			for (State state : tmpAFA1.getS()) {
				S.add(new State(state.getID()));
			}
			for (State state : tmpAFA2.getS()) {
				S.add(new State(state.getID()));
			}

			transitions.add(new Transition(s, tmpAFA1.getf().getID(), AFA.epsilon, -1));
			transitions.add(new Transition(s, tmpAFA2.getf().getID(), AFA.epsilon, -1));
			for (Transition transition : tmpAFA1.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}
			for (Transition transition : tmpAFA2.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			for (State state : tmpAFA1.getF()) {
				F.add(new State(state.getID()));
			}
			for (State state : tmpAFA2.getF()) {
				F.add(new State(state.getID()));
			}
			if (tmpAFA2.getF().size() != 1) {
				System.out.println("FATAL ERROR : AFAConverter : Poption : tmpAFA2.getF().size() should be 1");
			}

			for (State state : tmpAFA1.getL()) {
				L.add(new State(state.getID()));
			}
			for (State state : tmpAFA2.getL()) {
				L.add(new State(state.getID()));
			}

			return new AFA(S, transitions, f, F, L);

			// // System.out.println("here is Poption : " + e);
			// System.out.println("WARNING : option FOUND ... e? should be
			// converted into ( e | !e ε )");
			//
			// // e? について、 e の AFA を tmpAfa として構築
			// // AFA tmpAfa = visitExpression(e.get(0));
			// AFA tmpAfa = visit(e.get(0));
			//
			// int s = getNewStateID();
			// int t = getNewStateID();
			//
			// HashSet<State> S = new HashSet<State>();
			// TreeSet<Transition> transitions = new TreeSet<Transition>();
			// State f = new State(s);
			// HashSet<State> F = new HashSet<State>();
			// HashSet<State> L = new HashSet<State>();
			//
			// S.add(new State(s));
			// S.add(new State(t));
			// for (State state : tmpAfa.getS()) {
			// S.add(new State(state.getID()));
			// }
			//
			// transitions.add(new Transition(s, t, AFA.epsilon, -1));
			// transitions.add(new Transition(s, tmpAfa.getf().getID(),
			// AFA.epsilon, -1));
			// for (State state : tmpAfa.getF()) {
			// transitions.add(new Transition(state.getID(), t, AFA.epsilon,
			// -1));
			// }
			// for (Transition transition : tmpAfa.getTau()) {
			// transitions.add(new Transition(transition.getSrc(),
			// transition.getDst(), transition.getLabel(),
			// transition.getPredicate()));
			// }
			//
			// F.add(new State(t));
			//
			// for (State state : tmpAfa.getL()) {
			// L.add(new State(state.getID()));
			// }
			//
			// return new AFA(S, transitions, f, F, L);
		}
	}

	// theNumberOfStatesを利用し、新たに状態に番号を割り振るためAFA内ではなくここに書いてある
	private AFA copyAFA(AFA base, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate) {
		if (base == null) {
			System.out.println("WARNING :: AFAConverter :: copyAFA :: argument base is null");
			return null;
		}
		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> tau = new TreeSet<Transition>();
		State f = new State();
		HashSet<State> F = new HashSet<State>();
		HashSet<State> L = new HashSet<State>();

		HashMap<Integer, Integer> newStateIDTable = new HashMap<Integer, Integer>();

		for (State state : base.getS()) {
			int newStateID;
			if (nonTerminalToVertexID.containsKey(new Integer(state.getID())) || nonTerminalToVertexIDInPredicate.containsKey(new Integer(state.getID()))) {
				newStateID = state.getID();
			} else {
				newStateID = getNewStateID();
			}
			newStateIDTable.put(state.getID(), newStateID);
			S.add(new State(newStateID));
		}

		for (Transition transition : base.getTau()) {
			int src = transition.getSrc();
			int dst = transition.getDst();
			int label = transition.getLabel();
			int predicate = transition.getPredicate();
			if (!newStateIDTable.containsKey(src)) {
				System.out.println("WARNING :: AFAConverter :: copyAFA :: newStateIDTable.containsKey(" + src + ") is false");
			}
			if (!newStateIDTable.containsKey(dst)) {
				DOTGenerator.writeAFA(base);
				for (State state : base.getS()) {
					System.out.println("nyan state = " + state);
				}
				try {
					int x = 0;
					for (int i = 0; i < 2000000000; i++) {
						x = x + i;
					}
					System.out.println(x);
				} catch (Exception e) {
				}
				System.out.println("WARNING :: AFAConverter :: copyAFA :: newStateIDTable.containsKey(" + dst + ") is false");
			}

			tau.add(new Transition(newStateIDTable.get(src), newStateIDTable.get(dst), label, predicate));
		}

		f = new State(newStateIDTable.get(base.getf().getID()));

		for (State state : base.getF()) {
			F.add(new State(newStateIDTable.get(state.getID())));
		}

		for (State state : base.getL()) {
			L.add(new State(newStateIDTable.get(state.getID())));
		}

		return new AFA(S, tau, f, F, L);
	}

	private AFA computePzeroAFA(AFA tmpAFA1, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate) {

		// Pnot pnot = new Pnot();
		// AFA tmpAFA2 = pnot.accept(e.get(0));
		AFA tmpAFA2 = computePnotAFA(copyAFA(tmpAFA1, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate));

		int s = getNewStateID();

		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> transitions = new TreeSet<Transition>();
		State f = new State(s);
		HashSet<State> F = new HashSet<State>();
		HashSet<State> L = new HashSet<State>();

		S.add(new State(s));
		for (State state : tmpAFA1.getS()) {
			S.add(new State(state.getID()));
		}
		for (State state : tmpAFA2.getS()) {
			S.add(new State(state.getID()));
		}

		transitions.add(new Transition(s, tmpAFA1.getf().getID(), AFA.epsilon, -1));
		transitions.add(new Transition(s, tmpAFA2.getf().getID(), AFA.epsilon, -1));
		for (State state : tmpAFA1.getF()) {
			transitions.add(new Transition(state.getID(), s, AFA.epsilon, -1));
		}
		for (Transition transition : tmpAFA1.getTau()) {
			transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
		}
		for (Transition transition : tmpAFA2.getTau()) {
			transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
		}

		F = tmpAFA2.getF();

		for (State state : tmpAFA1.getL()) {
			L.add(new State(state.getID()));
		}
		for (State state : tmpAFA2.getL()) {
			L.add(new State(state.getID()));
		}

		return new AFA(S, transitions, f, F, L);

	}

	// e* ... A <- e A / !e ''
	public class ZeroMore extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Pzero : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			// ((e*)*)* => e* | (e*)* causes epsilon cycle, so eliminate inner *
			// (e?)* => e* | (e?)* causes epsilon cycle, so eliminate ?
			boolean update = true;
			while (update) {
				update = false;
				// while (e.get(0) instanceof nez.lang.expr.Pzero) {
				while (e.get(0) instanceof nez.lang.Nez.ZeroMore) {
					update = true;
					e = e.get(0);
				}
				// while (e.get(0) instanceof nez.lang.expr.Poption) {
				while (e.get(0) instanceof nez.lang.Nez.Option) {
					update = true;
					e = e.get(0);
				}
			}
			AFA tmpAFA1 = visit(e.get(0), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate);

			// Pnot pnot = new Pnot();
			// AFA tmpAFA2 = pnot.accept(e.get(0));
			AFA tmpAFA2 = computePnotAFA(copyAFA(tmpAFA1, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate));

			int s = getNewStateID();

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			for (State state : tmpAFA1.getS()) {
				S.add(new State(state.getID()));
			}
			for (State state : tmpAFA2.getS()) {
				S.add(new State(state.getID()));
			}

			transitions.add(new Transition(s, tmpAFA1.getf().getID(), AFA.epsilon, -1));
			transitions.add(new Transition(s, tmpAFA2.getf().getID(), AFA.epsilon, -1));
			for (State state : tmpAFA1.getF()) {
				transitions.add(new Transition(state.getID(), s, AFA.epsilon, -1));
			}
			for (Transition transition : tmpAFA1.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}
			for (Transition transition : tmpAFA2.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			F = tmpAFA2.getF();

			for (State state : tmpAFA1.getL()) {
				L.add(new State(state.getID()));
			}
			for (State state : tmpAFA2.getL()) {
				L.add(new State(state.getID()));
			}
			return new AFA(S, transitions, f, F, L);

			// return REzero(e);
		}
	}

	// e+ ... e e*
	public class OneMore extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Pone : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}
			AFA tmpAFA1 = visit(e.get(0), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate); // e

			AFA tmpAFA2 = computePzeroAFA(copyAFA(tmpAFA1, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate,
					nonTerminalToVertexIDInPredicate); // e*

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = tmpAFA1.getf();
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			for (State state : tmpAFA1.getS()) {
				S.add(new State(state.getID()));
			}
			for (State state : tmpAFA2.getS()) {
				S.add(new State(state.getID()));
			}

			for (State state : tmpAFA1.getF()) {
				transitions.add(new Transition(state.getID(), tmpAFA2.getf().getID(), AFA.epsilon, -1));
			}
			for (Transition transition : tmpAFA1.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}
			for (Transition transition : tmpAFA2.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			for (State state : tmpAFA2.getF()) {
				F.add(new State(state.getID()));
			}

			for (State state : tmpAFA1.getL()) {
				L.add(new State(state.getID()));
			}
			for (State state : tmpAFA2.getL()) {
				L.add(new State(state.getID()));
			}

			return new AFA(S, transitions, f, F, L);

			// // System.out.println("here is Pone : " + e);
			// System.out.println("WARNING : zero or more FOUND ... e+ should be
			// converted into eA where A <- eA | !(eA) ε ");
			// // e+ について、 e の AFA を tmpAfa として構築
			// // AFA tmpAfa = visitExpression(e.get(0));
			// AFA tmpAfa = visit(e.get(0));
			//
			// int s = getNewStateID();
			// int t = getNewStateID();
			//
			// HashSet<State> S = new HashSet<State>();
			// TreeSet<Transition> transitions = new TreeSet<Transition>();
			// State f = new State(s);
			// HashSet<State> F = new HashSet<State>();
			// HashSet<State> L = new HashSet<State>();
			//
			// S.add(new State(s));
			// S.add(new State(t));
			// for (State state : tmpAfa.getS()) {
			// S.add(new State(state.getID()));
			// }
			//
			// transitions.add(new Transition(s, tmpAfa.getf().getID(),
			// AFA.epsilon, -1));
			// for (State state : tmpAfa.getF()) {
			// transitions.add(new Transition(state.getID(),
			// tmpAfa.getf().getID(), AFA.epsilon, -1));
			// transitions.add(new Transition(state.getID(), t, AFA.epsilon,
			// -1));
			// }
			// for (Transition transition : tmpAfa.getTau()) {
			// transitions.add(new Transition(transition.getSrc(),
			// transition.getDst(), transition.getLabel(),
			// transition.getPredicate()));
			// }
			//
			// F.add(new State(t));
			//
			// for (State state : tmpAfa.getL()) {
			// L.add(new State(state.getID()));
			// }
			//
			// return new AFA(S, transitions, f, F, L);
		}
	}

	public class And extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Pand : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			// AFA tmpAfa = visitExpression(e.get(0));
			AFA tmpAfa = visit(e.get(0), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, true);
			int s = getNewStateID();

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			for (State state : tmpAfa.getS()) {
				S.add(new State(state.getID()));
			}

			transitions.add(new Transition(s, tmpAfa.getf().getID(), AFA.epsilon, 0));
			for (Transition transition : tmpAfa.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			F.add(new State(s));

			for (State state : tmpAfa.getF()) {
				L.add(new State(state.getID()));
			}

			for (State state : tmpAfa.getL()) {
				L.add(new State(state.getID()));
			}

			// <--
			// for (State state : L) {
			// transitions.add(new Transition(state.getID(), state.getID(),
			// AFA.anyCharacter, -1));
			// }
			// -->

			return new AFA(S, transitions, f, F, L);
		}
	}

	private AFA computePnotAFA(AFA tmpAfa) {
		if (tmpAfa == null) {
			System.out.println("WARNING :: AFAConverter :: computePnotAFA :: argument tmpAfa is null");
			return null;
		}
		int s = getNewStateID();

		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> transitions = new TreeSet<Transition>();
		State f = new State(s);
		HashSet<State> F = new HashSet<State>();
		HashSet<State> L = new HashSet<State>();

		S.add(new State(s));
		for (State state : tmpAfa.getS()) {
			S.add(new State(state.getID()));
		}

		transitions.add(new Transition(s, tmpAfa.getf().getID(), AFA.epsilon, 1));
		for (Transition transition : tmpAfa.getTau()) {
			transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
		}

		F.add(new State(s));

		for (State state : tmpAfa.getF()) {
			L.add(new State(state.getID()));
		}

		for (State state : tmpAfa.getL()) {
			L.add(new State(state.getID()));
		}

		// <--
		// for (State state : L) {
		// transitions.add(new Transition(state.getID(), state.getID(),
		// AFA.anyCharacter, -1));
		// }
		// -->

		return new AFA(S, transitions, f, F, L);
	}

	public class Not extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Pnot : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			// AFA tmpAfa = visitExpression(e.get(0));

			AFA tmpAfa = visit(e.get(0), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, true);
			int s = getNewStateID();

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			for (State state : tmpAfa.getS()) {
				S.add(new State(state.getID()));
			}

			transitions.add(new Transition(s, tmpAfa.getf().getID(), AFA.epsilon, 1));
			for (Transition transition : tmpAfa.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			F.add(new State(s));

			for (State state : tmpAfa.getF()) {
				L.add(new State(state.getID()));
			}

			for (State state : tmpAfa.getL()) {
				L.add(new State(state.getID()));
			}

			// <--
			// for (State state : L) {
			// transitions.add(new Transition(state.getID(), state.getID(),
			// AFA.anyCharacter, -1));
			// }
			// -->

			return new AFA(S, transitions, f, F, L);
		}
	}

	// public class Psequence extends DefaultVisitor {
	public class Pair extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Psequence : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			// AFA tmpAfa1 = visitExpression(e.getFirst());
			// AFA tmpAfa2 = visitExpression(e.getNext());
			// AFA tmpAfa1 = visit(e.getFirst(), visitedNonTerminal,
			// nonTerminalToVertexID, visitedNonTerminalInPredicate,
			// nonTerminalToVertexIDInPredicate, inPredicate);
			AFA tmpAfa1 = visit(((nez.lang.Nez.Pair) e).first, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate);
			// AFA tmpAfa2 = visit(e.getNext(), visitedNonTerminal,
			// nonTerminalToVertexID, visitedNonTerminalInPredicate,
			// nonTerminalToVertexIDInPredicate, inPredicate);
			AFA tmpAfa2 = visit(((nez.lang.Nez.Pair) e).next, visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate);

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(tmpAfa1.getf().getID());
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			for (State state : tmpAfa1.getS()) {
				S.add(new State(state.getID()));
			}
			for (State state : tmpAfa2.getS()) {
				S.add(new State(state.getID()));
			}

			for (Transition transition : tmpAfa1.getTau()) {
				if (tmpAfa1.getF().contains(new State(transition.getDst()))) {
					transitions.add(new Transition(transition.getDst(), tmpAfa2.getf().getID(), AFA.epsilon, -1));
				}
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}
			for (Transition transition : tmpAfa2.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			if (tmpAfa1.getF().contains(tmpAfa1.getf())) {
				transitions.add(new Transition(tmpAfa1.getf().getID(), tmpAfa2.getf().getID(), AFA.epsilon, -1));
			}

			for (State state : tmpAfa2.getF()) {
				F.add(new State(state.getID()));
			}

			for (State state : tmpAfa1.getL()) {
				L.add(new State(state.getID()));
			}

			for (State state : tmpAfa2.getL()) {
				L.add(new State(state.getID()));
			}

			return new AFA(S, transitions, f, F, L);
		}
	}

	// concatenate tmpAfa1 with tmpAfa2
	private AFA computeConcatenateAFAs(AFA tmpAfa1, AFA tmpAfa2) {
		if (tmpAfa1 == null && tmpAfa2 == null) {
			System.out.println("WARNING :: AFAConverter :: computeConcatenateAFAs :: tmpAfa1 and tmpAfa2 are null");
			return null;
		}
		if (tmpAfa1 == null) {
			return tmpAfa2;
		}
		if (tmpAfa2 == null) {
			return tmpAfa1;
		}
		AFA newAfa1 = tmpAfa1;
		AFA newAfa2 = tmpAfa2;

		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> transitions = new TreeSet<Transition>();
		State f = new State(newAfa1.getf().getID());
		HashSet<State> F = new HashSet<State>();
		HashSet<State> L = new HashSet<State>();

		for (State state : newAfa1.getS()) {
			S.add(new State(state.getID()));
		}

		for (State state : newAfa2.getS()) {
			S.add(new State(state.getID()));
		}

		for (State state : newAfa1.getF()) {
			transitions.add(new Transition(state.getID(), newAfa2.getf().getID(), AFA.epsilon, -1));
		}

		for (Transition transition : newAfa1.getTau()) {
			transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
		}

		for (Transition transition : newAfa2.getTau()) {
			transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
		}

		for (State state : newAfa2.getF()) {
			F.add(new State(state.getID()));
		}

		for (State state : newAfa1.getL()) {
			L.add(new State(state.getID()));
		}

		for (State state : newAfa2.getL()) {
			L.add(new State(state.getID()));
		}
		return new AFA(S, transitions, f, F, L);
	}

	public class Choice extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				System.out.println("here is Pchoice : " + e + " " + (inPredicate ? "(in predicate)" : ""));
			}

			int s = getNewStateID();
			int t = getNewStateID();

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			S.add(new State(t));

			F.add(new State(t));

			for (int i = 0; i < e.size(); i++) {
				// AFA tmpAfa = visitExpression(e.get(i));
				AFA tmpAfa = visit(e.get(i), visitedNonTerminal, nonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate);

				for (State state : tmpAfa.getS()) {
					S.add(new State(state.getID()));
				}

				transitions.add(new Transition(s, tmpAfa.getf().getID(), AFA.epsilon, -1));
				for (State state : tmpAfa.getF()) {
					transitions.add(new Transition(state.getID(), t, AFA.epsilon, -1));
				}
				for (Transition transition : tmpAfa.getTau()) {
					transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
				}

				for (State state : tmpAfa.getL()) {
					L.add(new State(state.getID()));
				}

			}

			return new AFA(S, transitions, f, F, L);

		}
	}

	/*
	 * 以下のPchoiceは非終端記号には未対応　 A = 'a' A / 'a'
	 * のように優先度付き選択ないに再帰する非終端記号が存在すると以下の方法ではどうしようもない
	 * そのためPchoiceは正規表現の選択と等価なものを作成し、グラマーのほうで否定先読みを追加するように変更する A = 'a' A / 'a'
	 * -> A = 'a' A / !('a' A) 'a' 非終端記号さえなければ以下のコードは正しく動作するため消さないように
	 */
	// public class Pchoice extends DefaultVisitor {
	// @Override
	// public AFA accept(Expression e, HashSet<String> visitedNonTerminal,
	// HashMap<String, Integer> nonTerminalToVertexID) {
	// System.out.println("here is Pchoice : " + e);
	//
	// int s = getNewStateID();
	// int t = getNewStateID();
	//
	// HashSet<State> S = new HashSet<State>();
	// TreeSet<Transition> transitions = new TreeSet<Transition>();
	// State f = new State(s);
	// HashSet<State> F = new HashSet<State>();
	// HashSet<State> L = new HashSet<State>();
	//
	// S.add(new State(s));
	// S.add(new State(t));
	//
	// F.add(new State(t));
	//
	// AFA notAfaSequence = null;
	//
	// for (int i = 0; i < e.size(); i++) {
	// // AFA tmpAfa = visitExpression(e.get(i));
	// System.out.println("e.get(" + i + ") = " + e.get(i));
	//
	// AFA tmpAfa = visit(e.get(i), visitedNonTerminal, nonTerminalToVertexID);
	//
	// System.out.println("-----");
	// // DOTGenerator.writeAFA(tmpAfa);
	// DOTGenerator.writeAFA(new AFA(S, transitions, f, F, L));
	//
	// AFA tmpAfaWithNot = computeConcatenateAFAs(copyAFA(notAfaSequence),
	// copyAFA(tmpAfa));
	//
	// // for (State state : tmpAfa.getS()) {
	// for (State state : tmpAfaWithNot.getS()) {
	// S.add(new State(state.getID()));
	// }
	//
	// // transitions.add(new Transition(s, tmpAfa.getf().getID(),
	// // AFA.epsilon, -1));
	// transitions.add(new Transition(s, tmpAfaWithNot.getf().getID(),
	// AFA.epsilon, -1));
	// // for (State state : tmpAfa.getF()) {
	// for (State state : tmpAfaWithNot.getF()) {
	// transitions.add(new Transition(state.getID(), t, AFA.epsilon, -1));
	// }
	// // for (Transition transition : tmpAfa.getTau()) {
	// for (Transition transition : tmpAfaWithNot.getTau()) {
	// transitions.add(new Transition(transition.getSrc(), transition.getDst(),
	// transition.getLabel(), transition.getPredicate()));
	// }
	//
	// // for (State state : tmpAfa.getL()) {
	// for (State state : tmpAfaWithNot.getL()) {
	// L.add(new State(state.getID()));
	// }
	//
	// notAfaSequence = computeConcatenateAFAs(notAfaSequence,
	// computePnotAFA(tmpAfa));
	//
	// }
	//
	// return new AFA(S, transitions, f, F, L);
	//
	// }
	// }

	public class NonTerminal extends DefaultVisitor {
		@Override
		public AFA accept(Expression e, HashSet<String> visitedNonTerminal, HashMap<String, Integer> nonTerminalToVertexID, HashSet<String> visitedNonTerminalInPredicate, HashMap<String, Integer> nonTerminalToVertexIDInPredicate, boolean inPredicate) {
			if (showTrace) {
				// System.out.println("here is NonTerminal : " +
				// ((nez.lang.expr.NonTerminal) e).getLocalName() + " = " +
				// ((nez.lang.expr.NonTerminal)
				// e).getProduction().getExpression() + " " + (inPredicate ?
				// "(in predicate)" : ""));
				System.out.println("here is NonTerminal : " + ((nez.lang.NonTerminal) e).getLocalName() + " = " + ((nez.lang.NonTerminal) e).getProduction().getExpression() + " " + (inPredicate ? "(in predicate)" : ""));
			}
			// String nonTerminalName = ((nez.lang.expr.NonTerminal)
			// e).getLocalName();
			String nonTerminalName = ((nez.lang.NonTerminal) e).getLocalName();
			boolean alreadyVisited = false;
			if (inPredicate && visitedNonTerminalInPredicate.contains(nonTerminalName)) {
				alreadyVisited = true;
			}
			if (!inPredicate && visitedNonTerminal.contains(nonTerminalName)) {
				alreadyVisited = true;
			}
			if (alreadyVisited) {
				System.out.println((inPredicate ? "(in predicate)" : "") + nonTerminalName + " again");
				int stateID = getNewStateID();
				HashSet<State> S = new HashSet<State>();
				TreeSet<Transition> transitions = new TreeSet<Transition>();
				State f = new State(stateID);
				HashSet<State> F = new HashSet<State>();
				HashSet<State> L = new HashSet<State>();

				S.add(new State(stateID));

				if (inPredicate) {
					if (nonTerminalToVertexIDInPredicate.containsValue(nonTerminalName)) {
						System.out.println("ERROR :: AFAConverter :: NonTerminal :: accept :: there are no " + nonTerminalName);
					}
					S.add(new State(nonTerminalToVertexIDInPredicate.get(nonTerminalName)));
					transitions.add(new Transition(stateID, nonTerminalToVertexIDInPredicate.get(nonTerminalName), AFA.epsilon, -1));
				} else {
					if (nonTerminalToVertexID.containsValue(nonTerminalName)) {
						System.out.println("ERROR :: AFAConverter :: NonTerminal :: accept :: there are no " + nonTerminalName);
					}
					S.add(new State(nonTerminalToVertexID.get(nonTerminalName)));
					transitions.add(new Transition(stateID, nonTerminalToVertexID.get(nonTerminalName), AFA.epsilon, -1));
				}
				return new AFA(S, transitions, f, F, L);

			} else {
				System.out.println((inPredicate ? "(in predicate)" : "") + nonTerminalName + " totally first");
				int stateID = getNewStateID();
				AFA tmpAfa = null;
				if (inPredicate) {
					// HashSet<String> tmpVisitedNonTerminal = new
					// HashSet<String>();
					// HashMap<String, Integer> tmpNonTerminalToVertexID = new
					// HashMap<String, Integer>();
					// HashSet<String> tmpVisitedNonTerminalInPredicate = new
					// HashSet<String>();
					// HashMap<String, Integer>
					// tmpNonTerminalToVertexIDInPredicate = new HashMap<String,
					// Integer>();
					// tmpVisitedNonTerminalInPredicate.add(nonTerminalName);
					// tmpNonTerminalToVertexIDInPredicate.put(nonTerminalName,
					// stateID);
					// for (String itr : visitedNonTerminal) {
					// tmpVisitedNonTerminal.add(itr);
					// }
					// for (Map.Entry<String, Integer> itr :
					// nonTerminalToVertexID.entrySet()) {
					// tmpNonTerminalToVertexID.put(itr.getKey(),
					// itr.getValue());
					// }
					// for (String itr : visitedNonTerminalInPredicate) {
					// tmpVisitedNonTerminalInPredicate.add(itr);
					// }
					// for (Map.Entry<String, Integer> itr :
					// nonTerminalToVertexIDInPredicate.entrySet()) {
					// tmpNonTerminalToVertexIDInPredicate.put(itr.getKey(),
					// itr.getValue());
					// }
					// tmpAfa = visitProduction(((nez.lang.expr.NonTerminal)
					// e).getProduction(), tmpVisitedNonTerminal,
					// tmpNonTerminalToVertexID,
					// tmpVisitedNonTerminalInPredicate,
					// tmpNonTerminalToVertexIDInPredicate, inPredicate);

					HashSet<String> newVisitedNonTerminalInPredicate = new HashSet<String>();
					for (String name : visitedNonTerminalInPredicate) {
						newVisitedNonTerminalInPredicate.add(name);
					}
					newVisitedNonTerminalInPredicate.add(nonTerminalName);
					HashMap<String, Integer> newNonTerminalToVertexIDInPredicate = new HashMap<String, Integer>();
					for (Map.Entry<String, Integer> itr : nonTerminalToVertexIDInPredicate.entrySet()) {
						newNonTerminalToVertexIDInPredicate.put(itr.getKey(), itr.getValue());
					}
					newNonTerminalToVertexIDInPredicate.put(nonTerminalName, stateID);
					// tmpAfa = visitProduction(((nez.lang.expr.NonTerminal)
					// e).getProduction(), visitedNonTerminal,
					// nonTerminalToVertexID, newVisitedNonTerminalInPredicate,
					// newNonTerminalToVertexIDInPredicate, inPredicate);
					tmpAfa = visitProduction(((nez.lang.NonTerminal) e).getProduction(), visitedNonTerminal, nonTerminalToVertexID, newVisitedNonTerminalInPredicate, newNonTerminalToVertexIDInPredicate, inPredicate);

					// ::::::::::::::::: temporary :::::::::::::::::::::::::::::
					// the map and the set needs to eliminate the
					// nonTerminalName when you finish the visitor.
					//
					// modified 2016/1/15 for fix a bug of Non Terminal
					// visitedNonTerminalInPredicate.add(nonTerminalName);
					// nonTerminalToVertexIDInPredicate.put(nonTerminalName,
					// stateID);
					// tmpAfa = visitProduction(((nez.lang.expr.NonTerminal)
					// e).getProduction(), visitedNonTerminal,
					// nonTerminalToVertexID, visitedNonTerminalInPredicate,
					// nonTerminalToVertexIDInPredicate, inPredicate);
					// :::::::::::::::::: temporary
					// ::::::::::::::::::::::::::::::

				} else {
					// HashSet<String> tmpVisitedNonTerminal = new
					// HashSet<String>();
					// HashMap<String, Integer> tmpNonTerminalToVertexID = new
					// HashMap<String, Integer>();
					// HashSet<String> tmpVisitedNonTerminalInPredicate = new
					// HashSet<String>();
					// HashMap<String, Integer>
					// tmpNonTerminalToVertexIDInPredicate = new HashMap<String,
					// Integer>();
					// tmpVisitedNonTerminal.add(nonTerminalName);
					// tmpNonTerminalToVertexID.put(nonTerminalName, stateID);
					// for (String itr : visitedNonTerminal) {
					// tmpVisitedNonTerminal.add(itr);
					// }
					// for (Map.Entry<String, Integer> itr :
					// nonTerminalToVertexID.entrySet()) {
					// tmpNonTerminalToVertexID.put(itr.getKey(),
					// itr.getValue());
					// }
					// for (String itr : visitedNonTerminalInPredicate) {
					// tmpVisitedNonTerminalInPredicate.add(itr);
					// }
					// for (Map.Entry<String, Integer> itr :
					// nonTerminalToVertexIDInPredicate.entrySet()) {
					// tmpNonTerminalToVertexIDInPredicate.put(itr.getKey(),
					// itr.getValue());
					// }
					// tmpAfa = visitProduction(((nez.lang.expr.NonTerminal)
					// e).getProduction(), tmpVisitedNonTerminal,
					// tmpNonTerminalToVertexID,
					// tmpVisitedNonTerminalInPredicate,
					// tmpNonTerminalToVertexIDInPredicate, inPredicate);

					HashSet<String> newVisitedNonTerminal = new HashSet<String>();
					for (String name : visitedNonTerminal) {
						newVisitedNonTerminal.add(name);
					}
					newVisitedNonTerminal.add(nonTerminalName);
					HashMap<String, Integer> newNonTerminalToVertexID = new HashMap<String, Integer>();
					for (Map.Entry<String, Integer> itr : nonTerminalToVertexID.entrySet()) {
						newNonTerminalToVertexID.put(itr.getKey(), itr.getValue());
					}
					newNonTerminalToVertexID.put(nonTerminalName, stateID);
					// tmpAfa = visitProduction(((nez.lang.expr.NonTerminal)
					// e).getProduction(), newVisitedNonTerminal,
					// newNonTerminalToVertexID, visitedNonTerminalInPredicate,
					// nonTerminalToVertexIDInPredicate, inPredicate);
					tmpAfa = visitProduction(((nez.lang.NonTerminal) e).getProduction(), newVisitedNonTerminal, newNonTerminalToVertexID, visitedNonTerminalInPredicate, nonTerminalToVertexIDInPredicate, inPredicate);

					// ::::::::::: temporary ::::::::::::::
					// visitedNonTerminal.add(nonTerminalName);
					// nonTerminalToVertexID.put(nonTerminalName, stateID);
					// tmpAfa = visitProduction(((nez.lang.expr.NonTerminal)
					// e).getProduction(), visitedNonTerminal,
					// nonTerminalToVertexID, visitedNonTerminalInPredicate,
					// nonTerminalToVertexIDInPredicate, inPredicate);
					// ::::::::::: temporary ::::::::::::::
				}
				if (tmpAfa == null) {
					System.out.println("ERROR :: AFAConverter :: NonTerminal :: accept :: tmpAfa is null");
				}
				HashSet<State> S = new HashSet<State>();
				TreeSet<Transition> transitions = new TreeSet<Transition>();
				State f = new State(stateID);
				HashSet<State> F = new HashSet<State>();
				HashSet<State> L = new HashSet<State>();

				S.add(new State(stateID));
				for (State state : tmpAfa.getS()) {
					S.add(new State(state.getID()));
				}

				transitions.add(new Transition(stateID, tmpAfa.getf().getID(), AFA.epsilon, -1));
				// for (State state : tmpAfa.getF()) {
				// transitions.add(new Transition(state.getID(), t, AFA.epsilon,
				// -1));
				// }
				for (Transition transition : tmpAfa.getTau()) {
					transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
				}

				for (State state : tmpAfa.getF()) {
					F.add(new State(state.getID()));
				}
				// F.add(new State(t));

				for (State state : tmpAfa.getL()) {
					L.add(new State(state.getID()));
				}

				return new AFA(S, transitions, f, F, L);

			}
			// System.out.println("here is NonTerminal : " + e);
			// String nonTerminalName = ((nez.lang.expr.NonTerminal)
			// e).getLocalName();
			//
			// if (initialStateOfNonTerminal.containsKey(nonTerminalName)) { //
			// 既にこの非終端記号を訪れた場合
			// /*
			// * t は消しても良いと思うので要検討 tから戻ってきてもその先は空文字と等価なはずなのでも戻ってくる辺を追加する必要はない
			// * 戻る辺があると通常の遷移からこっちへ遷移してこれることになる（結局行き先は空文字と等価な状態なのだが） 正しくは t
			// * は存在しないはず
			// */
			// int s = getNewStateID();
			// int t = getNewStateID();
			// State nonTerminal_s =
			// initialStateOfNonTerminal.get(nonTerminalName);
			// State nonTerminal_t =
			// acceptingStateOfNonTerminal.get(nonTerminalName);
			// HashSet<State> S = new HashSet<State>();
			// TreeSet<Transition> transitions = new TreeSet<Transition>();
			// State f = new State(s);
			// HashSet<State> F = new HashSet<State>();
			// HashSet<State> L = new HashSet<State>();
			//
			// S.add(new State(s));
			// S.add(new State(t));
			// S.add(new State(nonTerminal_s.getID()));
			// S.add(new State(nonTerminal_t.getID()));
			//
			// transitions.add(new Transition(s, nonTerminal_s.getID(),
			// AFA.epsilon, -1));
			// transitions.add(new Transition(nonTerminal_t.getID(), t,
			// AFA.epsilon, -1));
			//
			// F.add(new State(t));
			//
			// return new AFA(S, transitions, f, F, L);
			// } else {
			//
			// int s = getNewStateID();
			// int t = getNewStateID();
			//
			// initialStateOfNonTerminal.put(nonTerminalName, new State(s));
			// acceptingStateOfNonTerminal.put(nonTerminalName, new State(t));
			//
			// AFA tmpAfa = visitProduction(((nez.lang.expr.NonTerminal)
			// e).getProduction());
			//
			// HashSet<State> S = new HashSet<State>();
			// TreeSet<Transition> transitions = new TreeSet<Transition>();
			// State f = new State(s);
			// HashSet<State> F = new HashSet<State>();
			// HashSet<State> L = new HashSet<State>();
			//
			// S.add(new State(s));
			// S.add(new State(t));
			// for (State state : tmpAfa.getS()) {
			// S.add(new State(state.getID()));
			// }
			//
			// transitions.add(new Transition(s, tmpAfa.getf().getID(),
			// AFA.epsilon, -1));
			// for (State state : tmpAfa.getF()) {
			// transitions.add(new Transition(state.getID(), t, AFA.epsilon,
			// -1));
			// }
			// for (Transition transition : tmpAfa.getTau()) {
			// transitions.add(new Transition(transition.getSrc(),
			// transition.getDst(), transition.getLabel(),
			// transition.getPredicate()));
			// }
			//
			// F.add(new State(t));
			//
			// for (State state : tmpAfa.getL()) {
			// L.add(new State(state.getID()));
			// }
			//
			// return new AFA(S, transitions, f, F, L);
			// }
		}
	}
}
