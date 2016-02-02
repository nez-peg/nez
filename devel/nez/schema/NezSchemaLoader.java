package nez.schema;

import java.lang.reflect.Type;

import nez.ast.Symbol;
import nez.ast.Tree;
import nez.ast.TreeVisitorMap;
import nez.type.Schema;
import nez.type.Schema.ObjectType;
import nez.type.Schema.Property;

public class NezSchemaLoader extends TreeVisitorMap<NezSchemaVisitor> {

	Schema schema;

	public Type load(Tree<?> node) {
		Type startType = (Type) this.visit(node.get(0));
		for (int i = 1; i < node.size(); i++) {
			this.visit(node.get(i));
		}
		return startType;
	}

	public Object visit(Tree<?> node) {
		return find(node.getTag().toString()).accept(node);
	}

	public NezSchemaLoader() {
		new NezSchemaLoader(new Schema());
	}

	public NezSchemaLoader(Schema schema) {
		this.schema = schema;
		addBuiltInTypes();
		this.init(NezSchemaLoader.class, new Undefined());
	}

	// Temporary use
	private final void addBuiltInTypes() {
		this.schema.newType("#String");
		this.schema.newType("#Number");
		this.schema.newType("#Integer");
		this.schema.newType("#Float");
		this.schema.newType("#Boolean");
	}

	public class Undefined implements NezSchemaVisitor {
		@Override
		public Object accept(Tree<?> node) {
			undefined(node);
			return null;
		}
	}

	// Top of Type Declaration

	public class _ObjectType extends Undefined implements NezSchemaSymbols {
		@Override
		public Object accept(Tree<?> node) {
			String name = node.getText(_Name, "");
			Tree<?> propList = node.get(_List);
			Property[] members = new Property[propList.size()];
			int index = 0;
			for (Tree<?> prop : propList) {
				members[index++] = (Property) visit(prop);
			}
			ObjectType oType = new ObjectType(name.substring(1), members);
			schema.add(name, oType);
			return oType;
		}
	}

	public class _ListType extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			// TODO Auto-generated method stub
			super.accept(node);
			return null;
		}
	}

	// Property Definition

	public class _Property extends Undefined implements NezSchemaSymbols {
		@Override
		public Object accept(Tree<?> node) {
			Type type = (Type) visit(node.get(_Type));
			return new Property(Symbol.unique(node.getText(_Name, "")), type);
		}
	}

	// Types for Specified Properties

	public class _TreeType extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			String typeName = node.toText();
			Type mappedType = schema.getType(typeName);
			return mappedType == null ? schema.newType(typeName) : mappedType;
		}
	}

	public class _Union extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			Type[] unions = new Type[node.size()];
			int index = 0;
			for (Tree<?> candidate : node) {
				unions[index++] = (Type) visit(candidate);
			}
			return new Schema.UnionType(unions);
		}
	}

	public class _Option extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			return new Schema.OptionType((Type) visit(node.get(0)));
		}
	}

	public class _ZeroMore extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			return new Schema.ZeroMoreType((Type) visit(node.get(0)));

		}
	}

	public class _OneMore extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			return new Schema.OneMoreType((Type) visit(node.get(0)));
		}
	}

	public class _Reference extends Undefined {
		@Override
		public Object accept(Tree<?> node) {
			// TODO Auto-generated method stub
			super.accept(node);
			return null;
		}
	}

}

interface NezSchemaVisitor {
	public Object accept(Tree<?> node);
}

interface NezSchemaSymbols {
	static Symbol _Name = Symbol.unique("name");
	static Symbol _List = Symbol.unique("list");
	static Symbol _Fixed = Symbol.unique("fixed");
	static Symbol _Type = Symbol.unique("type");
	static Symbol _Alias = Symbol.unique("alias");
	static Symbol _Value = Symbol.unique("value");
}
