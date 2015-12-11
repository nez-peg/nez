package nez.main;

import java.io.IOException;

import nez.ParserGenerator;
import nez.Version;
import nez.io.SourceStream;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public abstract class Command {

	public final static String ProgName = "Nez";
	public final static String CodeName = "yokohama";
	public final static int MajorVersion = 1;
	public final static int MinerVersion = 0;
	public final static int PatchLevel = nez.Version.REV;

	public static void main2(String[] args) {
		try {
			CommandContext c = new CommandContext();
			c.parseCommandOption(args, true/* nezCommand */);
			Command com = c.newCommand();
			com.exec(c);
		} catch (IOException e) {
			ConsoleUtils.println(e);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		try {
			Command com = newCommand(args);
			com.exec();
		} catch (IOException e) {
			ConsoleUtils.println(e);
			System.exit(1);
		}
	}

	private static Command newCommand(String[] args) {
		try {
			String className = args[0];
			if (className.indexOf('.') == -1) {
				className = "nez.ext.C" + className;
			}
			Command cmd = (Command) Class.forName(className).newInstance();
			cmd.parseCommandOption(args);
			return cmd;
		} catch (Exception e) {
			// Verbose.traceException(e);
			showUsage("unknown command");
		}
		return null;
	}

	public abstract void exec(CommandContext config) throws IOException;

	public void exec() throws IOException {
		System.out.println(strategy);
	}

	protected String command = null;
	protected ParserStrategy strategy = new ParserStrategy();
	protected String grammarFile = null;
	protected String grammarSource = null;
	protected String grammarType = null;
	protected UList<String> grammarFiles = new UList<String>(new String[4]);
	protected String startProduction = null;
	protected UList<String> inputFiles = new UList<String>(new String[4]);
	protected String format = null;
	protected String directory = null;

	private void parseCommandOption(String[] args) {
		for (int index = 1; index < args.length; index++) {
			String argument = args[index];
			if (!argument.startsWith("-")) {
				break;
			}
			if (index + 1 < args.length) {
				if (argument.equals("-g") || argument.equals("-p")) {
					grammarFile = args[index + 1];
					grammarSource = null;
					index++;
					continue;
				}
				if (argument.equals("-e")) {
					grammarFile = null;
					grammarSource = args[index + 1];
					grammarType = "nez";
					index++;
					continue;
				}
				if (argument.equals("-a")) {
					grammarFiles.add(args[index + 1]);
					index++;
					continue;
				}
				if (argument.equals("-s")) {
					startProduction = args[index + 1];
					index++;
					continue;
				}
				if (argument.equals("-t")) {
					format = args[index + 1];
					index++;
					continue;
				}
				if (argument.equals("-d")) {
					directory = args[index + 1];
					index++;
					continue;
				}
			}
			if (!strategy.setOption(argument)) {
				if (argument.equals("-") && argument.length() > 1) {
					showUsage("undefined option: " + argument);
				}
				this.inputFiles.add(argument);
			}
		}
	}

	protected static void showUsage(String msg) {
		ConsoleUtils.println("nez <command> options files");
		ConsoleUtils.println("  -g <file>      Specify a grammar file");
		ConsoleUtils.println("  -e <text>      Specify a Nez parsing expression");
		ConsoleUtils.println("  -a <file>      Specify a Nez auxiliary grammar files");
		ConsoleUtils.println("  -s <NAME>      Specify a starting production");
		ConsoleUtils.println("  -d <dirname>   Specify an output dir");
		ConsoleUtils.println("  -f <string>    Specify an output format");
		ConsoleUtils.println("");
		ConsoleUtils.println("The most commonly used nez commands are:");
		ConsoleUtils.println("  shell      an interactive mode (by default)");
		ConsoleUtils.println("  match      match an input");
		ConsoleUtils.println("  parse      parse an input and construct ASTs (.ast)");
		ConsoleUtils.println("  compile    compile a grammar to Nez bytecode .moz");
		ConsoleUtils.println("    cnez     generate a C-based parser generator (.c)");
		ConsoleUtils.exit(0, msg);
	}

	public final Grammar newGrammar() throws IOException {
		ParserGenerator pg = new ParserGenerator();
		Grammar grammar = pg.loadGrammar(grammarFile);
		for (String f : this.grammarFiles) {
			pg.updateGrammar(grammar, f);
		}
		return grammar;
	}

	public final Parser newParser() throws IOException {
		return this.strategy.newParser(newGrammar());
	}

	private int fileIndex = 0;

	public final boolean hasInputSource() {
		return fileIndex < inputFiles.size();
	}

	public final SourceStream nextInputSource() throws IOException {
		if (hasInputSource()) {
			String path = this.inputFiles.ArrayValues[fileIndex];
			fileIndex++;
			return SourceStream.newFileContext(path);
		}
		return SourceStream.newStringContext(""); // empty input
	}

	public final String getOutputFileName(SourceStream input, String ext) {
		if (directory != null) {
			return StringUtils.toFileName(input.getResourceName(), directory, ext);
		} else {
			return null; // stdout
		}
	}

	// public final ParserGenerator newParserGenerator(Class<?> c) {
	// if (c != null) {
	// try {
	// ParserGenerator gen;
	// gen = (ParserGenerator) c.newInstance();
	// gen.init(this.getStrategy(), outputDirName, this.getGrammarName());
	// return gen;
	// } catch (InstantiationException e) {
	// e.printStackTrace();
	// } catch (IllegalAccessException e) {
	// e.printStackTrace();
	// }
	// ConsoleUtils.exit(1, "error: new parser generator");
	// }
	// return null;
	// }

	public final static void displayVersion() {
		ConsoleUtils.println(ProgName + "-" + nez.Version.Version + " (" + CodeName + ") on Java JVM-" + System.getProperty("java.version"));
		ConsoleUtils.println(Version.Copyright);
	}

}
