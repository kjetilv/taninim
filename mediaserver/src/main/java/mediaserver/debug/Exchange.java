package mediaserver.debug;

import mediaserver.http.Handling;
import mediaserver.sessions.Session;

import java.time.Instant;

@SuppressWarnings("unused")
public final class Exchange {

    private final Instant time;

    private final long sequenceNo;

    private final String handler;

    private final String sessionStatus;

    private final String request;

    private final String response;

    private final String session;

    public Exchange(Instant time, long sequenceNo, Handling handling) {

        this.time = time;
        this.sequenceNo = sequenceNo;
        this.handler = String.valueOf(handling.getHandler());

        Session session = handling.getWebPath() == null ? null : handling.getWebPath().getSession();
        this.session = session == null ? null : session.toString();
        this.request = handling.getWebPath() == null ? null : String.valueOf(handling.getWebPath().getRequest());
        this.response = String.valueOf(handling.getSentResponse());
        this.sessionStatus = session == null ? null :  session.toString() + " status: " + session.getCurrentStatus();
    }

    public Instant getTime() {

        return time;
    }

    public String getHandler() {

        return handler;
    }

    public String getSession() {

        return session;
    }

    public String getSessionStatus() {

        return sessionStatus;
    }

    public String getRequest() {

        return request;
    }

    public String getResponse() {

        return response;
    }

    public long getSequenceNo() {

        return sequenceNo;
    }

    @Override
    public int hashCode() {

        return (int) sequenceNo;
    }

    @Override
    public boolean equals(Object o) {

        return this == o || o instanceof Exchange && sequenceNo == ((Exchange) o).sequenceNo;
    }
}
