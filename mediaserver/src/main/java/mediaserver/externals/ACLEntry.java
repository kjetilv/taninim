package mediaserver.externals;

import mediaserver.util.DAC;

@SuppressWarnings("InstanceVariableMayNotBeInitialized")
public class ACLEntry {

    private String als;

    private String lev;

    private String ser;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + als + "/" + lev + "]";
    }

    @DAC
    public String getAls() {
        return als;
    }

    @DAC
    public void setAls(String als) {
        this.als = als;
    }

    public String getLev() {
        return lev;
    }

    @DAC
    public void setLev(String lev) {
        this.lev = lev;
    }

    public String getSer() {
        return ser;
    }

    @DAC
    public void setSer(String ser) {
        this.ser = ser;
    }
}
