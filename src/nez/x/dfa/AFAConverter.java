package nez.x.dfa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import nez.Grammar;
import nez.ast.TreeVisitor;
import nez.lang.Expression;
import nez.lang.Production;
import nez.lang.expr.Cany;
import nez.lang.expr.Cbyte;
import nez.lang.expr.Cset;
import nez.lang.expr.NonTerminal;
import nez.lang.expr.Pand;
import nez.lang.expr.Pchoice;
import nez.lang.expr.Pempty;
import nez.lang.expr.Pfail;
import nez.lang.expr.Pnot;
import nez.lang.expr.Pone;
import nez.lang.expr.Poption;
import nez.lang.expr.Psequence;
import nez.lang.expr.Pzero;

/*
 * 非終端記号ー制約：
 * 	非終端記号の初期状態と受理状態は１つ
 * 	複数存在する場合は新たな状態を作成し、それを初期（受理）状態とし、それにε遷移をはるようにする
 * 	
 */
public class AFAConverter extends TreeVisitor {

	private Grammar grammar = null;
	private AFA afa = null;
	private HashMap<String, State> initialStateOfNonTerminal; // 非終端記号の初期状態は１つ
	private HashMap<String, State> acceptingStateOfNonTerminal; // 非終端記号の受理状態は１つ
	private int theNumberOfStates;
	final private String StartProduction = "Start";

	public AFAConverter(Grammar grammar) {
		this.grammar = grammar;
		this.initialStateOfNonTerminal = new HashMap<String, State>();
		this.acceptingStateOfNonTerminal = new HashMap<String, State>();
		this.theNumberOfStates = 0;
	}

	public void build() {
		Production p = grammar.getProduction(StartProduction);
		if (p == null) {
			System.out.println("ERROR : START PRODUCTION \"" + StartProduction + "\" ... NOT FOUND");
			System.out.println("BUILD FAILED");
			return;
		}
		AFA afa = visitProduction(p);
	}

	public int getNewStateID() {
		return theNumberOfStates++;
	}

	// <----- Visitor ----->

	private AFA visitProduction(Production rule) {
		System.out.println("here is Production : " + rule.getLocalName());
		return visitExpression(rule.getExpression());
	}

	private AFA visitExpression(Expression e) {
		if (e instanceof Pempty) {
			return visitPempty(e);
		} else if (e instanceof Pfail) {
			return visitPfail(e);
		} else if (e instanceof Cany) {
			return visitCany(e);
		} else if (e instanceof Cbyte) {
			return visitCbyte(e);
		} else if (e instanceof Cset) {
			return visitCset(e);
		} else if (e instanceof Poption) {
			return visitPoption(e);
		} else if (e instanceof Pzero) {
			return visitPzero(e);
		} else if (e instanceof Pone) {
			return visitPone(e);
		} else if (e instanceof Pand) {
			return visitPand(e);
		} else if (e instanceof Pnot) {
			return visitPnot(e);
		} else if (e instanceof Psequence) {
			return visitPsequence(e);
		} else if (e instanceof Pchoice) {
			return visitPchoice(e);
		} else if (e instanceof nez.lang.expr.NonTerminal) {
			return visitNonTerminal(e);
		}
		// else if (e instanceof Cmulti) {
		// return visitCmulti(e);
		// } else if (e instanceof Tlink) {
		// return visitTlink(e);
		// } else if (e instanceof Tnew) {
		// return visitTnew(e);
		// } else if (e instanceof Tcapture) {
		// return visitTcapture(e);
		// } else if (e instanceof Treplace) {
		// return visitTreplace(e);
		// } else if (e instanceof Xblock) {
		// return visitXblock(e);
		// }
		System.out.println("visitExpression :: INVALID INSTANCE : WHAT IS " + e);
		assert false : "invalid instance";
		return null;
	}

	private AFA visitPempty(Expression e) {
		System.out.println("here is Pempty : " + e);

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

	private AFA visitPfail(Expression e) {
		System.out.println("here is Pfail : " + e);
		System.out.println("ERROR : WHAT IS Pfail : UNIMPLEMENTED FUNCTION");
		return null;
	}

	private AFA visitCany(Expression e) {
		System.out.println("here is Cany : " + e);

		int s = getNewStateID();
		int t = getNewStateID();
		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> transitions = new TreeSet<Transition>();
		State f = new State(s);
		HashSet<State> F = new HashSet<State>();
		HashSet<State> L = new HashSet<State>();

		S.add(new State(s));
		S.add(new State(t));

		transitions.add(new Transition(s, t, '.', -1));

		F.add(new State(t));

		return new AFA(S, transitions, f, F, L);
	}

	private AFA visitCbyte(Expression e) {
		System.out.println("here is Cbyte : " + e);

		int s = getNewStateID();
		int t = getNewStateID();
		HashSet<State> S = new HashSet<State>();
		TreeSet<Transition> transitions = new TreeSet<Transition>();
		State f = new State(s);
		HashSet<State> F = new HashSet<State>();
		HashSet<State> L = new HashSet<State>();

		S.add(new State(s));
		S.add(new State(t));

		transitions.add(new Transition(s, t, ((Cbyte) e).byteChar, -1));

		F.add(new State(t));

		return new AFA(S, transitions, f, F, L);
	}

	private AFA visitCset(Expression e) {
		System.out.println("here is Cset : " + e);

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
			if (((Cset) e).byteMap[i]) {
				transitions.add(new Transition(s, t, i, -1));
			}
		}

		F.add(new State(t));

		return new AFA(S, transitions, f, F, L);
	}

	private AFA visitPoption(Expression e) {
		System.out.println("here is Poption : " + e);
		return null;
	}

	private AFA visitPzero(Expression e) {
		System.out.println("here is Pzero : " + e);
		return null;
	}

	private AFA visitPone(Expression e) {
		System.out.println("here is Pone : " + e);
		return null;
	}

	private AFA visitPand(Expression e) {
		System.out.println("here is Pand : " + e);
		return null;
	}

	private AFA visitPnot(Expression e) {
		System.out.println("here is Pnot : " + e);
		return null;
	}

	private AFA visitPsequence(Expression e) {
		System.out.println("here is P : " + e);
		return null;
	}

	private AFA visitPchoice(Expression e) {
		System.out.println("here is Pchoice : " + e);
		return null;
	}

	private AFA visitNonTerminal(Expression e) {
		System.out.println("here is NonTerminal : " + e);
		String nonTerminalName = ((NonTerminal) e).getLocalName();

		if (initialStateOfNonTerminal.containsKey(nonTerminalName)) { // 既にこの非終端記号を訪れた場合
			int s = getNewStateID();
			int t = getNewStateID();
			State nonTerminal_s = initialStateOfNonTerminal.get(nonTerminalName);
			State nonTerminal_t = acceptingStateOfNonTerminal.get(nonTerminalName);
			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			S.add(new State(t));
			S.add(new State(nonTerminal_s.getID()));
			S.add(new State(nonTerminal_t.getID()));

			transitions.add(new Transition(s, nonTerminal_s.getID(), AFA.epsilon, -1));
			transitions.add(new Transition(nonTerminal_t.getID(), t, AFA.epsilon, -1));

			F.add(new State(t));

			return new AFA(S, transitions, f, F, L);
		} else {

			int s = getNewStateID();
			int t = getNewStateID();

			initialStateOfNonTerminal.put(nonTerminalName, new State(s));
			acceptingStateOfNonTerminal.put(nonTerminalName, new State(t));

			AFA tmpAfa = visitProduction(((NonTerminal) e).getProduction());

			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			S.add(new State(t));
			for (State state : tmpAfa.getS()) {
				S.add(new State(state.getID()));
			}

			transitions.add(new Transition(s, tmpAfa.getf().getID(), AFA.epsilon, -1));
			for (State state : tmpAfa.getF()) {
				transitions.add(new Transition(state.getID(), t, AFA.epsilon, -1));
			}
			for (Transition transition : tmpAfa.getTransitions()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			F.add(new State(t));

			for (State state : tmpAfa.getL()) {
				L.add(new State(state.getID()));
			}

			return new AFA(S, transitions, f, F, L);
		}
	}
}
