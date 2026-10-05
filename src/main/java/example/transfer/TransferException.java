package example.transfer;

/** Transfer failure with its cause and the number of elements written before it. */
public final class TransferException extends Exception {
    private static final long serialVersionUID = 1L;
    private final long transferredCount;

    public TransferException(long transferredCount, Throwable cause) {
        super("Transfer failed, elements written: " + transferredCount, cause);
        this.transferredCount = transferredCount;
    }

    public long transferredCount() {
        return transferredCount;
    }
}
