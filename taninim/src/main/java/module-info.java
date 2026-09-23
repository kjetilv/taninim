module taninim.taninim {
    requires java.management;
    requires uplift.flogs;
    requires uplift.hash;
    requires uplift.kernel;
    requires uplift.s3;
    requires uplift.synchttp;
    requires uplift.util;

    exports taninim;
    exports taninim.auth;
    exports taninim.music;
    exports taninim.music.aural;
    exports taninim.music.legal;
    exports taninim.music.medias;
    exports taninim.util;
}
