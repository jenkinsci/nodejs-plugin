Jenkins NodeJS Plugin
=====================


Configure plugin via Groovy script
---------
Either automatically upon [Jenkins post-initialization](https://wiki.jenkins.io/display/JENKINS/Post-initialization+script) or through [Jenkins script console](https://wiki.jenkins.io/display/JENKINS/Jenkins+Script+Console), example:

```groovy
#!/usr/bin/env groovy
import hudson.tools.InstallSourceProperty
import jenkins.model.Jenkins
import jenkins.plugins.nodejs.tools.NodeJSInstallation
import jenkins.plugins.nodejs.tools.NodeJSInstaller
import static jenkins.plugins.nodejs.tools.NodeJSInstaller.DEFAULT_NPM_PACKAGES_REFRESH_HOURS

final versions = [
        'NodeJS 8.x': '8.16.1'
]

Jenkins.instance.getDescriptor(NodeJSInstallation).with {
    installations = versions.collect {
        new NodeJSInstallation(it.key, null, [
                new InstallSourceProperty([
                        new NodeJSInstaller(it.value, null, DEFAULT_NPM_PACKAGES_REFRESH_HOURS)
                ])
        ])
    }  as NodeJSInstallation[]
}
```
