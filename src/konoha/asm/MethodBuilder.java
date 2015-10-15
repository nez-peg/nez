package konoha.asm;

import java.util.ArrayDeque;
import java.util.Deque;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import org.objectweb.asm.commons.Method;

/**
 * wrapper class of generator adapter
 * 
 * @author skgchxngsxyz-osx
 *
 */
public class MethodBuilder extends GeneratorAdapter {
	/**
	 * contains loop statement label(break, continue). left is break label and
	 * right is continue label.
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

	Method method;

	public MethodBuilder(int accessFlag, Method method, ClassVisitor cv) {
		super(Opcodes.ASM4, toMethodVisitor(accessFlag, method, cv), accessFlag, method.getName(), method.getDescriptor());
		this.loopLabels = new ArrayDeque<>();
		this.tryLabels = new ArrayDeque<>();
		int startIndex = 0;
		if ((accessFlag & Opcodes.ACC_STATIC) != Opcodes.ACC_STATIC) {
			startIndex = 1;
		}
		this.varScopes = new VarScopes(startIndex);
		this.method = method;
	}

	Method getMethod() {
		return this.method;
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
		JSRInlinerAdapter inlinerAdapter = new JSRInlinerAdapter(visitor, access, method.getName(), method.getDescriptor(), null, null);
		return inlinerAdapter;
	}

	/**
	 * get loop labels
	 * 
	 * @return - stack of label pair. pair's left value is break label, right
	 *         value is continue label.
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
		if (existFinally) {
			finallyLabel = this.newLabel();
		}
		return new TryCatchLabel(startLabel, endLabel, finallyLabel);
	}

	/**
	 * create new ret adrr entry and store return address
	 */

	public void storeReturnAddr() {
		VarEntry entry = this.varScopes.peek().newRetAddressEntry();
		this.visitVarInsn(ClassBuilder.ASTORE, entry.getVarIndex());
		this.tryLabels.peek().retAddrEntry = entry;
	}

	public void returnFromFinally() {
		VarEntry entry = this.tryLabels.peek().retAddrEntry;
		this.visitVarInsn(ClassBuilder.RET, entry.getVarIndex());
	}

	public void jumpToFinally() {
		this.jumpToFinally(this.tryLabels.peek());
	}

	private void jumpToFinally(TryCatchLabel label) {
		Label finallyLabel = label.getFinallyLabel();
		if (finallyLabel != null) {
			this.visitJumpInsn(ClassBuilder.JSR, finallyLabel);
		}
	}

	public void jumpToMultipleFinally() {
		for (TryCatchLabel label : this.tryLabels) {
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
		if (type.equals(Type.LONG_TYPE) || type.equals(Type.DOUBLE_TYPE)) {
			this.pop2();
		} else if (!type.equals(Type.VOID_TYPE)) {
			this.pop();
		}
	}

	public final void pop(Class<?> clazz) {
		if (clazz == long.class || clazz == double.class) {
			pop2();
		} else if (clazz != void.class) {
			pop();
		}
	}

	public final void dup(Class<?> clazz) {
		if (clazz == long.class || clazz == double.class) {
			dup2();
		} else if (clazz != void.class) {
			dup();
		}
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
	public VarEntry defineArgument(String argName, Class<?> argClass) {
		assert this.varScopes.size() == 1;
		return this.varScopes.peek().newVarEntry(argName, argClass);
	}

	/**
	 * create new local variable
	 * 
	 * @param varClass
	 * @return
	 */
	public VarEntry createNewVar(String varName, Class<?> varClass) {
		return this.varScopes.peek().newVarEntry(varName, varClass);
	}

	/**
	 * create new local variable entry and store stack top value to created
	 * entry
	 * 
	 * @param name
	 * 
	 * @param varClass
	 * @return
	 */

	public VarEntry createNewVarAndStore(String varName, Class<?> varClass) {
		VarEntry entry = this.varScopes.peek().newVarEntry(varName, varClass);
		Type typeDesc = Type.getType(varClass);
		this.visitVarInsn(typeDesc.getOpcode(ClassBuilder.ISTORE), entry.getVarIndex());
		return entry;
	}

	/**
	 * store stack top value to local variable.
	 * 
	 * @param entry
	 */
	public void storeToVar(VarEntry entry) {
		Type typeDesc = Type.getType(entry.getVarClass());
		this.visitVarInsn(typeDesc.getOpcode(ClassBuilder.ISTORE), entry.getVarIndex());
	}

	/**
	 * load value from local variable and put it at stack top.
	 * 
	 * @param entry
	 */
	public void loadFromVar(VarEntry entry) {
		Type typeDesc = Type.getType(entry.getVarClass());
		this.visitVarInsn(typeDesc.getOpcode(ClassBuilder.ILOAD), entry.getVarIndex());
	}

	public VarEntry getVar(String varName) {
		return this.varScopes.getFirst().getLocalVar(varName);
	}

	public void callIinc(VarEntry entry, int amount) {
		this.iinc(entry.getVarIndex(), amount);
	}

	/**
	 * generate line number.
	 * 
	 * @param lineNum
	 */
	public void setLineNum(int lineNum) {
		if (lineNum > this.currentLineNum) {
			this.visitLineNumber(lineNum, this.mark());
		}
	}

	/**
	 * push null value to stack top
	 */
	public void pushNull() {
		this.visitInsn(ClassBuilder.ACONST_NULL);
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
	public void callInstanceMethod(Class<?> ownerClass, Class<?> returnClass, String methodName, Class<?>... paramClasses) {
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
	public void callStaticMethod(Class<?> ownerClass, Class<?> returnClass, String methodName, Class<?>... paramClasses) {
		Method methodDesc = Methods.method(returnClass, methodName, paramClasses);
		this.invokeStatic(Type.getType(ownerClass), methodDesc);
	}

	public void callInterfaceMethod(Class<?> ownerClass, Class<?> returnClass, String methodName, Class<?>... paramClasses) {
		Method methodDesc = Methods.method(returnClass, methodName, paramClasses);
		this.invokeInterface(Type.getType(ownerClass), methodDesc);
	}

	// /**
	// * only support invokestatic, invokevirtual
	// *
	// * @param target
	// */
	// public void callInvocationTarget(InvocationTarget target) {
	// switch (target.getInvocationType()) {
	// case INVOKE_STATIC:
	// this.invokeStatic(target.getOwnerTypeDesc(), target.getMethodDesc());
	// break;
	// case INVOKE_VIRTUAL:
	// this.invokeVirtual(target.getOwnerTypeDesc(), target.getMethodDesc());
	// break;
	// case INVOKE_INTERFACE:
	// this.invokeInterface(target.getOwnerTypeDesc(), target.getMethodDesc());
	// default:
	// break;
	// }
	// }
	//
	// public void callDynamicMethod(String bsmClassPath, String bsmName, String
	// methodName, String invokeClassPath, Type... argTypes) {
	// MethodType mt = MethodType.methodType(CallSite.class,
	// MethodHandles.Lookup.class, String.class, MethodType.class,
	// String.class);
	// Handle bsm = new Handle(Opcodes.H_INVOKESTATIC, bsmClassPath, bsmName,
	// mt.toMethodDescriptorString());
	// this.invokeDynamic(methodName,
	// Type.getMethodDescriptor(Type.getType(Object.class), argTypes), bsm,
	// invokeClassPath);
	// }
	//
	// public void getStatic(StaticField field) {
	// this.getStatic(field.getOwnerType(), field.getFieldName(),
	// field.getFieldType());
	// }

}