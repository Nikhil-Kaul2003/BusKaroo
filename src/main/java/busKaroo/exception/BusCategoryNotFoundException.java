package busKaroo.exception;

public class BusCategoryNotFoundException extends ResourceNotFoundException {
    public BusCategoryNotFoundException(Long id) {
        super("Bus category not found with id: " + id);
    }
}
