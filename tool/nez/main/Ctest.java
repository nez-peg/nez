package nez.main;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import nez.ast.Source;
import nez.ast.Tree;
import nez.ast.TreeUtils;
import nez.lang.Grammar;
import nez.lang.ast.GrammarExample;
import nez.lang.ast.GrammarExample.Example;
import nez.parser.CoverageProfiler;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.parser.io.StringSource;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.Verbose;

public class Ctest extends Command {
	@Override
	public void exec() throws IOException {
		Grammar grammar = newGrammar();
		GrammarExample example = (GrammarExample) grammar.getMetaData("example");
		if (example == null) {
			ConsoleUtils.println("No example is specified");
			return;
		}
		if (!testAll(grammar, example.getExampleList(), strategy)) {
			System.exit(1);
		}
	}

	public final boolean testAll(Grammar grammar, List<Example> exampleList, ParserStrategy strategy) {
		TestStat result = new TestStat();
		if (!(this instanceof Cexample)) {
			strategy.Coverage = true;
			strategy.Oinline = false;
			strategy.ChoicePrediction = 1;
		}
		HashMap<String, Parser> parserMap = new HashMap<>();
		long t1 = System.nanoTime();
		for (Example ex : exampleList) {
			if (this instanceof Cexample && !ex.isPublic) {
				continue; // skip nonpublic
			}
			String name = ex.getName();
			Parser p = parserMap.get(name);
			if (p == null) {
				p = grammar.newParser(name, strategy);
				if (p == null) {
					ConsoleUtils.println(ex.formatWarning("undefined nonterminal: " + name));
					continue;
				}
				parserMap.put(name, p);
			}
			testExample(ex, p, result, this instanceof Cexample);
		}
		long t2 = System.nanoTime();
		CoverageProfiler prof = strategy.getCoverageProfier();
		if (prof != null) {
			prof.dumpCoverage();
		}
		ConsoleUtils.println("Elapsed time (including all tests): " + ((t2 - t1) / 1000000) + "ms");
		ConsoleUtils.print("Syntax Pass: " + result.getSuccSyntax() + "/" + result.getTotal() + " ratio: " + result.getRatioSyntax() + "%");
		ConsoleUtils.println(", AST Pass: " + result.getSuccAST() + "/" + result.getTotalAST() + " ratio: " + result.getRatioAST() + "%");

		if (!(this instanceof Cexample) && !result.hasFailure()) {
			double cov = prof.getCoverage();
			if (cov > 0.5) {
				ConsoleUtils.println("");
				ConsoleUtils.bold();
				ConsoleUtils.println("Congratulation!!");
				ConsoleUtils.println("You are invited to share your grammar at Nez open grammar repository, ");
				ConsoleUtils.println("                                   http://github.com/nez-peg/grammar.");
				ConsoleUtils.println("If you want, please send a pull request with:");
				ConsoleUtils.println("\tgit commit -am '" + grammar.getDesc() + ", " + (cov * 100) + "% (coverage) tested.'");
				ConsoleUtils.end();
			}
		}
		return !result.hasFailure();
	}

	public boolean testExample(Example ex, Parser p, TestStat stat, boolean verbose) {
		Source source = newSource(ex.textNode, p.getParserStrategy());
		if (source == null) {
			return false;
		}
		String name = ex.nameNode.toText() + " (" + ex.textNode.getSource().getResourceName() + ":" + ex.textNode.getLineNum() + ")";
		stat.testCount += 1;
		try {
			Tree<?> node = p.parse(source);
			if (node == null) {
				ConsoleUtils.begin(ConsoleUtils.Red);
				ConsoleUtils.println("[FAIL] " + name);
				p.showErrors();
				ConsoleUtils.end();
				return false;
			}
			stat.succSyntax += 1;
			String nodehash = TreeUtils.digestString(node);
			if (ex.hash == null) {
				display(ex, ConsoleUtils.Gray, "[PASS?]", name, nodehash, node);
				// ex.hash = nodehash;
				stat.untestedAST += 1;
				return true;
			}
			if (nodehash.startsWith(ex.hash)) {
				display(ex, ConsoleUtils.Green, "[PASS]", name, verbose ? nodehash : null, verbose ? node : null);
				stat.succAST += 1;
				return true;
			}
			display(ex, ConsoleUtils.Magenta, "[PASS??]", name, nodehash, node);
			stat.failAST += 1;
			return false;
		} catch (Exception e) {
			display(ex, ConsoleUtils.Red, "[PANIC]", name, "detected: " + e, null);
			Verbose.traceException(e);
		} catch (Throwable e) {
			display(ex, ConsoleUtils.Red, "[PANIC]", name, "detected: " + e, null);
			Verbose.traceException(e);
		}
		stat.failAST += 1;
		return false;
	}

	public void display(Example ex, int color, String msg, String name, String nodehash, Tree<?> node) {
		ConsoleUtils.begin(color);
		if (nodehash != null) {
			ConsoleUtils.println(msg + " " + name + " ~" + nodehash);
		} else {
			ConsoleUtils.println(msg + " " + name);
		}
		ConsoleUtils.end();
		if (node != null) {
			ConsoleUtils.printlnIndent("   ", ex.getText());
			ConsoleUtils.println("---");
			ConsoleUtils.printlnIndent("   ", node);
			ConsoleUtils.println("---");
		}
	}

	private Source newSource(Tree<?> textNode, ParserStrategy strategy) {
		if (strategy.BinaryGrammar) {
			byte[] b = parseBinary(textNode.toText());
			if (b == null) {
				ConsoleUtils.begin(ConsoleUtils.Red);
				ConsoleUtils.println(textNode.formatSourceMessage("error", "Invalid binary format"));
				ConsoleUtils.end();
				return null;
			}
			return new StringSource(textNode.getSource().getResourceName(), textNode.getSourcePosition(), b, true);
		}
		return textNode.toSource();
	}

	byte[] parseBinary(String t) {
		UList<Byte> bytes = new UList<>(new Byte[t.length()]);
		for (int i = 0; i < t.length(); i++) {
			char ch = t.charAt(i);
			if (ch == ' ' || ch == '\n' || ch == '\r') {
				continue;
			}
			int b = parseHex(t, i);
			if (b != -1) {
				// System.out.println("hex=" + b);
				i += 1;
				bytes.add((byte) b);
				continue;
			}
		}
		bytes.add((byte) 0);
		byte[] b = new byte[bytes.size()];
		for (int i = 0; i < b.length; i++) {
			b[i] = bytes.get(i);
		}
		return b;
	}

	int parseHex(String t, int i) {
		try {
			char ch = t.charAt(i);
			char ch2 = t.charAt(i + 1);
			return Integer.parseInt("" + ch + ch2, 16);

		} catch (Exception e) {
			return -1;
		}
	}

	class TestStat {
		int testCount = 0;
		int succSyntax = 0;

		int untestedAST = 0;
		int succAST = 0;
		int failAST = 0;

		int getTotal() {
			return testCount;
		}

		int getSuccSyntax() {
			return succSyntax;
		}

		int getSuccAST() {
			return succAST;
		}

		int getTotalAST() {
			return testCount - untestedAST;
		}

		float getRatioSyntax() {
			return succSyntax * 100.0f / this.getTotal();
		}

		float getRatioAST() {
			return succAST * 100.0f / this.getTotalAST();
		}

		String getStatus(float r) {
			if (testCount - succSyntax > 0) {
				return (succSyntax < (testCount / 2)) ? "bad" : "very bad";
			} else {
				if (failAST > 0) {
					return "not bad";
				}
				if (untestedAST > succAST) {
					return "rough";
				}
				if (r > 95.0) {
					return "excellent";
				}
				return "good";
			}
		}

		boolean hasFailure() {
			return testCount - succSyntax > 0;
		}
	}

}
