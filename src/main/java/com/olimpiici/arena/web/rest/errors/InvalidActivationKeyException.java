package com.olimpiici.arena.web.rest.errors;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

public class InvalidActivationKeyException extends AbstractThrowableProblem {

    private static final long serialVersionUID = 1L;

    public InvalidActivationKeyException() {
        super(ErrorConstants.DEFAULT_TYPE, "No user was found for this activation key", Status.BAD_REQUEST);
    }
}
