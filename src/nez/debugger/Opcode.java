package nez.debugger;

public enum Opcode {
	Iexit,
	Inop,
	Icall,
	Iret,
	Ijump,
	Iiffail,
	Ipush,
	Ipop,
	Ipeek,
	Isucc,
	Ifail,
	Ichar,
	Istr,
	Icharclass,
	Iany,
	Inew,
	Ileftnew,
	Icapture,
	Imark,
	Itag,
	Ireplace,
	Icommit,
	Iabort,
	Idef,
	Iis,
	Iisa,
	Iexists,
	Ibeginscope,
	Ibeginlocalscope,
	Iendscope,
	Ialtstart,
	Ialt,
	Ialtend,
	Ialtfin
}