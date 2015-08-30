package nez.main;

import java.io.IOException;

import nez.NezOption;
import nez.SourceContext;
import nez.generator.GeneratorLoader;
import nez.generator.NezGenerator;
import nez.lang.Grammar;
import nez.lang.GrammarFile;
import nez.lang.NezGrammar;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

class CommandContext {

	public CommandContext(String[] args) {
		this.parseCommandOption(args);
	}

	public String commandName = "shell";

	// -p konoha.nez
	public String grammarFile = null; // default

	// -e "peg rule"
	public String grammarExpression = null;

	// --option
	private NezOption option = new NezOption(); // default

	public final NezOption getNezOption() {
		return this.option;
	}

	// -s, --start
	private String startingProduction = "File"; // default

	// -i, --input
	private int InputFileIndex = -1; // shell mode
	public UList<String> inputFileLists = new UList<String>(new String[2]);

	// -t, --text
	public String inputText = null;

	// -o, --output
	public String OutputFileName = null;

	// -W
	public int CheckerLevel = 1;

	// -g
	public int DebugLevel = 1;

	// --verbose
	public boolean VerboseMode = false;

	void showUsage(String Message) {
		ConsoleUtils.println("nez <command> optional files");
		ConsoleUtils.println("  -p | --peg <filename>      Specify an Nez grammar file");
		ConsoleUtils.println("  -e | --expr  <text>        Specify an Nez parsing expression");
		ConsoleUtils.println("  -i | --input <filenames>   Specify input file(s)");
		ConsoleUtils.println("  -t | --text  <string>      Specify an input text");
		ConsoleUtils.println("  -o | --output <filename>   Specify an output file");
		ConsoleUtils.println("  -s | --start <NAME>        Specify Non-Terminal as the starting point (default: File)");
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

	public void parseCommandOption(String[] args) {
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
					Class<?> c = Class.forName(args[index]);
					// if(ParsingWriter.class.isAssignableFrom(c)) {
					// OutputWriterClass = c;
					// }
				} catch (ClassNotFoundException e) {
					ConsoleUtils.exit(1, "-X specified class is not found: " + args[index]);
				}
				index = index + 1;
			} else if ((argument.equals("-p") || argument.equals("--peg")) && (index < args.length)) {
				grammarFile = args[index];
				index = index + 1;
			} else if ((argument.equals("-e") || argument.equals("--expr")) && (index < args.length)) {
				grammarExpression = args[index];
				index = index + 1;
			} else if ((argument.equals("-t") || argument.equals("--text")) && (index < args.length)) {
				inputText = args[index];
				index = index + 1;
				InputFileIndex = 0;
			} else if ((argument.equals("-i") || argument.equals("--input")) && (index < args.length)) {
				inputFileLists = new UList<String>(new String[4]);
				while (index < args.length && !args[index].startsWith("-")) {
					inputFileLists.add(args[index]);
					index = index + 1;
					InputFileIndex = 0;
				}
			} else if ((argument.equals("-o") || argument.equals("--output")) && (index < args.length)) {
				OutputFileName = args[index];
				index = index + 1;
			} else if ((argument.equals("-s") || argument.equals("--start")) && (index < args.length)) {
				startingProduction = args[index];
				index = index + 1;
			} else if (argument.startsWith("-W")) {
				CheckerLevel = StringUtils.parseInt(argument.substring(2), 0);
			} else if (argument.startsWith("--option:")) {
				String s = argument.substring(9);
				this.option.setOption(s);
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
	}

	public final static String LoaderPoint = "nez.main.LC";

	public final Command getCommand() {
		Command cmd = null;
		try {
			Class<?> c = Class.forName(LoaderPoint + commandName);
			cmd = (Command) c.newInstance();
		} catch (ClassNotFoundException e) {

		} catch (InstantiationException e) {
			Verbose.traceException(e);
		} catch (IllegalAccessException e) {
			Verbose.traceException(e);
		}
		if (cmd == null) {
			if (grammarFile != null && GeneratorLoader.isSupported(this.commandName)) {
				return new GrammarCommand(GeneratorLoader.load(this.commandName));
			}
			this.showUsage("unknown command: " + this.commandName);
		}
		return cmd;
	}

	class GrammarCommand extends Command {
		NezGenerator gen;

		@Override
		public String getDesc() {
			return "parser generator";
		}

		GrammarCommand(NezGenerator gen) {
			this.gen = gen;
		}

		@Override
		public void exec(CommandContext config) {
			Grammar g = config.getGrammar();
			gen.generate(g, option, OutputFileName);
		}
	}

	public final String getGrammarFileName(String ext) {
		if (grammarFile != null) {
			int loc = grammarFile.lastIndexOf('.');
			if (loc > 0) {
				return grammarFile.substring(0, loc + 1) + ext;
			}
			return grammarFile + "." + ext;
		}
		return "noname." + ext;
	}

	public final GrammarFile getGrammarFile(boolean grammarFileCreation) {
		if (grammarFile != null) {
			if (grammarFile.equals("nez")) {
				return NezGrammar.newGrammarFile();
			}
			try {
				return GrammarFile.loadGrammarFile(grammarFile, option);
			} catch (IOException e) {
				ConsoleUtils.exit(1, "cannot open " + grammarFile + "; " + e.getMessage());
			}
		}
		if (grammarFileCreation) {
			return GrammarFile.newGrammarFile(option);
		} else {
			ConsoleUtils.println("unspecifed grammar");
			return NezGrammar.newGrammarFile();
		}
	}

	public final Grammar getGrammar(String start, NezOption option) {
		if (start == null) {
			start = this.startingProduction;
		}
		Grammar g = getGrammarFile(false).newGrammar(start, option);
		if (g == null) {
			ConsoleUtils.exit(1, "undefined production: " + start);
		}
		if (option.enabledProfiling) {
			NezProfier rec = new NezProfier("nezprof.csv");
			rec.setText("nez", Command.Version);
			rec.setText("config", option.toString());
			g.setProfiler(rec);
		}
		return g;
	}

	public final Grammar getGrammar(String start) {
		return this.getGrammar(start, option);
	}

	public final Grammar getGrammar() {
		return this.getGrammar(this.startingProduction, option);
	}

	public final boolean hasInputSource() {
		if (this.InputFileIndex == -1) {
			// this.inputText = ConsoleUtils.readMultiLine(">>> ", "... ");
			// return this.inputText != null;
			return false;
		}
		return this.inputText != null || this.InputFileIndex < this.inputFileLists.size();
	}

	public final void setInputFileList(UList<String> list) {
		this.InputFileIndex = 0;
		this.inputFileLists = list;
	}

	public final UList<String> getInputFileList() {
		return this.inputFileLists;
	}

	public final SourceContext nextInputSource() {
		if (this.inputText != null) {
			String text = this.inputText;
			this.inputText = null;
			return SourceContext.newStringContext(text);
		}
		if (this.InputFileIndex < this.inputFileLists.size()) {
			String f = this.inputFileLists.ArrayValues[this.InputFileIndex];
			this.InputFileIndex++;
			try {
				return SourceContext.newFileContext(f);
			} catch (IOException e) {
				ConsoleUtils.exit(1, "cannot open: " + f);
			}
		}
		return SourceContext.newStringContext(""); // empty input
	}

	public final String getOutputFileName(SourceContext input, String ext) {
		return StringUtils.toFileName(input.getResourceName(), null, ext);
	}

	public final String getOutputFileName() {
		return this.OutputFileName;
	}

}
