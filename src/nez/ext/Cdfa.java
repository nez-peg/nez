package nez.ext;

import java.io.IOException;
import java.util.Scanner;

import nez.Grammar;
import nez.io.FileContext;
import nez.lang.GrammarFile;
import nez.lang.regex.RegularExpression;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;
import nez.x.dfa.DFAConverter;

public class Cdfa extends Command {

	@Override
	public String getDesc() {
		return "dfa converter";
	}

	static Grammar regexGrammar = null;
	static DFAConverter dfaConverter = null;
	static RegularExpression regexConverter = null;
	static GrammarFile convertedRegexGrammarFile = null;

	@Override
	public void exec(CommandContext config) throws IOException {
		// try {
		/*
		 * if (regexGrammar == null) { try { regexGrammar =
		 * GrammarFileLoader.loadGrammar("regex.nez", null, null); } catch
		 * (IOException e) { ConsoleUtils.exit(1, "can't load regex.nez"); } }
		 */
		/*
		 * // -i Parser p = regexGrammar.newParser("File"); SourceContext
		 * regexFile = config.nextInput(); CommonTree node =
		 * p.parseCommonTree(regexFile); System.out.println("tree = " + node);
		 * // System.out.println("start production = " +
		 * g.getStartProduction());
		 * 
		 * String filePath = regexFile.getResourceName();
		 * System.out.println("filePath = " + filePath);
		 * 
		 * GrammarFile gfile = GrammarFile.newGrammarFile("re", null);
		 * gfile.addProduction(node, "File", pi(node, null));
		 * 
		 * convertedRegexGrammarFile = GrammarFile.newGrammarFile(filePath,
		 * NezOption.newDefaultOption());
		 */
		// String filePath = null;
		/*
		 * if (config.hasInput()) { try { convertedRegexGrammarFile =
		 * RegexGrammar.loadGrammar(config.nextInput(),
		 * NezOption.newSafeOption()); } catch (IOException e) {
		 * ConsoleUtils.exit(1, "can't load grammar file"); } }
		 */
		/*
		 * if (config.hasInput()) { try { convertedRegexGrammarFile =
		 * RegularExpression.newGrammar(config.nextInput().toString(),
		 * NezOption.newSafeOption(), null).; } catch (IOException e) {
		 * ConsoleUtils.exit(1, "can't load grammar file"); } }
		 */
		/*
		 * if (convertedRegexGrammarFile == null) { ConsoleUtils.exit(1,
		 * "can't load grammar file"); }
		 */
		if (config.hasInput()) {
			String fileName = config.nextInput().getResourceName();
			FileContext fc = new FileContext(fileName);
			String regex = fc.substring(0, fc.length());
			System.out.println("regex = " + regex);
			Grammar g = RegularExpression.newGrammar(regex);
			dfaConverter = new DFAConverter(g, null);
		} else {
			ConsoleUtils.println("no input file");
		}
		// dfaConverter = new DFAConverter(convertedRegexGrammarFile, null);
		boolean printTime = false;
		Scanner in = new Scanner(System.in);
		ConsoleUtils.print(">>>");
		while (in.hasNext()) {
			String query = in.next();
			if (query.length() >= 2 && query.charAt(0) == ';' && query.charAt(1) == ';') {
				if (query.equals(";;toDOT")) {
					dfaConverter.convertBFAtoDOT();
					ConsoleUtils.println("|- fin -|");
				} else if (query.equals(";;prDOT")) {
					dfaConverter.printBFA();
					ConsoleUtils.println("|- fin -|");
				} else if (query.equals(";;switchBE")) {
					dfaConverter.switchShowBooleanExpression();
				} else if (query.equals(";;execTime")) {
					printTime = true;
				} else {
					ConsoleUtils.println("|- wrong query -|");
				}
			} else {
				long st = System.currentTimeMillis();
				ConsoleUtils.println(dfaConverter.exec(query) ? "accepted" : "rejected");
				long ed = System.currentTimeMillis();
				if (printTime) {
					System.out.println((ed - st) + "ms");
					printTime = false;
				}
			}
			ConsoleUtils.print(">>>");
		}
	}
}
