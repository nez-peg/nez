package nez.ast;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nez.Strategy;
import nez.util.StringUtils;

public class LiteralTransducer extends AbstractTreeVisitor {

	Strategy strategy;

	LiteralTransducer(Strategy strategy) {
		this.strategy = strategy;
	}

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

	public String newText(AbstractTree<?> node) {
		return node.toText();
	}

	protected List<Object> newList(int n) {
		return new ArrayList<Object>(n);
	}

	public List<Object> newList(AbstractTree<?> node) {
		try {
			List<Object> l = newList(node.size());
			for (AbstractTree<?> sub : node) {
				l.add(newInstance(sub));
			}
			return l;
		} catch (LiteralFormatException e) {
			throw e;
		} catch (RuntimeException e) {
			return report(e, node, newList(0));
		}
	}

	protected Map<String, Object> newMap() {
		return new HashMap<String, Object>();
	}

	public Map<String, Object> newMap(AbstractTree<?> node) {
		Map<String, Object> m = newMap();
		for (int i = 0; i < node.size(); i++) {
			SymbolId label = node.getLabel(i);
			if (label != null) {
				m.put(label.getSymbol(), newInstance(node.get(i)));
			}
		}
		return m;
	}

	// alias

	public Integer newInt(AbstractTree<?> node) {
		return newInteger(node);
	}

	// report

	private <T> T report(RuntimeException e, AbstractTree<?> node, T initValue) {
		if (strategy == null) {
			throw new LiteralFormatException(node.formatSourceMessage("error", e.getMessage()));
		}
		strategy.reportError(node, e.getMessage());
		return initValue;
	}

	private <T> T report(String msg, AbstractTree<?> node, T initValue) {
		if (strategy == null) {
			throw new LiteralFormatException(node.formatSourceMessage("error", msg));
		}
		strategy.reportError(node, msg);
		return initValue;
	}

}

class LiteralFormatException extends RuntimeException {

	public LiteralFormatException(String msg) {
		super(msg);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 9141813132121528343L;

}