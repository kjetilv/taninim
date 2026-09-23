module taninim.lambdatest.test {

    requires java.net.http;

    requires taninim.taninim;
    requires taninim.fb;
    requires taninim.kudu;
    requires taninim.yellin;
    requires uplift.flambda;
    requires uplift.flogs;
    requires uplift.hash;
    requires uplift.kernel;
    requires uplift.json;
    requires uplift.json.anno;
    requires uplift.json.gen;
    requires uplift.lambda;
    requires uplift.s3;
    requires uplift.synchttp;
    requires uplift.util;
    requires taninim.lambdatest;

    requires org.junit.jupiter;
    requires org.junit.jupiter.api;
    requires org.assertj.core;

    opens taninim.lambdatest.test;
}
