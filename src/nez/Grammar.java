package nez;

import java.util.HashMap;
import java.util.List;

import nez.lang.GrammarBase;
import nez.lang.Production;
import nez.parser.GenerativeGrammar;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class Grammar extends GrammarBase {
	private static int serialNumbering = 0;

	private int id;
	private final String ns;
	private Grammar parent;
	protected UList<Production> prodList;
	protected HashMap<String, Production> prodMap = null;

	public Grammar() {
		this(null, null);
	}

	public Grammar(String ns) {
		this(ns, null);
	}

	public Grammar(String ns, Grammar parent) {
		this.id = serialNumbering++;
		this.parent = parent;
		this.ns = ns != null ? ns : "g";
		this.prodList = new UList<Production>(new Production[1]);
	}

	public final int id() {
		return this.id;
	}

	public final String uniqueName(String name) {
		if (name.indexOf(':') == -1) {
			return name; // already prefixed;
		}
		return this.ns + this.id + ":" + name;
	}

	@Override
	public final boolean isEmpty() {
		return this.prodList.size() == 0;
	}

	@Override
	public final int size() {
		return this.prodList.size();
	}

	@Override
	public final Production get(int index) {
		return this.prodList.ArrayValues[index];
	}

	public final Production getStartProduction() {
		return this.prodList.ArrayValues[0];
	}

	public final List<Production> getProductionList() {
		return this.prodList;
	}

	public final Production getProduction(String name) {
		Production p = this.getLocalProduction(name);
		if (p == null && this.parent != null) {
			return this.parent.getProduction(name);
		}
		return p;
	}

	private Production getLocalProduction(String name) {
		if (prodMap != null) {
			return this.prodMap.get(name);
		}
		for (Production p : this.prodList) {
			if (name.equals(p.getLocalName())) {
				return p;
			}
		}
		return null;
	}

	public final boolean hasProduction(String name) {
		return this.getLocalProduction(name) != null;
	}

	@Override
	public final void addProduction(Production p) {
		Production p2 = this.getLocalProduction(p.getLocalName());
		if (p2 == null) {
			this.prodList.add(p);
			if (this.prodMap != null) {
				this.prodMap.put(p.getLocalName(), p);
			} else if (this.prodList.size() > 4) {
				this.prodMap = new HashMap<String, Production>();
				for (Production p3 : this.prodList) {
					this.prodMap.put(p3.getLocalName(), p3);
				}
			}
		} else {
			String name = p.getLocalName();
			for (int i = 0; i < this.prodList.size(); i++) {
				p2 = this.prodList.ArrayValues[i];
				if (name.equals(p2.getLocalName())) {
					this.prodList.ArrayValues[i] = p;
					if (this.prodMap != null) {
						this.prodMap.put(name, p);
					}
					break;
				}
			}
		}
		if (p.isPublic() && this.parent != null) {
			this.parent.addProduction(p2);
		}
	}

	public void dump() {
		for (Production p : this) {
			ConsoleUtils.println(p.getLocalName() + " = " + p.getExpression());
		}
	}

	// public final void setSymbolExpresion(String tableName, Expression e) {
	// this.newProduction(null, Production.PublicProduction |
	// Production.SymbolTableProduction, ":^" + tableName, e);
	// }
	//
	// public final Expression getSymbolExpresion(String tableName) {
	// Production p = this.getProduction(":^" + tableName);
	// if (p != null) {
	// return p.getExpression();
	// }
	// return null;
	// }

	// ----------------------------------------------------------------------

	public String getDesc() {
		if (this.parent != null) {
			return parent.getDesc();
		}
		return null;
	}

	public void setDesc(String desc) {
		/* Description is supported in GrammarFile */
	}

	// ----------------------------------------------------------------------
	/**
	 * Create a new parser
	 * 
	 * @param strategy
	 * @return
	 */
	public final Parser newParser(Strategy strategy) {
		GenerativeGrammar gg = (this instanceof GenerativeGrammar) ? (GenerativeGrammar) this : new GenerativeGrammar(this.getStartProduction(), strategy, null);
		return new Parser(gg, strategy);
	}

	public final Parser newParser(String name) {
		return newParser(name, Strategy.newDefaultStrategy());
	}

	public final Parser newParser(String name, Strategy strategy) {
		if (name != null) {
			Production p = this.getProduction(name);
			if (p != null) {
				GenerativeGrammar gg = new GenerativeGrammar(p, strategy, null);
				return new Parser(gg, strategy);
			}
			Verbose.println("undefined production: " + name);
		}
		return newParser(strategy);
	}

}
