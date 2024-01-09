# Code coverage model 

[![Join the chat at Gitter/Matrix](https://badges.gitter.im/jenkinsci/code-coverage-api-plugin.svg)](https://gitter.im/jenkinsci/code-coverage-api-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge)
[![Jenkins](https://ci.jenkins.io/job/Plugins/job/coverage-model/job/main/badge/icon?subject=Jenkins%20CI)](https://ci.jenkins.io/job/Plugins/job/coverage-model/job/main/)
[![CI on all platforms](https://github.com/jenkinsci/coverage-model/workflows/GitHub%20CI/badge.svg)](https://github.com/jenkinsci/coverage-model/actions/workflows/ci.yml)
[![CodeQL](https://github.com/jenkinsci/coverage-model/workflows/CodeQL/badge.svg)](https://github.com/jenkinsci/coverage-model/actions/workflows/codeql.yml)
[![Line Coverage](https://raw.githubusercontent.com/jenkinsci/coverage-model/main/badges/line-coverage.svg)](https://app.codecov.io/gh/jenkinsci/coverage-model)
[![Branch Coverage](https://raw.githubusercontent.com/jenkinsci/coverage-model/main/badges/branch-coverage.svg)](https://app.codecov.io/gh/jenkinsci/coverage-model)
[![Mutation Coverage](https://raw.githubusercontent.com/jenkinsci/coverage-model/main/badges/mutation-coverage.svg)](https://github.com/jenkinsci/coverage-model/actions/workflows/quality-monitor.yml)

This library Provides a Java API to parse and collect code coverage results.
It is used by my [Jenkins' coverage plug-in](https://github.com/jenkinsci/coverage-plugin) to visualize
the coverage of individual builds.

![Jenkins Coverage Plug-in Overview](doc/jenkins-1.png)
![Jenkins Coverage Plug-in Files](doc/jenkins-2.png)

Additionally, this library is used by my additional [Quality Monitor GitHub Action](https://github.com/uhafner/quality-monitor), that monitors the quality of projects based on a configurable set of metrics and gives feedback on pull requests (or single commits) in GitHub.
There are also two additional actions available, to autograde student software projects based
on these metrics: [GitHub Autograding action](https://github.com/uhafner/autograding-github-action) and [GitLab Autograding action](https://github.com/uhafner/autograding-gitlab-action).

![Quality Monitor GitHub Action](doc/quality-monitor.png)

This library consists basically of two separate parts:

1. A model to manage several metrics in a software project. Supported metrics are code coverage (line, branch, instruction), mutation coverage, number of tests, lines of code, and cyclomatic complexity.
2. Parsers for several code coverage formats:
    * JaCoCo code coverage results
    * Cobertura code coverage results
    * PIT Mutation coverage results
    * JUnit test results

All source code is licensed under the MIT license. Contributions to this library are welcome! 
