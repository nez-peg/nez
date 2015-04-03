//package nez.x;
//
//import nez.Grammar;
//import nez.Production;
//import nez.SourceContext;
//import nez.ast.CommonTree;
//import nez.main.Command;
//import nez.main.CommandConfigure;
//import nez.main.Recorder;
//import nez.util.ConsoleUtils;
//import nez.util.UList;
//
//public class DTDCommand extends Command {
//
//	final static String DTDNEZ = "sample/xmldtd.nez";
//	private String DTDFile;
//	private String inputXMLFile;
//	private SourceContext file;
//
//	@Override
//	public void exec(CommandConfigure config) {
//		setupConfig(config);
//		Recorder rec = config.getRecorder();
//		Production p = config.getProduction(config.StartingPoint);
//		CommonTree dtdNode = parse(config, rec, p, false);
//		String outputfile = config.getOutputFileName();
//		if (outputfile == null) {
//			outputfile = file.getResourceName() + ".nez";
//			int index = outputfile.indexOf("/");
//			while (index > -1) {
//				outputfile = outputfile.substring(index + 1);
//				index = outputfile.indexOf("/");
//			}
//			outputfile = "gen/" + outputfile;
//		}
//		DTDConverter conv = new DTDConverter(new Grammar(file.getResourceName()), outputfile);
//		conv.convert(dtdNode);
//		config.GrammarFile = outputfile;
//		config.InputFileLists = new UList<String>(new String[1]);
//		config.InputFileLists.add(inputXMLFile);
//		rec = config.getRecorder();
//		p = conv.grammar.newProduction(config.StartingPoint);
//		parse(config, rec, p, true);
//	}
//
//	private final void setupConfig(CommandConfigure config) {
//		DTDFile = config.GrammarFile;
//		inputXMLFile = config.InputFileLists.get(0);
//		config.InputFileLists = new UList<String>(new String[1]);
//		config.InputFileLists.add(DTDFile);
//		config.GrammarFile = DTDNEZ;
//
//	}
//
//	private CommonTree parse(CommandConfigure config, Recorder rec, Production p, boolean validation) {
//		if (p == null) {
//			ConsoleUtils.exit(1, "undefined nonterminal: " + config.StartingPoint);
//		}
//		p.record(rec);
//		file = config.getInputSourceContext();
//		file.start(rec);
//		CommonTree node = p.parse(file);
//		file.done(rec);
//		if (node == null) {
//			ConsoleUtils.println(file.getSyntaxErrorMessage());
//		}
//		if (file.hasUnconsumed()) {
//			ConsoleUtils.println(file.getUnconsumedMessage());
//			ConsoleUtils.println("\n invalid XML(DTD) file");
//		}
//		if (rec != null) {
//			rec.log();
//		}
//		if (validation) {
//			ConsoleUtils.println("\n valid XML file");
//		}
//		return node;
//	}
//
//	@Override
//	public String getDesc() {
//		// TODO Auto-generated method stub
//		return null;
//	}
//}
