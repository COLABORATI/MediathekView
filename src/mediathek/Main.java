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
package mediathek;

import com.jidesoft.utils.SystemInfo;
import com.jidesoft.utils.ThreadCheckingRepaintManager;
import mediathek.controller.Log;
import mediathek.daten.Daten;
import mediathek.mac.MediathekGuiMac;
import mediathek.tool.Konstanten;
import mediathek.tool.MVSingleInstance;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public enum StartupMode {

        GUI, AUTO, FASTAUTO
    }

    /**
     * Ensures that old film lists in .mediathek directory get deleted because they were moved to
     * ~/Library/Caches/MediathekView
     */
    private static void cleanupOsxFiles() {
        try {
            Path oldFilmList = Paths.get(Daten.getSettingsDirectory_String() + File.separator + Konstanten.JSON_DATEI_FILME);
            Files.deleteIfExists(oldFilmList);
        } catch (Exception ignored) {
        }
    }

     /*
     * Aufruf:
     * java -jar Mediathek [Pfad zur Konfigdatei, sonst homeverzeichnis] [Schalter]
     *
     * Programmschalter:
     *
     * -M Fenster maximiert starten
     * -A Automodus
     * -noGui ohne GUI starten und die Filmliste laden
     *
     * */

    /**
     * @param args the command line arguments
     */
    public static void main(final String args[]) {
        if (SystemInfo.isMacOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            cleanupOsxFiles();
        }

        StartupMode state = StartupMode.GUI;
        boolean showWindowMaximized = false;

        if (args != null) {
            for (String s : args) {
                s = s.toLowerCase();
                switch (s) {
                    case "-auto":
                        state = StartupMode.AUTO;
                        break;

                    case "-fastauto":
                        state = StartupMode.FASTAUTO;
                        break;

                    case "-v":
                        Log.versionsMeldungen();
                        System.exit(0);
                        break;

                    case "-d":
                        Daten.debug = true;
                        break;

                    case "-M":
                        showWindowMaximized = true;
                        break;
                }
            }
        }

        /*
        If user tries to start MV from command-line without proper options,
        instead of crashing while trying to open Swing windows, just change to CLI mode and warn the user.
         */
        if (GraphicsEnvironment.isHeadless() && (state == StartupMode.GUI)) {
            System.err.println("MediathekView wurde nicht als Kommandozeilenprogramm gestartet.");
            System.err.println("Startmodus wurde auf -auto geändert.");
            System.err.println();
            state = StartupMode.AUTO;
        }

        switch (state) {
            case AUTO:
                new MediathekAuto(args).starten();
                break;

            case FASTAUTO:
                final MediathekAuto mvAuto = new MediathekAuto(args);
                mvAuto.setFastAuto(true);
                mvAuto.starten();
                break;

            case GUI:
                final boolean finalShowWindowMaximized = showWindowMaximized;
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        if (Daten.debug) {
                            // use for debugging EDT violations
                            RepaintManager.setCurrentManager(new ThreadCheckingRepaintManager());

                            if (SystemInfo.isMacOSX()) {
                                //prevent startup of multiple instances...useful during debugging :(
                                MVSingleInstance singleInstanceWatcher = new MVSingleInstance();
                                if (singleInstanceWatcher.isAppAlreadyActive()) {
                                    JOptionPane.showMessageDialog(null, "MediathekView is already running!");
                                    //System.exit(1);
                                }
                            }
                        }
                        if (SystemInfo.isMacOSX()) {
                            new MediathekGuiMac(args,finalShowWindowMaximized).setVisible(true);
                        }
                        else
                            new MediathekGui(args, finalShowWindowMaximized).setVisible(true);
                    }
                });
                break;
        }
    }
}
