package nez.ext;

import java.io.IOException;

import nez.ast.Tree;
import nez.junks.GrammarFileLoader;
import nez.lang.Grammar;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
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

	public Gtpeg() {
		init(Gtpeg.class, new Undefined());
	}

	public class Undefined extends DefaultVisitor {
		@Override
		public void accept(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in Typed PEG #" + node));
		}
	}

	private final void visit(Tree<?> node) {
		find(node.getTag().toString()).accept(node);
	}

	@Override
	public Parser getLoaderParser(String start) {
		if (parser == null) {
			ParserStrategy option = ParserStrategy.newSafeStrategy();
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
		TypeEnv env = TypeEnv.getInstance();
		new ExprTypeChecker(env).visit(rootExpr);
		new NezGrammarGenerator().visit(rootExpr, this.getGrammar());
	}

	private static LongRange range(Tree<?> tree) {
		return new LongRange(tree.getSourcePosition(), tree.getLength());
	}

	private TypedPEG translate(Tree<?> tree) {
		visit(tree);
		TypedPEG g = this.generated;
		this.generated = null;
		return g;
	}

	public final class _RootExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			RuleExpr[] ruleExprs = new RuleExpr[node.size()];
			int index = 0;
			for (Tree<?> t : node) {
				ruleExprs[index++] = (RuleExpr) translate(t);
			}
			generated = new RootExpr(range(node), ruleExprs);
		}
	}

	public final class _RuleExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String typeName = node.get(1).toText();
			if (typeName.equals("")) { // untyped
				generated = new RuleExpr(range(node), node.get(0).toText(), translate(node.get(2)));
			} else {
				generated = new TypedRuleExpr(range(node), node.get(0).toText(), typeName, translate(node.get(2)));
			}
		}
	}

	public final class _ChoiceExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new ChoiceExpr(translate(node.get(0)), translate(node.get(1)));
		}
	}

	public final class _SequenceExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new SequenceExpr(translate(node.get(0)), translate(node.get(1)));
		}
	}

	public final class _LabeledExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new LabeledExpr(range(node), node.get(0).toText(), translate(node.get(1)));
		}
	}

	public final class _AndExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = PredicateExpr.andPredicate(range(node), translate(node.get(0)));
		}
	}

	public final class _NotExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = PredicateExpr.notPredicate(range(node), translate(node.get(0)));
		}
	}

	public final class _ZeroMoreExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = RepeatExpr.zeroMore(range(node), translate(node.get(0)));
		}
	}

	public final class _OneMoreExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = RepeatExpr.oneMore(range(node), translate(node.get(0)));
		}
	}

	public final class _OptionalExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new OptionalExpr(range(node), translate(node.get(0)));
		}
	}

	public final class _NonTerminal extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new NonTerminalExpr(range(node), node.toText());
		}
	}

	public final class _AnyExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new AnyExpr(range(node));
		}
	}

	public final class _StringExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new StringExpr(range(node), node.toText());
		}
	}

	public final class _CharClassExpr extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			generated = new CharClassExpr(range(node), node.toText());
		}
	}

}
