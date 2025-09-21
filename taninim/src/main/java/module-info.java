module taninim.taninim {
    requires uplift.flogs;
    requires uplift.kernel;
    requires uplift.s3;
    requires uplift.uuid;

    exports taninim;
    exports taninim.music;
    exports taninim.music.aural;
    exports taninim.music.legal;
    exports taninim.music.medias;
    exports taninim.util;
}