package nez.debugger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import jline.ArgumentCompletor;
import jline.Completor;
import jline.ConsoleReader;
import jline.MultiCompletor;
import jline.SimpleCompletor;
import nez.ast.CommonTree;
import nez.debugger.Context.FailOverInfo;
import nez.lang.Expression;
import nez.lang.Nez;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.main.ReadLine;
import nez.parser.moz.ParserGrammar;
import nez.util.ConsoleUtils;

public class NezDebugger {
	HashMap<String, BreakPoint> breakPointMap = new HashMap<String, BreakPoint>();
	HashMap<Nez.Choice, ChoicePoint> choicePointMap = new HashMap<Nez.Choice, ChoicePoint>();
	HashMap<String, Production> ruleMap = new HashMap<String, Production>();
	List<String> nameList = new ArrayList<String>();
	DebugOperator command = null;
	ParserGrammar peg;
	DebugVMCompiler compiler;
	Module module;
	DebugVMInstruction code;
	DebugSourceContext sc;
	String text = null;
	int linenum = 0;
	boolean running = false;
	ConsoleReader cr;

	public NezDebugger(ParserGrammar peg, DebugVMInstruction code, DebugSourceContext sc, DebugVMCompiler c) {
		this.peg = peg;
		this.code = code;
		this.sc = sc;
		this.compiler = c;
		this.module = c.getModule();
		for (Production p : peg) {
			this.ruleMap.put(p.getLocalName(), p);
			this.nameList.add(p.getLocalName());
		}
		ReadLine.addCompleter(this.nameList);
		try {
			this.cr = new ConsoleReader();
			Completor[] br = { new SimpleCompletor(new String[] { "b", "break", "goto", "reach" }), new SimpleCompletor(this.nameList.toArray(new String[this.nameList.size()])) };
			ArgumentCompletor abr = new ArgumentCompletor(br);
			ArrayList<String> printlist = new ArrayList<String>();
			printlist.addAll(this.nameList);
			printlist.addAll(Arrays.asList("-pos", "-node", "-pr", "-call"));
			Completor[] print = { new SimpleCompletor(new String[] { "p", "print" }), new SimpleCompletor(printlist.toArray(new String[printlist.size()])) };
			ArgumentCompletor apr = new ArgumentCompletor(print);
			Completor[] bt = { new SimpleCompletor(new String[] { "bt" }), new SimpleCompletor("-l") };
			ArgumentCompletor abt = new ArgumentCompletor(bt);
			ArgumentCompletor commands = new ArgumentCompletor(new SimpleCompletor(new String[] { "n", "s", "f", "c", "r", "q", "exit", "h", "start", "consume", "fover" }));
			MultiCompletor mc = new MultiCompletor(new ArgumentCompletor[] { abr, apr, abt, commands });
			this.cr.addCompletor(mc);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class BreakPoint {
		Production pr;
		Integer id;

		public BreakPoint(Production pr, int id) {
			this.pr = pr;
			this.id = id;
		}
	}

	class ChoicePoint {
		Nez.Choice e;
		int index;
		boolean first;
		boolean succ;

		public ChoicePoint(Nez.Choice e, int index) {
			this.e = e;
			this.index = index;
			this.first = true;
			this.succ = false;
		}
	}

	// public boolean exec(DebugVMInstruction code, DebugSourceContext sc) {
	// boolean result = false;
	// try {
	// while(true) {
	// code = code.exec(sc);
	// }
	// } catch (MachineExitException e) {
	// result = e.result;
	// }
	// return result;
	// }

	public boolean exec() {
		boolean result = false;
		showCurrentExpression();
		try {
			while (true) {
				readLine("(nezdb) ");
				command.exec(this);
				if (code instanceof Iexit) {
					code.exec(sc);
				}
				showCurrentExpression();
			}
		} catch (MachineExitException e) {
			result = e.result;
			if (!result) {
				if (this.sc.longestTrace != null) {
					for (int i = 1; i <= this.sc.longestStackTop; i++) {
						NonTerminal ne = (NonTerminal) this.sc.longestTrace[i].val;
						long pos = this.sc.longestTrace[i].pos;
						CommonTree tree = (CommonTree) ne.getSourceLocation();
						ConsoleUtils.println("[" + i + "] " + ne.getLocalName() + " (" + tree.getSource().linenum(tree.getSourcePosition()) + ")");
						ConsoleUtils.println(sc.formatDebugPositionLine(pos, ""));
					}
				}
				ConsoleUtils.println(sc.formatDebugPositionLine(this.sc.longest_pos, "[longest]"));
				ConsoleUtils.println(sc.formatDebugPositionLine(this.sc.pos, "[last]"));
			} else {
				for (int i = 0; i < this.sc.failOverList.size(); i++) {
					FailOverInfo fover = this.sc.failOverList.get(i);
					ConsoleUtils.println("Code Layout bug " + i + ":");
					ConsoleUtils.println("Expression: " + fover.e.getSourceLocation().formatSourceMessage("", ""));
					ConsoleUtils.println("Input: " + this.sc.formatDebugPositionLine(fover.fail_pos, ""));
				}
			}
		}
		return result;
	}

	public boolean execCode() throws MachineExitException {
		if (this.code instanceof Icall) {
			if (this.breakPointMap.containsKey(((Icall) this.code).ne.getLocalName())) {
				this.code = this.code.exec(this.sc);
				return false;
			}
		}
		if (this.code instanceof Ialtstart) {
			this.code = this.code.exec(this.sc);
		}
		this.code = this.code.exec(this.sc);
		if (this.code instanceof AltInstruction) {
			if (this.code instanceof Ialtend) {
				this.execUnreachableChoiceCheck();
			}
			this.code = this.code.exec(this.sc);
		}
		return true;
	}

	// FIXME
	public void execUnreachableChoiceCheck() throws MachineExitException {
		ChoicePoint point = this.choicePointMap.get(((Ialtend) (this.code)).c);
		while (true) {
			this.code = this.code.exec(this.sc);
			if (point == null || ((this.code instanceof Ialtfin) && this.code.expr.equals(point.e))) {
				break;
			}
		}
	}

	public void showCurrentExpression() {
		Expression e = null;
		if (code instanceof Icall) {
			e = ((Icall) code).ne;
		} else if (code instanceof Ialtstart) {
			e = code.expr.get(0);
		} else if (code instanceof Inop) {
			if (running) {
				ConsoleUtils.println(((Inop) code).p.getExpression().getSourceLocation().formatSourceMessage("debug", ""));
				return;
			}
		} else {
			e = code.getExpression();
		}
		if (running && e != null) {
			ConsoleUtils.println(e.getSourceLocation().formatSourceMessage("debug", ""));
		} else if (running && e == null) {
			ConsoleUtils.println("e = null");
		}
	}

	public void showDebugUsage() {
		ConsoleUtils.println("Nez Debugger support following commands:");
		ConsoleUtils.println("  p | print [-ctx field? | ProductionName]  Print");
		ConsoleUtils.println("  b | break [ProductionName]                BreakPoint");
		ConsoleUtils.println("  n                                         StepOver");
		ConsoleUtils.println("  s                                         StepIn");
		ConsoleUtils.println("  f | finish                                StepOut");
		ConsoleUtils.println("  c                                         Continue");
		ConsoleUtils.println("  r | run                                   Run");
		ConsoleUtils.println("  q | exit                                  Exit");
		ConsoleUtils.println("  h | help                                  Help");
	}

	private void readLine(String prompt) {
		while (true) {
			try {
				String line = this.cr.readLine(prompt);
				if (line == null || line.equals("")) {
					if (this.command == null) {
						continue;
					}
					return;
				}
				this.cr.getHistory().addToHistory(line);
				linenum++;
				String[] tokens = line.split("\\s+");
				String command = tokens[0];
				int pos = 1;
				if (command.equals("p") || command.equals("print")) {
					Print p = new Print();
					if (tokens.length < 2) {
						this.showDebugUsage();
						return;
					}
					if (tokens[pos].startsWith("-")) {
						if (tokens[pos].equals("-pos")) {
							p.setType(Print.printPos);
						} else if (tokens[pos].equals("-node")) {
							p.setType(Print.printNode);
						} else if (tokens[pos].equals("-pr")) {
							p.setType(Print.printProduction);
						} else if (tokens[pos].equals("-call")) {
							p.setType(Print.printCallers);
						}
						pos++;
					}
					if (pos < tokens.length) {
						p.setCode(tokens[pos]);
					}
					this.command = p;
					return;
				} else if (command.equals("bt")) {
					if (!running) {
						ConsoleUtils.println("error: invalid process");
					} else {
						this.command = new BackTrace();
						if (tokens.length != 1) {
							if (tokens[pos].equals("-l")) {
								((BackTrace) this.command).setType(BackTrace.longestTrace);
							}
						}
						return;
					}
				} else if (command.equals("b") || command.equals("break")) {
					this.command = new Break();
					if (tokens.length < 2) {
						return;
					}
					this.command.setCode(tokens[pos]);
					return;
				} else if (command.equals("n")) {
					if (!running) {
						ConsoleUtils.println("error: invalid process");
					} else {
						this.command = new StepOver();
						return;
					}
				} else if (command.equals("s")) {
					if (!running) {
						ConsoleUtils.println("error: invalid process");
					} else {
						this.command = new StepIn();
						return;
					}
				} else if (command.equals("f") || command.equals("finish")) {
					if (!running) {
						ConsoleUtils.println("error: invalid process");
					} else {
						this.command = new StepOut();
						return;
					}
				} else if (command.equals("c")) {
					if (!running) {
						ConsoleUtils.println("error: invalid process");
					} else {
						this.command = new Continue();
						return;
					}
				} else if (command.equals("r") || command.equals("run")) {
					if (!running) {
						this.command = new Run();
						running = true;
						return;
					} else {
						ConsoleUtils.println("error: now running");
					}
				} else if (command.equals("q") || command.equals("exit")) {
					this.command = new Exit();
					return;
				} else if (command.equals("start")) {
					if (tokens[pos].equals("pos")) {
						pos++;
						this.command = new StartPosition(Long.parseLong(tokens[pos]));
					}
					return;
				} else if (command.equals("consume")) {
					this.command = new Consume(Long.parseLong(tokens[pos]));
					return;
				} else if (command.equals("goto")) {
					this.command = new Goto(tokens[pos]);
					return;
				} else if (command.equals("reach")) {
					if (tokens.length < 3) {
						System.out.println("error: this command is required 2 argument(name, path)");
						return;
					}
					this.command = new Reachable(tokens[pos], tokens[pos + 1]);
					return;
				} else if (command.equals("fover")) {
					if (this.sc.failOver) {
						this.sc.failOver = false;
						System.out.println("finish FailOver mode");
					} else {
						this.sc.failOver = true;
						System.out.println("start FailOver mode");
					}
				} else if (command.equals("h") || command.equals("help")) {
					this.showDebugUsage();
				} else {
					ConsoleUtils.println("command not found: " + command);
					this.showDebugUsage();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public boolean exec(Print o) {
		if (o.type == Print.printPos || o.type == Print.printNode) {
			Context ctx = sc;
			// if (o.code == null) {
			// ConsoleUtils.println("context {");
			// ConsoleUtils.println(" input_name = " + ctx.getResourceName());
			// ConsoleUtils.println(" pos = " + ctx.getPosition());
			// Object obj = ctx.getLeftObject();
			// if (obj == null) {
			// ConsoleUtils.println(" left = " + ctx.getLeftObject());
			// } else {
			// ConsoleUtils.println(" left = " +
			// ctx.getLeftObject().hashCode());
			// }
			// ConsoleUtils.println("}");
			// } else if (o.code.equals("pos")) {
			// ConsoleUtils.println("pos = " + ctx.getPosition());
			// ConsoleUtils.println(sc.formatDebugPositionLine(((Context)
			// sc).getPosition(), ""));
			// } else if (o.code.equals("input_name")) {
			// ConsoleUtils.println("input_name = " + ctx.getResourceName());
			// } else if (o.code.equals("left")) {
			// ConsoleUtils.println("left = " + ctx.getLeftObject());
			// } else {
			// ConsoleUtils.println("error: no member nameed \'" + o.code + "\'
			// in context");
			// }
			if (o.type == Print.printPos) {
				ConsoleUtils.println("pos = " + ctx.getPosition());
				ConsoleUtils.println(sc.formatDebugPositionLine(((Context) sc).getPosition(), ""));
			} else if (o.type == Print.printNode) {
				ConsoleUtils.println("left = " + ctx.getLeftObject());
			}
		} else if (o.type == Print.printProduction) {
			Production rule = ruleMap.get(o.code);
			if (rule != null) {
				ConsoleUtils.println(rule.getLocalName());
				ConsoleUtils.println(rule.getExpression().getSourceLocation()); // FIXME
																				// debug
																				// message
			} else {
				ConsoleUtils.println("error: production not found '" + o.code + "'");
			}
		} else if (o.type == Print.printCallers) {
			Function f = this.module.get(o.code);
			if (f != null) {
				ConsoleUtils.println(f.funcName + " >>");
				for (int i = 0; i < f.callers.size(); i++) {
					ConsoleUtils.println("  [" + i + "] " + f.callers.get(i).funcName);
				}
			} else {
				ConsoleUtils.println("error: production not found '" + o.code + "'");
			}
		}
		return true;
	}

	public boolean exec(BackTrace o) {
		if (o.getType() == BackTrace.longestTrace) {
			if (this.sc.longestTrace != null) {
				for (int i = 1; i <= this.sc.longestStackTop; i++) {
					NonTerminal ne = (NonTerminal) this.sc.longestTrace[i].val;
					CommonTree tree = (CommonTree) ne.getSourceLocation();
					ConsoleUtils.println("[" + i + "] " + ne.getLocalName() + " (" + tree.getSource().linenum(tree.getSourcePosition()) + ")");
					ConsoleUtils.println(sc.formatDebugPositionLine(this.sc.longestTrace[i].pos, ""));
				}
			} else {
				ConsoleUtils.println("backtracking has not occurred");
			}
		} else {
			for (int i = 1; i <= this.sc.callStackTop; i++) {
				NonTerminal ne = (NonTerminal) this.sc.callStack[i].val;
				CommonTree tree = (CommonTree) ne.getSourceLocation();
				ConsoleUtils.println("[" + i + "] " + ne.getLocalName() + " (" + tree.getSource().linenum(tree.getSourcePosition()) + ")");
				ConsoleUtils.println(sc.formatDebugPositionLine(this.sc.callStack[i].pos, ""));
			}
		}
		return true;
	}

	public boolean exec(Break o) {
		if (this.command.code != null) {
			Production rule = ruleMap.get(this.command.code);
			if (rule != null) {
				this.breakPointMap.put(rule.getLocalName(), new BreakPoint(rule, this.breakPointMap.size() + 1));
				ConsoleUtils.println("breakpoint " + (this.breakPointMap.size()) + ": where = " + rule.getLocalName() + " " /*
																															 * FIXME
																															 * +
																															 * (
																															 * rule
																															 * .
																															 * getExpression
																															 * (
																															 * )
																															 * .
																															 * getSourcePosition
																															 * (
																															 * )
																															 * )
																															 * .
																															 * formatDebugSourceMessage
																															 * (
																															 * ""
																															 * )
																															 */);
			} else {
				ConsoleUtils.println("production not found");
			}
		} else {
			this.showBreakPointList();
		}
		return true;
	}

	public void showBreakPointList() {
		if (this.breakPointMap.isEmpty()) {
			ConsoleUtils.println("No breakpoints currently set");
		} else {
			List<Entry<String, BreakPoint>> mapValuesList = new ArrayList<>(this.breakPointMap.entrySet());
			Collections.sort(mapValuesList, new Comparator<Entry<String, BreakPoint>>() {
				@Override
				public int compare(Entry<String, BreakPoint> entry1, Entry<String, BreakPoint> entry2) {
					return (entry1.getValue().id).compareTo(entry2.getValue().id);
				}
			});
			for (Entry<String, BreakPoint> s : mapValuesList) {
				BreakPoint br = s.getValue();
				Production rule = (br.pr);
				ConsoleUtils.println(br.id + ": " + rule.getLocalName() + " " /*
																			 * +
																			 * FIXME
																			 * rule
																			 * .
																			 * getExpression
																			 * (
																			 * )
																			 * .
																			 * getSourcePosition
																			 * (
																			 * )
																			 * .
																			 * formatDebugSourceMessage
																			 * (
																			 * ""
																			 * )
																			 */);
			}
		}
	}

	public boolean exec(StepOver o) throws MachineExitException {
		if (this.code.getExpression() instanceof NonTerminal) {
			while (!this.code.op.equals(Opcode.Icall)) {
				this.execCode();
			}
			this.execCode();
			int stackTop = this.sc.callStackTop;
			while (stackTop <= this.sc.callStackTop) {
				if (!this.execCode()) {
					break;
				}
			}
		} else {
			if (this.code instanceof AltInstruction) {
				this.code = this.code.exec(this.sc);
			}
			Expression e = this.code.getExpression();
			Expression current = this.code.getExpression();
			while (e.equals(current)) {
				this.execCode();
				current = this.code.getExpression();
			}
		}
		if (this.code.op.equals(Opcode.Iret)) {
			this.code = this.code.exec(this.sc);
		}
		return true;
	}

	public boolean exec(StepIn o) throws MachineExitException {
		Expression e = this.code.getExpression();
		Expression current = this.code.getExpression();
		while (e.equals(current)) {
			this.execCode();
			current = this.code.getExpression();
		}
		if (this.code.op.equals(Opcode.Iret)) {
			this.code = this.code.exec(this.sc);
		}
		return true;
	}

	public boolean exec(StepOut o) throws MachineExitException {
		int stackTop = this.sc.callStackTop;
		while (stackTop <= this.sc.callStackTop) {
			if (!this.execCode()) {
				break;
			}
		}
		return true;
	}

	public boolean exec(Continue o) throws MachineExitException {
		while (true) {
			if (!this.execCode()) {
				break;
			}
		}
		return true;
	}

	public boolean exec(Run o) throws MachineExitException {
		// Production p = (Production) this.code.getExpression();
		// if (this.breakPointMap.containsKey(p.getLocalName())) {
		// this.code = this.code.exec(this.sc);
		// return false;
		// } FIXME
		while (true) {
			if (!this.execCode()) {
				return true;
			}
		}
	}

	public boolean exec(Exit o) {
		ConsoleUtils.exit(0, "debugger (status=0)");
		return false;
	}

	public boolean exec(StartPosition o) {
		this.sc.pos = o.pos;
		ConsoleUtils.println("set start position: " + this.sc.pos);
		return true;
	}

	public boolean exec(Consume o) throws MachineExitException {
		while (this.sc.pos < o.pos) {
			this.code = this.code.exec(this.sc);
		}
		ConsoleUtils.println("current position: " + this.sc.pos);
		return true;
	}

	public boolean exec(Goto o) throws MachineExitException {
		while (true) {
			if (this.code instanceof Icall) {
				if ((((Icall) this.code).ne.getLocalName()).equals(o.name)) {
					this.code = this.code.exec(this.sc);
					return false;
				}
			}
			this.code = this.code.exec(this.sc);
		}
	}

	public boolean exec(Reachable o) {
		Production rule = this.ruleMap.get(o.name);
		Expression e = rule.getExpression();
		Expression prev = null;
		String[] tokens = o.path.split("\\.");
		int index = 0;
		for (int i = 0; i < tokens.length; i++) {
			String indexString = tokens[i];
			index = Integer.parseInt(indexString.substring(1, indexString.length())) - 1;
			prev = e;
			e = e.get(index);
		}
		if (prev instanceof Nez.Choice) {
			if (index < 0) {
				System.out.println("error: set number is unexpected");
				return true;
			}
			ChoicePoint c = new ChoicePoint((Nez.Choice) prev, index);
			this.choicePointMap.put((Nez.Choice) prev, c);
			Alt alt = new Alt(index, this.compiler.altInstructionMap.get(prev.get(index)));
			this.sc.altJumpMap.put(prev, alt);
			ConsoleUtils.print("check unreachable choice " + this.choicePointMap.size() + ": where = ");
			ConsoleUtils.println(e.getSourceLocation().formatSourceMessage("reach", ""));
		} else {
			System.out.println("error");
		}
		return true;
	}

}
