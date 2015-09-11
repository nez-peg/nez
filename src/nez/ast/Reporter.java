package nez.ast;

import java.util.ArrayList;
import java.util.HashSet;

import nez.NezOption;
import nez.util.ConsoleUtils;

public class Reporter {
	ArrayList<String> logs;
	HashSet<String> checks;

	public Reporter() {
		init();
	}

	void init() {
		this.logs = new ArrayList<String>();
		this.checks = new HashSet<String>();
	}

	private void log(String msg) {
		if (!this.checks.contains(msg)) {
			this.checks.add(msg);
			this.logs.add(msg);
		}
	}

	public void report(NezOption option) {
		for (String s : this.logs) {
			if (!option.enabledNoticeReport) {
				if (s.indexOf("notice") != -1) {
					continue; // skip notice
				}
			}
			ConsoleUtils.println(s);
		}
		this.init();
	}

	public final void reportError(SourcePosition s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("error", message));
		}
	}

	public final void reportWarning(SourcePosition s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("warning", message));
		}
	}

	public final void reportNotice(SourcePosition s, String message) {
		if (s != null) {
			log(s.formatSourceMessage("notice", message));
		}
	}

}
