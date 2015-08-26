package nez.ast.jcode;

import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class InvocationTarget {
	public static enum InvocationType {
		INVOKE_STATIC,
		INVOKE_VIRTUAL,
		INVOKE_INTERFACE;
	}

	private final InvocationType type;

	private final Class<?> ownerClass;

	private final Class<?> returnClass;

	private final String methodName;

	private final Class<?>[] paramClasses;

	private final Type ownerTypeDesc;

	private final Method methodDesc;

	public InvocationTarget(InvocationType type, Class<?> ownerClass, Class<?> returnClass, String methodName,
			Class<?>... paramClasses) {
		this.type = type;
		this.ownerClass = ownerClass;
		this.returnClass = returnClass;
		this.methodName = methodName;
		this.paramClasses = paramClasses;

		// init descriptor
		this.ownerTypeDesc = Type.getType(this.ownerClass);
		this.methodDesc = Methods.method(this.returnClass, this.methodName, this.paramClasses);
	}

	public InvocationType getInvocationType() {
		return this.type;
	}

	public Type getOwnerTypeDesc() {
		return this.ownerTypeDesc;
	}

	public Method getMethodDesc() {
		return this.methodDesc;
	}

	@Override
	public String toString() {
		StringBuilder sBuilder = new StringBuilder();
		switch(this.type) {
		case INVOKE_STATIC:
			sBuilder.append("<static> ");
			break;
		case INVOKE_VIRTUAL:
			sBuilder.append("<virtual> ");
			break;
		case INVOKE_INTERFACE:
			sBuilder.append("<interface>");
		default:
			break;
		}
		sBuilder.append(this.ownerClass.getSimpleName());
		sBuilder.append("# ");
		sBuilder.append(this.returnClass.getSimpleName());
		sBuilder.append(' ');
		sBuilder.append(this.methodName);
		sBuilder.append('(');

		final int size = this.paramClasses.length;
		for(int i = 0; i < size; i++) {
			if(i > 0) {
				sBuilder.append(", ");
			}
			sBuilder.append(this.paramClasses[i].getSimpleName());
		}
		sBuilder.append(')');
		return sBuilder.toString();
	}

	public static InvocationTarget newStaticTarget(Class<?> ownerClass, Class<?> returnClass, String methodName,
			Class<?>... paramClasses) {
		return new InvocationTarget(InvocationType.INVOKE_STATIC, ownerClass, returnClass, methodName, paramClasses);
	}

	public static InvocationTarget newVirtualTarget(Class<?> ownerClass, Class<?> returnClass, String methodName,
			Class<?>... paramClasses) {
		return new InvocationTarget(InvocationType.INVOKE_VIRTUAL, ownerClass, returnClass, methodName, paramClasses);
	}

	public static InvocationTarget newInterfaceTarget(Class<?> ownerClass, Class<?> returnClass, String methodName,
			Class<?>... paramClasses) {
		return new InvocationTarget(InvocationType.INVOKE_INTERFACE, ownerClass, returnClass, methodName, paramClasses);
	}
}
