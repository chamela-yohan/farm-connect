package lk.farmconnect.order.exception;

public class ProductUnavailableException extends CartException {
    public ProductUnavailableException(String message) {
        super(message);
    }
}