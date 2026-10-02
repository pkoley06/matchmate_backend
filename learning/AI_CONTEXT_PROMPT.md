# AI Context Prompt (paste into a new chat / Claude Code session)

> Copy everything in the block below. Update the "Current position" lines first.

```
I'm learning Spring Boot backend by typing the code myself (do NOT create or edit source files
for me — put fixes and explanations in markdown docs). Project: MatchMate backend
(Spring Boot 4.1.1, Java 25, JJWT 0.13.0, Firebase Admin, PostgreSQL/PostGIS).

Please read these files first, in order:
1. learning/README.md                    (my progress tracker)
2. learning/01-system-design.md          (who calls whom, and when)
3. the note for the class I'm on (learning/NN-*.md)
4. BACKEND_PROGRESS.md                   (the step-by-step code guide)

Current position:
- Understood: (list ✅ items from README)
- Working on: (e.g. Step 2.2 item 3 — JwtAuthenticationFilter)
- Open bugs to fix in my code: (see "Mismatch checklist" in each note)

What I want now:
- Explain the next class in the same style as the existing notes (what it is, who calls it,
  annotations, every method, common errors, quiz) and save it as learning/NN-ClassName.md.
- Update the Call Map in 01-system-design.md and the progress table in README.md.
- Use easy English. Compare my code to BACKEND_PROGRESS.md and list mismatches.
```

## Prompt templates for later
- **Review my code:** "Read `<file>`. Compare with BACKEND_PROGRESS.md. List bugs and mismatches in
  the note's checklist. Don't edit the source."
- **Explain a concept:** "Add a section to `learning/NN-*.md` explaining `<concept>` with a
  real-life analogy."
- **Quiz me:** "Ask me 5 questions on notes 01–03, one at a time, then update README progress."
- **Debug:** "Here is my error: … Which class in the Call Map is involved and why?"
