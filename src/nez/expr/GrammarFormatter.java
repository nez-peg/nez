package nez.expr;

import nez.util.StringUtils;

public class GrammarFormatter extends GrammarVisitor {
	private StringBuilder sb = null;
	private final static String NewIndent = "\n\t";

	public GrammarFormatter() {
		this(new StringBuilder());
	}
	
	public GrammarFormatter(StringBuilder sb) {
		this.sb = sb;
	}
	
	public final String format(Expression e) {
		visit(e);
		return sb.toString();
	}

	public void visitRule(Rule rule) {
		Expression e = rule.getExpression();
		sb.append(rule.getLocalName());
		sb.append(NewIndent);
		sb.append("= ");
		if(e instanceof Choice) {
			for(int i = 0; i < e.size(); i++) {
				if(i > 0) {
					sb.append(NewIndent);
					sb.append("/ ");
				}
				visit(e.get(i));
			}
		}
		else {
			visit(e);
		}
	}	
	
	public void visitEmpty(Empty e) {
		sb.append("''");
	}

	public void visitFailure(Failure e) {
		sb.append("!''/*failure*/");
	}

	public void visitNonTerminal(NonTerminal e) {
		sb.append(e.getLocalName());
	}
	
	public void visitByteChar(ByteChar e) {
		sb.append(StringUtils.stringfyByte(e.byteChar));
	}

	public void visitByteMap(ByteMap e) {
		sb.append(StringUtils.stringfyCharClass(e.byteMap));
	}
	
	public void visitAnyChar(AnyChar e) {
		sb.append(".");
	}
	
	public void visitTagging(Tagging e) {
		sb.append("#");
		sb.append(e.tag.toString());
	}
	
	public void visitReplace(Replace e) {
		sb.append(StringUtils.quoteString('`', e.value, '`'));
	}
	
	protected void format(String prefix, Unary e, String suffix) {
		if(prefix != null) {
			sb.append(prefix);
		}
		if(/*e.inner instanceof String ||*/ e.inner instanceof NonTerminal/* || e.inner instanceof NewClosure*/) {
			this.visit(e.inner);
		}
		else {
			sb.append("(");
			this.visit(e.inner);
			sb.append(")");
		}
		if(suffix != null) {
			sb.append(suffix);
		}
	}
	
	public void visitOption(Option e) {
		this.format( null, e, "?");
	}
	
	public void visitRepetition(Repetition e) {
		this.format(null, e, "*");
	}

	public void visitRepetition1(Repetition1 e) {
		this.format(null, e, "+");
	}

	public void visitAnd(And e) {
		this.format( "&", e, null);
	}
	
	public void visitNot(Not e) {
		this.format( "!", e, null);
	}

	
	public void visitLink(Link e) {
		String predicate = "@";
		if(e.index != -1) {
			predicate += "[" + e.index + "]";
		}
		this.format(predicate, e, null);
	}

	protected void appendSequence(SequentialExpression l) {
		for(int i = 0; i < l.size(); i++) {
			if(i > 0) {
				sb.append(" ");
			}
			int n = appendAsString(l, i);
			if(n > i) {
				i = n;
				continue;
			}
			Expression e = l.get(i);
			if(e instanceof Choice || e instanceof Sequence) {
				sb.append("( ");
				visit(e);
				sb.append(" )");
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
			sb.append(StringUtils.quoteString('\'', s, '\''));
		}
		return end - 1;
	}
	
	
	public void visitSequence(Sequence e) {
		this.appendSequence( e);
	}
	
	public void visitChoice(Choice e) {
		for(int i = 0; i < e.size(); i++) {
			if(i > 0) {
				sb.append(" / ");
			}
			visit(e.get(i));
		}
	}

	public void visitNew(New e) {
		sb.append(e.lefted ? "{@ " : "{ ");
	}

	public void visitCapture(Capture e) {
		sb.append("}");
	}

	@Override
	public void visitExpression(Expression e) {
		sb.append("<");
		sb.append(e.getPredicate());
		for(Expression se : e) {
			sb.append(" ");
			visit(se);
		}
		sb.append(">");
	}


}

