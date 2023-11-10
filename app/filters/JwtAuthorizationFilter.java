package filters;

import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import utils.JwtUtil;
import utils.ResponseHelper;

import java.util.Optional;



public class JwtAuthorizationFilter extends Security.Authenticator {

    @Override
    public Optional<String> getUsername(Http.Request req) {
        String userId = JwtUtil.verifyToken(getTokenFromRequest(req));
        return Optional.ofNullable(userId);
    }

    @Override
    public Result onUnauthorized(Http.Request req) {
        return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
    }

    private String getTokenFromRequest(Http.Request request) {
        String authHeader = request.headers().get("Authorization").get();
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}