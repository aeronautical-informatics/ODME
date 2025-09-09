# ODME Developer Guide

This guide is for developers working directly on the **ODME repository** (not forks).

It explains how to set up the project, follow safe workflows, and ensure the CI/CD pipeline continues to run smoothly.

---

## Getting Started

### 1. Clone the Repository
git clone https://github.com/aeronautical-informatics/ODME.git

cd ODME

### 2. Build the Project
We use Maven:

mvn clean install

This creates a runnable JAR file inside the target/ folder.

### 3. Run the Application
java -jar target/SESEditor-1.0-SNAPSHOT-shaded.jar

if it doesnt work, run following command

mvn clean package shade:shade

this will run explicitly shade plugin

## Working with the Repository
Always Sync Before Starting Work

git checkout main

git pull origin main

## Create a Feature Branch
git checkout -b your_branch

Never commit directly to main. Work in a separate branch(your own issue specific branch).

## Commit Changes
git add . OR specific files that you want to add

git commit -m "Describe your changes clearly"

## Merge Latest Main Before Pushing
git fetch origin

git merge origin/main


# Very Important: Fix any conflicts locally before pushing.
after analyzing and fixing of any new update related issues

Push Your Branch

git push origin Your_branch


Then create a Pull Request (PR) on GitHub.

CI/CD will run automatically on your PR

## Protecting the CI/CD Pipeline

Some files are critical for the project’s automated builds and workflows:

.github/workflows/* (CI/CD pipelines)

pom.xml (Maven build setup)

Dockerfile

.gitignore

## Rules:

Do not overwrite or delete these files.

If your change affects them, explain the reason clearly in your PR.

Test your build locally with mvn clean install before pushing.

## Best Practices

Use clear, meaningful commit messages.

Test everything locally before pushing.

Keep pull requests small and focused.

Discuss large changes with the team before implementation.

Avoid committing build artifacts (e.g., target/ directory).

Don’t touch pipeline files unless discussed.

## More Info

Project overview → see README.md.

Developer workflow → see this guide (DEVELOPER_GUIDE.md).

For questions, coordinate with the maintainer or open an issue.

Welcome to the ODME developer team!
Let’s keep the project stable, collaborative, and easy to maintain.
