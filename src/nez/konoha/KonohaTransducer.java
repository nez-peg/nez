package nez.konoha;

import java.util.HashMap;

import nez.konoha.KonohaLanguage.OperatorTypeRule;
import nez.main.Verbose;
import nez.util.ConsoleUtils;

public class KonohaTransducer {
	Konoha root;
	KonohaTransducer parent;
	HashMap<String, KonohaTypeRule> typeRuleMap;
	HashMap<String, KonohaType> typeMap;
	HashMap<String, KonohaTree> nameMap;

	public KonohaTransducer(Konoha root) {
		this.root = root;
		this.parent = null;
		initRoot();
	}

	public KonohaTransducer(KonohaTransducer parent) {
		this.root = parent.root;
		this.parent = parent;
	}

	private void initRoot() {
		this.setType("bool", new KonohaPrimitiveType("bool"));
		this.setType("int", new KonohaPrimitiveType("int"));
		this.setType("float", new KonohaPrimitiveType("float"));
		this.setType("string", new KonohaPrimitiveType("string"));
		// for(char c = 'a'; c <= 'z'; c++) {
		// String n = String.valueOf(c);
		// this.setType(n, new KonohaGreekType(n));
		// }
		new KonohaLanguage(this);
	}

	public void setTypeRule(KonohaTypeRule rule) {
		if (this.typeRuleMap == null) {
			this.typeRuleMap = new HashMap<>();
		}
		OperatorTypeRule oldTypeRule = (OperatorTypeRule) this.typeRuleMap.get(rule.getName());
		if (oldTypeRule != null) {
			oldTypeRule.appendTypes(rule);
		} else {
			this.typeRuleMap.put(rule.getName(), rule);
		}
	}

	public final KonohaType typeCheck(KonohaType req, KonohaTree node) {
		if (req instanceof KonohaErrorType) {
			req = null;
		}
		KonohaType t = typeCheck(this, req, node);
		if (req != null && t != null) {
			if (!req.matchType(t)) {
				if (!(t instanceof KonohaErrorType)) {
					t = new KonohaErrorType(node, "typeerror " + req + " <> " + t);
					node.typed = t;
				}
			}
		}
		return t;
	}

	private KonohaType typeCheck(KonohaTransducer konoha, KonohaType req, KonohaTree node) {
		if (node.matched != null) {
			if (node.typed != null) {
				node.matched.match(konoha, node);
			}
			return node.typed;
		}
		String ruleName = node.getRuleName();
		boolean found = false;
		while (konoha != null) {
			if (konoha.typeRuleMap != null) {
				KonohaTypeRule rule = konoha.typeRuleMap.get(ruleName);
				while (rule != null) {
					found = true;
					rule.match(this, node);
					if (node.matched != null) {
						return node.typed;
					}
					rule = rule.nextChoice;
				}
			}
			konoha = konoha.parent;
		}
		if (!found) {
			Verbose.println("undefined rule: '" + ruleName + "'\n" + node);
		}
		return null;
	}

	public final void setType(String name, KonohaType t) {
		if (this.typeMap == null) {
			this.typeMap = new HashMap<>();
		}
		this.typeMap.put(name, t);
	}

	public final KonohaType getType(String name) {
		return getType(this, name);
	}

	private KonohaType getType(KonohaTransducer konoha, String name) {
		while (konoha != null) {
			if (konoha.typeMap != null) {
				KonohaType t = konoha.typeMap.get(name);
				if (t != null) {
					return t;
				}
			}
			konoha = konoha.parent;
		}
		return null;
	}

	public final KonohaType getType(KonohaTree node) {
		if (node.size() == 0) {
			return getType(this, node.toText());
		}
		if (node.getTag() == KonohaArrayType.ArrayTag) {
			KonohaType t = getType(node.get(0));
			KonohaArrayType.newArrayType(t);
		}
		Verbose.println("unknown type: " + node);
		return null;
	}

	public final void setName(String name, KonohaTree nameNode) {
		if (this.nameMap == null) {
			this.nameMap = new HashMap<>();
		}
		this.nameMap.put(name, nameNode);
	}

	public final KonohaTree getName(String name) {
		return getName(this, name);
	}

	private KonohaTree getName(KonohaTransducer konoha, String name) {
		while (konoha != null) {
			if (konoha.nameMap != null) {
				KonohaTree t = konoha.nameMap.get(name);
				if (t != null) {
					return t;
				}
			}
			konoha = konoha.parent;
		}
		return null;
	}

	public final boolean importFile(String path) {
		Verbose.println("loading:" + path);
		return root.loadFile(this, path);
	}

	boolean eval(KonohaTree node) {
		if (node != null) {
			this.typeCheck(null, node);
			Verbose.println("typed: \n" + node);
			return true;
		}
		return false;
	}

	public void report(KonohaTree node, String errorType, String msg) {
		ConsoleUtils.println(node.getSource().formatPositionLine(errorType, node.getSourcePosition(), msg));
	}

}
