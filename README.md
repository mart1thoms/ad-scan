# ad-scan

## Branding des cartes (Apple Wallet + Google Wallet)

Le nom et la couleur de fond affichés sur les deux cartes viennent d'un seul et même bloc, partagé entre les deux, dans `data/config.yml` :

```yaml
app:
  wallet:
    name: "Mon BDE"
    background-color: "#2b3a55"
```

Les images, elles, ne se configurent pas : ce sont des fichiers à des chemins **fixes**, à
placer dans `branding/` à la racine du repo — c'est le seul endroit du repo pensé pour être
**versionné** (contrairement à `data/`, jamais commité). Un fichier manquant est simplement
ignoré (repli sur un carré de couleur pour l'icône, pas de logo/bannière sinon).

| Fichier               | Utilisé par    | Dimensions conseillées | Rôle                                 |
|------------------------|----------------|-------------------------|---------------------------------------|
| `branding/icon.png`   | Apple          | ≥ 87×87 px, carré       | Notifications / écran verrouillé      |
| `branding/logo.png`   | Apple + Google | ~160×50 px              | Logo affiché sur la face de la carte  |
| `branding/banner.png` | Apple + Google | ~375×123 px             | Bannière en haut de la carte          |

Détail par fichier dans [`branding/README.md`](branding/README.md).

Apple embarque directement ces fichiers dans le `.pkpass` généré : rien d'autre à faire. Google,
lui, charge les images depuis ses propres serveurs et a donc besoin d'une URL HTTPS **publique** —
c'est le seul rôle de `app.public-base-url` : si elle est renseignée, les URLs de `logo.png` et
`banner.png` s'en déduisent automatiquement (l'app sert déjà `branding/*` en HTTP, sans
authentification, à `/branding/nom-du-fichier`) :

```yaml
app:
  public-base-url: "https://ad-scan.mon-asso.fr"
```

Inutile si l'app ne tourne qu'en local/LAN — laissez `public-base-url` vide dans ce cas : Google
Wallet fonctionnera quand même, simplement sans logo ni bannière.

## Configuration Apple Wallet

Toutes les commandes ci-dessous s'exécutent **depuis le dossier `data/`** du projet, et produisent directement les fichiers aux noms attendus par `data/config.yml` (voir `data/config.yml.example`). Prérequis : avoir un compte Apple Developer.

```bash
cd data
```

### 1. Créer le Pass Type ID (une seule fois, sur le site Apple)

Sur [developer.apple.com/account](https://developer.apple.com/account) → **Certificates, Identifiers & Profiles → Identifiers → (+) → Pass Type IDs** → renseigner un identifiant, par ex. `pass.fr.reniti.adscan.membership`.

C'est la valeur à mettre dans `data/config.yml` → `apple-wallet.pass-type-identifier`.

Notez aussi votre **Team ID** (page **Account → Membership details**, 10 caractères) → `apple-wallet.team-identifier`.

### 2. Générer la clé privée et la CSR

```bash
openssl req -new -newkey rsa:2048 -nodes \
  -keyout apple-wallet-pass.key \
  -out apple-wallet-pass.csr \
  -subj "/CN=Mon BDE Pass Type ID"
```

### 3. Créer le certificat sur Apple et le récupérer

Sur le portail : **Certificates, Identifiers & Profiles → Certificates → (+)** → service **Pass Type ID Certificate** → sélectionner l'identifiant créé à l'étape 1 → uploader `apple-wallet-pass.csr` → télécharger le certificat généré.

Enregistrez le fichier téléchargé dans `data/` sous le nom `pass.cer`, puis convertissez-le en PEM :

```bash
openssl x509 -inform der -in pass.cer -out apple-wallet-pass-cert.pem
```

### 4. Fabriquer le `.p12` (clé + certificat)

```bash
openssl pkcs12 -export \
  -in apple-wallet-pass-cert.pem \
  -inkey apple-wallet-pass.key \
  -out apple-wallet-pass.p12 \
  -passout pass:VOTRE_MOT_DE_PASSE
```

→ Reportez `VOTRE_MOT_DE_PASSE` dans `data/config.yml` → `apple-wallet.p12-password`. Le fichier `apple-wallet-pass.p12` est déjà au bon nom et au bon endroit pour `apple-wallet.p12-path`.

### 5. Récupérer le certificat intermédiaire Apple (WWDR)

Télécharger le certificat **"Apple Worldwide Developer Relations Certification Authority" (G4)** depuis [apple.com/certificateauthority](https://www.apple.com/certificateauthority/), l'enregistrer dans `data/` sous le nom `AppleWWDRCAG4.cer`, puis convertir :

```bash
openssl x509 -inform der -in AppleWWDRCAG4.cer -out apple-wallet-wwdr.pem
```

→ Fichier final `apple-wallet-wwdr.pem`, déjà au bon nom pour `apple-wallet.wwdr-cert-path`.

### 6. Nettoyage (optionnel)

Les fichiers intermédiaires ne sont plus utiles une fois `apple-wallet-pass.p12` et `apple-wallet-wwdr.pem` obtenus :

```bash
rm -f apple-wallet-pass.csr apple-wallet-pass-cert.pem pass.cer AppleWWDRCAG4.cer
```

Gardez `apple-wallet-pass.key` de côté si vous devez régénérer le `.p12` plus tard (par ex. mot de passe oublié) — sinon vous pouvez aussi le supprimer.

### 7. Activer

Dans `data/config.yml` :

```yaml
app:
  apple-wallet:
    pass-type-identifier: "pass.com.reniti.ad-scan"
    team-identifier: "VOTRE_TEAM_ID"
    p12-path: "data/apple-wallet-pass.p12"
    p12-password: "VOTRE_MOT_DE_PASSE"
    wwdr-cert-path: "data/apple-wallet-wwdr.pem"
```

Pas de flag `enabled` à part : Apple Wallet s'active automatiquement dès que
`pass-type-identifier` et `team-identifier` sont tous les deux renseignés. Redémarrez
l'application : le lien "Apple Wallet" apparaît dans la liste des adhérents.

> Tous ces fichiers (`.p12`, `.pem`, `.cer`, `.key`, `.csr`) sont ignorés par git (`.gitignore`) — ils ne doivent jamais être versionnés.

### Bouton "Ajouter à Apple Wallet"

Le bouton affiché sur les pages de partage/inscription (`ad-scan-app/src/main/resources/static/images/wallet/apple-wallet-badge.svg`)
est un **remplaçant générique**, pas le vrai badge Apple : celui-ci est soumis à un accord de
licence ("Wallet Marketing Agreement") à accepter sur
[developer.apple.com/wallet](https://developer.apple.com/wallet/), donc pas redistribuable
directement dans ce repo. Pour utiliser le vrai badge, téléchargez-le une fois l'accord accepté et
remplacez ce fichier (même nom, même emplacement) par le SVG officiel.

## Configuration Google Wallet

Contrairement à Apple, pas de commande openssl à faire soi-même : Google fournit directement une clé de compte de service au format JSON, déjà prête à l'emploi.

### 1. Devenir issuer

Sur [pay.google.com/business/console](https://pay.google.com/business/console), créer un compte issuer (utilise "Create a pass" si proposé). Notez l'**Issuer ID** (numérique, du style `3388000000022123456`).

> Ne le confondez pas avec un ID de classe auto-généré par un assistant de la console — ce n'est pas le même identifiant. L'Issuer ID est toujours numérique.

### 2. Créer le compte de service Google Cloud

Sur [console.cloud.google.com](https://console.cloud.google.com), dans un projet (existant ou nouveau) :

1. **API et services → Bibliothèque** → activer **Google Wallet API**
2. **IAM et administration → Comptes de service → Créer un compte de service** (nom libre, ex. `ad-scan-wallet`)
3. Ouvrir le compte créé → onglet **Clés → Ajouter une clé → JSON** → télécharge un fichier `.json`

### 3. Autoriser ce compte de service dans la Wallet Console

Retour sur `pay.google.com/business/console` → section utilisateurs de votre compte issuer → ajouter l'email du compte de service (visible dans le JSON téléchargé, du style `xxx@xxx.iam.gserviceaccount.com`) avec le rôle **Developer**.

### 4. Placer la clé

Enregistrez le fichier JSON téléchargé sous :

```
data/google-wallet-service-account.json
```

### 5. Activer

Dans `data/config.yml` :

```yaml
app:
  google-wallet:
    issuer-id: "VOTRE_ISSUER_ID"
    class-suffix: "bde_membership"
    service-account-key-path: "data/google-wallet-service-account.json"
```

Pas de flag `enabled` à part : Google Wallet s'active automatiquement dès que `issuer-id` est
renseigné. Le nom affiché sur la carte et sa couleur de fond viennent de `app.wallet`, et le
logo/la bannière de `branding/logo.png`/`branding/banner.png` (voir "Branding des cartes" en haut
de ce fichier) — pas besoin de les redéfinir ici. Ces deux images nécessitent en plus
`app.public-base-url` renseignée (Google les récupère via une URL HTTPS publique, pas un chemin
local) ; sans elle, la carte s'affiche quand même, juste sans logo ni bannière.

Redémarrez l'application : le lien "Google Wallet" apparaît dans la liste des adhérents.

> `data/google-wallet-service-account.json` est ignoré par git — il ne doit jamais être versionné.
