package konoha;

import java.lang.reflect.Method;

import konoha.script.Reflector;

public abstract class Function {
	public final Method f;

	protected Function() {
		f = Reflector.findInvokeMethod(this);
	}
}
