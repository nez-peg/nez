package nez.type;

import java.lang.reflect.Type;

public class TreeType implements Type {
	String tag;
	Property[] properties;

	public TreeType(String tag, Property[] properties) {
		this.tag = tag;
		this.properties = properties;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("#");
		sb.append(tag);
		sb.append("[");
		for (int i = 0; i < properties.length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			if (properties[i].label != null) {
				sb.append(properties[i].label);
				sb.append(": ");
			}
			sb.append(properties[i].type);
		}
		sb.append("]");
		return sb.toString();
	}

}
