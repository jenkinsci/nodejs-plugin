/*
 * The MIT License
 *
 * Copyright (c) 2018, Nikolas Falco
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package jenkins.plugins.nodejs;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import org.jenkinsci.lib.configprovider.model.Config;
import org.jenkinsci.plugins.configfiles.ConfigFiles;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.plugins.nodejs.configfiles.NPMConfig;
import jenkins.plugins.nodejs.configfiles.NPMConfig.NPMConfigProvider;
import jenkins.plugins.nodejs.tools.NodeJSInstallation;
import jenkins.plugins.nodejs.configfiles.VerifyConfigProviderException;

/*package*/ final class NodeJSDescriptorUtils {

    private NodeJSDescriptorUtils() {
    }

    /**
     * Get all NPMConfig defined for the given context.
     *
     * @param context the context where lookup the config files
     * @return a collection of user npmrc files found for the given context
     *         always including a system default.
     */
    @NonNull
    public static ListBoxModel getConfigs(@Nullable ItemGroup<?> context) {
        ListBoxModel items = new ListBoxModel();
        items.add(Messages.NPMConfig_default(), "");
        for (Config config : ConfigFiles.getConfigsInContext(context, NPMConfigProvider.class)) {
            items.add(config.name, config.id);
        }
        return items;
    }

    /**
     * Verify that the given configId exists in the given context.
     *
     * @param context where lookup
     * @param configId the identifier of an npmrc file
     * @return an validation form for the given npmrc file identifier, otherwise
     *         returns {@code ok} if the identifier does not exists for the
     *         given context.
     */
    public static FormValidation checkConfig(@Nullable ItemGroup<?> context, @CheckForNull String configId) {
        if (configId != null) {
            Config config = ConfigFiles.getByIdOrNull(context, configId);
            if (config != null) {
                try {
                    ((NPMConfig) config).doVerify();
                } catch (VerifyConfigProviderException e) {
                    return FormValidation.error(e.getMessage());
                }
            }
        }
        return FormValidation.ok();
    }

    public static ListBoxModel getNodeJSInstallations(@Nullable Item item, boolean allowDefault) {
        ListBoxModel items = new ListBoxModel();
        if (item == null || !item.hasPermission(Item.CONFIGURE)) {
            return items;
        }
        if (allowDefault) {
            items.add(Messages.NodeJSInstallation_default(), "");
        }
        for (NodeJSInstallation tool : NodeJSUtils.getInstallations()) {
            items.add(tool.getName(), tool.getName());
        }
        return items;
    }

}