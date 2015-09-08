package nez.lang;

import java.util.HashMap;
import java.util.List;

import nez.util.UList;

public class GrammarMap {
	// private static int nsid = 0;

	GrammarMap parent;
	UList<Production> prodList;
	HashMap<String, Production> prodMap = null;

	// TreeMap<String, Boolean> condMap = null;

	GrammarMap(GrammarMap parent) {
		this.parent = parent;
		this.prodList = new UList<Production>(new Production[1]);
	}

	public final boolean isEmpty() {
		return this.prodList.size() == 0;
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
			if (p.getLocalName().equals(name)) {
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
		if (p2 != null) {
			this.prodList.add(p);
			if (this.prodMap == null && this.prodList.size() > 4) {
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

}
