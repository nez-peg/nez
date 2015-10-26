package nez.dfa;

import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import nez.Grammar;
import nez.dfa.AFAConverter.DefaultVisitor;
import nez.lang.Expression;
import nez.lang.Production;
import nez.util.VisitorMap;

/*
 * 
 * 謎現象 ... + が * と解釈される
 * 
 * 非終端記号ー制約：
 * 	非終端記号の初期状態と受理状態は１つ
 * 	複数存在する場合は新たな状態を作成し、それを初期（受理）状態とし、それにε遷移をはるようにする
 * 	
 */
public class AFAConverter extends VisitorMap<DefaultVisitor> {

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
		init(AFAConverter.class, new DefaultVisitor());
	}

	public void build() {
		Production p = grammar.getProduction(StartProduction);
		if (p == null) {
			System.out.println("ERROR : START PRODUCTION \"" + StartProduction + "\" ... NOT FOUND");
			System.out.println("BUILD FAILED");
			return;
		}
		this.afa = visitProduction(p);

		this.afa = eliminateEpsilonCycle(this.afa);
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

	private AFA visitProduction(Production rule) {
		// System.out.println("here is Production : " + rule.getLocalName());
		// return visitExpression(rule.getExpression());
		return visit(rule.getExpression());
	}

	public AFA visit(Expression e) {
		return find(e.getClass().getSimpleName()).accept(e);
	}

	// private AFA visitExpression(Expression e) {
	// if (e instanceof Pempty) {
	// return visitPempty(e);
	// } else if (e instanceof Pfail) {
	// return visitPfail(e);
	// } else if (e instanceof Cany) {
	// return visitCany(e);
	// } else if (e instanceof Cbyte) {
	// return visitCbyte(e);
	// } else if (e instanceof Cset) {
	// return visitCset(e);
	// } else if (e instanceof Poption) {
	// return visitPoption(e);
	// } else if (e instanceof Pzero) {
	// return visitPzero(e);
	// } else if (e instanceof Pone) {
	// return visitPone(e);
	// } else if (e instanceof Pand) {
	// return visitPand(e);
	// } else if (e instanceof Pnot) {
	// return visitPnot(e);
	// } else if (e instanceof Psequence) {
	// return visitPsequence(e);
	// } else if (e instanceof Pchoice) {
	// return visitPchoice(e);
	// } else if (e instanceof nez.lang.expr.NonTerminal) {
	// return visitNonTerminal(e);
	// }
	// // else if (e instanceof Cmulti) {
	// // return visitCmulti(e);
	// // } else if (e instanceof Tlink) {
	// // return visitTlink(e);
	// // } else if (e instanceof Tnew) {
	// // return visitTnew(e);
	// // } else if (e instanceof Tcapture) {
	// // return visitTcapture(e);
	// // } else if (e instanceof Treplace) {
	// // return visitTreplace(e);
	// // } else if (e instanceof Xblock) {
	// // return visitXblock(e);
	// // }
	// System.out.println("visitExpression :: INVALID INSTANCE : WHAT IS " + e);
	// assert false : "invalid instance";
	// return null;
	// }

	public class DefaultVisitor {
		public AFA accept(Expression e) {
			System.out.println("ERROR :: INVALID INSTANCE : WHAT IS " + e);
			return null;
		}
	}

	public class Pempty extends DefaultVisitor {

		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Pempty : " + e);

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

	public class Pfail extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Pfail : " + e);
			System.out.println("ERROR : WHAT IS Pfail : UNIMPLEMENTED FUNCTION");
			return null;
		}
	}

	public class Cany extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Cany : " + e);

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
	}

	public class Cbyte extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Cbyte : " + e);

			int s = getNewStateID();
			int t = getNewStateID();
			HashSet<State> S = new HashSet<State>();
			TreeSet<Transition> transitions = new TreeSet<Transition>();
			State f = new State(s);
			HashSet<State> F = new HashSet<State>();
			HashSet<State> L = new HashSet<State>();

			S.add(new State(s));
			S.add(new State(t));

			transitions.add(new Transition(s, t, ((nez.lang.expr.Cbyte) e).byteChar, -1));

			F.add(new State(t));

			return new AFA(S, transitions, f, F, L);
		}
	}

	public class Cset extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Cset : " + e);

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
				if (((nez.lang.expr.Cset) e).byteMap[i]) {
					transitions.add(new Transition(s, t, i, -1));
				}
			}

			F.add(new State(t));

			return new AFA(S, transitions, f, F, L);
		}
	}

	/*
	 * 正しく入力のファイルが変換されているのであればこの関数が呼び出されることはない 解析表現文法における e? は e / ε と変換され、 e /
	 * ε は e | !e ε と変換される
	 */
	public class Poption extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Poption : " + e);
			System.out.println("WARNING : option FOUND ... e? should be converted into ( e | !e ε )");

			// e? について、 e の AFA を tmpAfa として構築
			// AFA tmpAfa = visitExpression(e.get(0));
			AFA tmpAfa = visit(e.get(0));

			int s = getNewStateID();
			int t = getNewStateID();

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

			transitions.add(new Transition(s, t, AFA.epsilon, -1));
			transitions.add(new Transition(s, tmpAfa.getf().getID(), AFA.epsilon, -1));
			for (State state : tmpAfa.getF()) {
				transitions.add(new Transition(state.getID(), t, AFA.epsilon, -1));
			}
			for (Transition transition : tmpAfa.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			F.add(new State(t));

			for (State state : tmpAfa.getL()) {
				L.add(new State(state.getID()));
			}

			return new AFA(S, transitions, f, F, L);
		}
	}

	/*
	 * 正しく入力のファイルが変換されているのであればこの関数が呼び出されることはない 解析表現文法における e* は新たに非終端記号を追加し、 A <-
	 * eA / ε と変換され、 A <- eA | !(eA) ε となる
	 */
	public class Pzero extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Pzero : " + e);
			System.out.println("WARNING : zero or more FOUND ... e* should be converted into A <- ( eA | !(eA) ε )");

			// e* について、 e の AFA を tmpAfa として構築
			// AFA tmpAfa = visitExpression(e.get(0));
			AFA tmpAfa = visit(e.get(0));

			int s = getNewStateID();
			int t = getNewStateID();

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
			transitions.add(new Transition(s, t, AFA.epsilon, -1));
			for (State state : tmpAfa.getF()) {
				transitions.add(new Transition(state.getID(), tmpAfa.getf().getID(), AFA.epsilon, -1));
				transitions.add(new Transition(state.getID(), t, AFA.epsilon, -1));
			}
			for (Transition transition : tmpAfa.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			F.add(new State(t));

			for (State state : tmpAfa.getL()) {
				L.add(new State(state.getID()));
			}

			return new AFA(S, transitions, f, F, L);
		}
	}

	/*
	 * 正しく入力のファイルが変換されているのであればこの関数が呼び出されることはない 解析表現文法における e+ は ee* と変換され、eA
	 * (Aは先ほどのe*から得られたもの) となる
	 */
	public class Pone extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Pone : " + e);
			System.out.println("WARNING : zero or more FOUND ... e+ should be converted into eA where A <- eA | !(eA) ε ");
			// e+ について、 e の AFA を tmpAfa として構築
			// AFA tmpAfa = visitExpression(e.get(0));
			AFA tmpAfa = visit(e.get(0));

			int s = getNewStateID();
			int t = getNewStateID();

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
				transitions.add(new Transition(state.getID(), tmpAfa.getf().getID(), AFA.epsilon, -1));
				transitions.add(new Transition(state.getID(), t, AFA.epsilon, -1));
			}
			for (Transition transition : tmpAfa.getTau()) {
				transitions.add(new Transition(transition.getSrc(), transition.getDst(), transition.getLabel(), transition.getPredicate()));
			}

			F.add(new State(t));

			for (State state : tmpAfa.getL()) {
				L.add(new State(state.getID()));
			}

			return new AFA(S, transitions, f, F, L);
		}
	}

	public class Pand extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Pand : " + e);

			// AFA tmpAfa = visitExpression(e.get(0));
			AFA tmpAfa = visit(e.get(0));
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

			return new AFA(S, transitions, f, F, L);
		}
	}

	public class Pnot extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Pnot : " + e);

			// AFA tmpAfa = visitExpression(e.get(0));
			AFA tmpAfa = visit(e.get(0));
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

			return new AFA(S, transitions, f, F, L);
		}
	}

	public class Psequence extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Psequence : " + e);

			// AFA tmpAfa1 = visitExpression(e.getFirst());
			// AFA tmpAfa2 = visitExpression(e.getNext());
			AFA tmpAfa1 = visit(e.getFirst());
			AFA tmpAfa2 = visit(e.getNext());

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

	public class Pchoice extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is Pchoice : " + e);

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
				AFA tmpAfa = visit(e.get(i));

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

	public class NonTerminal extends DefaultVisitor {
		@Override
		public AFA accept(Expression e) {
			// System.out.println("here is NonTerminal : " + e);
			// String nonTerminalName = ((NonTerminal) e).getLocalName();
			String nonTerminalName = ((nez.lang.expr.NonTerminal) e).getLocalName();

			if (initialStateOfNonTerminal.containsKey(nonTerminalName)) { // 既にこの非終端記号を訪れた場合
				/*
				 * t は消しても良いと思うので要検討 tから戻ってきてもその先は空文字と等価なはずなのでも戻ってくる辺を追加する必要はない
				 * 戻る辺があると通常の遷移からこっちへ遷移してこれることになる（結局行き先は空文字と等価な状態なのだが） 正しくは t
				 * は存在しないはず
				 */
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

				AFA tmpAfa = visitProduction(((nez.lang.expr.NonTerminal) e).getProduction());

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
				for (Transition transition : tmpAfa.getTau()) {
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
}
