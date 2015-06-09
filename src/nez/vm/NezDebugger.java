package nez.vm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import nez.SourceContext;
import nez.lang.Expression;
import nez.lang.Grammar;
import nez.lang.Link;
import nez.lang.NonTerminal;
import nez.lang.Production;
import nez.util.ConsoleUtils;

public class NezDebugger {
	HashMap<String, BreakPoint> breakPointMap = new HashMap<String, BreakPoint>();
	HashMap<String, Production> ruleMap = new HashMap<String, Production>();
	NezDebugOperator command = null;
	SourceContext sc = null;
	Grammar peg = null;
	Instruction code = null;
	String text = null;
	int linenum = 0;
	boolean running = false;

	public NezDebugger(Grammar peg, Instruction code, SourceContext sc) {
		this.peg = peg;
		this.code = code;
		this.sc = sc;
		for(Production p : peg.getProductionList()) {
			ruleMap.put(p.getLocalName(), p);
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

	public Instruction exec_code() throws TerminationException {
		return code.exec(sc);
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

	public boolean exec() {
		boolean result = false;
		showCurrentExpression();
		try {
			while (true) {
				readLine("(nezdb) ");
				command.exec(this);
				if(code instanceof IExit) {
					code.exec(sc);
				}
				showCurrentExpression();
			}
		} catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}

	Expression current = null;

	public void showCurrentExpression() {
		Expression e = null;
		if(code instanceof ICallPush) {
			e = ((ICallPush) code).ne;
		}
		else {
			e = code.getExpression();
		}
		if(running && e != null) {
			if(e.getSourcePosition() == null) {
				ConsoleUtils.println(e.toString());
			}
			else {
				ConsoleUtils.println(e.getSourcePosition().formatSourceMessage("debug", ""));
			}
			current = e;
		}
		else if(e == null) {
			ConsoleUtils.println("e = null");
		}
	}

	private void readLine(String prompt) {
		while (true) {
			Object console = ConsoleUtils.getConsoleReader();
			String line = ConsoleUtils.readSingleLine(console, prompt);
			if(line == null || line.equals("")) {
				if(this.command == null) {
					continue;
				}
				return;
			}
			String[] tokens = line.split("\\s+");
			String command = tokens[0];
			int pos = 1;
			if(command.equals("p") || command.equals("print")) {
				Print p = new Print();
				if(tokens.length < 2) {
					this.showDebugUsage();
					return;
				}
				if(tokens[pos].startsWith("-")) {
					if(tokens[pos].equals("-ctx")) {
						p.setType(Print.printContext);
					}
					else if(tokens[pos].equals("-pr")) {
						p.setType(Print.printProduction);
					}
					pos++;
				}
				if(pos < tokens.length) {
					p.setCode(tokens[pos]);
				}
				this.command = p;
				return;
			}
			else if(command.equals("b") || command.equals("break")) {
				this.command = new Break();
				if(tokens.length < 2) {
					return;
				}
				this.command.setCode(tokens[pos]);
				return;
			}
			else if(command.equals("n")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				}
				else {
					this.command = new StepOver();
					return;
				}
			}
			else if(command.equals("s")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				}
				else {
					this.command = new StepIn();
					return;
				}
			}
			else if(command.equals("f") || command.equals("finish")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				}
				else {
					this.command = new StepOut();
					return;
				}
			}
			else if(command.equals("c")) {
				if(!running) {
					ConsoleUtils.println("error: invalid process");
				}
				else {
					this.command = new Continue();
					return;
				}
			}
			else if(command.equals("r") || command.equals("run")) {
				if(!running) {
					this.command = new Run();
					running = true;
					return;
				}
				else {
					ConsoleUtils.println("error: now running");
				}
			}
			else if(command.equals("q") || command.equals("exit")) {
				this.command = new Exit();
				return;
			}
			else if(command.equals("h") || command.equals("help")) {
				this.showDebugUsage();
			}
			else {
				ConsoleUtils.println("command not found: " + command);
				this.showDebugUsage();
			}
			ConsoleUtils.addHistory(console, line);
			linenum++;
		}
	}

	public boolean exec(Print o) {
		if(o.type == Print.printContext) {
			Context ctx = (Context) sc;
			if(o.code == null) {
				ConsoleUtils.println("context {");
				ConsoleUtils.println("  input_name = " + ctx.getResourceName());
				ConsoleUtils.println("  pos = " + ctx.getPosition());
				Object obj = ctx.getParsingObject();
				if(obj == null) {
					ConsoleUtils.println("  left = " + ctx.getParsingObject());
				}
				else {
					ConsoleUtils.println("  left = " + ctx.getParsingObject().hashCode());
				}
				ConsoleUtils.println("}");
			}
			else if(o.code.equals("pos")) {
				ConsoleUtils.println("pos = " + ctx.getPosition());
				ConsoleUtils.println(sc.formatDebugPositionLine(((Context) sc).getPosition(), ""));
			}
			else if(o.code.equals("input_name")) {
				ConsoleUtils.println("input_name = " + ctx.getResourceName());
			}
			else if(o.code.equals("left")) {
				ConsoleUtils.println("left = " + ctx.getParsingObject());
			}
			else {
				ConsoleUtils.println("error: no member nameed \'" + o.code + "\' in context");
			}
		}
		else if(o.type == Print.printProduction) {
			Production rule = ruleMap.get(o.code);
			if(rule != null) {
				ConsoleUtils.println(rule.toString());
			}
			else {
				ConsoleUtils.println("error: production not found '" + o.code + "'");
			}
		}
		return true;
	}

	public boolean exec(Break o) {
		if(this.command.code != null) {
			Production rule = ruleMap.get(this.command.code);
			if(rule != null) {
				this.breakPointMap.put(rule.getLocalName(), new BreakPoint(rule, this.breakPointMap.size() + 1));
				ConsoleUtils.println("breakpoint " + (this.breakPointMap.size()) + ": where = " + rule.getLocalName() + " "
						+ rule.getSourcePosition().formatDebugSourceMessage(""));
			}
			else {
				ConsoleUtils.println("production not found");
			}
		}
		else {
			this.showBreakPointList();
		}
		return true;
	}

	public void showBreakPointList() {
		if(this.breakPointMap.isEmpty()) {
			ConsoleUtils.println("No breakpoints currently set");
		}
		else {
			List<Map.Entry> mapValuesList = new ArrayList<Map.Entry>(this.breakPointMap.entrySet());
			Collections.sort(mapValuesList, new Comparator<Map.Entry>() {
				@Override
				public int compare(Entry entry1, Entry entry2) {
					return (((BreakPoint) entry1.getValue()).id).compareTo(((BreakPoint) entry2.getValue()).id);
				}
			});
			for(Entry s : mapValuesList) {
				BreakPoint br = (BreakPoint) s.getValue();
				Production rule = (br.pr);
				ConsoleUtils.println(br.id + ": " + rule.getLocalName() + " " + rule.getSourcePosition().formatDebugSourceMessage(""));
			}
		}
	}

	public boolean exec(StepOver o) throws TerminationException {
		Expression e = code.getExpression();
		Expression current = code.getExpression();
		if(e instanceof NonTerminal) {
			code = exec_code();
			int stackTop = ((Context) sc).getUsedStackTop();
			while (stackTop <= ((Context) sc).getUsedStackTop()) {
				code = exec_code();
				current = code.getExpression();
			}
		}
		else if(e instanceof Production) {
			int stackTop = ((Context) sc).getUsedStackTop();
			while (stackTop <= ((Context) sc).getUsedStackTop()) {
				code = exec_code();
				current = code.getExpression();
			}
		}
		else if(e instanceof Link) {
			code = exec_code();
			int stackTop = ((Context) sc).getUsedStackTop();
			if(code.getExpression() instanceof Production) {
				code = exec_code();
				while (stackTop <= ((Context) sc).getUsedStackTop()) {
					code = exec_code();
					current = code.getExpression();
				}
			}
		}
		else {
			while (e.getId() == current.getId()) {
				code = exec_code();
				current = code.getExpression();
			}
		}
		while ((current instanceof Production) && code instanceof IRet) {
			code = exec_code();
			current = code.getExpression();
		}
		return true;
	}

	public boolean exec(StepIn o) throws TerminationException {
		Expression e = code.getExpression();
		Expression current = code.getExpression();
		if(e instanceof NonTerminal) {
			code = exec_code();
			current = code.getExpression();
			while ((current instanceof Production)) {
				code = exec_code();
				current = code.getExpression();
			}
		}
		else if(e instanceof Link) {
			code = exec_code();
			if(code.getExpression() instanceof Production) {
				code = exec_code();
			}
		}
		else {
			while (e.getId() == current.getId()) {
				code = exec_code();
				current = code.getExpression();
			}
		}
		while ((current instanceof Production) && code instanceof IRet) {
			code = exec_code();
			current = code.getExpression();
		}
		return true;
	}

	public boolean exec(StepOut o) throws TerminationException {
		Expression current = code.getExpression();
		int stackTop = ((Context) sc).getUsedStackTop();
		code = exec_code();
		while (stackTop <= ((Context) sc).getUsedStackTop()) {
			code = exec_code();
			current = code.getExpression();
		}
		while ((current instanceof Production)) {
			code = exec_code();
			current = code.getExpression();
		}
		return true;
	}

	public boolean exec(Continue o) throws TerminationException {
		while (true) {
			Expression e = code.getExpression();
			if(e instanceof Production && code instanceof ICallPush) {
				if(this.breakPointMap.containsKey(((Production) e).getLocalName())) {
					code = exec_code();
					return true;
				}
			}
			code = exec_code();
			current = code.getExpression();
		}
	}

	public boolean exec(Run o) throws TerminationException {
		while (true) {
			Expression e = code.getExpression();
			if(e instanceof Production && code instanceof ICallPush) {
				if(this.breakPointMap.containsKey(((Production) e).getLocalName())) {
					code = exec_code();
					return true;
				}
			}
			code = exec_code();
			current = code.getExpression();
		}
	}

	public boolean exec(Exit o) {
		ConsoleUtils.exit(0, "debugger (status=0)");
		return false;
	}
}
