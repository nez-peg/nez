//package nez.lang;
//
//import java.security.MessageDigest;
//import java.security.NoSuchAlgorithmException;
//
//import nez.ast.Symbol;
//import nez.lang.Nez.Function;
//import nez.util.StringUtils;
//
//class Digest extends Expression.Visitor {
//
//	MessageDigest md;
//
//	public byte[] digest(Expression e, StringBuilder sb) {
//		try {
//			md = MessageDigest.getInstance("MD5");
//			e.updateDigest(node, md);
//			return md.digest();
//		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
//		}
//		return new byte[16];
//	}
//
//	private final static byte[] byteChoice = { (byte) 0xfe, 0x00 };
//	private final static byte[] byteNonTerminal = { (byte) 0xfe, 0x01 };
//	private final static byte[] byteFail = { (byte) 0xfe, 0x02 };
//
//	@Override
//	public Object visitNonTerminal(NonTerminal e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		md.update(byteNonTerminal);
//		md.update(e.getLocalName().getBytes());
//		return null;
//	}
//
//	@Override
//	public Object visitEmpty(Nez.Empty e, Object a) {
//		return null;
//	}
//
//	@Override
//	public Object visitFail(Nez.Fail e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		md.update(byteNonTerminal);
//		return null;
//	}
//
//	@Override
//	public Object visitByte(Nez.Byte e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		md.update((byte) e.byteChar);
//		return null;
//	}
//
//	@Override
//	public Object visitByteSet(Nez.ByteSet e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append(StringUtils.stringfyCharacterClass(e.byteMap));
//		return null;
//	}
//
//	@Override
//	public Object visitAny(Nez.Any e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append(".");
//		return null;
//	}
//
//	@Override
//	public Object visitMultiByte(Nez.MultiByte e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append("'");
//		for (int i = 0; i < e.byteSeq.length; i++) {
//			StringUtils.appendByteChar(sb, e.byteSeq[i] & 0xff, "\'");
//		}
//		sb.append("'");
//		return null;
//	}
//
//	@Override
//	public Object visitPair(Nez.Pair e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatSequence(e, sb, " ");
//		return null;
//	}
//
//	@Override
//	public Object visitSequence(Nez.Sequence e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatSequence(e, sb, " ");
//		return null;
//	}
//
//	@Override
//	public Object visitChoice(Nez.Choice e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatSequence(e, sb, " / ");
//		return null;
//	}
//
//	@Override
//	public Object visitOption(Nez.Option e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatUnary(sb, null, e.get(0), "?");
//		return null;
//	}
//
//	@Override
//	public Object visitZeroMore(Nez.ZeroMore e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatUnary(sb, null, e.get(0), "*");
//		return null;
//	}
//
//	@Override
//	public Object visitOneMore(Nez.OneMore e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatUnary(sb, null, e.get(0), "+");
//		return null;
//	}
//
//	@Override
//	public Object visitAnd(Nez.And e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatUnary(sb, "&", e.get(0), null);
//		return null;
//	}
//
//	@Override
//	public Object visitNot(Nez.Not e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatUnary(sb, "!", e.get(0), null);
//		return null;
//	}
//
//	@Override
//	public Object visitBeginTree(Nez.BeginTree e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append("{");
//		return null;
//	}
//
//	@Override
//	public Object visitLeftFold(Nez.LeftFold e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append("{$");
//		if (e.label != null) {
//			sb.append(e.label);
//		}
//		return null;
//	}
//
//	@Override
//	public Object visitLink(Nez.Link e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		formatUnary(sb, (e.label != null) ? "$" + e.label + "(" : "$(", e.get(0), ")");
//		return null;
//	}
//
//	@Override
//	public Object visitTag(Nez.Tag e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append("#" + e.tag);
//		return null;
//	}
//
//	@Override
//	public Object visitReplace(Nez.Replace e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append(StringUtils.quoteString('`', e.value, '`'));
//		return null;
//	}
//
//	@Override
//	public Object visitEndTree(Nez.EndTree e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		sb.append("}");
//		return null;
//	}
//
//	@Override
//	public Object visitDetree(Nez.Detree e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatUnary(sb, "~", e.get(0), null);
//		return null;
//	}
//
//	@Override
//	public Object visitBlockScope(Nez.BlockScope e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, null, sb);
//		return null;
//	}
//
//	@Override
//	public Object visitLocalScope(Nez.LocalScope e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, e.tableName, sb);
//		return null;
//	}
//
//	@Override
//	public Object visitSymbolAction(Nez.SymbolAction e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, null, sb);
//		return null;
//	}
//
//	@Override
//	public Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, e.tableName, sb);
//		return null;
//	}
//
//	@Override
//	public Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, e.tableName, sb);
//		return null;
//	}
//
//	@Override
//	public Object visitSymbolExists(Nez.SymbolExists e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, symbol(e.tableName, e.symbol), sb); // FIXME
//		return null;
//	}
//
//	@Override
//	public Object visitIf(Nez.If e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, condition(e.predicate, e.flagName), sb);
//		return null;
//	}
//
//	@Override
//	public Object visitOn(Nez.On e, Object a) {
//		MessageDigest md = (MessageDigest) a;
//		this.formatFunction(e, condition(e.predicate, e.flagName), sb);
//		return null;
//	}
//
//	private void formatSequence(Expression e, StringBuilder sb, String delim) {
//		for (int i = 0; i < e.size(); i++) {
//			if (i > 0) {
//				sb.append(delim);
//			}
//			format(e.get(i), sb);
//		}
//	}
//
//	private void formatUnary(StringBuilder sb, String prefix, Expression inner, String suffix) {
//		if (prefix != null) {
//			sb.append(prefix);
//		}
//		if (inner instanceof NonTerminal || inner instanceof Nez.Terminal) {
//			format(inner, sb);
//		} else {
//			sb.append("(");
//			format(inner, sb);
//			sb.append(")");
//		}
//		if (suffix != null) {
//			sb.append(suffix);
//		}
//	}
//
//	private void formatFunction(Function e, Object argument, StringBuilder sb) {
//		sb.append("<");
//		sb.append(e.op);
//		if (argument != null) {
//			sb.append(" ");
//			sb.append(argument);
//		}
//		if (e.hasInnerExpression()) {
//			sb.append(" ");
//			sb.append(e.get(0));
//		}
//		sb.append(">");
//	}
//
//	private String symbol(Symbol table, String name) {
//		return name == null ? table.toString() : table + " " + name;
//	}
//
//	private String condition(boolean predicate, String name) {
//		return predicate ? name : "!" + name;
//	}
//
// }