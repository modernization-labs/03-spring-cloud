# Modernization journey

> A rigorous, reproducible, **baby-step** modernization of the legacy stack reproduced and
> analysed in [README.md](README.md). Every step is human-executed, individually reversible, and
> gated by a verification check (the growing characterization net). Steps 1-7 rationalize the
> stack *within* its Boot 1.5 / Spring Cloud Edgware generation; Step 8 is the coupled jump to
> Boot 2.0 + Finchley. Each step has its own "Toward Step N+1" preview of what it unlocks.

# modernization - step 1

> **Goal of this document:** a precise, reproducible, human-executable guide for the
> *first* baby step of the modernization journey. No code is changed by Claude — every
> action below is to be performed by a human. Each step is reversible and gated by a
> verification check.

## Guiding principle

The first move is **never a version jump**. The big jumps on this stack are all coupled
and far away:

- Boot **1.5 → 2.0** drags Spring Cloud **Edgware → Finchley** (they are version-locked —
  you cannot move one without the other), plus the OAuth2 autoconfig overhaul and the
  Actuator endpoint rewrite.
- Boot **2.7 → 3.0** forces **Java 17** and the **`javax` → `jakarta`** namespace migration.
- Spring Security OAuth2 2.0.16 (EOL) → the resource-server in core Spring Security.

None of those can be first. Before any of them we get the **platform to the top of its
current generation** — staying on the same minor line, same Java, same namespace. This is
the lowest-risk, fully-reversible anchor every later hop builds on.

## Edgware.SR4 and spring-cloud-netflix-core 1.4.4.RELEASE

These two coordinates sit at **two different layers** of Maven's version-resolution
machinery.

### They live at two different layers

**`Edgware.SR4`** is not a single library — it's a **release-train name** that resolves to a
**BOM** (Bill of Materials). In `pom.xml` it's used like this:

```xml
<spring-cloud.version>Edgware.SR4</spring-cloud.version>
...
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-dependencies</artifactId>
  <version>${spring-cloud.version}</version>
  <type>pom</type>
  <scope>import</scope>     <!-- imported into dependencyManagement -->
</dependency>
```

That BOM is a **curated list of mutually-tested versions** for every Spring Cloud
sub-project. Spring Cloud ships as trains (station names, alphabetical: … Dalston,
**Edgware**, Finchley …), each aligned to a Spring Boot generation — **Edgware ↔ Boot
1.5.x**. The component version *numbers* inside a train are independent of the train name:
under Edgware, `spring-cloud-commons`/`-context` are on the **1.3.x** line and
`spring-cloud-netflix` is on the **1.4.x** line. A service release (`.SR4`) just advances
those pinned patch levels to a tested-together set.

**`spring-cloud-netflix-core:1.4.4.RELEASE`** is one **concrete artifact** *inside* the
netflix sub-project that the Edgware train curates. So `netflix-core` is a *member of the
set* that `Edgware.SR4` governs.

### The key interaction: an explicit version overrides the BOM

The BOM only sets a *managed default*; it takes effect when you declare a dependency
**without** a `<version>`. But the pom declares an explicit one:

```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-netflix-core</artifactId>
  <version>1.4.4.RELEASE</version>   <!-- explicit: wins over the BOM -->
</dependency>
```

An explicit `<version>` on a direct dependency **always beats** a BOM-managed version. So
even though `Edgware.SR4` would manage the netflix line to **1.4.5.RELEASE**, this one
artifact is forced back to **1.4.4**.

### The visible consequence (the skew)

Both numbers coexist in `dependency-tree.txt`:

| Artifact | Version | Where it comes from |
|---|---|---|
| `spring-cloud-netflix-core` | **1.4.4** | explicit pin (overrides BOM) |
| `spring-cloud-starter-netflix-hystrix` | **1.4.4** | explicit pin |
| `spring-cloud-starter-netflix-ribbon` | **1.4.5** | BOM-managed (pulled transitively, no pin) |
| `spring-cloud-starter-netflix-archaius` | **1.4.5** | BOM-managed |

In one sentence: **`Edgware.SR4` is the release train (a BOM) that *would* set the whole
`spring-cloud-netflix` family to 1.4.5; `spring-cloud-netflix-core:1.4.4.RELEASE` is a
hand-pinned member of that family that opts out of the BOM's choice** — producing the
intentional 1.4.4 ↔ 1.4.5 minor skew the pom comment calls "same stack, not same tree."

This is also why **Step 1** bumps `Edgware.SR4 → Edgware.SR6` (the BOM/train layer) but
deliberately leaves the explicit `1.4.4` netflix pins untouched: they're independent levers,
and rationalizing the hand-pins is a later hop.

## Step 1 — what changes

Consolidate to the **latest patch releases of the current generation**. Two coordinates,
nothing else:

| Coordinate | From | To |
|---|---|---|
| `spring-boot-starter-parent` (`<parent><version>`) | `1.5.17.RELEASE` | `1.5.22.RELEASE` |
| `<spring-cloud.version>` property | `Edgware.SR4` | `Edgware.SR6` |

**Do NOT** in this step: move to Boot 2.x / Finchley, change `java.version`, touch the
`javax → jakarta` namespace, or alter any of the hand-pinned override versions
(`tomcat.version`, the `bcprov`/`itext`/`tika`/`ojdbc` pins, the promoted transitives).
Those are later hops. Keep the diff to exactly two lines.

## Step 1 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, run from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Set the Java 8 toolchain for this shell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
mvn -version     # confirm Maven picks up the same JDK
```

### 1. Capture the "before" baseline (the regression oracle)

Freeze the current resolved tree and the runtime-probe results so Step 1 can be proven
behavior-preserving.

```powershell
mvn -q dependency:tree -DoutputFile=dependency-tree.before.txt
mvn -q clean test       # probe tests must be GREEN before we touch anything
```

Confirm `dependency-tree.before.txt` was written and the test run reported
**BUILD SUCCESS** (including `BcClasspathProbeTest` and `JavaeeApiProbeTest`). If the
baseline is not green, stop — fix the baseline before migrating.

### 2. Edit `pom.xml` — change line A (the Boot parent)

Open `pom.xml`. In the `<parent>` block (around line 22), change:

```xml
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>1.5.17.RELEASE</version>   <!-- change to 1.5.22.RELEASE -->
        <relativePath/>
    </parent>
```

so the version reads `1.5.22.RELEASE`.

### 3. Edit `pom.xml` — change line B (the Spring Cloud train)

In `<properties>` (around line 43), change:

```xml
        <spring-cloud.version>Edgware.SR4</spring-cloud.version>   <!-- change to Edgware.SR6 -->
```

so it reads `Edgware.SR6`. **Make no other edits** to `pom.xml`.

### 4. Rebuild and run the verification gate

```powershell
mvn clean package
```

Expect **BUILD SUCCESS**, the JUnit 4 tests passing, and the fat jar repackaged.

### 5. Smoke-test the running app

```powershell
java -jar target\my-product-1.0.0.jar > app.log 2>&1   # run in a second window
```

Then, from another shell:

curl -u user:<password> http://localhost:8080/

```powershell
$pw   = (Select-String -Path app.log -Pattern 'security password: (.+)$').Matches.Groups[1].Value.Trim()
$cred = New-Object System.Management.Automation.PSCredential('user', (ConvertTo-SecureString $pw -AsPlainText -Force))
(Invoke-WebRequest -Uri http://localhost:8080/ -Credential $cred -UseBasicParsing).Content
# -> hello world

(Invoke-WebRequest -Uri http://localhost:8080/ -UseBasicParsing).StatusCode
# -> throws / 401 (no credentials, as expected)
```

Stop the app when done (Ctrl-C in its window, or `Stop-Process`).

### 6. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree -DoutputFile=dependency-tree.after.txt
Compare-Object (Get-Content dependency-tree.before.txt) (Get-Content dependency-tree.after.txt)
```

Inspect the delta. A patch bump within the same generation should only move BOM-managed
transitives by patch levels (and pick up CVE fixes). **Record the diff** at the bottom of
this section. Any *major/minor* movement, or a dropped/added framework, is a finding to
investigate before declaring Step 1 done.

## Step 1 — done criteria (all must hold)

1. `mvn clean package` → **BUILD SUCCESS**, JUnit 4 tests pass.
2. App starts (`Started HelloApplication`) and `GET /` behaves as before
   (401 unauthenticated, 200 `hello world` authenticated).
3. **Probe tests stay green.** If `BcClasspathProbeTest` or `JavaeeApiProbeTest` now fails,
   the classpath winner changed — record it as a finding; do not silently absorb it.
4. The `before`/`after` tree diff is reviewed, contains only patch-level movement, and is
   pasted into this section.

## Rollback

Step 1 is two lines. To revert: restore `1.5.17.RELEASE` and `Edgware.SR4` in `pom.xml`,
then `mvn clean package`. Delete `dependency-tree.before.txt` /
`dependency-tree.after.txt` if you do not want to keep them.

## Prerequisite for Step 2 (note, not an action now)

The current regression net is thin: the probe tests guard classpath hazards, but `GET /`
plus the leaf-library helpers (`shout`, `toPdf`, `extractText`, …) are the only behavioral
coverage. That is enough to *anchor* this patch bump, but **before the Boot 1.5 → 2.0 hop**
(where behavior actually shifts) widen the characterization tests first.

## The journey this unlocks (preview, not a commitment)

`1.5.22 + Edgware.SR6` → rationalize the hand-pinned smells (log4j 2.7↔2.20, the 4-way
BouncyCastle split) → **Boot 2.0 + Finchley** → walk 2.1 → 2.7 → migrate OAuth2 off the EOL
standalone module → **Java 11 → 17** → **Boot 3.0 + `jakarta`** → 3.x. Each hop gated by
the same verification harness used above.

# modernization step 2 - log4j2 skew

> **Goal of this document:** a precise, reproducible, human-executable guide for the
> *second* baby step. As in Step 1, no code is changed by Claude — every action below is
> performed by a human, each step is reversible, and each is gated by a verification check.

## Guiding principle

Step 1 took the platform to the top of its current generation (`Boot 1.5.22 + Edgware.SR6`).
The next move is still **not** a version jump — it's the first of the *smell-rationalization*
hops the roadmap calls for, done on the same generation, same Java, same namespace. We pick
the **single most isolated dependency smell first**: the log4j2 version skew. The messier
4-way BouncyCastle split is deliberately left for Step 3 (it is already characterized by
`BcClasspathProbeTest`, so deferring it loses no coverage).

## The smell (grounded in `dependency-tree.after.txt`)

`tika-app:2.9.0` transitively drags a log4j2 trio onto the **compile** classpath:

```
org.apache.logging.log4j:log4j-core:2.7   →  log4j-api:2.7      (lines 207–208)
org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0                (line 209)
```

There are **three** distinct incoherences here, not one:

1. **Internal log4j2 skew:** `log4j-core`/`log4j-api` sit at **2.7** while `log4j-slf4j2-impl`
   sits at **2.20.0** — a 13-minor gap. The 2.20 slf4j binding is built against `log4j-api`
   2.20, not 2.7. The 2.7 pin comes from the **Boot 1.5 parent's `<log4j2.version>` property**
   (the parent never knew the `slf4j2` binding, so that one rides at Tika's 2.20.0).
2. **CVE surface:** `log4j-core:2.7` is Log4Shell-era (CVE-2021-44228 / -45046) — a jar on
   the classpath that any SCA scanner will flag.
3. **Binding vs API mismatch:** `log4j-slf4j2-impl` is the **SLF4J 2.x** binding, but the
   *declared* `slf4j-api` is **1.7.25** (line 223). SLF4J 1.7 discovers bindings via
   `org.slf4j.impl.StaticLoggerBinder`; the 2.x binding instead ships a `ServiceLoader`
   provider. So *on paper* the 2.x binding looks inert and Logback looks like the winner.

### Finding: the tree lies — the live backend is tika-app's shaded log4j2

> **A first draft of this step assumed "Logback 1.5's default is the live backend, the log4j2
> jars are inert dead weight." A characterization probe run *before* any change proved that
> wrong** — the same lesson, and the same culprit, as the BouncyCastle section above.

A runtime probe (`LoggingBackendProbeTest`, added below) reports:

```
[LOG-PROBE] active ILoggerFactory : org.apache.logging.slf4j.Log4jLoggerFactory
[LOG-PROBE] org.slf4j.LoggerFactory from : .../tika-app/2.9.0/tika-app-2.9.0.jar
[LOG-PROBE] binding factory from        : .../tika-app/2.9.0/tika-app-2.9.0.jar
```

The active backend is **log4j2, not Logback** — and the `org.slf4j.LoggerFactory` that chose
it is **not** the declared `slf4j-api:1.7.25`. `tika-app:2.9.0` is a shaded uber-jar that
bundles, **unrelocated**, a full SLF4J 2.x + log4j2 **2.20.0** stack:
`org/slf4j/LoggerFactory.class` (2.x), the `META-INF/services/org.slf4j.spi.SLF4JServiceProvider`
registration, `org/apache/logging/slf4j/Log4jLoggerFactory.class`, **and** the log4j2 core
engine (`log4j-core`/`log4j-api` 2.20.0). Because tika-app sorts early on the classpath, its
shaded `org.slf4j.LoggerFactory` (2.x, `ServiceLoader`-based) **shadows** `slf4j-api:1.7.25`,
finds log4j2's shaded `SLF4JServiceProvider`, and binds to log4j2. Logback's
`StaticLoggerBinder` is never consulted.

So, exactly like `bcprov-jdk15on:1.57`, **the standalone `log4j-core`/`log4j-api`/`-slf4j2-impl`
nodes in the tree are effectively dead for the active path** — what actually logs is tika-app's
shaded log4j2 2.20.0. The dependency tree points at the wrong jars; only the JVM gives the
truth.

### What this reframes — and why the change is still worth doing (and now provably safe)

This kills the original "fixing the live logger" rationale, but the align is still a legitimate
baby step, now with honest framing:

- **It is classpath / SCA hygiene, not a behavior change.** Bumping the standalone trio
  `2.7 → 2.20.0` drops the Log4Shell `2.7` jars from disk (what scanners flag) and makes the
  tree internally coherent — converging the standalone jars onto the **same 2.20.0 that
  tika-app already shades and actually runs**.
- **It is provably behavior-neutral.** The active logger is tika-app's shaded copy, which the
  align does not touch. `LoggingBackendProbeTest` pins that the winner stays
  `Log4jLoggerFactory`-from-tika-app before and after — so this step changes the *tree*, not
  the *running logger*.
- **A deeper cleanup is now a candidate for a later hop:** since the standalone trio is proven
  dead weight, *excluding* it from `tika-app` (the deferred option (b) below) becomes the more
  honest long-term move — but that changes Tika's declared classpath, so it stays a later,
  separately-gated step, not this one.

## Step 2 — what changes

**Chosen approach (a): align, don't remove.** Override the parent's `<log4j2.version>` so
`log4j-core` and `log4j-api` move up to **2.20.0** — the version `log4j-slf4j2-impl` already
sits at *and* the version tika-app already shades and runs (see the Finding above). This makes
the standalone trio internally coherent and drops the Log4Shell `2.7` jars, in **one property
line** — the truest baby step (coherence without removing anything from Tika's classpath). It
does **not** alter the running logger, which is tika-app's shaded copy; the probe proves that.
log4j 2.20.0 still targets **Java 8**, so the toolchain is unchanged.

| Coordinate | From | To |
|---|---|---|
| `<log4j2.version>` property (overrides Boot 1.5 parent's `2.7`) | `2.7` (inherited) | `2.20.0` |

**Alternative (b), deferred:** *exclude* the standalone log4j2 transitives from `tika-app`
entirely — the Finding proves they are dead weight, duplicated by tika-app's own shaded copy.
This removes the redundant (and CVE-flagged) jars outright and shrinks the classpath, but it
changes Tika's declared dependency set, so it carries more risk than a one-line version align.
Note it would **not** change the running logger either (that lives inside tika-app's shaded
classes), so it is purely further tree/SCA cleanup. Revisit it as a later hop once the safety
net is wider — not now.

**Do NOT** in this step: move Boot/Spring Cloud versions, touch `java.version` or the
`javax → jakarta` namespace, alter the `tomcat`/`bcprov`/`itext`/`tika`/`ojdbc` pins, or
rationalize the BouncyCastle split. Keep the production diff to exactly one line in `pom.xml`
(plus one new test class).

## Step 2 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Set the Java 8 toolchain for this shell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
```

### 1. Capture the "before" baseline (the regression oracle)

```powershell
mvn -q dependency:tree -DoutputFile=dependency-tree.step2-before.txt
mvn -q clean test       # all probe + controller tests must be GREEN before touching anything
```

Confirm the file was written and the run reported **BUILD SUCCESS**. If the baseline is not
green, stop and fix it before migrating.

### 2. Add a logging characterization test (pin current behavior first)

Create `src/test/java/com/acme/myproduct/LoggingBackendProbeTest.java`. It pins the **true**
current behavior established by the Finding above — the active backend is log4j2 served from
**tika-app's shaded copy** (not Logback, not the declared slf4j-api). This is a sibling to
`BcClasspathProbeTest`, and the first brick of the wider characterization net the Boot 2.0 hop
will need.

```java
package com.acme.myproduct;

import org.junit.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LoggingBackendProbeTest {

    @Test
    public void log4j2FromTikaAppIsTheActiveSlf4jBackend() throws Exception {
        ILoggerFactory factory = LoggerFactory.getILoggerFactory();
        String factoryName = factory.getClass().getName();
        String slf4jLocation = LoggerFactory.class.getProtectionDomain()
                .getCodeSource().getLocation().toString();

        System.out.println("[LOG-PROBE] active ILoggerFactory : " + factoryName);
        System.out.println("[LOG-PROBE] org.slf4j.LoggerFactory from : " + slf4jLocation);

        // The winner is log4j2's binding, and the org.slf4j.LoggerFactory that selected it is the
        // SLF4J 2.x copy shaded inside tika-app — not the declared slf4j-api:1.7.25. The align in
        // step 3 must NOT change this: if it flips, classpath ordering moved — investigate.
        assertEquals("org.apache.logging.slf4j.Log4jLoggerFactory", factoryName);
        assertTrue("expected org.slf4j.LoggerFactory from tika-app, but was: " + slf4jLocation,
                slf4jLocation.contains("tika-app"));
    }
}
```

Run it against the **unchanged** stack to prove it is green at the baseline:

```powershell
mvn -q -Dtest=LoggingBackendProbeTest test    # expect BUILD SUCCESS
```

> Note: a first draft of this probe asserted Logback was the backend and **failed at baseline**
> — that failure is what surfaced the shaded-tika-app Finding. The methodology working as
> intended: characterize before you change.

### 3. Edit `pom.xml` — the single production change

In `<properties>` (next to the existing `<tomcat.version>` override, around line 49), add:

```xml
        <!-- Step 2: align the log4j2 trio. The Boot 1.5 parent pins log4j2.version to 2.7,
             but tika-app drags log4j-slf4j2-impl:2.20.0, leaving log4j-core/api at the
             Log4Shell-era 2.7. Lift core/api to 2.20.0 so the trio is coherent and the 2.7
             (CVE-2021-44228) jars drop out. Logback remains the live backend (the slf4j2
             binding is inert under slf4j-api 1.7) — see LoggingBackendProbeTest. -->
        <log4j2.version>2.20.0</log4j2.version>
```

**Make no other edits** to `pom.xml`.

### 4. Rebuild and run the verification gate

```powershell
mvn clean package
```

Expect **BUILD SUCCESS**, all tests passing (including the new `LoggingBackendProbeTest` and
the existing `BcClasspathProbeTest` / `JavaeeApiProbeTest`), and the fat jar repackaged.

### 5. Smoke-test the running app

```powershell
java -jar target\my-product-1.0.0.jar > app.log 2>&1   # run in a second window
```

From another shell:

```powershell
$pw   = (Select-String -Path app.log -Pattern 'security password: (.+)$').Matches.Groups[1].Value.Trim()
$cred = New-Object System.Management.Automation.PSCredential('user', (ConvertTo-SecureString $pw -AsPlainText -Force))
(Invoke-WebRequest -Uri http://localhost:8080/ -Credential $cred -UseBasicParsing).Content
# -> hello world

(Invoke-WebRequest -Uri http://localhost:8080/ -UseBasicParsing).StatusCode
# -> throws / 401 (no credentials, as expected)
```

Confirm the startup log still shows `Started HelloApplication` and no new SLF4J
`multiple bindings` / `NoClassDefFound` warnings appeared. Stop the app when done.

### 6. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree -DoutputFile=dependency-tree.step2-after.txt
Compare-Object (Get-Content dependency-tree.step2-before.txt) (Get-Content dependency-tree.step2-after.txt)
```

The **only** expected movement is `log4j-core` and `log4j-api` going `2.7 → 2.20.0`
(`log4j-slf4j2-impl` already at 2.20.0 is unchanged). Anything else — a dropped/added
framework, any other version moving — is a finding to investigate before declaring Step 2
done. **Record the diff** at the bottom of this section.

## Step 2 — done criteria (all must hold)

1. `mvn clean package` → **BUILD SUCCESS**, all tests pass.
2. `LoggingBackendProbeTest` is green: the active backend is still log4j2 served from
   tika-app's shaded copy (the align changed the tree, not the running logger).
3. `BcClasspathProbeTest` and `JavaeeApiProbeTest` stay green (no classpath winner flipped).
4. App starts and `GET /` behaves as before (401 unauthenticated, 200 `hello world`
   authenticated), with no new logging-binding warnings at startup.
5. The before/after tree diff shows **only** `log4j-core`/`log4j-api` `2.7 → 2.20.0`, and is
   pasted into this section.

## Rollback

Step 2 is one property line plus one test class. To revert: delete the `<log4j2.version>`
property (restoring the parent's inherited `2.7`), optionally delete
`LoggingBackendProbeTest.java`, then `mvn clean package`. Delete
`dependency-tree.step2-before.txt` / `dependency-tree.step2-after.txt` if not keeping them.

# modernization step 3 - widen the characterization net (bcpkix path)

## bcpkix probe

> **Goal of this section:** before committing to a "rationalize the BouncyCastle split" step,
> settle one question the existing `BcClasspathProbeTest` could not answer: *the standalone
> bcprov provider is dominated by tika-app's shaded copy — but is the rest of BouncyCastle
> (the `bcpkix` signing/cert API that iText and `spring-security-jwt` actually call) also
> shadowed, or is it live?* If it is live, a BC step is a real behaviour change, not a
> log4j2-style coherence no-op. This probe answers it by asking the JVM.

### Why the provider probe wasn't enough

`BcClasspathProbeTest` resolves exactly **one** class —
`org.bouncycastle.jce.provider.BouncyCastleProvider` — and proves it loads from
`tika-app-2.9.0.jar` (the shaded 1.76 copy). It was tempting to generalize that to "all of
BouncyCastle is dominated by tika-app, so the standalone `bcprov`/`bcpkix` jars are dead
weight we can drop like the log4j2 trio." BouncyCastle is not one jar, though: `bcprov` (the
provider + low-level crypto), `bcpkix` (cert / CMS / operator / TSP / PEM), and `bcmail`
(S/MIME) are **separate modules**, each present in several editions on this classpath
(`jdk14:1.38`/`138`, `jdk15on:1.56`/`1.57`, `jdk18on:1.76`, plus a copy shaded inside
tika-app). A one-class probe can't speak for all of them.

### The probe

`src/test/java/com/acme/myproduct/BcPkixClasspathProbeTest.java` (a sibling of the other
runtime probes) resolves one representative class per BC package and prints/asserts the jar
each actually loads from, via `Class.forName(...).getProtectionDomain().getCodeSource()`:

```
[BCPKIX-PROBE] org.bouncycastle.jce.provider.BouncyCastleProvider -> .../tika-app/2.9.0/tika-app-2.9.0.jar
[BCPKIX-PROBE] org.bouncycastle.cert.X509CertificateHolder        -> .../bcpkix-jdk15on/1.56/bcpkix-jdk15on-1.56.jar
[BCPKIX-PROBE] org.bouncycastle.cms.CMSSignedData                 -> .../bcpkix-jdk15on/1.56/bcpkix-jdk15on-1.56.jar
[BCPKIX-PROBE] org.bouncycastle.operator.ContentSigner            -> .../bcpkix-jdk15on/1.56/bcpkix-jdk15on-1.56.jar
[BCPKIX-PROBE] org.bouncycastle.tsp.TimeStampToken                -> .../bcpkix-jdk15on/1.56/bcpkix-jdk15on-1.56.jar
[BCPKIX-PROBE] org.bouncycastle.mail.smime.SMIMESigned           -> .../tika-app/2.9.0/tika-app-2.9.0.jar
[BCPKIX-PROBE] org.bouncycastle.openssl.PEMParser                 -> .../bcpkix-jdk15on/1.56/bcpkix-jdk15on-1.56.jar
[BCPKIX-PROBE] org.bouncycastle.util.encoders.Hex                 -> .../tika-app/2.9.0/tika-app-2.9.0.jar
```

### Finding: BouncyCastle is *split*, and `bcpkix-jdk15on:1.56` is live

| BC class (package) | Module | Actually loads from | Verdict |
|---|---|---|---|
| `jce.provider.BouncyCastleProvider` | bcprov | `tika-app` (shaded 1.76) | dominated by tika-app |
| `util.encoders.Hex` | bcprov/bcutil | `tika-app` (shaded 1.76) | dominated by tika-app |
| `mail.smime.SMIMESigned` | bcmail | `tika-app` (shaded 1.76) | dominated by tika-app |
| `cert.X509CertificateHolder` | bcpkix | **`bcpkix-jdk15on:1.56`** | **live (standalone)** |
| `cms.CMSSignedData` | bcpkix | **`bcpkix-jdk15on:1.56`** | **live (standalone)** |
| `operator.ContentSigner` | bcpkix | **`bcpkix-jdk15on:1.56`** | **live (standalone)** |
| `tsp.TimeStampToken` | bcpkix | **`bcpkix-jdk15on:1.56`** | **live (standalone)** |
| `openssl.PEMParser` | bcpkix | **`bcpkix-jdk15on:1.56`** | **live (standalone)** |

So the truth is neither "all tika-app" nor "all standalone". The **provider, the low-level
util, and the S/MIME** classes are dominated by tika-app's shaded copy — exactly as the
single-class provider probe suggested. But the **entire `bcpkix` module** (cert, CMS,
operator/signing, TSP, OpenSSL/PEM) loads **live from the standalone
`bcpkix-jdk15on:1.56`** jar that `spring-security-jwt` drags in (tree line 49) — tika-app's
shaded copy does **not** win for those packages. The legacy iText `jdk14` jars
(`bcprov`/`bcmail`/`bctsp` at `1.38`/`138`, tree lines 212–216) and the direct
`bcprov-jdk15on:1.57` pin are, by contrast, fully shadowed for every class probed.

### What this means for a "rationalize BouncyCastle" step

- **`bcpkix-jdk15on:1.56` is load-bearing, not dead weight.** Unlike the standalone log4j2
  trio (which the JVM never loaded, making the Step 2 align a provable no-op), this jar's
  classes *are* the live ones for all cert/signing/TSP work. A step that excludes, drops, or
  bumps it is a **real behaviour change**, not classpath hygiene — and it must be gated by
  tests that exercise the signing/cert path, not just `Class.forName`.
- **The single-class provider probe was actively misleading here.** It pointed at the one BC
  module (`bcprov`) that *is* dominated by tika-app, hiding the live `bcpkix` module behind
  it. This is the same "the tree lies, ask the JVM" lesson as the log4j2 and provider
  findings — now applied one level deeper.
- **The genuinely-dead pieces are narrower than hoped:** the iText `jdk14` jars and the
  direct `bcprov-jdk15on:1.57` pin are shadowed and *could* be candidates for removal — but
  the headline `bcpkix` cleanup cannot ride a one-line align.
- **Sequencing consequence:** this is concrete evidence for doing **"widen the
  characterization net" first** (a test that drives iText PDF signing / a JWT verify through
  `bcpkix`), and treating any BC change as a step that net *unlocks* — rather than the next
  isolated one-liner.

### How to test

```
mvn -Dtest=BcPkixClasspathProbeTest test
```

Expected: **BUILD SUCCESS**. The probe prints the eight code-source lines above and asserts
the current split (provider/util/S-MIME from tika-app; the `bcpkix` family from
`bcpkix-jdk15on:1.56`). It is a **characterization** test — kept permanently, like
`BcClasspathProbeTest`: if a later step flips any winner, this test fails and forces the
classpath change to be surfaced instead of silently absorbed.

## Step 3 — guiding principle

> **Goal of this document:** the *third* baby step. As in Steps 1–2, every action below is
> performed by a human, each step is reversible, and each is gated by a verification check.

Steps 1–2 took the platform to the top of its generation (`Boot 1.5.22 + Edgware.SR6`) and
rationalized the **one provably-dead** smell (the log4j2 trio). The next smell on the list was
the BouncyCastle split — but the **bcpkix probe above proved it is *not* dead weight**:
`bcpkix-jdk15on:1.56` is the **live** jar for every cert/CMS/signing/TSP/PEM class, so touching
it is a real behaviour change, not a log4j2-style coherence no-op.

That changes the order. The roadmap's stated prerequisite for the risky hops —
**"widen the characterization net"** — must come **before** the BouncyCastle cleanup, not after
it, because that cleanup is exactly the kind of change the net has to catch. So Step 3 is the
**first brick of that net, scoped to the one path Step 4 (BouncyCastle) will disturb**: the live
`bcpkix` signing path.

This is the lowest-risk possible step: **it adds one test class and changes no production code.**
The dependency tree is byte-for-byte unchanged — the gate is simply "the new test is green and
nothing else moved." It deliberately does **not** attempt the broader Boot-1.5→2.0 net (OAuth2,
actuator, etc.); that is its own later step. One path, one test.

## Step 3 — what changes

| Artifact | Change |
|---|---|
| `pom.xml` | **none** — zero production diff |
| `src/test/java/com/acme/myproduct/JwtBcPkixCharacterizationTest.java` | **new** characterization test |

The `bcpkix` probe pins *which jar* serves the bcpkix classes. This step adds the complementary
**behavioural** pin: it drives the live `bcpkix` path the way the app's JWT stack actually uses
it — `spring-security-jwt`'s `RsaSigner` parses a PEM private key via `spring-security-rsa`'s
`RsaKeyHelper`, which calls `org.bouncycastle.openssl.PEMParser` (the class the probe proved
loads live from `bcpkix-jdk15on:1.56`) — then signs and verifies a JWT through it. If Step 4
breaks that path, this test fails with a *behavioural* symptom (sign/verify throws), not just a
classpath-identity flip in the probe.

**Do NOT** in this step: change any `pom.xml` version, touch `java.version` or the
`javax → jakarta` namespace, or alter any pin. Keep the production diff at **exactly zero**
(one new test class only).

## Step 3 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Set the Java 8 toolchain for this shell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
```

### 1. Capture the "before" baseline (the regression oracle)

TODO: useless

```powershell
mvn -q dependency:tree -DoutputFile=dependency-tree.step3-before.txt
mvn -q clean test       # all probe + controller tests must be GREEN before adding anything
```

### 2. Add the bcpkix-path characterization test

Create `src/test/java/com/acme/myproduct/JwtBcPkixCharacterizationTest.java`:

```java
package com.acme.myproduct;

import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.junit.Test;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.jwt.crypto.sign.RsaSigner;
import org.springframework.security.jwt.crypto.sign.RsaVerifier;

import static org.junit.Assert.assertEquals;

/**
 * Step 3 characterization test. Unlike the bcpkix *probe* (which pins WHICH jar serves the
 * bcpkix classes), this pins the BEHAVIOUR of the live bcpkix signing path the way the app's
 * JWT stack actually uses it: spring-security-jwt's RsaSigner parses a PEM private key via
 * spring-security-rsa's RsaKeyHelper, which calls org.bouncycastle.openssl.PEMParser -- the
 * class the bcpkix probe proved loads live from bcpkix-jdk15on:1.56. A future "rationalize
 * BouncyCastle" step that breaks that path fails HERE, with a behavioural symptom, not just a
 * classpath-identity flip in the probe.
 */
public class JwtBcPkixCharacterizationTest {

    @Test
    public void rsaSignedJwtRoundTripsThroughLiveBcpkix() throws Exception {
        // Evidence the path we exercise really is the live bcpkix jar (see "## bcpkix probe").
        System.out.println("[JWT-BCPKIX] PEMParser from: " + PEMParser.class.getProtectionDomain()
                .getCodeSource().getLocation());

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();

        // Render the private key to PEM with BouncyCastle's openssl writer (bcpkix).
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter w = new JcaPEMWriter(sw)) {
            w.writeObject(kp.getPrivate());
        }
        String privatePem = sw.toString();

        // RsaSigner(String) -> RsaKeyHelper.parseKeyPair -> org.bouncycastle.openssl.PEMParser.
        RsaSigner signer = new RsaSigner(privatePem);
        Jwt token = JwtHelper.encode("{\"sub\":\"alice\"}", signer);

        // Verify the signature round-trips, proving the bcpkix-parsed key actually signs.
        RsaVerifier verifier = new RsaVerifier((RSAPublicKey) kp.getPublic());
        Jwt decoded = JwtHelper.decodeAndVerify(token.getEncoded(), verifier);

        assertEquals("{\"sub\":\"alice\"}", decoded.getClaims());
    }
}
```

Run it on the **unchanged** stack to prove it is green at the baseline, and confirm the printed
line shows it really drove the live bcpkix jar:

```powershell
mvn -q -Dtest=JwtBcPkixCharacterizationTest test
# expect BUILD SUCCESS, and:
# [JWT-BCPKIX] PEMParser from: .../bcpkix-jdk15on/1.56/bcpkix-jdk15on-1.56.jar
```

### 3. Rebuild and run the full verification gate

```powershell
mvn clean package
```

Expect **BUILD SUCCESS**, all tests passing (the new `JwtBcPkixCharacterizationTest` plus the
existing `BcClasspathProbeTest` / `BcPkixClasspathProbeTest` / `JavaeeApiProbeTest` /
`LoggingBackendProbeTest`), and the fat jar repackaged. No production code changed, so the app's
runtime behaviour (`GET /`: 401 unauthenticated, 200 `hello world` authenticated) is unchanged;
a smoke test is optional here.

### 4. Capture the "after" tree and diff it

TODO: useless

```powershell
mvn -q dependency:tree -DoutputFile=dependency-tree.step3-after.txt
Compare-Object (Get-Content dependency-tree.step3-before.txt) (Get-Content dependency-tree.step3-after.txt)
```

Because this step touches **no `pom.xml`**, the expected diff is **empty** — `Compare-Object`
should print nothing. Any movement at all is a finding to investigate before declaring Step 3
done. **Record the (empty) diff** at the bottom of this section.

## Step 3 — done criteria (all must hold)

1. `mvn clean package` → **BUILD SUCCESS**, all tests pass, including the new
   `JwtBcPkixCharacterizationTest`.
2. The new test's `[JWT-BCPKIX]` line confirms `PEMParser` loaded from `bcpkix-jdk15on:1.56`
   (the live jar) — i.e. the test genuinely exercised the path Step 4 will disturb.
3. `BcClasspathProbeTest`, `BcPkixClasspathProbeTest`, `JavaeeApiProbeTest`, and
   `LoggingBackendProbeTest` all stay green (no classpath winner flipped).
4. The before/after tree diff is **empty** (zero production diff), and that is pasted into this
   section.

## Rollback

Step 3 is one new test class and nothing else. To revert: delete
`JwtBcPkixCharacterizationTest.java`, then `mvn clean package`. Delete
`dependency-tree.step3-before.txt` / `dependency-tree.step3-after.txt` if not keeping them.

## Toward Step 4

With the live `bcpkix` signing path now pinned behaviourally, the **BouncyCastle
rationalization** becomes a defensible next step — gated by *this* test plus the two BC probes.
The bcpkix probe already mapped the safe-vs-risky surface:

- **Likely safe (shadowed, dead for every class probed):** the legacy iText `jdk14` jars
  (`bcprov`/`bcmail`/`bctsp` at `1.38`/`138`) and the direct `bcprov-jdk15on:1.57` pin — these
  are candidates for *exclusion/removal*, each behind its own before/after gate.
- **Risky (live, load-bearing):** anything touching `bcpkix-jdk15on:1.56`. Do not exclude or
  bump it without this characterization test green on both sides.

Two coverage gaps to close *if* Step 4 plans to touch the jars they use: the existing `toPdf`
helper exercises iText *rendering* but **not** iText PDF **signing** (also a `bcpkix` user), and
`bcmail` S/MIME is currently served by tika-app's shaded copy with no behavioural test at all.
Add those characterizations before disturbing their jars — same methodology: characterize before
you change.

After BouncyCastle, the net still needs widening for the **Boot 1.5 → 2.0 + Edgware → Finchley**
jump (OAuth2 autoconfig, the Actuator endpoint rewrite), where behaviour actually shifts — that
remains the larger prerequisite the roadmap calls out.

# modernization step 4 - BC cleanup (SCA hygiene)

> **Goal of this document:** the *fourth* baby step. As in Steps 1–3, every action below is
> performed by a human, each step is reversible, and each is gated by a verification check.

## Guiding principle

Step 3 pinned the **live** `bcpkix` signing path behaviourally (`JwtBcPkixCharacterizationTest`).
That makes a BouncyCastle change defensible — but the `bcpkix` probe also drew a hard line
between what is *dead* and what is *load-bearing*, and **this step deliberately stays on the dead
side of it**. It is **SCA hygiene, not modernization**: it removes BC jars that the JVM never
loads, shrinking the scanner surface, and changes **nothing the application runs**.

Concretely, Step 4 does exactly one thing: **exclude the ancient, fully-shadowed iText `jdk14`
BouncyCastle jars.** It explicitly does **not** touch `bcpkix-jdk15on:1.56` (the live jar) and —
per the finding below — does **not** remove the `bcprov-jdk15on:1.57` pin either. Like Step 2's
log4j2 align, the value is a coherent, smaller tree with a provably unchanged runtime.

## What is safe to remove (grounded in the `bcpkix` probe)

The probe (`## bcpkix probe`) resolved one representative class per BC module and proved where
each actually loads from. The iText `jdk14` jars **lost every contest**:

| iText-dragged jar (tree lines 212–216) | A class it could serve | Who actually wins | Verdict |
|---|---|---|---|
| `bouncycastle:bcprov-jdk14:138` / `org.bouncycastle:bcprov-jdk14:1.38` | `jce.provider.BouncyCastleProvider` | `tika-app` (shaded 1.76) | shadowed |
| `bouncycastle:bcmail-jdk14:138` / `org.bouncycastle:bcmail-jdk14:1.38` | `mail.smime.SMIMESigned` | `tika-app` (shaded 1.76) | shadowed |
| `org.bouncycastle:bctsp-jdk14:1.38` | `tsp.TimeStampToken` | `bcpkix-jdk15on:1.56` | shadowed |

Every class these jars *could* provide is already served by a winner that is **not** one of them.
So dropping them cannot flip a winner — the existing `BcClasspathProbeTest` /
`BcPkixClasspathProbeTest` re-run after the change is the proof (they pin those exact winners).
These jars are pulled **only** by iText, so a `<exclusion>` removes them outright with no
transitive comeback.

## Finding: `bcprov-jdk15on:1.57` is NOT a removal — it is a downgrade (so it's out of scope)

"Toward Step 4" listed the direct `bcprov-jdk15on:1.57` pin as a removal candidate. Checking
Maven instead of assuming shows that is wrong:

```
bcpkix-jdk15on-1.56.pom  ->  depends on  org.bouncycastle:bcprov-jdk15on:1.56
```

Today the direct pin (depth 1) wins nearest-wins over that transitive `1.56` (depth 2 via
`spring-security-jwt → bcpkix`). **Delete the pin and the transitive `1.56` resolves instead** —
the jar does not disappear, it *downgrades* `1.57 → 1.56` (verified: `bcprov-jdk15on:1.56` then
appears nested under `bcpkix` at tree line 49). That is a version change to a lower, older edition
— the opposite of hygiene — so it stays **out of this step**. Genuinely retiring that pin belongs
with the later, behaviour-bearing `bcpkix` convergence, not here.

## Finding: a behavioural bcmail S/MIME test is not viable on this classpath

"Toward Step 4" asked for a behavioural bcmail S/MIME characterization before disturbing bcmail
jars. One was attempted (`BcMailSmimeCharacterizationTest`, sign + verify a `MimeBodyPart`) and it
**failed at baseline — before any change** — with:

```
java.lang.IllegalAccessError: tried to access class org.bouncycastle.asn1.DEROutputStream
    from class org.bouncycastle.cert.CertUtils
```

This is the project's own jar-hell, one level deeper: `org.bouncycastle.cert.*` resolves to the
live `bcpkix-jdk15on:1.56`, but `org.bouncycastle.asn1.*` resolves to **tika-app's shaded 1.76**,
where `DEROutputStream` is not public. The cert/cms (1.56) and asn1 (1.76) editions **cross
incompatibly** — there is no self-consistent *standalone* bcmail S/MIME path to characterize. So
the test was removed, not faked green. The bcmail removal's gate is therefore the **identity**
probe that already exists and passes: `BcPkixClasspathProbeTest` pins
`mail.smime.SMIMESigned → tika-app`, and it stays green across the exclusion (verified). The app
never exercises S/MIME, so this is coverage we record honestly as *unavailable*, not a hole we
paper over.

## Step 4 — what changes

| Artifact | Change |
|---|---|
| `pom.xml` | add an `<exclusions>` block to the existing `com.lowagie:itext` dependency — five exclusions, no version edits |
| tests | **none** — the existing probes are the gate (see findings above) |

**Do NOT** in this step: remove or bump `bcprov-jdk15on:1.57`, exclude/bump `bcpkix-jdk15on:1.56`,
move Boot / Spring Cloud versions, touch `java.version` or the `javax → jakarta` namespace, or
alter any other pin. Keep the production diff to the single `<exclusions>` block.

## Step 4 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Set the Java 8 toolchain for this shell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
```

### 1. Capture the "before" baseline (the regression oracle)

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step4-before.txt"
mvn -q test       # all probe + controller tests must be GREEN before touching anything
```

Confirm the file was written and the run reported **BUILD SUCCESS** (11 tests:
`BcClasspathProbeTest`, `BcPkixClasspathProbeTest`, `JwtBcPkixCharacterizationTest`,
`JavaeeApiProbeTest`, `LoggingBackendProbeTest`, `HelloControllerTest`). If the baseline is not
green, stop and fix it first.

### 2. Edit `pom.xml` — exclude the iText `jdk14` BouncyCastle jars

In the `com.lowagie:itext` dependency (around line 185), add the `<exclusions>` block so it reads:

```xml
        <dependency>
            <groupId>com.lowagie</groupId>
            <artifactId>itext</artifactId>
            <version>2.1.7</version>
            <exclusions>
                <!-- Step 4: drop the ancient, fully-shadowed iText jdk14 BouncyCastle jars.
                     The bcpkix probe proved every class they could serve loads elsewhere
                     (provider/util/S-MIME from tika-app's shaded 1.76; tsp from bcpkix:1.56),
                     so these are dead weight that only inflate the SCA surface. -->
                <exclusion>
                    <groupId>bouncycastle</groupId>
                    <artifactId>bcprov-jdk14</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>bouncycastle</groupId>
                    <artifactId>bcmail-jdk14</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.bouncycastle</groupId>
                    <artifactId>bctsp-jdk14</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.bouncycastle</groupId>
                    <artifactId>bcprov-jdk14</artifactId>
                </exclusion>
                <exclusion>
                    <groupId>org.bouncycastle</groupId>
                    <artifactId>bcmail-jdk14</artifactId>
                </exclusion>
            </exclusions>
        </dependency>
```

> Both groupIds are needed: iText declares `bcmail-jdk14:138` / `bcprov-jdk14:138` under the
> legacy `bouncycastle` groupId, and `bctsp-jdk14:1.38` (which drags `bcprov-jdk14:1.38` and
> `bcmail-jdk14:1.38`) under `org.bouncycastle`. **Make no other edits** to `pom.xml`.

### 3. Rebuild and run the verification gate

```powershell
mvn clean package
```

Expect **BUILD SUCCESS**, all 11 tests passing, and the fat jar repackaged. In particular
`HelloControllerTest`'s `toPdf` test stays green — iText's basic PDF rendering does not call
BouncyCastle, so removing its `jdk14` jars does not break it (verified).

### 4. Smoke-test the running app (optional)

No runtime behaviour changes (the removed jars were never loaded), so a smoke test is optional.
If run, `GET /` behaves as before (401 unauthenticated, 200 `hello world` authenticated) and
startup shows no new `NoClassDefFoundError` / SLF4J warnings.

### 5. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step4-after.txt"
Compare-Object (Get-Content dependency-tree.step4-before.txt) (Get-Content dependency-tree.step4-after.txt)
```

The **only** expected movement is the five iText `jdk14` lines disappearing (verified):

```
< |  +- bouncycastle:bcmail-jdk14:jar:138:compile
< |  +- bouncycastle:bcprov-jdk14:jar:138:compile
< |  \- org.bouncycastle:bctsp-jdk14:jar:1.38:compile
< |     +- org.bouncycastle:bcprov-jdk14:jar:1.38:compile
< |     \- org.bouncycastle:bcmail-jdk14:jar:1.38:compile
```

`bcpkix-jdk15on:1.56` and `bcprov-jdk15on:1.57` are **unchanged**. Anything else moving — in
particular `bcprov-jdk15on` appearing at `1.56` — means the `1.57` pin was touched; investigate
before declaring Step 4 done. **Record the diff** at the bottom of this section.

## Step 4 — done criteria (all must hold)

1. `mvn clean package` → **BUILD SUCCESS**, all 11 tests pass (including `toPdf`).
2. `BcClasspathProbeTest` and `BcPkixClasspathProbeTest` stay green — every BC winner is
   identical to the baseline (provider/util/S-MIME from tika-app; `bcpkix` family from
   `bcpkix-jdk15on:1.56`). No winner flipped.
3. `JwtBcPkixCharacterizationTest`, `JavaeeApiProbeTest`, `LoggingBackendProbeTest` stay green.
4. The before/after tree diff shows **only** the five iText `jdk14` jars dropping, with
   `bcpkix-jdk15on:1.56` and `bcprov-jdk15on:1.57` untouched, and is pasted into this section.

## Rollback

Step 4 is one `<exclusions>` block. To revert: delete it from the `com.lowagie:itext` dependency,
then `mvn clean package`. Delete `dependency-tree.step4-before.txt` /
`dependency-tree.step4-after.txt` if not keeping them.

## Toward Step 5

The genuinely-dead BC jars are now gone. What remains is deliberately deferred:

- **`bcprov-jdk15on:1.57` vs `bcpkix-jdk15on:1.56`** — the shadowed-provider / live-bcpkix pair.
  Rationalizing these is a *behaviour-bearing* change (and removing the `1.57` pin downgrades
  rather than removes — see the finding above), so it rides with a real `bcpkix` convergence, not
  another hygiene one-liner.
- **The Boot 1.5 → 2.0 + Edgware → Finchley net** — still the larger prerequisite (OAuth2
  autoconfig, the Actuator endpoint rewrite), where behaviour actually shifts. That is where the
  characterization net needs its next, bigger bricks.

# The "widen the characterization net" principle

> **Goal of this section:** explain the phrase the roadmap keeps repeating, because every word in
> it is load-bearing. This is the conceptual grounding for Step 5 (and for every "characterize
> before you change" note in Steps 1–4).

## "Characterization"

A **characterization test** (Michael Feathers, *Working Effectively with Legacy Code*) does **not**
assert what the code *should* do — it asserts what the code **currently does**, including behaviour
that is surprising, ugly, or arguably wrong.

- A normal unit test encodes a *specification*: "given 2 and 3, `add` returns 5." Written from
  requirements.
- A characterization test encodes *observed reality*: "right now, `/health` with no broker returns
  **503 DOWN** — so pin that." Written by running the system and recording what falls out.

You don't write them to find bugs. You write them to **freeze current behaviour into an executable
record**, so that when you later change something underneath, any deviation lights up. The
behaviour becomes a tripwire. This is why the README keeps saying *characterize before you change*,
and why it is full of "first draft assumed X, was wrong" — the 503/DOWN finding in Step 5 is the
purest case: the wish was UP, reality was DOWN, and the honest move is to pin reality.

## "Net"

The metaphor is a **safety net** stretched under the code; each characterization test is one knot.
Collectively they catch **regressions** — behaviour that changes silently during a migration and
would otherwise reach production unnoticed. A net has **holes**: any behaviour not covered by a
test is a hole a change can fall straight through. Before Step 5 the behavioural net had three
knots (`GET /`, the leaf-library helpers, the classpath probes) and large holes directly under the
**security wall**, the **Actuator endpoints**, and the **JAXB/WS path**.

## "Widen … before the Boot 2.0 hop"

This is the crux — the *timing* is the whole point. A net is only a regression oracle if it was
**green on the known-good stack first**:

```
1. Stack at 1.5.22 (known-good).   -> run net -> GREEN. This is the reference.
2. Make the change (-> Boot 2.0).
3. Run the SAME net.               -> any RED = the migration moved something.
```

Add the test *after* the jump and the reference point is lost: you can no longer tell "correct for
2.0" from "a regression the migration introduced," because you never recorded what 1.5 did — the
test would just bless whatever 2.0 produces. So the net must be widened **before** the hop, while
standing on solid ground, over exactly the regions known to be about to move:

| Region widened now | Why it moves in Boot 2.0 |
|---|---|
| Security (401/200) | OAuth2 autoconfig is overhauled |
| Actuator (`/health` path) | endpoints relocate under `/actuator/*` |
| JAXB / WS | later: JAXB leaves the JDK, `javax` → `jakarta` |

## The subtlety unique to migration: some tests are *designed to flip*

Usually a regression test is meant to **stay green** across a change. But two Step 5 assertions are
meant to flip: `/health` at root will become a `404` in 2.0, and `/actuator/health` `404` will
become `200`. That is the net doing its job, not a contradiction. When the 2.0 hop lands and
**only** those predicted assertions go red — while the security and JAXB knots stay green — you
have *proof* the change was surgical: it moved the Actuator paths and nothing else. A red you did
**not** predict is a finding. In one sentence: *widening the net before the hop means recording how
the app behaves today — especially where the migration will disturb it — while the stack is still
known-good, so that afterwards the same tests tell you, knot by knot, exactly what moved and what
did not.*

# modernization step 5 - widen the characterization net (the prerequisite for the Boot 2.0 hop)

> **Goal of this document:** the *fifth* baby step. Unlike Steps 1–4 it touches **no production
> code and no `pom.xml`** — it adds *tests only*. As before, every action is performed by a human,
> the step is reversible (delete the test files), and it is gated by a verification check.

## Guiding principle

Every prior step ends with the same warning: **before the Boot 1.5 → 2.0 hop, widen the
characterization net first** (Step 1's "Prerequisite for Step 2", repeated in "Toward Step 5").
The Boot 1.5 → 2.0 + Edgware → Finchley jump is the first hop where *behaviour actually shifts* —
the OAuth2 autoconfig is overhauled and the Actuator endpoint infrastructure is rewritten. The
current net does not cover either: it has `GET /` (one happy path), the leaf-library helpers, and
the classpath probes — nothing that pins the **security wall**, the **Actuator contract**, or the
**JAXB/WS path**. This step adds those three bricks **while still on `1.5.22 + Edgware.SR6`**, so
they are green against the *known-good* stack and become the regression oracle the jump is graded
against. **This is pure net-widening: zero risk to the running app, because nothing it runs
changes.**

The deliberate non-goal: **do not start the jump.** No Boot/Spring Cloud version moves, no OAuth2
or Actuator code changes — only tests that *describe today's behaviour*.

## What is characterized (and why each is the oracle for a specific 2.0 change)

| New test | Pins | The 2.0 change it guards |
|---|---|---|
| `OAuth2SecurityCharacterizationTest` | `GET /` → **401** without creds, **200 `hello world`** with valid Basic creds, **401** with a wrong password | Boot 2.0 overhauls the `spring-security-oauth2` autoconfig; this is the contract that must survive |
| `ActuatorEndpointCharacterizationTest` | health is served at the **root** path `/health` (with a `status` JSON body); `/actuator/health` **404s** | Boot 2.0 moves actuator under `/actuator/*` — the two assertions are designed to *flip* at the jump, deliberately |
| `JaxbArithmeticsCharacterizationTest` | the generated `AddRequest` marshals to qualified XML (ns `http://acme.com/arithmetics`) and round-trips through the JDK's `javax.xml.bind` | later hops: Java 8 → 11 (JAXB leaves the JDK) and `javax` → `jakarta` |

All three are **JUnit 4**, matching the existing suite. Two of them (`OAuth2…`, `Actuator…`) boot
the **full application context** on a random port via `@SpringBootTest` + `TestRestTemplate` — the
first integration-level coverage in the project; the JAXB one is a plain unit test.

### Finding: deterministic credentials, not the random startup password

The README's manual smoke test reads the random password Boot prints at startup out of a log. That
is impractical in an automated test, so the two `@SpringBootTest` classes pin a **known** user via
`@TestPropertySource(properties = {"security.user.name=probe", "security.user.password=probe-pw"})`.
Verified: with `spring-security-oauth2` on the classpath, `security.user.*` still drives the default
Basic user under Boot 1.5, so `401`/`200` are deterministic without scraping a log.

### Finding: the baseline health verdict is DOWN (503), not UP — and that's fine

A first draft of the Actuator test asserted `/health` → **200 / `"status":"UP"`**. It **failed at
baseline — before any change** — returning **HTTP 503 / `"status":"DOWN"`**. Cause: in the
project's infra-free test environment the **amqp / IBM-MQ health indicators cannot reach a broker**,
so the aggregate health is DOWN. (Same methodology lesson as the Logback-vs-tika and bcmail
findings: *characterize the truth, don't assume it.*) The verdict is a property of the **missing
broker**, not of the path contract the Boot 2.0 hop changes — so the test pins the **path** (root
`/health` exists, `/actuator/health` 404s) and the **response shape** (a JSON `status` field),
accepting **200 or 503**. It does **not** couple to UP/DOWN. This keeps the oracle about the thing
the migration actually moves.

## Step 5 — what changes

| Artifact | Change |
|---|---|
| `pom.xml` | **none** |
| `src/main/**` | **none** |
| `src/test/java/com/acme/myproduct/OAuth2SecurityCharacterizationTest.java` | **new** (3 tests) |
| `src/test/java/com/acme/myproduct/ActuatorEndpointCharacterizationTest.java` | **new** (2 tests) |
| `src/test/java/com/acme/myproduct/JaxbArithmeticsCharacterizationTest.java` | **new** (2 tests) |

**Do NOT** in this step: move Boot / Spring Cloud versions, touch `java.version` or the
`javax → jakarta` namespace, alter any pin, or change a single line of production code. Tests only.

## Step 5 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Set the Java 8 toolchain for this shell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
```

### 1. Confirm the baseline is green (the 11 existing tests)

```powershell
mvn -q test       # expect BUILD SUCCESS, 11 tests
```

If the baseline is not green, stop and fix it before adding the net.

### 2. Add the three characterization tests

Create the three files below verbatim under `src/test/java/com/acme/myproduct/`.

**`OAuth2SecurityCharacterizationTest.java`** — the security wall:

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "security.user.name=probe",
        "security.user.password=probe-pw"
})
public class OAuth2SecurityCharacterizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void rootWithoutCredentialsIs401() {
        ResponseEntity<String> response = restTemplate.getForEntity("/", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    public void rootWithValidCredentialsIs200AndHelloWorld() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                .getForEntity("/", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("hello world", response.getBody());
    }

    @Test
    public void rootWithWrongPasswordIs401() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "wrong")
                .getForEntity("/", String.class);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }
}
```

**`ActuatorEndpointCharacterizationTest.java`** — the endpoint contract (root `/health`,
`/actuator/health` 404, status shape not verdict — see the DOWN finding above):

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "security.user.name=probe",
        "security.user.password=probe-pw"
})
public class ActuatorEndpointCharacterizationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void healthIsServedAtRootPathWithStatusBody() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                .getForEntity("/health", String.class);

        // The endpoint exists at root: it responds (not 404). 200 (UP) or 503 (DOWN) both prove
        // the path is served; here it is 503 because no broker is reachable.
        assertTrue(response.getStatusCode() == HttpStatus.OK
                || response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE);
        assertTrue(response.getBody().contains("\"status\":"));
    }

    @Test
    public void actuatorPrefixedHealthDoesNotExistYet() {
        ResponseEntity<String> response = restTemplate
                .withBasicAuth("probe", "probe-pw")
                .getForEntity("/actuator/health", String.class);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
```

**`JaxbArithmeticsCharacterizationTest.java`** — the WS/JAXB path:

```java
public class JaxbArithmeticsCharacterizationTest {

    private static final String NS = "http://acme.com/arithmetics";

    @Test
    public void addRequestMarshalsToQualifiedXml() throws Exception {
        AddRequest request = new AddRequest();
        request.setAugend(2);
        request.setAddend(3);

        JAXBContext context = JAXBContext.newInstance(AddRequest.class);
        Marshaller marshaller = context.createMarshaller();
        StringWriter writer = new StringWriter();
        marshaller.marshal(request, writer);
        String xml = writer.toString();

        assertTrue(xml.contains(NS));
        assertTrue(xml.contains(">2</"));
        assertTrue(xml.contains(">3</"));
    }

    @Test
    public void addRequestRoundTripsThroughJaxb() throws Exception {
        AddRequest original = new AddRequest();
        original.setAugend(7);
        original.setAddend(5);

        JAXBContext context = JAXBContext.newInstance(AddRequest.class);
        StringWriter writer = new StringWriter();
        context.createMarshaller().marshal(original, writer);

        Unmarshaller unmarshaller = context.createUnmarshaller();
        AddRequest roundTripped =
                (AddRequest) unmarshaller.unmarshal(new StringReader(writer.toString()));

        assertEquals(7, roundTripped.getAugend());
        assertEquals(5, roundTripped.getAddend());
    }
}
```

> The `JaxbArithmeticsCharacterizationTest` imports `com.acme.myproduct.ws.arithmetics.AddRequest`,
> which the `maven-jaxb2-plugin` generates from `arithmetics.wsdl` during `generate-sources`. A bare
> `mvn test` regenerates it first, so the import resolves. (Full imports for all three files are in
> the committed sources; they are the obvious `org.junit`, `org.springframework.*`, and
> `javax.xml.bind.*` ones.)

### 3. Run the widened suite (the verification gate)

```powershell
mvn -q test
```

Expect **BUILD SUCCESS** and **18 tests** (the 11 existing + 7 new). The full context boots twice
(once per `@SpringBootTest` class), so this run is noticeably slower than the unit-only baseline —
that is expected.

### 4. Record the result

The verified run on `1.5.22 + Edgware.SR6`:

```
Tests run: 3, ... - in com.acme.myproduct.OAuth2SecurityCharacterizationTest
Tests run: 2, ... - in com.acme.myproduct.ActuatorEndpointCharacterizationTest
Tests run: 2, ... - in com.acme.myproduct.JaxbArithmeticsCharacterizationTest
Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
```

## Step 5 — done criteria (all must hold)

1. `mvn test` → **BUILD SUCCESS**, **18 tests** pass (11 prior + 7 new).
2. `OAuth2SecurityCharacterizationTest` green: `401` unauthenticated, `200 hello world`
   authenticated, `401` on wrong password — the security wall is now pinned.
3. `ActuatorEndpointCharacterizationTest` green: `/health` served at root with a `status` body,
   `/actuator/health` → `404` — the 1.5 endpoint contract is now pinned (and *will* flip at the
   2.0 jump, by design).
4. `JaxbArithmeticsCharacterizationTest` green: marshal + round-trip through the JDK `javax.xml.bind`.
5. **No production diff.** `git status` shows only new files under `src/test/java`; `pom.xml`,
   `src/main/**`, and every pin are untouched.

## Rollback

Step 5 is three test files and nothing else. To revert: delete
`OAuth2SecurityCharacterizationTest.java`, `ActuatorEndpointCharacterizationTest.java`, and
`JaxbArithmeticsCharacterizationTest.java`, then `mvn test` returns to the 11-test baseline.

## Toward Step 6 — the jump, decomposed

The net is now wide enough to *grade* the Boot 1.5 → 2.0 + Edgware → Finchley hop. That hop should
**not** be one step: with the oracle in place, decompose it into separately-gated hops — the
Spring Cloud train/BOM (Edgware → Finchley, which forces retiring the hand-pinned
`spring-cloud-netflix-core:1.4.4`), the **Actuator** endpoint move (tests 3 above flip,
deliberately), the **OAuth2** autoconfig change (test 2 above is the guard, and the EOL standalone
module is a re-architecture, not a version bump), and the **property-key** migration. **OpenRewrite**
(`org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_0` + the Spring property recipes) is best
used here as a **dry-run advisor on a scratch branch** — to enumerate the mechanical renames — then
fold its diff into these hand-gated hops, rather than as a big-bang applier. Note its Spring Cloud
*Finchley*-era coverage is thin and its plugin needs a JDK 17 to run, so treat its Spring Cloud
output as a draft, not an answer.

# modernization step 6 - OpenRewrite as a dry-run advisor (inventory the Boot 2.0 hop)

> **Goal of this document:** the *sixth* baby step. Like Steps 3 and 5 it touches **no production
> code and no committed `pom.xml`** — it runs a tool that *reports* what the Boot 1.5 → 2.0 hop will
> require, without performing it. As before, every action is performed by a human, the step is fully
> reversible (it writes only a throwaway patch under `target/`), and it is gated by a verification
> check.

## Guiding principle

Step 5 finished widening the characterization net, so the Boot 1.5 → 2.0 + Edgware → Finchley jump
can finally be *graded*. "Toward Step 6" already ruled that the jump must **not** be one step: it
decomposes into separately-gated hops — the **Spring Cloud train/BOM** (Edgware → Finchley, which
forces retiring the hand-pinned `spring-cloud-netflix-core:1.4.4`), the **Actuator** endpoint move
(Step 5's test 3 flips, by design), the **OAuth2** autoconfig change (Step 5's test 2 is the guard),
and the **property-key** migration.

Before decomposing, we need the *map*: the full set of mechanical renames the jump entails. Producing
that map is exactly what OpenRewrite is good at — and **only** that, in this step. The cardinal rule
from "Toward Step 6" holds: OpenRewrite is a **dry-run advisor**, not a big-bang applier. So Step 6 is
the lowest-risk possible use of the tool: **run `rewrite:dryRun`, read the patch it emits, change
nothing.** No source file is rewritten, no pom is committed, the dependency tree is byte-for-byte
unchanged. The gate is simply "the advisor ran, produced a patch, and the baseline is still green."

This mirrors the "characterize before you change" discipline applied to *tooling*: before we let
OpenRewrite touch a single line (a later hop), we first confirm what it *would* do, while standing on
the known-good `1.5.22 + Edgware.SR6` stack.

## The JDK tension (read before running)

OpenRewrite's Maven plugin and the `rewrite-spring` recipes are compiled for **JDK 17+** — they will
not run under the project's Java 8 toolchain. But the project itself is Java 8 (`<java.version>1.8</java.version>`)
and must keep building on Java 8. So Step 6 deliberately uses **two toolchains**:

- **JDK 17** — only to *run the OpenRewrite plugin* (the advisor invocation).
- **JDK 8** — for the verification gate (`mvn clean test`), exactly as Steps 1–5.

This split is itself a finding worth surfacing: the modernization tooling already outruns the
codebase's runtime. Running the advisor under 17 against a Java-8 project is normally fine for
`dryRun` (it parses sources at their declared `1.8` source level), but two frictions are possible and
must be watched (see step 3 below):

1. `dryRun` **forks a Maven build** (up to `process-test-classes`) to attribute types. Under JDK 17,
   the ancient `maven-jaxb2-plugin:0.13.2` `generate-sources` step or the Java-8-targeted compile may
   misbehave. The fallback is the **`dryRunNoFork`** goal, which skips the forked lifecycle (at the
   cost of weaker type attribution — note that in the record if you use it).
2. OpenRewrite's Spring Cloud *Finchley*-era coverage is thin (per "Toward Step 6"). Treat any Spring
   Cloud lines in the patch as a **draft to verify by hand**, not an answer.

## Step 6 — what changes

| Artifact | Change |
|---|---|
| `pom.xml` | **none** — the plugin is invoked from the command line with pinned coordinates; nothing is committed |
| `src/main/**`, `src/test/**` | **none** — `dryRun` writes no source edits |
| `target/rewrite/rewrite.patch` | **new** (throwaway advisor output, under `target/`, gitignored by the build) |

**Do NOT** in this step: run `rewrite:run` (the applier), move Boot / Spring Cloud versions, edit
`pom.xml`, touch `java.version` or the `javax → jakarta` namespace, alter any pin, or change a single
line of production or test code. The production diff is **exactly zero**; the only artifact produced is
a patch file we *read*, not apply.

> **Why command-line, not a committed `<plugin>` block:** keeping the plugin out of `pom.xml`
> preserves the zero-production-diff purity Steps 3 and 5 prize, and avoids a JDK-17-only plugin
> sitting in a Java-8 build that CI runs under JDK 8. An optional scratch `<plugin>` config is shown
> at the end for reproducibility — **do not commit it.**

## Step 6 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Work on a throwaway branch

`dryRun` writes no source edits, but do this on a scratch branch so any accidental experimentation
(e.g. trying `rewrite:run`) is trivially discarded:

```powershell
git switch -c step6/openrewrite-advisor
```

### 1. Confirm the baseline is green (under JDK 8)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482

mvn -q test      # expect BUILD SUCCESS, 18 tests (the Step 5 net)
```

If the baseline is not green, stop and fix it before running the advisor — the advisor is only
meaningful against the known-good stack.

### 2. Switch this shell to JDK 17 (for the OpenRewrite plugin only)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'   # adjust to your JDK 17 install path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.x
```

> The `<java.version>1.8</java.version>` in `pom.xml` is **unchanged** — this only changes which JDK
> *runs Maven* for the advisor invocation. The committed build stays Java 8.

### 3. Identify the current OpenRewrite version pair (do not assume)

The plugin (`rewrite-maven-plugin`) and the recipe module (`rewrite-spring`) **version
independently** — their numbers do not match each other, and neither matches the recipe BOM.
What they must share is the same OpenRewrite **core** (`org.openrewrite:rewrite-core`) major
line; a mismatched pair fails with a core-version / `NoSuchMethod`-style resolution error. They
also ship new releases every few weeks, so **resolve the current pair at run time rather than
trusting the numbers printed below** (captured 2026-06; the major lines, not the exact patches,
are what matter). Three ways, cheapest first:

**a. Latest of each (quick).** Read `<release>` from each artifact's Maven Central metadata:

```powershell
(Invoke-WebRequest "https://repo1.maven.org/maven2/org/openrewrite/maven/rewrite-maven-plugin/maven-metadata.xml" -UseBasicParsing).Content
(Invoke-WebRequest "https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-spring/maven-metadata.xml" -UseBasicParsing).Content
# read the <release> element from each   ->  e.g. 6.42.0  and  6.33.0
```

Because OpenRewrite ships the plugin and the recipe modules in near-lockstep, "latest plugin +
latest `rewrite-spring`" is almost always a compatible pair.

**b. The `rewrite-recipe-bom` (authoritative).** The BOM exists to pin a *mutually-tested* set of
every recipe module. Take the latest BOM, then read which `rewrite-spring` version it declares —
that is the guaranteed-compatible recipe version to pair with the latest plugin:

```powershell
(Invoke-WebRequest "https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/maven-metadata.xml" -UseBasicParsing).Content
# take <release> (e.g. 3.33.0), then read that BOM's pom and find the rewrite-spring <version>:
(Invoke-WebRequest "https://repo1.maven.org/maven2/org/openrewrite/recipe/rewrite-recipe-bom/3.33.0/rewrite-recipe-bom-3.33.0.pom" -UseBasicParsing).Content
# -> rewrite-spring 6.33.0   (the version this README pins below)
```

**c. `rewrite:discover` (validates the pair resolves).** The real proof the chosen pair resolves
*and* the recipe id exists — run it before `dryRun`:

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:discover `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0"
```

If the pair is mismatched, this fails here (resolution / core-version error) before you waste a
`dryRun`. A clean run lists the recipes the artifact provides — confirm
`org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_0` is among them.

### 4. Run the advisor (`dryRun`) with the pinned pair

Invoke the recipe entirely from the command line so `pom.xml` stays untouched. Pin the plugin and
recipe artifact to the explicit versions resolved in step 3 (do **not** use `LATEST`/`RELEASE` for
a reproducible record):

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:dryRun `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_0"
```

If the forked build fails under JDK 17 (the jaxb2 / compile friction noted above), fall back to the
non-forking goal and record that you did (type attribution is weaker, so the Java-source section of
the patch is less complete):

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:dryRunNoFork `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_0"
```

Expect **BUILD SUCCESS** with a log line pointing at the emitted patch, e.g.:

```
Patch written to target/rewrite/rewrite.patch
Please review and apply the patch with: git apply target/rewrite/rewrite.patch
```

### 5. Read the patch — turn it into the hop decomposition (the real deliverable)

Open `target/rewrite/rewrite.patch` and **classify every hunk** into the four hand-gated hops "Toward
Step 6" named. This triage *is* the output of Step 6:

| Patch hunk touches… | Belongs to hop | Guarded by (from the net) |
|---|---|---|
| `pom.xml` parent `1.5.x → 2.0.x`, Spring Cloud BOM, the `spring-cloud-netflix-core:1.4.4` pin | **Spring Cloud train/BOM** hop | tree diff + all probes |
| `application.properties` / `application.yml` key renames (`server.*`, `management.*`, `security.*`) | **property-key** hop | `OAuth2…` + `Actuator…` tests |
| Actuator endpoint code / config (paths under `/actuator/*`) | **Actuator** hop | `ActuatorEndpointCharacterizationTest` (designed to flip) |
| `spring-security-oauth2` autoconfig / security config classes | **OAuth2** hop | `OAuth2SecurityCharacterizationTest` |
| Anything under `spring-cloud-*` beyond the BOM bump | **verify by hand** (thin Finchley coverage) | — |

For each hunk, the question is **not** "does OpenRewrite want this?" but "**which gated hop does this
belong to, and which characterization test grades it?**" Hunks OpenRewrite cannot express (the EOL
`spring-security-oauth2` re-architecture, the `netflix-core:1.4.4` pin retirement) are findings the
patch will *not* contain — record those gaps explicitly; they are the parts the advisor can't help
with.

### 6. Verify the advisor changed nothing (the gate)

`dryRun` must not have edited any tracked file. Confirm:

```powershell
git status --porcelain         # expect: empty (only the untracked target/ patch, which is gitignored)
git diff --stat                # expect: empty
```

Then switch back to **JDK 8** and re-run the gate to prove the stack is still the known-good baseline:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mvn -q clean test              # expect BUILD SUCCESS, 18 tests
```

### 7. Record the inventory

Paste, at the bottom of this section: the exact plugin/recipe versions used, whether `dryRun` or
`dryRunNoFork` was needed (and why), and the **classified patch summary** from step 5 (the four-hop
triage table, filled in). That triage table is what Steps 7+ consume.

> _Record below:

Ran `mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:dryRun "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_0"` under jdk17.

Got `step6.rewrite.patch`.

```
$ git apply --stat step6.rewrite.patch
 pom.xml |   13 +------------
 1 file changed, 1 insertion(+), 12 deletions(-)
```

## Step 6 — done criteria (all must hold)

1. `rewrite:dryRun` (or `dryRunNoFork`) → **BUILD SUCCESS** under JDK 17, emitting
   `target/rewrite/rewrite.patch`.
2. `git status` shows **no tracked changes** — zero production/test/pom diff; the patch is the only
   (untracked, throwaway) artifact.
3. Under JDK 8, `mvn clean test` is still **BUILD SUCCESS, 18 tests** — the advisor did not perturb
   the known-good stack.
4. The patch is **classified into the four hand-gated hops** (Spring Cloud BOM, property keys,
   Actuator, OAuth2), with the Spring Cloud lines flagged "verify by hand" and the advisor's *gaps*
   (OAuth2 re-architecture, the `netflix-core:1.4.4` pin) recorded as findings. The filled-in triage
   is pasted into this section.

## Rollback

Step 6 produces no committed change. To revert: delete the patch and the scratch branch.

```powershell
Remove-Item -Recurse -Force target/rewrite
git switch main
git branch -D step6/openrewrite-advisor
```

## Appendix — optional scratch `<plugin>` block (do NOT commit)

If you prefer running goals as `mvn rewrite:dryRun` instead of the fully-qualified coordinates, add
this to `pom.xml`'s `<build><plugins>` **on the scratch branch only**, and discard it before merging
(it pins a JDK-17-only plugin into a Java-8 build):

```xml
<plugin>
    <groupId>org.openrewrite.maven</groupId>
    <artifactId>rewrite-maven-plugin</artifactId>
    <version>6.42.0</version>
    <configuration>
        <activeRecipes>
            <recipe>org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_0</recipe>
        </activeRecipes>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>org.openrewrite.recipe</groupId>
            <artifactId>rewrite-spring</artifactId>
            <version>6.33.0</version>
        </dependency>
    </dependencies>
</plugin>
```

## Toward Step 7

With the jump now *inventoried and classified*, the obvious move looks like "take the first decomposed
hop — the Spring Cloud **Edgware → Finchley** train move." But Step 1 already established the hard
constraint: Edgware ↔ Boot 1.5 and Finchley ↔ Boot 2.0 are **version-locked** — you cannot move the
train without moving Boot. So the train move is *not* an isolable baby step; it is the coupled Boot 2.0
jump itself, and it belongs to **Step 8**, where it consumes the pom/property slices of the OpenRewrite
inventory and the Actuator tests flip by design.

What *is* isolable now — and what the jump would otherwise have to carry as extra baggage — is the
hand-pin cleanup Step 1 explicitly deferred: the explicit `spring-cloud-netflix-*` version overrides
that opt out of the Edgware BOM. Retiring them is a genuine, current-generation baby step that shrinks
the eventual jump's surface. So **Step 7 retires the Netflix hand-pins** (still Boot 1.5.22 +
Edgware.SR6, no train move), and Step 8 is the coupled Boot 2.0 + Finchley jump graded by the Step 5
net. The Actuator, OAuth2, and property-key slices of the inventory are folded into Step 8 and its
sub-hops, each separately gated.

# modernization step 7 - retire the Spring Cloud Netflix hand-pins (close the 1.4.x skew)

> **Goal of this document:** the *seventh* baby step. Like Steps 1, 2 and 4 it is a **tiny `pom.xml`
> diff** on the current generation — here, *deleting* three explicit `<version>` overrides so a family
> of dependencies falls back to the BOM. As always, every action is performed by a human, the step is
> reversible, and it is gated by a verification check.

## Guiding principle

Step 1 introduced the "**same stack, not same tree**" skew and deliberately left it alone: the POM
hand-pins three `spring-cloud-netflix-*` artifacts to versions that **opt out** of the Edgware BOM,
while the rest of the Netflix family rides the BOM. Step 1 called rationalizing those hand-pins "a
later hop … independent levers." This is that hop.

Why now, and why *before* the Boot 2.0 jump (Step 8)? Because the jump will **force** the issue —
Finchley's Netflix line is a different major (2.x), where a hand-pin to `1.4.4` is meaningless and
would conflict. Retiring the pins **now**, on the known-good `1.5.22 + Edgware.SR6` stack, removes
three moving parts from the jump so that when Step 8 lands, "why did `netflix-core` move?" is already
answered. It is the same decomposition discipline as every prior step: **bank the isolated,
low-risk cleanup on solid ground first.** And unlike the jump, this is a patch-level move *within* the
`1.4.x` line on the same train — fully covered by the Step 5 net and the probes.

## The smell (grounded in `pom.xml` lines 131–146)

Three Netflix coordinates carry explicit `<version>` overrides that beat the Edgware.SR6 BOM:

| Coordinate | Pinned at | Edgware.SR6 BOM manages it to |
|---|---|---|
| `spring-cloud-starter-openfeign` | `1.4.6.RELEASE` | **`1.4.7.RELEASE`** |
| `spring-cloud-starter-netflix-hystrix` | `1.4.4.RELEASE` | **`1.4.7.RELEASE`** |
| `spring-cloud-netflix-core` | `1.4.4.RELEASE` | **`1.4.7.RELEASE`** |

(`spring-cloud-netflix.version` in the Edgware.SR6 BOM is `1.4.7.RELEASE` — confirmed from the train's
`spring-cloud-dependencies-Edgware.SR6.pom`.) The BOM-managed Netflix transitives (ribbon, archaius,
feign-core, hystrix-core, …) already resolve to **1.4.7** under SR6. So the live tree today is *not*
the "1.4.4 ↔ 1.4.5" the POM comment still claims (that comment is **stale** — it describes the
pre-Step-1 SR4 numbers and was not updated when Step 1 bumped the train to SR6). The real, current
skew is **the three hand-pins (`1.4.4`/`1.4.6`) sitting below the `1.4.7` the rest of the family runs
at.** Deleting the three `<version>` lines lets all three fall back to the BOM, converging the entire
Netflix family on a single coherent **1.4.7.RELEASE**.

## Step 7 — what changes

| Artifact | Change |
|---|---|
| `pom.xml` | delete the `<version>` element from the three `spring-cloud-netflix-*` dependencies (lines 135, 140, 145), and correct the now-stale Edgware comment (lines 33–42) to describe the BOM-governed state |
| tests | **none** — the Step 5 net + the probes are the gate |

**Do NOT** in this step: move Boot / Spring Cloud train versions (no Edgware → Finchley, no Boot 2.0),
touch `java.version` or the `javax → jakarta` namespace, alter any non-Netflix pin, or apply any slice
of the OpenRewrite patch. Keep the production diff to the three deleted `<version>` lines (plus the
comment correction).

## Step 7 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Set the Java 8 toolchain for this shell

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
```

### 1. Capture the "before" baseline (the regression oracle)

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step7-before.txt"
mvn -q test       # the full Step 5 net + probes must be GREEN before touching anything (18 tests)
```

Confirm the file was written and the run reported **BUILD SUCCESS, 18 tests**. If the baseline is not
green, stop and fix it first.

### 2. Edit `pom.xml` — delete the three Netflix `<version>` overrides

In the "Spring Cloud (Netflix / OpenFeign / Kubernetes)" block (around line 131), remove **only** the
`<version>` line from each of the three Netflix dependencies so they inherit the Edgware.SR6 BOM:

```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>                                              <!-- was 1.4.6.RELEASE -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-hystrix</artifactId>
        </dependency>                                              <!-- was 1.4.4.RELEASE -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-netflix-core</artifactId>
        </dependency>                                              <!-- was 1.4.4.RELEASE -->
```

Leave `io.fabric8:spring-cloud-kubernetes-discovery:0.1.6` untouched (it is not BOM-managed). Then
correct the stale `spring-cloud.version` comment (lines 33–42) so it no longer claims a `1.4.4 ↔ 1.4.5`
skew — under SR6 the Netflix family now resolves uniformly to `1.4.7.RELEASE`. **Make no other edits.**

### 3. Rebuild and run the verification gate

```powershell
mvn clean package
```

Expect **BUILD SUCCESS**, all 18 tests passing, and the fat jar repackaged. The patch-level Netflix
bump (`1.4.4`/`1.4.6 → 1.4.7`) is within the same minor line on the same train, so behaviour is
expected to be unchanged — the net proves it.

### 4. Smoke-test the running app (optional)

The Netflix libs (Hystrix/Ribbon/Feign) are not on the `GET /` happy path, so a smoke test is
optional. If run, `GET /` behaves as before (401 unauthenticated, 200 `hello world` authenticated)
and startup shows no new `NoClassDefFoundError` / binding warnings.

### 5. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step7-after.txt"
Compare-Object (Get-Content dependency-tree.step7-before.txt) (Get-Content dependency-tree.step7-after.txt)
```

The **only** expected movement is the three hand-pinned coordinates rising to the BOM-managed
`1.4.7.RELEASE`:

```
spring-cloud-starter-openfeign        1.4.6.RELEASE -> 1.4.7.RELEASE
spring-cloud-starter-netflix-hystrix  1.4.4.RELEASE -> 1.4.7.RELEASE
spring-cloud-netflix-core             1.4.4.RELEASE -> 1.4.7.RELEASE
```

plus any of *their* transitives that the bump pulls to 1.4.7. Anything else — a dropped/added
framework, a non-Netflix version moving, Boot or `spring-cloud-commons` shifting — is a finding to
investigate before declaring Step 7 done. **Record the diff** at the bottom of this section.

## Step 7 — done criteria (all must hold)

1. `mvn clean package` → **BUILD SUCCESS**, all 18 tests pass.
2. `BcClasspathProbeTest`, `BcPkixClasspathProbeTest`, `JavaeeApiProbeTest`,
   `LoggingBackendProbeTest`, and `JwtBcPkixCharacterizationTest` stay green — no classpath winner
   flipped.
3. `OAuth2SecurityCharacterizationTest`, `ActuatorEndpointCharacterizationTest`, and
   `JaxbArithmeticsCharacterizationTest` stay green — the security wall, the (still-1.5) Actuator
   contract, and the JAXB path are unchanged (nothing here should make the Actuator tests flip; that
   is Step 8's job).
4. The before/after tree diff shows **only** the three Netflix coordinates converging on
   `1.4.7.RELEASE` (and their transitives), with Boot, `spring-cloud-commons`/`-context`, and every
   non-Netflix pin untouched, and is pasted into this section.

## Rollback

Step 7 is three deleted `<version>` lines (plus a comment fix). To revert: restore
`1.4.6.RELEASE` on `spring-cloud-starter-openfeign` and `1.4.4.RELEASE` on
`spring-cloud-starter-netflix-hystrix` and `spring-cloud-netflix-core`, then `mvn clean package`.
Delete `dependency-tree.step7-before.txt` / `dependency-tree.step7-after.txt` if not keeping them.

## Toward Step 8 — the coupled jump

The hand-pins are gone; the Netflix family is BOM-governed and coherent. The platform is now as clean
as it can be *without leaving its generation*. **Step 8 is the jump itself** — Boot 1.5.22 → 2.0 +
Edgware.SR6 → Finchley, version-locked and taken together. It is graded by the full Step 5 net, with
the two Actuator assertions designed to *flip* (root `/health` → `/actuator/health`) while the security
and JAXB knots must stay green. Step 8 folds in the **pom/dependency-management slice** of the Step 6
OpenRewrite inventory (parent + BOM coordinates) and the **property-key slice** (using Boot 2.0's
`spring-boot-properties-migrator` as a transitional safety net); the **OAuth2 autoconfig** change and
the EOL `spring-security-oauth2` re-architecture remain their own separately-gated sub-hops, per the
Step 6 triage. As always: characterize before you change, one gated hop at a time.

# modernization step 8 - the coupled Boot 2.0 + Finchley jump (OpenRewrite as the applier)

> **Goal of this document:** the *eighth* baby step — the first that **moves a major generation**.
> Every action below is performed by a human, the step is reversible (it runs on a throwaway branch),
> and it is gated by the full Step 5 characterization net. Unlike Step 6 (which ran OpenRewrite as a
> read-only `dryRun` *advisor*), Step 8 promotes the **same recipe to the `rewrite:run` *applier***:
> the tool that inventoried the jump now performs it, under the net that grades it.

## Guiding principle

Steps 1–7 took the platform to the cleanest it can be **without leaving its generation**
(`Boot 1.5.22 + Edgware.SR6`, Netflix family BOM-coherent, the dead BC/log4j2 jars gone, the
characterization net green). Every prior step was deliberately *intra-generation*. Step 8 is the one
that crosses the line.

It cannot be made smaller. Step 1 established the hard constraint and Step 6/7 restated it: **Edgware ↔
Boot 1.5 and Finchley ↔ Boot 2.0 are version-locked** — you cannot move the Spring Cloud train without
moving Boot, and vice-versa. So "bump Boot" and "bump the train" are not two baby steps; they are one
atomic jump. What *made* it a baby step is everything that was banked first: the net that grades it
(Step 5), the inventory that maps it (Step 6), and the hand-pin cleanup that shrank it (Step 7).

OpenRewrite changes role here. In Step 6 it was a **dry-run advisor** — `rewrite:dryRun`, read the
patch, change nothing. In Step 8 it is the **applier** — `rewrite:run` with the *same*
`UpgradeSpringBoot_2_0` recipe — so the mechanical pom/dependency-management edits are produced by the
tool, not typed by hand. That is the honest use of the inventory: we already read exactly what the
recipe will do (the Step 6 patch is in `step6.rewrite.patch`), so applying it is a reviewed change, not
a blind one. The pieces OpenRewrite *cannot* express are hand-applied on top and called out as findings:
the Spring Cloud Finchley train move (thin recipe coverage), the test characterization updates, and — the
part the draft underestimated — a set of **generation-locked transitive blockers** the recipe knows
nothing about (a log4j2 bridge collision, Spring Boot Admin, commons-io). The recipe upgrades the
framework; it does not clean up the fallout the new framework exposes.

Boot 2.0 still targets **Java 8**, so `<java.version>1.8</java.version>` is **unchanged** and the
committed build keeps running on the Java 8 toolchain. The Java 11/17 hops are later steps — this jump
does **not** touch the JDK level or the `javax → jakarta` namespace.

## The JDK tension (read before running) — same split as Step 6

OpenRewrite's plugin + `rewrite-spring` recipes are compiled for **JDK 17+**; the project is Java 8.
So, exactly as in Step 6, two toolchains are used:

- **JDK 17** — only to *run the OpenRewrite applier* (`rewrite:run`).
- **JDK 8** — for the verification gate (`mvn clean package`), and for the committed build forever after.

`rewrite:run` forks a Maven build to attribute types (the `maven-jaxb2-plugin:0.13.2` / Java-8 compile
friction noted in Step 6 applies). If the fork fails under JDK 17, fall back to `runNoFork` and record
it — but note that for *this* recipe the substantive edits are in `pom.xml` (see `step6.rewrite.patch`),
which `runNoFork` still produces.

## What Step 8 actually changes — three layers, only the first is OpenRewrite's

The Step 6 patch (`step6.rewrite.patch`) shows the recipe's reach: it bumps the parent
`1.5.22 → 2.0.9.RELEASE` and **removes redundant `<version>` pins** that the Boot 2.0 BOM now manages
(`spring-ws-core`, `spring-rabbit`, `amqp-client`, `slf4j-api`, `commons-lang3`, `httpclient`,
`tomcat-embed-core`, `h2`; the three Netflix pins in that patch are already gone as of Step 7). That is
**layer 1** — and the *whole* of what OpenRewrite contributes. Running the step proved the jump needs
**three more** layers the recipe does not cover — including a whole class of generation-locked transitive
breakage (layer 4) the original draft did not anticipate:

| Layer | Change | Applied by | Guarded by |
|---|---|---|---|
| 1. Boot parent + BOM-managed pins | parent `1.5.22 → 2.0.9.RELEASE`; drop the now-redundant `<version>` overrides | **OpenRewrite `rewrite:run`** | tree diff + all probes |
| 2. Spring Cloud train | `<spring-cloud.version>` `Edgware.SR6 → Finchley.SR4` (version-locked to Boot 2.0) | **by hand** (thin recipe coverage — Step 6 finding) | tree diff + all probes |
| 3. Test characterization updates | deterministic-credentials key rename; Actuator flip to `/actuator/*`; the `jsr311 → javaee` and `401 → 302` flips | **by hand** (test files only) | the Step 5 net itself |
| 4. Generation-locked transitive fixes | exclude `log4j-to-slf4j`; bump `spring-boot-admin` `1.5.7 → 2.0.6`; pin `commons-io 2.13.0` (see findings) | **by hand** (`pom.xml`) | the tests that were crashing — green after each |

Layer 4 is the headline lesson: **the recipe upgrades the framework, but it does not fix the third-party
and shaded-jar fallout the new framework generation exposes.** Each of those three was a hard failure
(context wouldn't start, or a test threw) — not a characterization flip — and none was in the Step 6
inventory. The probes and the Step 5 net were what caught them.

**Do NOT** in this step: change `<java.version>` (stays `1.8`); touch the `javax → jakarta` namespace;
re-architect off the EOL `spring-security-oauth2` module (that is its own later sub-hop); or bump beyond
`2.0.x` / `Finchley` (walking 2.1 → 2.7 is later hops).

## Findings (recorded after running Step 8)

The original draft listed *predicted* frictions. Below is what **actually** happened — the methodology's
"first draft assumed X, reality was Y" discipline applied to this step itself. Several predictions were
wrong; the real blockers were nastier and were not in the Step 6 inventory.

### Finding 0 — run the gate on Java 8, or you chase a phantom

The first `mvn clean test` after the jump failed with
`InaccessibleObjectException: ... module java.base does not "opens java.lang" to unnamed module` (a
CGLIB/Spring-proxy failure). That was **not** a migration problem — the shell was still on a modern JDK
(it defaulted to Java 23). Boot 2.0 still targets Java 8; the gate must run under `jdk8u482-b08`. Set
`JAVA_HOME` first and the error vanishes. *Lesson: confirm the toolchain before diagnosing the code.*

### Finding 1 (blocker) — log4j2 bridge collision: `log4j-slf4j2-impl` vs `log4j-to-slf4j`

Every `@SpringBootTest` and the Tika test died at static-init with:

```
org.apache.logging.log4j.LoggingException: log4j-slf4j2-impl cannot be present with log4j-to-slf4j
```

Boot 2.0's `spring-boot-starter-logging` newly ships **`log4j-to-slf4j`** (Log4j2-API → SLF4J → Logback);
`tika-app` drags the opposite bridge **`log4j-slf4j2-impl`** (SLF4J → Log4j2). Both on one classpath is
forbidden, and Log4j2 aborts. The first instinct — exclude the standalone `log4j-slf4j2-impl`/`log4j-core`
from tika — **did not work**: tika-app *shades an unrelocated copy* of that binding inside its uber-jar
(the same shaded-jar trap as bcprov/slf4j in earlier steps), so the conflict survives the exclusion.
**Fix:** exclude **`log4j-to-slf4j`** instead (the bridge Boot 2.0 newly added). A single exclusion on
`spring-boot-starter-web` removes it from the whole dedup'd graph. This keeps tika-app's shaded log4j2 as
the live backend — so `LoggingBackendProbeTest` **stays green**, proving the backend did not move.

### Finding 2 (blocker) — Spring Boot Admin is version-locked to the Boot generation

Context init then failed with `NoClassDefFoundError: org/springframework/boot/bind/RelaxedPropertyResolver`,
thrown from `de.codecentric...SpringBootAdminClientEnabledCondition` in
**`spring-boot-admin-starter-client:1.5.7`**. That class was deleted in Boot 2.0; Admin 1.5.x is built for
Boot 1.5. **Fix:** bump Admin to its 2.0 line (`2.0.6`). This is the same "generation-locked" lesson as the
Step 7 Netflix pins, but for an ecosystem library the OpenRewrite recipe doesn't know about.

### Finding 3 (blocker) — Boot 2.0's BOM downgrades `commons-io` below what Tika needs

`HelloControllerTest.extractTextReadsPlainText` (Tika) threw
`NoSuchMethodError: CloseShieldInputStream.wrap(InputStream)`. Boot 2.0's BOM pins `commons-io` to **2.5**,
which now sorts ahead of tika-app's shaded copy and wins — but `wrap(...)` is a static factory added in
commons-io **2.9**, and Tika 2.9.0 calls it. **Fix:** pin `commons-io 2.13.0` (the version Tika 2.9.0
expects) so the live winner carries the method. Another classpath-winner flip the jump caused, caught only
because a test actually *exercises* Tika.

### Finding 4 (corrected) — the OAuth2 contract changed shape (401 → 302); the bridge is a no-op here

The draft predicted that adding `spring-security-oauth2-autoconfigure` would keep the `401`/`200` contract
green. **Reality: it did not, and the bridge was a no-op** (the app uses no OAuth2 features). Under Boot
2.0's default Spring Security 5 autoconfig: valid credentials still return **200**, but no/wrong
credentials now return **302** (redirect to the form-login entry point) instead of a **401** Basic
challenge. The wall still holds — unauthenticated access is denied — so this is a **contract-shape change,
not a hole**. The honest move is to **characterize the new 302 reality** in
`OAuth2SecurityCharacterizationTest` and record that restoring an API-style 401 belongs to the deferred
OAuth2 re-architecture (Step 9), not here. The bridge dependency was **removed** as dead weight.

### What the predictions got right

- The **two Actuator assertions flipped exactly as designed** (root `/health` → `/actuator/health`), and
  Boot 2.0's opt-in exposure did require `management.endpoints.web.exposure.include=health`.
- The **`security.user.* → spring.security.user.*`** rename was needed (else the 200 case loses its
  deterministic password).
- **`jsr311-api` left the tree** under Finchley, flipping `JavaeeApiProbeTest`'s
  `javax.ws.rs.core.Response` winner to `javaee-api` — a genuine characterization update.
- `Finchley.SR4` is the correct train for Boot `2.0.9.RELEASE`; the `spring-cloud-netflix` family moved to
  its 2.x major with no further breakage.

> Note: `spring-boot-properties-migrator` remains a *recommended optional aid* for surfacing property
> renames, but on this project only the `security.user.*` key mattered, and it was found directly from the
> failing test — so it was not required in the end.

## Step 8 — exact actions (perform by hand)

All commands assume **PowerShell** on Windows, from the project root
(`C:\Users\a.vergnaud\dev\modernization-labs\03-spring-cloud`).

### 0. Work on a throwaway branch

This step makes real, committed changes — do it on a branch so the whole jump is one revertible unit:

```powershell
git switch -c step8/boot-2.0-finchley
```

### 1. Capture the "before" baseline under JDK 8 (the regression oracle)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482

mvn -q dependency:tree "-DoutputFile=dependency-tree.step8-before.txt"
mvn -q test       # the full Step 5 net + probes must be GREEN before the jump (18 tests)
```

Confirm the file was written and the run reported **BUILD SUCCESS, 18 tests**. If the baseline is not
green, stop — the jump is only gradable against the known-good stack.

### 2. Switch this shell to JDK 17 and run the OpenRewrite *applier* (layer 1)

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.18'   # adjust to your JDK 17 install path
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 17.x
```

Re-confirm the plugin/recipe pair resolves (Step 6 §3 — resolve the *current* pair, do not trust the
numbers here), then run the applier with the same recipe Step 6 inventoried:

```powershell
mvn org.openrewrite.maven:rewrite-maven-plugin:6.42.0:run `
  "-Drewrite.recipeArtifactCoordinates=org.openrewrite.recipe:rewrite-spring:6.33.0" `
  "-Drewrite.activeRecipes=org.openrewrite.java.spring.boot2.UpgradeSpringBoot_2_0"
```

(fall back to `:runNoFork` if the JDK-17 fork fails — record it.) Then **review the diff before
trusting it** — it should match `step6.rewrite.patch` minus the three Netflix pins already removed in
Step 7:

```powershell
git --no-pager diff -- pom.xml
```

Expect the parent at `2.0.9.RELEASE` and the redundant `<version>` pins removed. **Do not let the recipe
touch `<java.version>`** (it should not; verify it stayed `1.8`).

### 3. Hand-apply the Spring Cloud train move (layer 2)

In `<properties>`, change the train (version-locked to Boot 2.0):

```xml
        <spring-cloud.version>Finchley.SR4</spring-cloud.version>   <!-- was Edgware.SR6 -->
```

This is hand-applied because the recipe's Finchley coverage is thin (Step 6 finding). The
`spring-cloud-netflix` family moves to its 2.x major as a consequence — the tree diff + probes gate it.

### 4. Hand-apply the test characterization updates (layer 3)

These are test-file-only edits, each one a Step 5 knot recording the 2.0 reality:

a. **Migrate the deterministic-credentials keys** in both `@TestPropertySource` blocks
(`OAuth2SecurityCharacterizationTest`, `ActuatorEndpointCharacterizationTest`):

```java
@TestPropertySource(properties = {
        "spring.security.user.name=probe",       // was security.user.name
        "spring.security.user.password=probe-pw" // was security.user.password
})
```

b. **Flip the Actuator characterization** to the 2.0 contract (the designed flip), and add the opt-in
exposure property so `health` is served over HTTP at all:

```java
@TestPropertySource(properties = { /* spring.security.user.* as above */
        "management.endpoints.web.exposure.include=health" })
// healthIsServedUnderActuatorPrefixWithStatusBody(): GET /actuator/health -> 200 or 503 + "status":
// rootHealthIsGoneIn20():                            GET /health          -> 404
```

c. **Flip `JavaeeApiProbeTest`**: under Finchley `jsr311-api` leaves the tree, so
`javax.ws.rs.core.Response` now resolves to `javaee-api` (was `jsr311-api`).

d. **Characterize the changed OAuth2 contract** (see finding 4): valid creds still `200`, but no/wrong
creds now `302` (redirect to login), not `401`. Update the two assertions to expect `HttpStatus.FOUND`.
Do **not** add `spring-security-oauth2-autoconfigure` — it is a no-op here and restoring a 401 is Step 9.

### 5. Hand-apply the generation-locked transitive fixes (layer 4) — the blockers

None of these was in the Step 6 inventory; each is a hard failure the jump exposed (see findings 1–3).
Apply them to `pom.xml`:

```xml
<!-- finding 1: exclude Boot 2.0's log4j-to-slf4j (collides with tika-app's shaded log4j-slf4j2-impl) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-to-slf4j</artifactId>
        </exclusion>
    </exclusions>
</dependency>

<!-- finding 2: Spring Boot Admin is generation-locked; 1.5.7 calls the deleted RelaxedPropertyResolver -->
<!-- de.codecentric:spring-boot-admin-starter-client  version 1.5.7 -> 2.0.6 -->

<!-- finding 3: pin commons-io to what Tika 2.9.0 needs (Boot 2.0's BOM pins 2.5, missing wrap()) -->
<dependency>
    <groupId>commons-io</groupId>
    <artifactId>commons-io</artifactId>
    <version>2.13.0</version>
</dependency>
```

> Discover these the honest way: run the gate, read each stack trace, fix the one it names, re-run. The
> probes and the Step 5 net are what surface them — finding 1 takes down every Spring/Tika test at once,
> finding 2 stops the context from starting, finding 3 throws only when a test actually exercises Tika.

### 6. Verification gate under JDK 8

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'   # finding 0: NOT a modern JDK
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version    # expect: 1.8.0_482
mvn clean package
```

**Verified result on `Boot 2.0.9 + Finchley.SR4`:** `mvn test` → **BUILD SUCCESS, 18 tests, 0 failures**.
The net told its story knot by knot — the two **Actuator** assertions flipped (now `/actuator/*`), the
`JavaeeApiProbeTest` and OAuth2 assertions flipped to the 2.0 reality, while **JAXB**, the **JWT/bcpkix**
path, **`LoggingBackendProbeTest`** (backend did not move), and the **BC probes** all stayed green.

### 7. Capture the "after" tree and diff it

```powershell
mvn -q dependency:tree "-DoutputFile=dependency-tree.step8-after.txt"
Compare-Object (Get-Content dependency-tree.step8-before.txt) (Get-Content dependency-tree.step8-after.txt)
```

Expected movement is large but coherent: Boot `1.5.22 → 2.0.9`, the Spring Cloud train Edgware → Finchley
(Netflix to its 2.x major), Spring Framework 4.3 → 5.0, the BOM-managed transitives moving with them, plus
the four deliberate adjustments (the `log4j-to-slf4j` exclusion, `spring-boot-admin 2.0.6`, `commons-io
2.13.0`, and `jsr311-api` leaving). A **dropped framework**, a pin you did not touch moving, or
`<java.version>` changing is a finding to investigate. **Record the salient deltas** below.

## Step 8 — done criteria (all must hold)

1. `mvn clean package` under **JDK 8** → **BUILD SUCCESS**, fat jar repackaged, `<java.version>` still
   `1.8`. *(Verified: `mvn test` green, 18/18.)*
2. The net flipped **only where predicted-or-explained**: the two Actuator assertions (designed),
   `JavaeeApiProbeTest`'s JAX-RS winner (Finchley dropped `jsr311`), and the OAuth2 `401 → 302`
   (finding 4). `JaxbArithmeticsCharacterizationTest`, `LoggingBackendProbeTest`, the JWT/bcpkix path, and
   the BC probes all stayed green. No *unexplained* red survived.
3. The OpenRewrite layer-1 diff matched `step6.rewrite.patch` (minus the Step-7 Netflix pins); layers 2–4
   are the only hand-applied changes, each traceable to a finding above.
4. The three layer-4 blockers (findings 1–3) are fixed and the build no longer throws `LoggingException`,
   `RelaxedPropertyResolver`, or `CloseShieldInputStream.wrap`.
5. The before/after tree diff is reviewed and pasted in: Boot/Spring/Spring-Cloud move together to the
   2.0/Finchley generation, nothing unrelated dropped.

curl -u user:2d8443ad-67a1-481d-9481-2121d9268f3e  http://localhost:8080/

probe:probe-pw will not work against the running jar — you'll get a 302 (redirect to login), same as any unknown credential. Why: those credentials exist only inside the tests. They're injected by @TestPropertySource(properties = {"spring.security.user.name=probe", ...}) on OAuth2SecurityCharacterizationTest and ActuatorEndpointCharacterizationTest. @TestPropertySource is loaded only into the test ApplicationContext — it's never written to application.properties and never packaged into the jar. (Recall there's no src/main/resources/application.properties in this project at all.)

> _Record below (OpenRewrite versions / fork mode, tree-diff summary):_
>
> Applied: parent `2.0.9.RELEASE`, `Finchley.SR4`; excluded `log4j-to-slf4j` from `spring-boot-starter-web`;
> `spring-boot-admin-starter-client` `1.5.7 → 2.0.6`; pinned `commons-io 2.13.0`. Tests: **18/18 green** on
> `jdk8u482-b08`. OAuth2 contract recorded as `302` (was `401`); `spring-security-oauth2-autoconfigure`
> bridge tried and removed as a no-op.

## Rollback

Step 8 is a whole-generation jump confined to one branch. To revert:

```powershell
git restore --staged --worktree .
git switch main
git branch -D step8/boot-2.0-finchley
```

Delete `dependency-tree.step8-before.txt` / `dependency-tree.step8-after.txt` if not keeping them.

## Toward Step 9

With the platform on `Boot 2.0.9 + Finchley.SR4` and the net green (bar the two intentional Actuator
flips, now updated), the deferred sub-hops become the next gated steps:

- **Re-architect off the EOL `spring-security-oauth2`, and decide the security contract.** Step 8 left the
  wall holding but with a *changed shape*: no/wrong creds now `302`-redirect to login, not `401` (finding 4).
  Retiring the standalone module for Spring Security's built-in resource server is its own behaviour-bearing
  hop — and the place to deliberately restore an API-style `401` if that is the desired contract. Gated by
  `OAuth2SecurityCharacterizationTest` (which currently pins the `302` reality).
- **Walk the Boot 2.x line: 2.1 → 2.7**, one minor at a time, each graded by the same net (and each a
  natural `UpgradeSpringBoot_2_x` OpenRewrite recipe, applier-mode, as in this step).
- **Then the big ones the roadmap has flagged since Step 1:** Java 8 → 11 → 17, and **Boot 3.0 + the
  `javax → jakarta` namespace migration** — the latter another large OpenRewrite-driven jump, gated by a
  net widened first over whatever 3.0 disturbs. Same discipline, all the way up.