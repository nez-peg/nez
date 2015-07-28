package nez.konoha;

import nez.string.StringTransducer;
import nez.string.StringTransducerCombinator;

public class KonohaLanguage extends StringTransducerCombinator {
	
	KonohaLanguage(KonohaTransducer konoha) {
		konoha.setTypeRule(new TopLevelTypeRule(key("Source")));
		konoha.setTypeRule(new ImportTypeRule(key("Import")));
		konoha.setTypeRule(new MetaDeclTypeRule(key("MetaDecl")));
		konoha.setTypeRule(new VarDeclTypeRule(key("VarDecl")));
		konoha.setTypeRule(new VarTypeRule(key("Name")));
		
		defineLiteral(konoha, "#True", "bool", Asis());
		defineLiteral(konoha, "#False", "bool", Asis());
		defineLiteral(konoha, "#Integer", "int", Asis());
		defineLiteral(konoha, "#Float", "float", Asis());
		defineLiteral(konoha, "#String", "string", Asis());

		defineBinary(konoha, "#Add", "int", "int", "int", "+");

	}
	
	private String key(String tagname) {
		return KonohaTree.keyTag(tagname);
	}

	private void defineLiteral(KonohaTransducer konoha, String tname, String type, StringTransducer st) {
		KonohaType t = konoha.getType(type);
		konoha.setTypeRule(new LiteralTypeRule(tname, t, st));
	}

	private void defineBinary(KonohaTransducer konoha, String tname, String rtype, String type1, String type2, String op) {
		KonohaType rt = konoha.getType(rtype);
		KonohaType t1 = konoha.getType(type1);
		KonohaType t2 = konoha.getType(type2);
		KonohaType[] types = {rt, t1, t2};
		konoha.setTypeRule(new OperatorTypeRule(tname, types, make(Node(0), S(op), Node(1))));
	}

	class TopLevelTypeRule extends KonohaTypeRule {
		public TopLevelTypeRule(String name) {
			super(name, -1, make(NL(), RangeNode(0, NL(), -1)));
		}
		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			for(KonohaTree subnode : node) {
				konoha.typeCheck(null, subnode);
				if(subnode.matched != null) {
					KonohaBuilder kb = new KonohaBuilder();
					StringTransducer st = kb.lookup(subnode);
					st.trasformTo(subnode, kb);
					System.out.println("Transformed: " + kb);
				}
			}
			super.match(konoha, node);
		}
	}
	
	class ImportTypeRule extends KonohaTypeRule {
		public ImportTypeRule(String name) {
			super(name, 1, Empty());
		}
		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			String path = node.textAt(0, "");
			konoha.importFile(path);
			super.match(konoha, node);
		}
	}

	class MetaDeclTypeRule extends KonohaTypeRule {
		public MetaDeclTypeRule(String name) {
			super(name, 3, Empty());
		}
		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			System.out.println("META: node" + node);
			// META = NAME FUNCTYPE FORMAT REQUIRED
			System.out.println("META: name" + node.textAt(0, ""));
			System.out.println("META: type" + node.get(1));
			System.out.println("META: format" + node.get(2));
			super.match(konoha, node);
		}

		KonohaType[] constructTypes(KonohaTransducer konoha, KonohaTree node) {
			KonohaType[] types = new KonohaType[node.size()];
			for(int i = 0; i < node.size(); i++) {
				KonohaTree subnode = node.get(i);
				KonohaType t = konoha.getType(subnode);
				if(t == null) {
					konoha.report(subnode, "error", "undefined type");
					return null;
				}
				types[i] = t;
			}
			return types;
		}
	}

	class VarDeclTypeRule extends KonohaTypeRule {
		public VarDeclTypeRule(String name) {
			super(name, 2, Empty());
		}
		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			KonohaType t = konoha.typeCheck(null, node.get(1));
			node.matched = this;
			if(t != null) {
				KonohaTree nameNode = node.get(0);
				String name = node.getText();				
				nameNode.typed = t;
				konoha.setName(name, nameNode);
			}
		}
	}

	class VarTypeRule extends KonohaTypeRule {
		public VarTypeRule(String name) {
			super(name, 0, Asis());
		}
		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			KonohaTree t = konoha.getName(node.getText());
			node.matched = this;
			if(t != null) {
				node.typed = t.typed;
			}
			else {
				node.typed = KonohaType.newErrorType(node, "undefined name: " + node.getText());
			}
		}
	}
	
	class ReturnTypeRule extends KonohaTypeRule {
		public ReturnTypeRule(String name) {
			super(name, 1, Asis());
		}

		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			KonohaTree nameNode = konoha.getName("");
			node.matched = this;
			node.typed = KonohaType.VoidType;
			if(nameNode != null) {
				if(node.size() == 0) {
					nameNode.typed.matchType(KonohaType.VoidType);
				}
				else {
					konoha.typeCheck(nameNode.typed, node.get(1));
				}
			}
		}
	}

	class FuncDeclTypeRule extends KonohaTypeRule {
		public FuncDeclTypeRule(String name, StringTransducer st) {
			super(name, 3, Asis());
		}
		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			String name = node.textAt(0, "");
			konoha.setTypeRule(new FuncTypeInferRule(konoha, name, node.get(1).size(), node));
			super.match(konoha, node);
		}
		
	}

	class FuncTypeInferRule extends KonohaTypeRule {
		KonohaTransducer ns;
		KonohaTree funcNode;
		public FuncTypeInferRule(KonohaTransducer konoha, String name, int size, KonohaTree funcNode) {
			super(name, size, Asis());
			this.funcNode = funcNode;
			this.ns = konoha;
		}
		
		@Override
		public void match(KonohaTransducer konoha, KonohaTree node) {
			KonohaTransducer funcLevel = new KonohaTransducer(ns);
			KonohaTree funcNode = node.dup();
			KonohaType[] types = setTypeVariable(konoha, node, funcLevel, funcNode.get(1));
			appendTypeRule(node.textAt(0, ""), types);
			funcLevel.typeCheck(null, funcNode.get(2));
			checkTypeVariable(types);
		}

		private KonohaType[] setTypeVariable(KonohaTransducer konoha, KonohaTree applyNode, KonohaTransducer funcLevel, KonohaTree funcNameNode) {
			KonohaType[] types = new KonohaType[funcNameNode.size()+1];
			types[0] = new KonohaVarType("", null);
			funcNameNode.typed = types[0];
			funcLevel.setName("", funcNameNode);  // return type
			for(int i = 0; i < funcNameNode.size(); i++) {
				KonohaTree nameNode = funcNameNode.get(i);
				KonohaType t = konoha.typeCheck(null, applyNode.get(i));
				types[i+1] = new KonohaVarType(nameNode.getText(), t);
				nameNode.typed = types[i+1];
				funcLevel.setName(nameNode.getText(), nameNode);
			}
			return types;
		}

		private void appendTypeRule(String name, KonohaType[] types) {
			OperatorTypeRule st = new OperatorTypeRule(name, types, Asis());
			st.nextChoice = this.nextChoice;
			this.nextChoice = st;
		}

		private boolean checkTypeVariable(KonohaType[] types) {
			int unresolved = 0;
			int resolved   = 0;
			int errors      = 0;
			for(int i = 0; i < types.length; i++) {
				if(types[i] instanceof KonohaVarType) {
					KonohaVarType var = (KonohaVarType)types[i];
					if(var.isUnresolved()) {
						unresolved++;
					}
					if(var.isResolved()) {
						resolved++;
					}
					if(var.isError()) {
						errors++;
					}
					types[i] = var.getResolvedType();
				}
			}
			return true;
		}
	}

	class LiteralTypeRule extends KonohaTypeRule {
		KonohaType type;  // types[0] is return type
		public LiteralTypeRule(String name, KonohaType type, StringTransducer st) {
			super(name, 0, st);
			this.type = type;
		}
		public final void match(KonohaTransducer konoha, KonohaTree node) {
			node.matched = this;
			node.typed = this.type;
		}
		
	}

	class OperatorTypeRule extends KonohaTypeRule {
		KonohaType[] types;  // types[0] is return type
		int shift = -1;
		
		public OperatorTypeRule(String name, KonohaType[] types, StringTransducer st) {
			super(name, types.length - 1, st);
			this.types = types;
		}
		
		public final void match(KonohaTransducer konoha, KonohaTree node) {
			if(node.size() - shift != types.length) {
				return;
			}
			for(int i = 1; i < types.length; i++) {
				KonohaType reqT = types[i];
				konoha.typeCheck(reqT, node.get(i + shift));
			}
			node.matched = this;
			node.typed = this.types[0];
		}
	}


}
