package nez.konoha;

import java.io.IOException;

import nez.NezOption;
import nez.SourceContext;
import nez.ast.Source;
import nez.ast.Tag;
import nez.ast.TreeTransducer;
import nez.lang.Parser;
import nez.lang.GrammarFile;
import nez.util.ConsoleUtils;

public class Konoha extends TreeTransducer {
	KonohaTransducer konoha;
	Parser grammar;

	public Konoha() {
		this.grammar = newKonohaGrammar();
		this.konoha = new KonohaTransducer(this);
	}

	private static GrammarFile konohaGrammar = null;

	public final static Parser newKonohaGrammar() {
		if (konohaGrammar == null) {
			try {
				konohaGrammar = GrammarFile.loadGrammarFile("konoha.nez", NezOption.newDefaultOption());
			} catch (IOException e) {
				ConsoleUtils.exit(1, "can't load konoha.nez");
			}
		}
		return konohaGrammar.newGrammar("File");
	}

	/* begin of tree transducer */

	static final Tag Expression = Tag.tag("node");

	@Override
	public Object newNode(Tag tag, Source s, long spos, long epos, int size, Object value) {
		return new KonohaTree(tag == null ? Expression : tag, s, spos, (int) (epos - spos), size, value);
	}

	@Override
	public void link(Object node, int index, Tag label, Object child) {
		((KonohaTree) node).set(index, (KonohaTree) child);
	}

	@Override
	public Object commit(Object node) {
		return node;
	}

	@Override
	public void abort(Object node) {
	}

	/* end of tree transducer */

	public boolean loadFile(KonohaTransducer konoha, String path) {
		try {
			SourceContext source = SourceContext.newFileContext(path);
			KonohaTree node = (KonohaTree) grammar.parse(source, this);
			if (node == null) {
				ConsoleUtils.println(source.getSyntaxErrorMessage());
				return false;
			}
			return konoha.eval(node);
		} catch (IOException e) {
			ConsoleUtils.exit(1, "cannot open: " + path);
		}
		return false;
	}

	public void parse(KonohaTransducer konoha, String urn, int linenum, String text) {
		SourceContext source = SourceContext.newStringSourceContext(urn, linenum, text);
		KonohaTree node = (KonohaTree) grammar.parse(source, this);
		if (node == null) {
			ConsoleUtils.println(source.getSyntaxErrorMessage());
		}
		System.out.println("parsed:\n" + node + "\n");
		konoha.eval(node);
	}

	public KonohaTransducer newKonohaTransducer() {
		return new KonohaTransducer(this);
	}

	public void shell() {
		ConsoleUtils.println("Konoha" + "-" + "5.0" + " (" + "Nez" + ") on Java JVM-" + System.getProperty("java.version"));
		ConsoleUtils.println("Copyright (c) 2014-2015, Nez project authors");

		KonohaTransducer konoha = newKonohaTransducer();
		int linenum = 1;
		String command = null;
		while ((command = readLine()) != null) {
			parse(konoha, "<stdio>", linenum, command);
			linenum += (command.split("\n").length);
		}
	}

	private String readLine() {
		ConsoleUtils.println("\n>>>");
		Object console = ConsoleUtils.getConsoleReader();
		StringBuilder sb = new StringBuilder();
		while (true) {
			String line = ConsoleUtils.readSingleLine(console, "   ");
			if (line == null) {
				return null;
			}
			if (line.equals("")) {
				return sb.toString();
			}
			ConsoleUtils.addHistory(console, line);
			sb.append(line);
			sb.append("\n");
		}
	}

}
