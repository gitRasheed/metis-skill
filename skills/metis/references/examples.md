# Metis examples

Load this file when the core skill is clear but concrete examples would help apply it. Do not load it for every small edit.

These examples are patterns, not templates. Keep the local language, framework, and repo style unless they conflict with the underlying behavior.

## Contents

1. Call-site-first API shape
2. Plain data plus systems
3. Mutually exclusive states vs composable features
4. I/O at the edge, pure logic inside
5. Push ifs up, fors down
6. Boundary validation and replay safety
7. Behavior-first tests
8. SOLID without ceremony
9. Comment discipline
10. Final cleanup

## 1. Call-site-first API shape

Do not build helpers first and let their argument lists leak upward. Sketch the caller you wish existed, then make helpers satisfy that shape.

Avoid:

```ts
async function sendDigestBatch(
  db: Pool,
  userTable: string,
  postsTable: string,
  templateId: string,
  batchSize: number,
  dryRun: boolean,
) {
  // Implementation details shape the public workflow.
}

await sendDigestBatch(db, "users", "posts", "weekly_digest_v4", 500, false);
```

Prefer:

```ts
const result = await publishWeeklyDigest({
  audience: await loadDigestAudience(now),
  issue: buildDigestIssue(recentPosts),
  delivery: emailDelivery,
  now,
});
```

```ts
async function publishWeeklyDigest(input: {
  audience: Subscriber[];
  issue: DigestIssue;
  delivery: DigestDelivery;
  now: Date;
}): Promise<DigestResult> {
  const messages = planDigestMessages(input.audience, input.issue, input.now);
  const ready = messages.filter((message) => message.shouldSend);
  return input.delivery.sendBatch(ready);
}
```

Why this is better:

- the top-level workflow reads as the product behavior
- helper names describe steps, not storage internals
- the delivery provider can change without reshaping the caller

## 2. Plain data plus systems

When behavior combines across many axes, avoid making every combination a class.

Avoid:

```python
class FeatureFlag:
    def enabled_for(self, user): ...

class RegionFeatureFlag(FeatureFlag):
    def enabled_for(self, user): ...

class BetaRegionFeatureFlag(RegionFeatureFlag):
    def enabled_for(self, user): ...

class StaffOnlyBetaRegionFeatureFlag(BetaRegionFeatureFlag):
    def enabled_for(self, user): ...
```

Prefer:

```python
from dataclasses import dataclass
from datetime import datetime


@dataclass(frozen=True)
class FlagRule:
    key: str
    allowed_regions: set[str]
    beta_only: bool
    staff_only: bool
    starts_at: datetime | None


@dataclass(frozen=True)
class UserContext:
    user_id: str
    region: str
    is_beta: bool
    is_staff: bool


def evaluate_flag(rule: FlagRule, user: UserContext, now: datetime) -> bool:
    if rule.starts_at is not None and now < rule.starts_at:
        return False
    if rule.allowed_regions and user.region not in rule.allowed_regions:
        return False
    if rule.beta_only and not user.is_beta:
        return False
    if rule.staff_only and not user.is_staff:
        return False
    return True
```

The data says what the rule is. The system says how rules are evaluated. Adding a new rule field does not create another subclass.

## 3. Mutually exclusive states vs composable features

Use tagged variants for values that are exactly one thing. Use composable fields for traits that can mix.

Avoid:

```ts
class LoginAttempt {}
class PasswordLoginAttempt extends LoginAttempt {}
class OAuthLoginAttempt extends LoginAttempt {}
class HighRiskOAuthLoginAttempt extends OAuthLoginAttempt {}
class HighRiskPasskeyLoginAttempt extends LoginAttempt {}
```

Prefer:

```ts
type LoginAttempt =
  | { kind: "password"; email: string; password: string }
  | { kind: "oauth"; provider: "github" | "google"; providerUserId: string }
  | { kind: "passkey"; credentialId: string; challengeId: string };

type RiskSignal =
  | { kind: "new_device"; deviceId: string }
  | { kind: "new_country"; countryCode: string }
  | { kind: "velocity"; attemptsLastHour: number };

type LoginDecisionInput = {
  attempt: LoginAttempt;
  riskSignals: RiskSignal[];
  now: Date;
};
```

The login method is mutually exclusive. Risk signals are composable. Do not force both axes through one inheritance tree.

## 4. I/O at the edge, pure logic inside

Keep storage, network, and framework code near the boundary. Let the core logic operate on plain values.

Avoid:

```go
func RankRecommendations(ctx context.Context, db *sql.DB, userID string) error {
    profile := loadProfile(ctx, db, userID)
    rows := loadCandidates(ctx, db, userID)

    for _, row := range rows {
        score := row.BaseScore
        if profile.Country == row.Country {
            score += 10
        }
        saveRecommendation(ctx, db, userID, row.ID, score)
    }

    return nil
}
```

Prefer:

```go
func RefreshRecommendations(
    ctx context.Context,
    userID string,
    store RecommendationStore,
) error {
    profile, err := store.ProfileForUser(ctx, userID)
    if err != nil {
        return err
    }

    candidates, err := store.CandidatesForUser(ctx, userID)
    if err != nil {
        return err
    }

    recommendations := RankRecommendations(profile, candidates)
    return store.ReplaceRecommendations(ctx, userID, recommendations)
}

func RankRecommendations(
    profile UserProfile,
    candidates []Candidate,
) []Recommendation {
    recommendations := make([]Recommendation, 0, len(candidates))

    for _, candidate := range candidates {
        score := candidate.BaseScore
        if profile.Country == candidate.Country {
            score += 10
        }

        recommendations = append(recommendations, Recommendation{
            CandidateID: candidate.ID,
            Score:       score,
        })
    }

    return recommendations
}
```

The outer function coordinates I/O. The inner function is deterministic and easy to test.

## 5. Push ifs up, fors down

Let the parent decide which path is being taken. Let leaf functions do one kind of work.

Avoid:

```python
def handle_alerts(alerts, pager, audit_log):
    for alert in alerts:
        if alert.severity == "critical":
            pager.page(alert.owner, alert.message)
            audit_log.write("paged", alert.id)
        elif alert.severity == "warning":
            audit_log.write("warning", alert.id)
        elif alert.severity == "info":
            pass
```

Prefer:

```python
def handle_alerts(alerts, pager, audit_log):
    critical = [alert for alert in alerts if alert.severity == "critical"]
    warnings = [alert for alert in alerts if alert.severity == "warning"]

    if critical:
        page_critical_alerts(critical, pager, audit_log)
    if warnings:
        record_warning_alerts(warnings, audit_log)


def page_critical_alerts(alerts, pager, audit_log):
    for alert in alerts:
        pager.page(alert.owner, alert.message)
        audit_log.write("paged", alert.id)


def record_warning_alerts(alerts, audit_log):
    for alert in alerts:
        audit_log.write("warning", alert.id)
```

The top-level function owns branching. Leaf functions own repeated work.

## 6. Boundary validation and replay safety

Validate where external data enters, and use stable external identity when repeated delivery is possible.

Avoid:

```go
func ApplyCalendarEvent(ctx context.Context, db *sql.DB, event CalendarEvent) error {
    _, err := db.ExecContext(
        ctx,
        "insert into meetings(title, starts_at, ends_at) values (?, ?, ?)",
        event.Title,
        event.StartsAt,
        event.EndsAt,
    )
    return err
}
```

Prefer:

```go
func ApplyCalendarEvent(
    ctx context.Context,
    store CalendarStore,
    event ExternalCalendarEvent,
) error {
    parsed, err := ParseCalendarEvent(event)
    if err != nil {
        return err
    }

    return store.UpsertMeetingByExternalID(ctx, parsed)
}

func ParseCalendarEvent(event ExternalCalendarEvent) (MeetingUpdate, error) {
    if event.ProviderEventID == "" {
        return MeetingUpdate{}, errors.New("missing provider event id")
    }
    if event.CalendarID == "" {
        return MeetingUpdate{}, errors.New("missing calendar id")
    }
    if !event.EndsAt.After(event.StartsAt) {
        return MeetingUpdate{}, errors.New("meeting end must be after start")
    }
    if event.Status != "confirmed" && event.Status != "cancelled" {
        return MeetingUpdate{}, errors.New("unknown calendar event status")
    }

    return MeetingUpdate{
        ExternalID: event.CalendarID + ":" + event.ProviderEventID,
        Title:      event.Title,
        StartsAt:   event.StartsAt,
        EndsAt:     event.EndsAt,
        Cancelled:  event.Status == "cancelled",
    }, nil
}
```

The boundary parser checks positive space and negative space. The store uses external identity, so replay updates the same meeting instead of creating another one.

## 7. Behavior-first tests

For non-trivial behavior, write the expected behavior before the implementation settles. Keep the test body obvious.

Avoid:

```python
def test_coupon_cases():
    cases = [
        {"cart": Cart(total=100), "code": "SAVE10", "want": 90, "want_error": False},
        {"cart": Cart(total=40), "code": "SAVE10", "want": 40, "want_error": True},
    ]

    for case in cases:
        result = apply_coupon(case["cart"], case["code"])
        if case["want_error"] and result.ok:
            raise AssertionError("expected error")
        if not case["want_error"] and result.total != case["want"]:
            raise AssertionError("wrong total")
```

Prefer:

```python
def test_save10_discount_applies_to_eligible_cart():
    result = apply_coupon(Cart(total=100), "SAVE10")

    assert result == CouponResult(ok=True, total=90)


def test_save10_discount_rejects_cart_below_minimum():
    result = apply_coupon(Cart(total=40), "SAVE10")

    assert result == CouponResult(
        ok=False,
        total=40,
        reason="minimum_not_met",
    )
```

If a table genuinely helps, use one. But do not hide the behavior inside a mini test framework unless the extra indirection earns its place.

## 8. SOLID without ceremony

Use SOLID as a check on responsibility and dependency direction, not as a reason to build class scaffolding.

Avoid:

```go
type UserRepository interface {
    Create(ctx context.Context, user User) error
    Update(ctx context.Context, user User) error
    Delete(ctx context.Context, id string) error
    FindByID(ctx context.Context, id string) (User, error)
    FindByEmail(ctx context.Context, email string) (User, error)
    BeginTx(ctx context.Context) (Tx, error)
}

func SendPasswordReset(
    ctx context.Context,
    repo UserRepository,
    mailer Mailer,
    userID string,
) error {
    user, err := repo.FindByID(ctx, userID)
    if err != nil {
        return err
    }
    return mailer.Send(ctx, user.Email, "reset password")
}
```

Prefer:

```go
type PasswordResetUserStore interface {
    EmailForUser(ctx context.Context, userID string) (string, error)
}

type ResetMailer interface {
    SendPasswordReset(ctx context.Context, email string) error
}

func SendPasswordReset(
    ctx context.Context,
    users PasswordResetUserStore,
    mailer ResetMailer,
    userID string,
) error {
    email, err := users.EmailForUser(ctx, userID)
    if err != nil {
        return err
    }

    return mailer.SendPasswordReset(ctx, email)
}
```

The caller owns the narrow interface it needs. The function depends on behavior, not a giant repository object.

## 9. Comment discipline

Most comments in LLM-written code are noise. Prefer names and structure that make comments unnecessary.

Avoid:

```ts
// This function validates the user input and returns an error if invalid.
function validateProfileInput(input: ProfileInput): ValidationError[] {
  // Create an array to store errors.
  const errors: ValidationError[] = [];

  // Check if the display name is missing.
  if (!input.displayName) {
    errors.push({ field: "displayName", reason: "required" });
  }

  // Return all errors.
  return errors;
}
```

Prefer:

```ts
function validateProfileInput(input: ProfileInput): ValidationError[] {
  const errors: ValidationError[] = [];

  if (!input.displayName) {
    errors.push({ field: "displayName", reason: "required" });
  }

  return errors;
}
```

Use a comment when it explains a non-obvious invariant or external quirk:

```ts
// The provider sends retry_after in seconds; this SDK expects milliseconds.
const retryAfterMs = response.retry_after * 1000;
```

If the comment only says what the next line does, delete it.

## 10. Final cleanup

Before committing or pushing, remove scaffolding that helped the agent think but does not belong in the repo.

Keep:

- tests that guard stable behavior
- validation gates that match the repo's normal workflow
- small adapters that hide awkward external APIs
- comments that explain non-obvious invariants

Remove:

- local scratch scripts
- temporary tests that only prove the implementation you just wrote
- broad docs created because the agent wanted to summarize itself
- speculative interfaces, factories, or strategy layers
- duplicated null/None checks that restate upstream validation or type guarantees
- defensive checks that are abnormal for the surrounding code path
- comments that restate function names or obvious control flow

The standard is not "more artifacts means more rigor." The standard is whether the artifact protects behavior or makes the code easier to maintain.
