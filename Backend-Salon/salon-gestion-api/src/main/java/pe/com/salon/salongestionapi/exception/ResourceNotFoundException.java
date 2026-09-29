package pe.com.salon.salongestionapi.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) { super(String.format("%s no encontrado con %s : '%s'", resourceName, fieldName, fieldValue)); }
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
