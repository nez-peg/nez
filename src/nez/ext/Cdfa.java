package nez.ext;

import java.io.IOException;
import java.util.Scanner;

import nez.Grammar;
import nez.io.FileContext;
import nez.lang.GrammarFile;
import nez.lang.GrammarFileLoader;
import nez.lang.regex.RegularExpression;
import nez.main.Command;
import nez.main.CommandContext;
import nez.util.ConsoleUtils;
import nez.x.dfa.DFAConverter;
import nez.x.dfa.DFAValidator;

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
		Grammar g;
		if (config.hasInput()) {
			String fileName = config.nextInput().getResourceName();
			FileContext fc = new FileContext(fileName);
			String regex = fc.substring(0, fc.length());
			System.out.println("regex = " + regex);
			g = RegularExpression.newGrammar(regex);
			dfaConverter = new DFAConverter(g, null);
		} else {
			ConsoleUtils.println("no input file");
			return;
		}
		boolean inputState = false; // false -> stdin, true -> file
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
				} else if (query.equals(";;switchIS")) {
					inputState = !inputState;
				} else if (query.equals(";;validate")) {
					DFAValidator dfavalidator = new DFAValidator(g);
					System.out.println("Convertible grammar ? " + (dfavalidator.convertible() ? "YES" : "NO"));
				} else if (query.equals(";;validateNez")) {
					ConsoleUtils.print("file name : ");
					String fileName = in.next();
					// FileContext fc = new FileContext(fileName);
					// String ctx = fc.substring(0, fc.length());
					// System.out.println("file context : " + ctx);
					Grammar g2 = GrammarFileLoader.loadGrammar(fileName, null, null);
					if (g2 == null) {
						ConsoleUtils.println("invalid file");
						ConsoleUtils.print(">>>");
						continue;
					}
					/*
					 * System.out.println(g2.getStartProduction()); for
					 * (Production p : g2.getProductionList()) {
					 * System.out.println(p.getExpression()); }
					 */
					DFAValidator dfavalidator = new DFAValidator(g2);
					System.out.println("Convertible grammar ? " + (dfavalidator.convertible() ? "YES" : "NO"));
				} else {
					ConsoleUtils.println("|- wrong query -|");
				}
			} else {
				if (inputState) {
					FileContext fc = new FileContext(query);
					query = "";
					for (int i = 0; i < fc.length(); i++) {
						query += (char) fc.byteAt(i);
					}
					System.out.println("file input = |" + query + "|");
				}
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
