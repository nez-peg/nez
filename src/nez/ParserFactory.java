package nez;

import java.io.IOException;

import nez.ast.Reporter;
import nez.lang.GrammarFileLoader;
import nez.lang.NezGrammar1;
import nez.main.Command;
import nez.main.NezProfier;

public class ParserFactory {

	protected Reporter repo = new Reporter();

	// --option
	protected NezOption option = new NezOption(); // default

	public final void setOption(NezOption option) {
		this.option = option;
	}

	public final NezOption getOption() {
		return this.option;
	}

	/* Grammar */

	protected Grammar grammar = null;

	// -p konoha.nez
	protected String gFileName = null; // default

	// -e "peg rule"
	protected String grammarExpression = null;

	// -r "regular expression"
	protected String regularExpression = null;

	public final Grammar newGrammar() {
		if (grammar == null) {
			return new Grammar();
		}
		return grammar;
	}

	protected final void setGrammarFileName(String path) throws IOException {
		this.gFileName = path;
		this.grammar = newGrammarImpl(path);
	}

	private Grammar newGrammarImpl(String path) throws IOException {
		if (path.equals("nez")) {
			return NezGrammar1.newGrammar();
		}
		return GrammarFileLoader.loadGrammar(path, option, repo);
	}

	// -s, --start
	protected String startProduction = null; // default

	public void setStartProduction(String start) {
		this.startProduction = start;
	}

	public final Parser newParser() {
		Reporter repo = new Reporter();
		Parser p = newGrammar().newParser(this.startProduction, this.option, repo);
		repo.report(option);
		if (p != null && option.enabledProfiling) {
			NezProfier rec = new NezProfier("nezprof.csv");
			rec.setText("nez", Command.Version);
			rec.setText("config", option.toString());
			p.setProfiler(rec);
		}
		return p;
	}

}
