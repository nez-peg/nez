package nez.parser.vm;

import nez.lang.Expression;
import nez.parser.Instruction;
import nez.parser.vm.MozCompiler.DefaultVisitor;
import nez.util.VisitorMap;

public class MozCompiler extends VisitorMap<DefaultVisitor> {
	public MozCompiler() {
		this.init(MozCompiler.class, new DefaultVisitor());
	}

	public Instruction generate(Expression e, Instruction next) {
		return find(e.getClass().getSimpleName()).accept(e, next);
	}

	public class DefaultVisitor {
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
		/**
		 * public boolean accept(Expression e, String a) { return false; }
		 **/
	}

	public class NonTerminal extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pempty extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pfail extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Cbyte extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Cany extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Cset extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Cmulti extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pchoice extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Psequence extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Poption extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pzero extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pone extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pand extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Pnot extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Tnew extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Tlink extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Tlfold extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Ttag extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Treplace extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Tcapture extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Tdetree extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xblock extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xlocal extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xif extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xon extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xsymbol extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xexisits extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xmatch extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

	public class Xis extends DefaultVisitor {
		@Override
		public Instruction accept(Expression e, Instruction next) {
			return next;
		}
	}

}
