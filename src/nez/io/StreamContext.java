//package nez.io;
//
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.RandomAccessFile;
//import java.io.UnsupportedEncodingException;
//import java.util.LinkedHashMap;
//import java.util.Map;
//
//import nez.util.StringUtils;
//
//public class StreamContext extends SourceStream {
//	protected StreamContext(String resourceName, long linenum) {
//		super(resourceName, linenum);
//	}
//
//	InputStream ins;
//	long readpos = 0;
//
//	@Override
//	public int byteAt(long pos) {
//		if(readpos < pos) {
//			// buffer;
//		}
//		ins.read()
//		return 0;
//	}
//
//	@Override
//	public byte[] subbyte(long startIndex, long endIndex) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public long length() {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
//	@Override
//	public boolean match(long pos, byte[] text) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//
//	@Override
//	public String substring(long startIndex, long endIndex) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//
//	@Override
//	public long linenum(long pos) {
//		// TODO Auto-generated method stub
//		return 0;
//	}
//
// }
