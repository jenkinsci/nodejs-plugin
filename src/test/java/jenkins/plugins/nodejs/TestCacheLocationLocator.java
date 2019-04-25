package jenkins.plugins.nodejs;

import hudson.Extension;
import hudson.FilePath;
import jenkins.plugins.nodejs.cache.CacheLocationLocator;
import jenkins.plugins.nodejs.cache.CacheLocationLocatorDescriptor;

public class TestCacheLocationLocator extends CacheLocationLocator {

    private FilePath location;

    public TestCacheLocationLocator(FilePath location) {
        this.location = location;
    }

    @Override
    public FilePath locate(FilePath workspace) {
        return location;
    }

    @Extension
    public static class DescriptorImpl extends CacheLocationLocatorDescriptor {
        @Override
        public String getDisplayName() {
            return "test cache locator";
        }
    }
}
