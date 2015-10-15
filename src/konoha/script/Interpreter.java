package konoha.script;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import konoha.Array;
import konoha.IArray;
import konoha.asm.ScriptCompiler;
import nez.ast.TreeVisitor2;

public class Interpreter extends TreeVisitor2<SyntaxTreeInterpreter> implements CommonSymbols {
	ScriptContext context;
	TypeSystem typeSystem;
	private ScriptCompiler compiler;

	public Interpreter(ScriptContext sc, TypeSystem base) {
		super();
		init(new Undefined());
		this.context = sc;
		this.typeSystem = base;
		this.compiler = new ScriptCompiler(this.typeSystem);
	}

	static EmptyResult empty = new EmptyResult();

	public Object visit(TypedTree node) {
		switch (node.hint) {
		case StaticApplyInterface:
			return evalApplyHint(node);
		case Constant:
			return node.getValue();
		case ConstructorInterface:
			return evalConstructorHint(node);
		case MethodApply2:
			return evalMethodApplyHint(node);
		case StaticUnaryInterface:
		case StaticBinaryInterface:
		case StaticInvocation2:
			return evalStaticInvocationHint(node);
		case GetField:
			return evalFieldHint(node);
		case SetField:
			return evalSetFieldHint(node);
		case Unique:
		default:
			break;
		}
		return find(node).accept(node);
	}

	public Object nullEval(TypedTree node) {
		return (node == null) ? null : visit(node);
	}

	public class Undefined implements SyntaxTreeInterpreter {
		@Override
		public Object accept(TypedTree node) {
			throw new ScriptRuntimeException("TODO: Interpreter " + node);
		}
	}

	public class Source extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			boolean foundError = false;
			Object result = empty;
			for (TypedTree sub : node) {
				if (sub.is(_Error)) {
					context.log(sub.getText(_msg, ""));
					foundError = true;
				}
				if (!foundError) {
					result = visit(sub);
				}
			}
			return foundError ? empty : result;
		}
	}

	/* TopLevel */

	public class FuncDecl extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			compiler.compileFuncDecl(node);
			return empty;
		}
	}

	public class VarDecl extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			if (node.has(_expr)) {
				evalSetFieldHint(node);
			}
			return empty;
		}
	}

	public class MultiFuncDecl extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			for (TypedTree sub : node.get(_list)) {
				if (sub.has(_expr)) {
					evalSetFieldHint(sub);
				}
			}
			return empty;
		}
	}

	/* boolean */

	public class Block extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object retVal = null;
			for (TypedTree child : node) {
				retVal = visit(child);
			}
			return retVal;
		}
	}

	public class If extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			boolean cond = (Boolean) visit(node.get(_cond));
			if (cond) {
				return visit(node.get(_then));
			} else {
				TypedTree elseNode = node.get(_else, null);
				if (elseNode != null) {
					return visit(elseNode);
				}
			}
			return empty;
		}
	}

	/* Expression Statement */
	public class Expression extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			if (node.getType() == void.class) {
				visit(node.get(0));
				return empty;
			}
			return visit(node.get(0));
		}
	}

	public class Empty extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			return empty;
		}
	}

	/* Expression */

	public class Cast extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_expr));
			Functor inf = node.getInterface();
			if (inf != null) {
				return inf.eval(null, v);
			}
			return v;
		}
	}

	public class Conditional extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Boolean v = (Boolean) visit(node.get(_cond));
			if (v) {
				return visit(node.get(_then));
			} else {
				return visit(node.get(_else));
			}

		}
	}

	public class InstanceOf extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_left));
			if (v == null) {
				return false;
			}
			return ((Class<?>) node.getValue()).isAssignableFrom(v.getClass());
		}
	}

	public class Inc extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_expr));
			visit(node.get(_body));
			return v;
		}
	}

	public class Dec extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object v = visit(node.get(_expr));
			visit(node.get(_body));
			return v;
		}
	}

	public Object evalFieldHint(TypedTree node) {
		Field f = node.getField();
		Object recv = null;
		if (!Modifier.isStatic(f.getModifiers())) {
			recv = visit(node.get(_recv));
		}
		// System.out.println("eval field:" + recv + " . " + f);
		return Reflector.getField(recv, f);
	}

	private Object evalSetFieldHint(TypedTree node) {
		Field f = node.getField();
		Object recv = null;
		if (!Modifier.isStatic(f.getModifiers())) {
			recv = nullEval(node.get(_recv, null));
		}
		Object value = visit(node.get(_expr));
		Reflector.setField(recv, f, value);
		return value;
	}

	private Object evalStaticInvocationHint(TypedTree node) {
		Object[] args = this.evalApplyArgument(node);
		Functor inf = node.getInterface();
		return inf.eval(null, args);
	}

	public Object evalConstructorHint(TypedTree node) {
		Object[] args = evalApplyArgument(node.get(_param));
		Functor inf = node.getInterface();
		return inf.eval(null, args);
	}

	public Object evalMethodApplyHint(TypedTree node) {
		Object recv = visit(node.get(_recv));
		Object[] args = evalApplyArgument(node.get(_param));
		Functor inf = node.getInterface();
		return inf.eval(recv, args);
	}

	public Object evalApplyHint(TypedTree node) {
		Object[] args = evalApplyArgument(node.get(_param));
		Functor inf = node.getInterface();
		return inf.eval(null, args);
	}

	private Object[] evalApplyArgument(TypedTree node) {
		Object[] args = new Object[node.size()];
		for (int i = 0; i < node.size(); i++) {
			args[i] = visit(node.get(i));
		}
		return args;
	}

	public class And extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Boolean b = (Boolean) visit(node.get(_left));
			if (b) {
				return visit(node.get(_right));
			}
			return false;
		}
	}

	public class Or extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Boolean b = (Boolean) visit(node.get(_left));
			if (!b) {
				return visit(node.get(_right));
			}
			return true;
		}
	}

	public class Interpolation extends Undefined {
		@Override
		public Object accept(TypedTree node) {
			Object[] args = evalApplyArgument(node);
			Functor inf = node.getInterface();
			return inf.eval(null, new Object[] { args });
		}
	}

	public class _Array extends Undefined {
		@Override
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public Object accept(TypedTree node) {
			Object[] args = evalApplyArgument(node);
			Class<?> atype = node.getClassType();
			if (atype == IArray.class) {
				return new IArray<Object>(args, args.length);
				// System.out.println("AAA " + a + " " + a.getClass());
			}
			return new Array(args, args.length);
		}
	}
}