## 1. Implementation

- [ ] 1.1 Step the selection to the next and the previous node of the outline,
      refusing to move past either end
- [ ] 1.2 Offer the two actions on an open card, unavailable at the ends, and
      not offered at all when the file is a report rather than an outline
- [ ] 1.3 Mark the selected node in the outline, so the two halves agree on a
      wide screen and the reader keeps their place on leaving the card

## 2. Verification

- [ ] 2.1 View model tests: stepping forward from a requirement reaches its
      first scenario, stepping on from the last scenario reaches the next
      requirement, and neither end wraps
- [ ] 2.2 An instrumented test stepping through a spec from the card
- [ ] 2.3 `scripts/gate.sh` passes

## 3. Not in this change

- [ ] 3.1 The delta view has the same outline and card and wants the same
      thing. Left out deliberately to keep this small; worth its own bean once
      this shape has been read on a phone.
