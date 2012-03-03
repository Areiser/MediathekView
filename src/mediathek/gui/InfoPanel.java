/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mediathek.gui;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import mediathek.Daten;
import mediathek.controller.filme.FilmListenerElement;
import mediathek.controller.filme.filmeImportieren.MediathekListener;
import mediathek.controller.filme.filmeImportieren.MediathekTimer;
import mediathek.daten.ListeFilme;

/**
 *
 * @author emil
 */
public final class InfoPanel extends javax.swing.JPanel {

    public static final int IDX_NIX = 0;
    public static final int IDX_GUI_FILME = 1;
    public static final int IDX_GUI_DOWNLOAD = 2;
    public static final int IDX_GUI_ABO = 3;
    private final int IDX_MAX = 4;
    private String[] idx = new String[IDX_MAX];
    private int aktIdx = 0;
    private MediathekTimer mediathekTimer = new MediathekTimer();

    public InfoPanel() {
        initComponents();
        clearProgress();
        for (int i = 0; i < IDX_MAX; ++i) {
            idx[i] = "";
        }
        jLabelStatusLinks.setMinimumSize(new Dimension(25, 25));
        jLabelRechts.setMinimumSize(new Dimension(25, 25));
        jButtonStop.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Daten.filmeLaden.setStop();
            }
        });
        mediathekTimer.addAdListener(new BeobTimer());
    }

    public void setTextLinks(int i, String text) {
        idx[i] = text;
        setIdx(aktIdx);
    }

    public void setIdx(int i) {
        aktIdx = i;
        jLabelStatusLinks.setText(idx[i]);
    }

    public void setProgressBar(FilmListenerElement filmListenerElement) {
        jProgressBar1.setVisible(true);
        jButtonStop.setVisible(true);
        jProgressBar1.setMaximum(filmListenerElement.max);
        jProgressBar1.setMinimum(0);
        jProgressBar1.setValue(filmListenerElement.progress);
        jProgressBar1.setStringPainted(true);
    }

    public void clearProgress() {
        jProgressBar1.setVisible(false);
        jButtonStop.setVisible(false);
    }

    private void setInfoRechts() {
        String textRechts;
        // Text rechts: alter/neuladenIn anzeigen
        textRechts = "Filmliste erstellt: ";
        textRechts += Daten.listeFilme.metaDaten[ListeFilme.FILMLISTE_DATUM_NR];
        textRechts += " Uhr  ||  Alter: ";
        int sekunden = Daten.listeFilme.alterFilmlisteSek();
        int minuten = sekunden / 60;
        String sek = String.valueOf(sekunden % 60);
        String min = String.valueOf(minuten % 60);
        String stu = String.valueOf(minuten / 60);
        while (sek.length() < 2) {
            sek = "0" + sek;
        }
        while (min.length() < 2) {
            min = "0" + min;
        }
        while (stu.length() < 2) {
            stu = "0" + stu;
        }
        textRechts += stu + ":" + min + ":" + sek + " ";
        // Infopanel setzen
        jLabelRechts.setText(textRechts);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabelStatusLinks = new javax.swing.JLabel();
        jLabelRechts = new javax.swing.JLabel();
        jProgressBar1 = new javax.swing.JProgressBar();
        jButtonStop = new javax.swing.JButton();

        setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(153, 153, 153)));
        setMinimumSize(new java.awt.Dimension(15, 15));

        jLabelStatusLinks.setText("jLabel2");

        jLabelRechts.setText("jLabel1");

        jButtonStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/stop_16.png"))); // NOI18N
        jButtonStop.setIconTextGap(1);
        jButtonStop.setMargin(new java.awt.Insets(0, 10, 0, 10));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jLabelStatusLinks)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelRechts)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonStop))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                .addComponent(jLabelStatusLinks)
                .addComponent(jLabelRechts)
                .addComponent(jProgressBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jButtonStop))
        );

        layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonStop, jProgressBar1});

    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonStop;
    private javax.swing.JLabel jLabelRechts;
    private javax.swing.JLabel jLabelStatusLinks;
    private javax.swing.JProgressBar jProgressBar1;
    // End of variables declaration//GEN-END:variables

    private class BeobTimer extends MediathekListener {

        @Override
        public void ping() {
            setInfoRechts();
        }
    }
}
