/**
 * Copyright (C) 2017 Devexperts LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */
package com.devexperts.usages.analyzer;

public enum UseKind {
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
}
