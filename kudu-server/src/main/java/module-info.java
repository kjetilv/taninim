/// @jenesis.main taninim.kudu.server.KuduServer
/// @jenesis.bom pin-jenesis.properties
/// @jenesis.bom pin-test.properties
/// @jenesis.release 25
module taninim.kudu.server {
    requires java.management;

    requires uplift.flogs;
    requires uplift.hash;
    requires uplift.kernel;
    requires uplift.s3;
    requires uplift.synchttp;
    requires uplift.util;

    requires taninim.kudu;
    requires taninim.server;
    requires taninim.taninim;

    requires jdk.incubator.vector;

    //noinspection JavaModuleDefinition
    opens taninim.kudu.server to org.graalvm.nativeimage.guest.staging;
}
