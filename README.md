# Appli GPS avec cartographie OSMaps & GPX traceur pour montre Android      ([english version](/readme-eng.md) )
----------------------------------------------------------------------
Voici la dernière version 9.5 de mon appli GPS OSMaps & compass Locator pour montre connectée Amazfit Stratos Android (testée sur Amazfit Stratos 2, Stratos 3, PACE, et aussi  SUUNTO...).

La première idée était de faire un simple localisateur pour enregistrer une position (parking) et la retrouver plus tard avec un écran sous forme de compas magnétique, et pourquoi pas de mémoriser les positions des parcours de golf pour faire un Gps de Golf ? Au final, il y a maintenant la possibilité de charger des cartes Open Street Map et de créer et/ou lire des fichiers Gpx.

Cette appli comporte 4 fenêtres principales :

1/ L'écran de Cartographie affiche les tuiles 'Open Street Maps' qui devront être stockées à l'avance (offline) dans le répertoire /osmaps de la montre. Nous verrons (plus loin) comment trouver les tuiles avec MOBAC. Les cartes fonctionnent par niveau de zoom (3 pour voir des pays, 16 pour voir une maison). Les symboles + et - en bas de l'écran permettent de changer de zoom. Le rond du milieu centre la carte sur la position actuelle. Une règle horizontale donne l'échelle en km.

Les fichiers GPX peuvent être représentés sur la carte de deux façons :
- une trace de points reliés dont la couleur dépend de l'altitude (bleu au plus bas, rouge en altitude) quand le fichier contient la balise TRK (trace).
- un ensemble de points non reliés de couleur identiques quand on a une liste de WPT (POI par exemple water.gpx liste des points d'eau) - pas de balise TRK.
En mode navigation vers un point, une ligne est dessinée vers la destination.
Un marqueur bleu indique la position du Gps sur la carte, que l'on peut déplacer en balayant l'écran.

Un Appui Court sur la carte provoque l'incrustation de la vitesse / altitude / distance, un autre Appui ferme la carte.

Un Appui LONG sur la carte, permet :
- de créer un nouveau WPT à l'endroit pointé sur la carte,
- de démarrer l'enregistrement d'une nouvelle trace et mesure de distance.

Voici des exemples de cartes.

<center><img src="/6-carto3.jpg" alt="gps carto osmaps"/></center>

2/ The compass is active when you firstly have selected a nav destination (long click on map or compas), or a waypoint in your Gpx file list.
The compass radar will display : the time, distance & altitude to your nav destination.
If you activate the magnetic sensor (with compatible wear) on the config window, the compass will turn to indicate the real direction.
Without magnetic sensor, the north is on the top of screen (at 12 hours).
Un clic long sur le compas pour marquer la position actuelle comme future Destination (ou pour sauver un nouveau waypoint).
Un clic rapide permet de incruster la vitesse , distance parcourue et altitudes en sur-impression.
Un nouveau clic rapide ferme la vue map.

<center><img src="/5-scann.jpg" alt="gps radar stratos 2"/></center>

3/ The Gpx list view, allows you select a destination by simple click on list.
A 'LONG CLICK' remove the point. A 'simple click' select your destination.
You must have a 'gpx' directory on the root of the wear (external storage memory).

<center><img src="/3-wpt-liste.jpg" alt="liste waypoint gps menu"/></center>

4/ The main menu appears even if the position is not ok. So it's possible to change the settings, or to find and select a waypoint to track.
From up left to right : first button start the settings, the second is for select a Gps waypoint from a gpx file.
Third (bottom) is to exit, and the arrow is to start the tracking radar. Storing the current position or not.

<center><img src="/2-main-screen.jpg" alt="gps main menu"/></center>

5/ L'écran de réglages permet de sélectionner un fichier Gpx (du dossier /GPXData). Cela peut très bien être une ancienne trace de l'appli 'Sports' de la montre.
- 'Auto center map' permet de centrer automatiquement la carte (en cas de footing quand on sort de la carte)...
- 'Show POI' pour afficher les noms des Wpts sur la carte
- 'Auto next wpt' en cas de nav vers un Wpt, force la montre à enchainer vers le prochain WPt (per exemple parcours Golf).
- 'Active compas' pour allumer le compas dans l'écran 'Goto Wpt' - Attention à la batterie !

<center><img src="/4-setting.jpg" alt="gps radar stratos 2"/></center>

Here's the first gps startup screen. This animation just appear for 10 seconds showing the number of satellites in view.
The first gps fixing time depends of your situation, the meteo... From 2 to 30 seconds, it's faster if you don't move...

<center><img src="/1-startscreen.jpg" alt="gps startup fix"/></center>

Ou mettre les MAPS ?
-------------------
Vous devez en premier créer le dossier 'OSMaps' dans le répertoire racine de votre montre avec l'explorateur de fichiers.
Vous aurez ensuite un sous répertoire pour chaque niveau de zzom - de '2' (niveau mondial) à '16' (niveau local).
Les noms des sous-répertoires et des tuiles permet ensuite au système de se repérer en fonction des coordonées X/Y.
Mais n'ayez pas peur, vous n'aurez qu'à télécharger 'MOBILE ATLAS CREATOR' ( https://mobac.sourceforge.io/ ) qui se chargera de créer l'atlas de cartes.

<center><img src="/mobac1.jpg" alt="mobac osm maps"/></center>

Après installation, Cliquez sur 'Nouvel Atlas' (OSMdroid ZIP). Avec les checkboxes de gauche vous devez choisir le/les niveaux de zoom à inclure dans vos cartes.
Le niveau 15 inclus des petits détails à la limite des noms de rue, mais n'est pas disponible pour tous les pays/villes. Et il ne faut pas oublier que le zoom niveau 15 prend 4x plus de mémoire que le niveau 14, et 4x4 fois que le niveau 13... Il faudra donc bien sélectionner les zones que vous voulez couvrir !

Cliquez sur 'Recréer/Ajuster tuiles' !
Puis 'Ajouter à la selection', et 'Creer l'Atlas'.

A la fin du process (qui peut être long), il n'y a plus qu'à dézipper votre atlas et à le copier tel quel dans le répertoire "/OSMaps" de la montre.
Cela doit ressembler à cela.

<center><img src="/osmaps-wear1.jpg" alt="wear osm maps"/></center>

Comment ça marche
-----------------
La première activité au démarrage charge une fois pour toute le fichier gpx déjà choisi. Elle lance aussi un 'Service android' pour capter les trames NMEA. Pour les utiliser, je n'utilise PLUS de Broadcast mais des variables globales (classe Glob. & locationManager + locationListener). Il ne faut surtout pas éteindre et redémarrer le Gps à chaque updates ou à chaque besoin sous peine d'attendre à chaque fois un nouveau fix gps.
J'utilise aussi un 'nmealistener' pour capter les trames $GPGSV & GPGGA pour suivre l'état du signal gps et le nombre de satellites, ainsi que VTG pour avoir la vitesse vraie.
Le compas ('magnetic orientation sensor type3') est utilisé pour tourner les graduations de la boussole avec un 'sensorManager.registerListener'.
Après des essais totalement infructueux d'autres librairies, j'ai décidé de faire ma propre implementation de calcul et d'affichage des tuiles OSM. Chaque tuile est une image png carrée de 256x256 pixels. Chacune est resizée à 512x512 pixels pour avoir plus de visibilité.

Versions log
------------
30/4/2020 Première v1.

1/5/2020  Location & NMEA transmis par broadcast dans Locservice.
          Gros bugs ! Impossible de fermer les process complet (vidait la batterie). Solution ' android.os.Process.killProcess(android.os.Process.myPid());' in OnDestroy().
         
14/5/2020  Version 2 , lecture des GPX, et option 'autonext' des waypoints.

24/05/2020 Version 3 - nombreuses petites corrections & modifs Map

2/06/2020  Version 4 - Long click sur la carte Maps & many fixs

05/06/2020 Version 4.2 - modif on stop app

07/06/2020 Version 4.4 - Modifs affichage, alertdialog...

12/06/2020 v4.6 - Modif pour different screen dimensions (SUUNTO)

13/06/2020 v4.6.2 - Possible bug fix with gpx display on cartodraw

15/06/2020 v4.6.4 - Modif 2 display on Suunto / bug fix on delete waypoint 

4/04/2021 v5.0 - Calc & Incrustation speed & distance and direct track saving.

11/2021 - Capture de trame VTG pour affichage vitesse vraie. GSV pour nombre de satellites en vue.

12/2021 - Gros changements pour gestion des GROS Gpx - au startup...
         Test si affichage de TRK ou de liste POI.



