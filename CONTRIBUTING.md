# Contributor Guidelines

Thank you for your interest in contributing. To begin, please follow the steps below.

## Find an Issue

Please [open an issue](https://github.com/theiterators/kebs-intellij/issues/new) if you have found a bug, or you have any idea how to improve experience with [kebs](https://github.com/theiterators/kebs) in IntelliJ IDEA.

If you have no ideas what to contribute, you can check project's [issue tracker](https://github.com/theiterators/kebs-intellij/issues).

## Build and run the project

Fork the repository, clone project using git, open it in IntelliJ and import as sbt project.

Please use Java SDK 17. Project provides [sdkman](https://sdkman.io/) [env file](.sdkmanrc) so you can easily use
proper version with `sdk env` (see https://sdkman.io/usage#env).

Project uses [sbt-idea-plugin](https://github.com/JetBrains/sbt-idea-plugin) to define runtime configuration "kebs-intellij".
Using it you can run a new instance of IntelliJ with actual plugin code.

All features of the plugin are tested with unit tests. Please remember to write tests to the code you want to contribute.

## Code formatting

You can use sbt command to format the code:
```
sbt fmt
```

## Pull request

Once you have a code that closes an issue (does not matter if opened by you or someone else), put the changes on a branch
and [create a pull request](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/proposing-changes-to-your-work-with-pull-requests/creating-a-pull-request-from-a-fork).
Wait for a review and merge it into the main branch after approve (before that you might be requested for changes that
improve your contribution and make it follow  existing conventions in the code base).

If you get stuck, please consider opening a pull request with your incomplete work, and ask for help (please prefix
the pull request with `[WIP] `). In addition, you can put a comment on the original issue, pointing people to your own fork.
