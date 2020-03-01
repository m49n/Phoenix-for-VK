package biz.dealnote.messenger.api;

/**
 * Created by admin on 16.07.2017.
 * phoenix
 */
public class NeedValidationException extends Exception {

    private final String type;
    private final String validate_url;

    public NeedValidationException(String type, String validate_url) {
        this.type = type;
        this.validate_url = validate_url;
    }

    public String getValidationType() {
        return type;
    }
    public String getValidationURL() {
        return validate_url;
    }
}