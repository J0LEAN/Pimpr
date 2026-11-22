"""Le catalogue des thèmes à images : textes, mots-clés et réglages de recadrage.

Clés facultatives : dir, focus (x, y de 0 à 1 pour le recadrage du verrouillage), home
("soft" flou teinté, "dim" assombri, "veil" voilé, "same"), dark, main/card/onCard/card2/
onCard2/accent/tiles/base (couleurs imposées, 0xRRGGBB), widgets (ordre des images),
wfocus, pixel, tile, shape, corner, ornament, caption, featured, credit.
"""

def t(id, name, tagline, cat, tags, font, quote, **kw):
    return dict(id=id, name=name, tagline=tagline, cat=cat, tags=tags, font=font, quote=quote, **kw)

THEMES = [
    # ------------------------------------------------------------ Esthétique
    t("cerise", "Cerise", "Rouge cerise et crème, façon carnet gourmand", "AESTHETIC",
      "rouge cerise fruit bordeaux crème vintage coquette amour", "abril",
      "Juteuse, sucrée, irrésistible.", quoteFont="playfair", ornament="favorite", recipe=0, featured=True,
      dark=False, main=0x9B2226, base=0xF3E9DD, accent=0x7A1B1F),
    t("sauge", "Sauge", "Vert sauge, roses blanches et papier crème", "AESTHETIC",
      "vert sauge crème fleurs roses doux botanique cottage calme", "playfair",
      "Ma maison de pierre, ton lierre y grimpe.", quoteFont="playfair", ornament="local_florist", recipe=4),
    t("academie", "Académie", "Vieux livres, marbre et lumière de bougie", "AESTHETIC",
      "dark academia livres bibliothèque marron sépia statue vintage sombre étude", "playfair",
      "Lis. Apprends. Recommence.", quoteFont="playfair", recipe=1),
    t("foret-pluie", "Sous la pluie", "Vert profond, mousse et sous-bois mouillé", "AESTHETIC",
      "vert sombre forêt pluie mousse fougères calme nature dark", "spacemono",
      "Même sans le voir, tu avances.", quoteFont="playfair", recipe=2),
    t("sakura", "Sakura", "Cerisiers en fleurs dans la brume", "AESTHETIC",
      "rose cerisier fleurs sakura brume japon printemps doux", "fredoka",
      "Bonjour, toi.", quoteFont="caveat", ornament="favorite", recipe=3, home="veil"),

    # ------------------------------------------------------------ Nature et fleurs
    t("herbier", "Herbier", "Planches botaniques sur papier ancien", "NATURE",
      "fleurs botanique herbier roses crème vintage papier dessin", "playfair",
      "Pousse à ton rythme.", quoteFont="playfair", ornament="local_florist", recipe=0),
    t("automne", "Automne", "Feuilles rousses et brume du matin", "NATURE",
      "automne feuilles orange roux forêt brume champignons cosy", "abril",
      "Un thé, un plaid, la pluie.", quoteFont="caveat", recipe=1),
    t("papillons", "Papillons", "Ailes éclatantes des planches naturalistes", "NATURE",
      "papillons insectes couleurs planche vintage ailes", "comfortaa",
      "Deviens ce que tu es.", quoteFont="playfair", recipe=4),

    # ------------------------------------------------------------ Paysages
    t("montagne", "Sommets", "Lac alpin et cimes enneigées", "LANDSCAPE",
      "montagne lac neige sapins bleu alpes randonnée froid", "bebas",
      "L'air est plus clair là-haut.", quoteFont="figtree", recipe=1),
    t("dunes", "Dunes", "Le désert à l'heure dorée", "LANDSCAPE",
      "désert dunes sable orange coucher soleil chaud canyon", "unbounded",
      "Rien ne presse.", quoteFont="playfair", recipe=2),
    t("lagon", "Lagon", "Eau turquoise et sable clair", "LANDSCAPE",
      "mer plage turquoise océan vagues été vacances bleu palmiers", "comfortaa",
      "Sel sur la peau, sable aux pieds.", quoteFont="caveat", recipe=3),
    t("aurore", "Aurore boréale", "Voiles verts dans la nuit polaire", "LANDSCAPE",
      "aurore boréale nuit vert violet neige étoiles nord sombre", "orbitron",
      "Regarde en l'air.", quoteFont="figtree", recipe=1),

    # ------------------------------------------------------------ Animaux
    t("renard", "Renard", "Les habitants de la forêt, de tout près", "ANIMALS",
      "renard animaux forêt cerf chouette roux nature sauvage", "abril",
      "Rusé, discret, libre.", quoteFont="playfair", recipe=0),
    t("chatons", "Chatons", "Moustaches, coussinets et siestes au soleil", "ANIMALS",
      "chat chaton animaux mignon cosy beige douceur", "fredoka",
      "Ronron en cours.", quoteFont="caveat", ornament="favorite", recipe=4),
    t("oiseaux", "Volière", "Les oiseaux d'Audubon, plume par plume", "ANIMALS",
      "oiseaux audubon plumes flamant héron planche vintage animaux", "playfair",
      "Prends ton envol.", quoteFont="playfair", recipe=2),
    t("abysses", "Abysses", "Méduses et créatures marines de Haeckel", "ANIMALS",
      "méduses mer océan haeckel abysses planche vintage bleu corail animaux", "abril",
      "Tout au fond, ça brille.", quoteFont="playfair", recipe=1),
    t("koson", "Corbeaux et lune", "Oiseaux et animaux des estampes de Koson", "ANIMALS",
      "estampe japon oiseaux koson lune corbeau animaux zen", "playfair",
      "Le silence a des ailes.", quoteFont="playfair", recipe=3),

    # ------------------------------------------------------------ Mignon
    t("chats-wain", "Chats farfelus", "Les chats rieurs de Louis Wain", "CUTE",
      "chat mignon drôle couleurs illustration vintage kawaii", "fredoka",
      "Miaou d'abord, le reste ensuite.", quoteFont="caveat", recipe=0, tile="CIRCLE"),
    t("lapin", "Pierre Lapin", "Aquarelles tendres de Beatrix Potter", "CUTE",
      "lapin mignon aquarelle beatrix potter enfance doux jardin kawaii", "comfortaa",
      "Petit, mais décidé.", quoteFont="caveat", recipe=4),
    t("sucreries", "Sucreries", "Macarons, glaces et couleurs dragée", "CUTE",
      "bonbons macarons glace rose pastel gourmand mignon kawaii sucre", "fredoka",
      "La vie en rose dragée.", quoteFont="caveat", ornament="favorite", recipe=3, tile="FLOWER"),
    t("peluches", "Doudous", "Oursons et jouets d'autrefois", "CUTE",
      "ours peluche doudou mignon enfance pastel jouets kawaii", "comfortaa",
      "Câlin gratuit.", quoteFont="caveat", ornament="favorite", recipe=2),

    # ------------------------------------------------------------ Anime et Japon
    t("vague", "Grande vague", "Hokusai : la mer, l'écume et le mont Fuji", "JAPAN",
      "japon vague hokusai estampe bleu mer fuji ukiyo-e", "abril",
      "Sept fois à terre, huit fois debout.", quoteFont="playfair", recipe=4),
    t("edo-nuit", "Nuit d'Edo", "Hiroshige : pluie, neige et lanternes", "JAPAN",
      "japon nuit hiroshige estampe indigo pluie neige lune ukiyo-e", "playfair",
      "La lune suit celui qui marche.", quoteFont="playfair", ornament="bedtime", recipe=1),
    t("ciel-anime", "Ciel d'été", "Des cumulus comme dans un film d'animation", "JAPAN",
      "anime ciel nuages été bleu coucher soleil manga japon rêve", "unbounded",
      "Un été qui ne finit pas.", quoteFont="figtree", recipe=3),
    t("neon-ville", "Tokyo la nuit", "Enseignes, pluie et reflets violets", "JAPAN",
      "anime ville nuit néon tokyo japon cyberpunk violet pluie rue", "orbitron",
      "La ville ne dort pas, moi non plus.", quoteFont="spacemono", recipe=2),

    # ------------------------------------------------------------ Gaming
    t("pixel-crepuscule", "Pixel crépuscule", "Un décor de jeu à l'heure bleue", "GAMING",
      "pixel art jeu rétro gaming crépuscule montagne 16-bit paysage", "pixel",
      "Appuie sur Start.", quoteFont="pixel", recipe=3, pixel=True, shape="ROUNDED", corner=0.12, tile="ROUNDED"),
    t("pixel-ville", "Pixel city", "Toits et néons en pixel art", "GAMING",
      "pixel art jeu rétro gaming ville néon cyberpunk nuit 16-bit", "pixel",
      "Nouvelle partie ?", quoteFont="pixel", recipe=1, pixel=True, corner=0.12, tile="ROUNDED"),
    t("pixel-donjon", "Donjon", "Tuiles de RPG rétro, vues de dessus", "GAMING",
      "pixel art jeu rétro gaming rpg donjon aventure 8-bit héros", "pixel",
      "Il est dangereux d'y aller seul.", quoteFont="pixel", recipe=0, pixel=True, corner=0.12, tile="ROUNDED"),
    t("arcade", "Salle d'arcade", "Bornes, néons et manettes rétro", "GAMING",
      "arcade gaming jeu rétro néon manette borne console magenta cyan", "monoton",
      "Insert coin.", quoteFont="orbitron", recipe=2, tile="ROUNDED"),
    t("setup", "Setup", "Clavier rétroéclairé et lumières RGB", "GAMING",
      "gaming setup clavier rgb pc manette casque sombre gamer", "orbitron",
      "GG WP.", quoteFont="orbitron", recipe=4, tile="ROUNDED"),

    # ------------------------------------------------------------ Cosmos
    t("nebuleuse", "Nébuleuse", "Nuages d'étoiles vus par les télescopes de la NASA", "COSMOS",
      "espace nébuleuse étoiles nasa galaxie cosmos orange bleu nuit", "orbitron",
      "Nous sommes faits de poussière d'étoiles.", quoteFont="figtree", recipe=1),
    t("planetes", "Planètes", "La Terre, la Lune et les géantes", "COSMOS",
      "espace planète terre lune jupiter saturne nasa cosmos noir", "unbounded",
      "Un point bleu pâle.", quoteFont="figtree", recipe=2),
    t("voie-lactee", "Voie lactée", "La galaxie au-dessus des crêtes", "COSMOS",
      "espace étoiles voie lactée nuit ciel galaxie montagne bleu violet", "bebas",
      "Lève les yeux.", quoteFont="playfair", recipe=3),

    # ------------------------------------------------------------ Art
    t("nympheas", "Nymphéas", "Monet et les jardins impressionnistes", "ART",
      "art peinture monet impressionnisme nénuphars jardin bleu vert lilas", "playfair",
      "Je peins comme l'oiseau chante.", quoteFont="playfair", recipe=4),
    t("vangogh", "Van Gogh", "Cyprès, iris et ciels tourbillonnants", "ART",
      "art peinture van gogh bleu jaune nuit étoilée iris tournesols", "abril",
      "Je rêve ma peinture, puis je peins mon rêve.", quoteFont="playfair", recipe=1),
    t("morris", "Morris", "Les papiers peints de William Morris", "ART",
      "art motif morris papier peint fleurs feuilles vintage arts and crafts", "playfair",
      "N'aie rien chez toi que tu ne trouves beau.", quoteFont="playfair", recipe=0),
    t("mucha", "Art nouveau", "Les affiches fleuries d'Alphonse Mucha", "ART",
      "art nouveau mucha affiche fleurs femme ocre vintage rose", "abril",
      "L'art existe pour dire l'âme.", quoteFont="playfair", recipe=2, tile="CIRCLE",
      wfocus={"w1": (0.5, 0.18), "w2": (0.5, 0.2), "w3": (0.5, 0.18), "w4": (0.5, 0.2)}),
    t("klimt-or", "Or", "Feuille d'or, mosaïques et arabesques", "ART",
      "art or doré klimt mosaïque luxe noir motif", "abril",
      "Tout ce qui brille.", quoteFont="playfair", recipe=3),
]
