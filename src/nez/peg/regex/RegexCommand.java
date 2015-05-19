package nez.peg.regex;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.lang.Grammar;
import nez.lang.NameSpace;
import nez.main.Command;
import nez.main.CommandConfigure;
import nez.main.Recorder;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class RegexCommand extends Command {
	SourceContext file;
	String NezFile = "sample/regex.nez";
	String RegexFile;
	UList<String> inputFileList;

	@Override
	public String getDesc() {
		return "regex";
	}

	@Override
	public void exec(CommandConfigure config) {
		init(config);
		Recorder rec = config.getRecorder();
		Grammar p = config.getGrammar(config.StartingPoint);
		CommonTree node = parse(config, rec, p, false);
		String outputfile = config.getOutputFileName();
		if (outputfile == null) {
			outputfile = file.getResourceName() + ".nez";
			int index = outputfile.indexOf("/");
			while(index > -1) {
				outputfile = outputfile.substring(index+1);
				index = outputfile.indexOf("/");
			}
			outputfile = "gen/" + outputfile;
		}
		GrammarConverter conv = new RegexConverter(NameSpace.newNameSpace(file.getResourceName()), outputfile);
		conv.convert(node);
		config.GrammarFile = outputfile;
		config.setInputFileList(inputFileList);
		rec = config.getRecorder();
		p = conv.grammar.newGrammar(config.StartingPoint, Grammar.RegexOption);
		parse(config, rec, p, true);
	}
	
	private void init(CommandConfigure config) {
		RegexFile = config.GrammarFile;
		config.GrammarFile = NezFile;
		inputFileList = config.InputFileLists;
		config.InputFileLists = new UList<String>(new String[1]);
		config.InputFileLists.add(RegexFile);
	}

	private CommonTree parse(CommandConfigure config, Recorder rec, Grammar p, boolean writeAST) {
		if(p == null) {
			ConsoleUtils.exit(1, "undefined nonterminal: " + config.StartingPoint);
		}
		p.record(rec);
		CommonTree node = null;
		while(config.hasInput()) {
			file = config.getInputSourceContext();
			file.start(rec);
			node = p.parse(file);
			file.done(rec);
			if(node == null) {
				ConsoleUtils.println(file.getSyntaxErrorMessage());
				continue;
			}
			if(file.hasUnconsumed()) {
				ConsoleUtils.println(file.getUnconsumedMessage());
			}
			if(rec != null) {
				rec.log();
			}
			if (writeAST) {
				//trans.transform(config.getOutputFileName(file), node);
				ConsoleUtils.println("nez: match");
			}
		}
		return node;
	}
}
