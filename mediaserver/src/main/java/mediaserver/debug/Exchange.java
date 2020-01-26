package mediaserver.debug;

import mediaserver.http.Handling;
import mediaserver.http.Req;
import mediaserver.sessions.AccessLevel;
import mediaserver.sessions.Session;
import mediaserver.util.Print;

@SuppressWarnings("unused")
public final class Exchange {

    private final long sequenceNo;

    private final String time;

    private final String handler;

    private final String sessionStatus;

    private final String request;

    private final String response;

    private final String session;

    private final String user;

    private final String accessLevel;

    private final String sessionShortId;

    private static final String UNSESSION = "unsession";

    private static final String UNKNOWN = "unknown";

    public Exchange(long sequenceNo, Handling handling) {

        this.sequenceNo = sequenceNo;

        this.time = String.valueOf(handling.getReq().getTime());
        this.handler = String.valueOf(handling.getHandler());

        Req req = handling.getReq();
        Session session = req.getSession();
        this.user = session == null ? UNKNOWN : session.getFbUser().getName();
        this.session = session == null ? UNSESSION : this.user + '/' + Print.uuid(session.getCookie()) + ": " + session;
        this.accessLevel = (session == null ? AccessLevel.NONE : session.getAccessLevel()).getDescription();
        this.sessionShortId = session == null ? UNKNOWN : Print.uuid(session.getCookie());
        this.request = String.valueOf(req.getRequest());
        this.response = String.valueOf(handling.getSentResponse());
        this.sessionStatus = session == null
            ? null
            : session.toString() + " status: " + session.getCurrentStatus(req.getTime());
    }

    public String getTime() {

        return time;
    }

    public String getHandler() {

        return handler;
    }

    public String getUser() {

        return user;
    }

    public String getAccessLevel() {

        return accessLevel;
    }

    public String getSessionShortId() {

        return sessionShortId;
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
