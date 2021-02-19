# Contributor Guidelines

Thank you for your interest in contributing. To begin, please follow the steps below.

## Find an Issue

Please [open an issue](https://github.com/theiterators/kebs-intellij/issues/new) if you have found a bug, or you have any idea how to improve experience with [kebs](https://github.com/theiterators/kebs) in IntelliJ IDEA.

if you have no ideas about what to contribute, you can check project's [issue tracker](https://github.com/theiterators/kebs-intellij/issues).

## Build and run the project

Clone project using git. Once cloned, open it in IntelliJ and import as sbt project.

Project uses [sbt-idea-plugin](https://github.com/JetBrains/sbt-idea-plugin) to define runtime configuration "kebs-intellij".
Using it you can run a new instance of IntelliJ with actual plugin code.

All features of the plugin are tested with unit tests. Please remember to write tests to the code you want to contribute.

## Code formatting

You can use sbt command to format the code:
```
sbt fmt
```

## Merge request

Once you have a code that closes an issue (does not matter if opened by you or someone else) you can
[create a merge request](https://github.com/theiterators/kebs-intellij/compare) and wait for a review and merge
into the main branch (before that you might be requested for changes that improve your contribution and make it follow
existing conventions in the code base).

If you get stuck, please consider opening a pull request for your incomplete work, and asking for help (just prefix
the pull request by *WIP*). In addition, you can comment on the original issue, pointing people to your own fork.
