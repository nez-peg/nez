package nez.lang;

import java.util.AbstractList;
import java.util.HashMap;

import nez.ast.SourceLocation;
import nez.ast.Tree;
import nez.lang.ast.NezExpressionConstructor;
import nez.lang.ast.NezGrammarCombinator;
import nez.parser.ParserOptimizer;
import nez.parser.Parser;
import nez.parser.ParserStrategy;
import nez.util.ConsoleUtils;
import nez.util.UList;
import nez.util.Verbose;

public class Grammar extends AbstractList<Production> {
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

	final String uniqueName(String name) {
		if (name.indexOf(':') == -1) {
			return name; // already prefixed;
		}
		return this.ns + this.id + ":" + name;
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
		if (size() > 0) {
			return this.prodList.ArrayValues[0];
		}
		return this.addProduction("START", Expressions.newEmpty(null));
	}

	public final boolean hasProduction(String name) {
		return this.getLocalProduction(name) != null;
	}

	public final Production getProduction(String name) {
		if (name == null) {
			return this.getStartProduction();
		}
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

	public final Production addProduction(SourceLocation s, String name, Expression e) {
		Production p = new Production(s, this, name, e);
		addProduction(p);
		return p;
	}

	public final Production addProduction(String name, Expression e) {
		return addProduction(null, name, e);
	}

	private void addProduction(Production p) {
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

	public void update(UList<Production> prodList) {
		this.prodList = prodList;
		this.prodMap = new HashMap<>();
		for (Production p : prodList) {
			this.prodMap.put(p.getLocalName(), p);
		}
	}

	public void dump() {
		for (Production p : this) {
			ConsoleUtils.println(p.getLocalName() + " = " + p.getExpression());
		}
	}

	public final static String nameTerminalProduction(String t) {
		return "\"" + t + "\"";
	}

	// ----------------------------------------------------------------------
	/* MetaData */

	protected HashMap<String, Object> metaMap = null;

	public Object getMetaData(String key) {
		if (this.metaMap != null) {
			Object v = this.metaMap.get(key);
			if (v != null) {
				return v;
			}
		}
		if (this.parent != null) {
			return parent.getMetaData(key);
		}
		return null;
	}

	public void setMetaData(String key, Object value) {
		if (this.metaMap == null) {
			this.metaMap = new HashMap<>();
		}
		this.metaMap.put(key, value);
	}

	public String getDesc() {
		return (String) this.getMetaData("desc");
	}

	public void setDesc(String desc) {
		this.setMetaData("desc", desc);
	}

	public String getURN() {
		return (String) this.getMetaData("urn");
	}

	public void setURN(String urn) {
		this.setMetaData("urn", urn);
	}

	// ----------------------------------------------------------------------

	private Parser nezExpressionParser() {
		if (getMetaData("_parser") != null) {
			Grammar grammar = new Grammar("nez");
			ParserStrategy strategy = ParserStrategy.newSafeStrategy();
			this.setMetaData("_parser", new NezGrammarCombinator().load(grammar, "Expression").newParser(strategy));
			this.setMetaData("_constructor", new NezExpressionConstructor(this, strategy));
		}
		return (Parser) this.getMetaData("_parser");
	}

	/**
	 * Creates an expression by parsing the given text.
	 * 
	 * @param expression
	 * @return null if the parsing fails.
	 */

	public final Expression newExpression(String expression) {
		Tree<?> parsed = nezExpressionParser().parse(expression);
		if (parsed != null) {
			return ((NezExpressionConstructor) getMetaData("_constructor")).newInstance(parsed);
		}
		return null;
	}

	// ----------------------------------------------------------------------

	/**
	 * Create a new parser
	 * 
	 * @param strategy
	 * @return
	 */

	public final Parser newParser(ParserStrategy strategy) {
		Grammar gg = new ParserOptimizer().optimize(this.getStartProduction(), strategy, null);
		return new Parser(gg, strategy);
	}

	public final Parser newParser(String name) {
		return newParser(name, ParserStrategy.newDefaultStrategy());
	}

	public final Parser newParser(String name, ParserStrategy strategy) {
		if (name != null) {
			Production p = this.getProduction(name);
			if (p != null) {
				Grammar gg = new ParserOptimizer().optimize(p, strategy, null);
				return new Parser(gg, strategy);
			}
			Verbose.println("undefined production: " + name);
		}
		return newParser(strategy);
	}

}
