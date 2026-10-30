package org.example.transactionservice.service;

public class TransferCompensationException extends RuntimeException {

    public TransferCompensationException(String message, Throwable cause) {
        super(message, cause);
    }
}
