package nez.vmgen;

import java.io.IOException;

import nez.ParserGenerator;
import nez.ast.Source;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.parser.Parser;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;
import nez.util.UList;

public class Command extends nez.main.Command {
	@Override
	public void exec() throws IOException {
		if (!this.hasInputSource()) {
			ConsoleUtils.exit(1, "no a vmnez file");
		}
		ParserGenerator generator = new ParserGenerator();
		Parser parser = generator.newParser("vmnez.nez");
		Source s = nextInputSource();
		MyTree tree = parser.parse(s, new MyTree());
		if (tree == null) {
			parser.showErrors();
			return;
		}
		System.out.println("parsed: " + tree);
		parseSource(tree);
		generateC();
		generatePEG();
	}

	final static Symbol _Opcode = Symbol.unique("Opcode");
	final static Symbol _Oprand = Symbol.unique("Oprand");
	final static Symbol _name = Symbol.unique("name");
	final static Symbol _params = Symbol.unique("params");
	final static Symbol _code = Symbol.unique("code");

	FileBuilder file = new FileBuilder();
	UList<MyTree> oprands = new UList<>(new MyTree[10]);
	UList<MyTree> opcodes = new UList<>(new MyTree[10]);

	private void parseSource(MyTree source) {
		for (MyTree t : source) {
			if (t.is(_Opcode)) {
				parseOpcode(t);
			}
			if (t.is(_Oprand)) {
				parseOprand(t);
			}
		}
	}

	private void parseOprand(MyTree t) {
		t.id = oprands.size();
		oprands.add(t);
	}

	private void parseOpcode(MyTree t) {
		t.id = opcodes.size();
		opcodes.add(t);
	}

	void generatePEG() {
		file.writeIndent("Start = { (");
		file.incIndent();
		file.writeIndent("$label(Label)");
		for (MyTree t : opcodes) {
			String name = t.getName();
			file.writeIndent("/ $%s(I%s)", name, name);
		}
		file.writeIndent(")* #Source }");
		file.decIndent();

		for (MyTree t : opcodes) {
			String name = t.getName();
			file.writeIndent("I%s = '\\t' { '%s' #%d", name, name, t.id);
			file.incIndent();
			{
				for (MyTree op : t.get(_params)) {
					String arg = op.toText();
					file.writeIndent("S+ $%s(A%s)", arg, arg);
				}
				file.writeIndent("}");
			}
			file.decIndent();
		}
		for (MyTree t : oprands) {
			file.writeIndent("A%s = { %s }", t.getName(), t.getText(_code, "''"));
		}
	}

	void generateC() {
		for (MyTree t : opcodes) {
			file.writeIndent("#define I%s %s", t.getName(), t.id);
		}
		for (MyTree t : opcodes) {
			generateStruct(t);
		}
		file.writeIndent("Counter* vm(Context *c, Counter *pc) {");
		file.incIndent();
		{
			file.writeIndent("while(1) {");
			file.incIndent();
			{
				file.writeIndent("switch(op) {");
				for (MyTree t : opcodes) {
					generateCase(t);
				}
				file.writeIndent("}");
			}
			file.decIndent();
			file.writeIndent("}");
		}
		file.decIndent();
		file.writeIndent("}");
		file.flush();
		file.close();
	}

	private void generateStruct(MyTree t) {
		file.writeIndent("struct _%s {", t.getName());
		file.incIndent();
		file.writeIndent("opcode_t opcode;");
		for (MyTree op : t.get(_params)) {
			String name = op.toText();
			file.writeIndent("%s_t %s;", name, name);
		}
		file.decIndent();
		file.writeIndent("}");
	}

	private void generateCase(MyTree t) {
		String name = t.getName();
		file.writeIndent("case I%s:", name);
		file.incIndent();
		file.writeIndent("struct _%s *op = (struct _%s *)pc;", name, name);
		file.writeMultiLine(t.getText(_code, ""));
		file.writeIndent("pc = (Counter *)(op+1);");
		file.writeIndent("break;");
		file.decIndent();
	}

	public static class MyTree extends Tree<MyTree> {
		int id = 0;

		public MyTree() {
			super(Symbol.tokenTag, null, 0, 0, null, null);
		}

		public MyTree(Symbol tag, Source source, long pos, int len, int size, Object value) {
			super(tag, source, pos, len, size > 0 ? new MyTree[size] : null, value);
		}

		@Override
		public MyTree newInstance(Symbol tag, Source source, long pos, int len, int size, Object value) {
			return new MyTree(tag, source, pos, len, size, value);
		}

		@Override
		public void link(int n, Symbol label, Object child) {
			this.set(n, label, (MyTree) child);
		}

		@Override
		public MyTree newInstance(Symbol tag, int size, Object value) {
			return new MyTree(tag, this.getSource(), this.getSourcePosition(), 0, size, value);
		}

		@Override
		protected MyTree dupImpl() {
			return new MyTree(this.getTag(), this.getSource(), this.getSourcePosition(), this.getLength(), this.size(), getValue());
		}

		public final String getName() {
			return this.getText(_name, "");
		}
	}

}
