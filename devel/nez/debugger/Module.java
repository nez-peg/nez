package nez.debugger;

import java.util.ArrayList;
import java.util.List;

import nez.lang.Production;
import nez.parser.moz.ParserGrammar;
import nez.util.ConsoleUtils;

public class Module {
	List<Function> funcList;
	ParserGrammar g;

	public Module() {
		this.funcList = new ArrayList<Function>();
	}

	public void setGrammar(ParserGrammar g) {
		this.g = g;
	}

	public DebugVMInstruction getStartPoint() {
		Production start = this.g.getStartProduction();
		for (Function func : this.funcList) {
			if (func.funcName.equals(start.getLocalName())) {
				Inop nop = new Inop(start);
				BasicBlock bb = func.get(0);
				while (bb.size() == 0) {
					bb = bb.getSingleSuccessor();
				}
				nop.next = bb.get(0);
				return nop;
			}
		}
		ConsoleUtils.exit(1, "error: StartPoint is not found");
		return null;
	}

	public Function get(int index) {
		return this.funcList.get(index);
	}

	public Function get(String name) {
		for (Function func : this.funcList) {
			if (func.funcName.equals(name)) {
				return func;
			}
		}
		// ConsoleUtils.exit(1, "error: NonTerminal is not found " + name);
		return null;
	}

	public void append(Function func) {
		this.funcList.add(func);
	}

	public int size() {
		return funcList.size();
	}

	public String stringfy(StringBuilder sb) {
		for (int i = 0; i < size(); i++) {
			this.get(i).stringfy(sb);
		}
		return sb.toString();
	}
}
