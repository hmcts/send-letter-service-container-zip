package uk.gov.hmcts.reform.sendletter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import uk.gov.hmcts.reform.sendletter.exceptions.ServiceConfigNotFoundException;

import java.util.List;

@ConfigurationProperties("accesstoken")
public class AccessTokenProperties {
    public List<TokenConfig> getServiceConfig() {
        return serviceConfig;
    }

    public void setServiceConfig(List<TokenConfig> serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    private List<TokenConfig> serviceConfig;

    public String getContainerForGivenType(String containerType) {
        return getServiceConfig().stream()
                .filter(tokenConfig -> tokenConfig.getContainerType().equals(containerType))
                .map(AccessTokenProperties.TokenConfig::getContainerName)
                .findFirst()
                .orElseThrow(() ->
                        new ServiceConfigNotFoundException(
                        "No service configuration found for container " + containerType));
    }

    public static class TokenConfig {
        private int validity;
        private String containerType;
        private String containerName;

        public int getValidity() {
            return validity;
        }

        public void setValidity(int validity) {
            this.validity = validity;
        }

        public String getContainerType() {
            return containerType;
        }

        public void setContainerType(String containerType) {
            this.containerType = containerType;
        }

        public String getContainerName() {
            return containerName;
        }

        public void setContainerName(String containerName) {
            this.containerName = containerName;
        }
    }
}
