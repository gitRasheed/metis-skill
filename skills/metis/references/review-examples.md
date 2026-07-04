# Metis review examples

Concrete contrasts for review lenses: boundary/replay safety, comment discipline, and cleanup.

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
