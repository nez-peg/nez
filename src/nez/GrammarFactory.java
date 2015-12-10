package nez;

import java.io.IOException;

import nez.io.SourceStream;
import nez.io.StringContext;

public class GrammarFactory {
	String[] classPath = null;

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

}
