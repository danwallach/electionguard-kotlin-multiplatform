# ElectionGuard-Kotlin-Multiplatform

This is a fork from [https://github.com/votingworks/electionguard-kotlin-multiplatform]() aiming to provide a kotlin/js implementation for ballot encryption in order to be used in a web environment.

Therefore, we split the project in the following modules targeting different platforms:

| Module         | JVM | JS Node | JS Browser |
|----------------|-----|--------|------------|
| egklib-core    | ✅   | ✅      | ✅          |
| egklib-trustee | ✅   | ✅      | ✅          |
| egklib-encrypt | ✅   | ✅      | ✅          |
| egklib         | ✅   | ✅      | ❌          |
| egk-cli        | ✅   | ❌       | ❌          |

### TODO's
- Provide readline functionality in the `egk-cli` module for the JS Node platform.
- Investigate performance deficits on node and browser platforms.


## The original README follows:
_last update 01/21/2024_

ElectionGuard-Kotlin-Multiplatform (EGK) is a [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) 
implementation of 
[ElectionGuard](https://github.com/microsoft/electionguard), 
[version 2.0.0](https://github.com/microsoft/electionguard/releases/download/v2.0/EG_Spec_2_0.pdf), 
available under an MIT-style open source [License](LICENSE). 

Our GitHub repository is now hosted by [VotingWorks](https://www.voting.works/).

Currently we have ~88% LOC code coverage on the common and jvm core library (7024/7995 LOC). We are focusing on just 
the JVM implementation, and will consider native and other implementations in the future. 

Library dependencies are summarized [here](dependencies.txt).

Currently Java 17 is required.

*Table of contents*:
<!-- TOC -->
* [ElectionGuard-Kotlin-Multiplatform](#electionguard-kotlin-multiplatform)
  * [Getting Started](#getting-started)
  * [Workflow and Command Line Programs](#workflow-and-command-line-programs)
  * [Serialization](#serialization)
    * [JSON Serialization specification](#json-serialization-specification)
  * [Validation](#validation)
  * [Verification](#verification)
  * [Test Vectors](#test-vectors)
  * [Implementation Notes](#implementation-notes)
  * [Authors](#authors)
<!-- TOC -->

## Getting Started
* [Getting Started](docs/GettingStarted.md)

## Workflow and Command Line Programs
* [Workflow and Command Line Programs](docs/CommandLineInterface.md)
* [Encryption Workflow](docs/Encryption.md)
* [Pre-encryption Workflow](docs/Preencryption.md)


## Serialization

_We are waiting for the 2.0 JSON serialization specification from Microsoft, before finalizing our serialization. For now,
we are still mostly using the 1.9 serialization._

Support for [Protocol Buffers](https://en.wikipedia.org/wiki/Protocol_Buffers) has been moved to [this repo](https://github.com/JohnLCaron/egk-protobuf).

### JSON Serialization specification
* [JSON serialization 1.9](docs/JsonSerializationSpec1.9.md)
* [Election Record JSON directory and file layout](docs/ElectionRecordJson.md)

## Validation
* [Input Validation](docs/InputValidation.md)
* [Tally Validation](docs/TallyValidation.md)

## Verification
* [Verification](docs/Verification.md)

## Test Vectors
These are JSON files that give inputs and expected outputs for the purpose of testing interoperability between implementations.
* [Test Vectors](docs/TestVectors.md)

## Implementation Notes
* [Cryptography Notes](docs/CryptographyNotes.md)
* [Implementation Notes](docs/ImplementationNotes.md)

## Authors
- [John Caron](https://github.com/JohnLCaron)
- [Dan S. Wallach](https://www.cs.rice.edu/~dwallach/)