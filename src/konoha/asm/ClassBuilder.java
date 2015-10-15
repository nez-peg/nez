package konoha.asm;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.FieldNode;

/**
 * wrapper class of ClassWriter
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class ClassBuilder extends ClassWriter implements Opcodes {
	private final String qualifiedClassName;

	/**
	 * generate new class builder
	 * 
	 * @param accessFlag
	 *            represent for java access flag (public, private, static ... )
	 * @param fullyQualifiedClassName
	 *            ex. org/peg4d/generated/Parser
	 * @param sourceName
	 *            source file name, may be null
	 * @param superClass
	 *            if null, super class is java/lang/Object
	 * @param interfaces
	 *            may be null, if has no interface
	 */

	public ClassBuilder(int accessFlag, String fullyQualifiedClassName, String sourceName, Class<?> superClass, Class<?>[] interfaces) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.qualifiedClassName = fullyQualifiedClassName;
		String[] interfaceNames = null;

		if (superClass == null) {
			superClass = Object.class;
		}
		if (interfaces != null) {
			final int size = interfaces.length;
			interfaceNames = new String[size];
			for (int i = 0; i < size; i++) {
				interfaceNames[i] = Type.getInternalName(interfaces[i]);
			}
		}
		this.visit(V1_7, accessFlag, this.qualifiedClassName, null, Type.getInternalName(superClass), interfaceNames);
		this.visitSource(sourceName, null);
	}

	/**
	 * equivalent to ClassBuilder(ACC_PUBLIC | ACC_FINAL,
	 * fullyQualifiedClassName, sourceName, superClass, interfaces)
	 * 
	 * @param fullyQualifiedClassName
	 * @param sourceName
	 * @param superClass
	 * @param interfaces
	 */

	public ClassBuilder(String fullyQualifiedClassName, String sourceName, Class<?> superClass, Class<?>[] interfaces) {
		this(ACC_PUBLIC | ACC_FINAL, fullyQualifiedClassName, sourceName, superClass, interfaces);
	}

	/**
	 * get fully qualified class name. for
	 * UserDefinedClassLoader#definedAndLoadClass()
	 * 
	 * @return
	 */
	public String getQualifiedClassName() {
		return this.qualifiedClassName;
	}

	/**
	 * get type descriptor of generating class
	 * 
	 * @return
	 */

	public Type getTypeDesc() {
		return Type.getType("L" + this.qualifiedClassName + ";");
	}

	public void addField(int acc, String name, Class<?> fieldClass, Object value) {
		FieldNode fn = new FieldNode(acc, name, Type.getDescriptor(fieldClass), null, value);
		fn.accept(this);
	}

	/**
	 * 
	 * @param accessFlag
	 *            represent for java access flag
	 * @param returnClass
	 * @param methodName
	 * @param paramClasses
	 * @return
	 */

	public MethodBuilder newMethodBuilder(int acc, Method method) {
		return new MethodBuilder(acc, method, this);
	}

	public MethodBuilder newMethodBuilder(int accessFlag, Class<?> returnClass, String methodName, Class<?>... paramClasses) {
		final int size = paramClasses.length;
		Type[] paramTypeDescs = new Type[paramClasses.length];
		for (int i = 0; i < size; i++) {
			paramTypeDescs[i] = Type.getType(paramClasses[i]);
		}
		Method method = new Method(methodName, Type.getType(returnClass), paramTypeDescs);
		return new MethodBuilder(accessFlag, method, this);
	}

	public MethodBuilder newConstructorBuilder(int accessFlag, Class<?>... paramClasses) {
		Method method = Methods.constructor(paramClasses);
		return new MethodBuilder(accessFlag, method, this);
	}

	@Override
	public String toString() {
		return this.getQualifiedClassName();
	}

}
