package taninim.yellin;

import java.util.function.Supplier;

import taninim.taninim.music.medias.UserAuths;

import static java.util.Objects.requireNonNull;

final class DefaultGatekeeper {

    private final Supplier<UserAuths> authIds;

    private final Authorizer authorizer;

    DefaultGatekeeper(Supplier<UserAuths> authIds, Authorizer authorizer) {
        this.authIds = requireNonNull(authIds, "authIds");
        this.authorizer = requireNonNull(authorizer, "authorizer");
    }
}
