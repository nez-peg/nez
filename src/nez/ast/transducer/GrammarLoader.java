package nez.ast.transducer;

import nez.Grammar;
import nez.Strategy;
import nez.ast.Tree;
import nez.lang.Expression;
import nez.lang.GrammarFile;
import nez.lang.Production;

public final class GrammarLoader extends GrammarVisitorMap<GrammarLoaderVisitor> {

	public GrammarLoader(Grammar grammar, Strategy strategy) {
		super(grammar, strategy);
		this.init(GrammarLoader.class, new Undefined());
	}

	public void load(Tree<?> node) {
		try {
			find(node.getTag().getSymbol()).accept(node);
		} catch (TransducerException e) {
			e.getMessage();
		}
	}

	public class Undefined implements GrammarLoaderVisitor {
		@Override
		public void accept(Tree<?> node) {
			throw new TransducerException(node, "undefined " + node);
		}
	}

	public class Source implements GrammarLoaderVisitor {
		@Override
		public void accept(Tree<?> node) {
			for (Tree<?> sub : node) {
				load(sub);
			}
		}
	}

	public class _Production implements GrammarLoaderVisitor, NezSymbols {
		ExpressionTransducer transducer = new NezExpressionTransducer(getGrammar(), getStrategy());

		@Override
		public void accept(Tree<?> node) {
			Tree<?> nameNode = node.get(_name);
			String localName = nameNode.toText();
			int productionFlag = 0;
			if (nameNode.is(_String)) {
				localName = GrammarFile.nameTerminalProduction(localName);
				productionFlag |= Production.TerminalProduction;
			}

			Production rule = getGrammar().getProduction(localName);
			if (rule != null) {
				reportWarning(node, "duplicated rule name: " + localName);
				rule = null;
			}
			Expression e = transducer.newInstance(node.get(_expr));
			rule = getGrammar().newProduction(node.get(0), productionFlag, localName, e);
		}
	}
}