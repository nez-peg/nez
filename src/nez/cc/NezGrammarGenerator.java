package nez.cc;

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
import nez.lang.SequentialExpression;
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
		file.writeIndent("// Parsing Expression Grammars for Nez");
		file.writeIndent("// ");
	}

	String stringfyName(String s) {
		return s;
	}
	
	@Override
	public void visitProduction(Production rule) {
		Expression e = rule.getExpression();
		if(rule.isPublic()) {
			file.writeIndent("public ");
			file.write(stringfyName(rule.getLocalName()));
		}
		else {
			file.writeIndent(stringfyName(rule.getLocalName()));
		}
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
		file.decIndent();
	}	
	
	public void visitEmpty(Empty e) {
		file.write("''");
	}

	public void visitFailure(Failure e) {
		file.write("!''/*failure*/");
	}

	public void visitNonTerminal(NonTerminal e) {
		file.write(stringfyName(e.getLocalName()));
	}
	
	public void visitByteChar(ByteChar e) {
		file.write(StringUtils.stringfyByte(e.byteChar));
	}

	public void visitByteMap(ByteMap e) {
		file.write(StringUtils.stringfyCharClass(e.byteMap));
	}
	
	public void visitAnyChar(AnyChar e) {
		file.write(".");
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

	protected void visitSequenceImpl(SequentialExpression l) {
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

	private int appendAsString(SequentialExpression l, int start) {
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
			file.write(StringUtils.quoteString('\'', s, '\''));
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
		file.write(e.lefted ? "{@" : "{");
	}

	public void visitCapture(Capture e) {
		file.write("}");
	}

	public void visitTagging(Tagging e) {
		file.write("#");
		file.write(e.tag.getName());
	}
	
	public void visitValue(Replace e) {
		file.write(StringUtils.quoteString('`', e.value, '`'));
	}
	
	public void visitLink(Link e) {
		String predicate = "@";
		if(e.index != -1) {
			predicate += "[" + e.index + "]";
		}
		this.visit(predicate, e, null);
	}

	@Override
	public void visitUndefined(Expression e) {
		file.write("<");
		file.write(e.getPredicate());
		for(Expression se : e) {
			file.write(" ");
			visit(se);
		}
		file.write(">");
	}

}
