#!/usr/bin/env bash
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
check_dir="$project_dir/app/build/cva-check"
mkdir -p "$check_dir"
javac -d "$check_dir" \
  "$project_dir/app/src/main/java/com/google/mlkit/vision/demo/java/neckposture/CvaMath.java" \
  "$project_dir/app/src/test/java/com/google/mlkit/vision/demo/java/neckposture/CvaMathCheck.java"
java -cp "$check_dir" com.google.mlkit.vision.demo.java.neckposture.CvaMathCheck
