package filters;

import org.bson.types.ObjectId;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Security;
import store.UserStore;
import utils.JwtUtil;
import utils.ResponseHelper;

import javax.inject.Inject;
import java.util.Optional;



public class JwtAuthorizationFilter extends Security.Authenticator {
    private final UserStore userStore;

    @Inject
    public JwtAuthorizationFilter(UserStore userStore)
    {
        this.userStore = userStore;
    }

    @Override
    public Optional<String> getUsername(Http.Request req) {
        String token = getTokenFromRequest(req);
        if (token == null)
            return Optional.empty();

        String userId = JwtUtil.verifyToken(token);
        if (userId == null)
            return Optional.empty();
        if (!ObjectId.isValid(userId))
            return Optional.empty();
        if (userStore.retrieve(new ObjectId(userId)).isEmpty())
            return Optional.empty();
        return Optional.ofNullable(userId);
    }

    @Override
    public Result onUnauthorized(Http.Request req) {
        return unauthorized(ResponseHelper.createResponse("Invalid credentials"));
    }

    private String getTokenFromRequest(Http.Request request) {
        Optional<String> authHeader = request.headers().get("Authorization");
        return authHeader.map(header -> {
            if (header.startsWith("Bearer "))
                return header.substring(7);
            return null;
        }).orElse(null);
    }
}