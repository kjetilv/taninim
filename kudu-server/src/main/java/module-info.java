/// @jenesis.main taninim.kudu.server.KuduServer
/// @jenesis.release 25
module taninim.kudu.server {
    requires java.management;
    requires taninim.kudu;
    requires taninim.server;
    requires taninim.taninim;
    requires uplift.flogs;
    requires uplift.hash;
    requires uplift.kernel;
    requires uplift.s3;
    requires uplift.synchttp;
    requires uplift.util;

    requires jdk.incubator.vector;

    //noinspection JavaModuleDefinition
    opens taninim.kudu.server to org.graalvm.nativeimage.guest.staging;
}
