package nez.generator;

public abstract class GrammarGenerator extends NezGenerator {

	protected GrammarGenerator(String fileName) {
		super(fileName);
	}

	protected GrammarGenerator W(String word) {
		file.write(word);
		return this;
	}

	protected GrammarGenerator L() {
		file.writeIndent();
		return this;
	}

	protected GrammarGenerator inc() {
		file.incIndent();
		return this;
	}

	protected GrammarGenerator dec() {
		file.decIndent();
		return this;
	}

	protected GrammarGenerator L(String line) {
		file.writeIndent(line);
		return this;
	}

}
