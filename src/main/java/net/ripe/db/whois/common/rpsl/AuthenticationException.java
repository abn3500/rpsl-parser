/*
 * Copyright (c) 2013 RIPE NCC
 * All rights reserved.
 */

package net.ripe.db.whois.common.rpsl;

/*TODO: check if replacing NestableRuntimeException with a generic
 * RuntimeException was wise. Based on the Apache commons docs:
 * http://commons.apache.org/proper/commons-lang/article3_0.html
 * the notion that Throwables could be linked to a cause was
 * introduced in ~Java 5. Commons had previously provided
 * Nestable exceptions to serve the purpose, but due to the
 * new found redundancy they were removed in commons v3.
 * 
 * Given no part of the code we are keeping appears to reference either
 * NestableRuntimeException, and AuthenticationException is referenced
 * only by RpslObjectFilter.java (which throws it), I think we are
 * reasonably safe in replacing it. As it stands, it probably only
 * gets used in printing out stack traces, and to my knowledge, the
 *  behaviour there shouldn't be changed by using a generic
 *  RuntimeException 
 */
//import org.apache.commons.lang3.exception.NestableRuntimeException;

public class AuthenticationException extends RuntimeException {

    public AuthenticationException(String message) {
        super(message);
    }
}
