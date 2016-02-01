package nez.type;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;

import nez.ast.Symbol;
import nez.util.StringUtils;
import nez.util.UList;

public class Schema {
	HashMap<String, Type> nameMap = new HashMap<>();

	public final void add(String name, Type type) {
		nameMap.put(name, type);
	}

	public final Type getType(String uname) {
		Type t = nameMap.get(uname);
		return t;
	}

	public final Type derefType(Type t) {
		if (t instanceof ReferenceType) {
			HashSet<String> names = new HashSet<>();
			t = deref(names, t);
		}
		return t;
	}

	public final ObjectType newType(String name, String... props) {
		Property[] members = null;
		if (props.length > 0) {
			members = new Property[props.length / 2];
			int c = 0;
			for (int i = 0; i < props.length; i += 2) {
				members[c] = new Property(Symbol.unique(props[i]), getType(props[i + 1]));
				c++;
			}
		}
		ObjectType t = new Schema.ObjectType(name.substring(1), members);
		add(name, t);
		return t;
	}

	public final void deref() {
		HashSet<String> names = new HashSet<>();
		for (String name : nameMap.keySet()) {
			try {
				names.add(name);
				Type t = deref(names, nameMap.get(name));
				names.clear();
				System.out.println(name + ": " + nameMap.get(name) + "\n =>\t" + t);
				nameMap.put(name, t);
			} catch (java.lang.StackOverflowError e) {
				System.out.println(name + ": " + e);
			}
		}
	}

	private Type deref(HashSet<String> names, Type t) {
		if (t instanceof ReferenceType) {
			String nname = ((ReferenceType) t).nname;
			if (!names.contains(nname)) {
				names.add(nname);
				t = deref(names, nameMap.get(nname));
				names.remove(nname);
				return t;
			}
			return null;
		}
		if (t instanceof UnaryType) {
			((UnaryType) t).type = deref(names, ((UnaryType) t).type);
			if (((UnaryType) t).type == null) {
				return null;
			}
			return t;
		}
		if (t instanceof UnionType) {
			UnionType u = (UnionType) t;
			boolean hasSelf = false;
			for (int i = 0; i < u.size(); i++) {
				u.unions[i] = deref(names, u.get(i));
				if (u.unions[i] == null) {
					hasSelf = true;
				}
			}
			if (hasSelf) {
				UList<Type> l = new UList<>(new Type[u.size() - 1]);
				for (int i = 0; i < u.size(); i++) {
					if (u.unions[i] != null) {
						l.add(u.unions[i]);
					}
				}
				u.unions = l.compactArray();
			}
			return u;
		}
		return t;
	}

	private Property deref(HashSet<String> names, Property p) {
		p.type = deref(names, p.type);
		if (p.type == null) {
			return null;
		}
		return p;
	}

	public static abstract class AbstractType implements Type {

		abstract void format(StringBuilder sb);

		@Override
		public final String toString() {
			StringBuilder sb = new StringBuilder();
			format(sb);
			return sb.toString();
		}
	}

	private static Property[] nullMember = new Property[0];

	public static class ObjectType extends AbstractType {
		Schema space;
		Symbol tag;
		boolean setSemantics;
		Property[] members;

		public ObjectType(String tag, Property[] members) {
			this.tag = Symbol.unique(tag);
			this.members = members == null ? nullMember : members;
		}

		public Symbol getTag() {
			return tag;
		}

		public final int size() {
			return members == null ? 0 : members.length;
		}

		public final Property get(int index) {
			return members[index];
		}

		public final boolean isSingleType() {
			return size() == 0;
		}

		public final boolean isListType() {
			if (size() == 1 && get(0).label == null) {
				Type t = get(0).type;
				return (t instanceof ZeroMoreType || t instanceof OneMoreType);
			}
			return false;
		}

		public final boolean isRecordType() {
			if (size() > 0) {
				for (Property p : members) {
					if (p.type instanceof Schema.OptionType) {
						return false;
					}
					if (p.label != null) {
						return false;
					}
				}
				return true;
			}
			return false;
		}

		public final Type getListElementType() {
			if (size() == 1 && get(0).label == null) {
				Type t = get(0).type;
				if ((t instanceof ZeroMoreType || t instanceof OneMoreType)) {
					return ((UnaryType) t).type;
				}
			}
			return null;
		}

		public final boolean isEmptyObjectType() {
			if (size() == 1 && get(0).label == SchemaTransformer._member) {
				Type t = get(0).type;
				return (t instanceof ZeroMoreType || t instanceof OneMoreType);
			}
			return false;
		}

		@Override
		public void format(StringBuilder sb) {
			sb.append("#");
			sb.append(tag);
			sb.append(setSemantics ? '{' : '[');
			int c = 0;
			for (Property p : members) {
				if (c > 0) {
					sb.append(", ");
				}
				p.format(sb);
				c++;
			}
			sb.append(setSemantics ? '}' : ']');
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ObjectType) {
				ObjectType t = (ObjectType) o;
				if (tag == t.tag && members.length == t.members.length) {
					for (Property p : members) {
						if (!t.contains(p)) {
							return false;
						}
					}
					return true;
				}
				return false;
			}
			if (o instanceof ReferenceType) {
				return ((ReferenceType) o).equals(this);
			}
			return false;
		}

		private boolean contains(Property p2) {
			for (Property p : members) {
				if (p.label == p2.label && p.key == p2.key) {
					if (p.type.equals(p2.type)) {
						return true;
					}
				}
			}
			return false;
		}

	}

	public static class Property {
		String key;
		Symbol label;
		Type type;

		public Property(Symbol label, Type type) {
			this.label = label;
			this.type = type;
		}

		public String getKey() {
			return key != null ? key : (label == null ? null : label.getSymbol());
		}

		public Symbol getLabel() {
			return label;
		}

		public Type getType() {
			return type;
		}

		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			format(sb);
			return sb.toString();
		}

		public void format(StringBuilder sb) {
			if (key != null) {
				StringUtils.formatQuoteString(sb, '"', key, '"');
			}
			if (label != null) {
				sb.append("$");
				sb.append(label);
			}
			if (key != null || label != null) {
				sb.append(": ");
			}
			formatType(type, sb);
		}
	}

	private final static void formatType(Type t, StringBuilder sb) {
		if (t instanceof AbstractType) {
			((AbstractType) t).format(sb);
		} else {
			sb.append(t);
		}
	}

	// TypeConstructor

	/* UnionType */

	public static class UnionType extends AbstractType {
		Type[] unions;

		public UnionType(Type[] unions) {
			this.unions = unions;
		}

		public final int size() {
			return unions.length;
		}

		public final Type get(int index) {
			return unions[index];
		}

		@Override
		void format(StringBuilder sb) {
			int c = 0;
			for (Type t : unions) {
				if (c > 0) {
					sb.append("|");
				}
				sb.append(t);
				c++;
			}
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof UnionType) {
				UnionType u = (UnionType) o;
				if (unions.length == u.unions.length) {
					for (Type t : unions) {
						if (!u.contains(t)) {
							return false;
						}
					}
					return true;
				}
				return false;
			}
			if (o instanceof ReferenceType) {
				return ((ReferenceType) o).equals(this);
			}
			return false;
		}

		public boolean contains(Type t2) {
			for (Type t : unions) {
				if (t.equals(t2)) {
					return true;
				}
			}
			return false;
		}

	}

	public static abstract class UnaryType extends AbstractType {
		public Type type;

		protected void format(StringBuilder sb, String postfix) {
			if (type instanceof UnionType) {
				sb.append("(");
			}
			formatType(type, sb);
			if (type instanceof UnionType) {
				sb.append(")");
			}
			sb.append("+");
		}

	}

	public static class OptionType extends UnaryType {
		public OptionType(Type type) {
			this.type = type;
		}

		@Override
		void format(StringBuilder sb) {
			format(sb, "?");
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof OptionType) {
				return type.equals(((UnaryType) o).type);
			}
			if (o instanceof ReferenceType) {
				return ((ReferenceType) o).equals(this);
			}
			return false;
		}

	}

	public static class ZeroMoreType extends UnaryType {

		public ZeroMoreType(Type type) {
			this.type = type;
		}

		@Override
		void format(StringBuilder sb) {
			format(sb, "*");
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ZeroMoreType) {
				return type.equals(((UnaryType) o).type);
			}
			if (o instanceof ReferenceType) {
				return ((ReferenceType) o).equals(this);
			}
			return false;
		}
	}

	public static class OneMoreType extends UnaryType {

		public OneMoreType(Type type) {
			this.type = type;
		}

		@Override
		void format(StringBuilder sb) {
			format(sb, "+");
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof OneMoreType) {
				return type.equals(((UnaryType) o).type);
			}
			if (o instanceof ReferenceType) {
				return ((ReferenceType) o).equals(this);
			}
			return false;
		}

	}

	public final static Type newOptionType(Type t) {
		if (t instanceof OptionType || t instanceof ZeroMoreType) {
			return t;
		}
		if (t instanceof OneMoreType) {
			return new Schema.ZeroMoreType(((OneMoreType) t).type);
		}
		if (t instanceof UnionType) {
			UnionType u = (UnionType) t;
			for (int i = 0; i < u.unions.length; i++) {
				u.unions[i] = newOptionType(u.unions[i]);
			}
			return u;
		}
		return new Schema.OptionType(t);
	}

	public final static Type newZeroMoreType(Type t) {
		if (t instanceof ZeroMoreType) {
			return t;
		}
		if (t instanceof OneMoreType || t instanceof OptionType) {
			return new ZeroMoreType(((UnaryType) t).type);
		}
		if (t instanceof UnionType) {
			UnionType u = (UnionType) t;
			for (int i = 0; i < u.unions.length; i++) {
				u.unions[i] = newZeroMoreType(u.unions[i]);
			}
			return u;
		}
		return new Schema.ZeroMoreType(t);
	}

	public final static Type newOneMoreType(Type t) {
		if (t instanceof OneMoreType || t instanceof ZeroMoreType) {
			return t;
		}
		if (t instanceof OptionType) {
			return new ZeroMoreType(((OneMoreType) t).type);
		}
		if (t instanceof UnionType) {
			UnionType u = (UnionType) t;
			for (int i = 0; i < u.unions.length; i++) {
				u.unions[i] = newOneMoreType(u.unions[i]);
			}
			return u;
		}
		return new Schema.OneMoreType(t);
	}

	public static class ReferenceType extends AbstractType {
		Schema space;
		String nname;

		public ReferenceType(Schema schema, String nname) {
			this.space = schema;
			this.nname = nname;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof ReferenceType) {
				ReferenceType t = (ReferenceType) o;
				return (nname.equals(t.nname));
			}
			if (space != null) {
				Type t = space.getType(nname);
				if (t != null) {
					return t.equals(o);
				}
			}
			return false;
		}

		@Override
		void format(StringBuilder sb) {
			sb.append(nname);
		}

	}

}
