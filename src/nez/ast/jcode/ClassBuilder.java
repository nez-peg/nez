package nez.ast.jcode;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.ArrayDeque;
import java.util.Deque;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;

/**
 * wrapper class of ClassWriter
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class ClassBuilder extends ClassWriter implements Opcodes {
	private final String internalName;

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
	public ClassBuilder(int accessFlag, String fullyQualifiedClassName, String sourceName, Class<?> superClass,
			Class<?>[] interfaces) {
		super(ClassWriter.COMPUTE_FRAMES);
		this.internalName = fullyQualifiedClassName;
		String[] interfaceNames = null;

		if(superClass == null) {
			superClass = Object.class;
		}
		if(interfaces != null) {
			final int size = interfaces.length;
			interfaceNames = new String[size];
			for(int i = 0; i < size; i++) {
				interfaceNames[i] = Type.getInternalName(interfaces[i]);
			}
		}
		this.visit(V1_7, accessFlag, this.internalName, null, Type.getInternalName(superClass), interfaceNames);
		this.visitSource(sourceName, null);
	}

	/**
	 * get fully qualified class name. for
	 * UserDefinedClassLoader#definedAndLoadClass()
	 * 
	 * @return
	 */
	public String getInternalName() {
		return this.internalName;
	}

	/**
	 * get type descriptor of generating class
	 * 
	 * @return
	 */
	public Type getTypeDesc() {
		return Type.getType("L" + this.internalName + ";");
	}

	@Override
	public String toString() {
		return this.getInternalName();
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
	public MethodBuilder newMethodBuilder(int accessFlag, Class<?> returnClass, String methodName,
			Class<?>... paramClasses) {
		Method method = Methods.method(returnClass, methodName, paramClasses);
		return new MethodBuilder(accessFlag, method, this);
	}

	/**
	 * wrapper class of generator adapter
	 * 
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class MethodBuilder extends GeneratorAdapter {
		/**
		 * contains loop statement label(break, continue). left is break label
		 * and right is continue label.
		 */
		protected final Deque<Pair<Label, Label>> loopLabels;

		/**
		 * used for try catch statement.
		 */
		protected final Deque<TryCatchLabel> tryLabels;

		/**
		 * contains variable scope
		 */
		protected final VarScopes varScopes;

		/**
		 * represent current line number. used for stack trace.
		 */
		protected int currentLineNum = -1;

		protected MethodBuilder(int accessFlag, Method method, ClassVisitor cv) {
			super(Opcodes.ASM4, toMethodVisitor(accessFlag, method, cv), accessFlag, method.getName(),
					method.getDescriptor());
			this.loopLabels = new ArrayDeque<>();
			this.tryLabels = new ArrayDeque<>();
			int startIndex = 0;
			if((accessFlag & ACC_STATIC) != ACC_STATIC) {
				startIndex = 1;
			}
			this.varScopes = new VarScopes(startIndex);
		}

		/**
		 * helper method for method visitor generation
		 * 
		 * @param access
		 * @param method
		 * @param cv
		 * @return
		 */
		private static MethodVisitor toMethodVisitor(int access, Method method, ClassVisitor cv) {
			MethodVisitor visitor = cv.visitMethod(access, method.getName(), method.getDescriptor(), null, null);
			JSRInlinerAdapter inlinerAdapter = new JSRInlinerAdapter(visitor, access, method.getName(),
					method.getDescriptor(), null, null);
			return inlinerAdapter;
		}

		/**
		 * get loop labels
		 * 
		 * @return - stack of label pair. pair's left value is break label,
		 *         right value is continue label.
		 */
		public Deque<Pair<Label, Label>> getLoopLabels() {
			return this.loopLabels;
		}

		public Deque<TryCatchLabel> getTryLabels() {
			return this.tryLabels;
		}

		public TryCatchLabel createNewTryLabel(boolean existFinally) {
			Label startLabel = this.newLabel();
			Label endLabel = this.newLabel();
			Label finallyLabel = null;
			if(existFinally) {
				finallyLabel = this.newLabel();
			}
			return new TryCatchLabel(startLabel, endLabel, finallyLabel);
		}

		/**
		 * create new ret adrr entry and store return address
		 */
		public void storeReturnAddr() {
			VarEntry entry = this.varScopes.peek().newRetAddressEntry();
			this.visitVarInsn(ASTORE, entry.getVarIndex());
			this.tryLabels.peek().retAddrEntry = entry;
		}

		public void returnFromFinally() {
			VarEntry entry = this.tryLabels.peek().retAddrEntry;
			this.visitVarInsn(RET, entry.getVarIndex());
		}

		public void jumpToFinally() {
			this.jumpToFinally(this.tryLabels.peek());
		}

		private void jumpToFinally(TryCatchLabel label) {
			Label finallyLabel = label.getFinallyLabel();
			if(finallyLabel != null) {
				this.visitJumpInsn(JSR, finallyLabel);
			}
		}

		public void jumpToMultipleFinally() {
			for(TryCatchLabel label : this.tryLabels) {
				this.jumpToFinally(label);
			}
		}

		/**
		 * generate pop instruction. if type is long or double, generate pop2.
		 * 
		 * @param type
		 *            - stack top type. if type is void, not generate pop ins
		 */
		public void pop(Type type) {
			if(type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE)) {
				this.pop2();
			} else if(!type.equals(Type.VOID_TYPE)) {
				this.pop();
			}
		}

		public void pop(Class<?> clazz) {
			this.pop(Type.getType(clazz));
		}

		/**
		 * enter block scope
		 */
		public void enterScope() {
			this.varScopes.createNewScope();
		}

		/**
		 * exit block scope
		 */
		public void exitScope() {
			this.varScopes.removeCurrentScope();
		}

		/**
		 * reserve local variable entry of argument.
		 * 
		 * @param argClass
		 * @return
		 */
		public VarEntry defineArgument(Class<?> argClass) {
			assert this.varScopes.size() == 1;
			return this.varScopes.peek().newVarEntry(argClass);
		}

		/**
		 * create new local variable
		 * 
		 * @param varClass
		 * @return
		 */
		public VarEntry createNewVar(Class<?> varClass) {
			return this.varScopes.peek().newVarEntry(varClass);
		}

		/**
		 * create new local variable entry and store stack top value to created
		 * entry
		 * 
		 * @param varClass
		 * @return
		 */
		public VarEntry createNewVarAndStore(Class<?> varClass) {
			VarEntry entry = this.varScopes.peek().newVarEntry(varClass);
			Type typeDesc = Type.getType(varClass);
			this.visitVarInsn(typeDesc.getOpcode(ISTORE), entry.getVarIndex());
			return entry;
		}

		/**
		 * store stack top value to local variable.
		 * 
		 * @param entry
		 */
		public void storeToVar(VarEntry entry) {
			Type typeDesc = Type.getType(entry.getVarClass());
			this.visitVarInsn(typeDesc.getOpcode(ISTORE), entry.getVarIndex());
		}

		/**
		 * load value from local variable and put it at stack top.
		 * 
		 * @param entry
		 */
		public void loadFromVar(VarEntry entry) {
			Type typeDesc = Type.getType(entry.getVarClass());
			this.visitVarInsn(typeDesc.getOpcode(ILOAD), entry.getVarIndex());
		}

		/**
		 * generate line number.
		 * 
		 * @param lineNum
		 */
		public void setLineNum(int lineNum) {
			if(lineNum > this.currentLineNum) {
				this.visitLineNumber(lineNum, this.mark());
			}
		}

		/**
		 * push null value to stack top
		 */
		public void pushNull() {
			this.visitInsn(ACONST_NULL);
		}

		/**
		 * generate invokevirtual instruction.
		 * 
		 * @param ownerClass
		 *            owner class of method.
		 * @param returnClass
		 *            return class
		 * @param methodName
		 * @param paramClasses
		 *            parameter classes (not contains receiver class)
		 */
		public void callInstanceMethod(Class<?> ownerClass, Class<?> returnClass, String methodName,
				Class<?>... paramClasses) {
			Method methodDesc = Methods.method(returnClass, methodName, paramClasses);
			this.invokeVirtual(Type.getType(ownerClass), methodDesc);
		}

		/**
		 * generate invokestatic instruction.
		 * 
		 * @param ownerClass
		 *            owner class of method.
		 * @param returnClass
		 * @param methodName
		 * @param paramClasses
		 */
		public void callStaticMethod(Class<?> ownerClass, Class<?> returnClass, String methodName,
				Class<?>... paramClasses) {
			Method methodDesc = Methods.method(returnClass, methodName, paramClasses);
			this.invokeStatic(Type.getType(ownerClass), methodDesc);
		}

		public void callInterfaceMethod(Class<?> ownerClass, Class<?> returnClass, String methodName,
				Class<?>... paramClasses) {
			Method methodDesc = Methods.method(returnClass, methodName, paramClasses);
			this.invokeInterface(Type.getType(ownerClass), methodDesc);
		}

		/**
		 * only support invokestatic, invokevirtual
		 * 
		 * @param target
		 */
		public void callInvocationTarget(InvocationTarget target) {
			switch(target.getInvocationType()) {
			case INVOKE_STATIC:
				this.invokeStatic(target.getOwnerTypeDesc(), target.getMethodDesc());
				break;
			case INVOKE_VIRTUAL:
				this.invokeVirtual(target.getOwnerTypeDesc(), target.getMethodDesc());
				break;
			case INVOKE_INTERFACE:
				this.invokeInterface(target.getOwnerTypeDesc(), target.getMethodDesc());
			default:
				break;
			}
		}

		/**
		 * generate invokedynamic instruction.
		 *
		 * @param bsmClassPath
		 *            class path that has bootstrap method.
		 * @param bsmName
		 *            bootstrap method name
		 * @param methodName
		 *            invocation method name
		 * @param invokeClassPath
		 *            invocation class path
		 * @param argTypes
		 *            types of arguments
		 */
		public void callDynamicMethod(String bsmClassPath, String bsmName, String methodName, String invokeClassPath,
				Type... argTypes) {
			MethodType mt = MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class,
					MethodType.class, String.class);
			Handle bsm = new Handle(Opcodes.H_INVOKESTATIC, bsmClassPath, bsmName, mt.toMethodDescriptorString());
			this.invokeDynamic(methodName, Type.getMethodDescriptor(Type.getType(Object.class), argTypes), bsm,
					invokeClassPath);
		}

		public void getStatic(StaticField field) {
			this.getStatic(field.getOwnerType(), field.getFieldName(), field.getFieldType());
		}
	}

	public static class TryCatchLabel {
		private final Label startLabel;
		private final Label endLabel;
		private final Label finallyLabel;

		private VarEntry retAddrEntry;

		/**
		 * 
		 * @param startLabel
		 * @param endLabel
		 * @param finallyLabel
		 *            if not found finally, null
		 */
		private TryCatchLabel(Label startLabel, Label endLabel, Label finallyLabel) {
			this.startLabel = startLabel;
			this.endLabel = endLabel;
			this.finallyLabel = finallyLabel;
		}

		public Label getStartLabel() {
			return this.startLabel;
		}

		public Label getEndLabel() {
			return this.endLabel;
		}

		/**
		 * 
		 * @return may be null
		 */
		public Label getFinallyLabel() {
			return this.finallyLabel;
		}
	}

	private static class VarScopes extends ArrayDeque<LocalVarScope> {
		private static final long serialVersionUID = 8905256606042979610L;

		/**
		 * local variable start index. if this builder represents static method
		 * or static initializer, index = 0. if this builder represents instance
		 * method or constructor, index = 1;
		 */
		protected final int startVarIndex;

		private VarScopes(int startIndex) {
			super();
			this.startVarIndex = startIndex;
		}

		public void createNewScope() {
			int startIndex = this.startVarIndex;
			if(!this.isEmpty()) {
				startIndex = this.peek().getEndIndex();
			}
			this.push(new LocalVarScope(startIndex));
		}

		public void removeCurrentScope() {
			this.pop();
		}
	}

	private static class LocalVarScope {
		/**
		 * represent start index of local variable in this scope.
		 */
		private final int localVarBaseIndex;

		/**
		 * represent local variable index. after adding new local variable,
		 * increment this index by value size.
		 */
		private int currentLocalVarIndex;

		protected LocalVarScope(int localVarBaseIndex) {
			this.localVarBaseIndex = localVarBaseIndex;
			this.currentLocalVarIndex = this.localVarBaseIndex;
		}

		public VarEntry newVarEntry(Class<?> clazz) {
			int valueSize = Type.getType(clazz).getSize();
			return this.newVarEntry(valueSize, clazz);
		}

		public VarEntry newRetAddressEntry() {
			return this.newVarEntry(1, null);
		}

		/**
		 * 
		 * @param valueSize
		 *            size of variable, long, double is 2, otherwise 1
		 * @param varClass
		 * @return
		 */
		private VarEntry newVarEntry(int valueSize, Class<?> varClass) {
			assert valueSize > 0;
			int index = this.currentLocalVarIndex;
			VarEntry entry = new VarEntry(index, varClass);
			this.currentLocalVarIndex += valueSize;
			return entry;
		}

		public int getEndIndex() {
			return this.currentLocalVarIndex;
		}
	}

	/**
	 * contains var index
	 * 
	 * @author skgchxngsxyz-osx
	 *
	 */
	public static class VarEntry {
		/**
		 * represents jvm local variable table's index.
		 */
		private final int varIndex;

		private final Class<?> varClass;

		/**
		 * 
		 * @param varIndex
		 * @param varClass
		 *            null, if this entry represents return address entry.
		 */
		private VarEntry(int varIndex, Class<?> varClass) {
			this.varIndex = varIndex;
			this.varClass = varClass;
		}

		private int getVarIndex() {
			return this.varIndex;
		}

		/**
		 * get class of variable
		 * 
		 * @return return null if this entry represents return address
		 */
		public Class<?> getVarClass() {
			return this.varClass;
		}
	}
}
