package nez.peg.tpeg.type;

import static nez.peg.tpeg.type.TypeException.typeError;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import nez.peg.tpeg.type.LType.AbstractStructureType;
import nez.peg.tpeg.type.LType.ArrayType;
import nez.peg.tpeg.type.LType.CaseContextType;
import nez.peg.tpeg.type.LType.OptionalType;
import nez.peg.tpeg.type.LType.StructureType;
import nez.peg.tpeg.type.LType.TupleType;
import nez.peg.tpeg.type.LType.UnionType;

/**
 * Created by skgchxngsxyz-osx on 15/08/27.
 */
public class TypeEnv {
	private final String packageName;

	/**
	 * key is mangled name
	 */
	private final Map<String, LType> typeMap = new HashMap<>();

	private LType intType;
	private LType floatType;
	private LType boolType;
	private LType stringType;

	public TypeEnv() {

		// generate package name
		int num = new Random(System.currentTimeMillis()).nextInt();
		if (num < 0) {
			num = -num;
		}
		this.packageName = "loglang/generated" + num;

		// register type
		this.typeMap.put(LType.voidType.getUniqueName(), LType.voidType);
		this.typeMap.put(LType.anyType.getUniqueName(), LType.anyType);

		try {
			// add basic type
			this.intType = this.newBasicType("int", int.class);
			this.floatType = this.newBasicType("float", float.class);
			this.boolType = this.newBasicType("bool", boolean.class);
			this.stringType = this.newBasicType("string", String.class);
		} catch (TypeException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public String getPackageName() {
		return this.packageName;
	}

	/**
	 *
	 * @param simpleName
	 * @param clazz
	 * @return
	 * @throws TypeException
	 */
	private LType newBasicType(String simpleName, Class<?> clazz) throws TypeException {
		String mangledName = Mangler.mangleBasicType(simpleName);
		LType type = new LType(mangledName, clazz.getCanonicalName(), LType.anyType);
		return this.registerType(mangledName, type);
	}

	/**
	 *
	 * @param mangledName
	 * @param type
	 * @return
	 * @throws TypeException
	 */
	private LType registerType(String mangledName, LType type) throws TypeException {
		if (this.typeMap.put(mangledName, type) == null) { // FIXME:nonNull
			typeError("already defined type: " + type.getSimpleName());
		}
		return type;
	}

	/**
	 *
	 * @param mangledName
	 *            must be mangled name
	 * @return if not found, return null
	 */
	public LType getTypeByMangledName(String mangledName) {
		return this.typeMap.get(mangledName);
	}

	/**
	 *
	 * @param simpleName
	 * @return
	 * @throws TypeException
	 */
	public LType getBasicType(String simpleName) throws TypeException {
		LType type = this.getTypeByMangledName(Mangler.mangleBasicType(simpleName));
		if (type == null) {
			typeError("undefined type: " + simpleName);
		}
		return type;
	}

	public LType getAnyType() {
		return LType.anyType;
	}

	public LType getVoidType() {
		return LType.voidType;
	}

	public LType getIntType() {
		return intType;
	}

	public LType getFloatType() {
		return floatType;
	}

	public LType getBoolType() {
		return boolType;
	}

	public LType getStringType() {
		return stringType;
	}

	public boolean isPrimaryType(String simpleName) {
		String mangledName = Mangler.mangleBasicType(simpleName);
		LType type = this.getTypeByMangledName(mangledName);
		return type != null && this.isPrimaryType(type);
	}

	/**
	 *
	 * @param type
	 * @return if type is int, float or string, return true.
	 */
	public boolean isPrimaryType(LType type) {
		return this.intType.equals(type) || this.floatType.equals(type) || this.stringType.equals(type);
	}

	/**
	 *
	 * @param elementType
	 *            must not be void
	 * @return
	 * @throws TypeException
	 */
	public ArrayType getArrayType(LType elementType) throws TypeException {
		String mangledName;
		try {
			mangledName = Mangler.mangleArrayType(elementType);
		} catch (IllegalArgumentException e) {
			throw new TypeException(e.getMessage());
		}

		LType type = this.getTypeByMangledName(mangledName);
		if (type == null) { // create array type
			type = this.registerType(mangledName, new ArrayType(mangledName, elementType));
		}
		return (ArrayType) type;
	}

	/**
	 *
	 * @param elementType
	 *            must not be void
	 * @return
	 * @throws TypeException
	 */
	public OptionalType getOptionalType(LType elementType) throws TypeException {
		String mangledName;
		try {
			mangledName = Mangler.mangleOptionalType(elementType);
		} catch (IllegalArgumentException e) {
			throw new TypeException(e.getMessage());
		}
		LType type = this.getTypeByMangledName(mangledName);
		if (type == null) {
			type = this.registerType(mangledName, new OptionalType(mangledName, elementType));
		}
		return (OptionalType) type;
	}

	public TupleType getTupleType(LType[] elementTypes) throws TypeException {
		String mangledName;
		try {
			mangledName = Mangler.mangleTupleType(elementTypes);
		} catch (IllegalArgumentException e) {
			throw new TypeException(e.getMessage());
		}
		LType type = this.getTypeByMangledName(mangledName);
		if (type == null) {
			type = this.registerType(mangledName, new TupleType(mangledName, elementTypes));
		}
		return (TupleType) type;
	}

	public UnionType getUnionType(LType[] elementTypes) throws TypeException {
		try {
			elementTypes = Mangler.flattenUnionElements(elementTypes);
		} catch (IllegalArgumentException e) {
			throw new TypeException(e.getMessage());
		}
		String mangledName = Mangler.mangleUnionTypeUnsafe(elementTypes);
		LType type = this.getTypeByMangledName(mangledName);
		if (type == null) {
			type = this.registerType(mangledName, new UnionType(mangledName, elementTypes));
		}
		return (UnionType) type;
	}

	/**
	 *
	 * @param name
	 *            must be simple name
	 * @return
	 * @throws TypeException
	 *             if already defined.
	 */
	public StructureType newStructureType(String name) throws TypeException {
		String mangledName = Mangler.mangleBasicType(Objects.requireNonNull(name));
		return (StructureType) this.registerType(mangledName, new StructureType(mangledName));
	}

	/**
	 *
	 * @param type
	 * @param fieldName
	 * @param fieldType
	 * @throws TypeException
	 *             if already defined.
	 */
	public void defineField(AbstractStructureType type, String fieldName, LType fieldType) throws TypeException {
		if (!type.addField(fieldName, fieldType)) {
			typeError("already undefined field: " + fieldName + ", in " + type.getSimpleName());
		}
	}

	/**
	 * create type name of anonymous type representing prefix pattern.
	 * 
	 * @return
	 */
	public static String getAnonymousPrefixTypeName() {
		return "__Anonymous_prefix_type__";
	}

	public static String getAnonymousCaseTypeNamePrefix() {
		return "__Anonymous_case_type_";
	}

	public static String createAnonymousCaseTypeName(int caseIndex) {
		return getAnonymousCaseTypeNamePrefix() + caseIndex + "__";
	}

	public CaseContextType newCaseContextType(int index) throws TypeException {
		String simpleName = "CaseContextImpl" + index;
		String mangledName = Mangler.mangleBasicType(simpleName);
		return (CaseContextType) this.registerType(mangledName, new CaseContextType(mangledName, this.packageName + "/" + simpleName));
	}
}
