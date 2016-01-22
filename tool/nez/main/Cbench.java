package nez.main;

import java.io.IOException;

import nez.ast.Source;
import nez.ast.Tree;
import nez.parser.Parser;
import nez.util.ConsoleUtils;
import nez.util.FileBuilder;

public class Cbench extends Command {
	@Override
	public void exec() throws IOException {
		checkInputSource();
		Parser parser = newParser();
		parser.setDisabledUnconsumed(true);
		parser.compile();
		long len = 0;
		double total = 0.0;
		while (hasInputSource()) {
			Source input = nextInputSource();
			ConsoleUtils.print(FileBuilder.extractFileName(input.getResourceName()) + ": ");
			double dsum = 0.0;
			double prev = 10000.0;
			boolean JIT = true;
			for (int c = 0; c < 5; c++) {
				long t1 = System.nanoTime();
				Tree<?> node = parser.parse(input);
				if (node == null) {
					parser.showErrors();
					break;
				}
				long t2 = System.nanoTime();
				double d = (t2 - t1) / 1000000.0;
				ConsoleUtils.print("%.2f ", d);
				if (JIT) {
					if ((prev - d) > 0.0) {
						prev = d;
						c--;
						continue;
					}
					JIT = false;
				}
				len += input.length();
				dsum += d;
			}
			ConsoleUtils.println("(ave) %.2f [ms]", dsum / 5);
			total += dsum;
		}
		double s = (total / 1000);
		ConsoleUtils.println("Throughput %.2f [B/s] %.2f [KiB/s] %.2f [MiB/s]", (len / s), (len / 1024 / s), (len / 1024 / 1024 / s));
	}
}
