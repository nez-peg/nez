package nez;

import nez.main.Command;

public class Version {
	public final static int MainVersion = 1;
	public final static int MinorVersion = 0;
	public final static int REV=787;
	public final static boolean ReleasePreview = true;
	public final static String Version = "" + Command.MajorVersion + "." + Command.MinerVersion + "_" + Command.PatchLevel;
	public final static String Copyright = "Copyright (c) 2014-2015, Nez project authors";
	public final static String License = "BSD-Licensed Open Source";
}
