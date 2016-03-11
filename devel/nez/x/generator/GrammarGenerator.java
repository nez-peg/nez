//package nez.x.generator;
//
//import nez.tool.peg.GrammarTranslatorVisitor;
//
//public abstract class GrammarGenerator extends GrammarTranslatorVisitor {
//
//	//
//	// protected char Quoatation() {
//	// return '\'';
//	// }
//	//
//	// protected String _RuleDef() {
//	// return "=";
//	// }
//	//
//	// protected String _Choice() {
//	// return "/";
//	// }
//	//
//	// protected String _Option() {
//	// return "?";
//	// }
//	//
//	// protected String _ZeroAndMore() {
//	// return "*";
//	// }
//	//
//	// protected String _OneAndMore() {
//	// return "+";
//	// }
//	//
//	// protected String _And() {
//	// return "&";
//	// }
//	//
//	// protected String _Not() {
//	// return "!";
//	// }
//	//
//	// protected String _Any() {
//	// return ".";
//	// }
//	//
//	// protected String _OpenGrouping() {
//	// return "(";
//	// }
//	//
//	// protected String _CloseGrouping() {
//	// return ")";
//	// };
//	//
//	// public void visitGrouping(Expression e) {
//	// W(_OpenGrouping());
//	// visitExpression(e);
//	// W(_CloseGrouping());
//	// }
//	//
//	// protected String _Open() {
//	// return "(";
//	// }
//	//
//	// protected String _Delim() {
//	// return " ";
//	// }
//	//
//	// protected String _Close() {
//	// return ")";
//	// };
//	//
//	// protected String _Name(Production p) {
//	// return p.getLocalName().replace("~", "_").replace("!", "_W");
//	// }
//	//
//	//
//	// protected String name(Production p) {
//	// return p.getLocalName().replace("~", "_").replace("!",
//	// "NOT").replace(".", "DOT");
//	// }
//	//
//	//
//	// @Override
//	// public void visitProduction(Production rule) {
//	// Expression e = rule.getExpression();
//	// L(name(rule));
//	// inc();
//	// L(_RuleDef() + " ");
//	// if (e instanceof Nez.Choice) {
//	// for (int i = 0; i < e.size(); i++) {
//	// if (i > 0) {
//	// L(_Choice() + " ");
//	// }
//	// visitExpression(e.get(i));
//	// }
//	// } else {
//	// visitExpression(e);
//	// }
//	// dec();
//	// }
//	//
//	// @Override
//	// public void visitEmpty(Expression e) {
//	// W("" + Quoatation() + Quoatation());
//	// }
//	//
//	// @Override
//	// public void visitFail(Expression e) {
//	// W(_Not() + Quoatation() + Quoatation());
//	// }
//	//
//	// @Override
//	// public void visitNonTerminal(NonTerminal e) {
//	// W(name(e.getProduction()));
//	// }
//	//
//	// @Override
//	// public void visitByte(Nez.Byte e) {
//	// W(StringUtils.stringfyByte(Quoatation(), e.byteChar, Quoatation()));
//	// }
//	//
//	// @Override
//	// public void visitByteSet(Nez.ByteSet e) {
//	// W(StringUtils.stringfyCharacterClass(e.byteMap));
//	// }
//	//
//	// @Override
//	// public void visitAny(Nez.Any e) {
//	// W(_Any());
//	// }
//	//
//	//
//	// @Override
//	// public void visitOption(Nez.Option e) {
//	// Unary(null, e, _Option());
//	// }
//	//
//	// @Override
//	// public void visitZeroMore(Nez.ZeroMore e) {
//	// Unary(null, e, _ZeroAndMore());
//	// }
//	//
//	// @Override
//	// public void visitOneMore(Nez.OneMore e) {
//	// Unary(null, e, _OneAndMore());
//	// }
//	//
//	// @Override
//	// public void visitAnd(Nez.And e) {
//	// Unary(_And(), e, null);
//	// }
//	//
//	// @Override
//	// public void visitNot(Nez.Not e) {
//	// Unary(_Not(), e, null);
//	// }
//	//
//	// @Override
//	// public void visitChoice(Nez.Choice e) {
//	// for (int i = 0; i < e.size(); i++) {
//	// if (i > 0) {
//	// W(" " + _Choice() + " ");
//	// }
//	// visitExpression(e.get(i));
//	// }
//	// }
//	//
//	// @Override
//	// public void visitPreNew(Nez.PreNew e) {
//	// // W(e.lefted ? "{@" : "{");
//	// }
//	//
//	// @Override
//	// public void visitNew(Nez.New e) {
//	// // W("}");
//	// }
//	//
//	// @Override
//	// public void visitTag(Nez.Tag e) {
//	// // W("#");
//	// // W(e.tag.getName());
//	// }
//	//
//	// public void visitValue(Treplace e) {
//	// // W(StringUtils.quoteString('`', e.value, '`'));
//	// }
//	//
//	// @Override
//	// public void visitLink(Nez.Link e) {
//	// // String predicate = "@";
//	// // if(e.index != -1) {
//	// // predicate += "[" + e.index + "]";
//	// // }
//	// // Unary(predicate, e, null);
//	// visitExpression(e.get(0));
//	// }
//	//
//	// @Override
//	// public void visitPair(Nez.Pair e) {
//	// int c = 0;
//	// for (int i = 0; i < e.size(); i++) {
//	// if (c > 0) {
//	// W(_Delim());
//	// }
//	// Expression s = e.get(i);
//	// if (s instanceof Nez.Byte && i + 1 < e.size() && e.get(i + 1) instanceof
//	// Cbyte) {
//	// i = checkString(e, i);
//	// c++;
//	// continue;
//	// }
//	// if (s instanceof Nez.Choice || s instanceof Nez.Sequence) {
//	// visitGrouping(s);
//	// c++;
//	// } else {
//	// visitExpression(s);
//	// c++;
//	// }
//	// }
//	// }
//	//
//	// private int checkString(Psequence l, int start) {
//	// int n = 0;
//	// for (int i = start; i < l.size(); i++) {
//	// Expression e = l.get(i);
//	// if (e instanceof Nez.Byte) {
//	// n++;
//	// continue;
//	// }
//	// break;
//	// }
//	// byte[] utf8 = new byte[n];
//	// for (int i = 0; i < n; i++) {
//	// utf8[i] = (byte) (((Cbyte) l.get(start + i)).byteChar);
//	// }
//	// visitString(StringUtils.newString(utf8));
//	// return start + n - 1;
//	// }
//	//
//	// public void visitString(String text) {
//	// W(StringUtils.quoteString('\'', text, '\''));
//	// }
//	//
//	// @Override
//	// public void visitUndefined(Expression e) {
//	// if (e.size() > 0) {
//	// visitExpression(e.get(0));
//	// }
//	// }
//	//
//	// @Override
//	// public String getDesc() {
//	// // TODO Auto-generated method stub
//	// return null;
//	// }
//	//
//	// @Override
//	// public void visitReplace(Nez.Replace p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitBlockScope(Nez.BlockScope p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitSymbolAction(Xdef p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitSymbolMatch(Nez.SymbolMatch p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitSymbolPredicate(Nez.SymbolPredicate p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitSymbolActionindent(Xdefindent p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitXindent(Xindent p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitSymbolExists(Nez.SymbolExists p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//	//
//	// @Override
//	// public void visitLocalScope(Nez.LocalScope p) {
//	// // TODO Auto-generated method stub
//	//
//	// }
//
// }
