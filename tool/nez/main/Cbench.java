package nez.main;

import java.io.IOException;

import nez.ast.Tree;
import nez.io.SourceStream;
import nez.parser.Parser;
import nez.util.ConsoleUtils;

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
			SourceStream input = nextInputSource();
			ConsoleUtils.print(input.getResourceName() + ": ");
			double dsum = 0.0;
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
				len += input.length();
				dsum += d;
			}
			ConsoleUtils.println("%.2f [ms]", dsum / 5);
			total += dsum;
		}
		double s = (total / 1000);
		ConsoleUtils.println("Throughput %.2f [B/s] %.2f [KiB/s] %.2f [MiB/s]", (len / s), (len / 1024 / s), (len / 1024 / 1024 / s));
	}

}
