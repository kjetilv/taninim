/// @jenesis.release 25
/// @jenesis.test
/// @jenesis.bom pin-test.properties
module taninim.taninim.test {
    requires org.junit.jupiter.api;
    requires org.assertj.core;
    requires taninim.taninim;

    opens taninim.music.test to org.junit.platform.commons;
}
