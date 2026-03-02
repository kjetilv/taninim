package taninim.kudu;

import module java.base;
import com.github.kjetilv.uplift.hash.Hash;
import com.github.kjetilv.uplift.kernel.io.BytesIO;
import com.github.kjetilv.uplift.lambda.LambdaClientSettings;
import com.github.kjetilv.uplift.s3.S3AccessorFactory;
import taninim.TaninimSettings;
import taninim.auth.Authed;
import taninim.music.LeasesRegistry;
import taninim.music.aural.Chunk;
import taninim.music.legal.ArchivedLeasesRegistry;
import taninim.music.legal.CloudMediaLibrary;
import taninim.music.legal.S3Archives;
import taninim.music.medias.MediaLibrary;

import static com.github.kjetilv.uplift.hash.HashKind.K128;

public interface Kudu {

    static Kudu create(
        LambdaClientSettings clientSettings,
        TaninimSettings taninimSettings,
        S3AccessorFactory s3AccessorFactory
    ) {
        var s3Accessor = s3AccessorFactory.create();
        var time = clientSettings.time();
        return create(
            ArchivedLeasesRegistry.create(
                S3Archives.create(s3Accessor),
                taninimSettings.leaseDuration(),
                time
            ),
            CloudMediaLibrary.create(s3Accessor, time),
            taninimSettings.transferSize(),
            time
        );
    }

    static Kudu create(
        LeasesRegistry leasesRegistry,
        MediaLibrary mediaLibrary,
        int transferSize,
        Supplier<Instant> time
    ) {
        return new DefaultKudu(leasesRegistry, mediaLibrary, transferSize, time);
    }

    default Authed<byte[]> library(Hash<K128> token) {
        return libraryStream(token)
            .map(Library::stream)
            .map(BytesIO::readInputStream);
    }

    Authed<Library> libraryStream(Hash<K128> token);

    Authed<AudioBytes> audioBytes(TrackRange trackRange);

    record Library(long size, InputStream stream) {
    }

    record AudioStream(TrackRange trackRange, Chunk chunk, InputStream stream) {
    }

    record AudioBytes(TrackRange trackRange, Chunk chunk, byte[] bytes) {
    }
}
