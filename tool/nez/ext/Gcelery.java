package nez.ext;

import java.io.IOException;

import nez.Grammar;
import nez.Parser;
import nez.ParserStrategy;
import nez.ast.Symbol;
import nez.ast.Tree;
import nez.lang.GrammarFileLoader;
import nez.lang.schema.JSONSchemaGrammarGenerator;
import nez.lang.schema.SchemaGrammarGenerator;
import nez.lang.schema.Type;
import nez.util.ConsoleUtils;

public class Gcelery extends GrammarFileLoader {

	public Gcelery() {
		init(Gcelery.class, new Undefined());
	}

	public class Undefined extends DefaultVisitor {
		@Override
		public void accept(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in Celery #" + node));
		}

		@Override
		public Type toSchema(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted in Celery #" + node));
			return null;
		}
	}

	static Parser celeryParser;
	boolean enableNezExtension;
	private SchemaGrammarGenerator schema;
	private String currentStructName;

	@Override
	public Parser getLoaderParser(String start) {
		if (celeryParser == null) {
			try {
				ParserStrategy option = ParserStrategy.newSafeStrategy();
				Grammar g = GrammarFileLoader.loadGrammar("celery.nez", option);
				celeryParser = g.newParser(option);
				strategy.report();
			} catch (IOException e) {
				ConsoleUtils.exit(1, "unload: " + e.getMessage());
			}
			assert (celeryParser != null);
		}
		this.schema = new JSONSchemaGrammarGenerator(getGrammarFile());
		this.enableNezExtension = !strategy.isEnabled("peg", ParserStrategy.PEG);
		return celeryParser;
	}

	private final void visit(Tree<?> node) {
		find(node.getTag().toString()).accept(node);
	}

	private final Type toType(Tree<?> node) {
		return find(node.getTag().toString()).toSchema(node);
	}

	@Override
	public void parse(Tree<?> node) {
		schema.loadPredefinedRules();
		visit(node);
		getGrammarFile().dump();
	}

	public final static Symbol _Name = Symbol.tag("Name");
	public final static Symbol _Type = Symbol.tag("Type");
	public final static Symbol _Range = Symbol.tag("Range");
	public final static Symbol _Length = Symbol.tag("Length");
	public final static Symbol _Size = Symbol.tag("Size");
	public final static Symbol _Max = Symbol.tag("Max");
	public final static Symbol _Min = Symbol.tag("Min");

	public final class Root extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String rootStructName = node.get(0).getText(_Name, "");
			for (Tree<?> structNode : node) {
				visit(structNode);
			}
			schema.newRoot(rootStructName);
		}
	}

	public final class Struct extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			currentStructName = node.getText(0, null);
			schema.initMemberList();
			for (Tree<?> memberNode : node) {
				visit(memberNode);
			}
			schema.newSymbols();
			if (enableNezExtension) {
				genStruct(currentStructName);
			} else {
				genStruct_Approximate(currentStructName);
			}
		}
	}

	public final class Name extends Undefined {
		@Override
		public void accept(Tree<?> node) {
		}
	}

	public final class Required extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String elementName = node.getText(_Name, "");
			schema.addRequired(elementName);
			schema.newElement(getUniqueName(elementName), schema.newUniq(elementName, toType(node.get(_Type))));
		}
	}

	public final class Option extends Undefined {
		@Override
		public void accept(Tree<?> node) {
			String elementName = node.getText(_Name, "");
			schema.addMember(elementName);
			schema.newElement(getUniqueName(elementName), schema.newUniq(elementName, toType(node.get(_Type))));
		}
	}

	public class _Type extends Undefined {
		@Override
		public Type toSchema(Tree<?> node) {
			ConsoleUtils.println(node.formatSourceMessage("error", "unsupproted type #" + node));
			return null;
		}
	}

	public final class TObject extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			if (node.has(_Range)) {
				int min = Integer.parseInt(node.getText(_Min, ""));
				int max = Integer.parseInt(node.getText(_Max, ""));
				// return schema.newTObject(min, max);
			}
			return schema.newTObject();
		}
	}

	public final class TStruct extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			return schema.newTStruct(node.toText());
		}
	}

	public final class TAny extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			return schema.newTAny();
		}
	}

	public final class TArray extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			if (node.has(_Range)) {
				int min = Integer.parseInt(node.getText(_Min, ""));
				int max = Integer.parseInt(node.getText(_Max, ""));
				// return schema.newTArray(toType(node), min, max);
			}
			return schema.newTArray(toType(node.get(0)));
		}
	}

	public final class TEnum extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			String[] candidates = new String[node.size()];
			int index = 0;
			for (Tree<?> subnode : node) {
				candidates[index++] = subnode.toText();
			}
			return schema.newTEnum(candidates);
		}
	}

	public final class TInteger extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			if (node.has(_Range)) {
				int min = Integer.parseInt(node.getText(_Min, ""));
				int max = Integer.parseInt(node.getText(_Max, ""));
				// return schema.newTInteger(min, max);
			}
			return schema.newTInteger();
		}
	}

	public final class TFloat extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			if (node.has(_Range)) {
				float min = Float.parseFloat(node.getText(_Min, ""));
				float max = Float.parseFloat(node.getText(_Max, ""));
				// return schema.newTFloat(min, max);
			}
			return schema.newTFloat();
		}
	}

	public final class TString extends _Type {
		@Override
		public Type toSchema(Tree<?> node) {
			if (node.has(_Length)) {
				int min = Integer.parseInt(node.getText(_Min, ""));
				int max = Integer.parseInt(node.getText(_Max, ""));
				// return schema.newTString(min, max);
			}
			return schema.newTString();
		}
	}

	private final void genStruct(String structName) {
		String memberListName = String.format("%s_SMembers", structName);
		genMembers(memberListName);
		schema.newStruct(structName, schema.newSet(memberListName));
	}

	private final void genMembers(String structMemberListName) {
		schema.newMembers(structMemberListName);
	}

	private final void genStruct_Approximate(String structName) {
		schema.newStruct(structName, schema.newPermutation());
	}

	private final String getUniqueName(String localName) {
		return currentStructName + "_" + localName;
	}

}