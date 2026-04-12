Dossier « simulateur » (environnement isolé)
==========================================

En lançant le launcher avec la variable d’environnement SOLEA_TEST_MODE=1,
Electron enregistre toutes les données ici :

  test/electron-user-data/

Contenu typique :
  - account.json (session Microsoft de test)
  - launcher-settings.json
  - instances/paladium-mc/ (modpack téléchargé)

Le dossier electron-user-data/ est ignoré par Git : vous pouvez le supprimer
à tout moment pour repartir d’un profil vierge.

Commande (depuis la racine du projet) :

  npm run dev:test

Profil normal (données dans %AppData%) :

  npm run dev
