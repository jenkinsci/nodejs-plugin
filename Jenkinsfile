#!/usr/bin/env groovy

// see https://github.com/jenkins-infra/pipeline-library
buildPlugin(useContainerAgent: true, configurations: [
  [platform: 'linux', jdk: 17],
  [platform: 'windows', jdk: 11],
])
