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

---

# Modernization step 16 — the springfox cliff and the train's last move: Boot 2.5 → 2.6 + 2020.0 → 2021.0 (Jubilee), springfox → springdoc

> **Status: DONE (executed, gate green — net 21/21).** Landed on **Boot `2.6.15` + Spring Cloud `2021.0.9`
> (Jubilee) + `springdoc-openapi-ui 1.5.13` + Java 11**. This is the **third version hop** of the target and
> its **second-heaviest set-piece** after the Netflix dismantling. It is a *coupled* hop by design (§7): three
> moving parts landed on one gated branch — the **Boot parent** (`2.5.15 → 2.6.15`), the **Spring Cloud train**
> taking its **last move of this target** (`2020.0.6 → 2021.0.9`, Jubilee — which serves Boot 2.6 **and**
> 2.7), and the **springfox → `springdoc-openapi` re-architecture** forced because **springfox 2.7.0 dies on
> Boot 2.6's `PathPatternParser`-default path matching**. Spring Framework moved **5.3 → 5.3** (same line),
> **JDK stayed 11** (§7 — held to the Step 18 capstone), **namespace stayed `javax`.** The net was the
> grader: the swagger surface was *uncharacterized*, so this hop first widened the net by one
> (`OpenApiDocsCharacterizationTest`, green on the 2.5/springfox baseline) before migrating it, then crossed
> to 2.6 — **net 20 → 21, green; `mvn clean test` and `mvn package` both BUILD SUCCESS; `dependency:tree`
> shows no `io.springfox:*` and springdoc present.** No findings — the predictions below all held green
> (springdoc served `/v3/api-docs` past the security wall with the same basic auth; the `info.env` flip did
> not red the Actuator knot; fabric8 and Admin `2.0.6` survived the 2.6/2021.0 move).

## Guiding principle — do not migrate a surface the net cannot see

Every prior re-architecture in this journey was graded by a test that existed **before** the move: Step 13
swapped the `FallbackFactory` import only because Step 12a had already pinned its contract green. The
springfox surface has **no such oracle**. `SwaggerConfig` (`@EnableSwagger2` + a `Docket` scanning
`com.acme.myproduct`) is a **live `src/main`, compile-scope** surface — verified in the pom and the class —
**but a grep of `src/test` for `swagger`/`springfox`/`api-docs`/`openapi` returns nothing.** The net is
blind to exactly the surface this hop re-architects. That is the §5 blind-spot condition that preceded the
Netflix cliff, and the discipline is identical: **characterize before you change.**

Because the chosen decomposition keeps the migration *coupled* with the 2.6 bump (not a separate Step 16a),
the net-widening happens **inside this hop, on the 2.5 baseline, as its first action** — before any
coordinate moves. The new test pins the *one load-bearing fact* a documentation surface owes the app: **the
OpenAPI/Swagger descriptor is served and carries the app's configured API title.** It is written against the
**2.5 springfox contract first** (`GET /v2/api-docs` with credentials → `200`, JSON body carrying the
configured title `My Product API`), goes green on the baseline, and the migration then **deliberately flips
its asserted path** `/v2/api-docs` → `/v3/api-docs` — a *predicted flip*, the net doing its job, exactly as
Step 13's import swap was. The *behaviour* it asserts (a descriptor is served carrying `My Product API`) is
invariant across the migration; the *coordinate* (`v2` → `v3`) is what springdoc changes, and the test
records that as the migration's visible contract delta.

> **Why the title, not the endpoint path.** The instinct is to assert the descriptor *names the app's real
> endpoint* — but the only mapped path is `GET /` (`HelloController.index`), and asserting a body `contains`
> `/` is vacuous. The configured title `My Product API` (set in `SwaggerConfig`'s `ApiInfo`/`OpenAPI.info`)
> is the meaningful invariant: it proves the served descriptor is *our* configured one, not just any `200`,
> and both springfox and springdoc emit it. *(Verified: this knot is green on the 2.5/springfox baseline —
> `Tests run: 1, Failures: 0`.)*

> **Why one assertion, not a full swagger schema diff.** Pinning the descriptor *exists and carries our
> configured title* is the minimal knot that grades the migration honestly without manufacturing scope — the same
> restraint Step 12a applied to the Hystrix contract. A byte-level schema characterization would pin
> springfox-vs-springdoc serialization differences that are *expected* to change and tell us nothing about
> whether the documentation surface survived.

## The three set-pieces (and why they are coupled here, not split)

1. **springfox 2.7.0 → `springdoc-openapi` (the re-architecture).** Springfox is abandoned and its
   `springfox.documentation.spring.web` path provider throws on Boot 2.6's **`PathPatternParser`** default
   (`management`/`mvc` path matching switched from `AntPathMatcher`). There is a known one-line escape hatch
   (`spring.mvc.pathmatch.matching-strategy=ant_path_matcher`), and §10.3 **rejected it**: it keeps an
   abandoned, soon-incompatible library limping rather than landing on the supported successor. So the move
   is a real re-architecture — retire both springfox coordinates, add `org.springdoc:springdoc-openapi-ui`,
   and rewrite `SwaggerConfig` from the springfox `Docket`/`@EnableSwagger2` idiom to the springdoc
   `OpenAPI`-bean idiom. **Springdoc is _not_ BOM-managed** by Boot or the Spring Cloud train, so it carries
   an **explicit `<version>`** (a springdoc 1.6.x line — the last major that targets Boot 2.x; springdoc 2.x
   is Boot-3/jakarta only and is **out of scope** until the next target).
2. **Train `2020.0.6 → 2021.0.x` (Jubilee) — the last train move of this target.** 2021.0 serves Boot 2.6
   **and** 2.7, so once landed here it does **not** move again before the Step 18 capstone. The Resilience4j
   circuit-breaker starter (Step 13, BOM-managed, no pin) and OpenFeign re-resolve against the 2021.0 BOM;
   the fabric8 K8s discovery coordinate is the most likely consumer to notice — *prediction, not finding*.
3. **`management.info.env.enabled` flips to `false` (Boot 2.6 default).** `/actuator/info` no longer exposes
   `info.*` environment properties by default. `ActuatorEndpointCharacterizationTest` is the oracle; whether
   it reds depends on what that test asserts about `/actuator/info` — *predicted to be inert if it only
   asserts `/actuator/health`, a finding to read if it touches `info`.*

> **Why coupled and not split (the decision on record).** The Netflix precedent (12a/13/14) would argue for
> migrating springfox on the green 2.5 plateau *before* 2.6 removes its floor. This hop instead follows §7's
> framing and keeps them together, accepting the coupling because — unlike Hystrix, whose removal was a
> *train* event that forced a separate boundary crossing — springfox's break and its successor's arrival are
> both **Boot-version events** that the same OpenRewrite/parent bump touches. The mitigation for the coupling
> is the in-hop net-widen above: the migration is still graded by a test that went green on 2.5 first.

## Boot 2.6's other default that bites springfox specifically — circular references

Boot 2.6 also flips **`spring.main.allow-circular-references` to `false`** by default. Springfox's
`documentationPluginsBootstrapper` is the canonical victim — it forms a bean cycle that 2.5 tolerated and
2.6 refuses, throwing `BeanCurrentlyInCreationException` at context init. This is a **second, independent
reason** the springfox surface cannot simply ride to 2.6, and it is why the escape-hatch property alone
would not even suffice (it would also need `allow-circular-references=true`). The springdoc successor has no
such cycle. *Prediction to verify at the gate — if a springfox cycle error appears while the migration is
mid-flight, it confirms the migration is mandatory, not optional.*

## The mechanical layer — OpenRewrite `UpgradeSpringBoot_2_6`, same JDK-17 detour

As at Steps 14–15, the layer-1 advisor returns: `org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_6`,
run **advisor-then-applier** under **JDK 17** (recipe runner only), then `JAVA_HOME` switched **back to 11**
for the gate. The recipe owns the mechanical parent/property slice. The **springfox → springdoc rewrite is
hand-applied** — OpenRewrite ships a springfox-to-springdoc recipe, but its coverage of a hand-written
`Docket` config is partial; treat any recipe output as **advisory only** and hand-write `SwaggerConfig`, the
pom coordinates, and the test flip, recording the result as a finding. The train property and any
generation-locked satellite bump are hand-applied as before.

> **Confirm the JDK before diagnosing anything (Step 8 "Finding 0").** The OpenRewrite run is the only
> JDK-17 touch; the **gate runs on Java 11**. Java 17 is held to Step 18 by decision, not by inability — a
> green recipe run under 17 is not licence to gate on 17.

## What Step 16 changes — net-widen, parent, train, and the springfox surface

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. New characterization test (net-widen, on 2.5 first) | add `OpenApiDocsCharacterizationTest` — a **Jupiter** `@SpringBootTest`(RANDOM_PORT) with basic auth, asserting the descriptor is served and carries the title `My Product API`; written to springfox `/v2/api-docs` on the 2.5 baseline | by hand | **green on 2.5 — verified** (net 20 → 21) **before** any coordinate moves |
| 1. OpenRewrite (advisor→applier) | run `UpgradeSpringBoot_2_6` under JDK 17 — `dryRun` then `run`; accept only the mechanical parent/property slice | recipe, reviewed | diff read before `run`; gate re-runs on JDK 11 |
| 2. train move (last of target) | `spring-cloud.version` `2020.0.6` → **`2021.0.x`** (Jubilee) | by hand | `dependency:tree` resolves; Resilience4j starter still BOM-managed (no pin) |
| 4. pom — parent | `spring-boot-starter-parent` `2.5.15` → **`2.6.x`** | recipe or by hand | net green on JDK 11 |
| 4. pom — remove springfox | delete `springfox-swagger2 2.7.0` **and** `springfox-swagger-ui 2.7.0` | by hand | `dependency:tree` shows no `io.springfox:*` |
| 4. pom — add springdoc | add `org.springdoc:springdoc-openapi-ui` with **explicit `<version>`** (1.6.x — last Boot-2.x line) | by hand | resolves; not BOM-managed, so the pin is required and deliberate |
| 3. src — rewrite `SwaggerConfig` | replace `@EnableSwagger2` + `Docket` with a springdoc `@Bean OpenAPI` carrying the same title/description/version | by hand | context init green; descriptor served at `/v3/api-docs` |
| 1. test — flip the pinned path | in `OpenApiDocsCharacterizationTest`, change the asserted path `/v2/api-docs` → `/v3/api-docs`; the "carries `My Product API`" assertion is unchanged | by hand | **predicted flip** — the migration's visible contract delta, net back to 21 green |
| 4. pom — satellites (only if the gate demands) | bump generation-locked libs the gate reds — `spring-boot-admin-starter-client 2.0.6` (§6) is now six minors behind | by hand, **as a finding** | `@SpringBootTest` context init green |

No JDK change (`<java.version>` stays `11`), no namespace change (`javax`), no production behaviour change —
the documented endpoints are identical; only the documentation *provider* and its descriptor path change.

## Step 16 — exact actions (proposed)

From the project root.

### 0–1. Branch and baseline (JDK 11), then net-widen on the 2.5 plateau

```powershell
git switch -c step16/boot-2.6-jubilee-springdoc
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
mvn -q test      # existing net GREEN first on 2.5.15/2020.0/Java 11 — confirm "Tests run: 20", NOT 0
```

Add `src/test/java/com/acme/myproduct/OpenApiDocsCharacterizationTest.java` — written against the **current
springfox** contract so it goes green on the 2.5 baseline **before** anything moves:

```java
package com.acme.myproduct;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterizes the live API-documentation surface (SwaggerConfig): a descriptor is served and carries
 * the configured title "My Product API". Written against springfox's /v2/api-docs on the Boot 2.5
 * baseline; Step 16's migration to springdoc-openapi deliberately flips the asserted path to /v3/api-docs
 * (a predicted flip) while the title assertion is invariant (both providers emit ApiInfo/OpenAPI.info).
 *
 * Jupiter, not JUnit 4: the net is JUnit 5 (the JUnit 4 API is not on the classpath). Basic auth: the
 * app secures every path (unauthenticated -> 302 /login), and /v2/api-docs sits behind that same wall,
 * so the descriptor is fetched WITH credentials, exactly as ActuatorEndpointCharacterizationTest does.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "spring.security.user.name=probe",
        "spring.security.user.password=probe-pw"
})
public class OpenApiDocsCharacterizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void apiDocsDescriptorIsServedAndCarriesTheConfiguredTitle() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                .getForEntity("/v2/api-docs", String.class); // -> /v3/api-docs after the springdoc migration
        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody().contains("My Product API"),
                "expected the descriptor to carry the configured title, but was: " + response.getBody());
    }
}
```

```powershell
mvn -q test "-Dtest=OpenApiDocsCharacterizationTest"   # GREEN on 2.5/springfox -> net is now 21
# verified: Tests run: 1, Failures: 0, Errors: 0  (springfox served /v2/api-docs carrying "My Product API")
```

> If this red on the 2.5 baseline (e.g. springfox serves the descriptor at a different path, or the body
> does not contain the title), **stop and adjust the test to the real springfox contract** before migrating —
> the whole point is a *green-on-2.5* reference. Diagnose against the running app, do not assume.

### 2. OpenRewrite advisor, then applier — under JDK 17

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.0.x  (recipe runner only — NOT the gate JDK)

# advisor: read the proposed diff, change nothing
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:dryRun `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_6"

# applier: accept the mechanical parent/property slice only
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_6"
```

### 3. Hand-apply the moves the recipe does not own — train, springfox→springdoc, the `SwaggerConfig` rewrite

```xml
<!-- parent (recipe may already have done this) -->
<version>2.6.15</version>   <!-- last 2.6.x; exact patch is this step's call -->

<!-- train property: the LAST move of this target -->
<spring-cloud.version>2021.0.9</spring-cloud.version>   <!-- Jubilee; serves Boot 2.6 AND 2.7 -->
```

```xml
<!-- DELETE both springfox coordinates -->
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger2</artifactId>
    <version>2.7.0</version>
</dependency>
<dependency>
    <groupId>io.springfox</groupId>
    <artifactId>springfox-swagger-ui</artifactId>
    <version>2.7.0</version>
</dependency>

<!-- ADD springdoc-openapi (NOT BOM-managed -> explicit version; 1.6.x is the last Boot-2.x line.
     springdoc 2.x is Boot-3/jakarta only and is out of scope until the next target). -->
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-ui</artifactId>
    <version>1.6.15</version>
</dependency>
```

Rewrite `src/main/java/com/acme/myproduct/SwaggerConfig.java` from the springfox idiom to the springdoc one —
**same title/description/version**, no `@Enable*` (springdoc auto-configures), default scan covers
`HelloController` (which is in `com.acme.myproduct`):

```java
package com.acme.myproduct;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi configuration -- the live API-documentation surface of the service.
 * Replaces the abandoned springfox 2.7.0 Docket/@EnableSwagger2 wiring (Step 16), which broke on
 * Boot 2.6's PathPatternParser default and circular-reference prohibition. springdoc auto-configures
 * the descriptor at /v3/api-docs and the UI at /swagger-ui.html; this bean only supplies the ApiInfo.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("My Product API")
                        .description("springdoc-openapi-generated OpenAPI 3 documentation for the my-product service.")
                        .version("1.0.0"));
    }
}
```

> If preserving the springfox `basePackage("com.acme.myproduct")` *scoping* matters beyond the default
> (it does not for this 2-class app — the only controller is already in that package), use a
> `GroupedOpenApi` bean with `packagesToScan`. Not added here: it would be configurability that was not
> requested (simplicity-first).

### 4. Flip the pinned path, then gate — back on JDK 11

In `OpenApiDocsCharacterizationTest`, make the one predicted change:

```java
// - rest.getForEntity("/v2/api-docs", String.class);   // springfox
rest.getForEntity("/v3/api-docs", String.class);         // springdoc
```

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x again — the gate JDK

mvn dependency:tree -Dincludes=io.springfox:*,org.springdoc:*   # expect: NO io.springfox:*, springdoc PRESENT
mvn clean test      # -> Tests run: 21, Failures: 0, Errors: 0  -> BUILD SUCCESS
mvn clean package   # full repackage, 21/21 green
```

Read the **run count** first (`Tests run: 21` — the net grew by the new doc characterization): a
`BUILD SUCCESS` with a *shrunken* run count is an empty or partly-discovered suite, not a pass.

## Predictions to verify by the gate (not findings — §6 discipline)

Each earns the word "finding" only if a stack trace (or a changed run count) confirms it:

1. **springfox cannot reach 2.6 — confirmed mandatory, not optional.** If the migration is staged
   incrementally, springfox on 2.6 throws either a `PathPatternParser`/`NullPointerException` in
   `springfox.documentation.spring.web` *or* a `BeanCurrentlyInCreationException` from
   `documentationPluginsBootstrapper` (circular-references now prohibited). *Predicted — this is the §6
   springfox row finally biting; it is why the migration is in this hop and not deferred.*
2. **springdoc serves the descriptor at `/v3/api-docs`.** The doc characterization flips from `/v2` to `/v3`
   and stays green. *Predicted green — the migration's visible contract delta; the asserted behaviour
   ("descriptor carries `My Product API`") is invariant.* (Note: springdoc may also require the descriptor
   path to be permitted past the security wall — if `/v3/api-docs` reds with a 302/401 under the same basic
   auth that worked for springfox, that is the finding, and the fix is a security-config permit, not a test
   change.)
3. **`management.info.env.enabled=false` may move `/actuator/info`.** `ActuatorEndpointCharacterizationTest`
   is the oracle. *Predicted inert if it asserts only `/actuator/health` (Step 15's two assertions were
   `/actuator/health` + root `/health` → 404, untouched by this flip); a finding to read if it touches
   `info`.*
4. **fabric8 K8s discovery on the 2021.0 train.** `spring-cloud-kubernetes-discovery:0.1.6` predates the
   official module; the last train move is the most likely place an *un*-predicted red appears.
   *Read the trace before reaching for any compatibility shim.*
5. **Spring Boot Admin `2.0.6` on Boot 2.6 — now six minors behind.** §6: generation-locked, each minor a
   fresh roll. *If context init throws a `de.codecentric…` `NoClassDefFound`, bump to a 2.6-generation Admin
   and record as a finding.*
6. **Resilience4j starter re-resolves on the 2021.0 circuitbreaker BOM.** Added with no `<version>` (Step
   13), it now resolves through 2021.0's line. *Predicted green; confirm a resilience4j node still appears.*
7. **commons-io / log4j-to-slf4j re-pin holds.** The 2.6 BOM re-pins both (Step 8 findings). *Predicted
   green — `HelloControllerTest` (Tika `CloseShieldInputStream.wrap`) and `LoggingBackendProbeTest` are the
   oracles; the `commons-io 2.13.0` pin and the `log4j-to-slf4j` exclusion stay as Step 8 set them.*
8. **The Step 15 `spring-data-commons-core` drop stays clean.** Finding 15.1 was resolved by removing the
   ancient pin (option 2); Boot 2.6's `RepositoryMetricsAutoConfiguration` still has no Spring Data API to
   mis-trigger against. *Predicted green — the region that red at Step 15 should stay quiet.*

## Step 16 — done criteria

1. `mvn clean package` under **JDK 11** → BUILD SUCCESS, `mvn test` green **`Tests run: 21`**, Failures 0,
   Errors 0; `<java.version>` still **`11`** (Java 17 deliberately not taken), namespace still `javax`.
2. Parent is **`2.6.x`** and `spring-cloud.version` is **`2021.0.x`** (Jubilee) — the train's **last** move
   of this target; it does not move again before Step 18.
3. `dependency:tree` shows **no `io.springfox:*`** and **`org.springdoc:springdoc-openapi-ui`** present with
   its explicit pin; `SwaggerConfig` is the springdoc `OpenAPI`-bean form, no `@EnableSwagger2`/`Docket`.
4. `OpenApiDocsCharacterizationTest` is in the net (20 → 21), asserts `/v3/api-docs` serves a descriptor that
   carries the title `My Product API`, and went **green on the 2.5 baseline first** (verified) — the migration
   was graded, not blind. The path flipped `/v2` → `/v3`; the documented descriptor's title did not.
5. The run count proves all tests are discovered (`Tests run: 21`). **The net is JUnit 5 Jupiter** — the new
   test is Jupiter, matching the existing `@SpringBootTest` characterizations (Actuator, OAuth2) and the
   already-Jupiter `CircuitBreakerFallbackCharacterizationTest`; the JUnit 4 API is not on the classpath.
   *(This corrects the §10.1 "vintage-first" assumption: the repo's net migrated to Jupiter at/around Step 13.
   See the note below — the Step 13/14/15 vintage narrative needs reconciling.)*
6. Any generation-locked satellite bump forced by the gate (Admin, fabric8) is **recorded as a finding** with
   its stack trace — not applied speculatively. No production behaviour changed — only the documentation
   provider and its descriptor path.

> **Cross-step correction — the net is Jupiter, not vintage JUnit 4 (surfaced while building this knot).**
> Steps 13–15 of this document describe the net as "all JUnit-4 / vintage-first" and treat the explicit
> `junit-vintage-engine` as Step 14's headline move. The **actual repo contradicts this**: there is no
> `junit-vintage-engine` in `pom.xml`, the JUnit 4 API is not on the test classpath, and the net is entirely
> JUnit 5 — `CircuitBreakerFallbackCharacterizationTest` was itself migrated to Jupiter at Step 13 (its
> `// step 13:` import of `org.junit.jupiter.api.Test`). So this Step 16 knot is written in Jupiter by
> necessity, not preference. **This does not change Step 16's outcome**, but the Step 13/14/15 vintage
> narrative (and §10.1) is now stale and should be reconciled in a separate pass — the `junit-vintage-engine`
> "headline" and "no test migrated to Jupiter" claims no longer match the code.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step16/boot-2.6-jubilee-springdoc
```

---

## Toward Step 17 — the quiet landing onto Boot 2.7

With springfox retired for `springdoc-openapi`, the train on its final stop (2021.0, Jubilee — which already
serves Boot 2.7), and the doc surface now graded green, the second-heaviest set-piece is behind us. What
remains of the Boot 2.x climb is a single quiet minor and the JDK capstone:

- **Step 17 — Boot 2.6 → 2.7.x (train stays 2021.0).** A Step-15-shaped *restraint* hop: bump the parent
  only, let the stationary 2021.0 BOM re-resolve, JDK still held at 11. Lands the platform on **Boot 2.7**,
  the last stable `javax` plateau. The OAuth2 `302` re-architecture (§10.4) is slotted into 16–17 wherever it
  least disturbs, gated by `OAuth2SecurityCharacterizationTest`.
- **Step 18 — Java 11 → 17 on Boot 2.7.** The isolated JDK capstone (JEP 403 strong-encapsulation fallout
  across BC/itext/the jaxb2 plugin/tika's shaded jars) — the end-state of this target: **Boot 2.7.x + Spring
  Cloud 2021.0 + Java 17**, the full net green, leaving the Boot 3 + jakarta jump as *purely* a namespace
  migration.

---

# Modernization step 17 — the quiet landing onto Boot 2.7: Boot 2.6 → 2.7.x, train stays 2021.0, Java held at 11

> **Status: DRAFT (planned, not yet executed).** This is the **fourth and last version hop** of the target
> and, like Step 15, deliberately the **quietest**. It bumps the **Boot parent only**
> (`2.6.15 → 2.7.x`); the **Spring Cloud train does _not_ move** — `2021.0 (Jubilee)` serves Boot 2.6
> **and** 2.7, so `spring-cloud.version` stays `2021.0.9` (its last move of this target was already spent at
> Step 16). Spring Framework stays **5.3** (same line), namespace stays **`javax`**, and the **JDK stays
> 11** (§7 — held to the Step 18 capstone). The net stays the grader: **21/21 must remain green.** When this
> hop lands, the platform sits on **Boot 2.7 — the last stable `javax` plateau** — with every coordinate but
> the JDK already at the top of the 2.x line, leaving Step 18 a single isolated `11 → 17` move.
>
> **Precondition (verify before the baseline).** Step 16 landed the net at **21** (the new
> `OpenApiDocsCharacterizationTest`). Confirm the baseline below reports `Tests run: 21` on Boot 2.6.15
> before moving — and note the **corrected net reality** Step 16 surfaced (its closing cross-step note): the
> net is **entirely JUnit 5 (Jupiter)**, there is **no `junit-vintage-engine` in `pom.xml`** (verified — the
> dependency list carries none), and the JUnit 4 API is not on the test classpath. The Step 13/14/15
> "vintage-first / explicit vintage engine" narrative is **stale**; Step 17 does not repeat it. The
> run-count discipline still holds for a different reason — a Jupiter `@SpringBootTest` whose context fails
> to load reports errors, not a silently-empty suite, so read `Tests run: 21` *and* `Failures/Errors: 0`.

## Guiding principle — restraint, again, on the same stationary train

Step 16 was the second-heaviest set-piece (springfox → springdoc, plus the train's last move). Step 17 is
its opposite: **no removed train, no removed default, no re-architecture, no new characterization.** It is
the Step-15 shape exactly — bump one coordinate (the parent), let the **same 2021.0 BOM** re-resolve
everything under the new Boot minor, and **leave the JDK at 11** even though 2.7 has long since made Java 17
solid. The value of the hop is the **graded proof** that Boot 2.7 alone — under a train that does not move —
disturbs nothing the net covers. Boot 2.7 is, by reputation, the gentlest minor of the 2.x line (its big
event, the `spring.factories` → `AutoConfiguration.imports` autoconfig relocation, is a **library-author**
concern that surfaces only as deprecation logs for a consumer like this app). So restraint is not just a
posture here; it is what the hop *is*.

## The OAuth2 `302` re-architecture — the §7/§10.4 planning decision, resolved here (and resolved as: *not in this hop*)

§7 left the EOL `spring-security-oauth2` `302` re-architecture as "a planning decision for whichever hop it
least disturbs," and the "Toward" notes narrowed the window to **16 or 17**. Step 16 did not take it; Step
17 is the last hop in that window, so the decision is made **now** — and the decision is **to keep Step 17 a
pure restraint bump and _not_ couple the OAuth2 re-architecture into it.** The reasoning is grounded in the
live tree, not preference:

1. **The OAuth2/`302` surface is _deps-only_ — the same shape as the Hystrix surface (Step 12a).** A read of
   `src/main` shows **exactly three classes** — `HelloApplication`, `HelloController`, `SwaggerConfig` — and
   **no `WebSecurityConfigurerAdapter`, no `@EnableResourceServer`/`@EnableAuthorizationServer`, no
   `SecurityConfig`, no OAuth2 wiring at all.** The `302 → /login` wall is **pure Spring Security default
   autoconfig**; `spring-security-oauth2:2.0.16.RELEASE` and `spring-security-jwt:1.0.9.RELEASE` are EOL
   **classpath weight** with no live consumer in `src`. As with Hystrix, there is **no live OAuth2 behaviour
   to re-architect** — only coordinates and the autoconfigured challenge contract.
2. **Nothing in the 2.6 → 2.7 bump _forces_ the re-architecture.** The forcing function for retiring
   `spring-security-oauth2` is **Boot 3 / jakarta** (where `spring-security-oauth2-autoconfigure` is gone) —
   a **next-target** event. On Boot 2.7 + `javax`, the EOL module still resolves and the `302` contract still
   holds. Taking the re-arch *here* would be re-architecting ahead of any version that demands it —
   manufacturing scope into the quiet landing, the precise inverse of restraint.
3. **Neither neighbour is a good host.** Coupling a behaviour-bearing security re-arch into **this quiet
   parent bump** would destroy the "what does Boot 2.7 alone change?" isolation; coupling it into **Step 18**
   would fuse it with the JEP 403 JDK capstone — two unrelated behaviour-bearing moves in one gate, exactly
   the coupling the methodology forbids.

**Decision (recorded, consistent with §10.4 "inside the walk"):** the OAuth2 `302` re-architecture is **its
own separately-gated sub-hop on the landed Boot 2.7 plateau** — provisionally **Step 17a**, mirroring the
Step 12a → 13 pattern (it already *has* its green oracle, `OAuth2SecurityCharacterizationTest`, so no
net-widen is owed first) — taken **after** Step 17 lands 2.7 and **before** the Step 18 JDK capstone, on a
single JDK (Java 11), with no Boot/train/JDK move. Its exact decomposition (whether it retires the EOL
module outright, what challenge contract the test then asserts — keep `302`, or restore an API-style `401`)
is that sub-hop's own up-front analysis, not this one's. **Step 17 itself touches no security coordinate**;
`OAuth2SecurityCharacterizationTest` rides through unchanged as one of the 21 graders, and its `302`
assertions must stay green across the parent bump.

## The train stays put — what that simplifies (as Step 15)

Step 16 moved parent **and** train together (2.6 left 2020.0 for 2021.0). Step 17 does not: **`2021.0`
serves Boot 2.6 _and_ 2.7**, so `spring-cloud.version` is untouched. The whole Spring-Cloud surface —
OpenFeign, the Resilience4j circuit-breaker starter (Step 13, BOM-managed, no pin), fabric8 K8s discovery —
re-resolves against the **same** 2021.0 BOM it already resolved against at Step 16. The bootstrap-off default
(a 2020.0 event, crossed at Step 14) does not re-trigger. **The only moving part is the Boot 2.7 BOM
re-pin.**

## The mechanical layer — OpenRewrite `UpgradeSpringBoot_2_7`, same JDK-17 detour

As at Steps 14–16, the layer-1 advisor returns: `org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_7`,
run **advisor-then-applier** under **JDK 17** (the recipe runner only), then `JAVA_HOME` switched **back to
11** for the gate. The recipe owns the mechanical parent/property slice; the train property (unchanged here)
and any generation-locked satellite bump are hand-applied and recorded as findings. Use the same plugin /
recipe-artifact form Steps 14–16 settled — the recipe lives in **`rewrite-spring`**.

> **Confirm the JDK before diagnosing anything (Step 8 "Finding 0").** The OpenRewrite run is the only
> JDK-17 touch; the **gate runs on Java 11**. The standing temptation is sharpest on *this* hop: Boot 2.7
> fully supports Java 17, so seeing the recipe run green under 17 invites leaving `JAVA_HOME` there for the
> gate. Don't — Java 17 is held to Step 18 **by decision, not by inability**, so the JDK stays one fixed
> value across every Boot hop and the `11 → 17` move is the single isolated capstone.

## What Step 17 changes — the parent, and nothing else by intent

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. OpenRewrite (advisor→applier) | run `UpgradeSpringBoot_2_7` under JDK 17 — `dryRun` then `run`; accept only the mechanical parent/property slice | recipe, reviewed | the diff is read before `run`; gate re-runs on JDK 11 |
| 4. pom — parent | `spring-boot-starter-parent` `2.6.15` → **`2.7.x`** (last 2.7 patch) | recipe or by hand | net stays 21/21 on JDK 11 |
| — train | **no change** — `2021.0.9` serves Boot 2.7 too (its last move was Step 16) | — | `dependency:tree` re-resolves under the same train |
| — JDK | **no change** — `<java.version>` stays `11` though Boot 2.7 fully supports 17 (§7) | — | gate runs on JDK 11; Java 17 deferred to Step 18 |
| — security | **no change** — OAuth2 `302` re-arch deferred to its own gated sub-hop (Step 17a, above) | — | `OAuth2SecurityCharacterizationTest` rides through unchanged, `302` green |
| 4. pom — satellites (only if the gate demands) | bump generation-locked libs the gate reds — first candidates `spring-boot-admin-starter-client 2.0.6` (§6, now seven minors behind) and `springdoc-openapi-ui 1.5.13` (1.5.x targets Boot 2.4/2.5; 2.7 may want a 1.6.x/1.7.x last-Boot-2.x line) | by hand, **as a finding** | `@SpringBootTest` context init green; `OpenApiDocsCharacterizationTest` green |

No namespace change (`javax`), no production code.

## Step 17 — exact actions (proposed)

From the project root.

### 0–1. Branch and baseline (JDK 11)

```powershell
git switch -c step17/boot-2.7
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
mvn -q test      # existing net GREEN first on 2.6.15/2021.0/Java 11 — confirm "Tests run: 21", Failures/Errors 0
```

### 2. OpenRewrite advisor, then applier — under JDK 17

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.0.x  (recipe runner only — NOT the gate JDK)

# advisor: read the proposed diff, change nothing
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:dryRun `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_7"

# applier: accept the mechanical parent/property slice
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_7"
```

### 3. Hand-apply / confirm the parent (recipe may already have done it); train and JDK unchanged

```xml
<!-- parent -->
<version>2.7.18</version>   <!-- last 2.7.x; exact patch is this step's call -->

<!-- train property: UNCHANGED — 2021.0 serves Boot 2.7 (its last move was Step 16) -->
<spring-cloud.version>2021.0.9</spring-cloud.version>

<!-- JDK: UNCHANGED — held at 11 by decision (§7), though Boot 2.7 fully supports 17 -->
<java.version>11</java.version>
```

### 4. Gate — back on JDK 11

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x again — the gate JDK

mvn clean test      # -> Tests run: 21, Failures: 0, Errors: 0  -> BUILD SUCCESS
mvn clean package   # full repackage, 21/21 green
```

Read the **run count** first (`Tests run: 21`): a `BUILD SUCCESS` with a *shrunken* run count or any
`Errors` is a context-load failure to diagnose, not a pass.

## Predictions to verify by the gate (not findings — §6 discipline)

Each earns the word "finding" only if a stack trace (or a changed run count) confirms it on the gate:

1. **Boot 2.7 is the gentlest minor — context loads unchanged.** The headline 2.7 event
   (`spring.factories` → `META-INF/spring/…AutoConfiguration.imports`) is a library-author migration that
   surfaces to a consumer only as **deprecation logs**, not failures. *Predicted green; any `spring.factories`
   deprecation WARN is a log, not a finding.*
2. **`springdoc-openapi-ui 1.5.13` may be marginal on Boot 2.7.** The landed pin (1.5.13) targets the Boot
   2.4/2.5 line; it was green on 2.6.15 (Step 16) but 2.7 is a minor further on. *Most likely place an
   un-predicted red appears — `OpenApiDocsCharacterizationTest` (`/v3/api-docs` carries `My Product API`) is
   the oracle; if it reds, bump springdoc to the last Boot-2.x line (1.6.x/1.7.x) and record as a finding.
   springdoc 2.x is Boot-3/jakarta and stays out of scope.*
3. **Spring Security 5.7 deprecates `WebSecurityConfigurerAdapter` — inert here.** Boot 2.7 ships Spring
   Security 5.7, whose marquee change is the `WebSecurityConfigurerAdapter` deprecation. The app has **no
   security config** (verified — the `302` wall is pure default autoconfig), so there is nothing to
   deprecate. *Predicted green — `OAuth2SecurityCharacterizationTest`'s three `302`/`200` assertions ride
   through unchanged.*
4. **Spring Boot Admin `2.0.6` on Boot 2.7 — now seven minors behind.** §6: generation-locked, each minor a
   fresh roll; it has survived 2.0 → 2.6. *If context init throws a `de.codecentric…` `NoClassDefFound`,
   bump to a 2.7-generation Admin and record as a finding.*
5. **fabric8 K8s discovery and MQ autoconfig.** Train unchanged (2021.0) so no new Spring-Cloud friction; the
   only variable is the Boot 2.7 BOM. *Predicted green; the bootstrap-off concern was a 2020.0 event crossed
   at Step 14, and the 2021.0 move was crossed at Step 16.*
6. **commons-io / log4j-to-slf4j re-pin holds.** The 2.7 BOM re-pins both (Step 8 findings). *Predicted
   green — `HelloControllerTest` (Tika `CloseShieldInputStream.wrap`) and `LoggingBackendProbeTest` are the
   oracles; the `commons-io 2.13.0` pin and the `log4j-to-slf4j` exclusion stay as Step 8 set them.*
7. **Resilience4j starter re-resolves on the (stationary) 2021.0 circuitbreaker BOM.** Added with no
   `<version>` (Step 13). *Predicted green; confirm a resilience4j node still appears in the tree.*
8. **`spring-boot-properties-migrator` (still on `runtime`) logs any 2.6 → 2.7 rename.** The transitional
   migrator is still present; it is the oracle for any property the 2.7 BOM renames. *Predicted quiet for a
   hello app; a logged rename is information, not a finding.*

## Step 17 — done criteria

1. `mvn clean package` under **JDK 11** → BUILD SUCCESS, `mvn test` green **`Tests run: 21`**, Failures 0,
   Errors 0; `<java.version>` still **`11`** (Java 17 deliberately **not** taken), namespace still `javax`.
2. Parent is **`2.7.x`**; `spring-cloud.version` is **unchanged at `2021.0.9`** (the train did not move — its
   last move was Step 16). Spring Framework is **5.3**.
3. **No security coordinate changed** — the OAuth2 `302` re-architecture is deferred to its own gated sub-hop
   (Step 17a), and `OAuth2SecurityCharacterizationTest` stays green with its `302`/`200` assertions intact.
   The decision and its rationale (deps-only surface, Boot-3 forcing function, restraint) are recorded above.
4. The net is **JUnit 5 (Jupiter)** throughout; **no `junit-vintage-engine`** is added or expected (the Step
   13/14/15 vintage narrative is stale, per Step 16's cross-step correction). The run count proves all 21
   tests are discovered and pass.
5. Any generation-locked satellite bump forced by the gate (Admin or springdoc first) is **recorded as a
   finding** with its stack trace — not applied speculatively. No production code added.
6. The JDK-held decision is recorded explicitly: Boot 2.7 fully supports Java 17, and this hop declines it on
   purpose (§7), so the JDK stays one fixed value through to the Step 18 capstone.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step17/boot-2.7
```

---

## Toward Step 17a / Step 18 — the OAuth2 sub-hop, then the JDK capstone

With Boot 2.7 landed, the platform rests on the **last stable `javax` plateau**: every coordinate but the
JDK is at the top of the 2.x line, the train is on its final stop (2021.0, Jubilee), and the net is green at
21/21. Two moves remain to close the target:

- **Step 17a — OAuth2 `302` re-architecture off the EOL `spring-security-oauth2` module** (its own gate, no
  Boot/train/JDK move, on Java 11). Resolved above as a deferral *out* of Step 17: a deps-only surface whose
  only forcing function is Boot 3 / jakarta, taken now — on the green 2.7 plateau — to keep the next target a
  *pure* namespace migration. Gated by `OAuth2SecurityCharacterizationTest`; its decomposition (retire the
  EOL coordinates; keep the `302` contract or restore an API-style `401`) is that hop's own up-front
  analysis.
- **Step 18 — Java 11 → 17 on Boot 2.7.** The isolated JDK capstone (JEP 403 strong-encapsulation fallout
  across BC/itext/the jaxb2 plugin/tika's shaded jars) — the end-state of this target: **Boot 2.7.x + Spring
  Cloud 2021.0 + Java 17**, the full net green, leaving the Boot 3 + jakarta jump as *purely* a namespace
  migration.

---

# Modernization step 17a — retire the EOL `spring-security-oauth2` module, keep the `302` wall green

> **Status: DRAFT (planned, not yet executed).** This is the §10.4 / §7 OAuth2 re-architecture, taken as
> its **own separately-gated sub-hop on the landed Boot 2.7 plateau** — the deferral *out* of Step 17,
> resolved there. It is the Step 12a → 13 pattern run a second time: a graded oracle already exists
> (`OAuth2SecurityCharacterizationTest`, banked at Step 8), so **no net-widen is owed first** — Step 17a is
> the move that oracle was built to grade. It runs **with no Boot parent change, no train change, no JDK
> change, no namespace change** (`2.7.18` / `2021.0.9` / Java 11 / `javax` all stand) — only the EOL
> security coordinate and the supported starter that replaces its silent role. The net stays the grader:
> **21/21 must remain green.** The hop is landed **before** the Step 18 JDK capstone so the `11 → 17` move
> stays a pure JDK hop with no EOL-OAuth2 reflective surface still in play.
>
> **Why this hop exists at all (the §9 payoff).** The only forcing function for retiring
> `spring-security-oauth2` is **Boot 3 / jakarta**, where `spring-security-oauth2-autoconfigure` is gone.
> Paying it *here*, on the green `javax` plateau, in isolation, is exactly what keeps the next target a
> *pure* namespace migration — the same decoupling logic that put Java 17 (Step 18) on this plateau rather
> than fused into Boot 3.

## Guiding principle — the same restraint, but the surface is load-bearing, not inert

Step 17's deferral note characterized the OAuth2 surface as "deps-only — the same shape as the Hystrix
surface (Step 12a)." That framing is **half right and half stale**, and Step 17a's first duty is to correct
it from the live tree before moving (the §6/§8 discipline; the same way Step 16 corrected the vintage
narrative and Step 17 carried that correction forward):

- **Right:** no `src` class imports `org.springframework.security.oauth.*`; there is **no**
  `WebSecurityConfigurerAdapter`, no `SecurityFilterChain`, no `@EnableResourceServer` /
  `@EnableAuthorizationServer`, no `SecurityConfig`. A full `src` grep for `org.springframework.security`
  returns **only** the JWT test's four imports (below). So there is **no live OAuth2 wiring to rewrite** —
  the `302 → /login` wall is **pure Spring Security _default_ autoconfig**, exactly as Step 8 recorded.
- **Stale / wrong:** the claim that `spring-security-oauth2` is therefore inert "classpath weight" like the
  Netflix jars. It is **not inert — it is load-bearing.** `dependency:tree` shows it is the **sole supplier**
  of the Spring Security runtime that *produces* the wall (the §-below dependency fact). Remove it naively
  and the wall it anchors disappears — the opposite of the Hystrix case, where removal changed nothing the
  net saw.
- **Also wrong:** the Step 17 note lumped `spring-security-jwt:1.0.9.RELEASE` in as "no live consumer in
  src." It **has** one — `JwtBcPkixCharacterizationTest` (Step 3) imports
  `org.springframework.security.jwt.{Jwt, JwtHelper, crypto.sign.RsaSigner, crypto.sign.RsaVerifier}` and
  round-trips a real RSA-signed JWT through the live bcpkix path. `spring-security-jwt` is a **separate** EOL
  module with its **own** live consumer and its **own** re-architecture (the JWT signing path), and it does
  **not** supply `spring-security-web`/`-config`. It is therefore **out of scope for this hop** (§ "scope"
  below).

So Step 17a is not "delete dead weight." It is: **swap the EOL module that silently anchors the `302`
contract for the supported, Boot-managed starter that anchors the same contract identically** — coordinates
only, the wall byte-for-byte preserved, graded green throughout. The restraint posture of Step 13 holds:
**no production code is added** (no `SecurityFilterChain`, no restored-`401` config); the test's three
assertions do not change; what changes is the coordinate the Spring Security runtime resolves *from*.

## The one non-obvious dependency fact this hop turns on (verified from the live tree)

The pom carries **no `spring-boot-starter-security`**. `dependency:tree` on the current (Boot 2.7.18) pom
shows the entire Spring Security runtime arrives **transitively through the EOL OAuth2 module**:

```
+- org.springframework.security.oauth:spring-security-oauth2:jar:2.0.16.RELEASE:compile
|  +- org.springframework.security:spring-security-core:jar:5.7.11:compile
|  |  \- org.springframework.security:spring-security-crypto:jar:5.7.11:compile
|  +- org.springframework.security:spring-security-config:jar:5.7.11:compile   <-- enables the autoconfig
|  \- org.springframework.security:spring-security-web:jar:5.7.11:compile      <-- builds the filter chain
+- org.springframework.security:spring-security-jwt:jar:1.0.9.RELEASE:compile  <-- SEPARATE module, live consumer
   \- org.springframework.security:spring-security-rsa:jar:1.0.12.RELEASE:compile
```

The consequence is load-bearing and is the whole reason this is a *re-architecture*, not a deletion:

> **`spring-security-web` + `spring-security-config:5.7.11` are on the classpath _only_ because
> `spring-security-oauth2` drags them.** They — not the OAuth2 module's own classes — are what trip Spring
> Boot's `SecurityAutoConfiguration` / `UserDetailsServiceAutoConfiguration` into building the default
> filter chain that returns `302 → /login`. **Deleting `spring-security-oauth2` alone removes Spring
> Security entirely → no filter chain → `/` answers `200` unauthenticated → all three
> `OAuth2SecurityCharacterizationTest` assertions flip.** To retire the EOL coordinate *and* keep the wall,
> Step 17a must **re-supply the same Spring Security runtime from a supported source** before (or with) the
> removal: add **`spring-boot-starter-security`** (Boot-managed, no `<version>`), which pulls the **same**
> `spring-security-web`/`-config`/`-core:5.7.11` the OAuth2 module was pulling. The default autoconfig is
> the identical code path, so the `302` contract is reproduced byte-for-byte.

`spring-security-jwt`'s subtree (`spring-security-rsa:1.0.12` → bcpkix, exercised by the Step 3 test) is
**untouched** — it supplies no web/config and has its own consumer, so it neither anchors the wall nor is
disturbed by this swap.

## Resolving the open question (raised in Step 17's deferral note, decided here with the tree in hand)

The deferral note left two questions for "this sub-hop's own up-front analysis": **(a)** retire the EOL
coordinates outright, and **(b)** keep the `302` contract or restore an API-style `401`. Decided:

1. **Keep the `302` contract — do _not_ restore `401`.** Restoring the Boot-1.5-era Basic-auth `401`
   challenge would require **adding production security config** — a `@Configuration` exposing a
   `SecurityFilterChain` bean (the Boot 2.7 / Spring Security 5.7 component-style replacement for the
   deprecated `WebSecurityConfigurerAdapter`) that enables `httpBasic` and disables `formLogin`. That is
   exactly the speculative production surface Step 13 refused to manufacture: the app has no API client that
   needs `401`, and inventing security config to change a challenge shape no consumer depends on is
   manufactured scope, not characterized behaviour. **The `302` wall is the real, graded contract; it
   stays.** `OAuth2SecurityCharacterizationTest`'s three assertions (`302` no-creds, `200` valid-creds,
   `302` wrong-creds) are unchanged.
2. **Retire `spring-security-oauth2` outright — but re-supply its Spring Security runtime, don't just
   delete it.** Per the dependency fact above: remove the EOL coordinate, add `spring-boot-starter-security`
   so the wall's anchor moves onto a supported, Boot-managed coordinate. Coordinates only; no production
   code.

This is safe to decide now (not deferred to Step 18) because the survivor is already present and
version-aligned on the **current** plateau: `spring-boot-starter-security` is managed by the Boot 2.7.18
BOM and resolves the **same `5.7.11`** Spring Security artifacts already on the tree — verified above, not
predicted.

## Scope — what this hop deliberately leaves alone (`spring-security-jwt`)

`spring-security-jwt:1.0.9.RELEASE` is **also** an EOL spring-security-oauth-family module, but it is **not**
this hop's target:

- It has a **live consumer** (`JwtBcPkixCharacterizationTest`), so retiring it is **compiler-enforced
  re-architecture** of the JWT signing path (Step 13's situation), not a coordinate swap — its successor is
  Nimbus JOSE via `spring-security-oauth2-jose`, a different surface with a different oracle.
- It **does not anchor the `302` wall** (supplies no web/config), so leaving it changes nothing
  `OAuth2SecurityCharacterizationTest` grades.
- Its only forcing function is, again, **Boot 3 / jakarta** — so it can ride the green 2.7 plateau and be
  retired in its **own** gated sub-hop (a candidate **Step 17b**, or folded into the Boot 3 jump's JWT
  work). Coupling it into Step 17a would fuse two unrelated EOL retirements — one a coordinate swap, one a
  signing-path re-architecture — into a single gate, the precise coupling the methodology forbids.

**Step 17a touches no JWT coordinate.** `JwtBcPkixCharacterizationTest` rides through unchanged as one of
the 21 graders.

## The JDK tension — single JDK, no detour (as Step 12a / 13)

Step 17a carries **no OpenRewrite recipe** — no Boot or train version moves — so, like Step 12a and Step 13,
there is **no JDK-17 detour**. The whole hop runs on a **single JDK: Java 11**, the working gate JDK held
through Step 17. Confirm `java -version` reports `11` before diagnosing anything (Step 8 "Finding 0"); the
Java 11 illegal-reflective-access *warnings* remain expected and are **not findings**. Java 17 stays the
isolated Step 18 capstone.

## What Step 17a changes — one EOL coordinate out, one supported starter in

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 4. pom — remove | delete `org.springframework.security.oauth:spring-security-oauth2:2.0.16.RELEASE` | by hand | `dependency:tree` shows no `spring-security-oauth:*`; net stays 21/21 |
| 4. pom — add | add `org.springframework.boot:spring-boot-starter-security` (**no `<version>`** — Boot 2.7.18 BOM-managed) | by hand | re-supplies `spring-security-web`/`-config`/`-core:5.7.11`; `302` wall reproduced identically |
| — JWT | **no change** — `spring-security-jwt:1.0.9.RELEASE` kept (separate EOL module, live consumer, own re-arch) | — | `JwtBcPkixCharacterizationTest` rides through unchanged |
| — contract | **no change** — keep the `302` wall; **no** `SecurityFilterChain` / restored-`401` production config | — | `OAuth2SecurityCharacterizationTest`'s three `302`/`200`/`302` assertions unchanged |

No parent/train/JDK/namespace edit; no production code.

## Step 17a — exact actions (proposed)

From the project root, on **JDK 11** (no JDK-17 detour — no OpenRewrite recipe).

### 0–1. Branch and baseline (JDK 11)

```powershell
git switch -c step17a/retire-spring-security-oauth2
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-11.0.30'; $env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 11.0.x
mvn -q test      # existing net GREEN first on 2.7.18/2021.0/Java 11 — confirm "Tests run: 21", Failures/Errors 0
```

### 2. Confirm the dependency fact before moving (so the gate result is interpretable)

```powershell
mvn dependency:tree "-Dincludes=org.springframework.security*:*" | Select-String "security"
#   expect: spring-security-web / -config / -core:5.7.11 hanging UNDER spring-security-oauth2:2.0.16.RELEASE,
#           and spring-security-jwt:1.0.9.RELEASE -> spring-security-rsa:1.0.12 as a SEPARATE node.
#   This is the proof that removing oauth2 WITHOUT adding starter-security would scrub web/config (the wall).
```

### 3. pom — swap the EOL module for the supported starter

Remove the EOL OAuth2 dependency:

```xml
<!-- DELETE: the EOL module that silently anchored the 302 wall via its transitive web/config -->
<dependency>
    <groupId>org.springframework.security.oauth</groupId>
    <artifactId>spring-security-oauth2</artifactId>
    <version>2.0.16.RELEASE</version>
</dependency>
```

Add the supported, Boot-managed security starter in its place (no version — BOM-managed):

```xml
<!-- Step 17a: re-supply the Spring Security runtime (web/config/core 5.7.11) from a SUPPORTED coordinate.
     The EOL spring-security-oauth2 was the sole transitive source of these jars; the default autoconfig
     they enable is what builds the 302 -> /login wall OAuth2SecurityCharacterizationTest pins. Swapping the
     anchor onto the Boot-managed starter keeps that contract byte-identical while retiring the EOL module
     ahead of the Boot 3/jakarta forcing function. spring-security-jwt is a SEPARATE EOL module (live
     consumer: JwtBcPkixCharacterizationTest) and is intentionally NOT touched here. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>
```

`spring-security-jwt` and its `spring-security-rsa`/bcpkix subtree are **left exactly as they are.**

### 4. Verify the surface moved, then gate — on JDK 11

```powershell
mvn dependency:tree "-Dincludes=org.springframework.security*:*" | Select-String "security"
#   expect: NO org.springframework.security.oauth:spring-security-oauth2,
#           spring-security-web/-config/-core:5.7.11 now hanging under spring-boot-starter-security,
#           spring-security-jwt:1.0.9.RELEASE -> spring-security-rsa:1.0.12 STILL present (untouched).

mvn clean test      # -> Tests run: 21, Failures: 0, Errors: 0  -> BUILD SUCCESS
mvn clean package   # full repackage, 21/21 green
```

Read the **run count** first (`Tests run: 21`): a `BUILD SUCCESS` with a shrunken count or any `Errors` is a
context-load failure to diagnose, not a pass.

## Predictions to verify by the gate (not findings — §6 discipline)

Each earns the word "finding" only if a stack trace (or a changed run count) confirms it on the gate:

1. **The `302` wall is reproduced identically.** `spring-boot-starter-security` brings the same
   `spring-security-web`/`-config:5.7.11`, so the same default `SecurityAutoConfiguration` filter chain is
   built. *Predicted green — `OAuth2SecurityCharacterizationTest`'s three assertions ride through unchanged.*
   This is the **most likely place an _un_predicted red appears** (if the starter's autoconfig differs in any
   defaulting from the bare-jars path), so read the trace before declaring done.
2. **The deterministic user still resolves.** `UserDetailsServiceAutoConfiguration` honours
   `spring.security.user.*` whenever `spring-security-config` is present — unchanged across the swap.
   *Predicted green — the `200` valid-creds assertion holds.*
3. **`JwtBcPkixCharacterizationTest` is unaffected.** `spring-security-jwt`/`-rsa`/bcpkix are untouched; the
   RSA JWT round-trip path does not depend on the OAuth2 module. *Predicted green.*
4. **springdoc / `OpenApiDocsCharacterizationTest` unchanged.** It already passes *with* Spring Security on
   the classpath; the filter chain after the swap is byte-identical, so `/v3/api-docs` behaves as before.
   *Predicted green.*
5. **No transitive version drift.** Both the removed module and the added starter resolve Spring Security at
   `5.7.11` (Boot 2.7.18 BOM). *Predicted green; confirm the tree still reads `5.7.11`.*
6. **Spring Security 5.7's `WebSecurityConfigurerAdapter` deprecation stays inert.** The app has no security
   config to deprecate (verified), and Step 17a adds none. *Predicted green — no deprecation surfaces because
   there is nothing to deprecate.*

## Step 17a — done criteria

1. `mvn clean package` under **JDK 11** → BUILD SUCCESS, `mvn test` green **`Tests run: 21`**, Failures 0,
   Errors 0; parent still **`2.7.18`**, `spring-cloud.version` still **`2021.0.9`**, `<java.version>` still
   **`11`**, namespace still `javax`.
2. `dependency:tree` shows **no `org.springframework.security.oauth:spring-security-oauth2`**;
   `spring-security-web`/`-config`/`-core:5.7.11` now resolve under **`spring-boot-starter-security`**; and
   `spring-security-jwt:1.0.9.RELEASE` → `spring-security-rsa:1.0.12` is **still present, untouched**.
3. `OAuth2SecurityCharacterizationTest`'s three assertions are **unchanged** and green — the `302` wall held
   across the coordinate swap. The EOL module retired; the contract did not move. **No `SecurityFilterChain`
   / restored-`401` production config was added** (the §-above decision: keep `302`, no manufactured scope).
4. **`spring-security-jwt` is deliberately out of scope** and recorded as such — a separate EOL module with
   its own live consumer (`JwtBcPkixCharacterizationTest`) and its own deferred re-architecture (candidate
   Step 17b / the Boot 3 JWT work).
5. The other 18 tests stay exactly as Steps 8–17 left them; the net is JUnit 5 (Jupiter) throughout — no
   vintage engine added or expected (Step 16's standing correction).
6. The two stale claims in Step 17's deferral note are corrected on the record: `spring-security-oauth2` was
   **load-bearing** (sole supplier of the wall's web/config), not inert; and `spring-security-jwt` **has** a
   live consumer. Both corrections are grounded in `dependency:tree` + the `src` grep, not asserted.

## Rollback

```powershell
git restore --staged --worktree .
git switch main
git branch -D step17a/retire-spring-security-oauth2
```

---

## Toward Step 18 — the JDK capstone, on a fully-supported `javax` plateau

With `spring-security-oauth2` retired onto the supported `spring-boot-starter-security` and the `302`
contract held green, the last EOL coordinate that the Boot 3 / jakarta jump would otherwise have forced into
the same step as the namespace rename is paid here, on the green 2.7 plateau, in isolation. One known EOL
module remains by design — `spring-security-jwt` (its live JWT-signing consumer makes it a separate
re-architecture, deferred as candidate Step 17b). The platform now sits on **Boot 2.7.18 + Spring Cloud
2021.0.9 + Java 11**, net green at 21/21, with only the JDK below the top of the line:

- **Step 18 — Java 11 → 17 on Boot 2.7.** The isolated JDK capstone (JEP 403 strong-encapsulation fallout
  across BC/itext/the jaxb2 plugin/tika's shaded jars) — the end-state of this target: **Boot 2.7.x + Spring
  Cloud 2021.0 + Java 17**, the full net green, leaving the Boot 3 + jakarta jump (where
  `spring-security-jwt` and the `javax → jakarta` rename are taken together) as *purely* a namespace
  migration on a modern JDK.
