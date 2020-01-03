package mediaserver.externals;

@SuppressWarnings("unused")
public class ACLEntry {

    private String als;

    private String lev;

    private String ser;

    public String getAls() {

        return als;
    }

    public void setAls(String als) {

        this.als = als;
    }

    public String getLev() {

        return lev;
    }

    public void setLev(String lev) {

        this.lev = lev;
    }

    public String getSer() {

        return ser;
    }

    public void setSer(String ser) {

        this.ser = ser;
    }

    @Override
    public String toString() {

        return getClass().getSimpleName() + "[" + als + "/" + lev + "]";
    }
}
