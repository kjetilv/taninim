package taninim.music.medias;

import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.hash.HashKind.K128;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public record UserRequest(String userId, Hash<K128> token, Hash<K128> albumId) {

    public UserRequest(String userId, Hash<K128> token, Hash<K128> albumId) {
        this.userId = requireNonNull(userId, "userId");
        this.token = requireNonNull(token, "token");
        this.albumId = requireNonNull(albumId, "albumId");
        if (this.userId.isBlank()) {
            throw new IllegalStateException(this + ": No user id");
        }
    }

    boolean matches(UserAuth userAuth) {
        return Objects.equals(userAuth.userId(), this.userId) &&
               Objects.equals(userAuth.token(), this.token);
    }
}
