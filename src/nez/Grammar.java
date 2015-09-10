package nez;

import java.util.HashMap;
import java.util.List;

import nez.ast.SourcePosition;
import nez.lang.Expression;
import nez.lang.GrammarBase;
import nez.lang.Production;
import nez.util.ConsoleUtils;
import nez.util.UList;

public class Grammar extends GrammarBase {
	private static int serialNumbering = 0;

	private int id;
	private Grammar parent;
	private UList<Production> prodList;
	private HashMap<String, Production> prodMap = null;

	public Grammar() {
		this(null);
	}

	public Grammar(Grammar parent) {
		this.id = serialNumbering++;
		this.parent = parent;
		this.prodList = new UList<Production>(new Production[1]);
	}

	public final int id() {
		return this.id;
	}

	public final String uniqueName(String name) {
		return "g" + this.id + ":" + name;
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

	public final Parser newParser(NezOption option) {
		return new Parser(this.getStartProduction(), option);
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

	public final void setSymbolExpresion(String tableName, Expression e) {
		Production p = this.newProduction(Production.PublicProduction, "^" + tableName, e);
		addProduction(p);
	}

	public final Expression getSymbolExpresion(String tableName) {
		Production p = this.getProduction("^" + tableName);
		if (p != null) {
			return p.getExpression();
		}
		return p;
	}

	// ----------------------------------------------------------------------

	// Grammar

	public final void reportError(Expression p, String message) {
		this.reportError(p.getSourcePosition(), message);
	}

	public final void reportError(SourcePosition s, String message) {
		if (s != null) {
			ConsoleUtils.println(s.formatSourceMessage("error", message));
		}
	}

	public final void reportWarning(Expression p, String message) {
		this.reportWarning(p.getSourcePosition(), message);
	}

	public final void reportWarning(SourcePosition s, String message) {
		if (s != null) {
			ConsoleUtils.println(s.formatSourceMessage("warning", message));
		}
	}

	public final void reportNotice(Expression p, String message) {
		this.reportNotice(p.getSourcePosition(), message);
	}

	public final void reportNotice(SourcePosition s, String message) {
		// if (option.enabledNoticeReport) {
		if (s != null) {
			ConsoleUtils.println(s.formatSourceMessage("notice", message));
		}
		// }
	}

	// ----------------------------------------------------------------------

}
