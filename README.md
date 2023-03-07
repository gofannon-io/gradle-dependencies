# Gestion de la cohérence des versions des dépendances dans le contexte d'une application multi-projets sous Gradle

## Problématiques

### Problématique 1 : la cohérence des versions 

Dans le cadre d'une application composée de différents projets, la gestion des versions des dépendances (externes et internes) 
se complexifient beaucoup.
Ainsi, il devient difficile de maintenir la cohérence des versions entre les applications.

Plus concrètement, prenons une application constituée de 10 projets dont tous dépendent d'une librairie externe 
Library1 en version 1.0. 
Au début, chaque projet va bien dépendre de la version 1.0.
Le build de l'applicatif va donc bien générer une application contenant une seule version de Library1, la 1.0.

Cependant, au fûr et à mesure des changements de versions de la librairie dans les projets, des erreurs vont se produirent,
et le build de l'applicatif va générer une application contenant plusieurs versions de la Library1.
L'effet sera l'apparition de problèmes lors de l'exécution de l'application. 

Il faut donc mettre en place un référentiel cohérent et unique contenant l'ensemble des dépendances et de leurs versions.
Ce référentiel sera utilisé par chaque projet de l'application pour obtenir la version de chaque librairie qu'ils 
souhaitent utiliser.


### Problématique 2 : la distinction entre le build et les dépendances

Un script *build.gradle* contient les instructions nécessaires au build, mais il contient également les instructions de 
gestion des dépendances et de leurs versions.
Cela est parfois considéré comme un mélange de genre.
En effet, lors d'un parcours rapide de l'historique d'un fichier *build.gradle*, il est nécessaire d'examiner attentivement
le log pour savoir s'il s'agit d'une modification du process de build ou de celui des dépendances.


Pour éviter cela, il faut stocker la partie concernant la gestion des dépendances et leurs versions dans un fichier 
séparé de *build.gradle*.

Note : la gestion des dépendances comprend la liste des dépendances directes et leurs versions, leurs périmètres d'usage,
la résolution de leurs dépendances.


## Objectifs de l'étude

Les objectifs de l'étude sont les suivants : 

* [OBJ-1] Mettre toutes les dépendances versionnées des projets d'une application dans un composant externe (librairie, plugin) .
* [OBJ-2] Au sein d'un projet, séparer les instructions de gestion des dépendances et celles relatives au build du projet.

L'étude se basera sur la documentation officielle de https://docs.gradle.org/current/userguide. 


## Réalisation de l'étude

### Configuration REF : Situation de référence

Le code de cette configuration est disponible dans [conf-ref](./conf-ref).

Cette configuration comporte le code initial avec une approche standard, c'est-à-dire avec la déclaration des dépendances 
et leurs versions dans chaque *build.gradle* de chaque projet.



### Configuration A : séparation des versions de dépendances

Cette première étape consiste à mettre en œuvre le mécanisme de version-catalog [S2].
Le *version-catalog* permet de placer dans un fichier *libs.versions.toml* les versions des dépendances directes (ie pas 
celles tirées par transitivité).
Ce fichier contient les sections suivantes :
* **versions** : elle contient une liste de versions 
* **libraries** : elle contient une liste de libraries dont les versions peuvent être référencées dans la section **versions**.
* **bundles** : elle contient une liste de bundles ou "paquets", chacun regroupant un ensemble de librairies.
* **plugin** : elle contient une liste de plugins dont les versions peuvent être référencées dans la section **versions**.

Par exemple :
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

En plaçant le fichier *libs.versions.toml* dans le répertoire *gradle/*, gradle le prendra en compte automatiquement.
Il existe d'autres moyens pour le placer ailleurs, mais cela sort du périmètre de l'étude. 


Dans le fichier *build.gradle*, il faut, dans la section *dependencies*, référencer directement le contenu du fichier 
*libs.versions.toml*.
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

Cette approche permet de remplir partiellement l'objectif [OBJ-2]. 
En effet, les dépendances de premier niveau sont bien mises dans un fichier distinct de *build.gradle*.
Cependant la gestion de la transitivité des dépendances reste configurée dans *build.gradle*.

Le code de cette configuration est disponible dans [conf-a](./conf-a)


### Configuration B : commonalisation des dépendances de premier niveau

La documentation de Gradle indique qu'il est possible de placer le *version-catalog* [S3].
Pour cela, il faut créer un nouveau projet, *vermgt-plugin* qui intègrera le contenu du fichier *libs.versions.toml* au 
sein du fichier *build.gradle*.
Son fichier *build.gradle* est :
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

Puis, dans chaque projet de l'application, il faut supprimer le fichier *libs.versions.toml* et ajouter dans le fichier 
*settings.gradle* la dépendance vers le *vermgt-plugin*.
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

Cette approche complémentaire à conf-a remplit pleinement l'objectif [OBJ-2]: toutes les dépendances directes sont 
stockées dans un plugin.

Le code de cette configuration est disponible dans [conf-b](./conf-b)



### Configuration C : séparation de la gestion de la transitivité des versions

L'approche par *version-catalog* permet uniquement de gérer les dépendances directes.
Pour la gestion des dépendances transitives, il faut utiliser le mécanisme de *java-platform* [S4].
Ce mécanisme consiste à créer un projet contenant la gestion des dépendances.
Ce projet sera à référencer par les autres comme une dépendance.

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


Cette approche ajoute une dépendance supplémentaire vers le projet *version-platform* qui sera en charge de la gestion 
des dépendances par transitivité. 
L'inconvénient de cette approche est la gestion des dépendances au travers de deux projets distincts.


Le code de cette configuration est disponible dans [conf-c](./conf-c)



### Configuration D : fusion des projets de gestion des dépendances

L'objectif de cette approche est de placer, dans un même projet, le *version-catalog* et le *java-platform*, afin de 
centraliser la gestion des dépendances.
Ce projet sera appelé *dependencies-plugin*.

Le fichier *build.gradle* combine les plugins *version-catalog* et *java-platform* et leurs configurations :
```groovy
plugins {
    id 'version-catalog'
    id 'java-platform'
    id 'maven-publish'
}

catalog {
  // declare the aliases, bundles and versions in this block
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

//[...]
```

Pour les projets de l'application doivent déclarer le *version-catalog* dans le fichier *settings.gradle* puis déclarer 
les dépendances dont la *java-platform*.

Le fichier *settings.gradle* du projet *copyrighter* est le suivant : 
```groovy
dependencyResolutionManagement {
  repositories {
    mavenLocal()
    mavenCentral()
  }

  versionCatalogs {
    libs {
      from("io.gofannon.gullfoss:dependencies-plugin:5.0")
    }
  }
}
```

Le fichier *build.gradle* du projet *copyrighter* est le suivant :
```groovy
plugins {
    id 'java-library'
    id 'maven-publish'
}

dependencies {
  api platform(libs.version.platform)

  implementation libs.annotations

  testRuntimeOnly libs.bundles.testRuntime

  testImplementation libs.bundles.testImplementation
}

//[...]
```

Cette approche combine les avantages de la précédente en un seul projet et allège le code dans le projet.


Le code de cette configuration est disponible dans [conf-d](./conf-d)


## Sources

* [S1] Sharing dependency versions between projects
  * https://docs.gradle.org/current/userguide/platforms.html
* [S2] Using a version catalog
  * https://docs.gradle.org/current/userguide/platforms.html#sub:version-catalog
* [S3] The version catalog plugin
  * https://docs.gradle.org/current/userguide/platforms.html#sec:version-catalog-plugin
* [S4] Using a platform to control transitive versions
  * https://docs.gradle.org/current/userguide/platforms.html#sub:using-platform-to-control-transitive-deps
