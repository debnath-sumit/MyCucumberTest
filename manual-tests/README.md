# manual-tests/

Drop JSON files exported from **ManualTestGenerateTool** here (the **Download
JSON** button), one file per story.

## Workflow

1. In ManualTestGenerateTool, generate test cases for a story and click
   **Download JSON**. You get `<story-slug>.json`.
2. Save it into this folder, e.g. `manual-tests/user-can-reset-password.json`.
3. Ask Claude: **"automate `manual-tests/<file>.json`"**. The `CreateNewTest`
   skill reads the JSON and generates the feature + step definitions + page
   object + locators + test data, following this repo's conventions.
4. For any **new page**, you'll first be asked to provide selectors in
   `src/main/resources/locators/<page>.properties`.
5. Verify with `mvn -q -B test-compile`.

## File shape

```json
{
  "shortDescription": "User can reset password via email",
  "description": "...",
  "acceptanceCriteria": "- Link expires after 30 min\n- Password min 8 chars",
  "generatedAt": "2026-06-20T10:00:00Z",
  "testCases": [
    { "id": "TC-01", "title": "...", "preconditions": "...",
      "steps": ["...", "..."], "expectedResult": "...",
      "priority": "High", "type": "Positive" }
  ]
}
```

See `.claude/skills/CreateNewTest/SKILL.md` → "Importing from a manual-test-case
JSON" for the full mapping rules.
