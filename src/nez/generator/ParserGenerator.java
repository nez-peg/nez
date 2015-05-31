package nez.generator;

import nez.ast.Tag;
import nez.lang.And;
import nez.lang.AnyChar;
import nez.lang.ByteChar;
import nez.lang.ByteMap;
import nez.lang.Capture;
import nez.lang.Choice;
import nez.lang.Empty;
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
import nez.lang.Tagging;

public abstract class ParserGenerator extends NezGenerator {

	ParserGenerator(String fileName) {
		super(fileName);
	}

	@Override
	public String getDesc() {
		return "a Nez parser generator";
	}

	@Override
	public abstract void visitProduction(Production rule);

	public abstract void visitEmpty(Empty e);

	public abstract void visitFailure(Failure e);

	public abstract void visitNonTerminal(NonTerminal e);

	public abstract void visitByteChar(ByteChar e);

	public abstract void visitByteMap(ByteMap e);

	public abstract void visitAnyChar(AnyChar e);

	public abstract void visitOption(Option e);

	public abstract void visitRepetition(Repetition e);

	public abstract void visitRepetition1(Repetition1 e);

	public abstract void visitAnd(And e);

	public abstract void visitNot(Not e);

	public abstract void visitSequence(Sequence e);

	public abstract void visitChoice(Choice e);

	public abstract void visitNew(New e);

	public abstract void visitCapture(Capture e);

	protected abstract String _tag(Tag tag);

	public abstract void visitTagging(Tagging e);

	public abstract void visitReplace(Replace e);

	public abstract void visitLink(Link e);

}