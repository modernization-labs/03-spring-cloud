# Target analysis: **Boot 2.3.12 + Java 11**

> **Goal of this document:** record the modernization target that took the platform from the bottom of
> the Boot 2.x generation — `Boot 2.0.9.RELEASE` on Java 8, where Step 8 left it — **up three Boot
> minors and across one JDK major** to its landing state: **`Boot 2.3.12.RELEASE` +
> `Spring Cloud Hoxton.SR12` + Java 11**, the full 18-test characterization net green. The discipline is
> unchanged from the earlier targets: **baby steps, multi-hop, characterize before you change, one gated
> hop at a time.** This target is deliberately the *first stable half* of a larger climb to
> **`Boot 2.7.x + Java 17`**, which continues in its companion roadmap,
> [MODERNIZATION_BOOT_2.7_Java_17.md](MODERNIZATION_BOOT_2.7_Java_17.md).

---

## 1. Where we started, where we landed

| Axis | Start (after Step 8) | Landing (after Step 12) | Set by |
|---|---|---|---|
| Spring Boot | `2.0.9.RELEASE` (Spring Framework 5.0) | **`2.3.12.RELEASE`** (Spring Framework 5.2) | Steps 9, 11, 12 |
| Spring Cloud train | `Finchley.SR4` | **`Hoxton.SR12`** | Steps 9, 11 |
| Java | **8** (`<java.version>1.8</java.version>`) | **11** (`<java.version>11</java.version>`) | Step 10 |
| Namespace | `javax` | `javax` (unchanged) | — |
| Characterization net | 18 green on the 2.0 stack | **18 green** on the 2.3 / Hoxton / Java-11 stack | Steps 9–12 |

The net that grades every hop — the OAuth2 `302` wall, the Actuator `/actuator/*` contract, the JAXB
round-trip, the JWT/bcpkix path, the logging-backend probe, the BC probes, and the `JavaeeApiProbeTest`
provenance map — is the same one Step 8 re-anchored against the 2.0 reality. It carried green, hop by
hop, onto the 2.3 plateau. **This landing state is the known-good reference the next target builds on.**

---

## 2. Why this target — the first stable half of the Java-17 climb

The eventual destination is Java 17, and Java 17 is structurally blocked until Spring Framework 5.3
(Boot ≈ 2.4+) — so it cannot be reached in one jump from Boot 2.0. It has to be *climbed to*, one Boot
minor at a time, and the JDK has to be moved early to the first version that supports it. This target is
exactly that preparatory climb taken to a **natural resting point**:

- **Boot `2.0 → 2.3`** is the stretch where every minor stays on the *same* `javax` namespace and the
  Netflix stack still exists — i.e. the **quiet** rungs, before the framework forces anything heavy.
- **Java `8 → 11`** is banked here because Boot 2.1 (Spring 5.1) is the first line that supports Java 11,
  and JEP 320 (the removal of the Java EE / JAXB modules from the JDK) is a clean, isolated, fully
  gradable move on solid ground. From Step 10 on, **Java 11 is the single fixed working JDK** so every
  later Boot diff stays purely "about Boot."

Boot 2.3 is the right place to stop and declare a target because it is the **last minor before the
cliff**: Boot 2.4 / Spring Cloud 2020.0 dismantles the Netflix stack (Hystrix/Ribbon/Zuul removed) and
drops `junit-vintage-engine` from the test starter's default — behaviour-bearing re-architecture that
deserves its own widened net and its own target. Landing cleanly on `Boot 2.3.12 + Java 11` puts the
platform on the doorstep of that work with the JDK question already settled.

---

## 3. The compatibility ceiling (why the waypoints are where they are)

| Boot line | Spring Framework | Highest Java | Spring Cloud train |
|---|---|---|---|
| **2.0** *(start)* | 5.0 | 8 | Finchley |
| 2.1 | 5.1 | **11** (first line to support it) | Greenwich |
| 2.2 – 2.3 *(landing)* | 5.2 | 13 – 14 | **Hoxton** |
| 2.4 *(next target)* | 5.3 | 15 | 2020.0 (Ilford) — **Netflix removed** |
| 2.7 / 3.0 *(later)* | 5.3 / 6.0 | 17 | 2021.0 / 2022.0 |

Two facts shaped this target: **Java 11 becomes reachable at Boot 2.1** (so it is banked early, Step 10),
and **the Netflix stack survives through 2.3 but is dismantled at 2.4** (so this target stops at 2.3,
just below that boundary).

---

## 4. The coupled passenger: Spring Cloud (version-locked to Boot)

The iron rule from Step 1 still binds: the Spring Cloud train is **version-locked to the Boot minor** —
you cannot move one without the other. Across this target the train crossed two boundaries (the third,
the rename to calendar versions, waits for the next target):

| Boot hop | Spring Cloud train moves to | Paid in |
|---|---|---|
| 2.0 → 2.1 | Finchley → **Greenwich** | Step 9 |
| 2.1 → 2.2 | Greenwich → **Hoxton** | Step 11 |
| 2.2 → 2.3 | **Hoxton (no move)** — Hoxton serves both 2.2 and 2.3 | Step 12 (no-op) |

The `spring-cloud-netflix` family (`netflix-hystrix`, `netflix-core`) rode along to its Hoxton line and
**still exists** at the landing state. Dismantling it is the headline of the *next* target, not this one.

---

## 5. The decomposition — the baby-step sequence as executed

One minor per Boot hop; the JDK switch banked right after 2.1. Numbering continues from Step 8. Each row
is a *completed, gated* hop — its full write-up and findings follow below.

```
Step 9   Boot 2.0.9 → 2.1.18     (Java 8 held)   train Finchley → Greenwich   → unlocked Java 11
Step 10  Java 8 → 11             (Boot 2.1)      JAXB/JAF re-internalized      → JaxbArithmetics... the oracle
Step 11  Boot 2.1 → 2.2.13       (Java 11)       train Greenwich → Hoxton       (JUnit 5 default — stayed vintage)
Step 12  Boot 2.2 → 2.3.12       (Java 11)       train Hoxton (no move)        → LANDING: Boot 2.3.12 + Java 11
```

**What each hop actually cost (findings in brief — full detail in each step below):**

| Step | Headline change | What the net actually caught |
|---|---|---|
| **9** | Boot 2.1 + Greenwich, Java 8 held | Clean minor; Admin `2.0.6` held (the predicted bump was **wrong**). |
| **10** | Java 8 → 11 | JEP 320: re-internalized `jaxb-api` + `org.glassfish.jaxb:jaxb-runtime` (javax `2.3.x` line); bumped `maven-jaxb2-plugin` `0.13.2 → 0.15.2` (last javax release) to clear the `${tools.jar}` codegen warts. |
| **11** | Boot 2.2 + Hoxton, Java 11 held | Removed the stale `tomcat.version 8.5.34` pin (Boot 2.2's `Registry.disableRegistry()` needs Tomcat 9.0.x); re-pinned `JavaeeApiProbeTest` for the `jakarta.annotation-api` **provenance** flip (same `javax.*` packages, renamed groupId). Vintage held at 18. |
| **12** | Boot 2.3, Java 11 held, **no train move** | Thinnest hop of the climb: the validation-starter split (Boot 2.3 drops Bean Validation from `spring-boot-starter-web`) surfaced as a single **provenance** re-pin in `JavaeeApiProbeTest` — a non-event for this app (no `@Valid` anywhere), so the starter was **not** re-added. |

The recurring shape across all four: the predicted "highest-likelihood blocker" (the Spring Boot Admin
generation bump) was **wrong four times running**, and the real findings were stale-pin and BOM-driven
**provenance** flips the net caught precisely because it was widened over those regions first.

---

## 6. How each hop was graded (methodology continuity)

1. **Baseline green** on the current minor under the *correct* JDK (Java 8 up to Step 9; **Java 11 from
   Step 10 on** — both pivots are heirs to Step 8's "Finding 0": confirm the JDK before diagnosing the
   code).
2. **One hop**, smallest coherent unit, on a throwaway branch.
3. **Re-run the same net.** Predicted flips are the net doing its job; an *un*predicted red is a finding
   to investigate before the hop is "done."
4. **OpenRewrite as advisor-then-applier** per minor (`UpgradeSpringBoot_2_1/2_2/2_3`), run under JDK 17
   — but it covers only the mechanical layer; the JAXB re-internalization, the codegen-plugin bump, and
   the stale-pin removals were hand-applied and recorded as findings.

The two-toolchain rule for the Boot-minor hops: **JDK 17** *only* to run the OpenRewrite applier; the
**verification gate runs on the working JDK** (Java 8 through Step 9, Java 11 from Step 10 on).

---

## 7. What this target left deliberately untaken (it is the next target's scope)

Held off on purpose, each its own later gated hop in
[MODERNIZATION_BOOT_2.7_Java_17.md](MODERNIZATION_BOOT_2.7_Java_17.md):

- **The Netflix cliff** — Hystrix → Resilience4j re-architecture, and the Boot 2.4 / Spring Cloud 2020.0
  removal of `netflix-hystrix`/`netflix-core` (preceded by the circuit-breaker net-widening, Step 12a).
- **The rest of the Boot climb** — 2.4 → 2.7, dragging the train to 2021.0 (Jubilee).
- **Java 11 → 17** — the JDK capstone, held until Boot reaches 2.7 so the JDK stays a single fixed value
  across the whole climb (Step 18).
- **springfox**, the EOL `spring-security-oauth2` `302` re-architecture, and `javax → jakarta` (Boot 3) —
  each orthogonal to this target's quiet `javax`-plateau climb.

> **§10 decisions carried forward (decided once, honoured here):** *vintage-first* on JUnit (migrate
> later); Netflix to be **ported, not retired**; springfox to migrate to `springdoc-openapi`; the OAuth2
> `302` re-architecture to land inside the walk. These bind the next target, not this one.

---

# Modernization step 9 - the first 2.x minor: Boot 2.0.9 → 2.1.x + Greenwich (Java 8 held)

> **Goal of this document:** the *ninth* baby step, and the **first rung of the 2.x climb** that §7
> laid out. It moves exactly one Boot minor (`2.0.9.RELEASE → 2.1.x`), drags its version-locked
> Spring Cloud train along (`Finchley → Greenwich`), and **deliberately changes nothing else** — the
> JDK stays Java 8, the namespace stays `javax`. It runs on a throwaway branch, is fully reversible,
> and is graded by the same 18-test net Step 8 re-anchored against the 2.0 reality. As in Step 8,
> OpenRewrite is promoted from *advisor* to **applier** for the mechanical layer; everything it cannot
> express is hand-applied on top and recorded as a finding.

## Guiding principle

Step 8 crossed the generation line (1.5 → 2.0) — the one unavoidable big jump. Step 9 is the
opposite in character: the **smallest coherent move on the new plateau**. One minor up. The whole
point of having paid for the 2.0 generation jump is that the rungs above it are now *small* — each a
single `UpgradeSpringBoot_2_x` recipe under a green net, exactly the cadence §7 committed to.

The same iron rule from Step 1 still binds: **the Spring Cloud train is version-locked to the Boot
minor.** Boot `2.1.x` pairs with the **Greenwich** train (Boot `2.0.x` ↔ Finchley, `2.1.x` ↔
Greenwich). So "bump Boot to 2.1" and "move the train to Greenwich" are not two steps — they are one
atomic hop, just as Edgware↔Finchley was in Step 8. The `spring-cloud-netflix` family rides along to
its Greenwich line; **Hystrix/Ribbon/Zuul still exist here** — the Netflix cliff is a 2.4 problem
(Step 14), not a 2.1 one.

**What this hop unlocks but does not spend:** Boot 2.1 is Spring Framework **5.1**, the first line
that supports **Java 11**. This step *makes Java 11 possible*; it does **not** take it. Per §7 the
JDK switch is its own isolated, separately-gated baby step (**Step 10**), so that this diff stays
"about Boot" and the JDK stays a single fixed value across the climb. `<java.version>` remains `1.8`
and the gate still runs on the Java 8 toolchain.

## The JDK tension (read before running) — same split as Steps 6 and 8

Unchanged from Step 8, and for the same reason. OpenRewrite's plugin + `rewrite-spring` recipes are
compiled for **JDK 17+**; the project is still Java 8. Two toolchains, two jobs:

- **JDK 17** — *only* to run the OpenRewrite applier (`rewrite:run`).
- **JDK 8** — for the verification gate (`mvn clean package`) and the committed build forever after.

This is the heir to Step 8's **Finding 0**: the very first symptom of running the gate on the wrong
(modern) JDK is an `InaccessibleObjectException: module java.base does not "opens java.lang"` CGLIB
failure that looks like a migration bug but is a toolchain slip. **Confirm `java -version` before
diagnosing any code.**

## What Step 9 changes — layers, only the first is OpenRewrite's

Same three-to-four-layer shape as Step 8, but each layer should be *far thinner* (a minor, not a
generation). The table is the **plan**; layers 3–4 are sized by what the gate actually reports.

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. Boot parent + BOM-managed pins | parent `2.0.9.RELEASE → 2.1.x`; let the recipe re-settle any `<version>` the 2.1 BOM now manages | **OpenRewrite `rewrite:run`** (`UpgradeSpringBoot_2_1`) | tree diff + all probes |
| 2. Spring Cloud train | `<spring-cloud.version>` `Finchley.SR4 → Greenwich.SR6` (version-locked to Boot 2.1) | **by hand** (thin recipe coverage — the Step 6/8 lesson) | tree diff + all probes |
| 3. Test characterization updates | *only if the net flips* — a property-key rename or a deliberate contract change the 2.1 minor introduces | **by hand** (test files only) | the Step 5 net itself |
| 4. Generation-locked transitive fixes | *only if the gate throws* — the prime suspect is `spring-boot-admin-starter-client` `2.0.6 → 2.1.x` (see anticipated frictions) | **by hand** (`pom.xml`) | the test/context that was crashing |

**Keep `spring-boot-properties-migrator` in place for this hop** (it is still in `pom.xml` at
`runtime` scope from Step 8). It is the cheapest oracle for layer 3 — it logs every `2.0 → 2.1`
property rename at startup. It is transitional and gets removed before the *target* is declared done,
not before this step.

**Do NOT** in this step: change `<java.version>` (stays `1.8` — Java 11 is Step 10); touch
`javax → jakarta`; bump beyond `2.1.x` / `Greenwich`; re-architect the EOL `spring-security-oauth2`
module (its own hop); or migrate JUnit 4 → 5 (the default swaps at Boot **2.2**, Step 11, and §10
decided *vintage first*).

## Anticipated frictions (predictions to verify by running the gate — **not** findings)

> Per the §6 discipline note, nothing below is a finding. Each earns the word "finding" only once a
> stack trace on *this* hop confirms it. They are listed so the walk runs with eyes open. Several may
> turn out wrong — that is the expected shape (Step 8's own retrospective corrected three predictions).

| Region | Why 2.1 might disturb it | First oracle / where it would bite | Prior likelihood |
|---|---|---|---|
| **`spring-boot-admin-starter-client`** | Generation-locked to the Boot minor — Step 8 proved this exactly (1.5.7 → 2.0.6, finding 2). 2.1 likely needs the matching `2.1.x` Admin line. | context-init `NoClassDefFound` of the Step-8 kind. | **High** — the single most likely blocker. |
| **`log4j-to-slf4j` exclusion** | Step 8 finding 1 excluded it from `spring-boot-starter-web` to avoid the collision with tika-app's shaded `log4j-slf4j2-impl`. Confirm the exclusion still resolves the same way under the 2.1 logging starter. | `LoggingBackendProbeTest` + every `@SpringBootTest` dies at static-init if it regresses. | Medium |
| **`commons-io` pin (`2.13.0`)** | Step 8 finding 3 pinned it for Tika's `CloseShieldInputStream.wrap()`. The 2.1 BOM may re-pin `commons-io` — re-verify the live winner still carries `wrap()`. | `HelloControllerTest` Tika test → `NoSuchMethodError`. | Low–Medium |
| **`io.fabric8:spring-cloud-kubernetes-discovery:0.1.6`** | Pre-official K8s coordinate; the Greenwich train move could disturb its resolution/autoconfig. | dependency resolution or autoconfig break at context start. | Low (watch) |
| **Property-key renames** | Each minor deprecates/renames keys; the migrator will log them. | `spring-boot-properties-migrator` startup log + `@TestPropertySource` keys. | Low (likely none material, as in Step 8) |

Regions the roadmap flags but that are **not** in scope here (confirm they stay quiet, do not act on
them): **JAXB/JAF** (only leaves the JDK at Java 11 — Step 10, so still present and green here);
**springfox** (breaks at 2.6+ — Step 16); the **Netflix cliff** (2.4 — Step 14); **JUnit 5 default**
(2.2 — Step 11).

## Step 9 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Work on a throwaway branch

```powershell
git switch -c step9/boot-2.1-greenwich
```

### 1. Capture the "before" baseline under JDK 8 (the regression oracle)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482

mvn -q dependency:tree "-DoutputFile=dependency-tree.step9-before.txt"
mvn -q test       # the full net + probes must be GREEN on 2.0.9/Finchley first (18 tests)
```

Confirm **BUILD SUCCESS, 18 tests**. If the baseline is not green, stop — the hop is only gradable
against the known-good 2.0 stack Step 8 left behind.

### 2. Switch this shell to JDK 17 and run the OpenRewrite *applier* (layer 1)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'   # adjust to your JDK 17 install path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.x
```

Re-resolve the *current* plugin/recipe pair (do not trust these numbers — Step 6 §3 discipline), then
run the applier with the **2.1** recipe:

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_1"
```

(fall back to `:runNoFork` if the JDK-17 fork fails — record it; the substantive edits are in
`pom.xml`.) Then **review the diff before trusting it**:

```powershell
git --no-pager diff -- pom.xml
```

Expect the parent at `2.1.x` and minor BOM re-settling. **Verify `<java.version>` stayed `1.8`** — the
recipe should not touch it; if it does, revert that one line.

### 3. Hand-apply the Spring Cloud train move (layer 2)

In `<properties>`, move the train (version-locked to Boot 2.1):

```xml
        <spring-cloud.version>Greenwich.SR6</spring-cloud.version>   <!-- was Finchley.SR4 -->
```

Hand-applied because the recipe's train coverage is thin (Step 6/8 lesson). The `spring-cloud-netflix`
family moves to its Greenwich line as a consequence — the tree diff + probes gate it. (Hystrix et al.
still exist on Greenwich; nothing to re-architect here.)

### 4. Run the gate, then hand-apply layers 3–4 *as the net dictates*

This is the heart of the hop, and it is a **loop**, not a one-shot edit. Unlike Step 8, do **not**
pre-edit tests or pin transitives speculatively — the 2.1 minor may need *none* of layers 3–4. The
rule is: **change pom/tests only in response to a specific failure the gate just printed.** One fix
per failure, re-run, repeat, until green. Below is exactly how to drive that loop.

#### 4.0 — Switch back to JDK 8, run the gate once, capture the output

The OpenRewrite applier (step 2) left this shell on **JDK 17**. The gate must run on **JDK 8**
(finding-0 lesson) — pivot the toolchain back *now*, before the first gate run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
```

Then run the net and keep the log so you can read the *first* failure, not just the summary:

```powershell
mvn clean test 2>&1 | Tee-Object -FilePath step9-gate.log
```

- **BUILD SUCCESS, 18 tests, 0 failures** → layers 3–4 were empty for this minor. Skip straight to
  step 5 (the final `package` confirmation) and 6. (This is a plausible and good outcome for a single
  minor.)
- **Any failure** → go to 4.1. Always diagnose the **first** failure in the log; later ones are often
  just cascades of the first (e.g. one broken context fails every `@SpringBootTest` after it).

#### 4.1 — Classify the failure: is it a *hard failure* or a *net flip*?

This classification decides whether you touch `pom.xml` (layer 4) or a test file (layer 3). They look
different in the log:

| | **Hard failure (→ layer 4, edit `pom.xml`)** | **Net flip (→ layer 3, edit a test)** |
|---|---|---|
| What you see | An exception during build/startup: `NoClassDefFoundError`, `ClassNotFoundException`, `NoSuchMethodError`, `BeanCreationException`, context fails to load | A clean assertion failure: `expected: <X> but was: <Y>` from one named test |
| What it means | The new framework removed/moved something a library or the context needs | Behaviour deliberately changed in 2.1; the test still ran, it just pinned the old value |
| Scope | Usually takes down **many** tests at once (the context can't start) | Usually **one** test, on one assertion |
| The fix | Bump/exclude/pin a dependency so the class is back on the classpath | Update the assertion to the new value **and** write down why it changed |

#### 4.2 — If it's a HARD FAILURE → fix the dependency (layer 4)

1. Read the exception and the class it names. Map the class to its owning artifact.
2. The **highest-likelihood case for this hop** (see anticipated-frictions table) is Spring Boot
   Admin: a `NoClassDefFoundError` thrown from a `de.codecentric...` class during context init means
   `spring-boot-admin-starter-client:2.0.6` is generation-locked to Boot 2.0 and must move to its 2.1
   line. Edit `pom.xml` (the dependency at lines ~144–152):

   ```xml
   <dependency>
       <groupId>de.codecentric</groupId>
       <artifactId>spring-boot-admin-starter-client</artifactId>
       <version>2.1.6</version>   <!-- was 2.0.6: Admin is version-locked to the Boot minor -->
   </dependency>
   ```
   (Resolve the actual latest `2.1.x` Admin patch when you do it — `2.1.6` is the expected coordinate,
   not a guarantee.)
3. For any *other* hard failure, apply the same shape of fix the Step-8 findings used — exclude a
   colliding bridge, or pin a transitive to the version a caller needs. **Make exactly one change**,
   then go to 4.4. Do not batch several speculative pom edits.

#### 4.3 — If it's a NET FLIP → update the characterization (layer 3)

A net flip is the net *doing its job*: it caught that 2.1 changed a behaviour you had pinned. The
action is to **re-pin to the new reality**, not to "make the test pass" blindly:

1. Open the named test and find the failing assertion.
2. Confirm the new value is a **legitimate, intended** 2.1 change (a renamed property key, a changed
   default, a contract shape) — not a real regression. If you cannot explain *why* it changed, treat
   it as a finding to investigate before proceeding, **not** an assertion to overwrite.
3. Update the assertion (or `@TestPropertySource` key) to the new value, and add a one-line comment
   recording the flip — mirroring how Step 8 recorded the Actuator `/actuator/*` and OAuth2 `401→302`
   flips. Example shape (illustrative — only if such a key actually surfaces):

   ```java
   // Boot 2.1: <old.key> renamed to <new.key>  (surfaced by spring-boot-properties-migrator)
   @TestPropertySource(properties = { "<new.key>=<value>" })
   ```
4. The `spring-boot-properties-migrator` (still in `pom.xml`, runtime scope) logs property renames at
   startup — grep `step9-gate.log` for its `WARN` lines to find the exact old→new key mapping rather
   than guessing.

#### 4.4 — Re-run and repeat

After **one** fix (4.2 or 4.3), re-run the gate command from 4.0 (`mvn clean test`, still on JDK 8).
Each pass should resolve one failure and reveal the next, if any — exactly the "fix the one it names,
re-run" loop. Record each fix as it lands in the **Findings** section below (it becomes a real finding
the moment the trace confirms it). Stop when the run reports **BUILD SUCCESS, 18/18**, then go to
step 5 to confirm the full `package` (fat-jar repackage) under the same JDK 8.

### 5. Confirm the full `package` under JDK 8

Once the test loop in step 4 is green (18/18), confirm the *whole* build — including the fat-jar
repackage, which `mvn test` does not exercise — still on JDK 8:

```powershell
java -version    # re-confirm: 1.8.0_482 (finding-0 lesson: NOT a modern JDK)
mvn clean package
```

Expect **BUILD SUCCESS, 18 tests, 0 failures**, jar repackaged. If `package` fails where `test`
passed (e.g. the `spring-boot-maven-plugin` repackage or the `maven-jaxb2-plugin` codegen), treat it
as a hard failure and return to the step-4 loop.

### 6. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step9-after.txt"
Compare-Object (Get-Content dependency-tree.step9-before.txt) (Get-Content dependency-tree.step9-after.txt)
```

Expected movement is **moderate and coherent** for a minor: Boot `2.0.9 → 2.1.x`, Spring Framework
`5.0 → 5.1`, the train Finchley → Greenwich (Netflix to its Greenwich line), BOM-managed transitives
moving with them, plus any single deliberate adjustment from step 4. A **dropped framework**, an
untouched pin moving, or `<java.version>` changing is a finding to investigate. **Record the salient
deltas below.**

## Findings (record after running Step 9)

> _Filled in after the gate runs — predictions above graded against reality, Step 8's retrospective
> discipline applied to this hop. Note which predictions held and which were wrong._

- _Finding 0 (toolchain): …_
- _Layer-1 OpenRewrite diff (versions / fork mode): …_
- _Blockers actually hit (if any): …_
- _Net flips actually seen (if any): …_

## Step 9 — done criteria (all must hold)

1. `mvn clean package` under **JDK 8** → **BUILD SUCCESS**, fat jar repackaged, `<java.version>` still
   `1.8`, `mvn test` green **18/18**.
2. The net flipped **only where predicted-or-explained**; no *unexplained* red survived. JAXB, the
   JWT/bcpkix path, `LoggingBackendProbeTest`, the BC probes, and the OAuth2 `302` shape all stay as
   Step 8 left them unless a flip is explicitly recorded as a finding.
3. The OpenRewrite layer-1 diff is reviewed; layers 2–4 are the only hand-applied changes, each
   traceable to a finding above.
4. Parent is `2.1.x`, `<spring-cloud.version>` is `Greenwich.SR6`, and the before/after tree diff
   shows Boot/Spring/Spring-Cloud moving together to the 2.1/Greenwich generation with nothing
   unrelated dropped.
5. **Java 11 is now *possible* but deliberately untaken** — `<java.version>` is still `1.8`; that
   switch is Step 10.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step9/boot-2.1-greenwich
```

Delete `dependency-tree.step9-before.txt` / `dependency-tree.step9-after.txt` if not keeping them.

## Toward Step 10

With the platform on `Boot 2.1.x + Greenwich` and the net green, **Spring Framework 5.1 makes Java 11
reachable** — and §7 banks it immediately as the next isolated baby step:

- **Step 10 — Java 8 → 11** (Boot 2.1 held). The oracle is `JaxbArithmeticsCharacterizationTest`: Java
  11 removes `java.xml.bind` / `java.xml.ws` / `javax.activation` from the JDK, so expect to add
  `jaxb-api` + a JAXB runtime (and possibly JAF), and to verify `maven-jaxb2-plugin:0.13.2` still runs
  its `generate-sources` codegen under JDK 11. Java 11 then becomes the steady working JDK for every
  Boot hop up to 2.7 (Step 18 is the Java 17 capstone).

---

# Modernization step 10 — the JDK waypoint: Java 8 → 11 (Boot 2.1 held)

> **Goal of this document:** the *tenth* baby step, and the **first of the two JDK hops** §4 committed
> to (`8 → 11` waypoint now; `11 → 17` capstone at Step 18). It changes exactly one axis — the **JDK**
> — moving the toolchain and `<java.version>` from `1.8` to `11`, and **deliberately changes nothing
> else**: the Boot parent stays `2.1.18.RELEASE`, the train stays `Greenwich.SR6`, the namespace stays
> `javax`. It runs on a throwaway branch, is fully reversible, and is graded by the same 18-test net
> Step 9 carried green onto the 2.1/Greenwich plateau. This is the hop the net was *designed* for: the
> `JaxbArithmeticsCharacterizationTest` knot Step 5 tied exists precisely to grade this JDK move.

## Guiding principle

Step 9 was "one Boot minor, JDK held." Step 10 is its mirror image: **one JDK major, Boot held.** §7
banks Java 11 the moment Boot 2.1 (Spring Framework 5.1) makes it *possible* — not at the end of the
walk — for one reason: it is a clean, isolated, fully-gradable move on solid ground, and carrying
Java 8 up through every later Boot hop would drag an extra moving part through each diff for no
benefit. After this step, **Java 11 is the single fixed working JDK** for Steps 11–17, so every Boot
diff stays purely "about Boot." Java **17** is *not* taken here even though it is the eventual
destination — it is the separately-gated capstone at Step 18, after Boot reaches 2.7.

The discipline that makes this a baby step: **the only thing allowed to change is what the JDK switch
forces.** The JDK switch forces exactly one structural thing — JEP 320 (Java 11) removed the Java EE
and CORBA modules from the JDK, so the JAXB / JAX-WS / activation APIs and their runtimes that the
app silently borrowed from the Java 8 JDK are simply gone. Everything we add in this step is the
*re-internalization* of those once-bundled libraries as explicit dependencies. Nothing else.

## The JDK tension (read before running) — the pivot, not a split

Step 9 (like Steps 6 and 8) needed **two** JDKs because OpenRewrite's recipes are compiled for JDK 17+
while the gate ran on JDK 8. **Step 10 has no OpenRewrite Boot-minor recipe** (there is no Boot bump
to apply), so the *mandatory* spine of this hop runs on **a single JDK — Java 11 — end to end.** The
**only** exception is the *optional* OpenRewrite advisor for the JAXB layer (step 1a below): those
recipes are still compiled for JDK 17+, so if you choose to run them you take a brief JDK-17 detour
(exactly as Step 9 step 2 did) that touches only `pom.xml`, then pivot back to JDK 11 for everything
else. Skip the advisor and the hop is genuinely single-JDK.

This inverts Step 8's **Finding 0**, and the inversion is the trap to watch for:

- **Before Step 10:** the gate *had* to run on Java 8; running it on a modern JDK produced the
  `InaccessibleObjectException` CGLIB failure that *looked* like a code bug but was a toolchain slip.
- **From Step 10 on:** the gate *must* run on **Java 11**; accidentally running it on the old Java 8
  toolchain will hide exactly the JAXB-removal failures this hop exists to surface (Java 8 still ships
  the modules, so the missing dependencies would appear to "work" and the regression would land
  silently on the next person). **Confirm `java -version` reports `11` before diagnosing anything.**

> Java 11 (unlike Java 17) only *warns* on illegal reflective access — `WARNING: An illegal reflective
> access operation has occurred` from CGLIB/Spring/Hibernate-style libraries. **Those warnings are
> expected and are not findings.** The strong-encapsulation *errors* (`InaccessibleObjectException`)
> are a JDK 16+ phenomenon and are the Step 18 problem, not this one. This is exactly why Java 11 is
> the gentle waypoint and Java 17 the capstone.

## What Step 10 changes — layers (none is OpenRewrite's by default)

Unlike Step 9, there is no `UpgradeSpringBoot_*` recipe driving layer 1. The mechanical work here is
small and well-known, so it is **hand-applied**; OpenRewrite appears only as an *optional advisor*
(see the note after the table), never as the primary applier.

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. JDK / compiler level | `<java.version>` `1.8 → 11`; `maven.compiler.source`/`target` `1.8 → 11` | **by hand** (`pom.xml` `<properties>`) | the whole net compiling & running under JDK 11 |
| 2. Re-internalize the removed Java EE APIs + runtimes | add `jaxb-api` + a JAXB **runtime** (`org.glassfish.jaxb:jaxb-runtime`), and JAF (`javax.activation`); add SAAJ/JAX-WS bits **only if** `spring-ws-core` demands them | **by hand** (`pom.xml`) | `JaxbArithmeticsCharacterizationTest` + every `@SpringBootTest` |
| 3. `maven-jaxb2-plugin` codegen under JDK 11 | the `0.13.2` plugin generated `AddRequest` under Java 8's in-JDK JAXB tooling; under JDK 11 it may need the XJC tooling on its own plugin classpath (or a plugin bump) | **by hand** (plugin `<dependencies>` or `<version>`) — *only if `generate-sources` fails* | `generate-sources` phase; then `JaxbArithmeticsCharacterizationTest` |
| 4. Test characterization updates | *only if the net flips* — unlikely on a pure JDK move, but a reflective-access change could shift a probe | **by hand** (test files only) | the Step 5 net itself |

**OpenRewrite as optional advisor only.** `rewrite-migrate-java` ships `UpgradeToJava11` (and the
narrower `org.openrewrite.java.migrate.javax.AddJaxbDependencies` / `AddJaxbRuntime`) which can
*suggest* — or apply — layers 1 and 2. The concrete `dryRun`/`run` commands are in **step 1a** below.
Run `dryRun` first, under JDK 17, read its patch, and apply with judgement: the layer-1 and layer-2
edits are small enough to write by hand with the BOM open, so the recipe is best used as a
**cross-check**, not the driver. Two guardrails when reading its patch (both are why `dryRun` comes
before `run`):

- **Namespace.** Newer `rewrite-migrate-java` versions may propose the **`jakarta.xml.bind`** (`3.x`)
  coordinates. **Reject those** — this hop stays on the **`javax`-namespace `2.3.x`** line; jakarta is
  a Boot 3 concern (next target). If the recipe insists on jakarta, hand-apply the `javax` coordinates
  instead.
- **JDK level.** Confirm any `<java.version>` it writes is **`11`**, not a higher default — and that it
  did not touch coordinates outside the JAXB/activation set.

**Prefer the Boot 2.1 BOM's managed versions where they exist.** Boot 2.1 manages several of the
JAXB/activation coordinates. Add the dependencies **without** a `<version>` first and let the BOM
settle them; only pin a `<version>` (to the `2.3.x` javax-namespace line) for a coordinate the BOM
does *not* manage. Resolve the actual managed versions with `mvn dependency:tree` — **do not trust the
numbers written below; they are the expected shape, not a guarantee** (Step 6 §3 discipline).

**Do NOT** in this step: bump the Boot parent or the train beyond `2.1.18` / `Greenwich.SR6`; go to
Java 17 (`<java.version>` becomes `11`, not `17` — Java 17 is Step 18); touch `javax → jakarta` (stay
on the `2.3.x` **javax-namespace** JAXB line, *not* the `jakarta.xml.bind` `3.x` line); re-architect
`spring-security-oauth2`, springfox, or Netflix (each its own later hop); or migrate JUnit 4 → 5.

**Keep `spring-boot-properties-migrator` in place** (still in `pom.xml`, `runtime` scope). It is a
pure JDK hop so it is unlikely to log anything new, but it costs nothing and stays the cheapest
property oracle until the *target* is declared done.

## Anticipated frictions (predictions to verify by running the gate — **not** findings)

> Per the §6 discipline note, nothing below is a finding. Each earns the word "finding" only once a
> stack trace on *this* hop confirms it. Step 9's retrospective is the template: its single
> highest-likelihood prediction (the Admin `2.0.6 → 2.1.x` bump) turned out **wrong** — Admin `2.0.6`
> ran fine on Boot 2.1 and the hop was a clean 2-line diff. Expect the same humbling here.

| Region | Why Java 11 disturbs it | First oracle / where it would bite | Prior likelihood |
|---|---|---|---|
| **JAXB API + runtime gone from the JDK** | JEP 320 removes `java.xml.bind`. `JAXBContext.newInstance(...)` finds no provider → `JAXBException`/`ClassNotFoundException` at runtime. The generated `AddRequest` won't even compile without the API. | `JaxbArithmeticsCharacterizationTest` (both methods) → compile error or "Implementation of JAXB-API has not been found". | **High** — this is the *designed* failure of the hop; layer 2 exists to fix it. |
| **`maven-jaxb2-plugin:0.13.2` codegen** | The ancient codegen plugin leaned on Java 8's in-JDK XJC tooling. Under JDK 11 the modules are gone from its runtime too. | `mvn generate-sources` / `mvn compile` failing in the `generate-arithmetics` execution before any test runs. | **Medium–High** — most likely needs XJC tooling added to the plugin's `<dependencies>`, possibly a plugin bump. |
| **`javax.activation` (JAF) gone** | Also removed by JEP 320; JAXB's runtime and SOAP attachments pull it transitively. | `NoClassDefFoundError: javax/activation/...` chained off the first JAXB call. | Medium — usually arrives as a transitive of `jaxb-runtime`; add explicitly only if it doesn't. |
| **`spring-ws-core` SAAJ / JAX-WS** | `java.xml.ws` (SAAJ `javax.xml.soap`, JAX-WS) also left the JDK. If any live WS path touches SAAJ, it breaks the same way JAXB does. | context start of a `@SpringBootTest`, or a WS-specific path. The current net is JAXB-only, so this may stay quiet. | Low–Medium (watch) |
| **`javax:javaee-api:8.0` already on the classpath** | It already provides the `javax.xml.bind` **API stubs** at compile time (no runtime impl). Adding `jaxb-api` could duplicate/clash the API, or mask that what's really missing is only the *runtime*. | split-package / duplicate-class warnings, or the test still failing for lack of a *runtime* despite the API resolving. | Medium — read carefully *which* layer (API vs runtime) the trace actually misses before adding both. |
| **Illegal reflective access warnings** | CGLIB, Spring, Hibernate-validator, etc. reflect into `java.base` / `java.xml` internals; Java 11 *warns* (does not fail). | `WARNING: An illegal reflective access operation has occurred` in `step10-gate.log`. | **Expected — not a finding.** Only an *error* (not a warning) is in scope, and errors are a Java 17 thing (Step 18). |
| **Other ancient libs under JDK 11** | itext 2.1.7, BC 1.57, springfox 2.7.0, tika's shaded jars, the gatling test dep — all predate Java 11. | any `@SpringBootTest` or the specific probe touching them. | Low (watch) — none is known to hard-fail on 11; flag if one does. |

Regions the roadmap flags but that are **not** in scope here (confirm they stay quiet, do not act on
them): the **Netflix cliff** (2.4 — Step 14); **springfox** (breaks at 2.6+ — Step 16); **JUnit 5
default** (2.2 — Step 11); strong-encapsulation `InaccessibleObjectException` **errors** (Java 17 —
Step 18); the `javax → jakarta` rename (Boot 3 — next target).

## Step 10 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Work on a throwaway branch

```powershell
git switch -c step10/java-11
```

### 1. Capture the "before" baseline under JDK 8 (the regression oracle)

The committed Step-9 state is green on **Java 8**. Capture it there first — that is the known-good
reference this hop is graded against.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482

mvn -q dependency:tree "-DoutputFile=dependency-tree.step10-before.txt"
mvn -q test       # the full net + probes must be GREEN on 2.1/Greenwich/Java 8 first (18 tests)
```

Confirm **BUILD SUCCESS, 18 tests**. If the baseline is not green, stop — the hop is only gradable
against the known-good 2.1/Greenwich stack Step 9 left behind.

### 1a. (Optional) OpenRewrite advisor for the Java 11 / JAXB layer — under JDK 17

This step is **optional** and is the *only* part of the hop that runs on JDK 17 (the rewrite recipes
are compiled for JDK 17+ — Step 6/8 discipline). It produces or applies layers 1–2 mechanically; if
you skip it, write those edits by hand in steps 3–4 instead. Either way, **the gate still runs on JDK
11** (step 4 onward).

Switch this shell to JDK 17 *only* for the rewrite command:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.x
```

Re-resolve the *current* plugin/recipe pair (do **not** trust these numbers — Step 6 §3 discipline)[https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/3.33.0/rewrite-recipe-bom-3.33.0.pom](https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/3.33.0/rewrite-recipe-bom-3.33.0.pom),
then run the **`dryRun`** advisor — it writes a patch and changes nothing:

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:dryRun `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-migrate-java:3.37.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.migrate.Java8toJava11"
```

Read the proposed patch and apply the two guardrails from the layers section (reject any
`jakarta.xml.bind` coordinates — stay on the `javax` `2.3.x` line; confirm `<java.version>` becomes
`11`, not higher):

dryRun result and analysis:

The `Java8toJava11` recipe (`rewrite-migrate-java:3.37.0`) produced a `target/rewrite/rewrite.patch`
with **three** hunks. Graded against this hop's "one axis: the JDK" discipline, only one survives —
a textbook case of *why dryRun-then-judge, never blind run*: the recipe simultaneously **over-reached**
(BouncyCastle) and **under-delivered** (no JAXB runtime, wrong scope).

| Hunk | Proposed change | Verdict |
|---|---|---|
| 1 | `<java.version>` / `maven.compiler.source` / `target` `1.8 → 11` | ✅ **Keep** — this is layer 1, and it correctly stopped at `11` (not higher). |
| 2 | `bcprov-jdk15on:1.57` → `bcprov-jdk18on:1.84` | ❌ **Reject** — out of scope (see below). |
| 3 | add `jakarta.xml.bind:jakarta.xml.bind-api:2.3.3`, `<scope>test</scope>` | ⚠️ **Do not apply as-is** — right namespace, wrong everything else (see below). |

**Hunk 3 — the `jakarta.xml.bind` line is *not* the namespace problem.** `jakarta.xml.bind-api:2.3.3`
is the Jakarta-EE-8 artifact: **renamed groupId, but still the `javax.xml.bind.*` packages** (the
package rename only happens at `3.0+`). So it would satisfy the test's `import javax.xml.bind.*` and
Spring 5.1 fine — the blunt "reject any `jakarta.xml.bind` coordinate" guardrail above is too coarse
and would wrongly reject it. It is still the *wrong fix* for two other reasons:

- **API-only, no runtime.** `jakarta.xml.bind-api` is just the interfaces;
  `JAXBContext.newInstance(...)` needs a runtime provider (`org.glassfish.jaxb:jaxb-runtime`), which
  the recipe **did not add**. Applied alone, `JaxbArithmeticsCharacterizationTest` still fails at
  runtime with *"Implementation of JAXB-API has not been found."*
- **`<scope>test</scope>` is wrong.** The recipe scoped it to `test` because the only
  `javax.xml.bind` import it could *see* was in the test source — it cannot see the generated
  `AddRequest` (generate-sources has not run when rewrite parses), which is **main** code. (In
  practice `javax:javaee-api:8.0` already supplies the `javax.xml.bind` **API** on the compile
  classpath everywhere — which is likely why the recipe added a redundant API and *missed* that the
  real gap is the **runtime**.)

So: do not be misled by the `jakarta` groupId. The right layer-2 move is to add the **runtime**
(`org.glassfish.jaxb:jaxb-runtime`, `2.3.x`) and let the JDK-11 gate (step 4) confirm whether the API
(already via `javaee-api`) and activation are needed — *not* to paste this `test`-scoped API line.

**Hunk 2 — the BouncyCastle bump is the more concerning change, and it is out of scope.** It is **not
forced by Java 11** (BC `1.57` runs fine on 11); the recipe just opportunistically modernizes BC,
changing both the artifact line (`jdk15on → jdk18on`) and the version (`1.57 → 1.84`) straight through
the characterized crypto path. `JwtBcPkixCharacterizationTest` exists *precisely* to fail when a
"rationalize BouncyCastle" move disturbs the bcprov/bcpkix signing path (its own Javadoc says so).
That is a behaviour-bearing change belonging to its own hop — **drop it from this patch.**

**Net decision:** take **hunk 1 only** from the patch; reject hunk 2; replace hunk 3 with a
hand-applied `jaxb-runtime` (decided by the step-4 gate). This is the concrete instance of the
"review the patch for out-of-scope changes and drop them" guardrail.

### 2. Pivot the toolchain to JDK 11 (this is the hop)

Switch this shell to **JDK 11** — where the gate and the committed build live from here on — and stay
there for the rest of the step (if you ran step 1a, this is the pivot *back* off JDK 17):

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
```

### 3. Raise the compiler level (layer 1)

In `<properties>`, move the JDK level from 8 to 11 — **11, not 17**:

```xml
        <java.version>11</java.version>            <!-- was 1.8 -->
        <maven.compiler.source>11</maven.compiler.source>   <!-- was 1.8 -->
        <maven.compiler.target>11</maven.compiler.target>   <!-- was 1.8 -->
```

### 4. Run the gate once to surface the *real* failures, then hand-apply layer 2 as the net dictates

This is the heart of the hop and it is a **loop**, exactly as Step 9 §4 was. Do **not** pre-add every
JAXB dependency speculatively — run the gate first and let it name precisely what is missing (API vs
runtime vs activation), so the diff stays minimal and each added coordinate traces to a real trace.

```powershell
mvn clean test 2>&1 | Tee-Object -FilePath step10-gate.log
```

- **BUILD SUCCESS, 18/18** → nothing was missing (implausible for this hop, but if so, skip to step 6).
- **A compile / codegen failure first** → it is almost certainly the `maven-jaxb2-plugin` codegen or
  the generated `AddRequest` lacking `javax.xml.bind` on the compile classpath → go to 4.1.
- **A runtime `JAXBException` / `ClassNotFoundException` from `JaxbArithmeticsCharacterizationTest`**
  → the API resolved but no JAXB **runtime** is present → go to 4.2.

Always diagnose the **first** failure in the log; later ones are usually cascades.

#### 4.2 — Runtime "JAXB-API not found" → re-internalize the API + runtime (layer 2)

Add the once-bundled libraries as explicit dependencies, on the **`2.3.x` javax-namespace line** (not
the `jakarta` 3.x line — namespace stays `javax` until Boot 3). Add **without** `<version>` first and
let the Boot 2.1 BOM settle them; pin only what the BOM does not manage:

```xml
<!-- Step 10 (Java 11 / JEP 320): java.xml.bind left the JDK. Re-internalize the JAXB API and a
     runtime that the app previously borrowed from the Java 8 JDK. javax-namespace 2.3.x line. -->
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <!-- version: prefer Boot 2.1 BOM; else 2.3.1 -->
</dependency>
<dependency>
    <groupId>org.glassfish.jaxb</groupId>
    <artifactId>jaxb-runtime</artifactId>
    <!-- version: prefer Boot 2.1 BOM; else 2.3.1 -->
</dependency>
```

Re-run (4.3). If the next failure is `NoClassDefFoundError: javax/activation/...`, JAF
(`javax.activation:javax.activation-api:1.2.0`, or `com.sun.activation:javax.activation`) did not
arrive transitively — add it explicitly, one coordinate, re-run. **Watch the `javaee-api:8.0`
interaction** (anticipated-frictions row): if the *API* already resolves via `javaee-api` and only the
*runtime* is missing, add `jaxb-runtime` alone rather than duplicating the API.

#### 4.3 — Re-run and repeat

After **one** fix, re-run the gate from step 4 (`mvn clean test`, still on **JDK 11**). Each pass
resolves one failure and reveals the next. Record each fix as it lands in **Findings** below. Stop at
**BUILD SUCCESS, 18/18**, then go to step 5.

> Ignore `WARNING: An illegal reflective access operation has occurred` lines — they are the expected
> Java 11 reflective-access warnings (see the JDK-tension note), not failures.

### 5. Confirm the full `package` under JDK 11

```powershell
java -version    # re-confirm: 11.0.x (inverted finding-0: must NOT be Java 8 now)
mvn clean package
```

Expect **BUILD SUCCESS, 18 tests, 0 failures**, fat jar repackaged. `package` exercises the
`spring-boot-maven-plugin` repackage *and* re-runs the `maven-jaxb2-plugin` codegen that `mvn test`
also triggers — if it fails where `test` passed, treat it as a hard failure and return to the step-4
loop.

### 6. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step10-after.txt"
Compare-Object (Get-Content dependency-tree.step10-before.txt) (Get-Content dependency-tree.step10-after.txt)
```

Expected movement is **small and JDK-shaped**: the **only** new nodes should be the re-internalized
JAXB API + runtime (+ activation if added) — i.e. libraries the Java 8 JDK used to supply. Boot,
Spring, Spring Cloud, and every other pin should be **unchanged** (this hop bumps no parent and no
train). A moved Boot/Spring/train version, a dropped framework, or any unrelated pin moving is a
finding to investigate. **Record the salient deltas below.**

## Findings (record after running Step 10)

> _Filled in after the gate runs — predictions above graded against reality, Step 9's retrospective
> discipline applied to this hop. Note which predictions held and which were wrong._

_Finding 0. Some transitive pom declares a system-scoped com.sun:tools:jar with systemPath=${tools.jar} — on Java 8 that resolved to $JAVA_HOME/lib/tools.jar, but JDK 11 has no tools.jar, so the property is empty. It's a JEP-320-adjacent wart worth a one-line finding.

## migrating the maven-jaxb2-plugin itself to a Java-11-aware release

This sub-hop **fixes Finding 0** (the `${tools.jar}` warnings). It is in scope for Step 10 because the
warning is JDK-forced (JEP 220/320 removed `tools.jar`), and the only durable fix is to stop pulling
the Java-8-era JAXB-RI codegen tooling — which is the "harden the codegen for JDK 11" move
anticipated-friction #2 already flagged. It stays on the **javax** line (no jakarta until Boot 3).

### Why the two cheap fixes do **not** work (verified dead ends — do not retry)

1. **A project `<tools.jar>` property does not propagate.** The warning is emitted while Maven
   interpolates the *transitive* `com.sun.xml.bind.mvn:jaxb-parent:2.2.11` model (whose
   `dependencyManagement` declares a `system`-scoped `com.sun:tools:jar` at `${tools.jar}`, set only by
   a profile keyed on `${java.home}/../lib/tools.jar` — absent on JDK 11). A property in *our* `pom.xml`
   is not in scope during a dependency pom's interpolation, so `${tools.jar}` stays an unresolved
   literal regardless. Verified: adding the property left all 7 warnings in place.
2. **Overriding only the plugin's XJC tooling to JAXB-RI 2.3.6 breaks codegen.** `maven-jaxb2-plugin:0.13.2`
   is welded to its 2.2.11 tooling: forcing `org.glassfish.jaxb:jaxb-xjc:2.3.6` into its `<dependencies>`
   fails with `NoClassDefFoundError: com/sun/tools/rngom/nc/NameClass` (missing RELAX NG) **and** the raw
   2.3.6 XJC rejects the plugin's `<schemaLanguage>WSDL</schemaLanguage>` ("WSDL support is experimental").
   Verified: `BUILD FAILURE`, warnings unchanged.

The `com.sun:tools` entry is `dependencyManagement`-only (never *declared*), so nothing resolves it —
the warning is purely cosmetic and the 2.2.11 build is green. But the only *clean* removal is to move
the whole plugin to a release that already ships Java-11-clean (2.3.x) tooling.

### The fix — bump the plugin to its last javax release (0.13.2 → 0.15.2)

Keep the **same** coordinates (`org.jvnet.jaxb2.maven2:maven-jaxb2-plugin`) — the `0.15.x` line is the
last on the **javax** namespace (JAXB-RI 2.3.x, JDK 9+ aware). **Do NOT** move to the renamed
`org.jvnet.jaxb:jaxb-maven-plugin` `4.x` line — that is the **jakarta** successor and belongs to the
Boot 3 target, not here. The change is a one-line version bump; no `<dependencies>` override, no config
change:

```xml
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <version>0.15.2</version>   <!-- was 0.13.2: last javax (2.3.x) release; JDK-9+ clean, drops the jaxb-parent:2.2.11 ${tools.jar} hack -->
    <executions> ... unchanged ... </executions>
    <configuration> ... unchanged ... </configuration>
</plugin>
```

### Verify (the gate decides — graded by the JAXB oracle)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn clean package 2>&1 | Tee-Object -FilePath step10-jaxb.log
```

Confirm all four:

1. **0** `${tools.jar}` warnings (`Select-String step10-jaxb.log -Pattern "must specify an absolute path"`
   → count `0`) — down from 7.
2. **BUILD SUCCESS, 18/18**, fat jar repackaged.
3. `JaxbArithmeticsCharacterizationTest` **2/2** — the generated-contract oracle: proves `AddRequest`
   still marshals to the same qualified XML and round-trips.
4. The freshly generated `target/generated-sources/xjc/.../AddRequest.java` still imports
   **`javax.xml.bind.annotation.*`** (not `jakarta.*`) and carries `protected int augend/addend` — the
   contract is unchanged and still javax.

**Residual benign warning (not a regression):** 0.15.2 logs one
`org.xml.sax.SAXParseException: ... WSDL support is experimental` line during `generate-arithmetics`.
It is an xjc *warning* — XJC's `ModelLoader.sanityCheck` peeks at the input root, sees
`<wsdl:definitions>` rather than `<xs:schema>` and warns, then the plugin's WSDL mode binds the
embedded schema correctly anyway. Codegen completes and `AddRequest` is generated correctly (oracle
green). The net trade is a clear win: **7 cryptic `${tools.jar}` model warnings → 1 understood, benign
xjc warning.**

**Suppression via plugin `<args>` was tested and does NOT work — do not retry:** both `-wsdl` (the
message's own suggestion) and `-quiet` leave the warning intact, because it originates in the plugin's
WSDL-load bridge (`RawXJC2Mojo → XJC23Mojo → ModelLoader.sanityCheck`) *ahead of* where XJC arg
handling takes effect. Verified: the generated sources are byte-identical with or without these args
(only the generation-timestamp comment in each file header differs; classes, fields, `@Xml*`
annotations and the `javax.xml.bind` namespace are unchanged). Left as a documented residual — truly
removing it would require feeding XJC a standalone `.xsd` extracted from the WSDL (changes source
artifacts) or moving off this plugin family, both out of scope for a JDK hop. Recorded here so a future
reader does not mistake it for a finding or re-run the failed `-wsdl`/`-quiet` attempts.

---

# Modernization step 11 — the second 2.x minor: Boot 2.1 → 2.2 + Hoxton (Java 11 held)

> **Goal of this document:** the *eleventh* baby step, and the **second rung of the 2.x climb** §7 laid
> out (`Step 11  Boot 2.1 → 2.2  train Greenwich → Hoxton`). It moves exactly one Boot minor
> (`2.1.18.RELEASE → 2.2.x`), drags its version-locked Spring Cloud train along
> (`Greenwich.SR6 → Hoxton.SR12`), and **deliberately changes nothing else** — the JDK stays the Java
> 11 that Step 10 banked, the namespace stays `javax`. It runs on a throwaway branch, is fully
> reversible, and is graded by the same 18-test net Steps 9–10 carried green onto the
> 2.1/Greenwich/Java-11 plateau. As in Step 9, OpenRewrite is promoted from *advisor* to **applier**
> for the mechanical layer; everything it cannot express is hand-applied on top and recorded as a
> finding.

## Guiding principle

Step 9 was the *first* rung of the new plateau (one Boot minor, JDK held on 8); Step 10 was its mirror
(one JDK major, Boot held). Step 11 returns to the Step-9 cadence on the new working JDK: **one Boot
minor, JDK held on 11.** The whole point of having paid for the 2.0 generation jump (Step 8) and
banked Java 11 (Step 10) is that the rungs above are now *small* — each a single `UpgradeSpringBoot_2_x`
recipe under a green net, exactly the cadence §7 committed to.

The same iron rule from Step 1 still binds: **the Spring Cloud train is version-locked to the Boot
minor.** Boot `2.2.x` pairs with the **Hoxton** train (Boot `2.1.x` ↔ Greenwich, `2.2.x`/`2.3.x` ↔
Hoxton). So "bump Boot to 2.2" and "move the train to Hoxton" are not two steps — they are one atomic
hop, just as Finchley↔Greenwich was in Step 9. The `spring-cloud-netflix` family rides along to its
Hoxton line; **Hystrix/Ribbon/Zuul still exist here** — the Netflix cliff is a 2.4 problem (Step 14),
not a 2.2 one.

**The headline event of this minor: Boot 2.2 swaps the test starter's default engine to JUnit 5
(Jupiter).** §10 decided this fork explicitly — *vintage first, migrate later* — so this hop does
**not** migrate the suite to Jupiter. The discipline that keeps it a baby step: JUnit 4 tests must keep
running **unchanged** via `junit-vintage-engine`, and the only thing allowed to change about the test
layer is whatever the 2.2 starter reshuffle *forces* to keep vintage running. Nothing about test
*code* moves in this hop.

**What this hop unlocks but does not spend:** Hoxton also serves Boot 2.3 (the next minor, Step 12),
so landing on Hoxton here means Step 12's train move is a no-op SR re-pin, not a train rename — the
Greenwich→Hoxton rename is paid *once*, here. This step makes 2.3 a cheaper hop; it does **not** take
it.

## The JDK tension (read before running) — same two-toolchain split as Step 9, with the inverted gate JDK

Step 11 is the first Boot-minor hop *after* the Java-11 pivot, so it inherits **both** prior toolchain
lessons at once:

- **It has an OpenRewrite Boot-minor recipe** (`UpgradeSpringBoot_2_2`), and those recipes are compiled
  for **JDK 17+**. So, exactly as Step 9, the *applier* runs on a brief JDK-17 detour that touches only
  `pom.xml`.
- **The gate runs on JDK 11** — the working JDK Step 10 made permanent — **not** JDK 8 and **not** JDK
  17. This is the inverted Finding-0 trap Step 10 named: running the gate on the *old* Java 8 toolchain
  would silently re-bundle the JAXB modules Step 10 externalized and hide a regression; running it on
  JDK 17 would surface Step 18's strong-encapsulation errors prematurely. **Confirm `java -version`
  reports `11` before diagnosing any code.**

Two toolchains, two jobs (identical shape to Step 9, only the gate JDK changed `8 → 11`):

- **JDK 17** — *only* to run the OpenRewrite applier (`rewrite:run`).
- **JDK 11** — for the verification gate (`mvn clean package`) and the committed build forever after.

> The Java 11 illegal-reflective-access *warnings* (`WARNING: An illegal reflective access operation
> has occurred`) remain expected and are **not findings** (Step 10's JDK-tension note). Only an *error*
> is in scope, and `InaccessibleObjectException` errors are a JDK 16+ phenomenon — Step 18, not here.

## What Step 11 changes — layers, only the first is OpenRewrite's

Same three-to-four-layer shape as Step 9 (a minor, not a generation — each layer should be *thin*). The
table is the **plan**; layers 3–4 are sized by what the gate actually reports.

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. Boot parent + BOM-managed pins | parent `2.1.18.RELEASE → 2.2.x`; let the recipe re-settle any `<version>` the 2.2 BOM now manages | **OpenRewrite `rewrite:run`** (`UpgradeSpringBoot_2_2`) | tree diff + all probes |
| 2. Spring Cloud train | `<spring-cloud.version>` `Greenwich.SR6 → Hoxton.SR12` (version-locked to Boot 2.2) | **by hand** (thin recipe coverage — the Step 6/8/9 lesson) | tree diff + all probes |
| 3. Test characterization / JUnit-vintage glue | *only if the net flips or vintage stops running* — keep JUnit 4 on `junit-vintage-engine`; re-pin only an assertion the 2.2 minor deliberately changed | **by hand** (test files / test deps only) | the Step 5 net itself |
| 4. Generation-locked transitive fixes | *only if the gate throws* — the prime suspect is again `spring-boot-admin-starter-client` (still `2.0.6`; see anticipated frictions) | **by hand** (`pom.xml`) | the test/context that was crashing |

**Keep `spring-boot-properties-migrator` in place for this hop** (still in `pom.xml`, `runtime` scope
from Step 8). It is the cheapest oracle for layer 3 — it logs every `2.1 → 2.2` property rename at
startup. It is transitional and gets removed before the *target* is declared done, not before this
step.

**Do NOT** in this step: change `<java.version>` (stays `11` — Java 17 is Step 18); touch
`javax → jakarta`; bump beyond `2.2.x` / `Hoxton`; **migrate JUnit 4 → 5** (the whole point of the §10
*vintage-first* decision — keep `junit-vintage-engine` running the existing `@RunWith` tests
unchanged); re-architect the EOL `spring-security-oauth2` module, Netflix/Hystrix, or springfox (each
its own later hop).

## Anticipated frictions (predictions to verify by running the gate — **not** findings)

> Per the §6 discipline note, nothing below is a finding. Each earns the word "finding" only once a
> stack trace on *this* hop confirms it. Step 9's and Step 10's retrospectives are the template — Step
> 9's single highest-likelihood prediction (the Admin `2.0.6 → 2.1.x` bump) turned out **wrong** (Admin
> `2.0.6` ran fine on Boot 2.1). Expect the same humbling here: several of these may stay quiet.

| Region | Why 2.2 might disturb it | First oracle / where it would bite | Prior likelihood |
|---|---|---|---|
| **JUnit Vintage default swap** | Boot 2.2 makes the test starter default to **JUnit 5 (Jupiter)**. JUnit 4 tests run only via `junit-vintage-engine`, which 2.2's `spring-boot-starter-test` *still bundles transitively* (its removal is a **2.4** change — Step 14). So 2.2 *should* keep the vintage suite green with no change. | the whole suite — if vintage were absent, every `@RunWith(SpringRunner.class)` test would simply not be discovered (0 tests run, not a failure). | **Low–Medium** — likely runs unchanged at 2.2; the real vintage-glue work is a 2.4 problem. Verify the test count stays **18**, not 0. |
| **`spring-boot-admin-starter-client` (still `2.0.6`)** | Generation-locked to the Boot minor (Step 8 finding 2). It survived 2.1 (Step 9), but 2.2 may finally need the matching `2.2.x` Admin line. | context-init `NoClassDefFound` / `BeanCreationException` from a `de.codecentric...` class. | **Medium** — the most likely hard blocker, but Step 9 proved Admin is stickier than predicted. |
| **Mockito major bump** | Boot 2.2's BOM moves Mockito (≈ 2.x → 3.x). Any test using removed/changed Mockito API, or `Matchers` vs `ArgumentMatchers`, could break at compile/run. | a unit test using Mockito → compile error or `NoSuchMethodError`. | Low–Medium (watch) — only bites if a test touches the changed surface. |
| **`log4j-to-slf4j` exclusion** | Step 8 finding 1 excluded it from `spring-boot-starter-web`. Confirm the exclusion still resolves the same way under the 2.2 logging starter. | `LoggingBackendProbeTest` + every `@SpringBootTest` dies at static-init if it regresses. | Low–Medium |
| **`commons-io` pin (`2.13.0`)** | Step 8 finding 3 pinned it for Tika's `CloseShieldInputStream.wrap()`. The 2.2 BOM may re-pin `commons-io` — re-verify the live winner still carries `wrap()`. | `HelloControllerTest` Tika test → `NoSuchMethodError`. | Low |
| **`io.fabric8:spring-cloud-kubernetes-discovery:0.1.6`** | Pre-official K8s coordinate; the Hoxton train move could disturb its resolution/autoconfig. | dependency resolution or autoconfig break at context start. | Low (watch) |
| **Property-key renames** | Each minor deprecates/renames keys; the migrator will log them. | `spring-boot-properties-migrator` startup log + `@TestPropertySource` keys. | Low (likely none material, as in Steps 8–9) |

Regions the roadmap flags but that are **not** in scope here (confirm they stay quiet, do not act on
them): **springfox** (breaks at 2.6+ — Step 16); the **Netflix cliff** *and* the `junit-vintage-engine`
removal (both 2.4 — Step 14); the circuit-breaker net-widening (Step 12a); strong-encapsulation
`InaccessibleObjectException` **errors** (Java 17 — Step 18); the `javax → jakarta` rename (Boot 3 —
next target).

## Step 11 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Work on a throwaway branch

```powershell
git switch -c step11/boot-2.2-hoxton
```

### 1. Capture the "before" baseline under JDK 11 (the regression oracle)

The committed Step-10 state is green on **Java 11** — that is the known-good reference this hop is
graded against (note: JDK **11**, the inverted Finding-0 lesson, *not* Java 8 anymore).

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x

mvn -q dependency:tree "-DoutputFile=dependency-tree.step11-before.txt"
mvn -q test       # the full net + probes must be GREEN on 2.1/Greenwich/Java 11 first (18 tests)
```

Confirm **BUILD SUCCESS, 18 tests**. If the baseline is not green, stop — the hop is only gradable
against the known-good 2.1/Greenwich stack Steps 9–10 left behind.

### 2. Switch this shell to JDK 17 and run the OpenRewrite *applier* (layer 1)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'   # adjust to your JDK 17 install path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.x
```

Re-resolve the *current* plugin/recipe pair (do **not** trust these numbers — Step 6 §3 discipline), [https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/3.33.0/rewrite-recipe-bom-3.33.0.pom](https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/3.33.0/rewrite-recipe-bom-3.33.0.pom)
then run the applier with the **2.2** recipe:

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_2"
```

(fall back to `:runNoFork` if the JDK-17 fork fails — record it; the substantive edits are in
`pom.xml`.) Then **review the diff before trusting it**:

```powershell
git --no-pager diff -- pom.xml
```

Expect the parent at `2.2.x` and minor BOM re-settling. **Two guardrails when reading the patch:**
(a) verify `<java.version>` stayed `11` — the recipe should not touch it; if it does, revert that one
line; (b) if the recipe tries to *remove* or *migrate* JUnit-vintage glue or rewrite `@RunWith`
annotations toward Jupiter, **reject it** — §10 decided vintage-first, so the JUnit-4 surface stays
untouched in this hop.

### 3. Hand-apply the Spring Cloud train move (layer 2)

In `<properties>`, move the train (version-locked to Boot 2.2):

```xml
        <spring-cloud.version>Hoxton.SR12</spring-cloud.version>   <!-- was Greenwich.SR6 -->
```

Hand-applied because the recipe's train coverage is thin (Step 6/8/9 lesson). The
`spring-cloud-netflix` family moves to its Hoxton line as a consequence — the tree diff + probes gate
it. (Hystrix et al. still exist on Hoxton; nothing to re-architect here — that is Step 13/14.)

### 4. Run the gate, then hand-apply layers 3–4 *as the net dictates*

This is the heart of the hop, and it is a **loop**, not a one-shot edit (identical machinery to Step 9
§4). Do **not** pre-edit tests or pin transitives speculatively — the 2.2 minor may need *none* of
layers 3–4. The rule is: **change pom/tests only in response to a specific failure the gate just
printed.** One fix per failure, re-run, repeat, until green.

#### 4.0 — Switch back to JDK 11, run the gate once, capture the output

The OpenRewrite applier (step 2) left this shell on **JDK 17**. The gate must run on **JDK 11**
(inverted Finding-0 lesson) — pivot the toolchain back *now*, before the first gate run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
```

Then run the net and keep the log so you can read the *first* failure, not just the summary:

```powershell
mvn clean test 2>&1 | Tee-Object -FilePath step11-gate.log
```

- **BUILD SUCCESS, 18 tests, 0 failures** → layers 3–4 were empty for this minor. Skip straight to
  step 5 (the final `package` confirmation) and 6. (A plausible, good outcome for a single minor.)
- **0 tests run / "No tests were executed"** → this is the JUnit-vintage symptom, **not** a pass: the
  Jupiter default discovered nothing and vintage is missing. Go to 4.3 (add `junit-vintage-engine`).
- **Any failure** → go to 4.1. Always diagnose the **first** failure in the log; later ones are often
  cascades of the first (one broken context fails every `@SpringBootTest` after it).

#### 4.1 — Classify the failure: *hard failure* (→ layer 4, `pom.xml`) or *net flip* (→ layer 3, a test)?

This classification (unchanged from Step 9 §4.1) decides whether you touch `pom.xml` or a test file:

| | **Hard failure (→ layer 4, edit `pom.xml`)** | **Net flip (→ layer 3, edit a test)** |
|---|---|---|
| What you see | `NoClassDefFoundError`, `ClassNotFoundException`, `NoSuchMethodError`, `BeanCreationException`, context fails to load — **or 0 tests discovered** (vintage missing) | A clean assertion failure: `expected: <X> but was: <Y>` from one named test |
| What it means | The 2.2 framework removed/moved something a library, the context, or the test *engine* needs | Behaviour deliberately changed in 2.2; the test still ran, it just pinned the old value |
| Scope | Usually takes down **many** tests at once (or discovers none) | Usually **one** test, on one assertion |
| The fix | Bump/exclude/pin a dependency (or re-add the vintage engine) so the class/engine is back | Update the assertion to the new value **and** write down why it changed |

#### 4.2 — If it's a HARD FAILURE → fix the dependency (layer 4)

1. Read the exception and the class it names. Map the class to its owning artifact.
2. The **highest-likelihood case for this hop** is again Spring Boot Admin: a `NoClassDefFoundError` /
   `BeanCreationException` thrown from a `de.codecentric...` class during context init means
   `spring-boot-admin-starter-client:2.0.6` (which survived 2.1) has finally hit its generation wall at
   2.2 and must move to its 2.2 line. Edit the dependency in `pom.xml` (lines ~144–152):

   ```xml
   <dependency>
       <groupId>de.codecentric</groupId>
       <artifactId>spring-boot-admin-starter-client</artifactId>
       <version>2.2.4</version>   <!-- was 2.0.6: Admin is version-locked to the Boot minor -->
   </dependency>
   ```
   (Resolve the actual latest `2.2.x` Admin patch when you do it — `2.2.4` is the *expected* coordinate,
   not a guarantee. Only apply this **if the gate actually throws** — Step 9 proved Admin is stickier
   than the prediction.)
3. For any *other* hard failure, apply the same shape of fix the Step-8/9 findings used — exclude a
   colliding bridge, or pin a transitive to the version a caller needs. **Make exactly one change**,
   then go to 4.4. Do not batch several speculative pom edits.

#### 4.3 — If it's a NET FLIP or the VINTAGE GAP → layer 3 (tests / test deps)

Two distinct layer-3 cases:

**(a) Vintage gap (0 tests discovered).** If 4.0 reported *no tests run*, the 2.2 starter's Jupiter
default found nothing and `junit-vintage-engine` is not on the test classpath. Re-add it (BOM-managed —
no `<version>`), keeping the JUnit-4 suite running unchanged per §10:

```xml
<!-- Step 11 (Boot 2.2): the test starter now defaults to JUnit 5 (Jupiter). Keep the existing
     JUnit 4 suite running via the Vintage engine (vintage-first; Jupiter migration deferred). -->
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
</dependency>
```

(This is expected to be **unnecessary** at 2.2 — the starter still bundles vintage until 2.4 — so add
it *only* if the count actually drops to 0. If you do add it, note that the same dependency becomes
*mandatory* at Step 14.)

**(b) Genuine net flip (one assertion).** A net flip is the net *doing its job*: it caught that 2.2
changed a behaviour you had pinned. **Re-pin to the new reality**, do not "make it pass" blindly:

1. Open the named test and find the failing assertion.
2. Confirm the new value is a **legitimate, intended** 2.2 change (renamed property key, changed
   default, contract shape) — not a real regression. If you cannot explain *why* it changed, treat it
   as a finding to investigate before proceeding.
3. Update the assertion (or `@TestPropertySource` key) to the new value, with a one-line comment
   recording the flip — mirroring how Step 8 recorded the Actuator `/actuator/*` and OAuth2 `401→302`
   flips. The `spring-boot-properties-migrator` (still in `pom.xml`, runtime scope) logs renames at
   startup — grep `step11-gate.log` for its `WARN` lines rather than guessing.

#### 4.4 — Re-run and repeat

After **one** fix (4.2 or 4.3), re-run the gate command from 4.0 (`mvn clean test`, still on JDK 11).
Each pass should resolve one failure and reveal the next, if any. Record each fix as it lands in the
**Findings** section below. Stop when the run reports **BUILD SUCCESS, 18/18**, then go to step 5.

### 5. Confirm the full `package` under JDK 11

Once the test loop is green (18/18), confirm the *whole* build — including the fat-jar repackage and
the `maven-jaxb2-plugin` codegen, neither fully exercised by `mvn test` — still on JDK 11:

```powershell
java -version    # re-confirm: 11.0.x (inverted Finding-0: NOT Java 8, NOT Java 17)
mvn clean package
```

Expect **BUILD SUCCESS, 18 tests, 0 failures**, jar repackaged. If `package` fails where `test` passed
(e.g. the `spring-boot-maven-plugin` repackage or the `generate-arithmetics` codegen), treat it as a
hard failure and return to the step-4 loop.

### 6. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step11-after.txt"
Compare-Object (Get-Content dependency-tree.step11-before.txt) (Get-Content dependency-tree.step11-after.txt)
```

Expected movement is **moderate and coherent** for a minor: Boot `2.1.18 → 2.2.x`, Spring Framework
`5.1 → 5.2`, the train Greenwich → Hoxton (Netflix to its Hoxton line), the JUnit Jupiter/Vintage
engines appearing in the test scope, BOM-managed transitives (Mockito, etc.) moving with them, plus any
single deliberate adjustment from step 4. A **dropped framework**, an untouched pin moving,
`<java.version>` changing, or the JAXB API/runtime Step 10 added disappearing is a finding to
investigate. **Record the salient deltas below.**

## Findings (record after running Step 11)

> Predictions graded against reality (Steps 9–10 retrospective discipline). The first gate run on JDK
> 11 reported **`Tests run: 18, Failures: 1, Errors: 5`** — exactly two *distinct* root causes (the 5
> errors are one cascade), one of each kind the §4.1 table anticipates. Neither was the predicted
> highest-likelihood blocker.

**Predictions that held / were wrong:**

- **Admin `2.0.6` held again — prediction wrong (3rd time).** No `de.codecentric` / `NoClassDefFound`
  in the log; Admin `2.0.6` starts on Boot 2.2 just as it did on 2.0 (Step 8) and 2.1 (Step 9). Do
  **not** bump it. The §4.2 Admin-bump action stays unused.
- **JUnit Vintage stayed green at 18, not 0 — prediction held.** The count is `Tests run: 18`, not 0,
  so `spring-boot-starter-test` still bundles `junit-vintage-engine` transitively at 2.2 (its removal
  is the 2.4 problem, Step 14). The §4.3(a) vintage re-add is **not** needed here. *Vintage-first held
  with a zero-line test-config diff* — the headline 2.2 event was a non-event, as §10 expected.
- **Mockito / `commons-io` / `log4j-to-slf4j` / fabric8 — all quiet.** No `NoSuchMethodError` from the
  Tika path, no logging-probe regression, no fabric8 resolution break. The BOM moved them coherently.

The two real failures, classified and fixed below.

### Finding 1 (HARD FAILURE → layer 4, `pom.xml`): stale `tomcat.version` pin vs Boot 2.2's `Registry.disableRegistry()`

**Symptom.** Every web-context `@SpringBootTest` (`OAuth2SecurityCharacterizationTest` ×3,
`ActuatorEndpointCharacterizationTest` ×2 — the 5 cascaded errors) fails identically at context start:

```
ApplicationContextException: Unable to start web server; nested exception is
java.lang.NoSuchMethodError: 'void org.apache.tomcat.util.modeler.Registry.disableRegistry()'
```

**Root cause.** Boot **2.2** introduced a startup optimization: it **disables Tomcat's MBean Registry
by default**, calling `Registry.disableRegistry()` from `TomcatWebServer`. That method exists in the
Tomcat **9.0.x** line Boot 2.2's BOM manages — but `pom.xml` carries a Boot-1.5-era override,
`<tomcat.version>8.5.34</tomcat.version>` (pom line ~39, comment: *"the parent ships 8.5.23"*), which
forces an old **Tomcat 8.5.34** that predates the method. This is a **stale pin from an earlier era now
colliding with the new framework** — the same shape as the Step-8 findings. It stayed quiet in Steps
9–10 precisely because **2.1 did not call `disableRegistry()`**; only 2.2 does.

**Fix — remove the stale override; let Boot 2.2's BOM manage Tomcat.** The pin's original reason (lift
8.5.23 → 8.5.34 for a Boot-1.5 CVE concern) is now obsolete: Boot 2.2's managed Tomcat 9.0.x is *newer*
than 8.5.34, so the pin is a pure cross-major **downgrade** (8.5 → 9.0) with no remaining benefit.
Deleting it keeps all `tomcat-embed-*` artifacts moving coherently with the BOM and keeps this hop's
diff "about Boot". Remove the property **and its now-stale comment** (pom lines ~35–39):

```xml
<!-- DELETE — Boot-1.5-era override; Boot 2.2's BOM-managed Tomcat 9.0.x supersedes it, and
     8.5.34 lacks Registry.disableRegistry() that Boot 2.2 calls at startup. -->
<tomcat.version>8.5.34</tomcat.version>
```

After deletion, `tomcat-embed-core` should appear in the after-tree at the BOM's **9.0.x** (e.g.
`9.0.39` for Boot `2.2.13.RELEASE` — confirm with `dependency:tree`, do not trust the number). This is
a deliberate, recorded version movement — not the "untouched pin moving" the §6 oracle warns about.

> Do **not** instead bump the pin to a newer 8.5.x. That would keep a redundant override fighting the
> BOM for no reason; the methodology-clean move is to retire the Boot-1.5 wart entirely.

### Finding 2 (NET FLIP → layer 3, `JavaeeApiProbeTest`): annotation API moved to the `jakarta.annotation` groupId

**Symptom.** One clean assertion failure (the single `Failures: 1`):

```
JavaeeApiProbeTest.whichJarProvidesEachJavaxApi:53
  javax.annotation.Resource expected from javax.annotation-api
  but was: .../jakarta/annotation/jakarta.annotation-api/1.3.5/jakarta.annotation-api-1.3.5.jar
```

**Root cause — a legitimate, intended Boot 2.2 change, not a regression.** Boot 2.2's BOM replaced the
managed JSR-250 annotation API coordinate `javax.annotation:javax.annotation-api` with
`jakarta.annotation:jakarta.annotation-api:1.3.5`. Critically — and exactly as Step 10's JAXB hunk-3
analysis warned — `jakarta.annotation-api:1.3.5` is the **Jakarta EE 8** artifact: **renamed groupId,
but still the `javax.annotation.*` packages** (the package rename to `jakarta.annotation.*` only lands
at `2.0+`). So `javax.annotation.Resource` still resolves and the runtime is unaffected; only the
*provenance jar* changed. The probe (re-pinned in Step 10 to `javax.annotation-api`) correctly caught
the BOM-driven provenance flip — the net doing its job.

**Fix — re-pin the probe's expected marker to the new reality**, with a comment recording the flip
(mirroring the Step-10 re-pin two lines above it). In
`src/test/java/com/acme/myproduct/JavaeeApiProbeTest.java`, line 36:

```java
// Step 11 (Boot 2.2): the BOM swapped the managed JSR-250 annotation API from
// javax.annotation:javax.annotation-api to jakarta.annotation:jakarta.annotation-api:1.3.5 — a
// renamed groupId that STILL ships the javax.annotation.* packages (Jakarta EE 8; the package
// rename is a 3.0/jakarta-namespace concern, not here). Same class, new provenance jar. Re-pinned.
expected.put("javax.annotation.Resource", "jakarta.annotation-api");   // was "javax.annotation-api"
```

`location.contains("jakarta.annotation-api")` matches `jakarta.annotation-api-1.3.5.jar`. No production
code changes; the `javax` namespace is untouched (jakarta-*namespace* is still a Boot 3 concern).

### The loop outcome

Both fixes are independent (one pom delete, one test re-pin) and each traces to a distinct trace —
apply both, then re-run the gate from §4.0 on **JDK 11**:

```powershell
mvn clean test 2>&1 | Tee-Object -FilePath step11-gate.log
```

Expect **BUILD SUCCESS, 18/18**: Finding 1 clears the 5 cascaded context-start errors, Finding 2
clears the lone assertion failure. Then proceed to §5 (`mvn clean package`) and §6 (after-tree diff —
where `tomcat-embed-*` 8.5.34 → 9.0.x and the `jakarta.annotation-api` node are the expected,
recorded deltas).

## Step 11 — done criteria (all must hold)

1. `mvn clean package` under **JDK 11** → **BUILD SUCCESS**, fat jar repackaged, `<java.version>` still
   `11`, `mvn test` green **18/18** (not 0 — vintage discovery confirmed).
2. The net flipped **only where predicted-or-explained**; no *unexplained* red survived. JAXB (the
   Step-10 re-internalized API/runtime), the JWT/bcpkix path, `LoggingBackendProbeTest`, the BC probes,
   and the OAuth2 `302` shape all stay as Steps 8–10 left them unless a flip is explicitly recorded.
3. The OpenRewrite layer-1 diff is reviewed; layers 2–4 are the only hand-applied changes, each
   traceable to a finding above. **No test code migrated to Jupiter** — vintage-first held.
4. Parent is `2.2.x`, `<spring-cloud.version>` is `Hoxton.SR12`, and the before/after tree diff shows
   Boot/Spring/Spring-Cloud moving together to the 2.2/Hoxton generation with nothing unrelated
   dropped.
5. **Java 17 remains untaken** — `<java.version>` is still `11`; that switch is the Step 18 capstone.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step11/boot-2.2-hoxton
```

Delete `dependency-tree.step11-before.txt` / `dependency-tree.step11-after.txt` if not keeping them.

## Toward Step 12

With the platform on `Boot 2.2.x + Hoxton` and the net green on Java 11, the next rung is the **last
quiet minor before the Netflix cliff**:

- **Step 12 — Boot 2.2 → 2.3** (Java 11 held). Because Hoxton *also* serves Boot 2.3, this hop's train
  move is a no-op SR re-pin (stay on `Hoxton.SR12`), not a rename — the Greenwich→Hoxton rename was
  already paid in Step 11. That makes Step 12 unusually thin: essentially `UpgradeSpringBoot_2_3` plus
  whatever the net flips.
- **Then the cliff approaches.** §7 places the **circuit-breaker characterization knot (Step 12a)** and
  the **Hystrix → Resilience4j re-architecture (Step 13)** immediately after, *before* the 2.4 boundary
  (Step 14) where `netflix-hystrix` / `netflix-core` cease to exist and `junit-vintage-engine` leaves
  the test starter's default. Step 11 deliberately leaves all three untouched — it is a quiet minor, and
  the heavy, separately-gated work is staged for where the framework actually forces it.

> **Open question to settle at Step 12 planning (raised, not decided here):** whether 2.2 and 2.3 — both
> on Hoxton, both quiet — could be *fused* into one hop. The §7 plan lists them separately (baby-step
> default), and the recommendation stands: **keep them separate** unless Step 12's up-front analysis
> shows 2.3's diff is genuinely empty, in which case fusing is defensible. Decide it there, with the
> tree in hand — not now.

---

# Modernization step 12 — the third 2.x minor: Boot 2.2 → 2.3 (Hoxton held, Java 11 held)

> **Goal of this document:** the *twelfth* baby step, and the **third rung of the 2.x climb** §7 laid
> out (`Step 12  Boot 2.2 → 2.3  train Hoxton (SR)`). It moves exactly one Boot minor
> (`2.2.x → 2.3.x`) and **deliberately changes nothing else** — the Spring Cloud train **stays on
> `Hoxton.SR12`** (Hoxton serves *both* 2.2 and 2.3, so this hop's train move is a no-op re-pin, not a
> rename — the Greenwich→Hoxton rename was already paid in Step 11), the JDK stays the Java 11 Step 10
> banked, the namespace stays `javax`. It runs on a throwaway branch, is fully reversible, and is graded
> by the same 18-test net Steps 9–11 carried green onto the 2.2/Hoxton/Java-11 plateau. As in Steps
> 9/11, OpenRewrite is promoted from *advisor* to **applier** for the mechanical layer; everything it
> cannot express is hand-applied on top and recorded as a finding.

## Guiding principle

Step 11 returned to the Step-9 cadence on the new working JDK (one Boot minor, JDK held on 11). Step 12
is the **thinnest hop of the whole climb so far** — and for a concrete, structural reason: **Boot 2.3
is still Spring Framework 5.2**, exactly as 2.2 was. The two minors share a Spring generation, so the
diff carries no Spring-core movement at all; what moves is only Boot's own auto-configuration layer,
its BOM-managed transitives, and the one starter Boot 2.3 deliberately re-shaped (see frictions). This
is the *last quiet minor before the Netflix cliff* — the calm immediately before Steps 12a/13/14 do the
heavy, separately-gated work.

**The train is the tell that this is a half-hop.** The iron rule from Step 1 still binds — the Spring
Cloud train is version-locked to the Boot minor — but the lock here points at the *same station*:
`Hoxton` covers Boot 2.2 **and** 2.3, and `Hoxton.SR12` is already the final Hoxton service release. So
unlike Steps 9 (Finchley→Greenwich) and 11 (Greenwich→Hoxton), there is **no `<spring-cloud.version>`
edit** in this hop. The `spring-cloud-netflix` family does not move generation; Hystrix/Ribbon/Zuul
still exist on Hoxton — the Netflix cliff is the 2.4 problem (Step 14), not a 2.3 one.

**The fuse question, decided here with the tree in hand (per Step 11's "Toward Step 12").** Step 11
asked whether 2.2 and 2.3 — both on Hoxton, both quiet — could be *fused* into one hop. **Decision:
keep them separate, as taken.** Two reasons survive contact with the analysis: (1) Boot 2.3 is *not*
behaviourally empty — it re-shaped `spring-boot-starter-web` (the validation-starter split, below),
which is a real, characterizable change that deserves its own clean diff and its own gate; and (2) the
baby-step default wins ties — fusing would only have saved one `mvn` cycle while muddying which minor
introduced any flip. The cost of separateness is one extra gated cycle; the benefit is a diff that says
exactly "2.2→2.3" and nothing else. Cheap insurance, taken.

**What this hop unlocks but does not spend:** landing 2.3 puts the platform on the *doorstep of the
2.4 boundary* — where the Netflix stack is dismantled and `junit-vintage-engine` leaves the test
starter's default. §7 deliberately inserts the **circuit-breaker characterization knot (Step 12a)** and
the **Hystrix → Resilience4j re-architecture (Step 13)** *before* that boundary. Step 12 makes the cliff
*reachable*; it does **not** approach it. Nothing about Hystrix, the net-widening, or the vintage glue
moves in this hop.

## The JDK tension (read before running) — identical to Step 11

Step 12 inherits Step 11's exact two-toolchain split, unchanged, because it has the same shape (a
Boot-minor hop with an OpenRewrite recipe, on the Java-11 plateau):

- **It has an OpenRewrite Boot-minor recipe** (`UpgradeSpringBoot_2_3`), compiled for **JDK 17+**. So,
  exactly as Step 11, the *applier* runs on a brief JDK-17 detour that touches only `pom.xml`.
- **The gate runs on JDK 11** — the working JDK Step 10 made permanent — **not** JDK 8 and **not** JDK
  17. This is the inverted Finding-0 trap: running the gate on the *old* Java 8 toolchain would silently
  re-bundle the JAXB modules Step 10 externalized and hide a regression; running it on JDK 17 would
  surface Step 18's strong-encapsulation errors prematurely. **Confirm `java -version` reports `11`
  before diagnosing any code.**

Two toolchains, two jobs (identical to Step 11):

- **JDK 17** — *only* to run the OpenRewrite applier (`rewrite:run`).
- **JDK 11** — for the verification gate (`mvn clean package`) and the committed build forever after.

> The Java 11 illegal-reflective-access *warnings* (`WARNING: An illegal reflective access operation
> has occurred`) remain expected and are **not findings** (Step 10's JDK-tension note). Only an *error*
> is in scope, and `InaccessibleObjectException` errors are a JDK 16+ phenomenon — Step 18, not here.

## What Step 12 changes — layers, only the first is OpenRewrite's

Same three-to-four-layer shape as Steps 9/11, but **layer 2 is empty this time** (the train does not
move). The table is the **plan**; layers 3–4 are sized by what the gate actually reports.

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. Boot parent + BOM-managed pins | parent `2.2.x → 2.3.x`; let the recipe re-settle any `<version>` the 2.3 BOM now manages | **OpenRewrite `rewrite:run`** (`UpgradeSpringBoot_2_3`) | tree diff + all probes |
| 2. Spring Cloud train | **no change** — `Hoxton.SR12` already serves Boot 2.3 (the rename was paid in Step 11). Confirm it stays put; do **not** edit it. | — (no-op) | tree diff |
| 3. Test characterization updates | *only if the net flips* — a property-key rename or a deliberate contract change the 2.3 minor introduces | **by hand** (test files only) | the Step 5 net itself |
| 4. Generation-locked transitive / starter fixes | *only if the gate throws* — the prime suspect is the **validation-starter split** (Boot 2.3 removed Hibernate Validator from `spring-boot-starter-web`); Admin (`2.0.6`) is the secondary suspect | **by hand** (`pom.xml`) | the test/context that was crashing |

**Keep `spring-boot-properties-migrator` in place for this hop** (still in `pom.xml`, `runtime` scope
from Step 8). It is the cheapest oracle for layer 3 — it logs every `2.2 → 2.3` property rename at
startup. It is transitional and gets removed before the *target* is declared done, not before this step.

**Do NOT** in this step: change `<java.version>` (stays `11` — Java 17 is Step 18); touch
`javax → jakarta`; **edit `<spring-cloud.version>`** (stays `Hoxton.SR12`); bump beyond `2.3.x`;
migrate JUnit 4 → 5 (still vintage-first, §10); re-architect the EOL `spring-security-oauth2` module,
Netflix/Hystrix, or springfox; or add the circuit-breaker net-widening (that is Step 12a).

## Anticipated frictions (predictions to verify by running the gate — **not** findings)

> Per the §6 discipline note, nothing below is a finding. Each earns the word "finding" only once a
> stack trace on *this* hop confirms it. Steps 9–11's retrospectives are the template — three times now
> the predicted highest-likelihood blocker (the Admin bump) turned out **wrong**, and the real Step-11
> findings (the stale `tomcat.version` pin; the `jakarta.annotation-api` provenance flip) were *not*
> top-of-list predictions. Expect the same humbling here.

| Region | Why 2.3 might disturb it | First oracle / where it would bite | Prior likelihood |
|---|---|---|---|
| **Validation starter split** | **Boot 2.3 removed Hibernate Validator from `spring-boot-starter-web`** — `spring-boot-starter-validation` is no longer pulled transitively. Any code or test relying on `javax.validation` / `@Valid` / a `Validator` bean being auto-present can break. | a `@SpringBootTest` whose context wires a validator, or a controller using `@Valid` → `NoClassDefFoundError: javax/validation/...` or a missing-bean failure at context start. | **Medium–High** — the single most likely 2.3-specific blocker; fix is to add `spring-boot-starter-validation` explicitly **only if** the gate names it. |
| **`spring-boot-admin-starter-client` (still `2.0.6`)** | Generation-locked to the Boot minor (Step 8 finding 2). It survived 2.0, 2.1, **and 2.2** — but each minor is a fresh roll of the dice. | context-init `NoClassDefFound` / `BeanCreationException` from a `de.codecentric...` class. | **Low–Medium** — prediction has been wrong 3× running; do not pre-bump. |
| **Actuator / management contract** | Boot 2.3 added liveness/readiness groups and reworked some actuator internals. A characterized actuator assertion could flip. | `ActuatorEndpointCharacterizationTest` → an assertion change on an `/actuator/*` shape. | Low–Medium |
| **Property-key renames** | Each minor deprecates/renames keys; the migrator will log them (e.g. some `server.*` / `management.*` keys shift around 2.3). | `spring-boot-properties-migrator` startup log + `@TestPropertySource` keys. | Low (likely none material, as in Steps 8–11) |
| **`commons-io` pin (`2.13.0`) / `log4j-to-slf4j` exclusion** | Step 8 findings 1 & 3. Each Boot minor re-pins these in its BOM — re-verify the Tika `wrap()` path and the logging-backend probe still resolve. | `HelloControllerTest` Tika test; `LoggingBackendProbeTest`. | Low |
| **`io.fabric8:spring-cloud-kubernetes-discovery:0.1.6`** | Pre-official K8s coordinate; even a no-op train re-pin re-resolves the graph. | dependency resolution or autoconfig break at context start. | Low (watch) |

Regions the roadmap flags but that are **not** in scope here (confirm they stay quiet, do not act on
them): **springfox** (breaks at 2.6+ — Step 16); the **Netflix cliff** *and* the `junit-vintage-engine`
removal (both 2.4 — Step 14); the circuit-breaker net-widening (Step 12a); strong-encapsulation
`InaccessibleObjectException` **errors** (Java 17 — Step 18); the `javax → jakarta` rename (Boot 3 —
next target).

## Step 12 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Work on a throwaway branch

```powershell
git switch -c step12/boot-2.3
```

### 1. Capture the "before" baseline under JDK 11 (the regression oracle)

The committed Step-11 state is green on **Java 11** — that is the known-good reference this hop is
graded against (JDK **11**, the inverted Finding-0 lesson, *not* Java 8).

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x

mvn -q dependency:tree "-DoutputFile=dependency-tree.step12-before.txt"
mvn -q test       # the full net + probes must be GREEN on 2.2/Hoxton/Java 11 first (18 tests)
```

Confirm **BUILD SUCCESS, 18 tests**. If the baseline is not green, stop — the hop is only gradable
against the known-good 2.2/Hoxton stack Step 11 left behind.

### 2. Switch this shell to JDK 17 and run the OpenRewrite *applier* (layer 1)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'   # adjust to your JDK 17 install path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.x
```

Re-resolve the *current* plugin/recipe pair (do **not** trust these numbers — Step 6 §3 discipline),
then run the applier with the **2.3** recipe:

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_3"
```

(fall back to `:runNoFork` if the JDK-17 fork fails — record it; the substantive edits are in
`pom.xml`.) Then **review the diff before trusting it**:

```powershell
git --no-pager diff -- pom.xml
```

Expect the parent at `2.3.x` and minor BOM re-settling. **Three guardrails when reading the patch:**
(a) verify `<java.version>` stayed `11` — the recipe should not touch it; if it does, revert that one
line; (b) verify `<spring-cloud.version>` stayed `Hoxton.SR12` — the recipe should not touch the train,
and this hop deliberately does not move it; (c) if the recipe tries to migrate `@RunWith` toward Jupiter
or strip vintage glue, **reject it** — §10 decided vintage-first.

### 3. (No layer 2 — the train does not move)

Confirm, do not edit: `<spring-cloud.version>` stays `Hoxton.SR12`. Unlike Steps 9 and 11, there is no
train rename in this hop — Hoxton serves Boot 2.3, and SR12 is its final service release. Skip straight
to the gate.

### 4. Run the gate, then hand-apply layers 3–4 *as the net dictates*

This is the heart of the hop, and it is a **loop**, not a one-shot edit (identical machinery to Steps
9/11 §4). Do **not** pre-edit tests or pin transitives speculatively — the 2.3 minor may need *none* of
layers 3–4. The rule is: **change pom/tests only in response to a specific failure the gate just
printed.** One fix per failure, re-run, repeat, until green.

#### 4.0 — Switch back to JDK 11, run the gate once, capture the output

The OpenRewrite applier (step 2) left this shell on **JDK 17**. The gate must run on **JDK 11**
(inverted Finding-0 lesson) — pivot the toolchain back *now*, before the first gate run:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
```

Then run the net and keep the log so you can read the *first* failure, not just the summary:

```powershell
mvn clean test 2>&1 | Tee-Object -FilePath step12-gate.log
```

- **BUILD SUCCESS, 18 tests, 0 failures** → layers 3–4 were empty for this minor. Skip straight to
  step 5 (the final `package` confirmation) and 6. (For this unusually thin Spring-5.2-held hop, a clean
  pass is a genuinely plausible outcome.)
- **Any failure** → go to 4.1. Always diagnose the **first** failure in the log; later ones are often
  cascades of the first (one broken context fails every `@SpringBootTest` after it).

#### 4.1 — Classify the failure: *hard failure* (→ layer 4, `pom.xml`) or *net flip* (→ layer 3, a test)?

This classification (unchanged from Steps 9/11 §4.1) decides whether you touch `pom.xml` or a test file:

| | **Hard failure (→ layer 4, edit `pom.xml`)** | **Net flip (→ layer 3, edit a test)** |
|---|---|---|
| What you see | `NoClassDefFoundError`, `ClassNotFoundException`, `NoSuchMethodError`, `BeanCreationException`, context fails to load | A clean assertion failure: `expected: <X> but was: <Y>` from one named test |
| What it means | The 2.3 framework removed/moved something a library or the context needs | Behaviour deliberately changed in 2.3; the test still ran, it just pinned the old value |
| Scope | Usually takes down **many** tests at once (the context can't start) | Usually **one** test, on one assertion |
| The fix | Bump/exclude/pin/add a dependency so the class is back on the classpath | Update the assertion to the new value **and** write down why it changed |

#### 4.2 — If it's a HARD FAILURE → fix the dependency (layer 4)

1. Read the exception and the class it names. Map the class to its owning artifact.
2. The **highest-likelihood case for this hop** is the **validation-starter split**: a
   `NoClassDefFoundError: javax/validation/...`, a missing `Validator` bean, or a context failure tied to
   `@Valid`/JSR-303 means Boot 2.3 stopped pulling Hibernate Validator through `spring-boot-starter-web`.
   The fix is to re-internalize it explicitly (BOM-managed — no `<version>`):

   ```xml
   <!-- Step 12 (Boot 2.3): spring-boot-starter-web no longer pulls Bean Validation transitively.
        Re-add the validation starter explicitly so @Valid / javax.validation stays satisfied. -->
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-validation</artifactId>
   </dependency>
   ```
   (Add this **only if the gate actually names a validation/`javax.validation` failure** — do not add it
   speculatively. If nothing in the app touches validation, the split is a non-event.)
3. The secondary suspect is again Spring Boot Admin — a `de.codecentric...` `NoClassDefFound` would mean
   `spring-boot-admin-starter-client:2.0.6` has finally hit its generation wall at 2.3 and must move to
   its `2.3.x` line. Apply the same one-line version bump Steps 9/11 described — **only if the gate
   throws** (the prediction has been wrong 3× running).
4. For any *other* hard failure, apply the same shape of fix the Step-8/9/11 findings used — exclude a
   colliding bridge, or pin/add a transitive to the version a caller needs. **Make exactly one change**,
   then go to 4.4. Do not batch several speculative pom edits.

#### 4.3 — If it's a NET FLIP → update the characterization (layer 3)

A net flip is the net *doing its job*: it caught that 2.3 changed a behaviour you had pinned. **Re-pin
to the new reality**, do not "make it pass" blindly:

1. Open the named test and find the failing assertion.
2. Confirm the new value is a **legitimate, intended** 2.3 change (a renamed property key, a changed
   actuator default/shape, a BOM-driven provenance flip like Step 11's `jakarta.annotation-api`) — not a
   real regression. If you cannot explain *why* it changed, treat it as a finding to investigate before
   proceeding, **not** an assertion to overwrite.
3. Update the assertion (or `@TestPropertySource` key) to the new value, with a one-line comment
   recording the flip — mirroring how Step 11 recorded the `jakarta.annotation-api` provenance flip and
   Step 8 the Actuator `/actuator/*` and OAuth2 `401→302` flips. The `spring-boot-properties-migrator`
   (still in `pom.xml`, runtime scope) logs renames at startup — grep `step12-gate.log` for its `WARN`
   lines rather than guessing.

#### 4.4 — Re-run and repeat

After **one** fix (4.2 or 4.3), re-run the gate command from 4.0 (`mvn clean test`, still on JDK 11).
Each pass should resolve one failure and reveal the next, if any. Record each fix as it lands in the
**Findings** section below. Stop when the run reports **BUILD SUCCESS, 18/18**, then go to step 5.

### 5. Confirm the full `package` under JDK 11

Once the test loop is green (18/18), confirm the *whole* build — including the fat-jar repackage and
the `maven-jaxb2-plugin` codegen, neither fully exercised by `mvn test` — still on JDK 11:

```powershell
java -version    # re-confirm: 11.0.x (inverted Finding-0: NOT Java 8, NOT Java 17)
mvn clean package
```

Expect **BUILD SUCCESS, 18 tests, 0 failures**, jar repackaged. If `package` fails where `test` passed
(e.g. the `spring-boot-maven-plugin` repackage or the `generate-arithmetics` codegen), treat it as a
hard failure and return to the step-4 loop.

### 6. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step12-after.txt"
Compare-Object (Get-Content dependency-tree.step12-before.txt) (Get-Content dependency-tree.step12-after.txt)
```

Expected movement is **small and Boot-only** for this Spring-5.2-held minor: Boot `2.2.x → 2.3.x`,
**Spring Framework unchanged at 5.2**, the train **unchanged at Hoxton.SR12** (Netflix line does not
move), BOM-managed transitives moving with Boot, plus any single deliberate adjustment from step 4
(e.g. a newly-explicit `spring-boot-starter-validation` node). A **moved Spring-Framework or train
version**, a **dropped framework**, an untouched pin moving, `<java.version>` changing, or the JAXB
API/runtime Step 10 added disappearing is a finding to investigate. **Record the salient deltas below.**

## Findings (record after running Step 12)

> Predictions graded against reality (Steps 9–11 retrospective discipline). The first gate run on JDK
> 11 (parent landed at **`2.3.12.RELEASE`**) reported **`Tests run: 18, Failures: 1, Errors: 0`** —
> exactly **one** clean assertion failure, a net flip, and **zero** hard failures. The single most
> likely 2.3-specific friction (the validation-starter split) was the one that bit — but it surfaced as
> a *net flip*, not the hard `NoClassDefFound` the §4.2 table anticipated.

**Predictions that held / were wrong:**

- **Admin `2.0.6` held again — prediction wrong (4th time).** No `de.codecentric` / `NoClassDefFound`
  in the log; Admin `2.0.6` starts on Boot 2.3 just as it did on 2.0/2.1/2.2. Do **not** bump it. The
  §4.2 Admin-bump action stays unused.
- **JUnit Vintage stayed green at 18, not 0 — prediction held.** `Tests run: 18`, not 0, so
  `spring-boot-starter-test` still bundles `junit-vintage-engine` transitively at 2.3 (its removal is
  the 2.4 problem, Step 14). Vintage-first held with a zero-line test-config diff.
- **Actuator / `commons-io` / `log4j-to-slf4j` / fabric8 — all quiet.** No actuator-contract flip, no
  Tika `NoSuchMethodError`, no logging-probe regression; the fabric8 *"Can't read configMap"* WARN is
  the same benign no-Kubernetes-runtime log as prior hops, not a resolution break. The BOM moved
  everything coherently; Tomcat re-settled to the BOM's **9.0.46** (Step 11's pin-removal holds).
- **No train move — confirmed.** `<spring-cloud.version>` stayed `Hoxton.SR12`; the Netflix family
  rode along on its Hoxton line (the `CoreAutoConfiguration ... This module is deprecated` WARN is the
  expected Hoxton-era deprecation notice, *not* the 2.4 removal — the cliff is Step 14).

### Finding 1 (NET FLIP → layer 3, `JavaeeApiProbeTest`): the validation-starter split — a non-event for this app

**Symptom.** One clean assertion failure (the single `Failures: 1`):

```
JavaeeApiProbeTest.whichJarProvidesEachJavaxApi:57
  javax.validation.Validation expected from validation-api
  but was: file:/.../javax/javaee-api/8.0/javaee-api-8.0.jar
```

**Root cause — a legitimate, intended Boot 2.3 change, not a regression.** Boot 2.3 **removed Bean
Validation from `spring-boot-starter-web`** (the "validation-starter split"): the web starter no longer
pulls `jakarta.validation:jakarta.validation-api:2.0.2` + `org.hibernate.validator:hibernate-validator:6.0.22.Final`
transitively (both present in the before-tree, gone from the after-tree). On 2.2 the probe's
`javax.validation.Validation` was served by that dragged-in `jakarta.validation-api-2.0.2.jar` (whose
filename `.contains("validation-api")` — the same renamed-groupId-still-`javax`-packages pattern as
Step 11's `jakarta.annotation-api`). On 2.3, with that jar gone, the class falls through to the
**`javaee-api:8.0` API stubs**, which still ship `javax.validation.*` — so `Class.forName` resolves
fine (no hard failure; every context still started), only the *provenance jar* changed.

**Why this is a non-event and the validation starter was NOT re-added.** A grep of the entire `src`
tree for any Bean Validation surface (`@Valid`, `javax.validation`, `@NotNull`/`@Size`/…, a `Validator`
bean, `ConstraintValidator`) returns **exactly one hit — the probe line itself**. The app wires no
validation anywhere, so losing the Hibernate Validator *runtime* regresses nothing — which is precisely
why all 18 contexts started clean with zero hard failures. Re-adding `spring-boot-starter-validation`
(the §4.2 layer-4 action) would re-introduce an **unused** dependency purely to satisfy a probe
assertion, fighting the BOM's deliberate simplification. The methodology-clean move is §4.3: **re-pin
the probe to the new reality**, exactly as Step 11 re-pinned the `jakarta.annotation-api` flip.

**Fix — re-pin the probe's expected marker** (`JavaeeApiProbeTest.java`, the `javax.validation.Validation`
entry), with a comment recording the flip:

```java
// Step 12 (Boot 2.3): spring-boot-starter-web no longer pulls Bean Validation transitively
// (the validation-starter split) ... the app uses no @Valid / javax.validation anywhere, so losing
// the Hibernate Validator runtime is a non-event ... Re-pinned; NOT re-adding the validation starter.
expected.put("javax.validation.Validation", "javaee-api");   // was "validation-api"
```

### The loop outcome

One fix (a single test re-pin; zero `pom.xml` edits beyond the OpenRewrite layer-1 parent bump) — re-run
the gate from §4.0 on **JDK 11**:

```
Tests run: 18, Failures: 0, Errors: 0   →   BUILD SUCCESS (Spring Boot 2.3.12.RELEASE)
```

The after-tree's expected, recorded deltas: Boot `2.2 → 2.3.12`, **Spring Framework held at 5.2**, the
train **held at Hoxton.SR12**, Tomcat re-settled to `9.0.46`, and `jakarta.validation-api:2.0.2` +
`hibernate-validator:6.0.22.Final` **dropping out** (the validation-starter split). This was the
thinnest hop of the climb so far — a single provenance re-pin — exactly as the Spring-5.2-held analysis
predicted.

## Step 12 — done criteria (all must hold)

1. `mvn clean package` under **JDK 11** → **BUILD SUCCESS**, fat jar repackaged, `<java.version>` still
   `11`, `mvn test` green **18/18**.
2. The net flipped **only where predicted-or-explained**; no *unexplained* red survived. JAXB (the
   Step-10 re-internalized API/runtime), the JWT/bcpkix path, `LoggingBackendProbeTest`, the BC probes,
   and the OAuth2 `302` shape all stay as Steps 8–11 left them unless a flip is explicitly recorded.
3. The OpenRewrite layer-1 diff is reviewed; layers 3–4 are the only hand-applied changes, each
   traceable to a finding above. **No test code migrated to Jupiter** — vintage-first held.
4. Parent is `2.3.x`, `<spring-cloud.version>` is **still `Hoxton.SR12`** (unchanged), and the
   before/after tree diff shows Boot moving `2.2 → 2.3` with **Spring Framework held at 5.2** and the
   train held on Hoxton — nothing unrelated dropped.
5. **Java 17 remains untaken** — `<java.version>` is still `11`; that switch is the Step 18 capstone.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step12/boot-2.3
```

Delete `dependency-tree.step12-before.txt` / `dependency-tree.step12-after.txt` if not keeping them.


## Target reached: **Boot 2.3.12 + Java 11**

With Step 12 green, this target is **complete**. The platform stands at:

- **Spring Boot `2.3.12.RELEASE`** (Spring Framework 5.2), **Spring Cloud `Hoxton.SR12`**, **Java 11**,
  namespace still `javax`.
- The **18-test characterization net green** end-to-end (`mvn clean package` on JDK 11), every flip along
  the way predicted-or-explained and recorded as a finding.
- The Netflix stack (`netflix-hystrix`/`netflix-core`, `feign-hystrix:10.12`) still present and intact —
  the cliff is deliberately untouched.

This is the **last quiet `javax` plateau** before the framework forces behaviour-bearing change. It is a
clean, durable resting point: a fully reversible, fully graded climb of three Boot minors and one JDK
major from where Step 8 left off.

## Toward the next target — **Boot 2.7.x + Java 17**

The climb continues in its own roadmap: **[MODERNIZATION_BOOT_2.7_Java_17.md](MODERNIZATION_BOOT_2.7_Java_17.md)**.
That target takes the platform from *this* landing state up the rest of the Boot 2.x line to 2.7,
re-architects the Netflix stack (Hystrix → Resilience4j), drags the train to 2021.0 (Jubilee), and
finishes with the Java 11 → 17 capstone — so that the Boot 3 + `jakarta` jump after it is left as a
*pure* namespace migration.

Its **first** step is the prerequisite net-widening §5 demands *before* the cliff:

- **Step 12a — the circuit-breaker characterization knot** (net-widening, *no version change*). It pins
  the Hystrix-era Feign fallback contract (`feign.hystrix.FallbackFactory`) as a green reference for the
  Step 13 re-architecture. **Already executed on this 2.3.12 / Java 11 plateau** — the net is now
  **20** (`CircuitBreakerFallbackCharacterizationTest`, 2 methods, green). It is documented as the first
  step of the next target because the contract it protects is what Step 13 ports; see the companion
  document for its full write-up and findings.
