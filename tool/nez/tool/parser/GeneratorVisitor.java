//package nez.tool.parser;
//
//import nez.lang.Expression;
//import nez.lang.Grammar;
//import nez.parser.Parser;
//import nez.util.ConsoleUtils;
//import nez.util.FileBuilder;
//
//public abstract class GeneratorVisitor extends Expression.Visitor implements SourceGenerator {
//	protected Parser parser;
//
//	protected String path;
//	protected FileBuilder file;
//
//	public GeneratorVisitor() {
//		this.file = null;
//	}
//
//	@Override
//	public final void init(Grammar g, Parser parser, String path) {
//		this.parser = parser;
//		if (path == null) {
//			this.file = new FileBuilder(null);
//		} else {
//			this.path = FileBuilder.extractFileName(path);
//			String filename = FileBuilder.changeFileExtension(path, this.getFileExtension());
//			this.file = new FileBuilder(filename);
//			ConsoleUtils.println("generating " + filename + " ... ");
//		}
//	}
//
//	protected abstract String getFileExtension();
//
//	@Override
//	public void doc(String command, String urn, String outputFormat) {
//		// file.writeIndent(LineComment + "Translated by nez " + command +
//		// " -g " + urn + " --format " + outputFormat);
//	}
//
// }
