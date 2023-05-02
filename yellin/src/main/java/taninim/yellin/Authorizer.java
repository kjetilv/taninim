package taninim.yellin;

import java.util.Optional;

import taninim.taninim.music.medias.UserAuth;
import taninim.taninim.music.medias.UserRequest;

public interface Authorizer {

    Optional<UserAuth> login(String userId, boolean createSession);

    Optional<UserAuth> authorize(UserRequest requested);

    Optional<UserAuth> deauthorize(UserRequest request);
}
