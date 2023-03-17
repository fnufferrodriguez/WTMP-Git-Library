package usbr.git;

import java.net.URL;

public class WTMPGitProperties {

    public static final String IGNORE_SCHANNEL = "wtmp.ignoreSChannel";
    private static final String WTMP_IGNORE_URL_PROPERTY = "wtmp.%url%.ignore";

    private WTMPGitProperties() {
        super();
    }

    public static String getWtmpIgnoreUrlProperty(URL forURL) {
        return WTMP_IGNORE_URL_PROPERTY.replace("%url%", forURL.toString());
    }

}
