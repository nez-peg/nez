package nez.ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import nez.util.StringUtils;

public class LiteralTransducer extends AbstractTreeVisitor {

	public Object newInstance(AbstractTree<?> node) {
		return visit("new", node);
	}

	public Boolean newBoolean(AbstractTree<?> node) {
		try {
			if (node.toText().equals("true")) {
				return true;
			}
			return false;
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report("unknown boolean literal: " + node.toText(), node, false);
		}
	}

	public Integer newInteger(AbstractTree<?> node) {
		try {
			return Integer.parseInt(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0);
		}
	}

	public Long newLong(AbstractTree<?> node) {
		try {
			return Long.parseLong(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0L);
		}
	}

	public BigInteger newBigInteger(AbstractTree<?> node) {
		try {
			return new BigInteger(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, BigInteger.ZERO);
		}
	}

	public Float newFloat(AbstractTree<?> node) {
		try {
			return Float.parseFloat(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0.0f);
		}
	}

	public Double newDouble(AbstractTree<?> node) {
		try {
			return Double.parseDouble(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, 0.0);
		}
	}

	public String newString(AbstractTree<?> node) {
		try {
			return StringUtils.unquoteString(node.toText());
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, "");
		}
	}

	public List<?> newList(AbstractTree<?> node) {
		try {
			ArrayList<Object> l = new ArrayList<Object>(node.size());
			for (AbstractTree<?> sub : node) {
				l.add(newInstance(sub));
			}
			return l;
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, new ArrayList<Object>());
		}
	}

	// alias

	public Integer newInt(AbstractTree<?> node) {
		return newInteger(node);
	}

	// report

	private <T> T report(RuntimeException e, AbstractTree<?> node, T initValue) {
		return initValue;
	}

	private <T> T report(String msg, AbstractTree<?> node, T initValue) {
		return initValue;
	}

}

class LiteralFormatException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9141813132121528343L;

}