# FactionOutposts (Repo + GitHub Actions)

Upload this folder to a new GitHub repository and run the included workflow to get a compiled `.jar` as an artifact.

## Steps
1) Create a new empty GitHub repository.
2) Upload everything in this folder.
3) Go to **Actions** → **Build FactionOutposts** → **Run workflow**.
4) Download the artifact; inside is `FactionOutposts-1.2.0-reflection.jar`.

## Server
- Put in `plugins/`: the jar above, plus `Vault.jar`, `SaberFactions.jar`, *(optional)* `WorldEdit.jar`
- Enable Factions econ: `/f config econEnabled true`