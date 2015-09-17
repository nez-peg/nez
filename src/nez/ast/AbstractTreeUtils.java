package nez.ast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import nez.util.StringUtils;

public class AbstractTreeUtils {

	public final static String digestString(AbstractTree<?> node) {
		StringBuilder sb = new StringBuilder();
		byte[] hash = digest(node);
		for (int i = 0; i < hash.length; i++) {
			int d = hash[i] & 0xff;
			// if (d < 0) {
			// d += 256;
			// }
			if (d < 16) {
				sb.append("0");
			}
			sb.append(Integer.toString(d, 16));
		}
		return sb.toString();
	}

	public final static byte[] digest(AbstractTree<?> node) {
		try {
			MessageDigest md;
			md = MessageDigest.getInstance("MD5");
			updateDigest(node, md);
			return md.digest();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return new byte[16];
	}

	static void updateDigest(AbstractTree<?> node, MessageDigest md) {
		md.update((byte) '#');
		md.update(StringUtils.toUtf8(node.getTag().getSymbol()));
		for (int i = 0; i < node.size(); i++) {
			Symbol label = node.getLabel(i);
			if (label != null) {
				md.update((byte) '$');
				md.update(StringUtils.toUtf8(label.getSymbol()));
			}
			updateDigest(node.get(i), md);
		}
		if (node.size() == 0) {
			md.update(StringUtils.toUtf8(node.toText()));
		}
	}

}
