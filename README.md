# ComputerStoreJava
# ComputerStoreJava

## Overview
ComputerStoreJava is a modular Java application intended to model the domain of a computer store. The project is organised into separate modules for the data-access layer (`DAO`), the service layer (`Service`), and the domain model (`model`). Each module can be developed and tested independently while sharing the same IntelliJ IDEA project (`TamarAndZehaviProject.iml`).

Although the repository currently includes only template code in `Java/src/Main.java`, the folder structure is ready for implementing a multi-layered application. Use this project as a starting point for experimenting with clean architecture patterns, DAO abstractions, and service orchestration around a computer store scenario (for example, managing inventory, customers, and orders).

## Repository Structure
```
Java/
├── DAO/          # Data-access module (IntelliJ module definition in `DAO.iml`)
├── Service/      # Service layer module (`Service.iml`)
├── model/        # Domain model module (`model.iml`)
├── src/          # Stand-alone module entry point (`Main.java` template)
└── TamarAndZehaviProject.iml
```

## Getting Started
1. **Install a JDK** – Java 17 or later is recommended.
2. **Open in IntelliJ IDEA** – Import the project via `TamarAndZehaviProject.iml` to keep the existing module configuration.
3. **Build & Run** – Use IntelliJ's build tools or run `Main.java` directly. Replace the current template logic with your application code as you develop the store features.

## Next Steps
- Define the domain entities inside the `model` module (e.g., `Computer`, `Customer`, `Order`).
- Implement DAO classes inside the `DAO` module to handle persistence.
- Add business logic and orchestration in the `Service` module.
- Update `Java/src/Main.java` to bootstrap the application, wire dependencies, and expose user interaction (CLI, REST API, etc.).
