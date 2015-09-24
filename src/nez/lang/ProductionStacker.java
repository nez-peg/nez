package nez.lang;

public final class ProductionStacker {
	int n;
	ProductionStacker prev;
	Production p;

	public ProductionStacker(Production p, ProductionStacker prev) {
		this.prev = prev;
		this.p = p;
		this.n = (prev == null) ? 0 : prev.n + 1;
	}

	public boolean isVisited(Production p) {
		ProductionStacker d = this;
		while (d != null) {
			if (d.p == p) {
				return true;
			}
			d = d.prev;
		}
		return false;
	}
}
