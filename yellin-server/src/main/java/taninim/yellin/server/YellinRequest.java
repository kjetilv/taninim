package taninim.yellin.server;

import module java.base;
import taninim.fb.ExtAuthResponse;
import taninim.yellin.LeasesRequest;

sealed interface YellinRequest {

    YellinRequest PREFLIGHT_REQ = new Preflight();

    YellinRequest HEALTH_REQ = new Health();

    record Preflight() implements YellinRequest {
    }

    record Health() implements YellinRequest {
    }

    record Lease(LeasesRequest request) implements YellinRequest {
    }

    record Auth(ExtAuthResponse response) implements YellinRequest {
    }

    record Unknown() implements YellinRequest {
    }
}
