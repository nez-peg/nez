package nez.ext;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.Strategy;
import nez.ast.Tree;
import nez.lang.GrammarFileLoader;
import nez.peg.tpeg.ExprTypeChecker;
import nez.peg.tpeg.LongRange;
import nez.peg.tpeg.NezGrammarGenerator;
import nez.peg.tpeg.TypedPEG;
import nez.peg.tpeg.TypedPEG.AnyExpr;
import nez.peg.tpeg.TypedPEG.CharClassExpr;
import nez.peg.tpeg.TypedPEG.ChoiceExpr;
import nez.peg.tpeg.TypedPEG.LabeledExpr;
import nez.peg.tpeg.TypedPEG.NonTerminalExpr;
import nez.peg.tpeg.TypedPEG.OptionalExpr;
import nez.peg.tpeg.TypedPEG.PredicateExpr;
import nez.peg.tpeg.TypedPEG.RepeatExpr;
import nez.peg.tpeg.TypedPEG.RootExpr;
import nez.peg.tpeg.TypedPEG.RuleExpr;
import nez.peg.tpeg.TypedPEG.SequenceExpr;
import nez.peg.tpeg.TypedPEG.StringExpr;
import nez.peg.tpeg.TypedPEG.TypedRuleExpr;
import nez.peg.tpeg.type.TypeEnv;
import nez.util.ConsoleUtils;

public class Gtpeg extends GrammarFileLoader {
	private static Parser parser = null;

	private TypedPEG generated = null;

	@Override
	public Parser getLoaderParser() {
		if (parser == null) {
			Strategy option = Strategy.newSafeStrategy();
			try {
				Grammar g = GrammarFileLoader.loadGrammar("tpeg.nez", option);
				parser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
		}
		assert parser != null;
		return parser;
	}

	@Override
	public void parse(Tree<?> node) {
		RootExpr rootExpr = (RootExpr) this.translate(node);
		TypeEnv env = new TypeEnv();
		new ExprTypeChecker(env).visit(rootExpr);
		new NezGrammarGenerator().visit(rootExpr, this.getGrammar());
	}

	private static LongRange range(Tree<?> tree) {
		return new LongRange(tree.getSourcePosition(), tree.getLength());
	}

	private TypedPEG translate(Tree<?> tree) {
		this.visit("visit", tree);
		TypedPEG g = this.generated;
		this.generated = null;
		return g;
	}

	public void visitRootExpr(Tree<?> tree) {
		RuleExpr[] ruleExprs = new RuleExpr[tree.size()];
		int index = 0;
		for (Tree<?> t : tree) {
			ruleExprs[index++] = (RuleExpr) this.translate(t);
		}
		this.generated = new RootExpr(range(tree), ruleExprs);
	}

	public void visitRuleExpr(Tree<?> t) {
		String typeName = t.get(1).toText();
		if (typeName.equals("")) { // untyped
			this.generated = new RuleExpr(range(t), t.get(0).toText(), this.translate(t.get(2)));
		} else {
			this.generated = new TypedRuleExpr(range(t), t.get(0).toText(), typeName, this.translate(t.get(2)));
		}
	}

	public void visitChoiceExpr(Tree<?> t) {
		this.generated = new ChoiceExpr(this.translate(t.get(0)), this.translate(t.get(1)));
	}

	public void visitSequenceExpr(Tree<?> t) {
		this.generated = new SequenceExpr(this.translate(t.get(0)), this.translate(t.get(1)));
	}

	public void visitLabeledExpr(Tree<?> t) {
		this.generated = new LabeledExpr(range(t), t.get(0).toText(), this.translate(t.get(1)));
	}

	public void visitAndExpr(Tree<?> t) {
		this.generated = PredicateExpr.andPredicate(range(t), this.translate(t.get(0)));
	}

	public void visitNotExpr(Tree<?> t) {
		this.generated = PredicateExpr.notPredicate(range(t), this.translate(t.get(0)));
	}

	public void visitZeroMoreExpr(Tree<?> t) {
		this.generated = RepeatExpr.zeroMore(range(t), this.translate(t.get(0)));
	}

	public void visitOneMoreExpr(Tree<?> t) {
		this.generated = RepeatExpr.oneMore(range(t), this.translate(t.get(0)));
	}

	public void visitOptionalExpr(Tree<?> t) {
		this.generated = new OptionalExpr(range(t), this.translate(t.get(0)));
	}

	public void visitNonTerminalExpr(Tree<?> t) {
		this.generated = new NonTerminalExpr(range(t), t.toText());
	}

	public void visitAnyExpr(Tree<?> t) {
		this.generated = new AnyExpr(range(t));
	}

	public void visitStringExpr(Tree<?> t) {
		this.generated = new StringExpr(range(t), t.toText());
	}

	public void visitCharClassExpr(Tree<?> t) {
		this.generated = new CharClassExpr(range(t), t.toText());
	}
}
