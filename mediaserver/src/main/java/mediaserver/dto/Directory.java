package mediaserver.dto;

public class Directory {

    private String name;

    private Directory[] subs;

    private AudioFile[] files;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AudioFile[] getFiles() {
        return files;
    }

    public void setFiles(AudioFile[] files) {
        this.files = files;
    }
}
