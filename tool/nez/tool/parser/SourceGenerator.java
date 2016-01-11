package nez.tool.parser;

import nez.lang.Grammar;
import nez.parser.Parser;

public interface SourceGenerator {

	void init(Grammar g, Parser parser, String path);

	void doc(String command, String urn, String outputFormat);

	void generate();

}
