#!/usr/bin/env bash
#
# Copyright © 2026 anyilanxin zxh(anyilanxin@aliyun.com)
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Affero General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
# One-shot offline Docker image build for kunpeng.
#
# Usage:
#   ./docs/build-offline-image.sh /path/to/base-jre.tar
#
# The single argument is a base image tar exported with:
#   docker save <base-image> -o base-jre.tar
# (base image reference: buildSrc/common-config.gradle)
#
# Steps performed:
#   1. ./gradlew :dist:jibBuildLocalTar -PbaseImageTar=<abs path>
#      (no registry access; the build itself does not require docker)
#   2. docker load -i dist/build/jib-image.tar   (requires a local docker daemon)
#
# Result: local images kunpeng:<version> and kunpeng:latest; the image tar
# stays at dist/build/jib-image.tar for transfer to offline hosts.
set -euo pipefail

usage() {
    echo "Usage: $0 <base-image-tar>" >&2
    echo "  <base-image-tar>  tar exported with: docker save <base-image> -o base-jre.tar" >&2
    echo "  (base image reference: buildSrc/common-config.gradle)" >&2
}

if [ $# -ne 1 ]; then
    echo "ERROR: exactly one argument required: path to the base image tar." >&2
    usage
    exit 1
fi

if [ ! -f "$1" ]; then
    echo "ERROR: base image tar not found: $1" >&2
    exit 1
fi

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_ROOT=$(cd "$SCRIPT_DIR/.." && pwd)
BASE_IMAGE_TAR=$(cd "$(dirname "$1")" && pwd)/$(basename "$1")

echo "==> Base image tar: $BASE_IMAGE_TAR"
echo "==> Building offline image tar (no registry access)"
(cd "$REPO_ROOT" && ./gradlew :dist:jibBuildLocalTar -PbaseImageTar="$BASE_IMAGE_TAR")

IMAGE_TAR="$REPO_ROOT/dist/build/jib-image.tar"
echo "==> Importing $IMAGE_TAR into docker"
docker load -i "$IMAGE_TAR"

VERSION=$(grep -E '^version=' "$REPO_ROOT/gradle.properties" | head -1 | cut -d= -f2)
echo ""
echo "Done."
echo "  image tags : kunpeng:$VERSION, kunpeng:latest"
echo "  image tar  : $IMAGE_TAR (copy to offline hosts and 'docker load -i' there)"
