"""Pull the final message, skill-load evidence and token counts out of one run's events.jsonl."""
import json
import os
import sys

run = sys.argv[1]
texts, tools, loaded, tokens = [], 0, False, {}
step_texts = []
for line in open(os.path.join(run, "events.jsonl")):
    try:
        event = json.loads(line)
    except ValueError:
        continue
    if event.get("type") == "result":
        texts.append(event.get("result") or "")
        tokens = {"output": (event.get("usage") or {}).get("output_tokens"),
                  "input": (event.get("usage") or {}).get("input_tokens"),
                  "cost_usd": event.get("total_cost_usd"), "turns": event.get("num_turns")}
    if event.get("type") == "assistant":
        for block in event.get("message", {}).get("content", []):
            if block.get("type") == "tool_use":
                tools += 1
                if "metis" in json.dumps(block.get("input", {})):
                    loaded = True
    part = event.get("part", {})
    if part.get("type") == "step-start":
        step_texts = []
    if part.get("type") == "text" and part.get("text"):
        step_texts.append(part["text"])
        texts = ["\n\n".join(step_texts)]
    if part.get("type") == "tool":
        tools += 1
        if part.get("tool") == "skill" or "skills/metis" in json.dumps(part.get("state", {}).get("input", {})):
            loaded = True
    if part.get("tokens"):
        for key, value in part["tokens"].items():
            if isinstance(value, dict):
                for sub, n in value.items():
                    tokens[f"{key}_{sub}"] = tokens.get(f"{key}_{sub}", 0) + n
            elif isinstance(value, (int, float)):
                tokens[key] = tokens.get(key, 0) + value

open(os.path.join(run, "final.md"), "w").write(texts[-1] if texts else "")
json.dump({"loaded": loaded, "tool_calls": tools, "tokens": tokens}, open(os.path.join(run, "meta.json"), "w"))
