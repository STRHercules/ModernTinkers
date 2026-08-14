## 2026-08-13

- Before the first feature port, add the initial milestone and acceptance checks to `Docs/TASK.md` so implementation work has an explicit task boundary.
- Keep `mod_version` and the exact versioned JAR filename synchronized in every future change; do not start a feature port with a reused version.

## 2026-08-13 — Next port slice

- Port the smallest smeltery recipe/fluid path that produces debris scrap and
  the crafted bone variants, then validate those recipes in a fresh NeoForge
  test world before adding more material items.

## 2026-08-13 — Next static-slice follow-up

- Port one complete table behavior path next, starting with the Part Builder:
  block entity/menu registration, data-driven recipes, server-side validation,
  and a fresh-world interaction check before adding more placeholder tables.

## 2026-08-13 — Next functional table slice

- Port the Part Builder block entity and menu next, keeping its recipe lookup
  server-authoritative and adding a minimal client screen only after the
  material-aware output contract is defined.

## 2026-08-13 — Next client-facing table slice

- Add the Part Builder client screen and selectable pattern list, then verify
  fresh-world insertion, material consumption, persistence, and output visuals
  in both client and dedicated-server runs before extending the same systems
  to the Tinker Station.

## 2026-08-13 — Core systems and parity sweep

- Finish the next pass by closing modifier application/removal, special-tool
  interactions, smeltery transaction persistence, and the resource/data audit;
  then run a fresh client and dedicated-server smoke test before expanding into
  gadgets, entities, or optional integrations.
