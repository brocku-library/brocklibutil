package brocklibutil.helper;

import org.springframework.stereotype.Component;

@Component
public class Helper {
    
    public static final String CLIENT_ID_KEY = "client_id";
    public static final String CLIENT_SECRET_KEY = "client_secret";
    public static final String GRANT_TYPE_KEY = "grant_type";
    public static final String GRANT_TYPE_VALUE = "client_credentials";
    public static final String ACCESS_TOKEN_KEY = "access_token";
    public static final String AUTH_TOKEN_KEY = "Authorization";
    public static final String AUTH_TOKEN_PREFIX = "Bearer ";
}
