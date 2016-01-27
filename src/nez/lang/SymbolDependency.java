package nez.lang;

public enum SymbolDependency {
	Independent, Dependent, Undecided;

	public static interface SymbolDependencyAnalyzer extends PropertyAnalyzer<SymbolDependency> {
		public boolean isDependent(Expression e);
	}

	public static final SymbolDependencyAnalyzer newAnalyzer() {
		return new SymbolDependencyVisitor();
	}

	final static class SymbolDependencyVisitor extends Expression.AnalyzeVisitor<SymbolDependency> implements SymbolDependencyAnalyzer {

		protected SymbolDependencyVisitor() {
			super(Independent, Undecided);
		}

		@Override
		public boolean isDependent(Expression e) {
			SymbolDependency s = (SymbolDependency) e.visit(this, null);
			return (s != SymbolDependency.Independent);
		}

		@Override
		public final Object visitBlockScope(Nez.BlockScope e, Object a) {
			return this.analyzeInners(e);
		}

		@Override
		public final Object visitLocalScope(Nez.LocalScope e, Object a) {
			return this.analyzeInners(e);
		}

		@Override
		public final Object visitSymbolAction(Nez.SymbolAction e, Object a) {
			return this.analyzeInners(e);
		}

		@Override
		public final Object visitSymbolMatch(Nez.SymbolMatch e, Object a) {
			return Dependent;
		}

		@Override
		public final Object visitSymbolPredicate(Nez.SymbolPredicate e, Object a) {
			return Dependent;
		}

		@Override
		public final Object visitSymbolExists(Nez.SymbolExists e, Object a) {
			return Dependent;
		}

		@Override
		public final Object visitIf(Nez.IfCondition e, Object a) {
			return Independent;
		}

		@Override
		public final Object visitOn(Nez.OnCondition e, Object a) {
			return this.analyzeInners(e);
		}

	}
}
