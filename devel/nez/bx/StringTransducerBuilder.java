package nez.bx;

import nez.ast.Tree;

public interface StringTransducerBuilder {
	public <E extends Tree<E>> StringTransducer lookup(Tree<E> sub);

	public void write(String text);

	public void writeNewLineIndent();

	public void incIndent();

	public void decIndent();
}

// class FormatterEntry {
// StringTransducer[] arguments;
// void set(int index, StringTransducer fmt) {
// index++;
// if(arguments == null) {
// arguments = new StringTransducer[index+1];
// arguments[index] = fmt;
// return;
// }
// if(!(index < arguments.length)) {
// StringTransducer[] a = new StringTransducer[index+1];
// System.arraycopy(this.arguments, 0, a, 0, this.arguments.length);
// this.arguments = a;
// }
// arguments[index] = fmt;
// }
// StringTransducer get(int index) {
// if(arguments == null) {
// return null;
// }
// index++;
// if(!(index < arguments.length)) {
// index = arguments.length - 1;
// }
// for(int i = index; i >=0; i--) {
// if(this.arguments[i] != null) {
// return this.arguments[i];
// }
// }
// return null;
// }
// }
//
// class FormatterMap {
// HashMap<String, FormatterEntry> map = new HashMap<String, FormatterEntry>();
// void set(String tag, int index, StringTransducer fmt) {
// FormatterEntry entry = map.get(tag);
// if(entry == null) {
// entry = new FormatterEntry();
// map.put(tag, entry);
// }
// entry.set(index, fmt);
// }
// StringTransducer get(String tag, int index) {
// FormatterEntry entry = map.get(tag);
// if(entry != null) {
// return entry.get(index);
// }
// return null;
// }
// }
//
// class FormatStringBuilder implements StringTransducerBuilder {
// final GrammarFile ns;
// StringBuilder sb = new StringBuilder();
// int indent = 0;
// FormatStringBuilder(GrammarFile ns) {
// this.ns = ns;
// }
//
// public String toString() {
// return sb.toString();
// }
//
// @Override
// public StringTransducer lookupFormatter(CommonTree sub) {
// StringTransducer fmt = ns.getFormatter(sub.getTag().getName(), sub.size());
// if(fmt == null) {
// return StringTransducer.Default;
// }
// return fmt;
// }
//
// @Override
// public void write(String text) {
// sb.append(text);
// }
//
// @Override
// public void writeNewLineIndent() {
// sb.append("\n");
// for(int i = 0; i < indent; i++) {
// sb.append("   ");
// }
// }
//
// @Override
// public void incIndent() {
// indent++;
// }
//
// @Override
// public void decIndent() {
// indent--;
// }
// }
