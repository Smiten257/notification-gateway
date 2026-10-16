package gateway.notification;

/** Thrown when a notification could not be delivered by any provider it was allowed to use. */
public class DeliveryFailedException extends RuntimeException {

    public DeliveryFailedException(String message) {
        super(message);
    }
}
