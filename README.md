# Operation Domain Modeling Environment (ODME)

### Table of Contents

* [Overview](#Overview)
* [Workflow](#workflow)
* [Getting Started](#getting-started)
* [Usage](#usage)
* [Contributing](#contributing)
* [License](#license)

## [Overview](#Overview)

The **Operation Domain Modeling Environment (ODME)** is a powerful modeling tool developed for creating and refining operation domain models. It consists of two main components: **Domain Modeling Editor** and **Scenario Modelling Editor**. ODME provides an intuitive graphical user interface for creating, visualizing, and validating domain models while supporting various modeling scenarios.

**Domain Modeling Editor** has been developed as a modeling environment. The graphical user interface is designed in such a way that the user can draw graphs on the screen almost as one would on paper. The vertices or elements and edges can be drawn by clicking and dragging the mouse. Elements icons are added in the toolbox for easy access. Also, nodes and edges can be easily moved to any position. The drawing panel is synchronized with the bottom left side tree. Element addition in one place either in the white drawing panel or in the left tree, it will be added in both the sections automatically. Variable can be attached with the nodes. And attached variables are shown in the variable table on the right top corner during any node selection from either of the trees. Also, a constraint can be added to the aspect node to restrict the choices of entities. Constraint is written using XPath query language. Like variable, constraint is also visible on the constraint table on the right side of the editor during aspect node selection. Domain Modeling Editor allows saving part of the designed model as a module for future use. That saved module can be reused in any project later. It has also the facility to validate the created model against axioms using predefined XML Schema. Validation result is displayed in the console window and for valid model XML and Schema is displayed in the bottom right display window. The editor’s export and import options increased the share ability of the designed models among researchers. The newly created or opened project name is displayed in the top left corner of the editor.

**Scenario Modelling Editor** has been developed as an interactive pruning tool. It also has a variable table for displaying variables or editing values of variables. Also, constraint table and console window work exactly the same way. Here also left side tree is synchronized with the white drawing panel. Nodes are also movable here. But here a new project can’t be created. Also, new elements can’t be added to the model in Scenario Modelling. Then, the main functionality of Scenario Modelling is pruning Domain Models to create pruned entity structure. After completing pruning of the created domain metamodel, executable scenario can be exported for target simulation environment by using a project-specific XSLT file.

**Behavior Modelling Editor** has been developed as a Behavior Tree editor for the available functions defined for each entity. The user can build a behavior tree and then export it as XML and merge it with the existing scenario file. *This mode is a work in progress*

**Operational Design Domain** has been developed as an ODD editor. The user can generate Operational Domain(OD) from the domain model and using that as the base can edit and save multiple ODDs using the ODD manager. *This mode is a work in progress*

**Scenario Manager** has been developed to manage the existing scenario and assign risk factors to them. Future work involves allocating build scripts to each scenario and having a feedback loop from the simulator execution. *This mode is a work in progress*

**Import Operational Design Domain Plugin** has been developed to import Operational scenarios from Cameo and transform them to operational domain model into ODME. *This mode is a work in progress*

**Scripting Add-On** has been developed to bridge the gap between scenario modeling and simulation execution within ODME. It allows users to seamlessly translate XML and YAML-based scenarios into executable Python scripts, enabling real-time validation and integration with external simulation environments.  *This mode is a work in progress*


## [Workflow](#workflow)

This repository uses GitHub Actions to automate the build, testing, packaging, and release of the ODME project. The workflow consists of several jobs that run whenever changes are pushed or pull requests are made to the **"main"** branch. Here's an overview of the workflow steps:
1. **Build and Test** : The build_test job compiles the Java code using Maven and runs tests to ensure code integrity.

2. **Publish Artifact** : The publish-job job verifies the project, stages the built artifact, and uploads it as a package.

3. **Automate Release** : The automate-release job creates a GitHub release based on the uploaded artifact, providing a versioned snapshot of the project.

This workflow streamlines the development process, ensuring code quality, artifact packaging, and release management.

## [Getting Started](#getting-started)

To get started with ODME, follow these steps:

1. Clone this repository to your local machine.
2. Install the required dependencies, including Java and Maven.
3. Use the provided editors to create and refine domain models.
4. Leverage the workflow for automated build, testing, packaging, and release (The provided GitHub Actions workflow automates the development process. It builds the project, packages artifacts, and releases new versions based on changes to the main branch.)
5. Also please read [Developer Guide](./DEVELOPER_GUIDE.md) before starting

## [Usage](#usage)

1. Download the package from Release and extract the contents
2. Execute the *SESEditor-1.0-SNAPSHOT-jar-with-dependencies* 


## [Contributing](#contributing)

Contributions to ODME are welcome!

## [License](#license)

ODME is released under the **MIT License**.  


ODME is released under the **MIT License**.
