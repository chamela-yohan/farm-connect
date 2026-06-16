package lk.farmconnect.order.exception;

public class InvalidQuantityException extends CartException {
    public InvalidQuantityException(String message) {
        super(message);
    }
}