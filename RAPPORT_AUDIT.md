# Rapport d'audit — JGrapa (Java Grade PArser)

**Date :** 15 juillet 2026
**Version auditée :** `0.0.1-SNAPSHOT`, branche `main`, commit `157e853`
**Périmètre :** 16 classes Java (`src/main`), 8 classes de test, 3 schémas JSON, `pom.xml`, `README.adoc`

---

## Résumé exécutif

Le cœur mathématique du projet (les trois agrégateurs) est correct dans son principe et bien pensé : la conception `sealed` + immuabilité + `WeightedMarks` comme représentation intermédiaire est un bon choix d'architecture, et l'algorithme OWA de duplication des poids implémente fidèlement la spécification du README. Le projet n'est cependant **pas en état d'être soumis**, pour trois raisons de nature différente :

1. **Le projet ne compile pas depuis un clone propre.** `mvn clean test` échoue. Les 38 tests ne passent actuellement que parce que `target/` contient des classes compilées par l'IDE. Un relecteur qui clone le dépôt obtient un échec immédiat.
2. **Trois bugs de correction confirmés**, dont un qui fausse silencieusement les notes et un qui rend tout agrégateur inutilisable dans une `HashMap`/`HashSet`.
3. **La fonctionnalité centrale annoncée est absente** : rien dans le code ne sait agréger un `AssessmentTree` (arbre) avec un `Aggregator`. La bibliothèque ne peut pas, aujourd'hui, calculer la note d'un étudiant.

Le point 1 se corrige en une ligne. Le point 2 en une dizaine. Le point 3 est le vrai travail restant, et c'est aussi ce qui constitue la contribution publiable.

> **Note méthodologique.** Chaque bug ci-dessous a été *reproduit à l'exécution*, pas seulement lu. Les correctifs proposés pour B1, B2 et le build ont été prototypés et vérifiés : **ils passent l'intégralité des 38 tests existants**. L'arbre de travail a été restauré à l'état d'origine ; aucune modification n'a été laissée dans le dépôt.

---

## 1. Bloquants

### BL1 — `mvn clean test` échoue : le projet ne compile pas

**Gravité : critique.** C'est le problème le plus grave du rapport, et le plus facile à corriger.

```
[ERROR] Source option 5 is no longer supported. Use 8 or later.
[ERROR] Target option 5 is no longer supported. Use 8 or later.
[INFO] BUILD FAILURE
```

**Chaîne causale exacte :**

| Maillon | Constat |
|---|---|
| Maven local | 3.8.7 |
| Défaut du cycle `jar` sous Maven 3.8 | `maven-compiler-plugin:3.1` |
| POM parent `io.github.oliviercailloux:pom:0.0.30` | définit `maven.compiler.release=21`, mais **ne fixe pas la version** du compiler-plugin |
| `maven-compiler-plugin:3.1` | antérieur au paramètre `release` (introduit en 3.6.0) → l'ignore silencieusement → retombe sur `source/target = 1.5` → fatal sous JDK 21 |

Le `pom.xml` du projet ne fixe pas non plus cette version. Le parent épingle checkstyle, dependency, enforcer, gpg, javadoc, source et surefire — mais pas compiler.

Pourquoi cela n'a jamais été vu : le `target/` du poste de travail contient des classes compilées par Eclipse. Maven affiche alors `Nothing to compile - all classes are up to date` et enchaîne sur les tests, qui passent. **La panne n'apparaît qu'au premier `clean`** — c'est-à-dire chez tout relecteur, et sur toute CI.

**Correctif vérifié** (ajouter dans `pom.xml`, section `<plugins>`) :

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.13.0</version>
</plugin>
```

Vérifié : `mvn clean test` → `BUILD SUCCESS`, 38/38 tests. Alternative : exiger Maven ≥ 3.9 (qui utilise 3.13.0 par défaut) — mais épingler la version est plus robuste et c'est la bonne pratique de toute façon (reproductibilité). Idéalement, corriger aussi le POM parent.

### BL2 — Le build dépend d'un répertoire externe absent

`pom.xml:32` copie les schémas depuis un répertoire frère :

```xml
<resource><directory>../Grading schemas/</directory></resource>
```

Ce répertoire **n'existe pas** :

```
[INFO] skip non existing resourceDirectory /home/.../JGrapa/../Grading schemas
```

Le plugin ne fait qu'avertir, donc le build ne casse pas — mais cela signifie que :
- le build n'est **pas auto-suffisant** : il dépend d'un état de disque hors du dépôt ;
- selon que ce répertoire existe ou non, le build écrase (ou non) `src/main/resources/.../schemas/` — donc **le résultat du build dépend de la machine**. C'est une faute de reproductibilité rédhibitoire pour un artefact scientifique.
- ce mécanisme écrit dans `src/`, ce qu'un build ne devrait jamais faire.

**Action :** supprimer cette exécution et versionner les schémas comme unique source de vérité (ils sont déjà dans `src/main/resources/`), **ou** faire du dépôt `grading-schema` une vraie dépendance Maven / un sous-module Git épinglé à un commit.

---

## 2. Bugs de correction (tous reproduits)

### B1 — `Weighter` : la masse de poids « complémentaire » est comptée au dénominateur mais attribuée à personne → notes faussées

**Gravité : critique** (résultats numériques silencieusement faux).
`Weighter.java:57-58`

```java
final double totalWeight = Sets.intersection(marks.criteria(), weights.keySet()).stream()
    .mapToDouble(weights::get).sum() + complement;   // ← complement ajouté inconditionnellement
```

Le `complement` (= `1 − Σ poids explicites`) est destiné aux critères *absents* de `weights`. Mais il est ajouté à `totalWeight` **même quand aucun critère n'est manquant** : il gonfle alors le dénominateur de normalisation sans être distribué à aucun critère. Les poids effectifs ne somment plus à 1.

Cela contredit directement le README (`README.adoc:44`) :
> « In any case, the weights are normalized to sum to one over the provided criteria. »

**Reproduction :** poids `{a: 0.25, b: 0.25}`, notes `{a: 1, b: 1}` (étudiant parfait) :

```
weights={a=0.25, b=0.25}  sum=0.5  →  note finale = 0.5
```

**Un étudiant ayant la note maximale partout obtient 0,5 sur 1.** De même, un critère unique pondéré 0.25 avec la note 1 rend 0.25 au lieu de 1.

Le garde-fou de `WeightedMarks` (`Aggregator.java:82`) ne détecte rien : il vérifie `total ≤ 1`, jamais `total = 1`.

**Correctif vérifié :**

```java
final double totalWeight = Sets.intersection(marks.criteria(), weights.keySet()).stream()
    .mapToDouble(weights::get).sum() + (missingInWeights.isEmpty() ? 0d : complement);
```

Après correctif : étudiant parfait → `1.0`, poids `{a=0.5, b=0.5}`. **Les 38 tests existants passent toujours** — ce qui prouve qu'il ne s'agit pas d'un désaccord d'interprétation avec la spec, mais d'un trou de couverture (cf. T1).

### B2 — `hashCode()` / `toString()` lèvent `StackOverflowError` sur tout agrégateur sans `defaultSub` explicite

**Gravité : critique.**
`Aggregator.java:156-158`, `Weighter.java:94`, `Owa.java:144`, `Parametric.java:105`

```java
public Aggregator defaultSub() {
  return defaultSub.orElse(Weighter.FULL_EQUAL_WEIGHTER);   // se résout sur un singleton…
}

@Override public int hashCode() {
  return Objects.hash(weights, subs(), defaultSub());        // …dont le hashCode rappelle defaultSub()
}
```

`FULL_EQUAL_WEIGHTER` est documenté comme « a default sub-aggregator that is itself » : il est auto-référentiel. Son `hashCode()` appelle `defaultSub()`, qui le renvoie lui-même, dont le `hashCode()` rappelle `defaultSub()`… → récursion infinie.

Comme `defaultSub()` retourne ce singleton pour **tout** agrégateur construit sans `defaultSub` explicite (le cas le plus courant, cf. `Weighter.given(weights)`), le bug touche l'ensemble de l'API.

**Reproduction :**
```
Weighter.FULL_EQUAL_WEIGHTER.hashCode()        → StackOverflowError
Weighter.FULL_EQUAL_WEIGHTER.toString()        → StackOverflowError
Weighter.given(ImmutableMap.of()).hashCode()   → StackOverflowError
```

Conséquences : impossible de placer un `Aggregator` dans un `HashSet`/`HashMap`, ni de le journaliser, ni de l'inspecter au débogueur. `equals()` en réchappe seulement par accident (`Objects.equals` court-circuite sur `==`).

Pourquoi c'est invisible : la suite de tests n'appelle **jamais** `hashCode()` ni `toString()` sur un agrégateur (`assertEquals` n'utilise que `equals`). Le commit `c53cf77` (« correction boucle infinie Weighter ») a traité une manifestation voisine ; celle-ci subsiste.

**Correctif vérifié :** fonder `equals`/`hashCode`/`toString` sur le champ **stocké** (`Optional`) plutôt que sur la valeur *résolue*. Ajouter dans `Aggregator` :

```java
protected Optional<Aggregator> rawDefaultSub() {
  return defaultSub;
}
```

puis, dans les trois sous-classes, remplacer `defaultSub()` par `rawDefaultSub()` dans `equals`, `hashCode` et `toString`. Vérifié :

```
FULL_EQUAL_WEIGHTER.hashCode() = 29791
toString = Weighter{weights={}, subs={}, defaultSub=Optional.empty}
HashSet<Aggregator> fonctionne
```
→ 38/38 tests toujours verts.

### B3 — `Parametric` : plante sur une note pondératrice négative

**Gravité : élevée.**
`Parametric.java:48-56`

```java
double weightingMark = marks.optionalMark(weighting).orElse(Mark.max()).value();
...
weightsBuilder.put(multiplied, weightingMark);          // ← devient un POIDS
otherCriteria.forEach(c -> weightsBuilder.put(c, (1d - weightingMark) / otherCriteria.size()));
```

Une note est dans `[-1, 1]` (invariant de `Mark`), mais elle est ici utilisée directement **comme poids**, et les poids doivent être dans `[0, +∞[` (`Aggregator.java:79`). Toute note pondératrice négative viole donc l'invariant en aval.

**Reproduction :**
```
multiplied=0.5, weighting=-0.5, other=0.5
  → IllegalArgumentException: All weights must be non-negative.
```
Et pour `weighting = -1`, `(1 − (−1))/n = 2 > 1` ferait aussi sauter le contrôle `total ≤ 1`.

Le README reconnaît le flou (« Not sure negative ones always make sense (for some aggregators) »), mais le code, lui, laisse fuiter une `IllegalArgumentException` provenant d'une classe interne — ni documentée, ni testée, ni rattrapable proprement par l'appelant.

**Action :** trancher explicitement et documenter. Trois options défendables :
1. restreindre `Parametric` aux notes dans `[0, 1]` et le vérifier **à l'entrée** de `aggregate` avec un message clair ;
2. introduire un type `UnitMark` (`[0,1]`) distinct de `Mark` (`[-1,1]`), et typer `weighting` avec ;
3. définir une sémantique pour les notes négatives (p. ex. troncature à 0) et la tester.

L'option 2 est la plus solide et la plus intéressante à défendre dans l'article ; l'option 1 est la plus rapide.

### B4 — Le schéma `exam.schema.json` est non fonctionnel

**Gravité : élevée** (fonctionnalité entièrement morte).

Deux défauts indépendants, chacun suffisant à le casser :

**(a) Références non résolubles.** `ExamJsonConverter.examSchema()` ne mappe vers le classpath que l'URL du schéma *exam*. Or `exam.schema.json` référence en absolu les deux autres schémas (`aggregator`, `assessment-tree`), qui ne sont pas mappés → tentative de **téléchargement réseau** → échec :

```
FileNotFoundException: https://raw.githubusercontent.com/oliviercailloux/grading-schema/main/assessment-tree.schema.json
```

Un validateur qui exige un accès réseau est doublement disqualifiant pour un artefact reproductible : il est non hermétique *et* il ne marche pas.

**(b) Schéma logiquement incohérent.** `exam.schema.json:8-11` :

```json
"required": ["aggregator"],
"$ref": "…/aggregator.schema.json",
"properties": { "assessments": { … } }
```

- `required: ["aggregator"]` exige une propriété `aggregator` qui n'est déclarée nulle part — et que `Exam/Complex.json` (le fichier d'exemple du projet) **ne possède pas** : celui-ci met `weights`/`subs`/`defaultSub` à la racine. Le fichier de test échouerait donc contre son propre schéma.
- le `$ref` vers `aggregator.schema.json` impose que l'examen *soit* un agrégateur, dont chaque branche `oneOf` porte `unevaluatedProperties: false` → `assessments` serait rejetée comme propriété non évaluée.

Rien de tout cela n'est détecté car **il n'existe aucun test pour `exam.schema.json` ni pour `Exam/Complex.json`** (ressource orpheline).

**Action :** mapper les trois URLs vers le classpath dans les trois convertisseurs (extraire une fabrique `SchemaRegistry` commune), puis réécrire le schéma — vraisemblablement `properties: { aggregator: {$ref: …}, assessments: {…} }` avec `required: ["aggregator", "assessments"]` — et ajouter un test de validation. Décider si un examen *contient* un agrégateur (recommandé, plus clair) ou *est* un agrégateur étendu (ce que suggère `Exam/Complex.json`) ; le schéma et l'exemple doivent s'accorder.

### B5 — `Parametric` : les valeurs par défaut donnent la note maximale

**Gravité : moyenne** (conforme au README, mais dangereux par conception).

Si `multiplied` **et** `weighting` sont tous deux absents des notes, les deux valent 1 par défaut, les autres critères reçoivent un poids `(1 − 1) = 0`, et le résultat est `1 × 1 = 1.0` — **quelles que soient les notes réellement obtenues**. Un étudiant noté 0 sur son unique critère évalué obtient 1,0.

C'est la conséquence littérale de la spec (« defaulting to 1 »), donc pas un bug au sens strict — mais c'est un défaut de conception à assumer : *par défaut, le silence vaut note maximale*. Dans un logiciel de notation, un défaut silencieux favorable est exactement le genre de choix qu'un relecteur attaquera. À défendre explicitement dans l'article, ou à remplacer par un échec explicite quand aucun des deux critères n'est présent.

---

## 3. Lacunes fonctionnelles

### F1 — L'agrégation d'arbre n'existe pas *(lacune centrale)*

C'est la fonctionnalité que le titre et le README promettent, et elle est absente.

- `Aggregator.aggregate(OneLevelMarksTree)` ne traite **qu'un seul niveau**.
- `Aggregator.subs(Set<Criterion>)` (`Aggregator.java:54`), qui est le point d'entrée prévu pour la descente récursive, **n'est appelé nulle part** — vérifié par recherche exhaustive.
- Aucune classe ne consomme conjointement un `AssessmentTree` et un `Aggregator`.
- Le README annonce pourtant : « aggregate as much as possible: keep some trees where cannot aggregate ».

**En l'état, la bibliothèque ne sait pas calculer la note d'un étudiant.** C'est le travail restant, et c'est précisément la contribution à publier. Il faut une opération du type :

```java
// Descend l'arbre, agrège les feuilles niveau par niveau via subs()/defaultSub(),
// et conserve les sous-arbres non agrégeables.
AggregatedTree aggregate(AssessmentTree tree, Aggregator aggregator)
```

avec une sémantique explicite pour : la profondeur, les critères présents dans l'arbre mais absents des `subs`, les sous-arbres non réductibles, et la propagation des `feedback`.

### F2 — `Exam` est une coquille vide

`Exam.java` (22 lignes) stocke deux champs et **n'expose aucune méthode** : ni accesseur, ni `equals`/`hashCode`/`toString`, ni calcul. La classe n'est **référencée nulle part** dans le code ni les tests. `ExamJsonConverter` ne contient qu'une méthode `examSchema()` cassée (B4) : ni `toJson`, ni `fromJson`. `StudentId` est un record sans aucune validation (accepte `null`, chaîne vide).

Tout le package `exam` est donc du code mort. Soit on le complète, soit on le retire de la version soumise — mais laisser du code mort dans un artefact évalué est un signal négatif fort.

### F3 — L'agrégation des commentaires n'est pas implémentée

Le README en fait un élément de conception :
> « joined with an Aggregator, a structure that says how to aggregate logically (but does not say how precisely to aggregate comments) […] Implementation is done by chosing a way to combine the comments (e.g. concatenate them with new lines). »

`Assessment` porte bien un `feedback`, mais `OneLevelMarksTree` ne manipule que des `Mark` : le feedback est **perdu dès la première agrégation**. Or la traçabilité de la note (« expliquer pourquoi l'étudiant a échoué ») est un argument de vente majeur pour ce type d'outil, et probablement l'angle le plus original de l'article. C'est une lacune à combler en priorité après F1.

### F4 — TODO de conception ouverts dans le README

`README.adoc:69-74` :
```
TODO ParametricAverager: w and m
TODO ParametricScaler: only w.
TODO defaultSub(): always one, give weighter if none.
```
À trancher et retirer avant soumission : un README qui contient des TODO signale un travail en cours, pas un artefact fini.

---

## 4. Qualité du code

| # | Problème | Emplacement |
|---|---|---|
| Q1 | Aucun Javadoc sur les classes/méthodes publiques ; les rares commentaires sont des notes de travail (`/** non-empty */` inséré au milieu d'une signature de record) | `Aggregator.java:18-21` |
| Q2 | Construction du `defaultSub` incohérente : `Weighter.given`/`Parametric.given` passent `null`, `Owa.given` passe `FULL_EQUAL_WEIGHTER`, et le constructeur d'`Owa` le reconvertit en `null` par comparaison d'identité (`==`) | `Owa.java:28`, `Owa.java:38` |
| Q3 | Comparaison de singleton par identité `!=` — fragile, casse dès qu'un `Weighter` vide équivalent est construit ailleurs | `AggregatorJsonConverter.java:107` |
| Q4 | Imports inutilisés (`checkArgument` dans `Criterion`; `Optional`, `assertTrue` dans 3 classes de test) → aucun linter n'est appliqué, alors que le parent fournit checkstyle | `Criterion.java:3`, tests |
| Q5 | `Serializable` incohérent : `Criterion` et `CompositeAssessmentTree` le sont, mais `Assessment` (record) et `Mark` ne le sont pas → toute sérialisation Java d'un arbre échoue | `assessment/` |
| Q6 | Indentation cassée (accolades fermantes désalignées) | `Aggregator.java:65,158`, `Weighter.java:35` |
| Q7 | `logback.xml` livré dans le JAR : une bibliothèque ne doit pas imposer sa configuration de journalisation à ses consommateurs (à déplacer vers `src/test/resources`) | `src/main/resources/logback.xml` |
| Q8 | `Criterion` accepte un nom vide (commit `c53a1c2`), alors que le schéma impose `minLength: 1` sur `multiplied`/`weighting` → code et schéma divergent | `Criterion.java` vs `aggregator.schema.json:91` |
| Q9 | `Criterion` : classe mutable-friendly avec constructeur package-private et fabrique statique, sans `final` sur la classe — pourrait être un `record` | `Criterion.java:11` |
| Q10 | Pas de `module-info.java` (JPMS) alors que le projet cible Java 21 | — |

---

## 5. Tests

**État :** 38 tests, tous verts (une fois le build réparé). La structure est saine (AAA, noms lisibles, ressources JSON externalisées). Mais la couverture est trompeuse — elle rate les trois bugs critiques ci-dessus.

| # | Lacune | Conséquence |
|---|---|---|
| T1 | **Aucun test avec des poids explicites couvrant tous les critères et sommant à moins de 1** | masque B1 (notes faussées) |
| T2 | **`hashCode()`/`toString()` ne sont jamais appelés** sur un agrégateur | masque B2 (`StackOverflowError`) |
| T3 | Aucun test pour `exam.schema.json`, `Exam`, `ExamJsonConverter`, `StudentId` ; `Exam/Complex.json` est une ressource orpheline | masque B4 |
| T4 | Aucun test pour `Criterion`, `Mark`, `CompositeAssessmentTree`, `WeightedMarks`, `Aggregator.subs(Set)`, `Owa.aggregate(Multiset)` | zones entières non couvertes |
| T5 | Aucun test des cas limites : notes négatives dans `Weighter`/`Owa`, `Mark` hors `[-1,1]`, poids `NaN`/infinis, un seul critère, `subs`/`defaultSub` imbriqués | masque B3 |
| T6 | **Aucun test du contrat `equals`/`hashCode`** (Guava `EqualsTester` est déjà disponible) | masque B2 |
| T7 | Aucun test de **round-trip** `fromJson(toJson(x)) == x` | le sérialiseur peut diverger du parseur sans alerte |
| T8 | Aucune mesure de couverture (pas de JaCoCo) | invérifiable par un relecteur |

**T9 — Test fragile à réécrire.** `aggregator/JsonTests.java:63-64` compare des **chaînes** JSON via une cascade de regex :

```java
assertEquals(expected.replaceAll("\\[\n +0.2,\n +0.8\n +\\]", "[ 0.2, 0.8 ]"),
    converter.toJson(tree).replaceAll(" :", ":").replaceAll("(?m)0\\.0$", "0")
        .replace("{ }", "{}") + "\n");
```

Ce test valide le *formatage* de Jackson, pas la sémantique ; il cassera à la moindre montée de version. `assessment/JsonTests.java:68` fait déjà les choses correctement (`mapper.readTree(expected)`) — appliquer la même approche ici.

---

## 6. Documentation

Le `README.adoc` est un **carnet de notes**, pas une documentation : notes à soi-même (« changed mind », « Not sure negative ones always make sense »), TODO ouverts, pseudo-code de conception, aucun exemple exécutable. Il manque, dans l'ordre d'importance :

- **Un exemple d'utilisation complet** (« construire un barème → noter → obtenir la note ») ;
- ce que le projet *fait*, en un paragraphe, dès la première ligne ;
- installation / coordonnées Maven / version de JDK requise ;
- la **spécification formelle** des trois agrégateurs (actuellement en prose ambiguë : c'est cette ambiguïté qui a permis à B1 de passer inaperçu) ;
- la définition des schémas JSON et leur relation ;
- une justification des choix de conception (pourquoi `[-1,1]` ? pourquoi ces trois agrégateurs ?).

---


## 8. Plan d'action priorisé

### Phase 1 — Réparer (≈ 1 journée, sans quoi rien d'autre ne compte)

1. **BL1** : épingler `maven-compiler-plugin` 3.13.0. *(1 ligne, correctif vérifié)*
2. **BL2** : supprimer la copie depuis `../Grading schemas/`, versionner les schémas.
3. **B1** : corriger la normalisation de `Weighter`. *(1 ligne, correctif vérifié)* + test de non-régression (T1).
4. **B2** : `equals`/`hashCode`/`toString` sur `rawDefaultSub()`. *(correctif vérifié)* + `EqualsTester` (T6).
5. **P1** : ajouter `LICENSE` (MIT ou Apache-2.0).
6. **P2** : GitHub Actions `mvn clean verify` sur JDK 21.
7. Purger `target/` du disque de travail et vérifier que la CI part d'un clone nu.



### Phase 2 — Compléter la contribution (≈ 1 à 2 semaines ; c'est le cœur de l'article)

8. **F1** : implémenter l'agrégation d'arbre (`AssessmentTree` × `Aggregator`), avec une sémantique explicite pour les sous-arbres non réductibles. **C'est la contribution.**
9. **F3** : agrégation des `feedback` — probablement l'angle le plus original vis-à-vis de l'état de l'art.
10. **F2/B4** : compléter le package `exam` (`toJson`/`fromJson`, schéma réparé, tests) **ou** le retirer de la soumission.
11. **B3/B5** : trancher la sémantique des notes négatives et des valeurs par défaut ; documenter et tester.
12. **F4** : trancher les TODO de conception.

### Phase 3 — Rendre publiable (≈ 1 semaine)

13. **Spécification formelle** des trois agrégateurs (définitions mathématiques, propriétés : idempotence, monotonie, bornes) — cela nourrit directement l'article *et* les tests.
14. Tests : combler T3–T5, T7 ; réécrire T9 ; ajouter JaCoCo et viser une couverture défendable.
15. Javadoc complet sur l'API publique + README réécrit avec exemple exécutable.
16. **P9** : un cas d'usage réel de bout en bout (un vrai barème, de vraies copies) — c'est l'argument qui convainc.
17. **P6/P10** : état de l'art et positionnement (Yager 1988 ; outils de notation existants ; bibliothèques MCDA).
18. **P3/P7** : tag `v1.0.0`, publication Maven Central, dépôt Zenodo → DOI.
19. **P5** : rédiger `paper.md`.

---

## 9. Ce qui est déjà bon

À conserver et à mettre en avant dans l'article :

- **Architecture solide** : hiérarchie `sealed` + `switch` exhaustif sur les types, immuabilité systématique (Guava `Immutable*`), fabriques statiques nommées, méthodes `withX` — c'est du Java moderne bien écrit.
- **`WeightedMarks` comme représentation intermédiaire** : très bonne idée. Exposer les poids *effectifs* plutôt que le seul scalaire final rend la note **explicable** et **auditable** — c'est un argument scientifique fort, à défendre explicitement dans l'article.
- **Validation par schéma JSON** aux deux extrémités (sérialisation *et* désérialisation) : rigoureux, et rare.
- **L'algorithme OWA** de duplication/répartition des poids implémente fidèlement la spécification du README (vérifié sur l'exemple `1,3,1 → 1/3,1/3,1/3,3,1/2,1/2`), y compris la conservation de la masse totale.
- **Programmation défensive** cohérente (`checkArgument`/`checkNotNull`/`verify` avec la bonne distinction sémantique entre les trois).
- Les tests existants sont **bien structurés** — le problème est leur couverture, pas leur qualité.

---

## Annexe A — Reproductions vérifiées

Toutes les commandes ont été exécutées sur le commit `157e853` (JDK 21.0.11, Maven 3.8.7, Ubuntu/WSL2).

```bash
# BL1 — le projet ne compile pas depuis un clone propre
$ mvn -B clean test
[ERROR] Source option 5 is no longer supported. Use 8 or later.
[INFO] BUILD FAILURE

# BL1 — après épinglage de maven-compiler-plugin 3.13.0
$ mvn -B clean test
[INFO] Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

# B1 — Weighter : poids {a:0.25, b:0.25}, notes {a:1, b:1}
weights={a=0.25, b=0.25} sum=0.5 PERFECT-STUDENT-GETS=0.5     # attendu : 1.0
# après correctif :
FIXED-1 perfect student -> 1.0 weights={a=0.5, b=0.5}          # 38/38 tests OK

# B2 — StackOverflowError
Weighter.FULL_EQUAL_WEIGHTER.hashCode()       → StackOverflowError
Weighter.FULL_EQUAL_WEIGHTER.toString()       → StackOverflowError
Weighter.given(ImmutableMap.of()).hashCode()  → StackOverflowError
# après correctif (rawDefaultSub) :
FIXED-3a hashCode=29791
FIXED-3b Weighter{weights={}, subs={}, defaultSub=Optional.empty}
FIXED-3c HashSet<Aggregator> fonctionne                        # 38/38 tests OK

# B3 — Parametric, note pondératrice négative
multiplied=0.5, weighting=-0.5, other=0.5
  → IllegalArgumentException: All weights must be non-negative.

# B4 — exam.schema.json exige le réseau et échoue
ExamJsonConverter.examSchema().validate("Exam/Complex.json")
  → FileNotFoundException: https://raw.githubusercontent.com/.../assessment-tree.schema.json

# F1 — aucun appel à subs(Set) : la descente d'arbre n'existe pas
$ grep -rn "subs(" --include=*.java src/main | grep -v "public\|private\|protected"
# (aucun appel à subs(Set<Criterion>) ; uniquement subs() sans argument)

# F2 — Exam n'est utilisé nulle part
$ grep -rn "Exam\b" --include=*.java src | grep -v "^src/main/.../exam"
# (aucun résultat)
```

**Note :** les correctifs BL1, B1 et B2 ont été prototypés puis **retirés**. Le dépôt a été restauré à son état d'origine (`git status` propre) ; ce rapport est la seule modification.

---

## Conclusion

Le projet a de vraies qualités d'ingénierie — l'architecture `sealed`/immuable est propre, et `WeightedMarks` est une idée qui mérite d'être défendue publiquement. Mais entre son état actuel et une soumission, il y a trois marches de hauteur très inégale.

Les deux premières sont **faciles et non négociables** : un projet qui ne compile pas chez le relecteur (BL1), qui calcule des notes fausses (B1) et sans licence (P1) sera rejeté avant même d'être lu. Une journée de travail suffit à les franchir, et les correctifs sont déjà vérifiés dans ce rapport.

La troisième est **le vrai travail** : la bibliothèque ne sait pas encore faire ce que son titre annonce (F1), et l'agrégation des commentaires (F3) — probablement son apport le plus original — n'existe pas. Aucune quantité de polissage ne compensera cette absence.

Enfin, un avertissement sur le fond : même complet et correct, le projet sera rejeté sans **positionnement par rapport à l'état de l'art** (P6) et sans **validation empirique** sur un cas réel (P9). Les OWA sont une théorie établie depuis 1988 ; la question à laquelle l'article doit répondre n'est pas « le code marche-t-il ? » mais « qu'apporte JGrapa que Moodle, un tableur ou une bibliothèque MCDA existante n'apportent pas ? ». Le meilleur candidat pour cette réponse est déjà présent dans le code : **une note explicable et auditable**, via des poids effectifs exposés plutôt qu'un scalaire opaque. C'est cet axe qu'il faut construire, tester et démontrer.

---

## Synthèse — état du code 



Six problèmes concrets subsistent, de natures très différentes :

| | Problème | Nature | Effort |
|---|---|---|---|
| **BL1** | `mvn clean test` échoue (`Source option 5 is no longer supported`) : le projet ne compile pas depuis un clone. C'est une **configuration Maven, pas le code** — le POM parent fixe `release=21` mais pas la version du compiler-plugin, et Maven 3.8 retombe alors sur la 3.1, antérieure au paramètre `release`, qu'elle ignore. Invisible en local parce qu'Eclipse a déjà rempli `target/`. | build | 1 ligne |
| **B1** | `Weighter` fausse les notes : la masse complémentaire est comptée au dénominateur de normalisation sans être attribuée à aucun critère. Poids `{a:0.25, b:0.25}` → un étudiant parfait obtient **0,5 au lieu de 1**. | calcul | 1 ligne |
| **B2** | `hashCode()` et `toString()` lèvent `StackOverflowError` sur tout agrégateur sans `defaultSub` explicite. Cause : `equals`/`hashCode`/`toString` passent par le getter public `defaultSub()`, qui résout sur `FULL_EQUAL_WEIGHTER` — lequel est auto-référentiel — au lieu de lire le champ privé. **La conception est bonne ; c'est son application interne qui est incomplète**, et le correctif la préserve intégralement. | correction | ~10 lignes |
| **B3** | `Parametric` plante sur une note pondératrice négative : une note (`[-1, 1]`) y est utilisée directement comme poids (`[0, +∞[`). | sémantique | à trancher |
| **B4** | `exam.schema.json` est non fonctionnel : ses références ne sont pas mappées vers le classpath (il tente un accès réseau, qui échoue), et son `required: ["aggregator"]` n'est satisfait par aucun exemple. Aucun test ne le couvre ; tout le package `exam` est du code mort. | schéma | ~1 h |
| **F1** | **L'agrégation d'arbre n'existe pas.** `subs(Set)` n'est appelé nulle part, et rien ne combine un `AssessmentTree` avec un `Aggregator` : la bibliothèque ne sait pas encore calculer la note d'un étudiant. L'agrégation des commentaires (`feedback`) manque également. C'est le travail restant — et c'est la contribution à publier. | fonctionnel | le vrai chantier |

Trois remarques pour cadrer la discussion. **BL1 et B1 se corrigent en deux lignes** et les correctifs sont vérifiés (les 38 tests passent). **B2 demande d'abord une décision de conception** : « rien de spécifié » et « pondérateur égal explicite » désignent-ils le même agrégateur ? Si oui — ce que suggère le nom du commit `babc792` « present default agg » — il faut canoniser dans le constructeur d'`Aggregator` (ce que `Owa.java:38` fait déjà, mais seul dans son coin) ; corriger `hashCode()` isolément romprait le contrat `equals`/`hashCode` et échangerait un crash bruyant contre une perte silencieuse d'entrées en `HashMap`. Enfin, et c'est le signal le plus important : **aucun de ces bugs n'est détecté par la suite de tests actuelle** — ils survivent précisément parce que les tests ne les touchent jamais (`hashCode()` n'est jamais appelé sur un agrégateur ; aucun test n'utilise des poids explicites couvrant tous les critères et sommant à moins de 1).
