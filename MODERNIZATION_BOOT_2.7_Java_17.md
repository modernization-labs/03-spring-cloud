# Next target analysis: **Boot 2.7.x + Java 17**

> **Goal of this document:** decide and justify the *next big target* now that the platform has landed at
> **`Boot 2.3.12.RELEASE` + `Spring Cloud Hoxton.SR12` + Java 11** (the target recorded in
> [MODERNIZATION_BOOT_2.3.12_Java_11.md](MODERNIZATION_BOOT_2.3.12_Java_11.md)). This is an
> **analysis / roadmap** document: it argues *which* target, *why*, and *in what order*, and inventories
> the frictions the remaining walk will hit. The discipline is unchanged: **baby steps, multi-hop,
> characterize before you change, one gated hop at a time.** Unlike the quiet `javax`-plateau climb that
> preceded it, this target contains the **one heavy re-architecture** of the whole journey — the Netflix
> dismantling — so its first move is a net-widening, not a version bump.

---

## 1. Where we stand (the start line)

The previous target carried the platform up three Boot minors and one JDK major to a clean resting point:

| Axis | Current value | Set by |
|---|---|---|
| Spring Boot | `2.3.12.RELEASE` (Spring Framework **5.2**) | prior target, Step 12 |
| Spring Cloud train | `Hoxton.SR12` (serves Boot 2.2 **and** 2.3) | prior target, Step 11 |
| Java | **11** (`<java.version>11</java.version>`) | prior target, Step 10 |
| Namespace | `javax` (unchanged) | — |
| Characterization net | **20 tests green** (18 + the Step 12a circuit-breaker knot) | Steps 9–12 + 12a |

The net that grades every move is already widened over the region about to move: alongside the OAuth2
`302` wall, the Actuator `/actuator/*` contract, the JAXB round-trip (now re-internalized as explicit
`javax`-line dependencies), the JWT/bcpkix path, the logging-backend probe, the BC probes, and the
`JavaeeApiProbeTest` provenance map, **Step 12a added the circuit-breaker characterization knot** over
the Hystrix-era Feign fallback (§ below). **This is the known-good reference for everything that follows.**

---

## 2. The destination — Java 17 — is reachable now, but still not in one jump

The platform is now **above the Java-17 floor in JDK terms** (it runs on Java 11) but **below it in Spring
terms**: Boot 2.3 is Spring Framework **5.2**, and Java 17 needs Spring Framework **5.3** (Boot ≈ 2.4+),
with *officially-supported* Java 17 landing only at **Boot 2.5.5 / 2.6**. So Java 17 is still
**downstream of a Boot climb** — it cannot precede 2.4–2.7; it is unlocked *by* reaching them.

This is the same structural constraint the prior target documented (Step 8's "Finding 0": a premature
modern JDK on an old Spring crashes with `InaccessibleObjectException`). The difference now is that the
**JDK half is already paid** (Java 11 banked at Step 10), so this target's JDK work is reduced to a single
isolated capstone (`11 → 17`) once Boot reaches 2.7 — not a variable smeared through the climb.

---

## 3. The compatibility ceiling (why the target is what it is)

| Boot line | Spring Framework | Highest Java | Spring Cloud train |
|---|---|---|---|
| **2.3** *(we are here)* | 5.2 | 13 – 14 | Hoxton |
| 2.4 | 5.3 | 15 | **2020.0 (Ilford)** — Netflix removed |
| 2.5 | 5.3 | **17** (from 2.5.5) | 2020.0 |
| 2.6 | 5.3 | 17 | **2021.0 (Jubilee)** |
| **2.7** *(target)* | 5.3 | 17 / 18 — solid | 2021.0 |
| 3.0 *(after this target)* | 6.0 | **17 floor — forced** | 2022.0 (Kilburn) |

Two facts fall straight out: **Spring 5.3 (the Java-17 prerequisite) arrives at Boot 2.4**, and **Java 17
becomes solid at 2.7** — so the target is the climb `2.3 → 2.7` finished with the JDK capstone.

---

## 4. The target: **Boot 2.7.x + Java 17**

The genuine next big target is **the climb to the top of the 2.x line — Boot `2.3.12 → 2.7.x`** — taken as
a *multi-hop walk*, one minor per gated step — **finished with the Java 11 → 17 capstone**. End-state:

> **`Boot 2.7.x` + `Spring Cloud 2021.0 (Jubilee)` + Java 17**, the full net green.

What this target deliberately leaves for *next* is **Boot 3.0 + `javax → jakarta`** — and landing Java 17
here is exactly what lets that next jump be *purely* jakarta.

### Why Java 17 **is included** — and why *on Boot 2.7*, not deferred to Boot 3

Boot 3.0 has Java 17 as a **hard floor** *and* mandates the `javax → jakarta` rename. Taking Java 17 here
**decouples** the two:

- **Arrive at Boot 3 still on Java 11** → the Boot 3 jump must carry *both* a JDK 11 → 17 migration *and*
  the jakarta rename in one coupled step — exactly the coupling the whole methodology avoids.
- **Arrive at Boot 3 already on Java 17** (this target) → the Boot 3 jump is reduced to **purely jakarta**.
  The JDK-17 cost — JEP 403 strong encapsulation, reflective-access fallout on the ancient libs (itext,
  BC, springfox, the jaxb2 plugin, tika's shaded jars) — is paid *here*, on the **last stable `javax`
  plateau** (Boot 2.7, everything else known-good), in isolation, graded by the net.

So Java 17 is not extra scope; it is the move that *un-couples* the next jump.

---

## 5. The headline friction: **Spring Cloud Netflix is dismantled at 2020.0** — now imminent

This project carries the Netflix stack — `spring-cloud-starter-netflix-hystrix` and
`spring-cloud-netflix-core` (and, via OpenFeign, `io.github.openfeign:feign-hystrix:10.12`). **Spring
Cloud `2020.0` (Boot 2.4) removed Hystrix, Ribbon, Zuul, Archaius and Turbine** — only Eureka survived,
and OpenFeign moved to its own surviving `spring-cloud-openfeign` module.

So the **very first version hop of this target — Boot 2.3 → 2.4 (Step 14)** — hits the wall where
`netflix-hystrix` / `netflix-core` cease to exist. That is **not** a version bump; it is a
**behaviour-bearing re-architecture** (Hystrix → **Resilience4j** for circuit-breaking), and it must be
its own separately-gated sub-hop **preceded by a re-architecture that lands before the framework removes
the floor under it**. That is why the order is: net-widen first (Step 12a, **done**), re-architect Hystrix
→ Resilience4j (Step 13), *then* cross the 2.4 boundary (Step 14).

> Boot 2.4 also drops `junit-vintage-engine` from the test starter's default — so the `@RunWith` suite,
> which has run untouched on vintage since Boot 2.2, will need the engine re-added explicitly at Step 14
> (§10 *vintage-first* still holds; the Jupiter migration stays deferred).

---

## 6. Other anticipated frictions along the remaining walk

> **Discipline note:** these are *predictions to verify by running the gate*, **not** findings — the same
> rule the prior target proved four times over (the predicted "highest-likelihood blocker," the Spring
> Boot Admin generation bump, was wrong on every hop). Each item earns the word "finding" only once a
> stack trace confirms it.

| Region | Why the remaining walk disturbs it | First oracle / where it bites |
|---|---|---|
| **`spring-cloud-starter-netflix-hystrix` / `netflix-core`** | Removed at Spring Cloud 2020.0 (Boot 2.4). | dependency resolution failure at Step 14; pre-empted by Steps 12a/13. |
| **`feign.hystrix.FallbackFactory`** | The Hystrix-era fallback type; OpenFeign keeps a fallback contract but it migrates to `org.springframework.cloud.openfeign.FallbackFactory`. | `CircuitBreakerFallbackCharacterizationTest` (the Step 12a knot) — green now, must stay green across the Step 13 port. |
| **`spring-boot-admin-starter-client` (still `2.0.6`)** | Generation-locked to the Boot minor — but it has survived 2.0→2.3. Each minor is a fresh roll. | context-init `NoClassDefFound` from a `de.codecentric...` class. |
| **`springfox-swagger 2.7.0`** (`provided`) | Springfox is abandoned and **breaks on Boot 2.6+** (`PathPatternParser` default path matching). | startup `NullPointerException` at 2.6 → §10 decided migrate to `springdoc-openapi` (Step 16). |
| **`io.fabric8:spring-cloud-kubernetes-discovery:0.1.6`** | Pre-official K8s coordinate; Spring Cloud absorbed K8s into the official module around Greenwich/Hoxton; the calendar-train moves may break its resolution. | dependency resolution / autoconfig break on the 2020.0/2021.0 moves. |
| **`mq-jms-spring-boot-starter:0.0.4`** | Very old IBM MQ starter; autoconfig may not track the later Boot 2.x changes. | broker/autoconfig wiring across minors. |
| **`commons-io` pin / `log4j-to-slf4j` exclusion** | Step 8 findings; each Boot minor re-pins these in its BOM. | `HelloControllerTest` Tika test; `LoggingBackendProbeTest`. |
| **Java 17 strong encapsulation (JEP 403)** | `InaccessibleObjectException` errors (not warnings) on reflective access by CGLIB/BC/itext/springfox/jaxb tooling. | every `@SpringBootTest` at the Step 18 capstone. |
| **EOL `spring-security-oauth2`** | The `302` contract from Step 8 finding 4; §10 places its re-architecture inside this walk. | `OAuth2SecurityCharacterizationTest`. |

---

## 7. The decomposition — the baby-step sequence (proposed)

Numbering continues from the prior target. The net-widening and the Netflix re-architecture are called
out as their own gated sub-hops *before* the 2.4 boundary; the JDK capstone is the final isolated hop.

```
Step 12a (net-widen) circuit-breaker characterization knot   ← DONE (net 18 → 20, green on 2.3.12/Java 11)
Step 13  Hystrix → Resilience4j re-architecture              ← behaviour-bearing, its own gate
Step 14  Boot 2.3 → 2.4   train Hoxton → 2020.0 (Ilford)     ← Netflix removed; junit-vintage leaves the default
Step 15  Boot 2.4 → 2.5   train 2020.0                       (Java 17 becomes possible — held on Java 11)
Step 16  Boot 2.5 → 2.6   train 2020.0 → 2021.0 (Jubilee)    (springfox break → migrate to springdoc-openapi)
Step 17  Boot 2.6 → 2.7.x train 2021.0                       → lands Boot 2.7 (still Java 11)
Step 18  Java 11 → 17     (Boot 2.7)   JEP 403 fallout       → CAPSTONE: Boot 2.7 + Java 17
```

The EOL `spring-security-oauth2` `302` re-architecture (§10) is slotted **inside** this walk, gated by
`OAuth2SecurityCharacterizationTest`; the exact step is a planning decision for whichever hop it least
disturbs. Java 17 is held until **after** Boot reaches 2.7 (Step 18), even though 2.5+ makes it possible
earlier (Step 15), so the JDK stays a single fixed value (Java 11) across every Boot hop and the JDK-17
move is one isolated, separately-gated capstone.

> The exact minor patch levels and whether some adjacent minors can be fused are decisions for each
> step's own up-front analysis. Step 13 (Netflix) is the known heavy one, and it earns its widened net
> first — already banked in Step 12a.

---

## 8. How each hop is graded (methodology continuity)

1. **Baseline green** on the current minor under **Java 11** (the working JDK from Step 10 through Step
   17; **Java 17 only from Step 18**). Confirm the JDK before diagnosing code — both pivots are heirs to
   Step 8's "Finding 0."
2. **One hop**, smallest coherent unit, on a throwaway branch.
3. **Re-run the same (now 20-test) net.** Predicted flips are the net doing its job; an *un*predicted red
   is a finding to investigate before the hop is "done."
4. **OpenRewrite stays advisor-then-applier** per minor (`UpgradeSpringBoot_2_4 … _2_7`), run under JDK
   17 — but it covers only the mechanical layer; the Netflix re-architecture, the springfox migration,
   and the generation-locked library bumps are hand-applied and recorded as findings.

---

## 9. What this target unlocks

Landing `Boot 2.7.x + Java 17` puts the platform on the **literal doorstep of Boot 3, with the JDK
question already settled**:

- **Boot 3.0 + `javax → jakarta`** — the next big jump — has Java 17 as a forced floor, which this
  target's capstone (Step 18) already satisfies. So that jump is reduced to a **pure namespace
  migration** — the decoupling is the whole payoff.
- The surviving Eureka/OpenFeign surface and the Resilience4j circuit-breaker (ported in Step 13) are on
  modern, supported lines, ready for the 2022.0 (Kilburn) train under Boot 3.

**In one sentence:** this target is the rest of the Boot 2.x climb taken to its top and finished on a
modern JDK — `Boot 2.7.x + Java 17` — with the Netflix dismantling as its one heavy re-architecture and
Java 17 as an isolated capstone, so that the Boot 3 + jakarta jump that follows is left as *purely* a
namespace migration.

---

## 10. Open decisions (carried forward from the prior target's §10)

These were decided once and bind this target:

1. **JUnit 4 vs 5** — *vintage-first, migrate later.* `junit-vintage-engine` stays; it becomes a
   **mandatory explicit dependency** at Step 14 (Boot 2.4 drops it from the starter default).
2. **Netflix target** — Resilience4j, **ported not retired.** Step 12a confirmed the app's live Hystrix
   surface is *deps-only* and pinned the `feign.hystrix.FallbackFactory` contract; Step 13 ports it.
3. **Springfox** — **migrate to `springdoc-openapi`** (Step 16), since springfox breaks at Boot 2.6+.
4. **OAuth2 `302` re-architecture** — **inside the walk**, independently gated by
   `OAuth2SecurityCharacterizationTest`.

---

# Modernization step 12a — the net-widening before the cliff: characterize the Hystrix-era Feign fallback

> **Status: DONE** (executed on the `Boot 2.3.12 + Hoxton.SR12 + Java 11` plateau). The net grew from
> **18 to 20** tests, green. This was the **net-widening prerequisite** §5/§8 demand *before* the Netflix
> cliff — not a version hop. It changed **no `pom.xml` coordinate, no Boot parent, no train, no JDK, no
> namespace**; it added exactly one new characterization test that pins the **current Hystrix-era Feign
> fallback contract** (`feign.hystrix.FallbackFactory`) as the green reference Step 13 will be graded
> against.

## Guiding principle

Every step of the prior target *moved* something and graded the move against a fixed net. Step 12a is the
inverse: **it moves nothing and grades nothing — it builds the oracle the next move will be graded by.**
§5 names the hole precisely: there was **no characterization test over the Hystrix / circuit-breaker
path**, and Step 13 (Hystrix → Resilience4j) plus Step 14 (Boot 2.4, where `netflix-hystrix`/`netflix-core`
cease to exist) are about to re-architect a behaviour the net was blind to. You do not re-architect into a
blind spot. So this step made that region visible — green now, so a regression in Step 13 turns it red.

## The surface, found in the code (not assumed)

The first task was to *find* the app's real Hystrix surface, not assume it — and the answer is itself a
finding:

**A full grep of `src` for every circuit-breaker surface — `@HystrixCommand`, `HystrixCommand`,
`@FeignClient`, `@EnableCircuitBreaker`, `FallbackFactory`, `fallback` — returns *nothing*.** The
Netflix/Hystrix/OpenFeign stack is present **only as classpath dependencies**, not live code:

| On the classpath (Step 12 after-tree) | In `src` |
|---|---|
| `spring-cloud-starter-openfeign:2.2.9` → `io.github.openfeign:feign-hystrix:10.12` | — |
| `spring-cloud-starter-netflix-hystrix:2.2.9` → `com.netflix.hystrix:hystrix-core:1.5.18` (+ javanica, serialization, metrics-event-stream) | — |
| `spring-cloud-netflix-core:2.2.9` | — |

§10 decided the posture for exactly this case: **port, not retire** — *"a minimal Unit test and port it."*
The directive on the surface was likewise explicit: characterize `feign.hystrix.FallbackFactory`. That
type (`io.github.openfeign:feign-hystrix:10.12`, on the compile classpath) is the canonical Hystrix-era
Feign degradation primitive, and it is precisely what the 2.4/2020.0 cull disturbs (Hystrix removed; the
OpenFeign fallback contract migrates to `org.springframework.cloud.openfeign.FallbackFactory`).

The methodology-clean knot is therefore a **pure unit test that exercises `feign.hystrix.FallbackFactory`
directly** — no Spring context, no live HTTP, **no production code added**. It pins the *contract* the next
two steps must preserve: a `FallbackFactory<T>` receives the **triggering `Throwable`** and returns a
**`T` fallback** that serves a degraded value.

> **Why characterize the contract rather than add a live Hystrix-wrapped client.** Adding a real
> `@FeignClient` + `@HystrixCommand` surface to a 2-class hello app purely to test it would be
> speculative production code that Step 13 would then re-architect *and* Step 14 would force to exist in a
> Hystrix-free world — manufacturing scope, not characterizing it. Pinning the `FallbackFactory` contract
> directly is the minimal knot that still grades Step 13's port honestly.

## The JDK tension — single JDK, no detour

Unlike every Boot-minor hop, Step 12a has **no OpenRewrite recipe** (no version to upgrade), so **no
JDK-17 detour**. The whole step — write the test, run the gate — happens on a **single JDK: Java 11**, the
working JDK from Step 10. Confirm `java -version` reports `11` before diagnosing anything; the Java 11
illegal-reflective-access *warnings* remain expected and are **not findings**.

## What Step 12a changed — one layer, test-only

No layer 1/2 (no OpenRewrite, no train) and no layer 4 (no `pom.xml` edit — the imported dependency was
*already* on the classpath). The entire step is a single test-source addition.

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. New characterization test | add `CircuitBreakerFallbackCharacterizationTest` — a pure JUnit-4 unit test exercising `feign.hystrix.FallbackFactory` (2 `@Test` methods) | by hand (new test file only) | the net itself: must compile against `feign-hystrix:10.12` and pass green on the current Hoxton stack |

## Step 12a — exact actions (as performed)

From the project root, on **JDK 11**:

### 0–1. Branch and baseline

```powershell
git switch -c step12a/circuit-breaker-net
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
mvn -q test      # existing net GREEN first (18 tests) on 2.3/Hoxton/Java 11
```

### 2. Confirm `feign.hystrix.FallbackFactory` is on the compile classpath (add nothing)

```powershell
mvn dependency:tree | Select-String "feign-hystrix"
# -> io.github.openfeign:feign-hystrix:jar:10.12:compile  (transitive via spring-cloud-starter-openfeign)
```

It is present at `compile` scope, so **nothing was added to `pom.xml`** — the test imports a class already
resolved.

### 3. Add the characterization knot — `src/test/java/com/acme/myproduct/CircuitBreakerFallbackCharacterizationTest.java`

A pure unit test (no Spring context) pinning the two load-bearing properties of the Hystrix-era Feign
fallback contract — the factory **receives the triggering cause**, and it **returns a fallback that serves
a degraded value** — written in JUnit 4 to match the existing net (vintage-first, §10):

```java
package com.acme.myproduct;

import feign.hystrix.FallbackFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class CircuitBreakerFallbackCharacterizationTest {

    /** The thin downstream contract a Feign client would expose. */
    interface GreetingClient {
        String greet(String name);
    }

    /** FallbackFactory that, given the failure cause, builds a fallback serving a degraded greeting. */
    static final class GreetingFallbackFactory implements FallbackFactory<GreetingClient> {
        Throwable seenCause;

        @Override
        public GreetingClient create(Throwable cause) {
            this.seenCause = cause;
            return name -> "offline:" + name;
        }
    }

    @Test
    public void factoryReceivesTheTriggeringCause() {
        GreetingFallbackFactory factory = new GreetingFallbackFactory();
        RuntimeException downstreamFailure = new RuntimeException("503 from greeting-service");
        factory.create(downstreamFailure);
        assertSame(downstreamFailure, factory.seenCause);
    }

    @Test
    public void fallbackServesDegradedValueOnFailure() {
        GreetingFallbackFactory factory = new GreetingFallbackFactory();
        GreetingClient fallback = factory.create(new RuntimeException("downstream down"));
        assertEquals("offline:acme", fallback.greet("acme"));
    }
}
```

(The full class — with its characterization Javadoc — is in the repository at that path.)

### 4–5. Gate, on JDK 11

```powershell
mvn clean test      # -> Tests run: 20, Failures: 0, Errors: 0  -> BUILD SUCCESS
mvn clean package   # full repackage, 20/20 green
```

## Findings (recorded)

- **Surface is deps-only — confirmed.** No `@HystrixCommand`/`@FeignClient`/`FallbackFactory`/`fallback`
  anywhere in `src`; the Netflix/Hystrix stack is purely classpath. This is the §10 "thin surface" case,
  resolved by characterizing the `feign.hystrix.FallbackFactory` contract directly rather than wiring a
  live client.
- **`feign-hystrix:10.12` is `compile`-scoped** (transitive via `spring-cloud-starter-openfeign`), so the
  import resolves with **zero `pom.xml` change**.
- **Net grew 18 → 20**, green: `Tests run: 20, Failures: 0, Errors: 0`. Every later step's done-criterion
  reads **20/20** from here.
- **Pre-existing 18 untouched** — no flip, no churn; the only changed file is the new test. The fabric8
  *"Error reading service account token"* WARNs and the XJC *"WSDL support is experimental"* WARN are the
  same benign no-runtime/codegen logs as prior hops, not failures.

## Step 12a — done criteria (all hold)

1. `mvn clean package` under JDK 11 → BUILD SUCCESS, `mvn test` green **20/20**; `<java.version>` still
   `11`.
2. **`pom.xml` unchanged** — no coordinate, parent, train, or JDK edit; the only changed file is
   `CircuitBreakerFallbackCharacterizationTest.java`.
3. The new test exercises `feign.hystrix.FallbackFactory` directly and pins both contract properties,
   establishing the green reference Step 13 will be graded against.
4. The pre-existing 18 tests stay exactly as Steps 8–12 left them. **No test code migrated to Jupiter** —
   vintage-first held.
5. **Nothing re-architected** — Hystrix is still present and untouched; the port is Step 13.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step12a/circuit-breaker-net
```

---

## Toward Step 13 — the first cut into the cliff

With the net at a green **20/20** and the Hystrix-era fallback contract pinned, the blind spot §5 named is
closed. The next move is the first *behaviour-bearing* one of this target:

- **Step 13 — Hystrix → Resilience4j re-architecture** (its own gate, no Boot bump). The OpenFeign
  fallback contract survives the Netflix cull, migrating from `feign.hystrix.FallbackFactory` to
  `org.springframework.cloud.openfeign.FallbackFactory`, with circuit-breaking moving from Hystrix to
  Resilience4j. `CircuitBreakerFallbackCharacterizationTest` must stay green across the port — its
  *imports* swap, its asserted behaviour does not.
- **Then Step 14 — Boot 2.3 → 2.4 + 2020.0 (Ilford)**, where `netflix-hystrix`/`netflix-core` actually
  cease to exist on the train and `junit-vintage-engine` leaves the test starter's default (re-add it
  explicitly, §10 vintage-first). Step 13 must land *first* so that when 2.4 removes Hystrix, nothing the
  net covers still depends on it.
- **Then Steps 15–17** climb 2.4 → 2.7 (springfox → springdoc-openapi at 2.6, Step 16; the OAuth2 `302`
  re-architecture slotted where it least disturbs), and **Step 18** takes the Java 11 → 17 capstone on
  Boot 2.7 — the end-state of this target.

> **Open question to settle at Step 13 planning (raised, not decided here):** whether the port keeps a
> `FallbackFactory`-shaped surface at all, or — given the app has no live Feign client — Step 13 is purely
> the dependency/import migration that keeps *this* characterization green while swapping the Hystrix
> coordinate for the Resilience4j one. Decide it there, with the 2.4 BOM in hand — not now.

---

# Modernization step 13 — the first cut into the cliff: Hystrix → Resilience4j, no Boot bump

> **Status: DRAFT (planned, not yet executed).** This is the first *behaviour-bearing* hop of the target —
> the Netflix re-architecture §5 names as "the one heavy re-architecture of the whole journey." It is taken
> **on the current plateau** (`Boot 2.3.12.RELEASE` + `Spring Cloud Hoxton.SR12` + Java 11), **with no Boot
> parent change, no train change, no JDK change, no namespace change** — only the circuit-breaker stack and
> the one characterization import it pins. The net stays the grader: **20/20 must remain green.** The hop is
> deliberately landed *before* Step 14 so that when Boot 2.4 / Spring Cloud 2020.0 (Ilford) removes Hystrix
> from the train, **nothing the net covers still depends on it** and Step 14's circuit-breaker region is a
> pure version bump.

## Guiding principle

Step 12a built the oracle; Step 13 makes the first move it grades. §5/§10 already decided the *what*
(**Resilience4j, ported not retired**), and Step 12a confirmed the *shape* — but state the shape precisely,
because it has two parts:

- **Production wiring is Hystrix-free.** The Step 12a grep that returned nothing was over `src/main`: no
  `@HystrixCommand`, no live `@FeignClient`, no `@EnableCircuitBreaker`. So **there is no live circuit
  breaker to rewrite** — the Netflix engine was only ever classpath weight.
- **But the Hystrix surface is _not_ absent from `src`.** Step 12a deliberately placed **exactly one**
  reference to it in `src/test`: `CircuitBreakerFallbackCharacterizationTest` *imports and uses*
  `feign.hystrix.FallbackFactory` — that was the whole point of the net-widening, to put a graded reference
  there. A `src` grep today therefore *does* hit `feign.hystrix.FallbackFactory` (one file).

**That test import is treated exactly as a production code reference would be** — not as an incidental test
detail. It is a real compile-time dependency on the `feign.hystrix` package, and because Step 13 **removes
`feign-hystrix` from the classpath** (the exclusion below), `mvn test` **will not compile until the
reference is migrated** to the survivor type. The compiler, not a hand-wave, enforces the port. So the hop
is: **retire the Hystrix coordinates, stand up the Resilience4j provider in their place, and migrate the
one (compiler-enforced) contract reference to `org.springframework.cloud.openfeign.FallbackFactory` — the
type that outlives the Netflix cull** — all while the net holds green. The *contract is byte-identical*
(`T create(Throwable)`, `javap`-confirmed), so the test's two **assertions** do not change; what changes —
and must change for the build to pass — is the **package the reference resolves against**.

## Resolving the open question (the §-above question, now decided with the BOM in hand)

The open question raised at the end of Step 12a was whether to add a live `FallbackFactory`-shaped
production surface, or keep Step 13 a pure dependency/import migration. **Decision: pure migration — no
production code is added.** The reasoning is exactly Step 12a's: wiring a real `@FeignClient` +
`fallbackFactory` into a 2-class hello app purely to exercise it would be speculative production code that
manufactures scope rather than characterizing it. The app has no live Feign client; Step 13 therefore
swaps **coordinates and one import**, and nothing else.

The decision is *safe to make now* (not deferred to the 2.4 BOM) because the survivor surface is already
present on the **current** Hoxton train — verified against the resolved jars in this repo, not predicted:

- **`org.springframework.cloud.openfeign.FallbackFactory<T>` already exists in
  `spring-cloud-openfeign-core:2.2.9.RELEASE`** (the jar already on the compile classpath), with the
  signature **`T create(Throwable)`** — `javap`-confirmed **byte-identical** to `feign.hystrix.FallbackFactory<T>`.
  So the characterization migrates by **swapping one `import` line**; both nested types and both `@Test`
  bodies are untouched, and the asserted behaviour ("factory sees the cause, returns a degraded fallback")
  is unchanged.
- **The same jar already ships the generic `FeignCircuitBreaker` targeter** (`FeignCircuitBreaker`,
  `FeignCircuitBreakerTargeter`, `FeignCircuitBreakerInvocationHandler`) — the Spring-Cloud-owned
  abstraction Resilience4j plugs into. The Hystrix-free circuit-breaker path is therefore *already wired in
  OpenFeign on Hoxton*; Step 13 only supplies the **provider** (Resilience4j) in place of Hystrix.
- **`spring-cloud-starter-circuitbreaker-resilience4j` is BOM-managed on Hoxton.SR12** — the
  `spring-cloud-dependencies:Hoxton.SR12` BOM imports `spring-cloud-circuitbreaker-dependencies:1.0.6.RELEASE`,
  which manages that starter coordinate. So it is added **with no `<version>`** — version-managed, like every
  other train artifact.

## The one non-obvious dependency fact this hop turns on (verified from the live tree)

`dependency:tree` on the current pom shows `io.github.openfeign:feign-hystrix:10.12` is a **direct,
non-optional child of `spring-cloud-starter-openfeign`** — *not* of `spring-cloud-starter-netflix-hystrix`:

```
+- spring-cloud-starter-openfeign:2.2.9.RELEASE:compile
|  +- spring-cloud-openfeign-core:2.2.9.RELEASE:compile
|  +- feign-core:10.12:compile
|  +- feign-slf4j:10.12:compile
|  \- feign-hystrix:10.12:compile          <-- comes from the OPENFEIGN starter
\- spring-cloud-starter-netflix-hystrix:2.2.9.RELEASE:compile
   +- hystrix-core:1.5.18 / -serialization / -metrics-event-stream / -javanica
```

(Note: `spring-cloud-openfeign-core` declares `feign-hystrix` as `optional=true` — so it does **not** arrive
through the core jar; the *starter* pulls it directly.) The consequence is load-bearing:

> **Removing the two Netflix Hystrix coordinates alone does _not_ scrub `feign.hystrix`.** `feign-hystrix:10.12`
> survives via the OpenFeign starter, and it in turn re-drags `com.netflix.hystrix:hystrix-core` transitively
> — so the `feign.hystrix.*` *and* `com.netflix.hystrix.*` surfaces would both linger. To leave Step 14 a
> clean bump, Step 13 must **exclude `io.github.openfeign:feign-hystrix` from `spring-cloud-starter-openfeign`.**
> The migrated test does not need it — `org.springframework.cloud.openfeign.FallbackFactory` lives in
> `spring-cloud-openfeign-core`, which the exclusion leaves untouched.

## The JDK tension — single JDK, no detour (as Step 12a)

Like Step 12a, Step 13 carries **no OpenRewrite recipe** (no Boot/version upgrade), so **no JDK-17 detour**.
The whole hop runs on a **single JDK: Java 11**, the working JDK from Step 10. Confirm `java -version`
reports `11` before diagnosing anything (Step 8's "Finding 0"); the Java 11 illegal-reflective-access
*warnings* remain expected and are **not findings**.

## What Step 13 changes — coordinates + one import

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 4. pom — remove | delete `spring-cloud-starter-netflix-hystrix` and `spring-cloud-netflix-core` | by hand | net stays 20/20 (no live Hystrix wiring per Step 12a grep) |
| 4. pom — add | add `spring-cloud-starter-circuitbreaker-resilience4j` (**no `<version>`** — BOM-managed) | by hand | resolves via Hoxton's circuitbreaker BOM; autoconfig inert with no configured breakers |
| 4. pom — exclude | exclude `io.github.openfeign:feign-hystrix` from `spring-cloud-starter-openfeign` | by hand | `dependency:tree` shows no `feign-hystrix` and no `com.netflix.hystrix:*` |
| 1. test — import swap | in `CircuitBreakerFallbackCharacterizationTest`, swap `feign.hystrix.FallbackFactory` → `org.springframework.cloud.openfeign.FallbackFactory`; bodies unchanged | by hand | the test compiles against openfeign-core and stays green (contract byte-identical) |

No layer 2 (no train move) and no parent/JDK/namespace edit.

## Step 13 — exact actions (proposed)

From the project root, on **JDK 11**:

### 0–1. Branch and baseline

```powershell
git switch -c step13/hystrix-to-resilience4j
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
mvn -q test      # existing net GREEN first (20 tests) on 2.3.12/Hoxton/Java 11
```

### 2. pom — retire Hystrix, add the Resilience4j provider, scrub the feign-hystrix transitive

Remove the two Netflix Hystrix dependencies:

```xml
<!-- DELETE both: -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-netflix-core</artifactId>
</dependency>
```

Add the Resilience4j circuit-breaker starter (BOM-managed — no version):

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

Exclude the lingering Hystrix-era Feign integration from the OpenFeign starter:

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
    <exclusions>
        <!-- Step 13: feign-hystrix is a direct child of THIS starter (not of the netflix-hystrix
             starter) and re-drags com.netflix.hystrix:hystrix-core. Excluding it scrubs the last
             feign.hystrix.* / com.netflix.hystrix.* surface so Step 14 (2.4/2020.0, where Hystrix
             leaves the train) is a pure bump. The migrated characterization imports
             org.springframework.cloud.openfeign.FallbackFactory, which lives in openfeign-core and
             is unaffected. -->
        <exclusion>
            <groupId>io.github.openfeign</groupId>
            <artifactId>feign-hystrix</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### 3. test — migrate the pinned contract import (behaviour unchanged)

In `src/test/java/com/acme/myproduct/CircuitBreakerFallbackCharacterizationTest.java`, swap the single
import; **everything else — the `GreetingClient` interface, the `GreetingFallbackFactory`, both `@Test`
methods — is unchanged** (the survivor's `T create(Throwable)` is byte-identical to the Hystrix type's):

```java
// - import feign.hystrix.FallbackFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
```

(The class Javadoc already anticipates this swap — "its imports swap, its asserted behaviour does not." Its
narrative may be updated from "Hystrix-era" to "the survivor contract" to keep the record honest, but no
assertion changes.)

### 4. Verify the surface is gone, then gate — on JDK 11

```powershell
mvn dependency:tree -Dincludes=*hystrix*,*netflix*,*circuitbreaker*,*resilience*
#   expect: NO com.netflix.hystrix:*, NO io.github.openfeign:feign-hystrix,
#           NO spring-cloud-netflix-core; resilience4j nodes PRESENT
```

- No com.netflix.hystrix:* — the Netflix engine is gone (removed the two starters).
- No io.github.openfeign:feign-hystrix — the exclusion on spring-cloud-starter-openfeign worked; this is the one that wouldn't have dropped on its own (it was the direct child of the OpenFeign starter, and it re-drags hystrix-core — both are now absent, confirming the exclusion took).
- No spring-cloud-netflix-core.
- Resilience4j present: spring-cloud-starter-circuitbreaker-resilience4j:1.0.6.RELEASE (BOM-managed, no pin — exactly the version the Hoxton.SR12 circuitbreaker BOM manages) pulling resilience4j 1.7.0. Nothing alarming in the subtree — resilience4j-spring-boot2 + -micrometer are the normal autoconfig/metrics nodes.

```
mvn clean test      # -> Tests run: 20, Failures: 0, Errors: 0  -> BUILD SUCCESS
mvn clean package   # full repackage, 20/20 green
```

## Predictions to verify by the gate (not findings — §6 discipline)

These are the highest-likelihood disturbances; each earns the word "finding" only if a stack trace
confirms it on the gate:

1. **Context init survives the Hystrix removal.** Step 12a's grep found no live Hystrix wiring, so removing
   `netflix-hystrix`/`netflix-core` should not break `@SpringBootTest` context startup. *Predicted green.*
2. **Resilience4j autoconfig is inert.** `spring-cloud-starter-circuitbreaker-resilience4j` auto-configures a
   `CircuitBreakerFactory`, but with **no configured breakers and no live Feign client using one**, it should
   add a bean and otherwise do nothing. *Predicted green.*
3. **Feign's default targeter is kept.** OpenFeign uses the circuit-breaker targeter only when
   `feign.circuitbreaker.enabled=true` (default **false**); so excluding `feign-hystrix` and adding
   Resilience4j should not change which targeter is wired. *Predicted green.*
4. **`spring-cloud-netflix-core` has no live consumer.** Per Step 12a it is deps-only; removing it should not
   orphan any referenced class. *Predicted green — but the most likely place an _un_predicted red appears,
   so read the trace before declaring done.*

## Step 13 — done criteria

1. `mvn clean package` under JDK 11 → BUILD SUCCESS, `mvn test` green **20/20**; `<java.version>` still `11`,
   parent still `2.3.12.RELEASE`, train still `Hoxton.SR12`, namespace still `javax`.
2. `dependency:tree` shows **no `com.netflix.hystrix:*`, no `io.github.openfeign:feign-hystrix`, no
   `spring-cloud-netflix-core`**, and shows the Resilience4j circuit-breaker nodes.
3. `CircuitBreakerFallbackCharacterizationTest` imports `org.springframework.cloud.openfeign.FallbackFactory`,
   and **both assertions are unchanged** — the contract ("factory sees the cause, returns a degraded
   fallback") still holds green. The import swapped; the behaviour did not.
4. **No production code added** — the port is coordinates + one test import; the §10 "port, not retire,
   without manufacturing a live surface" posture held.
5. The other 19 tests stay exactly as Steps 8–12a left them. **No test code migrated to Jupiter** —
   vintage-first held (the Jupiter migration is still deferred; `junit-vintage-engine` only becomes a
   *mandatory explicit* dependency at Step 14).

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step13/hystrix-to-resilience4j
```

---

## Toward Step 14 — crossing the 2.4 boundary

With Hystrix retired in favour of Resilience4j, the survivor contract pinned on its Spring-Cloud-owned
import, and the `feign.hystrix.*`/`com.netflix.hystrix.*` surfaces scrubbed from the tree, the cliff §5
named is de-risked *before* the framework removes the floor under it. The next move is the first **version**
hop of this target:

- **Step 14 — Boot 2.3 → 2.4 + Spring Cloud Hoxton → 2020.0 (Ilford)**, where `netflix-hystrix`/`netflix-core`
  actually *cease to exist on the train* (now a no-op for this app — Step 13 already removed them) and
  `junit-vintage-engine` **leaves the test starter's default**, so it must be **re-added explicitly** (§10
  vintage-first). The circuit-breaker region crosses as a pure version bump precisely because Step 13 paid
  the re-architecture on the green plateau first.
- **Then Steps 15–17** climb 2.4 → 2.7 (springfox → springdoc-openapi at 2.6, Step 16; the OAuth2 `302`
  re-architecture slotted where it least disturbs), and **Step 18** takes the Java 11 → 17 capstone on Boot
  2.7 — the end-state of this target.

---

# Modernization step 14 — the first version hop: Boot 2.3.12 → 2.4 + Hoxton → 2020.0 (Ilford)

> **Status: DRAFT (planned, not yet executed).** This is the **first version hop** of the target — the
> climb leaves the 2.3 plateau and crosses the **2.4 / Spring Cloud 2020.0 (Ilford)** boundary that §5
> named as the cliff. Unlike Steps 12a and 13 (which moved no version), this hop bumps the **Boot parent**
> (`2.3.12.RELEASE → 2.4.x`) **and** the **train property** (`Hoxton.SR12 → 2020.0.x`) together, because
> the two are co-versioned: Boot 2.4 is served by Spring Cloud 2020.0, not by Hoxton. **JDK stays 11,
> namespace stays `javax`.** The net stays the grader: **20/20 must remain green.** The Netflix removal
> that defines this boundary is, by design, a **no-op for this app** — Step 13 already scrubbed
> `feign.hystrix.*`/`com.netflix.hystrix.*` from the tree, so when 2020.0 deletes Hystrix from the train
> there is nothing left to break.

## Guiding principle

This is the hop the whole net-widening (Step 12a) and re-architecture (Step 13) were sequenced to make
*boring*. §5's discipline — *"re-architect before the framework removes the floor under it"* — has been
paid: the circuit-breaker region crosses 2.4 as a **pure version bump** because its Hystrix dependency was
already retired on the green 2.3 plateau. What remains at this boundary is therefore **not** the cliff but
the **ordinary mechanical friction of a Boot minor**, plus **two boundary-specific defaults that 2.4 /
2020.0 flip** and that no earlier hop has exercised:

1. **`junit-vintage-engine` leaves `spring-boot-starter-test`'s default at Boot 2.4.** The entire net is
   JUnit-4 (`@RunWith`, `org.junit.Test`) and has ridden the vintage engine implicitly since Boot 2.2. At
   2.4 it must be **re-added as an explicit `test`-scope dependency** or **every one of the 20 tests stops
   being discovered** — a green-to-*nothing-ran* failure, not a red. This is the headline mechanical move
   of the hop and the one §5/§10 flagged by name (§10.1: vintage-first, *"becomes a mandatory explicit
   dependency at Step 14"*).
2. **Spring Cloud 2020.0 disables the bootstrap context by default.** Through Hoxton, `bootstrap.yml` and
   the bootstrap application context were on by default; 2020.0 makes bootstrap **opt-in** (re-enabled via
   `spring-cloud-starter-bootstrap` or `spring.cloud.bootstrap.enabled=true`). The fabric8 K8s discovery
   coordinate is the most likely consumer to notice — *prediction, not finding* (§6); verified at the gate.

Everything else this hop touches is the same per-minor BOM re-pin friction the prior target proved four
times — graded, not assumed.

## The mechanical layer vs. the hand-applied layer (OpenRewrite returns, with its JDK-17 detour)

Steps 12a and 13 carried **no OpenRewrite recipe** (no version moved), so they ran on a single JDK. Step
14 is the first hop of this target where the **layer-1 mechanical advisor returns**: `UpgradeSpringBoot_2_4`
(per §8.4). With it returns the **JDK-17 detour** the prior target established — the OpenRewrite runner is
executed under **JDK 17**, then `JAVA_HOME` is switched **back to 11** for the gate. The recipe is run
**advisor-then-applier**: `rewrite:dryRun` first (read the proposed diff), then `rewrite:run` only for the
mechanical slice it gets right. The boundary-specific moves — the explicit `junit-vintage-engine`, the
train property, any generation-locked library bump — are **hand-applied and recorded as findings**, because
the recipe covers only the property/parent mechanics, not the Spring-Cloud train or the version-locked
satellites (Admin, fabric8, MQ).

> **Confirm the JDK before diagnosing anything (Step 8 "Finding 0").** The OpenRewrite run is the *only*
> JDK-17 touch in this hop; the **gate runs on Java 11**. A red seen while `JAVA_HOME` still points at 17
> is a JDK artefact, not a hop finding — re-point to 11 and re-read.

## What Step 14 changes — parent + train + the vintage engine

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. OpenRewrite (advisor→applier) | run `UpgradeSpringBoot_2_4` under JDK 17 — `dryRun` then `run`; accept only the mechanical parent/property slice | recipe, reviewed | the diff is read before `run`; gate re-runs on JDK 11 |
| 2. train move | `spring-cloud.version` `Hoxton.SR12` → **`2020.0.x`** (Ilford) | by hand | `dependency:tree` resolves; Resilience4j starter still BOM-managed (no pin) |
| 4. pom — parent | `spring-boot-starter-parent` `2.3.12.RELEASE` → **`2.4.x`** | recipe or by hand | net stays 20/20 on JDK 11 |
| 4. pom — test (the headline) | **add explicit** `org.junit.vintage:junit-vintage-engine` (`test` scope, **no `<version>`** — BOM-managed) | by hand | **all 20 tests are discovered and run** — without it the suite silently empties |
| 4. pom — satellites (only if the gate demands) | bump generation-locked libs that the gate reds — first candidate `spring-boot-admin-starter-client 2.0.6 → 2.4.x` (§6) | by hand, **as a finding** | `@SpringBootTest` context init green |

No JDK change (`<java.version>` stays `11`), no namespace change (`javax`), no production code.

## Step 14 — exact actions (proposed)

From the project root.

### 0–1. Branch and baseline (JDK 11)

```powershell
git switch -c step14/boot-2.4-ilford
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
mvn -q test      # existing net GREEN first (20 tests) on 2.3.12/Hoxton/Java 11
```

### 2. OpenRewrite advisor, then applier — under JDK 17

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.0.x  (recipe runner only)
```

[https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/3.33.0/rewrite-recipe-bom-3.33.0.pom](https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/3.33.0/rewrite-recipe-bom-3.33.0.pom),

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_4"
```

### 3. Hand-apply the boundary moves the recipe does not own

```xml
<!-- parent (recipe may already have done this) -->
<version>2.4.13</version>   <!-- last 2.4.x; exact patch is this step's call -->

<!-- train property -->
<spring-cloud.version>2020.0.6</spring-cloud.version>   <!-- Ilford final -->

<!-- the headline: vintage engine no longer in the 2.4 starter default -->
<dependency>
    <groupId>org.junit.vintage</groupId>
    <artifactId>junit-vintage-engine</artifactId>
    <scope>test</scope>
    <!-- no <version>: managed by the 2.4 spring-boot-dependencies BOM (§10 vintage-first) -->
</dependency>
```

### 4. Gate — back on JDK 11

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x again

mvn dependency:tree -Dincludes=*hystrix*,*netflix*   # expect: still NOTHING (Step 13 scrubbed it)
mvn clean test      # -> Tests run: 20, Failures: 0, Errors: 0  -> BUILD SUCCESS
mvn clean package   # full repackage, 20/20 green
```

The **first thing to read in the test output is the run count**: `Tests run: 20`. A `BUILD SUCCESS` with
`Tests run: 0` is the vintage-engine omission, not a pass — that is the trap this hop is built around.

## Predictions to verify by the gate (not findings — §6 discipline)

Each earns the word "finding" only if a stack trace (or an empty run count) confirms it on the gate:

1. **Vintage omission empties the suite.** Without the explicit engine, Surefire discovers **0** of the 20
   JUnit-4 tests and still reports `BUILD SUCCESS`. *Predicted: the explicit dependency restores all 20 —
   verify by the run count, not the build status.*
2. **Bootstrap-off (2020.0) disturbs fabric8 K8s discovery.** `spring-cloud-kubernetes-discovery:0.1.6`
   predates the official module and may assume a bootstrap context that 2020.0 no longer starts by default.
   *Most likely place an un-predicted red appears — read the trace before reaching for
   `spring-cloud-starter-bootstrap`.*
3. **Spring Boot Admin `2.0.6` rolls to the 2.4 generation.** §6: Admin is generation-locked and each
   minor is a fresh roll; it has survived 2.0→2.3 but 2.4 is untested. *If context init throws a
   `de.codecentric…` `NoClassDefFound`, bump to `2.4.x` and record as a finding.*
4. **Config processing change is inert for a hello app.** Boot 2.4 ships the new ConfigData API
   (`spring.config.import`, multi-document ordering). The app has no profile-spanning `application.yml`
   tricks, so this should be a no-op. *Predicted green; `spring-boot-properties-migrator` (still on
   `runtime`) will log any rename it catches.*
5. **commons-io / log4j-to-slf4j re-pin holds.** The 2.4 BOM re-pins both (Step 8 findings). *Predicted
   green — `HelloControllerTest` (Tika `CloseShieldInputStream.wrap`) and `LoggingBackendProbeTest` are the
   oracles; the `commons-io 2.13.0` pin and the `log4j-to-slf4j` exclusion stay as Step 8 set them.*
6. **springfox is _not_ disturbed here.** Its break is a 2.6 event (`PathPatternParser`); on 2.4 the
   `provided` springfox 2.7.0 surface is untouched. *Predicted green — the migration is Step 16, not now.*
7. **Resilience4j starter re-resolves on the 2020.0 circuitbreaker BOM.** Added with no `<version>` (Step
   13), it now resolves through 2020.0's `spring-cloud-circuitbreaker` line (≈ 2.0.x → resilience4j 1.7.x)
   instead of Hoxton's 1.0.6. *Predicted green; confirm a resilience4j node still appears in the tree.*

## Step 14 — done criteria

1. `mvn clean package` under **JDK 11** → BUILD SUCCESS, `mvn test` green **`Tests run: 20`**, Failures 0,
   Errors 0; `<java.version>` still `11`, namespace still `javax`.
2. Parent is **`2.4.x`** and `spring-cloud.version` is **`2020.0.x`** (Ilford) — the two moved together.
3. **`junit-vintage-engine` is an explicit `test` dependency** (no version — BOM-managed) and the run count
   proves all 20 tests are still discovered. **No test migrated to Jupiter** — vintage-first held (§10.1).
4. `dependency:tree` shows **no `com.netflix.hystrix:*` / `feign-hystrix`** (Step 13's scrub survived the
   train move, confirming the Netflix removal was a no-op for this app) and a Resilience4j node is present.
5. Any generation-locked satellite bump forced by the gate (Admin, fabric8, MQ) is **recorded as a finding**
   with its stack trace — not applied speculatively. No production code added.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step14/boot-2.4-ilford
```

---

## Toward Step 15 — climbing inside 2020.0

With the 2.4 / Ilford boundary crossed, the train is on the calendar-versioned 2020.0 line and the vintage
engine is explicit — the two facts every subsequent hop inherits. The cliff is behind us; Steps 15–17 are
ordinary minor climbs whose only set-pieces are already scheduled:

- **Step 15 — Boot 2.4 → 2.5 (train stays 2020.0).** Java 17 becomes *possible* here (Boot 2.5.5+) but is
  **held on Java 11** (§7) so the JDK stays one fixed value across every Boot hop. The likely friction is
  the next BOM re-pin and any satellite that 2.5 rolls.
- **Step 16 — Boot 2.5 → 2.6 + train 2020.0 → 2021.0 (Jubilee).** This is where **springfox 2.7.0 breaks**
  (`PathPatternParser` default) and is **migrated to `springdoc-openapi`** (§10.3) — the second-heaviest
  set-piece after the Netflix dismantling.
- **Step 17 — Boot 2.6 → 2.7.x (train 2021.0).** Lands Boot 2.7, still Java 11.
- **Step 18 — Java 11 → 17 on Boot 2.7.** The isolated JDK capstone (JEP 403 fallout) — the end-state of
  this target. The OAuth2 `302` re-architecture (§10.4) is slotted into whichever of 15–17 it least
  disturbs, gated by `OAuth2SecurityCharacterizationTest`.

---

# Modernization step 15 — the quiet minor: Boot 2.4 → 2.5, train stays 2020.0, Java held at 11

> **Status: DRAFT (planned, not yet executed).** This is the **second version hop** of the target and,
> deliberately, the **quietest**. It bumps the **Boot parent only** (`2.4.x → 2.5.x`); the **Spring Cloud
> train does _not_ move** — `2020.0 (Ilford)` serves Boot 2.4 **and** 2.5, so `spring-cloud.version` stays
> `2020.0.6`. Spring Framework stays **5.3**, namespace stays `javax`. The one conceptually-loaded thing
> about this hop is what it **refuses** to do: **Boot 2.5.5+ makes Java 17 officially supported, and this
> hop declines it** — the JDK stays **11** (§7), so the whole 2.x climb runs on one fixed JDK and the
> 11 → 17 move stays the isolated Step 18 capstone. The net stays the grader: **20/20 must remain green.**
>
> **Precondition (verify before the baseline).** Step 14's headline move was adding `junit-vintage-engine`
> as an explicit `test` dependency, because Boot 2.4 dropped it from the `spring-boot-starter-test` default.
> The current `pom.xml` carries the 2.4 parent and the 2020.0.6 train but **does not list
> `junit-vintage-engine`** — so confirm the baseline below actually reports `Tests run: 20` and **not**
> `Tests run: 0`. If it reports `0`, Step 14 is not fully landed; add the explicit vintage engine (Step 14
> §3) and re-baseline before starting Step 15. Step 15 assumes a genuinely green 20/20 on Boot 2.4.

## Guiding principle

Step 14 crossed the cliff; Step 15 is the first hop with **no set-piece** — no removed train, no removed
default, no re-architecture. Its discipline is therefore *restraint*: bump exactly one coordinate (the
parent), let the same 2020.0 BOM re-resolve everything under the new Boot minor, and **leave the JDK alone
even though 2.5.5+ now invites it to move**. §7 is explicit about why the invitation is declined: holding
Java 11 across every Boot hop keeps the JDK a **single fixed value** so the 11 → 17 migration is one
isolated, separately-gated capstone (Step 18) rather than a variable smeared through the climb. Taking Java
17 *here* would couple a JDK major to a Boot minor — exactly the coupling the methodology exists to avoid.

So the move is small by construction, and the value of the hop is the **graded proof** that a Boot minor
moves under a *stationary* train without disturbing the net — the cleanest possible isolation of "what does
Boot 2.5 alone change?"

## The train stays put — what that simplifies (contrast with Step 14)

Step 14 had to move the parent **and** the train together because 2.4 left Hoxton behind. Step 15 does not:
**`2020.0` is the train for Boot 2.4 _and_ 2.5**, so `spring-cloud.version` is untouched. The consequence
is that the whole Spring-Cloud surface — OpenFeign, the Resilience4j circuit-breaker starter (Step 13),
fabric8 K8s discovery — re-resolves against the **same** BOM it already resolved against at Step 14. The
bootstrap-off default (a 2020.0 property, already crossed at Step 14) does not re-trigger here; if it was
going to bite fabric8 it already did at 14. **The only moving part is the Boot BOM re-pin.**

## The mechanical layer — OpenRewrite `UpgradeSpringBoot_2_5`, same JDK-17 detour

As at Step 14, the layer-1 advisor returns: `org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_5`, run
**advisor-then-applier** under **JDK 17** (the recipe runner only), then `JAVA_HOME` switched **back to 11**
for the gate. The recipe owns the mechanical parent/property slice; the train property (unchanged here) and
any generation-locked satellite bump are hand-applied and recorded as findings. Use the same plugin /
recipe-artifact form the Step 14 correction settled — the recipe lives in **`rewrite-spring`**, not
`rewrite-migrate-java`:

> **Confirm the JDK before diagnosing anything (Step 8 "Finding 0").** The OpenRewrite run is the only
> JDK-17 touch; the **gate runs on Java 11**. And note the standing temptation specific to *this* hop:
> seeing the recipe run green under JDK 17 is **not** permission to leave `JAVA_HOME` on 17 for the gate —
> Java 17 is held until Step 18, by decision, not by inability.

## What Step 15 changes — the parent, and nothing else by intent

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. OpenRewrite (advisor→applier) | run `UpgradeSpringBoot_2_5` under JDK 17 — `dryRun` then `run`; accept only the mechanical parent/property slice | recipe, reviewed | the diff is read before `run`; gate re-runs on JDK 11 |
| 4. pom — parent | `spring-boot-starter-parent` `2.4.13` → **`2.5.x`** (last 2.5 patch) | recipe or by hand | net stays 20/20 on JDK 11 |
| — train | **no change** — `2020.0.6` serves Boot 2.5 too | — | `dependency:tree` re-resolves under the same train |
| — JDK | **no change** — `<java.version>` stays `11` **though 2.5.5+ permits 17** (§7) | — | gate runs on JDK 11; Java 17 deferred to Step 18 |
| 4. pom — satellites (only if the gate demands) | bump generation-locked libs the gate reds — first candidate `spring-boot-admin-starter-client 2.0.6` (§6) | by hand, **as a finding** | `@SpringBootTest` context init green |

No namespace change (`javax`), no production code.

## Step 15 — exact actions (proposed)

From the project root.

### 0–1. Branch and baseline (JDK 11)

```powershell
git switch -c step15/boot-2.5
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
mvn -q test      # existing net GREEN first on 2.4.13/2020.0/Java 11 — confirm "Tests run: 20", NOT 0
```

> If the baseline reports `Tests run: 0`, stop: the explicit `junit-vintage-engine` from Step 14 is
> missing (see Precondition). Add it, re-baseline to 20 green, then proceed.

### 2. OpenRewrite advisor, then applier — under JDK 17

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.0.x  (recipe runner only — NOT the gate JDK)

# advisor: read the proposed diff, change nothing
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:dryRun `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_5"

# applier: accept the mechanical parent/property slice
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_5"
```

### 3. Hand-apply / confirm the parent (recipe may already have done it); train and JDK unchanged

```xml
<!-- parent -->
<version>2.5.15</version>   <!-- last 2.5.x; exact patch is this step's call -->

<!-- train property: UNCHANGED — 2020.0 serves Boot 2.5 -->
<spring-cloud.version>2020.0.6</spring-cloud.version>

<!-- JDK: UNCHANGED — held at 11 by decision (§7), though 2.5.5+ permits 17 -->
<java.version>11</java.version>
```

### 4. Gate — back on JDK 11

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x again — the gate JDK

mvn clean test      # -> Tests run: 20, Failures: 0, Errors: 0  -> BUILD SUCCESS
mvn clean package   # full repackage, 20/20 green
```

Read the **run count** first (`Tests run: 20`) — the vintage-engine trap from Step 14 still applies: a
`BUILD SUCCESS` with `Tests run: 0` is an empty suite, not a pass.

## Predictions to verify by the gate (not findings — §6 discipline)

Each earns the word "finding" only if a stack trace (or an empty/changed run count) confirms it:

1. **SQL init property rename.** Boot 2.5 deprecates `spring.datasource.initialization-mode` in favour of
   `spring.sql.init.mode` and decouples script-based init from JPA. The app uses H2 at `test` scope; if it
   relies on `schema.sql`/`data.sql` ordering, the behaviour may shift. *Predicted inert for a hello app;
   `spring-boot-properties-migrator` (still on `runtime`) is the oracle that will log any such rename.*
2. **Actuator contract drift.** Boot 2.5 adjusts health-group / endpoint sanitization defaults.
   *`ActuatorEndpointCharacterizationTest` is the oracle — predicted green; a changed status payload is a
   finding to read, not assume.* (The bigger Actuator default change — `management.info.env.enabled=false`
   — is a **2.6** event, Step 16, not here.)
3. **Spring Boot Admin `2.0.6` on Boot 2.5.** §6: Admin is generation-locked and each minor is a fresh
   roll; 2.0.6 is now four minors behind the parent. *Most likely place an un-predicted red appears — if
   context init throws a `de.codecentric…` `NoClassDefFound`, bump to a 2.5-generation Admin and record as
   a finding.*
4. **commons-io / log4j-to-slf4j re-pin holds.** The 2.5 BOM re-pins both (Step 8 findings). *Predicted
   green — `HelloControllerTest` (Tika `CloseShieldInputStream.wrap`) and `LoggingBackendProbeTest` are the
   oracles; the `commons-io 2.13.0` pin and the `log4j-to-slf4j` exclusion stay as Step 8 set them.*
5. **springfox is _not_ disturbed here — but this is the last hop before it breaks.** Springfox 2.7.0
   (`provided`) survives on 2.5; its `PathPatternParser` break is a **2.6** event (Step 16). *Predicted
   green — flagged so the Step 16 migration to `springdoc-openapi` is expected, not a surprise.*
6. **fabric8 K8s discovery and MQ autoconfig.** Train unchanged (2020.0) so no new Spring-Cloud friction;
   the only variable is the Boot 2.5 BOM. *Predicted green; the bootstrap-off concern was a 2020.0 event
   already crossed at Step 14.*

## Step 15 — done criteria

1. `mvn clean package` under **JDK 11** → BUILD SUCCESS, `mvn test` green **`Tests run: 20`**, Failures 0,
   Errors 0; `<java.version>` still **`11`** (Java 17 deliberately **not** taken), namespace still `javax`.
2. Parent is **`2.5.x`**; `spring-cloud.version` is **unchanged at `2020.0.6`** (the train did not move).
3. `junit-vintage-engine` remains an explicit `test` dependency and the run count proves all 20 tests are
   discovered. **No test migrated to Jupiter** — vintage-first held (§10.1).
4. Any generation-locked satellite bump forced by the gate (Admin first) is **recorded as a finding** with
   its stack trace — not applied speculatively. No production code added.
5. The JDK-held decision is recorded explicitly: 2.5.5+ permits Java 17, and this hop declines it on
   purpose (§7), so the JDK stays one fixed value through to the Step 18 capstone.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step15/boot-2.5
```

## findings

```
mvn test "-Dtest=ActuatorEndpointCharacterizationTest" *>&1 | Tee-Object -FilePath step15_ActuatorEndpointCharacterizationTest_logs.txt
```

### Finding 15.1 — Boot 2.5's new repository-metrics autoconfig collides with the ancient `spring-data-commons-core` pin

**Symptom (graded red, not predicted exactly).** On the Boot 2.5.15 gate, `ActuatorEndpointCharacterizationTest`
fails at **context load** — both methods error before any assertion runs:

```
[ERROR] Tests run: 2, Failures: 0, Errors: 2  <<< FAILURE! - in com.acme.myproduct.ActuatorEndpointCharacterizationTest
  rootHealthIsGoneIn20 .......................... IllegalState: Failed to load ApplicationContext
  healthIsServedUnderActuatorPrefixWithStatusBody  IllegalState: Failed to load ApplicationContext
```

This is a **real `ApplicationContext` failure, not the vintage-engine trap**: the suite was discovered and
ran (`Tests run: 2`), and this test is in fact **JUnit 5 (Jupiter)** (`org.junit.jupiter.api.Test`), so the
missing `junit-vintage-engine` precondition is orthogonal to it — the context simply refuses to start.

**The causal chain (read bottom-up from the log).**

```
IllegalStateException: Error processing condition on
    o.s.b.actuate.autoconfigure.metrics.data.RepositoryMetricsAutoConfiguration.metricsRepositoryMethodInvocationListener
  └─ @ConditionalOnMissingBean did not specify a bean by type/name/annotation and the attempt to DEDUCE the type failed
     └─ OnBeanCondition$BeanTypeDeductionException: Failed to deduce bean type for …metricsRepositoryMethodInvocationListener
        └─ NoClassDefFoundError:  org/springframework/data/repository/core/support/RepositoryMethodInvocationListener
           └─ ClassNotFoundException: org.springframework.data.repository.core.support.RepositoryMethodInvocationListener
```

To evaluate the `@ConditionalOnMissingBean` on the `metricsRepositoryMethodInvocationListener` factory
method, Spring must **deduce that method's return type** — which transitively references
`org.springframework.data.repository.core.support.RepositoryMethodInvocationListener`. That class is **not
on the classpath**, so the deduction throws `NoClassDefFoundError`, the condition evaluation aborts, and the
whole context fails to refresh.

**Root cause — a new-in-2.5 autoconfig switched ON by a legacy pin it predates.** Two facts collide:

1. **`RepositoryMetricsAutoConfiguration` is new in Spring Boot 2.5** (repository-invocation metrics). This
   is exactly why the region was **green at Boot 2.4 (Step 14) and reds only now** — Step 15 is the first
   gate where this autoconfiguration exists. The §6 "Actuator drift" prediction (Step 15 prediction #2)
   anticipated *a* disturbance in this region; the precise mechanism — a brand-new autoconfig — is the part
   that earns the word **finding**.
2. **The pom pins the ancient `org.springframework.data:spring-data-commons-core:1.4.1.RELEASE`** (a Spring
   Data **1.x** coordinate, carried since the legacy stack as a "promoted transitive"). That jar ships
   enough of `org.springframework.data.repository.*` to satisfy the autoconfig's class gate and turn it
   **ON**, but it **predates `RepositoryMethodInvocationListener`** (added in Spring Data Commons **2.4+**).
   So the autoconfig activates against a Spring Data API that is years too old to supply the type its own
   bean method returns.

In one line: **the legacy `spring-data-commons-core:1.4.1` pin is new enough to *trigger* Boot 2.5's
repository-metrics autoconfig and too old to *satisfy* it.**

**Resolution options (to decide and apply as the Step 15 fix; not yet applied).** In methodology order from
most surgical to heaviest:

1. **Exclude the autoconfiguration** — `spring.autoconfigure.exclude=\
   org.springframework.boot.actuate.autoconfigure.metrics.data.RepositoryMetricsAutoConfiguration`
   (the app has no Spring Data repositories, so repository metrics are meaningless here). Smallest change,
   keeps the legacy pin, no dependency-tree churn. **Recommended** — it matches the "this app has no live
   data layer" reality and the surgical-changes discipline.
2. **Drop the ancient `spring-data-commons-core:1.4.1.RELEASE` pin** *if* a `dependency:tree` /usage check
   confirms nothing references it. With the class gate gone, the autoconfig never activates. This removes
   dead legacy weight rather than masking it — slightly larger blast radius (verify no transitive consumer
   first), so weigh against option 1.
3. **Replace it with the BOM-managed modern `spring-data-commons`** so the type is present and the autoconfig
   works *for real*. Heaviest — introduces a real Spring Data surface the hello app does not otherwise have;
   manufactures scope. Rejected unless options 1–2 prove infeasible.

Whichever is chosen, the **done-criterion is unchanged**: `ActuatorEndpointCharacterizationTest` back to
green with its two assertions (path `/actuator/health` served with a `"status":` body; root `/health` →
404) intact, and the full net at **20/20** on Boot 2.5 / Java 11.

--> option 2. was chosen

---

## Toward Step 16 — the springfox cliff and the train's last move

Step 15 proved a Boot minor moves under a stationary train with the JDK held. Step 16 is the hop with the
target's **second-heaviest set-piece**:

- **Step 16 — Boot 2.5 → 2.6 + train 2020.0 → 2021.0 (Jubilee).** Two things land together here: the train
  takes its **last move of this target** (2020.0 → 2021.0, which serves Boot 2.6 and 2.7), and **springfox
  2.7.0 breaks** on Boot 2.6's `PathPatternParser`-default path matching — forcing the **migration to
  `springdoc-openapi`** (§10.3). Boot 2.6 also flips `management.info.env.enabled` to `false`, an Actuator
  default `ActuatorEndpointCharacterizationTest` will catch. JDK still held at 11.
- **Step 17 — Boot 2.6 → 2.7.x (train 2021.0).** The quiet landing hop onto Boot 2.7, still Java 11.
- **Step 18 — Java 11 → 17 on Boot 2.7.** The isolated JDK capstone (JEP 403 fallout) — the end-state of
  this target. The OAuth2 `302` re-architecture (§10.4) is slotted into whichever of 16–17 it least
  disturbs, gated by `OAuth2SecurityCharacterizationTest`.
