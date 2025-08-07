package taninim.lambdatest;

import com.github.kjetilv.uplift.json.JsonReader;
import com.github.kjetilv.uplift.uuid.Uuid;
import taninim.yellin.LeasesActivation;
import taninim.yellin.LeasesActivationRW;

final class Parse {

    private Parse() {

    }

    static Lambdas2Test.LeasesActivation leasesActivation(String body) {
        LeasesActivation read = ACTIVATION_READER.read(body);
        return new Lambdas2Test.LeasesActivation(
            read.name(),
            read.userId(),
            read.token().digest(),
            read.trackUUIDs().stream().map(Uuid::digest).toList(),
            read.expiry().toEpochMilli()
        );
    }

    static Lambdas2Test.AuthResponse authResponse(String body) {
        LeasesActivation read = ACTIVATION_READER.read(body);
        return new Lambdas2Test.AuthResponse(
            read.name(),
            read.userId(),
            read.token().digest(),
            read.trackUUIDs().stream().map(Uuid::digest).toList(),
            read.expiry().getEpochSecond()
        );
    }

    private static final JsonReader<String, LeasesActivation> ACTIVATION_READER =
        LeasesActivationRW.INSTANCE.stringReader();
}
