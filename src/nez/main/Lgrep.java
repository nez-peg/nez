//package nez.main;
//
//import nez.NezOption;
//import nez.SourceContext;
//import nez.ast.CommonTree;
//import nez.lang.Grammar;
//import nez.lang.NameSpace;
//import nez.peg.regex.GrammarConverter;
//import nez.peg.regex.RegexConverter;
//import nez.util.ConsoleUtils;
//import nez.util.UList;
//
//public class Lgrep extends Command {
//	SourceContext file;
//	String NezFile = "sample/regex.nez";
//	String RegexFile;
//	UList<String> inputFileList;
//
//	@Override
//	public String getDesc() {
//		return "regex";
//	}
//
//	@Override
//	public void exec(CommandConfigulation config) {
//		init(config);
//		Recorder rec = config.getRecorder();
//		Grammar p = config.getGrammar();
//		CommonTree node = parse(config, rec, p, false);
//		String outputfile = config.getOutputFileName();
//		if (outputfile == null) {
//			outputfile = file.getResourceName() + ".nez";
//			int index = outputfile.indexOf("/");
//			while(index > -1) {
//				outputfile = outputfile.substring(index+1);
//				index = outputfile.indexOf("/");
//			}
//			outputfile = "gen/" + outputfile;
//		}
//		GrammarConverter conv = new RegexConverter(NameSpace.newNameSpace(file.getResourceName()), outputfile);
//		conv.convert(node);
//		config.grammarFile = outputfile;
//		config.setInputFileList(inputFileList);
//		rec = config.getRecorder();
//		p = conv.grammar.newGrammar(config.StartingPoint, NezOption.RegexOption);
//		parse(config, rec, p, true);
//	}
//	
//	private void init(CommandConfigulation config) {
//		RegexFile = config.grammarFile;
//		config.grammarFile = NezFile;
//		inputFileList = config.inputFileLists;
//		config.inputFileLists = new UList<String>(new String[1]);
//		config.inputFileLists.add(RegexFile);
//	}
//
//	private CommonTree parse(CommandConfigulation config, Recorder rec, Grammar p, boolean writeAST) {
//		p.record(rec);
//		CommonTree node = null;
//		while(config.hasInput()) {
//			file = config.getInputSourceContext();
//			file.start(rec);
//			node = p.parse(file);
//			file.done(rec);
//			if(node == null) {
//				ConsoleUtils.println(file.getSyntaxErrorMessage());
//				continue;
//			}
//			if(file.hasUnconsumed()) {
//				ConsoleUtils.println(file.getUnconsumedMessage());
//			}
//			if(rec != null) {
//				rec.log();
//			}
//			if (writeAST) {
//				//trans.transform(config.getOutputFileName(file), node);
//				ConsoleUtils.println("nez: match");
//			}
//		}
//		return node;
//	}
//}
