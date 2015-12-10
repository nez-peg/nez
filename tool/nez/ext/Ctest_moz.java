//package nez.ext;
//
//import java.io.IOException;
//
//import nez.lang.Example;
//import nez.lang.Grammar;
//import nez.main.Command;
//import nez.main.CommandContext;
//
//public class Ctest_moz extends Command {
//	@Override
//	public void exec(CommandContext config) throws IOException {
//		Grammar g = config.newGrammar();
//		if (g instanceof Grammar) {
//			Example.testMoz(config.getGrammarName(), g, config.getStrategy());
//		}
//	}
// }