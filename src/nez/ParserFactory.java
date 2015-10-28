package nez;

import java.io.File;
import java.io.IOException;

import nez.lang.GrammarFileLoader;
import nez.lang.util.NezConstructor;
import nez.util.UList;

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
	private String grammarFilePath = null; // default

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

	public final void setGrammarFilePath(String path) throws IOException {
		this.grammarFilePath = path;
		this.grammar = newGrammarImpl(path);
	}

	public final String getGrammarName() {
		if (grammarFilePath != null) {
			String path = grammarFilePath;
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

	public final String getGrammarPath() {
		if (grammarFilePath != null) {
			return grammarFilePath;
		}
		return null;
	}

	public final boolean isUnspecifiedGrammarFilePath() {
		return (grammarFilePath == null);
	}

	private Grammar newGrammarImpl(String path) throws IOException {
		// if (path.equals("nez")) {
		// return aux(new NezGrammar1());
		// }
		return aux(GrammarFileLoader.loadGrammar(path, strategy));
	}

	// -a, --aux Auxiliary
	private UList<String> auxFileLists = null;

	public void addAuxiliaryGrammar(String path) {
		if (new File(path).isFile() && path.endsWith(".nez")) {
			if (auxFileLists == null) {
				this.auxFileLists = new UList<String>(new String[2]);
			}
			auxFileLists.add(path);
		}
	}

	protected final Grammar aux(Grammar g) throws IOException {
		if (auxFileLists != null) {
			for (String url : auxFileLists) {
				NezConstructor c = new NezConstructor(g);
				c.load(g, url, this.getStrategy());
			}
		}
		return g;
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
			rec.setText("nez", Version.Version);
			rec.setText("config", strategy.toString());
			p.setProfiler(rec);
		}
		return p;
	}

}
