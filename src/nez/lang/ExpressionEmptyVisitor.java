package nez.lang;

import nez.lang.ExpressionEmptyVisitor.DefaultVisitor;
import nez.util.VisitorMap;

public class ExpressionEmptyVisitor extends VisitorMap<DefaultVisitor> {

	public void visit(Expression e) {
		find(e.getClass().getSimpleName()).accept(e);
	}

	public class DefaultVisitor {
		void accept(Expression e) {
			/* undefined node */
		}
	}

	public class Pempty extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Pfail extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Cbyte extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Cany extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Cset extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Cmulti extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Pchoice extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Psequence extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Poption extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Pzero extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Pone extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Pand extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Pnot extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Tnew extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Tlink extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Tlfold extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Ttag extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Treplace extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Tcapture extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Tdetree extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xblock extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xlocal extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xif extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xon extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xsymbol extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xexisits extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xmatch extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

	public class Xis extends DefaultVisitor {
		@Override
		void accept(Expression e) {
		}
	}

}
