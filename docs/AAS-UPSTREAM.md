# Upstream integration

Official GitLab master is the primary source; the GitHub parent is checked as a mirror. The script measures both sides of divergence, lists version tags and compares gplayapi pins. It attempts a merge only in a disposable checkout and never pushes, signs or publishes a release. Conflicts fail the run. Successful merges run JVM tests and unsigned release compilation. Android Keystore tests remain in the dedicated release workflow and must also pass before release.

GitHub scheduled workflows run only from the default branch and may be disabled on forks. This workflow is staged on feature/aas-v0.5; enabling its schedule requires installing the workflow on master or running it through a separate scheduler. No automatic merge into a maintained branch is authorized by this workflow.

Known limits: tags are a discovery signal, not a classification of security fixes; gplayapi comparison checks the version declared by upstream, not every independent library release. An actual integration must be reviewed and tested before promotion.
