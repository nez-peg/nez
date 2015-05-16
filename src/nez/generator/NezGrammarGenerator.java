package nez.generator;

import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.Empty;
import nez.lang.Expression;
import nez.lang.Failure;
import nez.lang.Link;
import nez.lang.New;
import nez.lang.NonTerminal;
import nez.lang.Not;
import nez.lang.Option;
import nez.lang.Production;
import nez.lang.Repetition;
import nez.lang.Repetition1;
import nez.lang.Replace;
import nez.lang.Sequence;
import nez.lang.Multinary;
import nez.lang.Tagging;
import nez.lang.Unary;
import nez.util.StringUtils;

public class NezGrammarGenerator extends GrammarGenerator {
	@Override
	public String getDesc() {
		return "a Nez grammar" ;
	}

	public NezGrammarGenerator() {
		super(null);
	}

	public NezGrammarGenerator(String fileName) {
		super(fileName);
	}
	
	@Override
	public void makeHeader() {
		L("// Parsing Expression Grammars for Nez");
		L("// ");
	}

	String stringfyName(String s) {
		return s;
	}
	
	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		if(rule.isPublic()) {
			L("public ");
			W(stringfyName(rule.getLocalName()));
		}
		else {
			L(stringfyName(rule.getLocalName()));
		}
		inc();
		L("= ");
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					L("/ ");
				}
				visit(e.get(i));
			}
		}
		else {
			visit(e);
		}
		dec();
	}	
	
	public void visitEmpty(Empty e) {
		W("''");
	}

	public void visitFailure(Failure e) {
		W("!''");
	}

	public void visitNonTerminal(NonTerminal e) {
		W(stringfyName(e.getLocalName()));
	}
	
	public void visitByteChar(ByteChar e) {
		W(StringUtils.stringfyByte(e.byteChar));
	}

	public void visitByteMap(ByteMap e) {
		W(StringUtils.stringfyCharClass(e.byteMap));
	}
	
	public void visitAnyChar(AnyChar e) {
		W(".");
	}

	public void visitOption(Option e) {
		Unary( null, e, "?");
	}
	
	public void visitRepetition(Repetition e) {
		Unary(null, e, "*");
	}
	
	public void visitRepetition1(Repetition1 e) {
		Unary(null, e, "+");
	}

	public void visitAnd(And e) {
		Unary( "&", e, null);
	}
	
	public void visitNot(Not e) {
		Unary( "!", e, null);
	}
	
	public void visitChoice(Choice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				W(" / ");
			}
			visit(e.get(i));
		}
	}
	
	public void visitNew(New e) {
		W(e.lefted ? "{@" : "{");
	}

	public void visitCapture(Capture e) {
		W("}");
	}

	public void visitTagging(Tagging e) {
		W("#");
		W(e.tag.getName());
	}
	
	public void visitValue(Replace e) {
		W(StringUtils.quoteString('`', e.value, '`'));
	}
	
	public void visitLink(Link e) {
		String predicate = "@";
		if(e.index != -1) {
			predicate += "[" + e.index + "]";
		}
		Unary(predicate, e, null);
	}

	@Override
	public void visitUndefined(Expression e) {
		W("<");
		W(e.getPredicate());
		for(Expression se : e) {
			W(" ");
			visit(se);
		}
		W(">");
	}

}
