package nez.ast.jcode;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

/**
 * helper utilities for Method Descriptor generation
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class Methods {

	/**
	 * create Method
	 * 
	 * @param returnClass
	 *            not null
	 * @param methodName
	 *            not null.
	 * @param paramClasses
	 *            not null. if method has no argument, require empty array
	 * @return generated Method Descriptor.
	 */
	public static Method method(Class<?> returnClass, String methodName, Class<?>... paramClasses) {
		final int size = paramClasses.length;
		Type[] paramTypeDescs = new Type[paramClasses.length];
		for(int i = 0; i < size; i++) {
			paramTypeDescs[i] = Type.getType(paramClasses[i]);
		}
		return new Method(methodName, Type.getType(returnClass), paramTypeDescs);
	}

	public static Method constructor(Class<?>... paramClasses) {
		return method(void.class, "<init>", paramClasses);
	}
}
