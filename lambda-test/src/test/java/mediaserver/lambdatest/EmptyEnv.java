package mediaserver.lambdatest;

import java.net.URI;

import com.github.kjetilv.uplift.kernel.Env;

@SuppressWarnings("NewExceptionWithoutArguments")
public class EmptyEnv implements Env {

    @Override
    public URI awsLambdaUri() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String awsLambdaRuntimeApi() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String accessKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String secretKey() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String sessionToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String authorizationToken() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String credentialsFullUri() {
        throw new UnsupportedOperationException();
    }
}
