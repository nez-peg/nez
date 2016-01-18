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
				code = code.exec(machine);
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
				if (code instanceof Moz.Call) {
					stack.add(u);
					u = ((Moz.Call) code).getNonTerminalName();
				}
				if (code instanceof Moz.Ret) {
					u = stack.ArrayValues[stack.size() - 1];
					stack.clear(stack.size() - 1);
				}
				ConsoleUtils.println(u + "(" + sc.getPosition() + ")  " + code.id + " " + code);
				MozInst code2 = code.exec(sc);
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
			if (inst.next != null && inst.id + 1 != inst.next.id) {
				MozInst.labeling(inst.next);
			}
			layoutCode(inst.branch());
			if (inst instanceof Moz.First) {
				Moz.First match = (Moz.First) inst;
				for (int ch = 0; ch < match.jumpTable.length; ch++) {
					layoutCode(match.jumpTable[ch]);
				}
			}
		}
	}

	public final void encode(ByteCoder coder) {
		if (coder != null) {
			coder.setHeader(codeList.size(), this.getInstructionSize(), this.getMemoPointSize());
			coder.setInstructions(codeList.ArrayValues, codeList.size());
		}
	}

	public final static void writeMozCode(Parser parser, String path) {
		MozCompiler compile = MozCompiler.newCompiler(parser.getParserStrategy());
		MozCode code = compile.compile(parser.getParserGrammar());
		ByteCoder c = new ByteCoder();
		code.encode(c);
		Verbose.println("generating " + path);
		c.writeTo(path);
	}

}
