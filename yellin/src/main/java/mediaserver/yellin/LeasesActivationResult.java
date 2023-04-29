package mediaserver.yellin;

import mediaserver.taninim.music.LeasesPath;

public record LeasesActivationResult(
    LeasesActivation leasesActivation,
    LeasesPath leasesPath
) {

}
