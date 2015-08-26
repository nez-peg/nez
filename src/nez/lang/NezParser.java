package nez.lang;

import java.io.IOException;
import java.util.ArrayList;

import nez.NezOption;
import nez.SourceContext;
import nez.ast.AbstractTree;
import nez.ast.CommonTree;
import nez.ast.AbstractTreeVisitor;
import nez.ast.Tag;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class NezParser extends AbstractTreeVisitor {
	Grammar nezGrammar;
	GrammarFile loaded;
	
	public NezParser() {
		this.nezGrammar = NezCombinator.newGrammar("Chunk", NezOption.newSafeOption());
	}
	
	public void eval(GrammarFile ns, String urn, int linenum, String text) {
		SourceContext sc = SourceContext.newStringSourceContext(urn, linenum, text);
		this.loaded = ns;
		while(sc.hasUnconsumed()) {
			AbstractTree<?> node = nezGrammar.parseCommonTree(sc);
			if(node == null) {
				ConsoleUtils.println(sc.getSyntaxErrorMessage());
			}
			if(!this.parseStatement(node)) {
				break;
			}
		}
	}

	public final void load(GrammarFile gfile, String urn) throws IOException {
		SourceContext sc = SourceContext.newFileContext(urn);
		this.loaded = gfile;
		while(sc.hasUnconsumed()) {
			CommonTree ast = nezGrammar.parseCommonTree(sc);
			if(ast == null) {
				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
			}
			if(!this.parseStatement(ast)) {
				break;
			}
		}
		gfile.verify();
	}

//	public final void load(GrammarFile ns, String urn) throws IOException {
//		load(ns, urn);
//	}
	
	private boolean parseStatement(AbstractTree<?> node) {
		System.out.println("DEBUG? parsed: " + node);
		if(node != null) {
			if(node.is(NezTag.Rule)) {
				parseProduction(node);
				return true;
			}
			if(node.is(NezTag.Example)) {
				return defineExample(node);
			}
			if(node.is(NezTag.Import)) {
				return importProduction(node);
			}
			if(node.is(NezTag.Format)) {
				return defineFormat(node);
			}
			Verbose.todo("undefined: " + node);
		}
		return false;
	}
	
	private boolean defineExample(AbstractTree<?> node) {
		if(node.size() == 2) {
			Example ex = new Example(node.get(0), node.get(1), true);
			this.loaded.addExample(ex);
		}
		else {
			Example ex = new Example(node.get(0), node.get(2), true);
			this.loaded.addExample(ex);
			ex = new Example(node.get(1), node.get(2), true);
			this.loaded.addExample(ex);
		}
		return true;
	}

	private boolean defineFormat(AbstractTree<?> node) {
		//System.out.println("node: " + node);
		String tag = node.getText(0, "token");
		int index = StringUtils.parseInt(node.getText(1, "*"), -1);
		Formatter fmt = toFormatter(node.get(2));
		this.loaded.addFormatter(tag, index, fmt);
		return true;
	}
	
	Formatter toFormatter(AbstractTree<?> node) {
		if(node.is(NezTag.List)) {
			ArrayList<Formatter> l = new ArrayList<Formatter>(node.size());
			for(AbstractTree<?> t : node) {
				l.add(toFormatter(t));
			}
			return Formatter.newFormatter(l);
		}
		if(node.is(NezTag.Integer)) {
			return Formatter.newFormatter(StringUtils.parseInt(node.toText(), 0));
		}
		if(node.is(NezTag.Format)) {
			int s = StringUtils.parseInt(node.getText(0, "*"), -1);
			int e = StringUtils.parseInt(node.getText(2, "*"), -1);
			Formatter fmt = toFormatter(node.get(1));
			return Formatter.newFormatter(s, fmt, e);
		}
		if(node.is(NezTag.Name)) {
			Formatter fmt = Formatter.newAction(node.toText());
			if(fmt == null) {
				loaded.reportWarning(node, "undefined formatter action");
				fmt = Formatter.newFormatter("${"+node.toText()+"}");
			}
			return fmt;
		}
		return Formatter.newFormatter(node.toText());
	}

	/* import */
	private boolean importProduction(AbstractTree<?> node) {
		System.out.println("DEBUG? parsed: " + node);
		String ns = null;
		String name = node.getText(0, "*");
		int loc = name.indexOf('.');
		if(loc >= 0) {
			ns = name.substring(0, loc);
			name = name.substring(loc+1);
		}
		String urn = path(node.getSource().getResourceName(), node.getText(1, ""));
		try {
			GrammarFile source = GrammarFile.loadNezFile(urn, NezOption.newDefaultOption());
			if(name.equals("*")) {
				int c = 0;
				for(String n : source.getNonterminalList()) {
					Production p = source.getProduction(n);
					if(p.isPublic()) {
						checkDuplicatedName(node.get(0));
						loaded.inportProduction(ns, p);
						c++;
					}
				}
				if(c == 0) {
					loaded.reportError(node.get(0), "nothing imported (no public production exisits)");
				}
			}
			else {
				Production p = source.getProduction(name);
				if(p == null) {
					loaded.reportError(node.get(0), "undefined production: " + name);
					return false;
				}
				loaded.inportProduction(ns, p);
			}
			return true;
		}
		catch(IOException e) {
			loaded.reportError(node.get(1), "unloaded: " + urn);
		}
		catch(NullPointerException e) {
			/* This is for a bug unhandling IOException at java.io.Reader.<init>(Reader.java:78) */
			loaded.reportError(node.get(1), "unloaded: " + urn);
		}
		return false;
	}

	private void checkDuplicatedName(AbstractTree<?> errorNode) {
		String name = errorNode.toText();
		if(loaded.hasProduction(name)) {
			loaded.reportWarning(errorNode, "duplicated production: " + name);
		}
	}

	private String path(String path, String path2) {
		if(path != null) {
			int loc = path.lastIndexOf('/');
			if(loc > 0) {
				return path.substring(0, loc+1) + path2;
			}
		}
		return path2;
	}

	private boolean binary = false;
	public Production parseProduction(AbstractTree<?> node) {
		String localName = node.getText(0, "");
		int productionFlag = 0;
		if(node.get(0).is(NezTag.String)) {
			localName = GrammarFile.nameTerminalProduction(localName);
			productionFlag |= Production.TerminalProduction;
		}
		this.binary = false;
		if(node.size() == 3) {
			AbstractTree<?> attrs = node.get(2);
			if(attrs.containsToken("binary")) {
				this.binary = true;
			}
			if(attrs.containsToken("public")) {
				productionFlag |= Production.PublicProduction;
			}
			if(attrs.containsToken("inline")) {
				productionFlag |= Production.InlineProduction;
			}
		}
		Production rule = loaded.getProduction(localName);
		if(rule != null) {
			loaded.reportWarning(node, "duplicated rule name: " + localName);
			rule = null;
		}
		Expression e = toExpression(node.get(1));
		//Verbose.debug("Parsed: " + node + "\n\t=>" + e);
		rule = loaded.defineProduction(node.get(0), productionFlag, localName, e);
		return rule;
	}

	Expression toExpression(AbstractTree<?> node) {
		return (Expression)this.visit(node);
	}
	
	public Expression toNonTerminal(AbstractTree<?> node) {
		String symbol = node.toText();
		return GrammarFactory.newNonTerminal(node, this.loaded, symbol);
	}

	public Expression toString(AbstractTree<?> node) {
		String name = GrammarFile.nameTerminalProduction(node.toText());
		return GrammarFactory.newNonTerminal(node, this.loaded, name);
	}

	public Expression toCharacter(AbstractTree<?> node) {
		return GrammarFactory.newString(node, StringUtils.unquoteString(node.toText()));
	}

	public Expression toClass(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		if(node.size() > 0) {
			for(int i = 0; i < node.size(); i++) {
				AbstractTree<?> o = node.get(i);
				if(o.is(NezTag.List)) {  // range
					l.add(GrammarFactory.newCharSet(node, o.getText(0, ""), o.getText(1, "")));
				}
				if(o.is(NezTag.Class)) {  // single
					l.add(GrammarFactory.newCharSet(node, o.toText(), o.toText()));
				}
			}
		}
		return GrammarFactory.newChoice(node, l);
	}

	public Expression toByte(AbstractTree<?> node) {
		String t = node.toText();
		if(t.startsWith("U+")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			c = (c * 16) + StringUtils.hex(t.charAt(4));
			c = (c * 16) + StringUtils.hex(t.charAt(5));
			if(c < 128) {
				return GrammarFactory.newByteChar(node, this.binary, c);					
			}
			String t2 = java.lang.String.valueOf((char)c);
			return GrammarFactory.newString(node, t2);
		}
		int c = StringUtils.hex(t.charAt(t.length()-2)) * 16 + StringUtils.hex(t.charAt(t.length()-1)); 
		return GrammarFactory.newByteChar(node, this.binary, c);
	}

	public Expression toAny(AbstractTree<?> node) {
		return GrammarFactory.newAnyChar(node, this.binary);
	}

	public Expression toChoice(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for(int i = 0; i < node.size(); i++) {
			GrammarFactory.addChoice(l, toExpression(node.get(i)));
		}
		return GrammarFactory.newChoice(node, l);
	}

	public Expression toSequence(AbstractTree<?> node) {
		UList<Expression> l = new UList<Expression>(new Expression[node.size()]);
		for(int i = 0; i < node.size(); i++) {
			GrammarFactory.addSequence(l, toExpression(node.get(i)));
		}
		return GrammarFactory.newSequence(node, l);
	}

	public Expression toNot(AbstractTree<?> node) {
		return GrammarFactory.newNot(node, toExpression(node.get(_expr)));
	}

	public Expression toAnd(AbstractTree<?> node) {
		return GrammarFactory.newAnd(node, toExpression(node.get(_expr)));
	}

	public Expression toOption(AbstractTree<?> node) {
		return GrammarFactory.newOption(node, toExpression(node.get(_expr)));
	}

	public Expression toRepetition1(AbstractTree<?> node) {
		return GrammarFactory.newRepetition1(node, toExpression(node.get(_expr)));
	}

	public Expression toRepetition(AbstractTree<?> node) {
		if(node.size() == 2) {
			int ntimes = StringUtils.parseInt(node.getText(1, ""), -1);
			if(ntimes != 1) {
				UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
				for(int i = 0; i < ntimes; i++) {
					GrammarFactory.addSequence(l, toExpression(node.get(0)));
				}
				return GrammarFactory.newSequence(node, l);
			}
		}
		return GrammarFactory.newRepetition(node, toExpression(node.get(_expr)));
	}

	// PEG4d TransCapturing

	public Expression toNew(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? GrammarFactory.newEmpty(node) : toExpression(exprNode);
		return GrammarFactory.newNew(node, false, null, p); 
	}

	public final static Tag _name = Tag.tag("name");
	public final static Tag _expr = Tag.tag("expr");

	private Tag parseLabelNode(AbstractTree<?> node) {
		Tag label = null;
		AbstractTree<?> labelNode = node.get(_name, null);
		if(labelNode != null) {
			label = Tag.tag(labelNode.toText());
		}
		return label;
	}
	
	public Expression toLeftNew(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		Expression p = (exprNode == null) ? GrammarFactory.newEmpty(node) : toExpression(exprNode);
		return GrammarFactory.newNew(node, true, parseLabelNode(node), p); 
	}

	public Expression toLink(AbstractTree<?> node) {
		return GrammarFactory.newLink(node, parseLabelNode(node), toExpression(node.get(_expr)));
	}

	public Expression toTagging(AbstractTree<?> node) {
		return GrammarFactory.newTagging(node, Tag.tag(node.toText()));
	}

	public Expression toReplace(AbstractTree<?> node) {
		return GrammarFactory.newReplace(node, node.toText());
	}

	public Expression toMatch(AbstractTree<?> node) {
		AbstractTree<?> exprNode = node.get(_expr, null);
		if(exprNode != null) {
			return GrammarFactory.newMatch(node, toExpression(exprNode));
		}
		return GrammarFactory.newMatchSymbol(node, parseLabelNode(node)); 
	}

//	public Expression toCatch(AST ast) {
//		return Factory.newCatch();
//	}
//
//	public Expression toFail(AST ast) {
//		return Factory.newFail(Utils.unquoteString(ast.textAt(0, "")));
//	}

	public Expression toIf(AbstractTree<?> node) {
		return GrammarFactory.newIfFlag(node, node.getText(_name, ""));
	}

	public Expression toOn(AbstractTree<?> node) {
		return GrammarFactory.newOnFlag(node, true, node.getText(_name, ""), toExpression(node.get(_expr)));
	}

//	public Expression toWith(AbstractTree<?> ast) {
//		return GrammarFactory.newOnFlag(ast, true, ast.getText(0, ""), toExpression(ast.get(1)));
//	}
//
//	public Expression toWithout(AbstractTree<?> ast) {
//		return GrammarFactory.newOnFlag(ast, false, ast.getText(0, ""), toExpression(ast.get(1)));
//	}

	public Expression toBlock(AbstractTree<?> node) {
		return GrammarFactory.newBlock(node, toExpression(node.get(_expr)));
	}

	public Expression toDef(AbstractTree<?> node) {
		return GrammarFactory.newDefSymbol(node, this.loaded, Tag.tag(node.getText(_name, "")), toExpression(node.get(_expr)));
	}

	public Expression toIs(AbstractTree<?> node) {
		return GrammarFactory.newIsSymbol(node, this.loaded, Tag.tag(node.getText(_name, "")));
	}

	public Expression toIsa(AbstractTree<?> node) {
		return GrammarFactory.newIsaSymbol(node, this.loaded, Tag.tag(node.getText(_name, "")));
	}

	public Expression toDefIndent(AbstractTree<?> ast) {
		return GrammarFactory.newDefIndent(ast);
	}

	public Expression toIndent(AbstractTree<?> ast) {
		return GrammarFactory.newIndent(ast);
	}

	public Expression toUndefined(AbstractTree<?> ast) {
		loaded.reportError(ast, "undefined or deprecated notation");
		return GrammarFactory.newEmpty(ast);
	}
		
}
