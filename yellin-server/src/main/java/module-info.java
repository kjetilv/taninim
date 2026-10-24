/// @jenesis.main taninim.yellin.server.YellinServer
/// @jenesis.bom pin-jenesis.properties
/// @jenesis.bom pin-test.properties
/// @jenesis.release 25
module taninim.yellin.server {
    requires java.management;

    requires jdk.incubator.vector;

    requires taninim.server;
    requires taninim.fb;
    requires taninim.taninim;
    requires taninim.yellin;

    requires uplift.flogs;
    requires uplift.json;
    requires uplift.json.gen;
    requires uplift.kernel;
    requires uplift.s3;
    requires uplift.synchttp;
    requires uplift.util;

    //noinspection JavaModuleDefinition
    opens taninim.yellin.server to org.graalvm.nativeimage.guest.staging;
}
