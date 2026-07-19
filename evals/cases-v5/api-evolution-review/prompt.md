# Notification Client — API Evolution PR Review

You are reviewing a PR to the company's internal `notifications` Python
library, used by ~40 backend services. The team is evolving the client
from a thin v1 wrapper into the "v2" public API below. The library's
whole value is that call sites read cleanly and the surface is small
enough to learn in one sitting; this is the interface ~40 teams will
import for years.

Report each finding as `file:line`, severity (blocking / should-fix /
nit), and one sentence (problem + fix). Order most severe first. Do not
rewrite the code. Do not pad: only findings you are confident in, no
invented problems.

## `notifications/client.py`

```python
  1  # notifications/client.py
  2  class NotificationClient:
  3      def __init__(self, api_key, base_url="https://api.notify.example",
  4                   timeout=30, max_retries=3, backoff=0.5, region=None,
  5                   sandbox=False, default_channel=None, on_error=None,
  6                   telemetry=None, _legacy_mode=False):
  7          self.api_key = api_key
  8          self.base_url = base_url
  9          self.timeout = timeout
 10          self.max_retries = max_retries
 11          self.backoff = backoff
 12          self.region = region
 13          self.sandbox = sandbox
 14          self.default_channel = default_channel
 15          self.on_error = on_error
 16          self.telemetry = telemetry
 17          self._legacy_mode = _legacy_mode
 18          self._session = None
 19
 20      def send(self, to, message, channel=None, template=None,
 21               template_vars=None, priority="normal", schedule_at=None,
 22               dedupe_key=None, metadata=None, _raw=None):
 23          chan = channel or self.default_channel
 24          if template and message:
 25              raise ValueError("pass message or template, not both")
 26          payload = {"to": to, "channel": chan, "priority": priority}
 27          if template:
 28              payload["template"] = template
 29              payload["vars"] = template_vars or {}
 30          else:
 31              payload["body"] = message
 32          if schedule_at:
 33              payload["schedule_at"] = schedule_at
 34          if dedupe_key:
 35              payload["dedupe_key"] = dedupe_key
 36          if _raw:
 37              payload.update(_raw)
 38          resp = self._post("/v2/send", payload)
 39          if resp.get("status") == "error" and self.on_error:
 40              self.on_error(resp)
 41          return resp
 42
 43      def send_batch(self, messages, channel=None, priority="normal",
 44                     template=None, template_vars=None, dedupe_key=None,
 45                     metadata=None, stop_on_error=False, _raw=None):
 46          results = []
 47          for m in messages:
 48              r = self.send(m["to"], m.get("message"), channel=channel,
 49                            template=template, template_vars=template_vars,
 50                            priority=priority, dedupe_key=dedupe_key)
 51              results.append(r)
 52              if stop_on_error and r.get("status") == "error":
 53                  break
 54          return results
 55
 56      def _post(self, path, payload):
 57          import json
 58          import urllib.request
 59          body = json.dumps(payload).encode()
 60          for attempt in range(self.max_retries):
 61              try:
 62                  req = urllib.request.Request(
 63                      self.base_url + path, data=body,
 64                      headers={"Authorization": "Bearer " + self.api_key,
 65                               "Content-Type": "application/json"})
 66                  with urllib.request.urlopen(req, timeout=self.timeout) as r:
 67                      return json.loads(r.read())
 68              except Exception as e:
 69                  if attempt == self.max_retries - 1:
 70                      return {"status": "error", "error": str(e)}
 71                  import time
 72                  time.sleep(self.backoff * (2 ** attempt))
```

Context:
- v1 shipped `send(to, message)` only; every other parameter here is new
  in v2. Backward compatibility with the two-arg call is required.
- `_raw` and `_legacy_mode` exist so two specific legacy services can be
  migrated; no other caller should touch them.
- The provider genuinely supports templates, scheduling, dedupe, and
  batch; those capabilities are real requirements, not speculation.
- There is exactly one provider and no concrete plan for a second.
- `on_error`, `telemetry`, and `metadata` were added preemptively; no
  current caller passes them.

Do not edit files. Return your review in Markdown.
