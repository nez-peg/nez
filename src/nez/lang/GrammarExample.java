package nez.lang;

import java.util.HashMap;
import java.util.List;

import nez.Verbose;
import nez.ast.Tree;
import nez.ast.TreeUtils;
import nez.io.SourceStream;
import nez.parser.Coverage;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class GrammarExample {
	private Grammar grammar;
	private UList<Example> exampleList;

	public GrammarExample(Grammar grammar) {
		this.grammar = grammar;
		exampleList = new UList<Example>(new Example[8]);
	}

	public List<Example> getExampleList() {
		return exampleList;
	}

	public final void addExample(boolean isPublic, Tree<?> nameNode, String hash, Tree<?> textNode) {
		exampleList.add(new Example(isPublic, nameNode, hash, textNode));
	}

	public final static void add(Grammar g, boolean isPublic, Tree<?> nameNode, String hash, Tree<?> textNode) {
		GrammarExample example = (GrammarExample) g.getMetaData("example");
		if (example == null) {
			example = new GrammarExample(g);
			g.setMetaData("example", example);
		}
		example.addExample(isPublic, nameNode, hash, textNode);
	}

	public final List<String> getExampleList(String name) {
		UList<String> l = new UList<String>(new String[4]);
		if (exampleList != null) {
			for (Example ex : exampleList) {
				if (name.equals(ex.getName())) {
					l.add(ex.getText());
				}
			}
		}
		return l;
	}

	public final void checkExample() {
		if (exampleList == null) {
			for (Example ex : exampleList) {
				if (ex.isPublic) {
					Production p = grammar.getProduction(ex.getName());
					if (p != null) {
						p.flag |= Production.PublicProduction;
					}
				}
			}
		}
	}

	public final boolean testAll(Grammar grammar, ParserStrategy strategy, boolean ExampleCommand) {
		Coverage.init();
		TestResult result = new TestResult();
		HashMap<String, Parser> parserMap = new HashMap<>();
		long t1 = System.nanoTime();
		for (Example ex : exampleList) {
			if (ExampleCommand && !ex.isPublic) {
				continue; // skip nonpublic
			}
			try {
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
				ex.test(p, result, ExampleCommand);
			} catch (Exception e) {
				ConsoleUtils.println((ex.formatPanic("exception detected: " + e)));
				if (ConsoleUtils.isDebug()) {
					e.printStackTrace();
				}
				Verbose.traceException(e);
			} catch (Error e) {
				ConsoleUtils.println((ex.formatPanic("error detected: " + e)));
				if (ConsoleUtils.isDebug()) {
					e.printStackTrace();
				}
			}
		}
		long t2 = System.nanoTime();
		float cov = Coverage.calc() * 100;
		if (!ExampleCommand) {
			Coverage.dump();
		}
		ConsoleUtils.println("Elapsed time (Example Tests): " + ((t2 - t1) / 1000000) + "ms");
		ConsoleUtils.print("Syntax Pass: " + result.getSuccSyntax() + "/" + result.getTotal() + " ratio: " + result.getRatioSyntax() + "%");
		ConsoleUtils.println(", AST Pass: " + result.getSuccAST() + "/" + result.getTotalAST() + " ratio: " + result.getRatioAST() + "%");
		if (!ExampleCommand) {
			ConsoleUtils.println("git commit -am '" + grammar.getDesc() + " - " + cov + "% tested, " + result.getStatus(cov) + "'");
		}
		return !result.hasFailure();
	}

	/**
	 * public void testMoz(String baseName, Grammar g, ParserStrategy strategy)
	 * { List<Example> exampleList = g.getExampleList(); if (exampleList ==
	 * null) { ConsoleUtils.println("no example exists in " + baseName); return;
	 * } int id = 0; FileBuilder fb = new FileBuilder(baseName + "_test.sh");
	 * fb.write("#!/bin/bash"); HashMap<String, Parser> parserMap = new
	 * HashMap<>(); for (Example ex : exampleList) { String name = ex.getName();
	 * Parser p = parserMap.get(name); if (p == null) { p = g.newParser(name,
	 * strategy); if (p == null) {
	 * ConsoleUtils.println(ex.formatWarning("undefined nonterminal: " + name));
	 * continue; } parserMap.put(name, p); } String path = baseName + "_" +
	 * ex.getName() + ".moz"; MozCode.writeMozCode(p, path); String path2 =
	 * String.format("%s_%s%03d.test_moz", baseName, ex.getName(), id); id++;
	 * ex.writeMozTest(path2); fb.writeIndent("moz -t -p " + path + " -i " +
	 * path2); } fb.close(); ConsoleUtils.println("run " + baseName +
	 * "_test.sh"); }
	 * 
	 * private void writeMozTest(String path) { FileBuilder fb = new
	 * FileBuilder(path); if (this.hash != null) { fb.write(this.hash); }
	 * fb.writeIndent(this.getText()); fb.close(); }
	 **/

	public class Example {
		boolean isPublic;
		Tree<?> nameNode;
		Tree<?> textNode;
		String hash;

		public Example(boolean isPublic, Tree<?> nameNode, String hash, Tree<?> textNode) {
			this.isPublic = isPublic;
			this.nameNode = nameNode;
			this.textNode = textNode;
			this.hash = hash;
		}

		public final String getName() {
			return nameNode.toText();
		}

		public final String getText() {
			return textNode.toText();
		}

		public final boolean hasHash() {
			return this.hash != null;
		}

		public String formatPanic(String msg) {
			return nameNode.formatSourceMessage("panic", msg);
		}

		public String formatWarning(String msg) {
			return nameNode.formatSourceMessage("warning", msg);
		}

		public boolean test(Parser p, TestResult result, boolean verbose) {
			SourceStream source = textNode.newSourceContext();
			String name = nameNode.toText() + " (" + textNode.getSource().getResourceName() + ":" + textNode.getLineNum() + ")";
			Tree<?> node = p.parse(source);
			if (node == null) {
				ConsoleUtils.println("[ERR*] " + name);
				p.showErrors();
				result.failSyntax += 1;
				return false;
			}
			result.succSyntax += 1;
			String nodehash = TreeUtils.digestString(node);
			if (hash == null) {
				display("[TODO]", name, nodehash, node);
				this.hash = nodehash;
				result.untestedAST += 1;
				return true;
			}
			if (nodehash.startsWith(hash)) {
				display("[PASS]", name, verbose ? nodehash : null, verbose ? node : null);
				result.succAST += 1;
				return true;
			}
			display("[FAIL]", name, nodehash, node);
			result.failAST += 1;
			return false;
		}

		public void display(String msg, String name, String nodehash, Tree<?> node) {
			if (nodehash != null) {
				ConsoleUtils.println(msg + " " + name + " ~" + nodehash);
			} else {
				ConsoleUtils.println(msg + " " + name);
			}
			if (node != null) {
				ConsoleUtils.println("   ", this.getText());
				ConsoleUtils.println("---");
				ConsoleUtils.println("   ", node);
				ConsoleUtils.println("---");
			}
		}

	}

	class TestResult {
		int succSyntax = 0;
		int failSyntax = 0;

		int untestedAST = 0;
		int succAST = 0;
		int failAST = 0;

		int getTotal() {
			return succSyntax + failSyntax;
		}

		public boolean hasFailure() {
			// TODO Auto-generated method stub
			return false;
		}

		int getSuccSyntax() {
			return succSyntax;
		}

		int getSuccAST() {
			return succAST;
		}

		int getTotalAST() {
			return succSyntax + failSyntax - untestedAST;
		}

		float getRatioSyntax() {
			return succSyntax * 100.0f / this.getTotal();
		}

		float getRatioAST() {
			return succAST * 100.0f / this.getTotalAST();
		}

		String getStatus(float r) {
			if (failSyntax > 0) {
				return (succSyntax < failSyntax) ? "very bad" : "bad";
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

		boolean hasFailrue() {
			return failSyntax > 0;
		}
	}

}
