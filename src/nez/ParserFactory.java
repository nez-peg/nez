package nez;

import java.io.IOException;

import nez.lang.GrammarFileLoader;
import nez.lang.NezGrammar1;
import nez.main.Command;
import nez.main.NezProfier;

public class ParserFactory {

	// --option
	protected Strategy strategy = new Strategy(); // default

	public final void setStrategy(Strategy option) {
		this.strategy = option;
	}

	public final Strategy getStrategy() {
		return this.strategy;
	}

	/* Grammar */

	protected Grammar grammar = null;

	// -p konoha.nez
	private String gFileName = null; // default

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

	public final String getGrammarName() {
		if (gFileName != null) {
			String path = gFileName;
			int loc = path.lastIndexOf('/');
			if (loc > 0) {
				path = path.substring(loc + 1);
			}
			loc = path.lastIndexOf('.');
			if (loc > 0) {
				return path.substring(0, loc);
			}
			return path;
		}
		return "g";
	}

	private Grammar newGrammarImpl(String path) throws IOException {
		if (path.equals("nez")) {
			return new NezGrammar1();
		}
		return GrammarFileLoader.loadGrammar(path, strategy);
	}

	// -s, --start
	protected String startProduction = null; // default

	public void setStartProduction(String start) {
		this.startProduction = start;
	}

	public final Parser newParser() {
		Parser p = newGrammar().newParser(this.startProduction, this.strategy);
		this.strategy.report();
		if (p != null && strategy.isEnabled("prof", Strategy.PROF)) {
			NezProfier rec = new NezProfier("nezprof.csv");
			rec.setText("nez", Command.Version);
			rec.setText("config", strategy.toString());
			p.setProfiler(rec);
		}
		return p;
	}

}
