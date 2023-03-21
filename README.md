# Etude des outils de gestion des dépendances dans Gradle

## Introduction
La gestion des dépendances est un point crucial pour la construction d'une application.
Une application doit embarquer tous les artefacts dont elle a besoin et avec la version la plus adaptée.

La détermination de ces artefacts et leurs versions est de la responsabilité des outils de build (Maven, Gradle, ...).
Celui-ci, à partir des dépendances de premier niveau, détermine les dépendances tirées par transitivité et leurs versions, en parcourant successivement chaque niveau de dépendance. 

Dans le cadre d'une application composée de multiples projets, la gestion des dépendances (externes et internes) se complexifie beaucoup et plusieurs problématiques apparaissent.
Ces problématiques ne sont pas gérables manuellement et nécessitent une automatisation qui est fournie par l'outil de build. 

Cette étude présente les problématiques liées à la gestion des dépendances, la fixation des objectifs à atteindre et la réalisation d'essais avec les outils Gradle.
Cette étude se termine par une synthèse d'utilisation de chaque outil mis à disposition par Gradle




## Problématiques liés à la gestion des dépendances

### Problématique 1 : la cohérence des versions
Lors de la détermination des dépendances, il s'avère régulièrement que des artefacts soient tirés plusieurs fois et avec des versions différentes.
Il est nécessaire de déterminer quelle serait la version la plus adéquate pour le bon fonctionnement de l'application. Et, le cas échéant, s'il faut interrompre le build quand aucune version satisfaisante n'est trouvée.


### Problématique 2 : la distinction entre le build et la gestion des dépendances
Par défaut, un script *build.gradle* contient les instructions nécessaires au build, mais aussi les informations et les instructions nécessaires à la gestion des dépendances, à savoir :
* (a) les dépendances de premier niveau, c'est-à-dire des artefacts et leurs versions
* (b) les instructions pour déterminer les dépendances transitives et leurs versions.

Dans un contexte multi-projets, il peut être intéressant de séparer les instructions (b) afin de les partager entre tous les projets.
Il est également intéressant de partager entre tous les projets les dépendances autorisées (ie. ses artefacts et leurs versions).

Un dernier point, certains préfèrent externaliser les informations les versions des artefacts de premier niveau du fichier *build.gradle*.
Cela éviterait de potentielles confusions lors de la lecture de log de gestions de configurations (git, subversion,...)

Pour éviter cela, il faut stocker les instructions et les dépendances (artefacts + versions) dans des composants (artefacts) externes distincts des fichiers *build.gradle* des projets.
Il est à noter que *build.gradle* contiendra toujours la liste des dépendances (artefacts) mais sans versions.


### Problématique 3 : extensibilité
Il n'est pas toujours pratique de placer toutes les dépendances dans un unique référentiel (ie artefact).
Par exemple, il peut être pratique de distinguer le référentiel lié aux dépendances externes et celui lié aux dépendances internes, ou de spécialiser certains référentiels (ex: un référentiel de dépendances de test, de framework d'IoC, d'ORM).




## Objectifs de l'étude
L'enjeu général de cet article est l'analyse des différents outils de gestion des dépendances disponibles dans Gradle.
Pour focaliser cette analyze, les objectifs suivants ont été retenus :
* [OBJ-1] Mettre toutes les dépendances versionnées des projets d'une application dans un composant externe (librairie, plugin) .
* [OBJ-2] Au sein d'un projet, séparer les instructions de gestion des dépendances, des dépendances (artefacts + versions) et du reste du build du projet.
* [OBJ-3] Pouvoir étendre les instructions de gestion des dépendances et celles relatives au build. 

L'étude se basera sur la documentation officielle de https://docs.gradle.org/current/userguide. 


## Réalisation de l'étude

### Configuration REF : Situation de référence
Cette configuration comporte le code initial avec une approche standard, c'est-à-dire avec la déclaration des dépendances, leurs versions et la gestion des dépendances transitives dans chaque *build.gradle* de chaque projet.

Le code de cette configuration est disponible dans [conf-ref](./conf-ref).



### Configuration A : séparation des versions de dépendances

#### Objectif
Cette première étape consiste à mettre en œuvre le mécanisme de *version-catalog* [S2].
Le *version-catalog* permet de placer dans un fichier *libs.versions.toml* les versions des dépendances directes (ie pas celles tirées par transitivité).
Ce fichier contient les sections suivantes :
* **versions** : elle contient une liste de versions
* **libraries** : elle contient une liste de libraries dont les versions peuvent être référencées dans la section **versions**.
* **bundles** : elle contient une liste de bundles ou "paquets", chacun regroupant un ensemble de librairies.
* **plugin** : elle contient une liste de plugins dont les versions peuvent être référencées dans la section **versions**.

#### Implémentation
Il faut ajouter le fichier *libs.versions.toml* dans le projet.
Dans le cadre du projet, le contenu du fichier est :
```toml
[versions]
junit = "5.9.2"
assertj = "3.24.2"
annotations="24.0.0"
copyrighter="2.0"
personProvider = "2.0"
translator = "2.0"

[libraries]
copyrighter = {module="io.gofannon.gullfoss:copyrighter", version.ref="copyrighter"}
person-provider = {module="io.gofannon.gullfoss:person-provider", version.ref="personProvider"}
translator = {module="io.gofannon.gullfoss:translator", version.ref="translator"}
annotations = { module = "org.jetbrains:annotations", version.ref = "annotations"}
junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit"}
junit-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit"}
junit-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit"}
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj"}

[bundles]
testImplementation = ["junit-api", "junit-params", "assertj-core"]
testRuntime = ["junit-engine"]

[plugins]
```

En plaçant le fichier *libs.versions.toml* dans le répertoire *gradle/*, Gradle le prendra en compte automatiquement.
Il existe d'autres moyens pour le placer ailleurs, mais cela sort du périmètre de l'étude. 


Dans le fichier *build.gradle*, il faut, dans la section *dependencies*, référencer directement le contenu du fichier *libs.versions.toml*.
Par exemple :
```groovy
dependencies {
    implementation libs.copyrighter
    implementation libs.annotations

    testRuntimeOnly libs.bundles.testRuntime

    testImplementation libs.bundles.testImplementation
}
```

Dans cet exemple, deux mécanismes de références sont utilisés :
Le premier mécanisme est la référence à une librairie. Ainsi l'instruction ```implementation libs.copyrighter``` est 
équivalente à ```implementation io.gofannon.gullfoss:copyrighter:2.0```.

Le second mécanisme est la référence à un bundle. Ainsi l'instruction 
```testImplementation libs.bundles.testImplementation``` est équivalente à :
```groovy
    testImplementation "org.assertj:assertj-core:3.24.2"
    testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.2'
    testImplementation 'org.junit.jupiter:junit-jupiter-params:5.9.2'
```

#### Conclusion
Cette approche remplit partiellement l'objectif [OBJ-2] : les dépendances de premier niveau sont bien mises dans un fichier distinct de *build.gradle*.
La gestion de la transitivité des dépendances reste configurée dans *build.gradle*.

Le code de cette configuration est disponible dans [conf-a](./conf-a)




### Configuration B : communalisation des dépendances de premier niveau
#### Objectif
La documentation de Gradle indique qu'il est possible de placer le concept de *version-catalog* [S3] dans un artefact à 
part qui sera partagé par tous les projets.

#### Implémentation
Pour externaliser le *version-catalog*, il faut créer un nouveau projet, *vermgt-plugin* qui intègrera le contenu du fichier *libs.versions.toml* au sein du fichier *build.gradle*.
Le fichier *build.gradle* est :
```groovy
catalog {
    versionCatalog {
        version("junit", "5.9.2")
        version("annotations", "24.0.0")
        version("assertj", "3.24.2")
        version("copyrighter", "3.0")
        version("person-provider", "3.0")
        version("translator", "3.0")

        library("copyrighter", "io.gofannon.gullfoss", "copyrighter").versionRef("copyrighter")
        library("person-provider", "io.gofannon.gullfoss", "person-provider").versionRef("person-provider")
        library("translator", "io.gofannon.gullfoss", "translator").versionRef("translator")

        library("annotations", "org.jetbrains", "annotations").versionRef("annotations")

        library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
        library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
        library("junit-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit")
        library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")

        bundle("testImplementation", ["junit-api", "junit-params", "assertj-core"])
        bundle("testRuntime", ["junit-engine"])
    }
}
```
Ce fichier contient le contenu du fichier *libs.versions.toml*.

Puis, dans chaque projet de l'application, il faut supprimer le fichier *libs.versions.toml* et ajouter dans le fichier *settings.gradle* la dépendance vers le *vermgt-plugin*.
```gradle
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    versionCatalogs {
        libs {
            from("io.gofannon.gullfoss:vermgt-plugin:3.0")
        }
    }
}
```

La production Maven du projet *vermgt-plugin* est constituée de :
* d'un *pom.xml* décrivant le projet avec un packaging *toml*
* le fichier *toml* du projet.

Le contenu du *pom.xml* est le suivant :
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.gofannon.gullfoss</groupId>
  <artifactId>vermgt-plugin</artifactId>
  <version>3.0</version>
  <packaging>toml</packaging>
</project>
```

#### Conclusion
Cette approche remplit :
* partiellement l'objectif [OBJ-1] : seules les dépendances directes sont externalisées du fichier *build.gradle*.
* partiellement l'objectif [OBJ-2] : toutes les dépendances directes sont mises dans un artefact

Le code de cette configuration est disponible dans [conf-b](./conf-b)



### Configuration C : séparation de la gestion de la transitivité des versions

#### Objectif
L'approche par *version-catalog* permet uniquement de gérer les dépendances directes.
Pour la gestion des dépendances transitives, il faut utiliser le mécanisme de *java-platform* [S4].
Ce mécanisme consiste à créer un projet contenant la gestion des dépendances.
Ce projet sera à référencer par les autres comme une dépendance.


#### Implémentation
Il faut créer le projet *java-platform*, dans lequel on trouvera :
* la déclaration de dépendance vers le *version-catalog* dans le fichier *settings.gradle*.
* la déclaration des contraintes et de gestion des dépendances transitives dans la section *dependencies*/*constraints* du fichier *build.gradle*.

Le *settings.gradle* est le suivant :
```groovy
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }

    versionCatalogs {
        libs {
            from("io.gofannon.gullfoss:vermgt-plugin:4.0")
        }
    }
}
```

Le *build.gradle* est le suivant :
```groovy
plugins {
    id 'java-platform'
    id 'maven-publish'
}

dependencies {
  constraints {
    api(libs.copyrighter)
    api(libs.annotations)

    api(libs.assertj.core)
    api(libs.bundles.testImplementation)

    runtime(libs.bundles.testRuntime)
  }
}
//[...]
```


Pour les projets, il faut :
* déclarer la dépendance vers le *version-catalog* dans le fichier *settings.gradle*. (identique au précédent exemple)
* déclarer la dépendance vers le *java-platform* dans la section *dependencies* du fichier *build.gradle*

Par exemple, la section *dependencies* du *build.gradle* du projet *copyrighter* est le suivant :
```groovy
dependencies {
  api platform(libs.version.platform)

  implementation libs.annotations

  testRuntimeOnly libs.bundles.testRuntime

  testImplementation libs.bundles.testImplementation
}
//[...]
```

La production du *java-platform* est constituée d'un fichier *pom.xml* contenant les dépendances java.

#### Conclusion
Cette approche ajoute une dépendance supplémentaire vers le projet *version-platform* qui sera en charge de la gestion des dépendances par transitivité. 
L'inconvénient de cette approche est la gestion des dépendances au travers de deux projets distincts.

Cette approche remplit :
* pleinement l'objectif [OBJ-1] : les dépendances directes et la gestion de la transitivité sont externalisées du fichier *build.gradle*.
* pleinement l'objectif [OBJ-2] : les dépendances directes sont mises dans un artefact et la gestion de la transitivité dans un autre artefact.

Le code de cette configuration est disponible dans [conf-c](./conf-c)




### Configuration D : tentative de fusion des projets de gestion des dépendances
#### Objectif
L'objectif de cette approche est de placer, dans un même projet, le *version-catalog* et le *java-platform*, afin de centraliser la gestion des dépendances.

#### Implémentation
Le projet sera appelé *dependency-manager*.
Il devra contenir les *version-catalog* et le *java-platform*.

Le fichier *build.gradle* combine les plugins *version-catalog* et *java-platform* et leurs configurations :
```groovy
plugins {
    id 'version-catalog'
    id 'java-platform'
    id 'maven-publish'
}

catalog {
  versionCatalog {
    version("junit", "5.9.2")
    version("annotations", "24.0.0")
    version("assertj", "3.24.2")
    version("copyrighter", "4.0")
    version("person-provider", "5.0")
    version("translator", "5.0")
    version("platform", "5.0")

    library("version-platform", "io.gofannon.gullfoss", "version-platform").versionRef("platform")

    library("copyrighter", "io.gofannon.gullfoss", "copyrighter").versionRef("copyrighter")
    library("person-provider", "io.gofannon.gullfoss", "person-provider").versionRef("person-provider")
    library("translator", "io.gofannon.gullfoss", "translator").versionRef("translator")

    library("annotations", "org.jetbrains", "annotations").versionRef("annotations")

    library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
    library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
    library("junit-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit")
    library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")

    bundle("testImplementation", ["junit-api", "junit-params", "assertj-core"])
    bundle("testRuntime", ["junit-engine"])
  }
}


dependencies {
  constraints {
    api(libs.copyrighter)
    api(libs.annotations)

    api(libs.assertj.core)
    api(libs.bundles.testImplementation)

    runtime(libs.bundles.testRuntime)
  }
}

publishing {
  publications {
    maven(MavenPublication) {
      from components.javaPlatform
    }
    maven2(MavenPublication) {
      from components.versionCatalog
    }
  }
}
//[...] 
```

Il faut noter la déclaration de publication de deux artefacts. 
Cependant, cela ne fonctionne pas.
L'exécution de Gradle retourne le message d'erreur suivant :
```shell
Build file '/home/gwen/repositories/gofannon.io/gradle-dependencies/conf-d/dependencies-plugin/build.gradle' line: 47

A problem occurred evaluating project ':dependencies-plugin'.
> Could not get unknown property 'libs' for object of type org.gradle.api.internal.artifacts.dsl.dependencies.DefaultDependencyConstraintHandler.
```

Cela signifie que les références *libs.xxx* ne sont pas reconnues. 
Gradle n'interprète pas dynamiquement le contenu de la section *catalog*.
La solution trouvée est de placer, dans le répertoire gradle, un fichier *libs.versions.toml*, reprenant à l'identique le contenu de la section *catalog*.

Fichier *gradle/libs.versions.toml*
```toml
[versions]
junit = "5.9.2"
assertj = "3.24.2"
annotations="24.0.0"
copyrighter="2.0"
personProvider = "2.0"
translator = "2.0"

[libraries]
copyrighter = {module="io.gofannon.gullfoss:copyrighter", version.ref="copyrighter"}
person-provider = {module="io.gofannon.gullfoss:person-provider", version.ref="personProvider"}
translator = {module="io.gofannon.gullfoss:translator", version.ref="translator"}
annotations = { module = "org.jetbrains:annotations[WARNING.md](conf-d%2FWARNING.md)", version.ref = "annotations"}
junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit"}
junit-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit"}
junit-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit"}
assertj-core = { module = "org.assertj:assertj-core", version.ref = "assertj"}

[bundles]
testImplementation = ["junit-api", "junit-params", "assertj-core"]
testRuntime = ["junit-engine"]
```

La commande```gradle build``` fonctionne, le problème précédent est résolu.

L'exécution de la commande ```gradle publishToMavenLocal``` va provoquer le message suivant :
```shell
Multiple publications with coordinates 'io.gofannon.gullfoss:dependency-manager:5.0' are published to repository 'mavenLocal'. The publications 'catalog' in project ':dependency-manager' and 'platform' in project ':dependency-manager' will overwrite each other!
```
Il faudrait utiliser la notion de classifier pour distinguer les deux productions.
Mais cela n'a pas été possible rapidement et nécessiterait une étude dédiée.

Néanmoins, même avec l'usage du classifier, cette approche pose un problème important : le contenu du *version-catalog* est dupliqué.
Ainsi, tout changement autour des versions nécessitera la duplication des informations dans le fichier *libs.versions.toml* et le fichier *build.gradle*.

De ce fait, cette approche n'est pas viable


#### Conclusion
Cette approche n'est pas viable.

Le code de cette configuration est disponible dans [conf-d](./conf-d). 
Attention, ce code ne fonctionne pas car la publication de *version-catalog* et java-platform* s'écrasent mutuellement.



### Configuration E : extensibilité des artefacts

#### Objectif
L'objectif est d'étendre l'artefact *version-catalog* dans un autre artefact, et de faire de même pour l'artefact *java-platform*.

#### Implémentation de version-catalog
Gradle [S5] indique qu'il est possible d'étendre un *version-catalog* via l'instruction *amendLibs* dans les fichiers *settings.gradle*.
Dans le cas du projet en cours, cela ne convient pas, car l'idée est de faire un autre projet indépendant de type *version-catalog*.

La solution consiste à créer un nouveau projet, *test-version-manager*, contiendra toutes les versions des librairies liées aux tests.
Ce projet sera étendu par *vertmgt-plugin* qui ne contiendra plus que les librairies liées aux implémentations.

Le fichier *build.gradle* du projet *test-version-manager* est le suivant :
```groovy
//[...]

catalog {
  versionCatalog {
    version("junit", "5.9.2")
    version("assertj", "3.24.2")

    library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef("junit")
    library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef("junit")
    library("junit-params", "org.junit.jupiter", "junit-jupiter-params").versionRef("junit")
    library("assertj-core", "org.assertj", "assertj-core").versionRef("assertj")

    bundle("testImplementation", ["junit-api", "junit-params", "assertj-core"])
    bundle("testRuntime", ["junit-engine"])
  }
}

//[...]
```
Seules les versions des dépendances de tests sont conservées. 

Le fichier *build.gradle* du proget *vertmgt-plugin* est le suivant :
```groovy
catalog {
    versionCatalog {
            from("io.gofannon.gullfoss:test-version-manager:6.0")

            version("annotations", "24.0.0")
            version("copyrighter", "6.0")
            version("person-provider", "6.0")
            version("translator", "6.0")
            version("platform", "6.0")

            library("version-platform", "io.gofannon.gullfoss", "version-platform").versionRef("platform")
            
            library("copyrighter", "io.gofannon.gullfoss", "copyrighter").versionRef("copyrighter")
            library("person-provider", "io.gofannon.gullfoss", "person-provider").versionRef("person-provider")
            library("translator", "io.gofannon.gullfoss", "translator").versionRef("translator")
            library("annotations", "org.jetbrains", "annotations").versionRef("annotations")
    }
}
```
La dépendance est tirée par l'instruction *from* avec comme paramètre l'arterfact *test-version-manager*. 



#### Implémentation de java-platform
L'extension de *java-platform* s'effectue dans le fichier *build.gradle* via deux étapes&nbsp:
* Ajouter la directive *allowDependencies()* dans la section *javaPlatform*
* Déclarer le *java-platform* parent dans la directive *platform*

Dans le cas présent, l'usage du *java-platform* est combiné à l'usage du *version-catalog* *io.gofannon.gullfoss:vermgt-plugin*.

Le fichier contenu du *build.gradle* du projet *version-platform* est : 
```groovy
plugins {
    id 'java-platform'
    //[...]
}

javaPlatform {
  allowDependencies()
}

dependencies {
  constraints {
    api platform(libs.test.version.platform)
    api(libs.copyrighter)
    api(libs.annotations)
  }
}

//[...]
```
Ainsi le projet *version-platform* hérite du projet *test-version-platform*.

La production Maven générée est un *pom.xml* intégrant une dépendance vers le *pom.xml* du *java-platform* parent.


```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <modelVersion>4.0.0</modelVersion>
  <groupId>io.gofannon.gullfoss</groupId>
  <artifactId>version-platform</artifactId>
  <version>6.0</version>
  <packaging>pom</packaging>
  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>io.gofannon.gullfoss</groupId>
        <artifactId>test-version-platform</artifactId>
        <version>6.0</version>
      </dependency>
      <dependency>
        <groupId>io.gofannon.gullfoss</groupId>[deployment-1.puml](documentation%2Fdeployment-1.puml)
        <artifactId>copyrighter</artifactId>
        <version>6.0</version>
      </dependency>
      <dependency>
        <groupId>org.jetbrains</groupId>
        <artifactId>annotations</artifactId>
        <version>24.0.0</version>
      </dependency>
    </dependencies>
  </dependencyManagement>
</project>
```

#### Conclusion 
Cette approche remplit :
* pleinement l'objectif [OBJ-1] : les dépendances directes et la gestion de la transitivité sont externalisées du fichier *build.gradle*.
* pleinement l'objectif [OBJ-2] : les dépendances directes et la gestion de la transitivité sont placées dans d'autres artefacts
* pleinement l'objectif [OBJ-3] : les dépendances directes et la gestion de la transitivité supportent l'extensibilité.

Le code de cette configuration est disponible dans [conf-e](./conf-e/).


## Conclusion
Gradle propose deux concepts liés aux dépendances :
* le *version-catalog* qui contient des versions de premier niveau, des raccourcis vers des artefacts et des plugins, des bundles.
* le *java-platform* qui contient les versions des artefacts et la gestion de la transitivité.

### Usage du *version-catalog* seul
Dans un projet simple qui tire uniquement des dépendances premier niveau, cette solution est parfaite.
Elle ne peut pas satisfaire les projets nécessitant la gestion de la transitivité.

### Usage du *java-platform* seul
Ce concept est utile à tout type de projet, car il combine le versionement des dépendances de premier niveau et la gestion de la transitivité.
L'usage de ce concept est à privilégier par défaut du fait qu'il couvre les deux points essentiels de la gestion de dépendances.

### Usage conjoint de version-catalog et java-platform
L'avantage de l'usage conjoint de *version-catalog* et *java-platform* est la possibilité d'exploiter les concepts de bundle et de raccourcis (*libs.xxx*).
Le désavantage est la duplication des informations relatives aux dépendances de premier niveau dans deux artefacts et, la nécessité de synchroniser deux artefacts.

Cette approche est à envisager uniquement pour des projets incorporant de nombreux projets, du fait du coût de maintenance de deux artefacts synchronisés.




## Annexe

## Exemple de mise en œuvre *version-catalog* et *java-platform*
Le diagramme suivant présente une mise en œuvre d'une application dépendant de plusieurs librairies.
Un *version-catalog* et un *java-platform* sont mis en œuvre.

![Dépendances entre projets](http://www.plantuml.com/plantuml/proxy?cache=no&src=https://raw.githubusercontent.com/gofannon-io/gradle-dependencies/master/documentation/deployment-1.puml)

* Chaque librairie et application indique ses dépendances en se basant sur *version-catalog*.
* La mise en œuvre des instructions de résolution des dépendances de *java-platform* est pleinement utilisée à la construction de l'application pour déterminer les librairies et leurs versions qui seront inclus dans l'application finale.
* Les librairies utilisent les instructions de résolution des dépendances de *java-platform* au moment du build pour les tests.


## Sources
* [S1] Sharing dependency versions between projects
  * https://docs.gradle.org/current/userguide/platforms.html
* [S2] Using a version catalog
  * https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
* [S3] The version catalog plugin
  * https://docs.gradle.org/current/userguide/platforms.html#sec:version-catalog-plugin
* [S4] Using a platform to control transitive versions
  * https://docs.gradle.org/current/userguide/platforms.html#sub:using-platform-to-control-transitive-deps
* [S5] Overwriting catalog versions
  * https://docs.gradle.org/current/userguide/platforms.html#sec:overwriting-catalog-versions
