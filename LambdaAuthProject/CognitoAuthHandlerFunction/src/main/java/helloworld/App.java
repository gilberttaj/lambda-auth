package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.exceptions.JWTDecodeException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;


import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Helper class for deserializing the request body
class IdTokenRequest {
    private String idToken;

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private List<String> allowedDomainsList;
    private final ObjectMapper objectMapper = new ObjectMapper(); // Jackson ObjectMapper

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("EVENT: " + input);

        if (allowedDomainsList == null) {
            String allowedDomainsEnv = System.getenv("ALLOWED_EMAIL_DOMAINS");
            if (allowedDomainsEnv == null || allowedDomainsEnv.trim().isEmpty()) {
                logger.log("ERROR: ALLOWED_EMAIL_DOMAINS environment variable is not set or is empty.");
                return createResponse("{\"error\":\"Configuration error: Allowed email domains not set.\"}", 500, logger);
            }
            allowedDomainsList = Arrays.asList(allowedDomainsEnv.split(",")).stream()
                                    .map(String::trim)
                                    .filter(s -> !s.isEmpty())
                                    .collect(Collectors.toList());
            logger.log("Allowed domains loaded: " + allowedDomainsList);
        }

        if (input == null || input.getBody() == null || input.getBody().trim().isEmpty()) {
            logger.log("ERROR: Request body is null or empty.");
            return createResponse("{\"error\":\"Request body is missing or empty.\"}", 400, logger);
        }

        String requestBody = input.getBody();
        logger.log("Request Body: " + requestBody);

        IdTokenRequest idTokenRequest;
        try {
            idTokenRequest = objectMapper.readValue(requestBody, IdTokenRequest.class);
        } catch (JsonProcessingException e) {
            logger.log("ERROR: Failed to parse JSON request body: " + e.getMessage());
            return createResponse("{\"error\":\"Invalid JSON format in request body.\"}", 400, logger);
        }

        if (idTokenRequest == null || idTokenRequest.getIdToken() == null || idTokenRequest.getIdToken().trim().isEmpty()) {
            logger.log("ERROR: 'idToken' not found or empty in request body.");
            return createResponse("{\"error\":\"'idToken' not found in request body.\"}", 400, logger);
        }

        String email = extractEmailFromIdToken(idTokenRequest.getIdToken(), logger);

        if (email == null || email.trim().isEmpty()) {
            logger.log("ERROR: Email not found or could not be decoded from ID Token.");
            return createResponse("{\"error\":\"Email not found in ID Token or token is invalid.\"}", 400, logger);
        }
        logger.log("Extracted email: " + email);

        String emailDomain = getDomainFromEmail(email, logger);
        if (emailDomain == null) {
            logger.log("ERROR: Could not extract domain from email: " + email);
            return createResponse("{\"error\":\"Invalid email format.\"}", 400, logger);
        }
        logger.log("Extracted domain: " + emailDomain);

        if (allowedDomainsList.contains(emailDomain.toLowerCase())) {
            logger.log("SUCCESS: Email domain '" + emailDomain + "' is allowed.");
            return createResponse("{\"message\":\"Authentication successful. Email domain allowed.\"}", 200, logger);
        } else {
            logger.log("FORBIDDEN: Email domain '" + emailDomain + "' is not in the allowed list: " + allowedDomainsList);
            return createResponse("{\"error\":\"Access denied. Email domain not authorized.\"}", 403, logger);
        }
    }

    private String extractEmailFromIdToken(String idToken, LambdaLogger logger) {
        try {
            DecodedJWT jwt = JWT.decode(idToken); // Decodes without verification
            // For production, VERIFY the token:
            // Algorithm algorithm = Algorithm.RSA256(publicKeyProvider); // publicKeyProvider fetches from JWKS
            // JWTVerifier verifier = JWT.require(algorithm)
            //     .withIssuer("https://cognito-idp.{region}.amazonaws.com/{userPoolId}")
            //     .build();
            // DecodedJWT verifiedJwt = verifier.verify(idToken);
            // return verifiedJwt.getClaim("email").asString();

            String email = jwt.getClaim("email").asString();
            boolean emailVerified = jwt.getClaim("email_verified").asBoolean();

            if(email != null && emailVerified) {
                return email;
            } else if (email != null && !emailVerified) {
                logger.log("WARN: Email claim present in token but email_verified is false. Email: " + email);
                // Depending on your policy, you might want to deny unverified emails.
                // For now, we'll proceed if the email claim exists.
                return email; // Or return null if unverified emails are not allowed.
            } else {
                logger.log("ERROR: Email claim not found or email_verified claim missing/false in ID Token.");
                return null;
            }
        } catch (JWTDecodeException e) {
            logger.log("ERROR: Invalid ID Token. Failed to decode: " + e.getMessage());
            return null;
        } catch (Exception e) {
            logger.log("ERROR: Unexpected error during ID token processing: " + e.getMessage());
            return null;
        }
    }

    private String getDomainFromEmail(String email, LambdaLogger logger) {
        if (email == null || !email.contains("@")) {
            logger.log("DEBUG: Cannot extract domain, email is null or missing '@': " + email);
            return null;
        }
        return email.substring(email.lastIndexOf("@") + 1);
    }

    private APIGatewayProxyResponseEvent createResponse(String body, int statusCode, LambdaLogger logger) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        // Add CORS headers if your Vue app is on a different domain/port than the API Gateway
        // during local development or if they are separate deployments.
        // For a private API Gateway accessed from within the same VPC/network, CORS might not be an issue,
        // but it's good to be aware of.
        // headers.put("Access-Control-Allow-Origin", "http://localhost:8080"); // Your Vue app's origin
        // headers.put("Access-Control-Allow-Methods", "POST, OPTIONS");
        // headers.put("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");

        logger.log("RESPONSE: statusCode=" + statusCode + ", body=" + body);
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(body);
    }
}
