package nez.konoha;

import java.util.HashMap;

import nez.ast.Tag;

public abstract class KonohaType {
	String name;
	KonohaType(String name) {
		this.name = name;
	}
	public String getName() {
		return this.name;
	}
	abstract boolean equalsType(KonohaType exprType);
	abstract boolean matchType(KonohaType exprType);
	abstract boolean isGreekType();
	boolean isPolymorphic() { return false; }
	public String toString() { return this.getName(); }
	
	public final static KonohaType VoidType = new KonohaPrimitiveType("void");
	public static KonohaType newErrorType(KonohaTree node, String msg) {
		return new KonohaErrorType(node, msg);
	}
}

class KonohaPrimitiveType extends KonohaType {
	KonohaPrimitiveType(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}
	@Override
	boolean isGreekType() {
		return false;
	}
	boolean equalsType(KonohaType exprType) {
		if(exprType instanceof KonohaVarType) {
			return ((KonohaVarType) exprType).equalsPrimitiveType(this, (KonohaVarType) exprType);
		}
		return this == exprType;
	}
	boolean matchType(KonohaType exprType) {
		if(exprType instanceof KonohaVarType) {
			return ((KonohaVarType) exprType).equalsPrimitiveType(this, (KonohaVarType) exprType);
		}
		return this == exprType;
	}
}

class KonohaStructType extends KonohaType {
	KonohaStructType(String name) {
		super(name);
	}
	@Override
	boolean isGreekType() {
		return false;
	}
	boolean equalsType(KonohaType exprType) {
		if(exprType instanceof KonohaVarType) {
			return ((KonohaVarType) exprType).equalsStructType(this, (KonohaVarType) exprType);
		}
		return this == exprType;
	}
	boolean matchType(KonohaType exprType) {
		if(exprType instanceof KonohaVarType) {
			return ((KonohaVarType) exprType).matchStructType(this, (KonohaVarType) exprType);
		}
		if(this == exprType) {
			return true;
		}
		if(exprType instanceof KonohaStructType) {
			return matchStructType((KonohaStructType)exprType);
		}
		return false;
	}
	private boolean matchStructType(KonohaStructType t) {
		// TODO;
		return false;
	}	
}

class KonohaArrayType extends KonohaType {
	public final static Tag ArrayTag = Tag.tag("Tarray");
	public final static KonohaType newArrayType(KonohaType t) {
		if(t == null) return t;
		return new KonohaArrayType(t);
	}
	//
	KonohaType paramType;
	KonohaArrayType(KonohaType paramType) {
		super("array");
		this.paramType = paramType;
	}
	@Override
	boolean isGreekType() {
		return this.paramType.isGreekType();
	}
	boolean equalsType(KonohaType exprType) {
		if(exprType instanceof KonohaVarType) {

		}
		if(exprType instanceof KonohaArrayType) {
			return this.paramType.equalsType(((KonohaArrayType) exprType).paramType);
		}
		return false;
	}
	boolean matchType(KonohaType exprType) {
		if(exprType instanceof KonohaVarType) {

		}
		if(exprType instanceof KonohaArrayType) {
			return this.paramType.equalsType(((KonohaArrayType) exprType).paramType);
		}
		return false;
	}
	public String toString() { return this.getName()+"[]"; }

}

class KonohaErrorType extends KonohaType {
	KonohaTree node;
	String msg;
	KonohaErrorType(KonohaTree node, String msg) {
		super("'" + msg + "'");
		this.node = node;
		this.msg = msg;
	}
	@Override
	boolean isGreekType() {
		return false;
	}
	boolean equalsType(KonohaType exprType) {
		return true;
	}
	boolean matchType(KonohaType exprType) {
		return true;
	}
}

class KonohaTypeEnv {
	public static boolean isGreek(KonohaType[] types) {
		for(KonohaType t: types) {
			if(t.isGreekType()) {
				return true;
			}
		}
		return false;
	}
	HashMap<String, KonohaType> varTypeMap;
	KonohaTypeEnv(KonohaType[] types) {
		varTypeMap = new HashMap<String, KonohaType>();
	}
	
	KonohaType[] newTypeVar(KonohaType[] types) {
		KonohaType[] newtypes = new KonohaType[types.length];
		for(int i = 0; i < types.length; i++) {
			if(types[i].isGreekType()) {
				newtypes[i] = newVarType(types[i]);
			}
			else {
				newtypes[i] = types[i];
			}
		}
		return newtypes;
	}

	private KonohaType newVarType(KonohaType t) {
		if(t instanceof KonohaGreekType) {
			KonohaType var = varTypeMap.get(t.getName());
			if(var == null) {
				var = new KonohaVarType(t.getName(), null);
				varTypeMap.put(t.getName(), var);
			}
			return var;
		}
		if(t instanceof KonohaArrayType) {
			KonohaType var = newVarType(((KonohaArrayType) t).paramType);
			String name = var.getName()+"[]";
			KonohaType avar = varTypeMap.get(name);
			if(avar == null) {
				avar = new KonohaArrayType(var);
				varTypeMap.put(name, avar);
			}
			return avar;
		}
		throw new RuntimeException("FIXME");
	}
}

class KonohaGreekType extends KonohaType {
	KonohaGreekType(char c) {
		super(String.valueOf(c));
	}
	@Override
	boolean isGreekType() {
		return true;
	}
	@Override
	boolean equalsType(KonohaType exprType) {
		return this == exprType;
	}
	@Override
	boolean matchType(KonohaType exprType) {
		return this == exprType;
	}
}

class KonohaVarType extends KonohaType {
	class TypeLog {
		int cmp;
		KonohaType t;
		TypeLog next;
		TypeLog(int cmp, KonohaType t, TypeLog next) {
			this.cmp = cmp;
			this.t = t;
			this.next = next;
		}
	}
	class TypeVarLog {
		boolean isEquiv;
		KonohaVarType var;
		TypeVarLog next;
		TypeVarLog(boolean isEquiv, KonohaVarType var, TypeVarLog next) {
			this.isEquiv = isEquiv;
			this.var = var;
			this.next = next;
		}
	}
	boolean typePoly = false;
	TypeLog typeLog = null;
	TypeVarLog varLog = null;
	
	KonohaVarType(String name, KonohaType t) {
		super(name);
		if(t != null) {
			this.typeLog = new TypeLog(0, t, this.typeLog);
		}
	}

	public final boolean isUnresolved() {
		return (this.typeLog == null);
	}

	public final boolean isResolved() {
		return (this.typeLog != null && this.typeLog.next == null);
	}

	public final boolean isError() {
		return (this.typeLog != null && this.typeLog.next != null);
	}

	public final KonohaType getResolvedType() {
		if(!this.isUnresolved()){
			return this.typeLog.t;
		}
		return null;
	}
	
	@Override
	boolean isGreekType() {
		return false;  // all GreekTypes are converted into VarTypes
	}
	
	@Override
	boolean equalsType(KonohaType exprType) {
		if(exprType == this) {
			return true;
		}
		if(exprType instanceof KonohaPrimitiveType) {
			return update((KonohaPrimitiveType)exprType);
		}
		if(exprType instanceof KonohaVarType) {
			return appendTypeVarLog(true, (KonohaVarType)exprType);
		}
		return false;
	}

	@Override
	boolean matchType(KonohaType exprType) {
		if(exprType == this) {
			return true;
		}
		if(exprType instanceof KonohaPrimitiveType) {
			return update((KonohaPrimitiveType)exprType);
		}
		if(exprType instanceof KonohaVarType) {
			return appendTypeVarLog(false, (KonohaVarType)exprType);
		}
		return false;
	}
	
	private boolean update(KonohaPrimitiveType t) {
		if(this.isUnresolved()) {
			this.typeLog = new TypeLog(0, t, null);
			return true;
		}
		if(this.isResolved() && this.getResolvedType().equalsType(t)) {
			return true;
		}
		appendTypeLog(0, t);
		return false;
	}

	private boolean update(KonohaStructType t) {
		if(this.isUnresolved()) {
			this.typeLog = new TypeLog(0, t, null);
			this.typePoly = true;
			return true;
		}
		if(this.isResolved() && this.getResolvedType().equalsType(t)) {
			return true;
		}
		appendTypeLog(0, t);
		return false;
	}

	private void appendTypeLog(int cmp, KonohaType t) {
		TypeLog l = this.typeLog;
		while(l != null) {
			if(l.t.equals(t)) {
				break;
			}
			l = l.next;
		}
		this.typeLog = new TypeLog(0, t, this.typeLog);
	}
	
	private boolean appendTypeVarLog(boolean isEquiv, KonohaVarType exprVarType) {
		TypeVarLog l = this.varLog;
		while(l != null) {
			if(l.var == exprVarType) {
				if(l.isEquiv == isEquiv || l.isEquiv) {
					return true;
				}
				if(isEquiv) {
					l.isEquiv = true;
					return this.syncVarType(l.var);
				}
				if(!isEquiv) { 
					if(l.var.isMatchVar(exprVarType)) {
						l.isEquiv = true;
						return this.syncVarType(l.var);
					}
				}
			}
			l = l.next;
		}
		this.varLog = new TypeVarLog(isEquiv, exprVarType, this.varLog);
		return true;
	}

	private boolean isMatchVar(KonohaVarType t) {
		TypeVarLog l = this.varLog;
		while(l != null) {
			if(l.var == t) {
				if(!l.isEquiv) {
					return true;
				}
			}
			l = l.next;
		}
		return false;
	}
	
	private boolean syncVarType(KonohaVarType var) {
		if(var.isUnresolved() && this.isUnresolved()) {
			return true;
		}
		if(this.isUnresolved() && var.isResolved()) {
			this.appendTypeLog(0, var.getResolvedType());
			return true;
		}
		if(var.isUnresolved() && this.isResolved()) {
			var.appendTypeLog(0, this.getResolvedType());
			return true;
		}
		return false;
	}

//	private boolean checkVarLog() {
//		if(this.isResolved()) {
//			TypeVarLog l = this.varLog;
//			KonohaType t = this.getResolvedType();
//			while(l != null) {
//				if(l.isEquiv) {
//					
//				}
//				l = l.next;
//			}
//		}
//	}

	boolean equalsPrimitiveType(KonohaPrimitiveType t, KonohaVarType var) {
		return var.update(t);
	}

	boolean equalsStructType(KonohaStructType t, KonohaVarType var) {
		return false; //var.updateRealType(t);
	}

	boolean matchStructType(KonohaStructType t, KonohaVarType var) {
		return false; //var.updateRealType(t); // FIXME
	}
}

