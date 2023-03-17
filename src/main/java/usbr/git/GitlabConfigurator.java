/*
 * Copyright 2023 United States Bureau of Reclamation (USBR).
 * United States Department of the Interior
 * All Rights Reserved. USBR PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval
 * from USBR
 */

package usbr.git;

import com.google.common.flogger.FluentLogger;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import usbr.git.cli.GitCLI;
import usbr.git.cli.GitCLIUnavailableException;
import usbr.git.cli.GitConfig;
import usbr.git.cli.GitProperty;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;

public class GitlabConfigurator {

    private static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();

    private WTMPGitConfig _configuration;

    public GitlabConfigurator(WTMPGitConfig configuration) {
        _configuration = configuration;
    }

    public void configureGit() throws IOException, InterruptedException, GitCLIUnavailableException {
        LOGGER.atConfig().log("Configuring Git");
        if (!GitCLI.isGitExectuable()) {
            throw new GitCLIUnavailableException("Git CLI Tools unavailable or not in PATH!");
        }
        LOGGER.atConfig().log("Git CLI Present");

        Map<String, String> gitConfig = retrieveMaybeInitializeGitConfig();

        if(!mapContainsProperty(gitConfig, _configuration.getDoNotSetSChannelProperty())) {
            LOGGER.atConfig().log("Setting sslbackend property");
            ensurePropertySet(gitConfig, _configuration.getSSLBackendProperty());
        } else {
            LOGGER.atInfo().log("Skipping SSLBackend configuration.");
        }

        for (GitlabConfiguration gitlabConfig : _configuration.getGitlabConfigs()) {
            LOGGER.atConfig().log("Configuring Gitlab: %s", gitlabConfig.getUrl());
            if (!mapContainsProperty(gitConfig, gitlabConfig.getIgnoreProperty())) {
                ensurePropertySet(gitConfig, gitlabConfig.getClientIdProperty());
                ensurePropertySet(gitConfig, gitlabConfig.getClientSecretProperty());
                ensurePropertySet(gitConfig, gitlabConfig.getProviderProperty());
                ensurePropertySet(gitConfig, gitlabConfig.getAuthModesProperty());
            } else {
                LOGGER.atInfo().log("Skipping autoconfiguration for Gitlab instance: %s", gitlabConfig.getUrl());
            }
        }
    }

    /**
     * This will retrieve the Global git config for the system as a map.
     * If the config comes back as blank (typically the case on a brand new Git install),
     * it will set WTMPGITProperties.IGNORE_SCHANNEL to 'false' in the global config.
     *
     * If that fails, it will log a message since we may be unable to modify the Git global config for some reason.
     *
     * @return A map of the system git properties (typically, but not always, containing at least one element)
     */
    private Map<String, String> retrieveMaybeInitializeGitConfig() throws IOException, InterruptedException {
        Map<String, String> gitConfig = GitConfig.listGlobalGitConfig();
        LOGGER.atConfig().log("Git Config: %s", gitConfig);

        if(!gitConfig.containsKey(WTMPGitProperties.IGNORE_SCHANNEL.toLowerCase())) {
            LOGGER.atConfig().log("Setting %s to default false", WTMPGitProperties.IGNORE_SCHANNEL);
            ensurePropertySet(gitConfig, new GitProperty(WTMPGitProperties.IGNORE_SCHANNEL, "false"));
            gitConfig = GitConfig.listGlobalGitConfig();
            LOGGER.atConfig().log("Updated Git Config: %s", gitConfig);
        }

        if(!gitConfig.containsKey(WTMPGitProperties.IGNORE_SCHANNEL.toLowerCase())) {
            LOGGER.atWarning().log("Failed to update git global config! Git autoconfiguration may not work. Trying to continue.");
        }
        return gitConfig;
    }

    private void ensurePropertySet(Map<String, String> gitGlobalConfig, GitProperty property) throws IOException, InterruptedException {
        if (!mapContainsProperty(gitGlobalConfig, property)) {
            GitConfig.setGlobalConfigProperty(property);
        }
    }

    private boolean mapContainsProperty(Map<String, String> map, GitProperty property) {
        // git config -l lowercases keys
        return Objects.equals(map.get(property.getKey().toLowerCase()), property.getValue());
    }

    WTMPGitConfig getConfiguration() {
        return _configuration;
    }

    public static GitlabConfigurator prepareFromConfigurationFile(URL configurationFile) throws IOException, JDOMException, XMLParseException {
        Document document = new SAXBuilder().build(configurationFile);
        WTMPGitConfig wtmpGitConfig = WTMPGitConfig.fromXML(document.getRootElement());
        return new GitlabConfigurator(wtmpGitConfig);
    }
}
