# initial stack

## Java 8 version

CMD (cmd.exe)
```
set "JAVA_HOME=C:\Program Files\Java\jdk8u482-b08"
set "PATH=%JAVA_HOME%\bin;%PATH%"
```

Powershell
```
$env:JAVA_HOME = 'C:\Program Files\Java\jdk8u482-b08'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

## Stack Analysis — `com.acme:my-product:1.0.0`

Analysis of the Maven dependency tree for the legacy application targeted for modernization.

> This section describes the **original declared stack** (the coordinates/versions the legacy
> POM intended). Some of these do not resolve from Maven Central as-is. The **reproduced,
> actually-resolving stack** — with the substitutions applied — is documented in
> *Reproduction & Verification* below.

This is fundamentally a **Java 8 / Spring Boot 1.5.17 / Spring 4.3 / Spring Cloud Edgware**
application (circa 2018), with a handful of libraries force-upgraded far past that era
(Tika 2.9.0, POI 5.2.3, BouncyCastle 1.76, `log4j-slf4j2-impl` 2.20.0) via explicit version
overrides in the original POM. That hybrid is exactly the kind of thing a migration wants to rationalize.

### Core frameworks

| Framework | Version | Notes |
|---|---|---|
| Spring Boot | 1.5.17.RELEASE | starter, web, amqp, actuator, test, configuration-processor |
| Spring Framework | 4.3.20.RELEASE | core/beans/context/jdbc/tx/aop/oxm/web/webmvc/jms/messaging/expression/test |
| Spring Security | 4.2.9.RELEASE | core/config/web/crypto |
| Spring Security OAuth2 | 2.0.16.RELEASE | the old standalone OAuth (EOL — migration pain point) |
| Spring Security JWT | 1.0.9.RELEASE | + spring-security-rsa 1.0.3 |
| Spring Cloud | Edgware train | Netflix 1.4.x (openfeign 1.4.6, ribbon 1.4.6, hystrix 1.4.4, core 1.4.4), commons 1.3.x, context 1.1.7 |
| Spring WS | 2.4.3.RELEASE | spring-ws-core + spring-xml |
| Spring AMQP / Rabbit | 1.7.11.RELEASE | + spring-retry 1.2.2 |
| Spring Data Commons (core) | 1.4.1.RELEASE | very old, transitive (see ignore rule below) |
| Springfox Swagger | 2.7.0 | `provided` — migration target is springdoc |
| Spring Boot Admin (codecentric) | 1.5.7 | client |

### Key libraries

- **Oracle**: `ojdbc7` 12.1.0.2, `ucp` 12.1.0.2, `ons` 12.1.0 *(groupId `com.oracle` — not on Maven Central; see resolvability note)*
- **IBM MQ**: `mq-jms-spring-boot-starter` 0.0.4, `com.ibm.mq.allclient` 9.0.4.0
- **RabbitMQ**: `amqp-client` 4.8.3, `http-client` 1.1.1
- **Apache Tika** 2.9.0 — full standard parser package (drags in the huge subtree)
- **Apache POI** 5.2.3, **PDFBox** 2.0.29 (both via Tika)
- **iText (legacy)** `com.lowagie:itext` 2.1.7 — ancient, pulls legacy BouncyCastle `jdk14:138`
- **Jackson** 2.8.x (databind 2.8.11.2)
- **Hibernate Validator** 5.3.6.Final
- **Tomcat embed** 8.5.34
- **Logging**: Logback 1.2.3, Log4j2 **2.7** (core/api) + `log4j-slf4j2-impl` **2.20.0** *(note the 2.7↔2.20 mismatch — a smell the migration should clean up)*, SLF4J 1.7.25
- **BouncyCastle**: jdk15on 1.56/1.57, jdk18on 1.76, jdk14 138/1.38
- **Netflix OSS**: Hystrix 1.5.12, Ribbon 2.2.5, Archaius 0.6.6, RxJava 1.2.0, Feign 9.5.0 (all EOL)
- **Kubernetes**: fabric8 `spring-cloud-kubernetes` 0.1.6, `kubernetes-client` 2.2.0, OkHttp 3.6.0
- **Guava** 18.0, **Jolokia** 1.3.7, **JavaEE API** 8.0, **javax.mail** 1.5.6
- **Gatling** 2.3.0 (test, Scala 2.12.3 / Akka 2.5.4 / Netty 4.0.51), **H2** 1.4.197 (test)

### Scopes in play

- **compile** — the bulk of the runtime stack
- **test** — H2, the entire Gatling tree, and `spring-boot-starter-test` (JUnit **4.12**, Mockito 1.10.19, AssertJ 2.6.0, Hamcrest 1.3, JSONassert, spring-test)
- **provided** — the Springfox tree (swagger2/ui/spi/core/schema, spring-plugin 1.2.0, mapstruct 1.1.0, byte-buddy, reflections, javassist). Note a few *leaves* are `compile` (swagger-annotations, guava, classmate)
- **runtime** — parts of the Netflix Ribbon tree (ribbon-transport, rxnetty 0.4.9, jersey-client, servo, javax.inject), findbugs `annotations`, jakarta.activation

### Resolvability notes for reproduction

- **Oracle** `com.oracle:ojdbc7:12.1.0.2` / `ucp` / `ons` are not on Maven Central (those
  coordinates only live on third-party mirrors; Oracle's official Central artifacts use
  groupId `com.oracle.database.jdbc` and don't publish 12.1.0.2). Substitute resolvable
  `com.oracle.database.jdbc` equivalents to reproduce the stack shape.
- **iText** `com.lowagie:itext:2.1.7` is on Central but drags legacy
  `bouncycastle:bcmail-jdk14:138` (version "138") which often won't resolve — exclude and
  substitute a modern BouncyCastle if needed.

## Reproduction & Verification

A minimal but **buildable and runnable** Spring Boot app reproducing the stack above, used as
a fixture for running modernization/migration recipes.

- `src/main/java/com/acme/myproduct/HelloApplication.java` — `@SpringBootApplication`
- `src/main/java/com/acme/myproduct/HelloController.java` — `@RestController`, `GET /` → `hello world`
- `src/test/java/com/acme/myproduct/HelloControllerTest.java` — trivial JUnit 4 test
- `pom.xml` — `com.acme:my-product:1.0.0`, jar, Java 8, Boot 1.5.17 parent + Spring Cloud Edgware.SR4 BOM
- `dependency-tree.txt` — the full resolved tree

### Build status (verified)

| Gate | Result |
|---|---|
| `mvn clean package` | **BUILD SUCCESS** — compiles, JUnit 4 test passes 1/1, fat jar repackaged |
| `mvn dependency:tree` | **BUILD SUCCESS** — written to `dependency-tree.txt` |
| App startup | **OK** — `Started HelloApplication`, embedded Tomcat up |
| `GET /` | **HTTP 200 `hello world`** (once authenticated — see below) |

### Substitutions applied vs the originaly required stack

| Original | Reproduced as | Why |
|---|---|---|
| `com.oracle:ojdbc7:12.1.0.2`, `ucp`/`ons:12.1.0` | `com.oracle.database.jdbc:ojdbc8:21.9.0.0` + `ucp:21.9.0.0` (ons dropped) | Originals not on Central; ojdbc8 keeps it Java-8/JDBC-4.2 |
| Spring Cloud `context 1.1.7` | resolves to **1.3.4** (Edgware.SR4) | In Edgware, `spring-cloud-context` shares its version with `spring-cloud-commons` (1.3.x); 1.1.7 doesn't exist for this train |
| netflix `ribbon 1.4.6` | ribbon/archaius starters resolve to **1.4.5** | BOM-managed skew alongside the pinned 1.4.4 netflix-core/hystrix — "same stack, not same tree" |
| `tomcat-embed-core 8.5.34` | bumped via `<tomcat.version>8.5.34</tomcat.version>` | Moves all `tomcat-embed-*` together (parent ships 8.5.23) so embedded Tomcat stays consistent |
| iText legacy BC `jdk14:138` | **no exclusion needed**; added `bcprov-jdk15on:1.57` | iText 2.1.7 on Central declares `org.bouncycastle:*-jdk14:1.38` (resolves fine); the broken `:138` coordinates never appeared |

Tika 2.9.0 pulls **POI 5.2.3** and **PDFBox 2.0.29** as real tree nodes (not shaded), matching the target.

### How to test `GET /` returns `hello world`

Because `spring-security-oauth2` is on the classpath, Boot 1.5 auto-secures **every** endpoint
with HTTP Basic. A bare request therefore returns **401**, which is expected — the controller
sits behind the security filter. Authenticate to see `hello world`.

**1. Build and run** (default port 8080):

```
mvn clean package
java -jar target/my-product-1.0.0.jar
```

**2. Grab the generated password** printed at startup (username is `user`):

```
Using default security password: 01f83977-1900-4afc-9d99-1b98f615ac0e
```

**3a. Test with curl** (bash):

```
curl -u user:<password> http://localhost:8080/
# -> hello world          (HTTP 200)

curl -i http://localhost:8080/
# -> HTTP/1.1 401          (no credentials, as expected)
```

**3b. Test with PowerShell** (parses the password straight out of a captured log):

```powershell
java -jar target\my-product-1.0.0.jar > app.log 2>&1   # run in another window
$pw   = (Select-String -Path app.log -Pattern 'security password: (.+)$').Matches.Groups[1].Value.Trim()
$cred = New-Object System.Management.Automation.PSCredential('user', (ConvertTo-SecureString $pw -AsPlainText -Force))
(Invoke-WebRequest -Uri http://localhost:8080/ -Credential $cred -UseBasicParsing).Content
# -> hello world
```

> To make `/` open (no auth) for recipe runs, add `security.basic.enabled=false` to
> `src/main/resources/application.properties`.

## org.apache.commons:commons-lang3:3.3.2

A pure-compute leaf dependency, exercised by a minimal `StringUtils` call so the
artifact lands on the genuinely-used classpath (not just resolved in the tree).

`HelloController` gains a small helper that routes a string through two
`commons-lang3` calls:

```java
import org.apache.commons.lang3.StringUtils;

public String shout(String value) {
    return StringUtils.upperCase(StringUtils.trimToEmpty(value));
}
```

### How to test

No broker, DB, or running server needed — it's covered by a JUnit 4 unit test
in the existing style (`HelloControllerTest`):

```java
@Test
public void shoutTrimsAndUppercases() {
    HelloController controller = new HelloController();
    assertEquals("HELLO", controller.shout("  hello  "));
}
```

Run just this test:

```
mvn -Dtest=HelloControllerTest test
```

Or the whole suite (the gate that proves the dependency compiles and runs):

```
mvn clean test
```

Expected: **BUILD SUCCESS**, `HelloControllerTest` passes. If `commons-lang3`
were missing from the classpath the test class would fail to compile, so a green
run is itself the proof the dependency is wired in.

## org.apache.commons:commons-collections4:4.1

Another pure-compute leaf, exercised with a `CollectionUtils` call that has **no
`java.util` equivalent** — so the usage unambiguously requires the artifact on the
classpath, not something the compiler could satisfy from the JDK.

`HelloController` gains a small helper:

```java
import java.util.Collection;
import org.apache.commons.collections4.CollectionUtils;

public boolean hasOverlap(Collection<?> a, Collection<?> b) {
    return CollectionUtils.containsAny(a, b);
}
```

### How to test

No infra needed — covered by a JUnit 4 unit test in the existing style
(`HelloControllerTest`):

```java
@Test
public void hasOverlapDetectsSharedElement() {
    HelloController controller = new HelloController();
    assertTrue(controller.hasOverlap(asList(1, 2, 3), asList(3, 4)));
    assertFalse(controller.hasOverlap(asList(1, 2), asList(3, 4)));
}
```

Run just this test, or the whole suite:

```
mvn -Dtest=HelloControllerTest test
mvn clean test
```

Expected: **BUILD SUCCESS**, `HelloControllerTest` passes. Since
`CollectionUtils.containsAny` exists only in `commons-collections4`, a green run
is itself the proof the dependency is wired in.

## com.lowagie:itext:2.1.7

The legacy iText PDF library — exercised entirely **in memory** (into a `byte[]`,
no file or stream to disk), so it stays infra-free while genuinely driving iText's
document pipeline (`Document` + `PdfWriter` + `Paragraph`).

`HelloController` gains a helper that renders a one-page PDF:

```java
import java.io.ByteArrayOutputStream;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;

public byte[] toPdf(String text) throws Exception {
    Document document = new Document();
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    PdfWriter.getInstance(document, out);
    document.open();
    document.add(new Paragraph(text));
    document.close();
    return out.toByteArray();
}
```

> Note: iText's API declares checked exceptions, so `toPdf` carries `throws
> Exception` — slightly less tidy than the commons one-liners, but unavoidable here.

### How to test

No infra needed — covered by a JUnit 4 unit test in the existing style
(`HelloControllerTest`). Every PDF starts with the magic header `%PDF`, so that's
the cheapest proof the bytes are a real document:

```java
@Test
public void toPdfProducesPdfBytes() throws Exception {
    HelloController controller = new HelloController();
    byte[] pdf = controller.toPdf("hello world");
    assertTrue(pdf.length > 0);
    assertEquals("%PDF", new String(pdf, 0, 4, "US-ASCII"));
}
```

Run just this test, or the whole suite:

```
mvn -Dtest=HelloControllerTest test
mvn clean test
```

Expected: **BUILD SUCCESS**, `HelloControllerTest` passes. The `%PDF` header can
only be produced by iText here, so a green run is itself the proof the dependency
is wired in.

## tika-app:2.9.0

Apache Tika — exercised entirely **in memory** (parsing a `byte[]`, nothing on
disk), so it stays infra-free while genuinely driving Tika's auto-detect parser
pipeline (the same machinery that pulls in the POI / PDFBox subtree).

`HelloController` gains a helper that extracts text from arbitrary bytes via the
`org.apache.tika.Tika` facade:

```java
import java.io.ByteArrayInputStream;
import org.apache.tika.Tika;

public String extractText(byte[] content) throws Exception {
    return new Tika().parseToString(new ByteArrayInputStream(content)).trim();
}
```

> Note: `tika-app` is a shaded "everything" jar, and `parseToString` loads parsers
> via the service loader at runtime — so a green test proves the dependency is
> wired in *and* actually functional, not just present on the classpath.

### How to test

No infra needed — covered by a JUnit 4 unit test in the existing style
(`HelloControllerTest`). Feed it plain-text bytes and assert the extracted text
comes back:

```java
@Test
public void extractTextReadsPlainText() throws Exception {
    HelloController controller = new HelloController();
    String text = controller.extractText("hello tika".getBytes("UTF-8"));
    assertTrue(text.contains("hello tika"));
}
```

Run just this test, or the whole suite:

```
mvn -Dtest=HelloControllerTest test
mvn clean test
```

Expected: **BUILD SUCCESS**, `HelloControllerTest` passes. Tika's
`parseToString` runs the full detect-and-parse pipeline, so a green run is itself
the proof the dependency is wired in.

## bcprov-jdk15on:1.57

### runtime probe

there are four different provider jars, all compile scope, all on
  the runtime classpath simultaneously:
```
┌───────────────────────────────────────────────────────┬─────────────────────┬───────────┐
│                   Jar (coordinate)                    │    Pulled in by     │ Tree line │
├───────────────────────────────────────────────────────┼─────────────────────┼───────────┤
│ bouncycastle:bcprov-jdk14:138 (old groupId)           │ iText               │ 222       │
├───────────────────────────────────────────────────────┼─────────────────────┼───────────┤
│ org.bouncycastle:bcprov-jdk14:1.38                    │ iText → bctsp-jdk14 │ 224       │
├───────────────────────────────────────────────────────┼─────────────────────┼───────────┤
│ org.bouncycastle:bcprov-jdk15on:1.57 (our direct dep) │ declared in pom     │ 226       │
├───────────────────────────────────────────────────────┼─────────────────────┼───────────┤
│ org.bouncycastle:bcprov-jdk18on:1.76                  │ Tika subtree        │ 144       │
└───────────────────────────────────────────────────────┴─────────────────────┴───────────┘
```

Maven's "nearest-wins" conflict resolution only dedupes within one groupId:artifactId. bcprov-jdk14, bcprov-jdk15on, bcprov-jdk18on are different artifactIds (the jdkXX suffix is the target-JDK edition), so Maven keeps all of them. But every edition ships the same package and class names — org.bouncycastle.jce.provider.BouncyCastleProvider, etc. So they're split-package duplicates: same FQCN, four jars.
When two jars on the classpath contain the same fully-qualified class, the JVM loads whichever the classloader reaches first — i.e. classpath order, which Maven derives from resolution/declaration order, not from the version. So you cannot conclude "1.57 wins because we declared it." In fact our direct 1.57 sits at line 226, after the Tika 1.76 (line 144) and the iText jdk14 jars — so an earlier edition could easily shadow it.
How to actually know: ask the JVM.

The only reliable answer is a runtime probe.

The BouncyCastleProvider that actually loads comes from tika-app-2.9.0.jar, at version 1.76 — not from any bcprov-* jar in the tree, and not our declared bcprov-jdk15on:1.57.

What this means: tika-app is a shaded uber-jar: it bundles a copy of the org.bouncycastle.* classes inside itself without relocating them. So there's actually a fifth copy of BouncyCastle on the classpath that doesn't even show up as a bcprov node in dependency-tree.txt — and because tika-app sorts early on the classpath (well before our bcprov-jdk15on:1.57 at line 226), its shaded BouncyCastleProvider wins.

Consequences:
 - bcprov-jdk15on:1.57 is effectively dead at runtime for the provider class — its classes are shadowed. The tree lists it, but the JVM never loads it.
 - The dependency tree is actively misleading here: the winner isn't any of the nodes you'd inspect. You only find it by asking the JVM.
 - This is textbook split-package "jar hell," and it's exactly the kind of latent risk a modernization effort needs to surface.

## javax:javaee-api:8.0

`javaee-api` is an **API aggregator** — one jar covering dozens of `javax.*`
packages. After the BouncyCastle lesson, we can't assume a `javax.*` import binds
to *this* jar: many of its packages are shadowed by more-specific jars elsewhere
on the classpath. So before implementing, a runtime probe (`Class.forName(...)` +
`getCodeSource()`) was used to find a package that genuinely resolves to
`javaee-api-8.0.jar`.

### Runtime probe — which jar actually serves each `javax.*` API

```
┌─────────────────────────────┬─────────────────────────────────────┬───────────────────────────┐
│         javax.* API         │              Served by              │          Verdict          │
├─────────────────────────────┼─────────────────────────────────────┼───────────────────────────┤
│ javax.ws.rs.core.Response   │ jsr311-api-1.1.1.jar                │ shadowed — NOT javaee-api │
│ javax.validation.Validation │ validation-api-1.1.0.Final.jar      │ shadowed — NOT javaee-api │
│ javax.annotation.Resource   │ JDK / bootstrap                     │ shadowed by the JDK       │
│ javax.persistence.*         │ javaee-api-8.0.jar                  │ genuinely javaee-api      │
│ javax.json.* / .ejb / .faces│ javaee-api-8.0.jar                  │ genuinely javaee-api      │
└─────────────────────────────┴─────────────────────────────────────┴───────────────────────────┘
```

Takeaway: JAX-RS, Bean Validation and `javax.annotation` imports do **not** exercise
`javaee-api` at all — they bind to `jsr311-api`, `validation-api`, and the JDK
respectively. Only packages the probe confirmed (e.g. `javax.persistence`) actually
load from `javaee-api-8.0.jar`.

### The implementation

`HelloController` gains a helper bound to `javax.persistence.EnumType` — a package
the probe confirmed resolves to `javaee-api-8.0.jar`:

```java
import javax.persistence.EnumType;

public String mappingStrategyFor(boolean storeAsText) {
    return (storeAsText ? EnumType.STRING : EnumType.ORDINAL).name();
}
```

Two subtleties that make this a *real* runtime touch of the jar:

- `javaee-api` is a **compile stub** (method bodies are empty/throwing — you bring a
  real app server at runtime). But `EnumType` is an **enum**, so its constants and
  `name()` are compiler-generated, not stubbed — they execute correctly with only the
  API jar present.
- Enum-constant references are **not inlined** (unlike `static final` String/primitive
  constants, which the compiler bakes into the caller). So this genuinely loads the
  `javax.persistence.EnumType` class from `javaee-api-8.0.jar` at runtime.

### How to test

No infra needed — a JUnit 4 unit test in the existing style (`HelloControllerTest`):

```java
@Test
public void mappingStrategyMapsBooleanToJpaEnumType() {
    HelloController controller = new HelloController();
    assertEquals("STRING", controller.mappingStrategyFor(true));
    assertEquals("ORDINAL", controller.mappingStrategyFor(false));
}
```

Run just this test, or the whole suite:

```
mvn -Dtest=HelloControllerTest test
mvn clean test
```

Expected: **BUILD SUCCESS**, `HelloControllerTest` passes. Because the probe
confirmed `javax.persistence.EnumType` is served by `javaee-api-8.0.jar`, a green
run is proof *this* jar — not a shadowing one — is wired in.

## org.apache.tomcat.embed:tomcat-embed-core:jar:8.5.34

tomcat-embed-core is on the runtime classpath and is actively used at runtime.

Here's the evidence:

It's compile scope, brought in two ways:
- Transitively via spring-boot-starter-web → spring-boot-starter-tomcat (dependency-tree.txt line 18, which pulls tomcat-embed-el and
tomcat-embed-websocket at lines 19–20).
- Declared directly in pom.xml (lines 240–244) to pin it to 8.5.34, reinforced by <tomcat.version>8.5.34</tomcat.version> (line 49) so all tomcat-embed-*
artifacts move together.
- It appears as a real tree node at compile scope: org.apache.tomcat.embed:tomcat-embed-core:jar:8.5.34:compile (line 238).

Why it's genuinely called at runtime (not just present):
This is a Spring Boot 1.5 web app. tomcat-embed-core is the embedded servlet container — it provides the Tomcat, Connector, StandardContext, Coyote HTTP
protocol handler, and the servlet/Catalina classes that Spring Boot instantiates to serve HTTP. The README's own verification confirms this end-to-end:

- App startup → "Started HelloApplication, embedded Tomcat up"
- GET / → HTTP 200 hello world (after auth), and 401 without credentials.

You can't serve a single request without tomcat-embed-core being loaded and executed. So unlike some of the "shaded/shadowed" jars discussed in the README
(e.g. bcprov-jdk15on:1.57, which the tree lists but the JVM never loads), this one is unambiguously live: every HTTP request runs through its classes.

One nuance worth noting: this is the direct-dependency declaration of tomcat-embed-core:8.5.34. There's no shaded duplicate competing for those
Catalina/Coyote class names (unlike the BouncyCastle situation), so the version you see is the version that runs.

## org.jvnet.jaxb2.maven2:maven-jaxb2-plugin:0.13.2

Unlike the dependency probes above, this is a **build-time plugin**: it contributes no
runtime classes of its own, it *generates* them. Its `generate` goal reads a schema and
emits JAXB-annotated Java sources before compilation. So "testing the jar" means proving
the goal runs and produces the expected classes from our schema.

### The implementation

A dummy WSDL — `src/main/resources/wsdl/arithmetics.wsdl` — declares one `Add` operation
(`AddRequest{augend, addend}` → `AddResponse{sum}`, namespace
`http://acme.com/arithmetics`). The plugin is wired into `pom.xml` bound to the `generate`
goal:

```xml
<plugin>
    <groupId>org.jvnet.jaxb2.maven2</groupId>
    <artifactId>maven-jaxb2-plugin</artifactId>
    <version>0.13.2</version>
    <executions>
        <execution>
            <id>generate-arithmetics</id>
            <goals><goal>generate</goal></goals>
        </execution>
    </executions>
    <configuration>
        <schemaLanguage>WSDL</schemaLanguage>
        <schemaDirectory>src/main/resources/wsdl</schemaDirectory>
        <schemaIncludes><include>*.wsdl</include></schemaIncludes>
        <generatePackage>com.acme.myproduct.ws.arithmetics</generatePackage>
    </configuration>
</plugin>
```

With no explicit `<phase>`, the execution binds to the plugin's default
`generate-sources` phase, so it runs automatically as part of `mvn compile` / `package`.

### How to test

No infra and no JUnit needed — the test is that the goal generates the expected sources.
Run the goal directly (or any lifecycle phase from `generate-sources` onward):

```
mvn org.jvnet.jaxb2.maven2:maven-jaxb2-plugin:0.13.2:generate
```

Then confirm the WSDL was turned into JAXB classes under `target/generated-sources/xjc`:

```
target/generated-sources/xjc/com/acme/myproduct/ws/arithmetics/AddRequest.java
target/generated-sources/xjc/com/acme/myproduct/ws/arithmetics/AddResponse.java
target/generated-sources/xjc/com/acme/myproduct/ws/arithmetics/ObjectFactory.java
target/generated-sources/xjc/com/acme/myproduct/ws/arithmetics/package-info.java
```

Expected: **BUILD SUCCESS** and those four files present. `AddRequest` exposes
`augend`/`addend` and `AddResponse` exposes `sum` — proof the plugin parsed our schema
and `xjc` produced the bindings. For a clean check, `mvn clean compile` regenerates them
from scratch and then compiles them alongside the hand-written sources.
