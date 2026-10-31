package busKaroo.exception;

public class BusNotFoundException extends ResourceNotFoundException {
    public BusNotFoundException(Long id) {
        super("Bus not found with id: " + id);
    }
}
