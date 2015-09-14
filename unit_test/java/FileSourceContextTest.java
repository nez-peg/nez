//import static org.junit.Assert.assertTrue;
//
//import java.io.IOException;
//import java.net.URISyntaxException;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//
//import nez.io.SourceContext;
//
//import org.junit.Test;
//
//
//public class FileSourceContextTest {
//	@Test
//	public void test() throws IOException, URISyntaxException {
//		assertTrue(true);
//		Path path = Paths.get(this.getClass().getResource("lines.txt").toURI());
//		SourceContext sc = SourceContext.newFileContext(path.getFileName().toString());
//		int linenum = 1;
//		while(sc.hasUnconsumed()) {
//			long pos = sc.getPosition();
//			int ch = sc.byteAt(pos);
//			assertTrue(sc.linenum(pos) == linenum);
//			if(ch == '\n') {
//				linenum++;
//			}
//			sc.consume(1);
//		}
//	}
//
// }
