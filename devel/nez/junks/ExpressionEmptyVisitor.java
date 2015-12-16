package nez.junks;

import nez.ast.TreeVisitorMap;
import nez.junks.ExpressionEmptyVisitor.DefaultVisitor;
import nez.lang.Expression;

public class ExpressionEmptyVisitor extends TreeVisitorMap<DefaultVisitor> {

	public ExpressionEmptyVisitor() {
		init(ExpressionEmptyVisitor.class, new DefaultVisitor());
	}

	public void visit(Expression e) {
		find(e.getClass().getSimpleName()).accept(e);
	}

	/*
	 * if you want different signature, modifiy acceptors as follows public
	 * boolean visit(Expression e, String a) { return
	 * find(e.getClass().getSimpleName()).accept(e, a);
	 * 
	 * }
	 */

	public class DefaultVisitor {
		public void accept(Expression e) {
			/* undefined node */
		}
		/**
		 * public boolean accept(Expression e, String a) { return false; }
		 **/
	}

	public class NonTerminal extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Pempty extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Pfail extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Cbyte extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Cany extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Cset extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Cmulti extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Pchoice extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Psequence extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Poption extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Pzero extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Pone extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Pand extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Pnot extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Tnew extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Tlink extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Tlfold extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Ttag extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Treplace extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Tcapture extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Tdetree extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xblock extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xlocal extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xif extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xon extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xsymbol extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xexisits extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xmatch extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

	public class Xis extends DefaultVisitor {
		@Override
		public void accept(Expression e) {
		}
	}

}
