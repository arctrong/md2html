# Development notes

## Building

<div class="tableGridded"></div>

Stage       | Build command                             | Artefact type | Artefact location
:-----------|:------------------------------------------|:--------------|:-----------------
Development | `mvn clean package`                       | Directory     | `target/md2html-<version>`
Release     | `mvn -Dassembly.format=zip clean package` | Zip-archive   | `target/md2html-<version>.zip`
