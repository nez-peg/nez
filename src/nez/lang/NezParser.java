package nez.lang;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import nez.SourceContext;
import nez.ast.CommonTree;
import nez.ast.CommonTreeVisitor;
import nez.ast.Source;
import nez.ast.Tag;
import nez.main.Verbose;
import nez.util.ConsoleUtils;
import nez.util.StringUtils;
import nez.util.UList;

public class NezParser extends CommonTreeVisitor {
	Grammar nezGrammar;
	NameSpace loaded;
	GrammarChecker checker;
	
	public NezParser() {
		this.nezGrammar = NezCombinator.newGrammar("Chunk", Grammar.SafeOption);
	}
	
	public void eval(NameSpace ns, String urn, int linenum, String text) {
		SourceContext sc = SourceContext.newStringSourceContext(urn, linenum, text);
		this.loaded = ns;
		this.checker = new GrammarChecker();
		while(sc.hasUnconsumed()) {
			CommonTree ast = nezGrammar.parse(sc);
			if(ast == null) {
				ConsoleUtils.println(sc.getSyntaxErrorMessage());
			}
			if(!this.parseStatement(ast)) {
				break;
			}
		}
	}

	public final void load(NameSpace ns, String urn, GrammarChecker checker) throws IOException {
		SourceContext sc = SourceContext.newFileContext(urn);
		this.loaded = ns;
		this.checker = checker;
		while(sc.hasUnconsumed()) {
			CommonTree ast = nezGrammar.parse(sc);
			if(ast == null) {
				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
			}
			if(!this.parseStatement(ast)) {
				break;
			}
		}
		checker.verify(ns);
	}

	public final void load(NameSpace ns, String urn) throws IOException {
		load(ns, urn, new GrammarChecker());
	}

//	public final NameSpace loadGrammar(SourceContext sc, GrammarChecker checker) {
//		this.loaded = NameSpace.newNameSpace(sc.getResourceName());
//		this.checker = checker;
//		while(sc.hasUnconsumed()) {
//			CommonTree ast = nezGrammar.parse(sc);
//			if(ast == null) {
//				ConsoleUtils.exit(1, sc.getSyntaxErrorMessage());
//			}
//			if(!this.parseStatement(ast)) {
//				break;
//			}
//		}
//		checker.verify(loaded);
//		return loaded;
//	}
//
//	public final NameSpace loadGrammar(SourceContext sc) {
//		return this.loadGrammar(sc, new GrammarChecker());
//	}

	
	private boolean parseStatement(CommonTree node) {
		//System.out.println("DEBUG? parsed: " + node);
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
	
	private boolean defineExample(CommonTree node) {
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

	private boolean defineFormat(CommonTree node) {
		//System.out.println("node: " + node);
		String tag = node.textAt(0, "token");
		int index = StringUtils.parseInt(node.textAt(1, "*"), -1);
		Formatter fmt = toFormatter(node.get(2));
		this.loaded.addFormatter(tag, index, fmt);
		return true;
	}
	
	Formatter toFormatter(CommonTree node) {
		if(node.is(NezTag.List)) {
			ArrayList<Formatter> l = new ArrayList<Formatter>(node.size());
			for(CommonTree t : node) {
				l.add(toFormatter(t));
			}
			return Formatter.newFormatter(l);
		}
		if(node.is(NezTag.Integer)) {
			return Formatter.newFormatter(StringUtils.parseInt(node.getText(), 0));
		}
		if(node.is(NezTag.Format)) {
			int s = StringUtils.parseInt(node.textAt(0, "*"), -1);
			int e = StringUtils.parseInt(node.textAt(2, "*"), -1);
			Formatter fmt = toFormatter(node.get(1));
			return Formatter.newFormatter(s, fmt, e);
		}
		if(node.is(NezTag.Name)) {
			Formatter fmt = Formatter.newAction(node.getText());
			if(fmt == null) {
				checker.reportWarning(node, "undefined formatter action");
				fmt = Formatter.newFormatter("${"+node.getText()+"}");
			}
			return fmt;
		}
		return Formatter.newFormatter(node.getText());
	}

	/* import */
	private boolean importProduction(CommonTree node) {
		System.out.println("DEBUG? parsed: " + node);
		String ns = null;
		String name = node.textAt(0, "*");
		int loc = name.indexOf('.');
		if(loc >= 0) {
			ns = name.substring(0, loc);
			name = name.substring(loc+1);
		}
		String urn = path(node.getSource().getResourceName(), node.textAt(1, ""));
		try {
			NameSpace source = NameSpace.loadNezFile(urn);
			if(name.equals("*")) {
				int c = 0;
				for(String n : source.getNonterminalList()) {
					Production p = source.getProduction(n);
					if(p.isPublic) {
						checkDuplicatedName(node.get(0));
						loaded.inportProduction(ns, p);
						c++;
					}
				}
				if(c == 0) {
					checker.reportError(node.get(0), "nothing imported (no public production exisits)");
				}
			}
			else {
				Production p = source.getProduction(name);
				if(p == null) {
					checker.reportError(node.get(0), "undefined production: " + name);
					return false;
				}
				loaded.inportProduction(ns, p);
			}
			return true;
		}
		catch(IOException e) {
			checker.reportError(node.get(1), "unloaded: " + urn);
		}
		catch(NullPointerException e) {
			/* This is for a bug unhandling IOException at java.io.Reader.<init>(Reader.java:78) */
			checker.reportError(node.get(1), "unloaded: " + urn);
		}
		return false;
	}

	private void checkDuplicatedName(CommonTree errorNode) {
		String name = errorNode.getText();
		if(loaded.hasProduction(name)) {
			checker.reportWarning(errorNode, "duplicated production: " + name);
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

	
	public Production parseProduction(CommonTree node) {
		String localName = node.textAt(0, "");
		boolean isTerminal = false;
		if(node.get(0).is(NezTag.String)) {
			localName = NameSpace.nameTerminalProduction(localName);
			isTerminal = true;
		}
		Production rule = loaded.getProduction(localName);
		if(rule != null) {
			checker.reportWarning(node, "duplicated rule name: " + localName);
			rule = null;
		}

		Expression e = toExpression(node.get(1));
		rule = loaded.defineProduction(node.get(0), localName, e);
		rule.isTerminal = isTerminal;
		if(node.size() == 3) {
			CommonTree attrs = node.get(2);
			if(attrs.containsToken("public")) {
				rule.isPublic = true;
			}
			if(attrs.containsToken("inline")) {
				rule.isInline = true;
			}
		}
		return rule;
	}


	Expression toExpression(CommonTree po) {
		return (Expression)this.visit(po);
	}
	
	public Expression toNonTerminal(CommonTree ast) {
		String symbol = ast.getText();
		return Factory.newNonTerminal(ast, this.loaded, symbol);
	}

	public Expression toString(CommonTree ast) {
		String name = NameSpace.nameTerminalProduction(ast.getText());
		return Factory.newNonTerminal(ast, this.loaded, name);
	}

	public Expression toCharacter(CommonTree ast) {
		return Factory.newString(ast, StringUtils.unquoteString(ast.getText()));
	}

	public Expression toClass(CommonTree ast) {
		UList<Expression> l = new UList<Expression>(new Expression[2]);
		if(ast.size() > 0) {
			for(int i = 0; i < ast.size(); i++) {
				CommonTree o = ast.get(i);
				if(o.is(NezTag.List)) {  // range
					l.add(Factory.newCharSet(ast, o.textAt(0, ""), o.textAt(1, "")));
				}
				if(o.is(NezTag.Class)) {  // single
					l.add(Factory.newCharSet(ast, o.getText(), o.getText()));
				}
			}
		}
		return Factory.newChoice(ast, l);
	}

	public Expression toByte(CommonTree ast) {
		String t = ast.getText();
		if(t.startsWith("U+")) {
			int c = StringUtils.hex(t.charAt(2));
			c = (c * 16) + StringUtils.hex(t.charAt(3));
			c = (c * 16) + StringUtils.hex(t.charAt(4));
			c = (c * 16) + StringUtils.hex(t.charAt(5));
			if(c < 128) {
				return Factory.newByteChar(ast, c);					
			}
			String t2 = java.lang.String.valueOf((char)c);
			return Factory.newString(ast, t2);
		}
		int c = StringUtils.hex(t.charAt(t.length()-2)) * 16 + StringUtils.hex(t.charAt(t.length()-1)); 
		return Factory.newByteChar(ast, c);
	}

	public Expression toAny(CommonTree ast) {
		return Factory.newAnyChar(ast);
	}

	public Expression toChoice(CommonTree ast) {
		UList<Expression> l = new UList<Expression>(new Expression[ast.size()]);
		for(int i = 0; i < ast.size(); i++) {
			Factory.addChoice(l, toExpression(ast.get(i)));
		}
		return Factory.newChoice(ast, l);
	}

	public Expression toSequence(CommonTree ast) {
		UList<Expression> l = new UList<Expression>(new Expression[ast.size()]);
		for(int i = 0; i < ast.size(); i++) {
			Factory.addSequence(l, toExpression(ast.get(i)));
		}
		return Factory.newSequence(ast, l);
	}

	public Expression toNot(CommonTree ast) {
		return Factory.newNot(ast, toExpression(ast.get(0)));
	}

	public Expression toAnd(CommonTree ast) {
		return Factory.newAnd(ast, toExpression(ast.get(0)));
	}

	public Expression toOption(CommonTree ast) {
		return Factory.newOption(ast, toExpression(ast.get(0)));
	}

	public Expression toRepetition1(CommonTree ast) {
		if(Expression.ClassicMode) {
			UList<Expression> l = new UList<Expression>(new Expression[2]);
			l.add(toExpression(ast.get(0)));
			l.add(Factory.newRepetition(ast, toExpression(ast.get(0))));
			return Factory.newSequence(ast, l);
		}
		else {
			return Factory.newRepetition1(ast, toExpression(ast.get(0)));
		}
	}

	public Expression toRepetition(CommonTree ast) {
		if(ast.size() == 2) {
			int ntimes = StringUtils.parseInt(ast.textAt(1, ""), -1);
			if(ntimes != 1) {
				UList<Expression> l = new UList<Expression>(new Expression[ntimes]);
				for(int i = 0; i < ntimes; i++) {
					Factory.addSequence(l, toExpression(ast.get(0)));
				}
				return Factory.newSequence(ast, l);
			}
		}
		return Factory.newRepetition(ast, toExpression(ast.get(0)));
	}

	// PEG4d TransCapturing

	public Expression toNew(CommonTree ast) {
		Expression p = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
		return Factory.newNew(ast, false, p);
	}

	public Expression toLeftNew(CommonTree ast) {
		Expression p = (ast.size() == 0) ? Factory.newEmpty(ast) : toExpression(ast.get(0));
		return Factory.newNew(ast, true, p);//		}
	}

	public Expression toLink(CommonTree ast) {
		int index = -1;
		if(ast.size() == 2) {
			index = StringUtils.parseInt(ast.textAt(1, ""), -1);
		}
		return Factory.newLink(ast, toExpression(ast.get(0)), index);
	}

	public Expression toTagging(CommonTree ast) {
		return Factory.newTagging(ast, Tag.tag(ast.getText()));
	}

	public Expression toReplace(CommonTree ast) {
		return Factory.newReplace(ast, ast.getText());
	}

	//PEG4d Function
	
//	public Expression toDebug(AST ast) {
//		return Factory.newDebug(toExpression(ast.get(0)));
//	}

	public Expression toMatch(CommonTree ast) {
		return Factory.newMatch(ast, toExpression(ast.get(0)));
	}

//	public Expression toCatch(AST ast) {
//		return Factory.newCatch();
//	}
//
//	public Expression toFail(AST ast) {
//		return Factory.newFail(Utils.unquoteString(ast.textAt(0, "")));
//	}

	public Expression toIf(CommonTree ast) {
		return Factory.newIfFlag(ast, ast.textAt(0, ""));
	}

	public Expression toOn(CommonTree ast) {
		return Factory.newOnFlag(ast, true, ast.textAt(0, ""), toExpression(ast.get(1)));
	}

	public Expression toWith(CommonTree ast) {
		return Factory.newOnFlag(ast, true, ast.textAt(0, ""), toExpression(ast.get(1)));
	}

	public Expression toWithout(CommonTree ast) {
		return Factory.newOnFlag(ast, false, ast.textAt(0, ""), toExpression(ast.get(1)));
	}

	public Expression toBlock(CommonTree ast) {
		return Factory.newBlock(ast, toExpression(ast.get(0)));
	}

	public Expression toDef(CommonTree ast) {
		return Factory.newDefSymbol(ast, this.loaded, Tag.tag(ast.textAt(0, "")), toExpression(ast.get(1)));
	}

	public Expression toIs(CommonTree ast) {
		return Factory.newIsSymbol(ast, this.loaded, Tag.tag(ast.textAt(0, "")));
	}

	public Expression toIsa(CommonTree ast) {
		return Factory.newIsaSymbol(ast, this.loaded, Tag.tag(ast.textAt(0, "")));
	}

	public Expression toDefIndent(CommonTree ast) {
		return Factory.newDefIndent(ast);
	}

	public Expression toIndent(CommonTree ast) {
		return Factory.newIndent(ast);
	}

	public Expression toUndefined(CommonTree ast) {
		checker.reportError(ast, "undefined or deprecated notation");
		return Factory.newEmpty(ast);
	}
	
//	public Expression toScan(AST ast) {
//		return Factory.newScan(Integer.parseInt(ast.get(0).getText()), toExpression(ast.get(1)), toExpression(ast.get(2)));
//	}
//	
//	public Expression toRepeat(AST ast) {
//		return Factory.newRepeat(toExpression(ast.get(0)));
//	}
	
}
