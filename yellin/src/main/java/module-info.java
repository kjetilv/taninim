module taninim.yellin {

    requires java.compiler;
    requires taninim.taninim;
    requires taninim.fb;

    requires uplift.flogs;
    requires uplift.hash;
    requires uplift.json;
    requires uplift.json.anno;
    requires uplift.json.gen;
    requires uplift.json.mame;
    requires uplift.kernel;
    requires uplift.lambda;
    requires uplift.s3;
    requires uplift.util;
    requires uplift.uuid;

    exports taninim.yellin;
}