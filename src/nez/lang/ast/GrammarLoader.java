package nez.lang.ast;

import java.util.ArrayList;

import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.TransducerException;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.Formatter;
import nez.lang.Grammar;
import nez.lang.GrammarExample;
import nez.lang.GrammarFile;
import nez.lang.Production;
import nez.parser.ParserStrategy;
import nez.util.StringUtils;

public final class GrammarLoader extends GrammarVisitorMap<GrammarLoaderVisitor> {

	public GrammarLoader(Grammar grammar, ParserStrategy strategy) {
		super(grammar, strategy);
		this.init(GrammarLoader.class, new Undefined());
	}

	public void load(Tree<?> node) {
		try {
			find(node.getTag().getSymbol()).accept(node);
		} catch (TransducerException e) {
			e.getMessage();
		}
	}

	public class Undefined implements GrammarLoaderVisitor {
		@Override
		public void accept(Tree<?> node) {
			throw new TransducerException(node, "undefined " + node);
		}
	}

	public class _Source implements GrammarLoaderVisitor {
		@Override
		public void accept(Tree<?> node) {
			for (Tree<?> sub : node) {
				load(sub);
			}
		}
	}

	public class _Production implements GrammarLoaderVisitor, NezSymbols {
		ExpressionConstructor transducer = new NezExpressionConstructor(getGrammar(), getStrategy());

		@Override
		public void accept(Tree<?> node) {
			Tree<?> nameNode = node.get(_name);
			String localName = nameNode.toText();
			int productionFlag = 0;
			if (nameNode.is(_String)) {
				localName = GrammarFile.nameTerminalProduction(localName);
				productionFlag |= Production.TerminalProduction;
			}

			Production rule = getGrammar().getProduction(localName);
			if (rule != null) {
				reportWarning(node, "duplicated rule name: " + localName);
				rule = null;
			}
			Expression e = transducer.newInstance(node.get(_expr));
			rule = getGrammar().newProduction(node.get(0), productionFlag, localName, e);
		}
	}

	public final static Symbol _Integer = Symbol.tag("Integer");
	public final static Symbol _List = Symbol.tag("List");
	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _Format = Symbol.tag("Format");

	public final static Symbol _name = Symbol.tag("name");
	public final static Symbol _hash = Symbol.tag("hash"); // example
	public final static Symbol _name2 = Symbol.tag("name2"); // example
	public final static Symbol _text = Symbol.tag("text"); // example

	public class _Example extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String hash = node.getText(_hash, null);
			Tree<?> textNode = node.get(_text);
			Tree<?> nameNode = node.get(_name2, null);
			if (nameNode != null) {
				GrammarExample.add(getGrammar(), true, nameNode, hash, textNode);
				nameNode = node.get(_name);
				GrammarExample.add(getGrammar(), false, nameNode, hash, textNode);
			} else {
				nameNode = node.get(_name);
				GrammarExample.add(getGrammar(), true, nameNode, hash, textNode);
			}
		}
	}

	public class Format extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String tag = node.getText(0, "token");
			int index = StringUtils.parseInt(node.getText(1, "*"), -1);
			Formatter fmt = toFormatter(node.get(2));
			// getGrammarFile().addFormatter(tag, index, fmt);
		}
	}

	Formatter toFormatter(Tree<?> node) {
		if (node.is(_List)) {
			ArrayList<Formatter> l = new ArrayList<Formatter>(node.size());
			for (Tree<?> t : node) {
				l.add(toFormatter(t));
			}
			return Formatter.newFormatter(l);
		}
		if (node.is(_Integer)) {
			return Formatter.newFormatter(StringUtils.parseInt(node.toText(), 0));
		}
		if (node.is(_Format)) {
			int s = StringUtils.parseInt(node.getText(0, "*"), -1);
			int e = StringUtils.parseInt(node.getText(2, "*"), -1);
			Formatter fmt = toFormatter(node.get(1));
			return Formatter.newFormatter(s, fmt, e);
		}
		if (node.is(_Name)) {
			Formatter fmt = Formatter.newAction(node.toText());
			if (fmt == null) {
				this.reportWarning(node, "undefined formatter action");
				fmt = Formatter.newFormatter("${" + node.toText() + "}");
			}
			return fmt;
		}
		return Formatter.newFormatter(node.toText());
	}

	/**
	 * public class Import extends Undefined {
	 * 
	 * @Override public void accept(Tree<?> node) { String ns = null; String
	 *           name = node.getText(0, "*"); int loc = name.indexOf('.'); if
	 *           (loc >= 0) { ns = name.substring(0, loc); name =
	 *           name.substring(loc + 1); } String urn =
	 *           path(node.getSource().getResourceName(), node.getText(1, ""));
	 *           try { GrammarFile source = (GrammarFile)
	 *           GrammarFileLoader.loadGrammar(urn, strategy); if
	 *           (name.equals("*")) { int c = 0; for (Production p : source) {
	 *           if (p.isPublic()) { checkDuplicatedName(node.get(0));
	 *           getGrammarFile().importProduction(ns, p); c++; } } if (c == 0)
	 *           { reportError(node.get(0),
	 *           "nothing imported (no public production exisits)"); } } else {
	 *           Production p = source.getProduction(name); if (p == null) {
	 *           reportError(node.get(0), "undefined production: " + name); }
	 *           getGrammar().importProduction(ns, p); } } catch (IOException e)
	 *           { reportError(node.get(1), "unfound: " + urn); } catch
	 *           (NullPointerException e) { reportError(node.get(1), "unfound: "
	 *           + urn); } } }
	 * 
	 *           private void checkDuplicatedName(Tree<?> errorNode) { String
	 *           name = errorNode.toText(); if
	 *           (this.getGrammar().hasProduction(name)) {
	 *           this.reportWarning(errorNode, "duplicated production: " +
	 *           name); } }
	 * 
	 *           private String path(String path, String path2) { if (path !=
	 *           null) { int loc = path.lastIndexOf('/'); if (loc > 0) { return
	 *           path.substring(0, loc + 1) + path2; } } return path2; }
	 **/

	public String parseGrammarDescription(Source sc) {
		StringBuilder sb = new StringBuilder();
		long pos = 0;
		boolean found = false;
		for (; pos < sc.length(); pos++) {
			int ch = sc.byteAt(pos);
			if (Character.isAlphabetic(ch)) {
				found = true;
				break;
			}
		}
		if (found) {
			for (; pos < sc.length(); pos++) {
				int ch = sc.byteAt(pos);
				if (ch == '\n' || ch == '\r' || ch == '-' || ch == '*') {
					break;
				}
				sb.append((char) ch);
			}
		}
		return sb.toString().trim();
	}

}