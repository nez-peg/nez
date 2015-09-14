package nez.generator;

import nez.parser.ParserGenerator;

public abstract class GrammarGenerator extends ParserGenerator {

	//
	// protected char Quoatation() {
	// return '\'';
	// }
	//
	// protected String _RuleDef() {
	// return "=";
	// }
	//
	// protected String _Choice() {
	// return "/";
	// }
	//
	// protected String _Option() {
	// return "?";
	// }
	//
	// protected String _ZeroAndMore() {
	// return "*";
	// }
	//
	// protected String _OneAndMore() {
	// return "+";
	// }
	//
	// protected String _And() {
	// return "&";
	// }
	//
	// protected String _Not() {
	// return "!";
	// }
	//
	// protected String _Any() {
	// return ".";
	// }
	//
	// protected String _OpenGrouping() {
	// return "(";
	// }
	//
	// protected String _CloseGrouping() {
	// return ")";
	// };
	//
	// public void visitGrouping(Expression e) {
	// W(_OpenGrouping());
	// visitExpression(e);
	// W(_CloseGrouping());
	// }
	//
	// protected String _Open() {
	// return "(";
	// }
	//
	// protected String _Delim() {
	// return " ";
	// }
	//
	// protected String _Close() {
	// return ")";
	// };
	//
	// protected String _Name(Production p) {
	// return p.getLocalName().replace("~", "_").replace("!", "_W");
	// }
	//
	//
	// protected String name(Production p) {
	// return p.getLocalName().replace("~", "_").replace("!",
	// "NOT").replace(".", "DOT");
	// }
	//
	//
	// @Override
	// public void visitProduction(Production rule) {
	// Expression e = rule.getExpression();
	// L(name(rule));
	// inc();
	// L(_RuleDef() + " ");
	// if (e instanceof Pchoice) {
	// for (int i = 0; i < e.size(); i++) {
	// if (i > 0) {
	// L(_Choice() + " ");
	// }
	// visitExpression(e.get(i));
	// }
	// } else {
	// visitExpression(e);
	// }
	// dec();
	// }
	//
	// @Override
	// public void visitPempty(Expression e) {
	// W("" + Quoatation() + Quoatation());
	// }
	//
	// @Override
	// public void visitPfail(Expression e) {
	// W(_Not() + Quoatation() + Quoatation());
	// }
	//
	// @Override
	// public void visitNonTerminal(NonTerminal e) {
	// W(name(e.getProduction()));
	// }
	//
	// @Override
	// public void visitCbyte(Cbyte e) {
	// W(StringUtils.stringfyByte(Quoatation(), e.byteChar, Quoatation()));
	// }
	//
	// @Override
	// public void visitCset(Cset e) {
	// W(StringUtils.stringfyCharacterClass(e.byteMap));
	// }
	//
	// @Override
	// public void visitCany(Cany e) {
	// W(_Any());
	// }
	//
	//
	// @Override
	// public void visitPoption(Poption e) {
	// Unary(null, e, _Option());
	// }
	//
	// @Override
	// public void visitPzero(Pzero e) {
	// Unary(null, e, _ZeroAndMore());
	// }
	//
	// @Override
	// public void visitPone(Pone e) {
	// Unary(null, e, _OneAndMore());
	// }
	//
	// @Override
	// public void visitPand(Pand e) {
	// Unary(_And(), e, null);
	// }
	//
	// @Override
	// public void visitPnot(Pnot e) {
	// Unary(_Not(), e, null);
	// }
	//
	// @Override
	// public void visitPchoice(Pchoice e) {
	// for (int i = 0; i < e.size(); i++) {
	// if (i > 0) {
	// W(" " + _Choice() + " ");
	// }
	// visitExpression(e.get(i));
	// }
	// }
	//
	// @Override
	// public void visitTnew(Tnew e) {
	// // W(e.lefted ? "{@" : "{");
	// }
	//
	// @Override
	// public void visitTcapture(Tcapture e) {
	// // W("}");
	// }
	//
	// @Override
	// public void visitTtag(Ttag e) {
	// // W("#");
	// // W(e.tag.getName());
	// }
	//
	// public void visitValue(Treplace e) {
	// // W(StringUtils.quoteString('`', e.value, '`'));
	// }
	//
	// @Override
	// public void visitTlink(Tlink e) {
	// // String predicate = "@";
	// // if(e.index != -1) {
	// // predicate += "[" + e.index + "]";
	// // }
	// // Unary(predicate, e, null);
	// visitExpression(e.get(0));
	// }
	//
	// @Override
	// public void visitPsequence(Psequence e) {
	// int c = 0;
	// for (int i = 0; i < e.size(); i++) {
	// if (c > 0) {
	// W(_Delim());
	// }
	// Expression s = e.get(i);
	// if (s instanceof Cbyte && i + 1 < e.size() && e.get(i + 1) instanceof
	// Cbyte) {
	// i = checkString(e, i);
	// c++;
	// continue;
	// }
	// if (s instanceof Pchoice || s instanceof Psequence) {
	// visitGrouping(s);
	// c++;
	// } else {
	// visitExpression(s);
	// c++;
	// }
	// }
	// }
	//
	// private int checkString(Psequence l, int start) {
	// int n = 0;
	// for (int i = start; i < l.size(); i++) {
	// Expression e = l.get(i);
	// if (e instanceof Cbyte) {
	// n++;
	// continue;
	// }
	// break;
	// }
	// byte[] utf8 = new byte[n];
	// for (int i = 0; i < n; i++) {
	// utf8[i] = (byte) (((Cbyte) l.get(start + i)).byteChar);
	// }
	// visitString(StringUtils.newString(utf8));
	// return start + n - 1;
	// }
	//
	// public void visitString(String text) {
	// W(StringUtils.quoteString('\'', text, '\''));
	// }
	//
	// @Override
	// public void visitUndefined(Expression e) {
	// if (e.size() > 0) {
	// visitExpression(e.get(0));
	// }
	// }
	//
	// @Override
	// public String getDesc() {
	// // TODO Auto-generated method stub
	// return null;
	// }
	//
	// @Override
	// public void visitTreplace(Treplace p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXblock(Xblock p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXdef(Xdef p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXmatch(Xmatch p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXis(Xis p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXdefindent(Xdefindent p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXindent(Xindent p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXexists(Xexists p) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// @Override
	// public void visitXlocal(Xlocal p) {
	// // TODO Auto-generated method stub
	//
	// }

}
