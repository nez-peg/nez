//package nez.x.nezvm;
//
//import nez.Grammar;
//import nez.expr.Rule;
//
//import org.peg4d.pegcode.GrammarVisitor;
//
//
//public abstract class GrammarGenerator extends GrammarVisitor {
//	protected StringBuilder sb = null;
//	
//	public GrammarGenerator() {
//		this.sb = null;
//	}
//	
//	public abstract String getDesc();
//
//	public void formatGrammar(Grammar peg, StringBuilder sb) {
//		this.sb = sb;
//		this.formatHeader();
//		for(Rule r: peg.getRuleList()) {
//			this.formatRule(r, sb);
//		}
//		this.formatFooter();
//		System.out.println(sb.toString());
//	}
//	
//	public void formatHeader() {
//	}
//	public void formatFooter() {
//	}
//	
//	public final void formatRule(Rule rule, StringBuilder sb) {
//		this.sb = sb;
//		this.visitRule(rule);
//	}
//
//	public final void formatExpression(Expression e, StringBuilder sb) {
//		this.sb = sb;
//		e.visit(this);
//	}
//	
//	protected final void formatString(String s) {
//		if(sb == null) {
//			System.out.print(s);
//		}
//		else {
//			this.sb.append(s);
//		}
//	}
//
//	public final static String stringfyByte(int ch) {
//		char c = (char)ch;
//		switch(c) {
//		case '\n' : return("'\\n'"); 
//		case '\t' : return("'\\t'"); 
//		case '\r' : return("'\\r'"); 
//		case '\'' : return("'\\''"); 
//		case '\\' : return("'\\\\'"); 
//		}
//		if(Character.isISOControl(c) || c > 127) {
//			return(String.format("0x%02x", (int)c));
//		}
//		return("'" + c + "'");
//	}
//
//	public final static String stringfyByte2(int ch) {
//		char c = (char)ch;
//		switch(c) {
//		case '\n' : return("\\n"); 
//		case '\t' : return("\\t"); 
//		case '\r' : return("\\r"); 
//		case '\'' : return("\\'"); 
//		case ']' : return("\\]"); 
//		case '-' : return("\\-"); 
//		case '\\' : return("\\\\"); 
//		}
//		if(Character.isISOControl(c) || c > 127) {
//			return(String.format("\\x%02x", (int)c));
//		}
//		return("" + c);
//	}
//
//}
