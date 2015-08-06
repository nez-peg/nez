package nez.vm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import nez.ast.Tag;

public class ByteCoder {
	ByteArrayOutputStream stream;
	
	public void encodeBoolean(boolean b) {
		stream.write(b ? 1: 0);
	}

	public void encodeInt(int num) {
		int n3 = num % 256;
		num = num / 256;
		int n2 = num % 256;
		num = num / 256;
		int n1 = num % 256;
		int n0 = num / 256;
		stream.write(n0);
		stream.write(n1);
		stream.write(n2);
		stream.write(n3);
	}

	public void encodeShort(int num) {
		int n1 = num % 256;
		int n0 = num / 256;
		stream.write(n0);
		stream.write(n1);
	}

	public void encodeOpcode(byte opcode) {
		stream.write(opcode);
	}


	public final void encodeJumpAddr(Instruction jump) {
		encodeInt(jump.id);
	}

	public void encodeShift(int shift) {
		stream.write(shift);
	}

	public void encodeByteChar(int byteChar) {
		stream.write(byteChar);
	}

	public void encodeByteMap(boolean[] byteMap) {
		for(int i = 0; i < 256; i+=32) {
			encodeByteMap(byteMap, i);
		}
	}

	private void encodeByteMap(boolean[] b, int offset) {
		int n = 0;
		for(int i = 0; i < 32; i++) {
			if(b[offset+i]) {
				n |= (1 << (31-i));
			}
		}
		encodeInt(n);
	}

	public void encodeString(byte[] utf8) {
		encodeShort(utf8.length);
		try {
			stream.write(utf8);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void encodeTag(Tag tag) {
		encodeString(tag.getName().getBytes());
	}

	public void encodeSymbolTable(Tag tableName) {
		// TODO Auto-generated method stub
		
	}

	public void encodeIndex(int index) {
		// TODO Auto-generated method stub
		
	}


}
