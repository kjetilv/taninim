package taninim.lambdatest;

import module taninim.yellin;
import module uplift.json;
import module uplift.uuid;

final class Parse {

    private Parse() {

    }

    static Lambdas2Test.LeasesActivation leasesActivation(String body) {
        var read = ACTIVATION_READER.read(body);
        return new Lambdas2Test.LeasesActivation(
            read.name(),
            read.userId(),
            read.token().digest(),
            read.trackUUIDs().stream().map(Uuid::digest).toList(),
            read.expiry().toEpochMilli()
        );
    }

    static Lambdas2Test.AuthResponse authResponse(String body) {
        var read = ACTIVATION_READER.read(body);
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
