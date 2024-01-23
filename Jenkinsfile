#!/usr/bin/env groovy

// see https://github.com/jenkins-infra/pipeline-library
buildPlugin(
        forkCount: '1C',
        useContainerAgent: true,
        configurations: [
                [platform: 'linux', jdk: 21],
                [platform: 'windows', jdk: 17],
        ])
