# Images des cartes Google Wallet / Apple Wallet

Placez ici les visuels de la carte de membre, sous ces noms **fixes** (pas de configuration
possible — voir le README principal). Contrairement à `data/` (secrets, base de données —
jamais versionné), ce dossier est fait pour être commité avec le repo.

| Fichier      | Utilisé par    | Dimensions conseillées | Rôle                                       |
|--------------|----------------|-------------------------|---------------------------------------------|
| `icon.png`   | Apple          | ≥ 87×87 px, carré       | Notifications / écran verrouillé            |
| `logo.png`   | Apple + Google | ~160×50 px              | Logo affiché sur la face de la carte        |
| `banner.png` | Apple + Google | ~375×123 px             | Bannière en haut de la carte                |

Aucune référence à ajouter dans `data/config.yml` : ces trois chemins sont câblés en dur dans le
code. Les fichiers manquants sont ignorés silencieusement (repli sur un simple carré de couleur
pour l'icône Apple, ou simplement absence de logo/bannière).

**Pour Google Wallet**, ces fichiers ne sont utilisables que si l'app est réellement exposée
publiquement : Google charge les images depuis ses propres serveurs via une URL HTTPS, construite
automatiquement à partir de `app.public-base-url` + ce nom de fichier fixe. Sans `public-base-url`
renseignée, Google Wallet fonctionne quand même mais sans logo ni bannière. Apple n'a pas cette
contrainte : les images sont embarquées directement dans le fichier `.pkpass`.
