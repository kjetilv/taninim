package mediaserver.debug;

import mediaserver.http.Handling;
import mediaserver.http.WebPath;
import mediaserver.sessions.Session;

@SuppressWarnings("unused")
public final class Exchange {

    private final long sequenceNo;

    private final String time;

    private final String handler;

    private final String sessionStatus;

    private final String request;

    private final String response;

    private final String session;

    public Exchange(long sequenceNo, Handling handling) {

        this.sequenceNo = sequenceNo;

        this.time = String.valueOf(handling.getWebPath().getTime());
        this.handler = String.valueOf(handling.getHandler());

        WebPath webPath = handling.getWebPath();
        Session session = webPath.getSession();

        this.session = session == null ? null : session.toString();
        this.request = String.valueOf(webPath.getRequest());
        this.response = String.valueOf(handling.getSentResponse());
        this.sessionStatus = session == null
            ? null
            : session.toString() + " status: " + session.getCurrentStatus(webPath.getTime());
    }

    public String getTime() {

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
