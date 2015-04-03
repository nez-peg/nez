package nez.expr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public abstract class GrammarVisitor {
	HashMap<Class<?>, Method> methodMap = new HashMap<Class<?>, Method>();
	public final void visit(Expression e) {
		Class<?> c = e.getClass();
		Method m = lookupMethod(c);
		if(m != null) {
			try {
				m.invoke(this, e);
			} catch (IllegalAccessException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (IllegalArgumentException ex) {
				// TODO Auto-generated catch block
				ex.printStackTrace();
			} catch (InvocationTargetException ex) {
				ex.printStackTrace();
			}
		}
		else {
			visitExpression(e);
		}
	}
		
	private final Method lookupMethod(Class<?> c) {
		Method m = this.methodMap.get(c);
		if(m == null) {
			String name = "visit" + c.getSimpleName();
			try {
				m = this.getClass().getMethod(name, c);
			} catch (NoSuchMethodException e) {
				return null;
			} catch (SecurityException e) {
				return null;
			}
			this.methodMap.put(c, m);
		}
		return m;
	}

	protected abstract void visitExpression(Expression e);

}

