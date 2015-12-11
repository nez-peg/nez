package nez;

import java.io.IOException;
import java.util.HashMap;

import nez.io.SourceStream;
import nez.io.StringContext;
import nez.lang.Grammar;
import nez.lang.NezGrammar1;
import nez.parser.Parser;
import nez.parser.ParserStrategy;

public class GrammarFactory {
	public static class GrammarExtension {
		String path;
		Parser parser;
	}

	String[] classPath = null;
	HashMap<String, GrammarExtension> extensionMap = new HashMap<>();

	public GrammarFactory(String path) {
		this.classPath = path.split(":");
	}

	public GrammarFactory() {
		this("nez/lib");
	}

	public Grammar newGrammar(String fileName) throws IOException {
		SourceStream source = StringContext.loadClassPath(fileName, classPath);

		return null;
	}

	// public Parser getParser(String ext) {
	// GrammarExtension e = extensionMap.get(ext);
	// if (e == null) {
	//
	// }
	// }

	static Parser bootParser;

	public Parser getBootParser() {
		if (bootParser == null) {
			Grammar g = new Grammar("nez");
			bootParser = new NezGrammar1().load(g, "File").newParser("File", ParserStrategy.newSafeStrategy());
			assert (bootParser != null);
		}
		return bootParser;
	}

}
