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
import nez.lang.Grammar;
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

public class MouseGrammarGenerator extends NezGenerator {
	public MouseGrammarGenerator(String filename) {
		super(filename);
	}
	
	public MouseGrammarGenerator() {
		super();
	}
	
	@Override
	public String getDesc() {
		return "a PEG-style grammar for Mouse" ;
	}
	
	@Override
	public void makeHeader(Grammar g) {
		file.write("// Parsing Expression Grammars for Mouse");
		file.writeIndent("// Translated from Nez");
	}

	String stringfyName(String s) {
		if(s.equals("_")) {
			return "SPACING";
		}
		return s;
	}
	
	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		file.writeIndent(stringfyName(rule.getLocalName().replaceAll("_", "under")));
		file.incIndent();
		file.writeIndent("= ");
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					file.writeIndent("/ ");
				}
				visit(e.get(i));
			}
		}
		else {
			visit(e);
		}
		file.writeIndent(";");
		file.decIndent();
	}	
	
	public void visitEmpty(Empty e) {
		file.write("\"\"");
	}

	public void visitFailure(Failure e) {
		file.write("!_");
	}

	public void visitNonTerminal(NonTerminal e) {
		file.write(stringfyName(e.getLocalName().replaceAll("_", "under")));
	}
	
	public void visitByteChar(ByteChar e) {
		file.write(stringfy("\"", e.byteChar, "\""));
	}

	public void visitByteMap(ByteMap e) {
		file.write(stringfy(e.byteMap));
	}
	
	public void visitAnyChar(AnyChar e) {
		file.write("_");
	}

	private final String stringfy(String s, int ch, String e) {
		char c = (char)ch;
		switch(c) {
		case '\n' : return s+ "\\n" + e; 
		case '\t' : return s+ "\\t" + e; 
		case '\r' : return s+ "\\r" + e; 
		case '"' : return s+ "\\\"" + e; 
		case '\\' : return s + "\\\\" + e; 
		}
		if(Character.isISOControl(c) || c > 127) {
			return s + String.format("0x%02x", (int)c) + e;
		}
		return(s + c + e);
	}
	
	private final String stringfy(boolean[] b) {
		StringBuilder sb = new StringBuilder();
		String delim = "";
		for(int s = 0; s < 256; s++) {
			if(b[s]) {
				int e = searchEndChar(b, s+1);
				if(s == e) {
					sb.append(delim);
					sb.append(stringfy("\"",s,"\""));
					delim = " / ";
				}
				else {
					sb.append(delim);
					sb.append("[");
					sb.append(stringfy("",s,""));
					sb.append("-");
					sb.append(stringfy("",e,""));
					sb.append("]");
					delim = " / ";
					s = e;
				}
			}
		}
		return sb.toString();
	}

	private final static int searchEndChar(boolean[] b, int s) {
		for(; s < 256; s++) {
			if(!b[s]) {
				return s-1;
			}
		}
		return 255;
	}

	protected void visit(String prefix, Unary e, String suffix) {
		if(prefix != null) {
			file.write(prefix);
		}
		if(/*e.get(0) instanceof String ||*/ e.get(0) instanceof NonTerminal/* || e.get(0) instanceof NewClosure*/) {
			this.visit(e.get(0));
		}
		else {
			file.write("(");
			this.visit(e.get(0));
			file.write(")");
		}
		if(suffix != null) {
			file.write(suffix);
		}
	}

	public void visitOption(Option e) {
		this.visit( null, e, "?");
	}
	
	public void visitRepetition(Repetition e) {
		this.visit(null, e, "*");
	}
	
	public void visitRepetition1(Repetition1 e) {
		this.visit(null, e, "+");
	}

	public void visitAnd(And e) {
		this.visit( "&", e, null);
	}
	
	public void visitNot(Not e) {
		this.visit( "!", e, null);
	}

	protected void visitSequenceImpl(Multinary l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				file.write(" ");
			}
			int n = appendAsString(l, i);
			if(n > i) {
				i = n;
				continue;
			}
			Expression e = l.get(i);
			if(e instanceof Choice || e instanceof Sequence) {
				file.write("( ");
				visit(e);
				file.write(" )");
				continue;
			}
			visit(e);
		}
	}

	private int appendAsString(Multinary l, int start) {
		int end = l.size();
		String s = "";
		for(int i = start; i < end; i++) {
			Expression e = l.get(i);
			if(e instanceof ByteChar) {
				char c = (char)(((ByteChar) e).byteChar);
				if(c >= ' ' && c < 127) {
					s += c;
					continue;
				}
			}
			end = i;
			break;
		}
		if(s.length() > 1) {
			file.write(StringUtils.quoteString('"', s, '"'));
		}
		return end - 1;
	}
	
	public void visitSequence(Sequence e) {
		this.visitSequenceImpl(e);
	}
	
	public void visitChoice(Choice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				file.write(" / ");
			}
			visit(e.get(i));
		}
	}
	
	public void visitNew(New e) {

	}

	public void visitCapture(Capture e) {
		
	}

	public void visitTagging(Tagging e) {
//		file.write("{");
//		file.write(e.tag.toString().toLowerCase());
//		file.write("}");
	}
	
	public void visitValue(Replace e) {
		//file.write(StringUtils.quoteString('`', e.value, '`'));
	}
	
	public void visitLink(Link e) {
//		String predicate = "@";
//		if(e.index != -1) {
//			predicate += "[" + e.index + "]";
//		}
//		this.visit(predicate, e, null);
		this.visit(e.get(0));
	}

	@Override
	public void visitUndefined(Expression e) {
		file.write("/* Mouse Unsupported <");
		file.write(e.getPredicate());
		for(Expression se : e) {
			file.write(" ");
			visit(se);
		}
		file.write("> */");
	}

}
