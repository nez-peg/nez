package nez.cc;

import nez.Grammar;
import nez.expr.And;
import nez.expr.AnyChar;
import nez.expr.ByteChar;
import nez.expr.ByteMap;
import nez.expr.Capture;
import nez.expr.Choice;
import nez.expr.Empty;
import nez.expr.Expression;
import nez.expr.Failure;
import nez.expr.Link;
import nez.expr.New;
import nez.expr.NonTerminal;
import nez.expr.Not;
import nez.expr.Option;
import nez.expr.Repetition;
import nez.expr.Repetition1;
import nez.expr.Replace;
import nez.expr.Rule;
import nez.expr.Sequence;
import nez.expr.SequentialExpression;
import nez.expr.Tagging;
import nez.expr.Unary;
import nez.util.StringUtils;
import nez.util.UList;

public class LPegGrammarGenerator extends GrammarGenerator {

	Grammar peg;
	
	
	public LPegGrammarGenerator(String fileName) {
		super(fileName);
	}
	
	@Override
	public String getDesc() {
		return "Lua script (required LPEG)" ;
	}

	@Override
	public void generate(Grammar grammar) {
		peg = grammar;
		makeHeader();
		file.writeIndent("G = lpeg.P{ File,");
		file.incIndent();
		UList<Rule> list = grammar.getDefinedRuleList();
		for(Rule r: list) {
			if(!r.getLocalName().startsWith("\"")) {
				visitRule(r);
			}
		}
		file.decIndent();
		file.writeIndent("}");
		file.writeIndent();
		makeFooter();
		file.writeNewLine();
		file.flush();
	}

	@Override
	public void makeHeader() {
		file.writeIndent("local lpeg = require \"lpeg\"");
		UList<Rule> list = peg.getDefinedRuleList();
		for(Rule r: list) {
			if(!r.getLocalName().startsWith("\"")) {
				String localName = r.getLocalName();
				file.writeIndent("local " + localName + " = lpeg.V\"" + localName + "\"");
			}
		}
	}

	@Override
	public void visitRule(Rule rule) {
		Expression e = rule.getExpression();
		file.incIndent();
		file.writeIndent(rule.getLocalName() + " = ");
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					file.write(" + ");
				}
				visit(e.get(i));
			}
		}
		else {
			visit(e);
		}
		file.write(";");
		file.decIndent();
	}
	
	@Override
	public void makeFooter() {
		file.writeIndent("function evalExp (s)");
		file.incIndent();
		file.writeIndent("for i = 0, 5 do");
		file.incIndent();
		file.writeIndent("local t1 = os.clock()");
		file.writeIndent("local t = lpeg.match(G, s)");
		file.writeIndent("local e1 = os.clock() - t1");
		file.writeIndent("print(\"elapsedTime1 : \", e1)");
		file.writeIndent("if not t then error(\"syntax error\", 2) end");
		file.decIndent();
		file.writeIndent("end");
		file.decIndent();
		file.writeIndent("end");
		file.writeIndent();
		file.writeIndent("fileName = arg[1]");
		file.writeIndent("fh, msg = io.open(fileName, \"r\")");
		file.writeIndent("if fh then");
		file.incIndent();
		file.writeIndent("data = fh:read(\"*a\")");
		file.decIndent();
		file.writeIndent("else");
		file.incIndent();
		file.writeIndent("print(msg)");
		file.decIndent();
		file.writeIndent("end");
		file.writeIndent("evalExp(data)");
	}
	
	public void visitEmpty(Empty e) {
		file.write("lpeg.P\"\"");
	}

	public void visitFailure(Failure e) {
		file.write("- lpeg.P(1) ");
	}

	public void visitNonTerminal(NonTerminal e) {
		file.write(e.getLocalName() + " ");
	}
	
	public String stringfyByte(int byteChar) {
		char c = (char)byteChar;
		switch(c) {
		case '\n' : return("'\\n'"); 
		case '\t' : return("'\\t'"); 
		case '\r' : return("'\\r'"); 
		case '\"' : return("\"\\\"\""); 
		case '\\' : return("'\\\\'"); 
		}
		return "\"" + c + "\""; 
	}
	
	public void visitByteChar(ByteChar e) {
		file.write("lpeg.P" + this.stringfyByte(e.byteChar) + " ");
	}
	
	private int searchEndChar(boolean[] b, int s) {
		for(; s < 256; s++) {
			if(!b[s]) {
				return s-1;
			}
		}
		return 255;
	}
	
	private void getRangeChar(int ch, StringBuilder sb) {
		char c = (char)ch;
		switch(c) {
		case '\n' : sb.append("\\n"); 
		case '\t' : sb.append("'\\t'"); 
		case '\r' : sb.append("'\\r'"); 
		case '\'' : sb.append("'\\''"); 
		case '\\' : sb.append("'\\\\'"); 
		}
		sb.append(c);
	}

	public void visitByteMap(ByteMap e) {
		boolean b[] = e.byteMap;
		for(int start = 0; start < 256; start++) {
			if(b[start]) {
				int end = searchEndChar(b, start+1);
				if (start == end) {
					file.write("lpeg.P" + this.stringfyByte(start) + " ");
				}
				else {
					StringBuilder sb = new StringBuilder();
					getRangeChar(start, sb);
					getRangeChar(end, sb);
					file.write("lpeg.R(\"" + sb.toString() + "\") ");
					start = end;
				}
			}
		}
	}
	
	public void visitAnyChar(AnyChar e) {
		file.write("lpeg.P(1)");
	}

	protected void visit(String prefix, Unary e, String suffix) {
		if(prefix != null) {
			file.write(prefix);
		}
		if(e.get(0) instanceof NonTerminal/* || e.get(0) instanceof NewClosure*/) {
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
		this.visit( null, e, "^-1");
	}
	
	public void visitRepetition(Repetition e) {
		this.visit(null, e, "^0");
	}

	public void visitRepetition1(Repetition1 e) {
		this.visit(null, e, "^1");
	}

	public void visitAnd(And e) {
		this.visit( "#", e, null);
	}
	
	public void visitNot(Not e) {
		this.visit( "-", e, null);
	}

	public void visitTagging(Tagging e) {
		file.write("lpeg.P\"\" --[[");
		file.write(e.tag.toString());
		file.write("]]");
	}
	
	public void visitValue(Replace e) {
		file.write("lpeg.P\"\"");
	}
	
	public void visitLink(Link e) {
//		String predicate = "@";
//		if(e.index != -1) {
//			predicate += "[" + e.index + "]";
//		}
//		this.visit(predicate, e, null);
		this.visit(e.get(0));
	}

	protected void visitSequenceImpl(SequentialExpression l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				file.write(" ");
			}
			int n = appendAsString(l, i);
			if(n > i) {
				i = n;
				if(i < l.size()-1) {
					file.write(" * ");
				}
				continue;
			}
			Expression e = l.get(i);
			if(e instanceof Choice || e instanceof Sequence) {
				file.write("( ");
				visit(e);
				file.write(" )");
			}
			else {
				visit(e);
			}
			if(i < l.size()-1) {
				file.write(" * ");
			}
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
			file.write("lpeg.P" + StringUtils.quoteString('"', s, '"'));
		}
		return end - 1;
	}
	
	public void visitSequence(Sequence e) {
		this.visitSequenceImpl(e);
	}
	
	public void visitChoice(Choice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				file.write(" + ");
			}
			file.write(" ( ");
			visit(e.get(i));
			file.write(" ) ");
		}
	}

//	public void visitNewClosure(NewClosure e) {
//		file.write("( ");
//		this.visitSequenceImpl(e);
//		file.write(" )");
//	}
//
//	public void visitLeftNew(LeftNewClosure e) {
//		file.write("( ");
//		this.visitSequenceImpl(e);
//		file.write(" )");
//	}

	public void visitNew(New e) {

	}

	public void visitCapture(Capture e) {

	}

	@Override
	public void visitUndefined(Expression e) {
		file.write("lpeg.P\"\" --[[ LPeg Unsupported <");
		file.write(e.getPredicate());
		for(Expression se : e) {
			file.write(" ");
			visit(se);
		}
		file.write("> ]]");
	}
}
