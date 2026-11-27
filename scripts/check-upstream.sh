#!/usr/bin/env bash
set -euo pipefail
git fetch --no-tags origin feature/aas-v0.5
git checkout --detach origin/feature/aas-v0.5
git config user.name 'Aurora AAS integration check'
git config user.email 'integration-check@users.noreply.github.com'
git fetch --no-tags https://gitlab.com/AuroraOSS/AuroraStore.git master
official=$(git rev-parse FETCH_HEAD)
git fetch --no-tags https://github.com/AuroraOSS/AuroraStore.git master
mirror=$(git rev-parse FETCH_HEAD)
base=$(git merge-base HEAD "$official")
read -r ahead behind < <(git rev-list --left-right --count HEAD..."$official")
{
  echo '## Aurora AAS upstream integration'
  echo
  echo "- Official GitLab: $official"
  echo "- GitHub mirror: $mirror"
  echo "- Common ancestor: $base"
  echo "- Fork-only commits: $ahead"
  echo "- Upstream-only commits: $behind"
  echo
  echo '### Official stable and prerelease tags (most recent version order)'
  echo '~~~'
  git ls-remote --tags --refs https://gitlab.com/AuroraOSS/AuroraStore.git | cut -f2 | sort -V | tail -15
  echo '~~~'
  echo '### gplayapi dependency'
  echo '~~~'
  git show HEAD:gradle/libs.versions.toml | grep -i gplayapi || true
  git show "$official":gradle/libs.versions.toml | grep -i gplayapi || true
  echo '~~~'
} >> "$GITHUB_STEP_SUMMARY"
if [[ "$behind" == 0 ]]; then
  echo 'Already integrated; no merge needed.' >> "$GITHUB_STEP_SUMMARY"
  exit 0
fi
if ! git merge --no-commit --no-ff "$official"; then
  echo '### Merge conflicts' >> "$GITHUB_STEP_SUMMARY"
  git diff --name-only --diff-filter=U >> "$GITHUB_STEP_SUMMARY"
  git merge --abort || true
  exit 1
fi
echo 'Merge succeeded in disposable checkout. No branch or release was changed.' >> "$GITHUB_STEP_SUMMARY"
chmod +x gradlew
./gradlew :app:testVanillaDebugUnitTest :app:assembleVanillaRelease --no-daemon --stacktrace
