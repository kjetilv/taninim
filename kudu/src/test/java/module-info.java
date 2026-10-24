/// @jenesis.release 25
/// @jenesis.test
/// @jenesis.bom pin-test.properties
module taninim.kudu.test {
    requires org.junit.jupiter.api;
    requires org.assertj.core;

    requires taninim.kudu;

    opens taninim.kudu.test to org.junit.platform.commons;
}
