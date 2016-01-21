package nez.parser.vm;

import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserCode;
import nez.parser.ParserInstance;
import nez.parser.TerminationException;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.Verbose;

public class MozCode extends ParserCode<MozInst> {

	public MozCode(Grammar compiledGrammar) {
		super(compiledGrammar, new MozInst[1026]);
	}

	UList<MozInst> codeList() {
		return this.codeList;
	}

	@Override
	public Object exec(ParserInstance context) {
		long startPosition = context.getPosition();
		MozMachine machine = (MozMachine) context.getRuntime();
		MozInst code = this.getStartInstruction();
		boolean result = false;
		try {
			while (true) {
				code = code.execMoz(machine);
			}
		} catch (TerminationException e) {
			result = e.status;
		}
		return result ? machine.getParseResult(startPosition, context.getPosition()) : null;
	}

	public boolean run(MozInst code, MozMachine sc) {
		boolean result = false;
		String u = "Start";
		UList<String> stack = new UList<String>(new String[128]);
		stack.add("Start");
		try {
			while (true) {
				if (code instanceof Moz86.Call) {
					stack.add(u);
					u = ((Moz86.Call) code).getNonTerminalName();
				}
				if (code instanceof Moz86.Ret) {
					u = stack.ArrayValues[stack.size() - 1];
					stack.clear(stack.size() - 1);
				}
				ConsoleUtils.println(u + "(" + sc.getPosition() + ")  " + code.id + " " + code);
				MozInst code2 = code.execMoz(sc);
				if (code2 == null) {
					Verbose.debug("@@ returning null at " + code);
				}
				code = code2;
			}
		} catch (TerminationException e) {
			result = e.status;
		}
		return result;
	}

	@Override
	public void layoutCode(MozInst inst) {
		if (inst == null) {
			return;
		}
		if (inst.id == -1) {
			inst.id = codeList.size();
			codeList.add(inst);
			layoutCode(inst.next);
			// if (inst.next != null && inst.id + 1 != inst.next.id) {
			// MozInst.joinPoint(inst.next);
			// }
			layoutCode(inst.branch());
			if (inst instanceof Moz86.Dispatch) {
				Moz86.Dispatch match = (Moz86.Dispatch) inst;
				for (int ch = 0; ch < match.jumpTable.length; ch++) {
					layoutCode(match.jumpTable[ch]);
				}
			}
		}
	}

	public final void encode(MozWriter coder) {
		if (coder != null) {
			coder.setHeader(codeList.size(), this.getInstructionSize(), this.getMemoPointSize());
			coder.setInstructions(codeList.ArrayValues, codeList.size());
		}
	}

	public final static void writeMozCode(Parser parser, String path) {
		ParserMachineCompiler compile = ParserMachineCompiler.newCompiler(parser.getParserStrategy());
		MozCode code = compile.compile(parser.getParserGrammar());
		MozWriter c = new MozWriter();
		code.encode(c);
		Verbose.println("generating " + path);
		c.writeTo(path);
	}

}
