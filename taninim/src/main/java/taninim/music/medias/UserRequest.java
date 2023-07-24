package taninim.music.medias;

import java.util.Objects;

import com.github.kjetilv.uplift.kernel.uuid.Uuid;

import static java.util.Objects.requireNonNull;

public record UserRequest(
    String userId,
    Uuid token,
    Uuid albumId
) {

    public UserRequest(String userId, Uuid token, Uuid albumId) {
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
