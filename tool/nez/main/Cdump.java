package nez.main;

import java.io.IOException;
import java.util.Map;

import nez.lang.ByteConsumption;
import nez.lang.Grammar;
import nez.lang.Production;
import nez.lang.Productions;
import nez.parser.Parser;
import nez.util.ConsoleUtils;

public class Cdump extends Command {

	@Override
	public void exec() throws IOException {
		if (!hasInputSource()) {
			// Grammar g0 = newGrammar();
			// ConsoleUtils.println("Specified grammar");
			// dumpGrammar(g0);
			Parser p = newParser();
			Grammar g1 = p.getCompiledGrammar();
			ConsoleUtils.println("Compiled grammar");
			dumpGrammar(g1);
		}
	}

	private void dumpGrammar(Grammar grammar) throws IOException {
		final ByteConsumption consumed = new ByteConsumption();
		final Map<String, Integer> refCounts = Productions.countNonterminalReference(grammar);
		for (Production p : grammar) {
			String uname = p.getUniqueName();
			ConsoleUtils.println(uname);
			ConsoleUtils.println("  expression : " + p.getExpression());
			ConsoleUtils.println("  recursion  : " + Productions.isRecursive(p));
			ConsoleUtils.println("  consumed   : " + consumed.isConsumed(p));
			ConsoleUtils.println("  ref count  : " + refCounts.get(uname));
		}
		ConsoleUtils.println("Productions: " + grammar.size());
	}

}
