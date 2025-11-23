package taninim.lambdatest;

import module java.base;
import com.github.kjetilv.uplift.json.JsonReader;
import taninim.yellin.LeasesActivation;
import taninim.yellin.LeasesActivationRW;
final class Parse {

    private Parse() {

    }

    static Lambdas2Test.LeasesActivation leasesActivation(String body) {
        var read = ACTIVATION_READER.read(body);
        return new Lambdas2Test.LeasesActivation(
            read.name(),
            read.userId(),
            read.token(),
            read.trackUUIDs(),
            read.expiry().toEpochMilli()
        );
    }

    static Lambdas2Test.AuthResponse authResponse(String body) {
        var read = ACTIVATION_READER.read(body);
        return new Lambdas2Test.AuthResponse(
            read.name(),
            read.userId(),
            read.token(),
            read.trackUUIDs(),
            read.expiry().getEpochSecond()
        );
    }

    private static final JsonReader<String, LeasesActivation> ACTIVATION_READER =
        LeasesActivationRW.INSTANCE.stringReader();
}
