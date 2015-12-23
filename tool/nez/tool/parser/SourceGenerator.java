package nez.tool.parser;

import nez.lang.Grammar;
import nez.parser.Parser;

public interface SourceGenerator {

	public void init(Grammar g, Parser parser, String path);

	public void doc(String command, String urn, String outputFormat);

	public void generate();

}
