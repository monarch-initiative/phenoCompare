package org.monarchinitiative.phcompare;

/**
 * EmptyGroupException is thrown when one or both of the PhenoCompare groups (gene or patient) is/are empty.
 *
 * @author Hannah Blau (blauh)
 * @version 0.0.1
 */
class EmptyGroupException extends Exception {

    EmptyGroupException() {
    }

    EmptyGroupException(String message) {
        super(message);
    }

    EmptyGroupException(String message, Throwable cause) {
        super(message, cause);
    }

    EmptyGroupException(Throwable cause) {
        super(cause);
    }

    EmptyGroupException(String message, Throwable cause,
                               boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
