## 1. The refresh belongs to the working copy

- [x] 1.1 Give `ProjectRepository` a scope of its own and a job per clone id,
      so a refresh is started rather than awaited. Verify with a test that a
      refresh asked for from a cancelled caller still loads its result
- [x] 1.2 Publish `refreshing` as the set of clone ids in flight. Verify a test
      sees it hold the clone through a refresh and release it after, on success
      and on failure alike
- [x] 1.3 Refuse a second refresh of a clone already in flight. Verify a test
      asking twice sees one fetch
- [x] 1.4 Publish the outcome of the last refresh per clone id, cleared when the
      next one starts. Verify a test reads the error of a failed refresh after
      it landed, not only while it was landing

## 2. Loading and failed mean there is nothing to show

- [x] 2.1 Set `Loading` on a refresh only when the repository holds no index.
      Verify a test refreshing a loaded repository never sees it leave `Loaded`
- [x] 2.2 Set `Failed` on a failed refresh only when there was no index to keep.
      Verify a test sees a loaded repository keep its index and its counts
      through a failed fetch, with the error on the outcome instead
- [x] 2.3 Give the two credential failures the same treatment, since neither
      touches the working copy. Verify a test of a loaded repository whose
      credential cannot be renewed keeps its index
- [x] 2.4 Verify a test that a repository with no index still goes `Loading`
      then `Failed`, so the list's retry affordance is untouched

## 3. The gesture

- [x] 3.1 Write the composable that carries the gesture: the pull-to-refresh
      box, the snackbar host, and the call. Verify it renders and fires in an
      instrumented test of one screen
- [x] 3.2 Report a failed refresh as a snackbar carrying its action: Try again
      for a network failure, Authorize for a repository the authorization does
      not cover. Verify a test of each
- [x] 3.3 Clear the outcome once it has been shown, so a snackbar is not shown
      twice. Verify a test that leaving and returning to a screen does not
      repeat it

## 4. The five screens

- [x] 4.1 Project view: the gesture on every tab, keeping the tab, the search
      and the place. Verify an instrumented test pulls on the Changes tab and
      finds the tab still open and the counts recomputed
- [x] 4.2 Change screen: the gesture, keeping the open artifact tab. Verify an
      instrumented test pulls with `design.md` open and finds it still open
- [x] 4.3 Spec screen: the gesture from the outline and from an open card.
      Verify an instrumented test pulls from a card and finds the card still
      open on the same node
- [x] 4.4 Delta screen: the gesture from the outline and from an open
      comparison, keeping the side being shown. Verify an instrumented test
- [x] 4.5 Repo list: read the shared refreshing state rather than its own flag,
      and report a failed refresh of a loaded row without replacing the row.
      Verify a test that such a row keeps its counts
- [x] 4.6 Make the centred empty states scrollable, so the gesture fires where
      there is nothing to scroll. Verify an instrumented test pulls on a project
      with no OpenSpec project and sees a refresh happen
- [x] 4.7 Make every list fill its pane. A list shorter than the window ended
      where its content ended, leaving a dead strip below it that answered no
      pull. Found by dumping the semantics tree when the Overview tab would not
      refresh: the list stopped at 879px on a 1794px screen and the gesture
      began below it. Verify the instrumented pull on the Overview tab
- [x] 4.8 Read the collected state in the scope that draws it. The project
      view read its index through a plain getter and the change screen built
      its tab row the same way, so neither redrew when a refresh landed under
      an open screen. Verify the instrumented tests for both

## 5. Nothing read outlives a refresh

- [x] 5.1 Key `ChangeViewModel`'s artifact text on the file's path, length and
      modification time, the way `SpecCache` is keyed. Verify a test that an
      artifact rewritten by a refresh is re-read and one left alone is not
- [x] 5.2 Follow the change's tabs when a refresh adds or removes an artifact
      file, moving off a tab that is gone. Verify a test of each
- [x] 5.3 Say so when a refresh removed the node a card is open on, rather than
      showing text no longer in the file. Verify a test on the spec screen and
      one on the delta screen
- [x] 5.4 Verify a test that a change archived by a refresh leaves the change
      screen saying it is no longer in the project

## 6. Verification

- [x] 6.1 An instrumented test that pulls on a change screen, leaves it before
      the refresh lands, and finds the project view carrying the result
- [x] 6.2 An instrumented test that two rows sharing one working copy show one
      refresh between them and both reload from it
- [x] 6.3 `scripts/gate.sh` passes, with the coverage floors held

## 7. Not in this change

- [x] 7.1 Following a change into the archive when a refresh archives it under
      the reader. The screen says it is gone, which is true; going after it is
      its own idea and its own bean
- [x] 7.2 Refreshing on a timer, on resuming the app, or on coming back online.
      The gesture is deliberate, and a phone reading specs in a tunnel should
      not be fetching behind the reader's back
