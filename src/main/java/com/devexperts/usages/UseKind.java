package com.devexperts.usages;

import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.StringTokenizer;

enum UseKind {
	UNKNOWN(false),
	EXTEND(true),
	IMPLEMENT(true),
	OVERRIDE(false),
	SIGNATURE(false),
	ANNOTATION(false),
	THROW(false),
	CATCH(false),
	RETURN(false),
	ARGUMENT(false),
	CONSTANT(false),
	FIELD(false),
	NEW(false),
	ANEWARRAY(false),
	CHECKCAST(false),
	INSTANCEOF(false),
	GETFIELD(true),
	PUTFIELD(true),
	GETSTATIC(true),
	PUTSTATIC(true),
	INVOKEVIRTUAL(true),
	INVOKESPECIAL(false),
	INVOKESTATIC(true),
	INVOKEINTERFACE(true),
	INVOKEDYNAMIC(true);

	public final boolean inheritedUse;

	UseKind(boolean inheritedUse) {
		this.inheritedUse = inheritedUse;
	}

	public static void printUseKinds(PrintWriter out, EnumSet<UseKind> useKinds) {
		out.print(Fmt.USE_KINDS_PREFIX);
		boolean firstKind = true;
		for (UseKind useKind : useKinds) {
			if (firstKind)
				firstKind = false;
			else
				out.print(Fmt.USE_KINDS_SEPARATOR);
			out.print(useKind);
		}
	}

	public static EnumSet<UseKind> parseUseKinds(String s) {
		EnumSet<UseKind> useKinds = EnumSet.noneOf(UseKind.class);
		StringTokenizer st = new StringTokenizer(s, Fmt.USE_KINDS_SEPARATOR);
		while (st.hasMoreTokens())
			useKinds.add(valueOf(st.nextToken()));
		return useKinds;
	}
}
