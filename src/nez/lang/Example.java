package nez.lang;

import java.util.HashMap;
import java.util.List;

import nez.Parser;
import nez.Strategy;
import nez.ast.Tree;
import nez.ast.TreeUtils;
import nez.io.SourceContext;
import nez.main.Verbose;
import nez.parser.Coverage;
import nez.parser.NezCode;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;

public class Example {
	boolean isPublic;
	Tree<?> nameNode;
	Tree<?> textNode;
	String hash;

	public Example(boolean isPublic, Tree<?> nameNode, String hash, Tree<?> textNode) {
		this.isPublic = true;
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

	public boolean test(Parser p, TestResult result) {
		SourceContext source = textNode.newSourceContext();
		String name = nameNode.toText() + " (" + textNode.getSource().getResourceName() + ":" + textNode.getLinenum() + ")";
		Tree<?> node = p.parseCommonTree(source);
		if (node == null) {
			ConsoleUtils.println("[ERR*] " + name);
			ConsoleUtils.println(source.getSyntaxErrorMessage());
			result.syntaxError += 1;
			return false;
		}
		String nodehash = TreeUtils.digestString(node);
		if (hash == null) {
			display("[TODO]", name, nodehash, node);
			this.hash = nodehash;
			result.untested += 1;
			return true;
		}
		if (nodehash.startsWith(hash)) {
			display("[PASS]", name, null, null);
			result.succ += 1;
			return true;
		}
		display("[FAIL]", name, nodehash, node);
		result.failed += 1;
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
		}
	}

	public static void testAll(GrammarFile g, Strategy strategy) {
		List<Example> exampleList = g.getExampleList();
		if (exampleList != null) {
			Coverage.init();
			TestResult result = new TestResult();
			HashMap<String, Parser> parserMap = new HashMap<>();
			long t1 = System.nanoTime();
			for (Example ex : exampleList) {
				try {
					String name = ex.getName();
					Parser p = parserMap.get(name);
					if (p == null) {
						p = g.newParser(name, strategy);
						if (p == null) {
							ConsoleUtils.println(ex.formatWarning("undefined nonterminal: " + name));
							continue;
						}
						parserMap.put(name, p);
					}
					ex.test(p, result);
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
			Coverage.dump();
			ConsoleUtils.println("Elapsed time (Example Tests): " + ((t2 - t1) / 1000000) + "ms");
			ConsoleUtils.println("Pass: " + result.getSucc() + "/" + result.getTotal() + " Pass ratio: " + result.getRatio() + "%");
			float cov = Coverage.calc() * 100;
			ConsoleUtils.println("git commit -am '" + g.getDesc() + " - " + cov + "%, " + result.getStatus() + "'");
		} else {
			ConsoleUtils.println("git commit -am '" + g.getDesc() + " - 0.0% tested. DO NOT USE'");
		}
	}

	public static void testMoz(String baseName, GrammarFile g, Strategy strategy) {
		List<Example> exampleList = g.getExampleList();
		if (exampleList == null) {
			ConsoleUtils.println("no example exists in " + baseName);
			return;
		}
		int id = 0;
		FileBuilder fb = new FileBuilder(baseName + "_test.sh");
		fb.write("#!/bin/bash");
		HashMap<String, Parser> parserMap = new HashMap<>();
		for (Example ex : exampleList) {
			String name = ex.getName();
			Parser p = parserMap.get(name);
			if (p == null) {
				p = g.newParser(name, strategy);
				if (p == null) {
					ConsoleUtils.println(ex.formatWarning("undefined nonterminal: " + name));
					continue;
				}
				parserMap.put(name, p);
			}
			String path = baseName + "_" + ex.getName() + ".moz";
			NezCode.writeMozCode(p, path);
			String path2 = String.format("%s_%s%03d.test_moz", baseName, ex.getName(), id);
			id++;
			ex.writeMozTest(path2);
			fb.writeIndent("moz test -g " + path + " -t " + path2);
		}
		fb.close();
		ConsoleUtils.println("run " + baseName + "_test.sh");
	}

	private void writeMozTest(String path) {
		FileBuilder fb = new FileBuilder(path);
		if (this.hash != null) {
			fb.write(this.hash);
		}
		fb.writeIndent(this.textNode.toString());
		fb.close();
	}

}

class TestResult {
	int syntaxError = 0;
	int untested = 0;
	int succ = 0;
	int failed = 0;

	int getTotal() {
		return syntaxError + untested + succ + failed;
	}

	int getSucc() {
		return succ;
	}

	float getRatio() {
		return succ * 100.0f / this.getTotal();
	}

	String getStatus() {
		if (syntaxError > 0) {
			return "very bad (syntax error)";
		}
		if (failed > 0) {
			return "bad";
		}
		if (untested > 0) {
			return "fine";
		}
		return "good";
	}
}
