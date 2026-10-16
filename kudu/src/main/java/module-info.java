/// @jenesis.main taninim.kudu.Main
/// @jenesis.release 25
module taninim.kudu {
    requires taninim.taninim;
    requires uplift.flogs;
    requires uplift.hash;
    requires uplift.kernel;
    requires uplift.lambda;
    requires uplift.s3;

    exports taninim.kudu;

    //noinspection JavaModuleDefinition
    opens taninim.kudu to org.graalvm.nativeimage.guest.staging;
}
