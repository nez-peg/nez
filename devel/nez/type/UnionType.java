package nez.type;

import java.lang.reflect.Type;

public class UnionType implements Type {
	Type[] unions;

	public UnionType(Type[] unions) {
		this.unions = unions;
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder();
		int c = 0;
		for (Type t : unions) {
			if (c > 0) {
				sb.append("|");
			}
			sb.append(t);
			c++;
		}
		return sb.toString();
	}
}
