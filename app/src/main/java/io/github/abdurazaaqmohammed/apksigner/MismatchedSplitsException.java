package io.github.abdurazaaqmohammed.apksigner;

public class MismatchedSplitsException extends Exception {
    public MismatchedSplitsException(String cancelled) {
        super(cancelled);
    }
}
