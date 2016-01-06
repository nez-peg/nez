package nez.parser.moz;

import java.util.List;

import nez.lang.Grammar;
import nez.parser.ByteCoder;
import nez.parser.MemoPoint;
import nez.parser.Parser;
import nez.parser.ParserCode;
import nez.parser.ParserContext;
import nez.parser.TerminationException;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.Verbose;

public class MozCode extends ParserCode<MozInst> {

	public MozCode(Grammar gg, UList<MozInst> codeList, List<MemoPoint> memoPointList) {
		super(gg, new MozInst[0]);
		this.codeList = codeList;
	}

	@Override
	public Object exec(ParserContext context) {
		long startPosition = context.getPosition();
		MozMachine machine = (MozMachine) context.getRuntime();
		MozInst code = this.getStartPoint();
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
		// TODO Auto-generated method stub

	}

	public final void encode(ByteCoder coder) {
		if (coder != null) {
			coder.setHeader(codeList.size(), this.getInstSize(), this.getMemoPointSize());
			coder.setInstructions(codeList.ArrayValues, codeList.size());
		}
	}

	public final static void writeMozCode(Parser parser, String path) {
		MozCompiler compile = MozCompiler.newCompiler(parser.getParserStrategy());
		MozCode code = compile.compile((ParserGrammar) parser.getParserGrammar());
		ByteCoder c = new ByteCoder();
		code.encode(c);
		Verbose.println("generating " + path);
		c.writeTo(path);
	}

}
