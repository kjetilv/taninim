package taninim.yellin;

import taninim.music.medias.UserAuth;
import taninim.music.medias.UserRequest;

import java.util.Optional;

public interface Authorizer {

    Optional<UserAuth> login(String userId, boolean createSession);

    Optional<UserAuth> authorize(UserRequest requested);

    Optional<UserAuth> deauthorize(UserRequest request);
}
