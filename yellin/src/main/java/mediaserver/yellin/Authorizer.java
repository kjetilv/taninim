package mediaserver.yellin;

import java.util.Optional;

import mediaserver.taninim.music.medias.UserAuth;
import mediaserver.taninim.music.medias.UserRequest;

public interface Authorizer {

    Optional<UserAuth> login(String userId, boolean createSession);

    Optional<UserAuth> authorize(UserRequest requested);

    Optional<UserAuth> deauthorize(UserRequest request);
}
