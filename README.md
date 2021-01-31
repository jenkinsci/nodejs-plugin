# NodeJS Plugin for Jenkins


[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/nodejs.svg)](https://plugins.jenkins.io/nodejs)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/nodejs-plugin.svg?label=release)](https://github.com/jenkinsci/nodejs-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/nodejs.svg?color=blue)](https://plugins.jenkins.io/nodejs)

Provides Jenkins integration for NodeJS & npm packages.

## Download & Installation

You can download the [latest
.hpi](http://updates.jenkins-ci.org/latest/nodejs.hpi) and install it
from the Manage Plugins menu, or install this plugin directly from the
Plugins Update Center.

## Main features

-   Provides NodeJS auto-installer, allowing to create as many NodeJS
    installations "profiles" as you want.  
    The auto-installer will automatically install a given version of
    NodeJS, on every jenkins agent where it will be needed
-   Allows to install globally some npm packages inside each
    installations, these npm packages will be made available to the PATH
-   Allows to execute some NodeJS script, under a given NodeJS
    installation
-   Allows use custom NPM user configuration file defined with
    config-file-provider plugin to setup custom NPM settings
-   Add a lightweight support to DSL pipeline
-   Force 32bit architecture
-   Relocate npm cache folder using pre defined streategies
-   Allow use of a mirror repo for downloading and installing NodeJS.

## Usage

1.  After installing the plugin, go to the global jenkins configuration
    panel (JENKINS\_URL/configure or JENKINS\_URL/configureTools if
    using jenkins 2),  
    and add new NodeJS installations.
    - If you wish to install NodeJS from a nodejs.org mirror, 
    select the "Install from nodejs.org mirror" option, where you can 
    then enter a mirror URL and then install NodeJS just like you would 
    from nodejs.org.
2.  For every Nodejs installation, you can choose to install some global
    npm packages.  
    Since 1.2.6 you could force the installation of the 32bit package
    for the underlying architecture if supported. If the package is not
    available the build will fail.

    *Note that you might provide npm package's version (with syntax
    "package@0.1.2" for instance, or maybe better, "package@\~0.1.0") in
    order to enforce*  
    *reproductibility of your npm execution environnment (the \~ syntax
    allows to benefits from bugfixes without taking the risk of a major
    version upgrade)*  
    See below:  
    ![](docs/images/image2018-3-31_16:40:29.png)

3.  Now, go to a job configuration screen, you will have 2 new items :
    -   On the "Build environnment" section, you will be able to pick
        one of the NodeJS installations to provide its bin/ folder to
        the PATH.  
        This way, during shell build scripts, you will have some npm
        executables available to the command line (like bower or
        grunt)  
        See below:  
        ![](docs/images/nodejs_npm_to_path.png)
    -   On the "Build" section, you will be able to add a "Execute
        NodeJS script" build step  
        ![](docs/images/nodejs_buildstep_menu.png)  
        This way, you will be able to fill a textarea with the script
        content you want to execute.  
        Note that you will have to select a NodeJS runtime you
        previously installed, to specify the NodeJS version you want to
        use  
        during your NodeJS script execution.  
        ![](docs/images/nodejs_buildstep_script.png)
4.  You can customise any [NPM
    settings](https://docs.npmjs.com/misc/config#config-settings) you
    need creating a NPM config file where you can also setup multiple
    npm registry (scoped or public)  
    and select for each one stored credential (only user/password
    supported type) as follow:  
    ![](docs/images/nodejs_npm_configfile.png)  
    and than select the config file to use for each configured build
    step  
    ![](docs/images/nodejs_choose_configfile.png)
5.  You would relocate the npm cache folder to swipe out it when a job
    is removed or workspace folder is deleted. There are three default
    strategy:
    -   per node, that is the default NPM behavour. All download package
        are placed in the \~/.npm on Unix system or
        %APP\_DATA%\\npm-cache on Windows system;
    -   per executor, where each executor has an own NPM cache folder
        placed in \~/npm-cache/$executorNumber;
    -   per job, placed in the workspace folder under
        $WORKSPACE/npm-cache. This cache will be swipe out together the
        workspace and will be removed when the job is deleted.

## Pipeline

The current supported DSL steps are:

-   nodejs (as buildwrapper)
-   tools

In a Declarative pipeline you can add any configured NodeJS tool to your
job, and it will enhance  
the PATH variable with the selected NodeJS installation folder, instead
in scripted pipeline you have to do it manually.

***Example of use tools in Jenkinsfile (Scripted Pipeline)***

``` groovy
node {
    env.NODEJS_HOME = "${tool 'Node 6.x'}"
    // on linux / mac
    env.PATH="${env.NODEJS_HOME}/bin:${env.PATH}"
    // on windows
    env.PATH="${env.NODEJS_HOME};${env.PATH}"
    sh 'npm --version'
}
```

This example show the use of **buildwrapper**, where enhanced PATH will
be available only inside the brace block

***Example of the use of buildwrapper Jenkinsfile (Declarative
Pipeline)***

``` groovy
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                nodejs(nodeJSInstallationName: 'Node 6.x', configId: '<config-file-provider-id>') {
                    sh 'npm config ls'
                }
            }
        }
    }
}
```

## Configure plugin via Groovy script

Either automatically upon [Jenkins post-initialization](https://www.jenkins.io/doc/book/managing/groovy-hook-scripts/#post-initialization-script-init-hook)
or through [Jenkins script console](https://www.jenkins.io/doc/book/managing/script-console/), example:

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

## Known limitations / issues

NodeJS version 1.0 has adapted its code to the most recent Jenkins API
(1.6xx). If also EnvInject is installed you will fall in
[JENKINS-26583](https://issues.jenkins-ci.org/browse/JENKINS-26583)  
that corrupts setup of the nodejs installation bin folder into PATH
environment. In this case consider if update or not or use an own build
from  
[this
branch](https://github.com/jenkinsci/nodejs-plugin/tree/workaround-26583)
untill the JENKINS-26583 will not be fixed.

-   If you update from NodeJS 0.2.2 or earlier to newer version
    materializes a data migration. This data migration is transparent to
    the users but  
    you can not back to 0.2.2 without lost global configuration tools
    and job build step configuration.
-   NodeJS versions prior to 0.9.0 won't be available at the moment
    (directory structure was not the same as today on distribution
    site).  
    This might be handled in the future (this is exposed as
    [PathResolver
    implementation](https://github.com/jenkinsci/nodejs-plugin/blob/master/src/main/java/jenkins/plugins/nodejs/tools/pathresolvers/LatestInstallerPathResolver.java))
    :  
    don't hesitate to provide new implementations for previous versions
    and submit a PR on github.
-   Supported architecture are:
    -   Windows 32/64 bit
    -   Linux 32/64 bit
    -   OSX (intel) 64 bit
    -   Arm 6l/7l/64
    -   SunOS

## Releases Notes

**Please refer to [github repository page](https://github.com/jenkinsci/nodejs-plugin/releases)**
