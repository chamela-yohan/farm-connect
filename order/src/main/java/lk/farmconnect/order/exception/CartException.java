package lk.farmconnect.order.exception;

import lk.farmconnect.common.exception.BusinessException; // Or create a base BusinessException

public class CartException extends BusinessException {
    public CartException(String message) {
        super(message);
    }
}