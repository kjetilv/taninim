package taninim.yellin;

import module java.base;
import taninim.auth.Authed;
import taninim.music.medias.UserAuth;
import taninim.music.medias.UserRequest;

public interface Authorizer {

    Authed<UserAuth> login(String userId, boolean createSession);

    Authed<UserAuth> authorize(UserRequest requested);

    Authed<UserAuth> deauthorize(UserRequest request);
}
