package nez.main;

import java.io.File;
import java.io.IOException;

import nez.ParserFactory;
import nez.io.SourceContext;
import nez.lang.regex.RegularExpression;
import nez.parser.ParserGenerator;
import nez.util.ConsoleUtils;
import nez.util.ExtensionLoader;
import nez.util.StringUtils;
import nez.util.UList;

public class CommandContext extends ParserFactory {

	public CommandContext() {
	}

	// nez [command]
	public String commandName = "shell";

	public Command newCommand() {
		Command cmd = (Command) ExtensionLoader.newInstance("nez.ext.C", commandName);
		if (cmd == null) {
			// if (gFileName != null &&
			// GeneratorLoader.isSupported(this.commandName)) {
			// return new
			// GrammarCommand(GeneratorLoader.load(this.commandName));
			// }
			this.showUsage("unknown command: " + this.commandName);
		}
		return cmd;
	}

	// -t, --text
	public String inputText = null;

	// -i, --input
	private int inputFileIndex = -1; // shell mode
	public UList<String> inputFileLists = new UList<String>(new String[2]);

	void addInputFile(String path) {
		if (new File(path).isFile()) {
			inputFileLists.add(path);
			inputFileIndex = 0;
		}
	}

	public final boolean hasInput() {
		if (this.inputFileIndex == -1) {
			// this.inputText = ConsoleUtils.readMultiLine(">>> ", "... ");
			// return this.inputText != null;
			return false;
		}
		return this.inputText != null || this.inputFileIndex < this.inputFileLists.size();
	}

	public final SourceContext nextInput() throws IOException {
		if (this.inputText != null) {
			String text = this.inputText;
			this.inputText = null;
			return SourceContext.newStringContext(text);
		}
		if (this.inputFileIndex < this.inputFileLists.size()) {
			String f = this.inputFileLists.ArrayValues[this.inputFileIndex];
			this.inputFileIndex++;
			return SourceContext.newFileContext(f);
		}
		return SourceContext.newStringContext(""); // empty input
	}

	// -d, --dir
	public String outputDirName = null;

	public final String getOutputFileName(SourceContext input, String ext) {
		if (outputDirName != null) {
			return StringUtils.toFileName(input.getResourceName(), outputDirName, ext);
		} else {
			return null; // stdout
		}
	}

	// -d, --dir
	public final ParserGenerator newParserGenerator(Class<?> c) {
		try {
			ParserGenerator gen;
			gen = (ParserGenerator) c.newInstance();
			gen.init(this.getStrategy(), outputDirName, this.getGrammarName());
			return gen;
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		ConsoleUtils.exit(1, "error: new parser generator");
		return null;
	}

	// --verbose
	public boolean VerboseMode = false;

	void showUsage(String Message) {
		ConsoleUtils.println("nez <command> optional files");
		ConsoleUtils.println("  -p | --peg <filename>      Specify an Nez grammar file");
		ConsoleUtils.println("  -e | --expr  <text>        Specify an Nez parsing expression");
		ConsoleUtils.println("  -s | --start <NAME>        Specify Non-Terminal as the starting point (default: File)");
		// ConsoleUtils.println("  -i | --input <filenames>   Specify input file(s)");
		ConsoleUtils.println("  -t | --text  <string>      Specify an input text");
		ConsoleUtils.println("  -d | --dir <dirname>       Specify an output dir");
		ConsoleUtils.println("  --option:(+enable:-disable)*");
		ConsoleUtils.println("     grammars: +ast +symbol");
		ConsoleUtils.println("     optimize: +lex +inline predict dfa");
		ConsoleUtils.println("     packrat:  packrat +sliding trace");
		ConsoleUtils.println("  --verbose                  Printing Debug infomation");
		ConsoleUtils.println("  --verbose:memo             Printing Memoization information");
		ConsoleUtils.println("  -X <class>                 Specify an extension class");
		ConsoleUtils.println("");
		ConsoleUtils.println("The most commonly used nez commands are:");
		ConsoleUtils.println("  shell      an interactive mode (by default)");
		ConsoleUtils.println("  match      match an input");
		ConsoleUtils.println("  parse      parse an input and construct ASTs (.ast)");
		ConsoleUtils.println("    xml      parse an input and convert into XML (.xml)");
		ConsoleUtils.println("    json     parse an input and convert into JSON (.json)");
		ConsoleUtils.println("  compile    compile a grammar to Nez bytecode .nzc");
		ConsoleUtils.println("    cnez     generate a C-based parser generator (.c)");
		ConsoleUtils.exit(0, Message);
	}

	public void parseCommandOption(String[] args) throws IOException {
		String gFileName = null;
		String regex = null;
		int index = 0;
		if (args.length > 0) {
			if (!args[0].startsWith("-")) {
				commandName = args[0];
				index = 1;
			}
		}
		while (index < args.length) {
			String argument = args[index];
			if (!argument.startsWith("-")) {
				break;
			}
			index = index + 1;
			if (argument.equals("-X") && (index < args.length)) {
				try {
					Class.forName(args[index]);
				} catch (ClassNotFoundException e) {
					ConsoleUtils.exit(1, "-X specified class is not found: " + args[index]);
				}
				index = index + 1;
			} else if ((argument.equals("-p") || argument.equals("--peg")) && (index < args.length)) {
				gFileName = args[index];
				index = index + 1;
			} else if ((argument.equals("-g") || argument.equals("--grammar")) && (index < args.length)) {
				gFileName = args[index];
				index = index + 1;
			} else if ((argument.equals("-r") || argument.equals("--re")) && (index < args.length)) {
				regex = args[index];
				index = index + 1;
			} else if ((argument.equals("-e") || argument.equals("--expr")) && (index < args.length)) {
				grammarExpression = args[index];
				index = index + 1;
			} else if ((argument.equals("-t") || argument.equals("--text")) && (index < args.length)) {
				inputText = args[index];
				index = index + 1;
				inputFileIndex = 0;
			} else if ((argument.equals("-i") || argument.equals("--input")) && (index < args.length)) {
				inputFileLists = new UList<String>(new String[4]);
				while (index < args.length && !args[index].startsWith("-")) {
					this.addInputFile(args[index]);
					index = index + 1;
				}
			} else if ((argument.equals("-d") || argument.equals("--dir")) && (index < args.length)) {
				outputDirName = args[index];
				index = index + 1;
			} else if ((argument.equals("-s") || argument.equals("--start")) && (index < args.length)) {
				startProduction = args[index];
				index = index + 1;
			} else if (argument.startsWith("--option:")) {
				String s = argument.substring(9);
				this.strategy.setOption(s);
			} else if (argument.startsWith("--verbose")) {
				if (argument.equals("--verbose:example")) {
					Verbose.Example = true;
				} else if (argument.equals("--verbose:time")) {
					Verbose.Time = true;
				} else if (argument.equals("--verbose:memo")) {
					Verbose.PackratParsing = true;
				} else if (argument.equals("--verbose:peg")) {
					Verbose.Grammar = true;
				} else if (argument.equals("--verbose:vm")) {
					Verbose.VirtualMachine = true;
				} else if (argument.equals("--verbose:debug")) {
					Verbose.Debug = true;
				} else if (argument.equals("--verbose:backtrack")) {
					Verbose.BacktrackActivity = true;
				} else if (argument.equals("--verbose:none")) {
					Verbose.General = false;
				} else {
					Verbose.setAll();
					Verbose.println("unknown verbose option: " + argument);
				}
			} else {
				this.showUsage("unknown option: " + argument);
			}
		}
		for (; index < args.length; index++) {
			String path = args[index];
			this.addInputFile(path);
		}
		if (regex != null) {
			this.grammar = RegularExpression.newGrammar(regex);
			return;
		}
		if (gFileName != null) {
			this.setGrammarFileName(gFileName);
		}
	}

	//
	// public final static String LoaderPoint = "nez.main.LC";
	//
	// public final Command getCommand() {
	// Command cmd = null;
	// try {
	// Class<?> c = Class.forName(LoaderPoint + commandName);
	// cmd = (Command) c.newInstance();
	// } catch (ClassNotFoundException e) {
	//
	// } catch (InstantiationException e) {
	// Verbose.traceException(e);
	// } catch (IllegalAccessException e) {
	// Verbose.traceException(e);
	// }
	// if (cmd == null) {
	// if (gFileName != null && GeneratorLoader.isSupported(this.commandName)) {
	// return new GrammarCommand(GeneratorLoader.load(this.commandName));
	// }
	// this.showUsage("unknown command: " + this.commandName);
	// }
	// return cmd;
	// }
	// class GrammarCommand extends Command {
	// NezGenerator gen;
	//
	// @Override
	// public String getDesc() {
	// return "parser generator";
	// }
	//
	// GrammarCommand(NezGenerator gen) {
	// this.gen = gen;
	// }
	//
	// @Override
	// public void exec(CommandContext config) throws IOException {
	// Parser g = config.newParser();
	// gen.generate(g, option, OutputFileName);
	// }
	// }

	public final void setInputFileList(UList<String> list) {
		this.inputFileIndex = 0;
		this.inputFileLists = list;
	}

	public final UList<String> getInputFileList() {
		return this.inputFileLists;
	}

}
