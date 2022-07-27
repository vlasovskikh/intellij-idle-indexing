#!/usr/bin/env sh

MODEL_FILE="./src/main/kotlin/com/jetbrains/inin/InInModel.kt"
DOT_FILE="./media/fsm.dot"
SVG_FILE="./media/fsm.svg"

cd "$(dirname "$0")/.." || exit

cat > "$DOT_FILE" << EOF
digraph {
  node [shape = doublecircle]; init;
  node [shape = rect, style=rounded];

  init -> active_smart;
EOF
sed -n 's/[[:space:]]*\(.*\) to Input\.\(.*\) -> \(.*\)/  \1 -> \3 [label="\2"];/gp' "$MODEL_FILE" \
    | tr "[:upper:]" "[:lower:]" \
    >> "$DOT_FILE"
echo "}" >> "$DOT_FILE"
dot -T svg < "$DOT_FILE" > "$SVG_FILE"
