/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.config;

import com.jidesoft.utils.SystemInfo;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import mSearch.Config;
import mSearch.daten.ListeFilme;
import mSearch.filmlisten.writer.FilmListWriter;
import mSearch.tool.Listener;
import mSearch.tool.ReplaceList;
import mediathek.MediathekGui;
import mediathek.controller.IoXmlLesen;
import mediathek.controller.IoXmlSchreiben;
import mediathek.controller.MVUsedUrls;
import mediathek.controller.starter.StarterClass;
import mediathek.daten.*;
import mediathek.filmlisten.FilmeLaden;
import mediathek.gui.actions.FilmListWriteWorkerTask;
import mediathek.gui.dialog.DialogMediaDB;
import mediathek.gui.filmInformation.InfoDialog;
import mediathek.gui.messages.BaseEvent;
import mediathek.gui.messages.TimerEvent;
import mediathek.javafx.CenteredBorderPane;
import mediathek.javafx.VerticalSeparator;
import mediathek.tool.GuiFunktionen;
import mediathek.tool.MVFont;
import mediathek.tool.MVMessageDialog;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.bus.config.BusConfiguration;
import net.engio.mbassy.bus.config.Feature;
import net.engio.mbassy.bus.config.IBusConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class Daten {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    // zentrale Klassen
    public static final MVColor mVColor = new MVColor(); // verwendete Farben
    private static final Logger logger = LogManager.getLogger(Daten.class);
    /**
     * Maximum number of backup files to be stored.
     */
    private final static int MAX_COPY = 5;
    public static ListePset listePset;
    //alle Programmeinstellungen
    public static InfoDialog filmInfo; // Infos zum Film
    private static Daten instance;
    // flags
    private static boolean startMaximized; // Fenster maximieren
    private static boolean auto; // Version: MediathekAuto
    private static boolean reset; // Programm auf Starteinstellungen zurücksetzen
    // Verzeichnis zum Speichern der Programmeinstellungen
    private static String basisverzeichnis;
    private MediathekGui mediathekGui; // JFrame der Gui
    public MVUsedUrls history; // alle angesehenen Filme
    public MVUsedUrls erledigteAbos; // erfolgreich geladenen Abos
    public StarterClass starterClass; // Klasse zum Ausführen der Programme (für die Downloads): VLC, flvstreamer, ...
    private FilmeLaden filmeLaden; // erledigt das updaten der Filmliste
    private ListeFilme listeFilme;
    private ListeFilme listeFilmeNachBlackList; // ist DIE Filmliste
    private ListeFilme listeFilmeHistory; // für die HEUTIGE HISTORY
    private ListeDownloads listeDownloads; // Filme die als "Download: Tab Download" geladen werden sollen
    private ListeDownloads listeDownloadsButton; // Filme die über "Tab Filme" als Button/Film abspielen gestartet werden
    private ListeBlacklist listeBlacklist;
    private ListeMediaDB listeMediaDB;
    private ListeMediaPath listeMediaPath;
    private ListeAbo listeAbo;
    private DownloadInfos downloadInfos;
    private DialogMediaDB dialogMediaDB;
    private boolean alreadyMadeBackup;
    private MBassador<BaseEvent> messageBus;
    private FilmListWriteWorkerTask writerTask;

    private Daten() {
        mediathekGui = null;
        start();
    }

    private Daten(MediathekGui aMediathekGui) {
        mediathekGui = aMediathekGui;
        start();
    }

    public static boolean isStartMaximized() {
        return startMaximized;
    }

    public static void setStartMaximized(final boolean aIsStartMaximized) {
        startMaximized = aIsStartMaximized;
    }

    public static boolean isAuto() {
        return auto;
    }

    public static void setAuto(final boolean aIsAuto) {
        auto = aIsAuto;
    }

    public static boolean isReset() {
        return reset;
    }

    public static void setReset(final boolean aIsReset) {
        reset = aIsReset;
    }

    public static Daten getInstance(String aBasisverzeichnis) {
        basisverzeichnis = aBasisverzeichnis;
        return getInstance();
    }

    public static Daten getInstance(String aBasisverzeichnis, MediathekGui aMediathekGui1) {
        basisverzeichnis = aBasisverzeichnis;
        return getInstance(aMediathekGui1);
    }

    private static Daten getInstance(MediathekGui aMediathekGui) {
        return instance == null ? instance = new Daten(aMediathekGui) : instance;
    }

    public static Daten getInstance() {
        return instance == null ? instance = new Daten() : instance;
    }

    /**
     * Liefert den Pfad zur Filmliste
     *
     * @return Den Pfad als String
     */
    public static String getDateiFilmliste() {
        String strFile;

        if (Config.isPortableMode()) {
            strFile = getSettingsDirectory_String() + File.separator + Konstanten.JSON_DATEI_FILME;
        } else {
            if (SystemInfo.isMacOSX()) {
                //place filmlist into OS X user cache directory in order not to backup it all the time in TimeMachine...
                strFile = GuiFunktionen.getHomePath() + File.separator + "Library/Caches/MediathekView" + File.separator + Konstanten.JSON_DATEI_FILME;
            } else {
                strFile = getSettingsDirectory_String() + File.separator + Konstanten.JSON_DATEI_FILME;
            }
        }

        return strFile;
    }

    /**
     * Return the location of the settings directory.
     * If it does not exist, create one.
     *
     * @return Path to the settings directory
     * @throws IllegalStateException Will be thrown if settings directory don't exist and if there is an error on creating it.
     */
    private static Path getSettingsDirectory() throws IllegalStateException {
        final Path baseDirectoryPath;
        if (basisverzeichnis == null || basisverzeichnis.isEmpty()) {
            baseDirectoryPath = Paths.get(System.getProperty("user.home"), Konstanten.VERZEICHNIS_EINSTELLUNGEN);
        } else {
            baseDirectoryPath = Paths.get(basisverzeichnis);
        }


        if (Files.notExists(baseDirectoryPath)) {
            try {
                Files.createDirectories(baseDirectoryPath);
            } catch (IOException ioException) {
                Messages.logMessage(Messages.ERROR_CANT_CREATE_FOLDER, ioException, baseDirectoryPath.toString());
                throw new IllegalStateException(Messages.ERROR_CANT_CREATE_FOLDER.getTextFormatted(baseDirectoryPath.toString()), ioException);
            }
        }

        return baseDirectoryPath;
    }

    public static String getSettingsDirectory_String() {
        return getSettingsDirectory().toString();
    }

    /**
     * Return the path to "mediathek.xml"
     *
     * @return Path object to mediathek.xml file
     */
    public static Path getMediathekXmlFilePath() {
        return Daten.getSettingsDirectory().resolve(Konstanten.CONFIG_FILE);
    }

    /**
     * Return the path to "mediathek.xml_copy_"
     * first copy exists
     *
     * @param xmlFilePath Path to file.
     */
    private static void getMediathekXmlCopyFilePath(ArrayList<Path> xmlFilePath) {
        for (int i = 1; i <= MAX_COPY; ++i) {
            Path path = Daten.getSettingsDirectory().resolve(Konstanten.CONFIG_FILE_COPY + i);
            if (Files.exists(path)) {
                xmlFilePath.add(path);
            }
        }
    }

    /**
     * Return the number of milliseconds from today´s midnight.
     *
     * @return Number of milliseconds from today´s midnight.
     */
    private static long getHeute_0Uhr() {
        final Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTimeInMillis();
    }

    public FutureTask<Void> getWriterTask() {
        return writerTask;
    }

    public MBassador<BaseEvent> getMessageBus() {
        return messageBus;
    }

    /**
     * Set up message bus to log errors to our default logger
     */
    private void setupMessageBus() {
        messageBus = new MBassador<>(new BusConfiguration()
                .addFeature(Feature.SyncPubSub.Default())
                .addFeature(Feature.AsynchronousHandlerInvocation.Default())
                .addFeature(Feature.AsynchronousMessageDispatch.Default())
                .addPublicationErrorHandler(error -> logger.error(error.getMessage(), error.getCause()))
                .setProperty(IBusConfiguration.Properties.BusId, "global bus"));
    }

    private void start() {
        setupMessageBus();

        listeFilme = new ListeFilme();
        filmeLaden = new FilmeLaden(this);
        listeFilmeHistory = new ListeFilme();

        updateSplashScreen("Lade Blacklist...");
        listeFilmeNachBlackList = new ListeFilme();
        listeBlacklist = new ListeBlacklist();

        updateSplashScreen("Lade Programmsets...");
        listePset = new ListePset();

        updateSplashScreen("Lade Abos...");
        listeAbo = new ListeAbo(this);

        updateSplashScreen("Lade Downloads...");
        listeDownloads = new ListeDownloads(this);
        listeDownloadsButton = new ListeDownloads(this);

        updateSplashScreen("Lade erledigte Abos...");
        erledigteAbos = new MVUsedUrls(Konstanten.FILE_ERLEDIGTE_ABOS, getSettingsDirectory_String(), Listener.EREIGNIS_LISTE_ERLEDIGTE_ABOS);

        updateSplashScreen("Lade History...");
        history = new MVUsedUrls(Konstanten.FILE_HISTORY, getSettingsDirectory_String(), Listener.EREIGNIS_LISTE_HISTORY_GEAENDERT);

        listeMediaDB = new ListeMediaDB(this);
        listeMediaPath = new ListeMediaPath();

        downloadInfos = new DownloadInfos(this);
        starterClass = new StarterClass(this);

        Timer timer = new Timer(1000, e ->
        {
            downloadInfos.makeDownloadInfos();
            messageBus.publishAsync(new TimerEvent());
        });
        timer.setInitialDelay(4000); // damit auch alles geladen ist
        timer.start();
    }

    public void filmlisteSpeichern() {
        try {
            if (writerTask != null) {
                logger.info("Waiting for worker task");
                writerTask.get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            logger.error("Error waiting for worker task", e);
        }

        if (mediathekGui != null) {
            Platform.runLater(() -> {
                HBox hb = new HBox();
                hb.setSpacing(4d);
                Label lb = new Label("");
                ProgressBar prog = new ProgressBar();
                prog.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
                hb.getChildren().addAll(
                        new VerticalSeparator(),
                        new CenteredBorderPane(lb),
                        new CenteredBorderPane(prog)
                );

                writerTask = new FilmListWriteWorkerTask(this);
                writerTask.setOnRunning(e -> {
                    mediathekGui.getStatusBarController().getStatusBar().getRightItems().add(hb);
                    lb.textProperty().bind(writerTask.messageProperty());
                    prog.progressProperty().bind(writerTask.progressProperty());
                });
                writerTask.setOnSucceeded(e -> mediathekGui.getStatusBarController().getStatusBar().getRightItems().remove(hb));
                writerTask.setOnFailed(e -> mediathekGui.getStatusBarController().getStatusBar().getRightItems().remove(hb));
                new Thread(writerTask).start();
            });
        } else {
            FilmListWriter writer = new FilmListWriter();
            writer.writeFilmList(getDateiFilmliste(), listeFilme, null);
        }
    }

    /**
     * Update the {@link java.awt.SplashScreen} only if we have a Swing UI.
     *
     * @param text The displayed text on the splash graphics.
     */
    private void updateSplashScreen(String text) {
        if (mediathekGui != null) {
            mediathekGui.updateSplashScreenText(text);
        }
    }

    public boolean allesLaden() {
        updateSplashScreen("Lade Konfigurationsdaten...");

        if (!load()) {
            logger.info("Weder Konfig noch Backup konnte geladen werden!");
            // teils geladene Reste entfernen
            clearKonfig();
            return false;
        }
        logger.info("Konfig wurde gelesen!");
        mVColor.load(); // Farben einrichten
        MVFont.initFont(); // Fonts einrichten

        // erst die Systemdaten, dann die Filmliste
        updateSplashScreen("Lade Filmliste...");
        return true;
    }

    private void clearKonfig() {
        listePset.clear();
        ReplaceList.list.clear();
        listeAbo.clear();
        listeDownloads.clear();
        listeBlacklist.clear();
    }

    private boolean load() {
        boolean ret = false;
        Path xmlFilePath = Daten.getMediathekXmlFilePath();

        if (Files.exists(xmlFilePath)) {
            final IoXmlLesen configReader = new IoXmlLesen();
            if (configReader.datenLesen(xmlFilePath)) {
                return true;
            } else {
                // dann hat das Laden nicht geklappt
                logger.info("Konfig konnte nicht gelesen werden!");
            }
        } else {
            // dann hat das Laden nicht geklappt
            logger.info("Konfig existiert nicht!");
        }

        // versuchen das Backup zu laden
        if (loadBackup()) {
            ret = true;
        }
        return ret;
    }

    private boolean loadBackup() {
        boolean ret = false;
        ArrayList<Path> path = new ArrayList<>();
        Daten.getMediathekXmlCopyFilePath(path);
        if (path.isEmpty()) {
            logger.info("Es gibt kein Backup");
            return false;
        }

        // dann gibts ein Backup
        logger.info("Es gibt ein Backup");
        mediathekGui.closeSplashScreen();
        int r = JOptionPane.showConfirmDialog(null, "Die Einstellungen sind beschädigt\n"
                + "und können nicht geladen werden.\n"
                + "Soll versucht werden, mit gesicherten\n"
                + "Einstellungen zu starten?\n\n"
                + "(ansonsten startet das Programm mit\n"
                + "Standardeinstellungen)", "Gesicherte Einstellungen laden?", JOptionPane.YES_NO_OPTION);

        if (r != JOptionPane.OK_OPTION) {
            logger.info("User will kein Backup laden.");
            return false;
        }

        for (Path p : path) {
            // teils geladene Reste entfernen
            clearKonfig();
            logger.info("Versuch Backup zu laden: {}", p.toString());
            final IoXmlLesen configReader = new IoXmlLesen();
            if (configReader.datenLesen(p)) {
                logger.info("Backup hat geklappt: {}", p.toString());
                ret = true;
                break;
            }

        }
        return ret;
    }

    public void allesSpeichern() {
        konfigCopy();

        final IoXmlSchreiben configWriter = new IoXmlSchreiben();
        configWriter.writeConfigurationFile(getMediathekXmlFilePath());

        if (Daten.isReset()) {
            // das Programm soll beim nächsten Start mit den Standardeinstellungen gestartet werden
            // dazu wird den Ordner mit den Einstellungen umbenannt
            String dir1 = getSettingsDirectory_String();
            if (dir1.endsWith(File.separator)) {
                dir1 = dir1.substring(0, dir1.length() - 1);
            }

            try {
                final Path path1 = Paths.get(dir1);
                final String dir2 = dir1 + "--" + new SimpleDateFormat("yyyy.MM.dd__HH.mm.ss").format(new Date());

                Files.move(path1, Paths.get(dir2), StandardCopyOption.REPLACE_EXISTING);
                Files.deleteIfExists(path1);
            } catch (IOException e) {
                logger.error("Die Einstellungen konnten nicht zurückgesetzt werden.", e);
                if (mediathekGui != null) {
                    MVMessageDialog.showMessageDialog(mediathekGui, "Die Einstellungen konnten nicht zurückgesetzt werden.\n"
                            + "Sie müssen jetzt das Programm beenden und dann den Ordner:\n"
                            + getSettingsDirectory_String() + '\n'
                            + "von Hand löschen und dann das Programm wieder starten.\n\n"
                            + "Im Forum finden Sie weitere Hilfe.", "Fehler", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    /**
     * Create backup copies of settings file.
     */
    private void konfigCopy() {
        if (!alreadyMadeBackup) {
            // nur einmal pro Programmstart machen
            logger.info("-------------------------------------------------------");
            logger.info("Einstellungen sichern");

            try {
                final Path xmlFilePath = Daten.getMediathekXmlFilePath();
                long creatTime = -1;

                Path xmlFilePathCopy_1 = Daten.getSettingsDirectory().resolve(Konstanten.CONFIG_FILE_COPY + 1);
                if (Files.exists(xmlFilePathCopy_1)) {
                    BasicFileAttributes attrs = Files.readAttributes(xmlFilePathCopy_1, BasicFileAttributes.class);
                    FileTime d = attrs.lastModifiedTime();
                    creatTime = d.toMillis();
                }

                if (creatTime == -1 || creatTime < getHeute_0Uhr()) {
                    // nur dann ist die letzte Kopie älter als einen Tag
                    for (int i = MAX_COPY; i > 1; --i) {
                        xmlFilePathCopy_1 = Daten.getSettingsDirectory().resolve(Konstanten.CONFIG_FILE_COPY + (i - 1));
                        final Path xmlFilePathCopy_2 = Daten.getSettingsDirectory().resolve(Konstanten.CONFIG_FILE_COPY + i);
                        if (Files.exists(xmlFilePathCopy_1)) {
                            Files.move(xmlFilePathCopy_1, xmlFilePathCopy_2, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                    if (Files.exists(xmlFilePath)) {
                        Files.move(xmlFilePath, Daten.getSettingsDirectory().resolve(Konstanten.CONFIG_FILE_COPY + 1), StandardCopyOption.REPLACE_EXISTING);
                    }
                    logger.info("Einstellungen wurden gesichert");
                } else {
                    logger.info("Einstellungen wurden heute schon gesichert");
                }
            } catch (IOException e) {
                logger.error("Die Einstellungen konnten nicht komplett gesichert werden!", e);
            }

            alreadyMadeBackup = true;
            logger.info("-------------------------------------------------------");
        }
    }

    public FilmeLaden getFilmeLaden() {
        return filmeLaden;
    }

    public ListeFilme getListeFilme() {
        return listeFilme;
    }

    public ListeFilme getListeFilmeNachBlackList() {
        return listeFilmeNachBlackList;
    }

    public ListeFilme getListeFilmeHistory() {
        return listeFilmeHistory;
    }

    public ListeDownloads getListeDownloads() {
        return listeDownloads;
    }

    public ListeDownloads getListeDownloadsButton() {
        return listeDownloadsButton;
    }

    public ListeBlacklist getListeBlacklist() {
        return listeBlacklist;
    }

    public ListeMediaDB getListeMediaDB() {
        return listeMediaDB;
    }

    public ListeMediaPath getListeMediaPath() {
        return listeMediaPath;
    }

    public ListeAbo getListeAbo() {
        return listeAbo;
    }

    public DownloadInfos getDownloadInfos() {
        return downloadInfos;
    }

    public MediathekGui getMediathekGui() {
        return mediathekGui;
    }

    public void setMediathekGui(MediathekGui gui) {
        mediathekGui = gui;
    }

    public DialogMediaDB getDialogMediaDB() {
        return dialogMediaDB;
    }

    public void setDialogMediaDB(final DialogMediaDB aDialogMediaDB) {
        dialogMediaDB = aDialogMediaDB;
    }

}
