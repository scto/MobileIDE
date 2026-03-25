#!/bin/bash
sed -i 's/gradle-9.4.1-bin.zip/gradle-8.10.2-bin.zip/' gradle/wrapper/gradle-wrapper.properties
sed -i 's/agp    = "9.1.0"/agp    = "8.7.2"/' gradle/libs.versions.toml
sed -i 's/kotlin = "2.2.0"/kotlin = "2.0.21"/' gradle/libs.versions.toml
